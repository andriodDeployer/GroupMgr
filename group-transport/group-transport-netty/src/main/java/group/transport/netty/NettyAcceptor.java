
package group.transport.netty;


import group.common.concurrent.NamedThreadFactory;
import group.common.util.JConstants;
import group.transport.JAcceptor;
import group.transport.JConfig;
import group.transport.JOption;
import group.transport.netty.estimator.JMessageSizeEstimator;
import group.transport.processor.Processor;
import group.transport.processor.ProviderProcessor;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.util.HashedWheelTimer;
import io.netty.util.concurrent.DefaultThreadFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.concurrent.ThreadFactory;

/**
 * jupiter
 * org.jupiter.transport.netty
 *
 * @author jiachun.fjc
 */
public abstract class NettyAcceptor implements JAcceptor {

    protected final Protocol protocol;
    protected final SocketAddress localAddress;

    protected final HashedWheelTimer timer = new HashedWheelTimer(new NamedThreadFactory("acceptor.timer", true));

    private final int nBosses;
    private final int nWorkers;

    private ServerBootstrap bootstrap;
    private EventLoopGroup boss;
    private EventLoopGroup worker;

    private ProviderProcessor processor_1;
    private Processor processor;

    public NettyAcceptor(Protocol protocol, SocketAddress localAddress) {
        this(protocol, localAddress, JConstants.AVAILABLE_PROCESSORS << 1);
    }

    public NettyAcceptor(Protocol protocol, SocketAddress localAddress, int nWorkers) {
        this(protocol, localAddress, 1, nWorkers);
    }

    public NettyAcceptor(Protocol protocol, SocketAddress localAddress, int nBosses, int nWorkers) {
        this.protocol = protocol;
        this.localAddress = localAddress;
        this.nBosses = nBosses;
        this.nWorkers = nWorkers;
    }

    protected void init() {
        ThreadFactory bossFactory = bossThreadFactory("jupiter.acceptor.boss");//boos线程池
        ThreadFactory workerFactory = workerThreadFactory("jupiter.acceptor.worker");//worker线程池
        boss = initEventLoopGroup(nBosses, bossFactory);//创建serverBootStrap的eventloopgroup
        worker = initEventLoopGroup(nWorkers, workerFactory);//创建worker的eventloopgroup

        bootstrap = new ServerBootstrap().group(boss, worker);

        // parent options
        JConfig parent = configGroup().parent();//将ration的值设置到parentconfig的ration实例属性上，就是初始化parentconfig的ration属性
        parent.setOption(JOption.IO_RATIO, 100);

        // child options
        JConfig child = configGroup().child();//将字ration设置上。
        child.setOption(JOption.IO_RATIO, 100);
    }

    @Override
    public Protocol protocol() {
        return protocol;
    }

    @Override
    public SocketAddress localAddress() {
        return localAddress;
    }

    @Override
    public int boundPort() {
        if (!(localAddress instanceof InetSocketAddress)) {
            throw new UnsupportedOperationException("Unsupported address type to get port");
        }
        return ((InetSocketAddress) localAddress).getPort();
    }

   // @Override
//    public ProviderProcessor processor() {
//        return processor;
//    }


    @Override
    public Processor processor() {
        return processor;
    }

    @Override
    public void withProcessor(ProviderProcessor processor) {
        setProcessor(this.processor_1 = processor);
    }

    @Override
    public void withProcessor(Processor processor) {
        setProcessor(this.processor = processor);
    }

    @Override
    public void shutdownGracefully() {
        boss.shutdownGracefully().syncUninterruptibly();
        worker.shutdownGracefully().syncUninterruptibly();
        timer.stop();
        if (processor != null) {
            processor.shutdown();
        }
    }

    protected ThreadFactory bossThreadFactory(String name) {
        return new DefaultThreadFactory(name, Thread.MAX_PRIORITY);
    }

    protected ThreadFactory workerThreadFactory(String name) {
        return new DefaultThreadFactory(name, Thread.MAX_PRIORITY);
    }

    protected void setOptions() {
        JConfig parent = configGroup().parent(); // parent options
        JConfig child = configGroup().child(); // child options

        setIoRatio(parent.getOption(JOption.IO_RATIO), child.getOption(JOption.IO_RATIO));

        bootstrap.childOption(ChannelOption.MESSAGE_SIZE_ESTIMATOR, JMessageSizeEstimator.DEFAULT);
    }

    /**
     * Which allows easy bootstrap of {@link io.netty.channel.ServerChannel}.
     */
    protected ServerBootstrap bootstrap() {
        return bootstrap;
    }

    /**
     * The {@link EventLoopGroup} which is used to handle all the events for the to-be-creates
     * {@link io.netty.channel.Channel}.
     */
    protected EventLoopGroup boss() {
        return boss;
    }

    /**
     * The {@link EventLoopGroup} for the child. These {@link EventLoopGroup}'s are used to
     * handle all the events and IO for {@link io.netty.channel.Channel}'s.
     */
    protected EventLoopGroup worker() {
        return worker;
    }

    /**
     * Sets provider's processor.
     */
    @SuppressWarnings("unused")
    protected void setProcessor(ProviderProcessor processor) {
        // the default implementation does nothing
    }
    protected void setProcessor(Processor processor) {
        // the default implementation does nothing
    }

    /**
     * Sets the percentage of the desired amount of time spent for I/O in the child event loops.
     * The default value is {@code 50}, which means the event loop will try to spend the same
     * amount of time for I/O as for non-I/O tasks.
     */
    public abstract void setIoRatio(int bossIoRatio, int workerIoRatio);

    /**
     * Create a new {@link io.netty.channel.Channel} and bind it.
     */
    protected abstract ChannelFuture bind(SocketAddress localAddress);

    /**
     * Create a new instance using the specified number of threads, the given {@link ThreadFactory}.
     */
    protected abstract EventLoopGroup initEventLoopGroup(int nThreads, ThreadFactory tFactory);

}
