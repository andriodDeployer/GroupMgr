package group.im1.client;/**
 * Created by DELL on 2018/8/31.
 */

import group.common.util.Maps;
import group.common.util.internal.logging.InternalLogger;
import group.common.util.internal.logging.InternalLoggerFactory;
import group.im1.GResponse;
import group.transport.Status;
import group.transport.channel.JChannel;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * user is lwb
 **/


public class DefaultSendFuture {

    private static final InternalLogger logger = InternalLoggerFactory.getInstance(DefaultSendFuture.class);
    private static final long DEFAULT_TIMEOUT_NANOSECOND = TimeUnit.MILLISECONDS.toNanos(3 * 1000);
    private static final ConcurrentMap<Long, DefaultSendFuture> futures = Maps.newConcurrentMap();


    private final long requestId;
    private final long startTime = System.nanoTime();
    private final long timeout;
    private volatile boolean sent = false;
    private final JChannel channel;
    private MessingSendListener listener;


    public static DefaultSendFuture with(long requestId,JChannel channel){
        return with(requestId,channel,DEFAULT_TIMEOUT_NANOSECOND);
    }

    public static DefaultSendFuture with(long requestId,JChannel channel,long timeout){
        return new DefaultSendFuture(requestId,channel,timeout);
    }

    public void addListener(MessingSendListener listener){
        this.listener = listener;
    }

    private DefaultSendFuture(long requestId, JChannel channel, long timeout){
        this.requestId = requestId;
        this.timeout = timeout;
        this.channel = channel;
        futures.put(requestId,this);
    }

    public void markSent(){
        this.sent = true;
    }

    //收到服务端响应的时候调用，响应是服务端发送过来
    public static void received(JChannel jChannel, GResponse response){
        long id = response.responseId();
        DefaultSendFuture future = futures.remove(id);

        if(future == null){
            logger.warn("A timeout response [{}] finally return on {}",response,jChannel);
            return;
        }
        future.doReceived(jChannel,response);
    }

    /**
     * 对收到响应（可能是服务端返回的，可能是客户端自己创建的）进行处理
     * @param channel
     * @param response
     */
    private void doReceived(JChannel channel,GResponse response){
        byte status = response.status();
        if(status == Status.OK.value()){
            //说明，对端(这个对端只能服务端，不能是另一个客户端，因为在发送消息的时候，另一个客户端可能不在线)
            if(listener != null)
                listener.sendSuccessful();
        } else {
            handleException(channel,response);
        }
    }

    private void handleException(JChannel channel,GResponse response) {
        if(response.status() == Status.CLIENT_TIMEOUT.value()){
            logger.warn("exception is {}","client time out");
        }else if(response.status() == Status.SERVER_TIMEOUT.value()){
            logger.warn("exception is {}","server time out ");
        }

        if(listener != null)
            listener.sendFailure();
    }

    //客户端发送超时的时候调用，响应是客户端自己创建的
    public static void fakeReceived(JChannel channel,GResponse response){
        long responseId = response.responseId();
        DefaultSendFuture future = futures.remove(responseId);
        if(future == null){//在执行remove前，已经被删除过了，说经在千钧一发之际，收到了服务端的响应
            return;
        }
        future.doReceived(channel,response);
    }


    //定时删除
    private static class TimeoutScanner implements Runnable {

        private static final long TIME_OUT_INTERVAL_MILLIS = 100;

        @Override
        public void run() {
            for(;;){
                for(DefaultSendFuture future : futures.values()){
                    process(future);
                }

                try {
                    Thread.sleep(TIME_OUT_INTERVAL_MILLIS);
                } catch (InterruptedException e) {

                }
            }
        }

        private void process(DefaultSendFuture future) {
            //正在处理的过程中，被其他线程删除掉了，同时gc有将其回收掉
            if(future == null){
                return;
            }

            if(System.nanoTime() - future.startTime > future.timeout){
                //超时了
                GResponse response = new GResponse(future.requestId);
                response.status(future.sent ? Status.SERVER_TIMEOUT : Status.CLIENT_TIMEOUT);
                DefaultSendFuture.fakeReceived(future.channel,response);
            }
        }
    }


    static {
        Thread t = new Thread(new TimeoutScanner(),"time.scanner");
        t.setDaemon(true);
        t.start();
    }



}
