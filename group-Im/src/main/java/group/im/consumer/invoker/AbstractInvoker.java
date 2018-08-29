package group.im.consumer.invoker;

import org.jupiter.rpc.*;
import org.jupiter.rpc.consumer.cluster.ClusterInvoker;
import org.jupiter.rpc.consumer.dispatcher.Dispatcher;
import org.jupiter.rpc.consumer.future.InvokeFuture;
import org.jupiter.rpc.model.metadata.ClusterStrategyConfig;
import org.jupiter.rpc.model.metadata.MessageWrapper;
import org.jupiter.rpc.model.metadata.MethodSpecialConfig;
import org.jupiter.rpc.model.metadata.ServiceMetadata;
import org.jupiter.rpc.tracing.TraceId;
import org.jupiter.rpc.tracing.TracingUtil;

import java.util.List;

/**
 * jupiter
 * org.jupiter.rpc.consumer.invoker
 *
 * @author jiachun.fjc
 */
public abstract class AbstractInvoker {

    private final String appName;
    private final ServiceMetadata metadata; // 目标服务元信息
    private final ClusterStrategyBridging clusterStrategyBridging;

    public AbstractInvoker(String appName,
                           ServiceMetadata metadata,
                           Dispatcher dispatcher,
                           ClusterStrategyConfig defaultStrategy,
                           List<MethodSpecialConfig> methodSpecialConfigs) {
        this.appName = appName;
        this.metadata = metadata;
        clusterStrategyBridging = new ClusterStrategyBridging(dispatcher, defaultStrategy, methodSpecialConfigs);
    }

    protected Object doInvoke(String methodName, Object[] args, Class<?> returnType, boolean sync) throws Throwable {

        JRequest request = createRequest(methodName, args);
        ClusterInvoker invoker = clusterStrategyBridging.findClusterInvoker(methodName);
        Context invokeCtx = new Context(invoker, returnType, sync);//context可以理解成一个bean，这个bean中包含了很多的信息。通常bean中的方法还是静态的，让其他模块可以从context中获取需要的信息

        Chains.invoke(request, invokeCtx);
        return invokeCtx.getResult();
    }

    private JRequest createRequest(String methodName, Object[] args) {
        MessageWrapper message = new MessageWrapper(metadata);
        message.setAppName(appName);
        message.setMethodName(methodName);
        // 不需要方法参数类型, 服务端会根据args具体类型按照JLS规则动态dispatch
        message.setArgs(args);

        setTraceId(message);

        JRequest request = new JRequest();
        request.message(message);

        return request;
    }

    private void setTraceId(MessageWrapper message) {
        if (TracingUtil.isTracingNeeded()) {
            TraceId traceId = TracingUtil.getCurrent();
            if (traceId == TraceId.NULL_TRACE_ID) {
                traceId = TraceId.newInstance(TracingUtil.generateTraceId());
            }
            message.setTraceId(traceId);
        }
    }

    static class Context implements JFilterContext {

        private final ClusterInvoker invoker;
        private final Class<?> returnType;
        private final boolean sync;

        private Object result;

        Context(ClusterInvoker invoker, Class<?> returnType, boolean sync) {
            this.invoker = invoker;
            this.returnType = returnType;
            this.sync = sync;
        }

        @Override
        public JFilter.Type getType() {
            return JFilter.Type.CONSUMER;
        }

        public ClusterInvoker getInvoker() {
            return invoker;
        }

        public Class<?> getReturnType() {
            return returnType;
        }

        public boolean isSync() {
            return sync;
        }

        public Object getResult() {
            return result;
        }

        public void setResult(Object result) {
            this.result = result;
        }
    }

    static class ClusterInvokeFilter implements JFilter {

        @Override
        public Type getType() {
            return JFilter.Type.CONSUMER;
        }

        @Override
        public <T extends JFilterContext> void doFilter(JRequest request, T filterCtx, JFilterChain next) throws Throwable {
            Context invokeCtx = (Context) filterCtx;
            ClusterInvoker invoker = invokeCtx.getInvoker();
            Class<?> returnType = invokeCtx.getReturnType();
            // invoke
            InvokeFuture<?> future = invoker.invoke(request, returnType);

            if (invokeCtx.isSync()) {
                //如果时同步的话，在这里阻塞，等待结果
                invokeCtx.setResult(future.getResult());//在一个线程中获取future的值。在另一个线程中在收到消息后，对future进行设置。完成异步过程。future模式主要就是两个线程同时持有一个对象。一个线程从对象中取值，另一个线程向对象中放值。
            } else {
                //如果是异步的话，直接放future返回过去。
                invokeCtx.setResult(future);
            }
        }
    }

    //因为这个Chains是AbstractInvoker的内部内，所以它提供的功能实用性也是很窄的，也就被AbstractInvocker和它的子类进行使用。
    //无论是AbstractInvoker还是它的子类，使用的chain中的filter，仅仅需要ClusterInvokerFilter，所以这个chaine实现也是写死的，
    static class Chains {

        private static final JFilterChain headChain;

        static {
            JFilterChain invokeChain = new DefaultFilterChain(new ClusterInvokeFilter(), null);
            headChain = JFilterLoader.loadExtFilters(invokeChain, JFilter.Type.CONSUMER);
        }

        static <T extends JFilterContext> T invoke(JRequest request, T invokeCtx) throws Throwable {
            headChain.doFilter(request, invokeCtx);
            return invokeCtx;
        }
    }
}
