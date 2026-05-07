package wo1261931780.testDisruptor.server;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Netty + Disruptor 压力测试客户端
 * 10 线程并发，测试 3 秒内的吞吐量
 */
@Slf4j
public class NettyPressureTestClient {

    private static final String SERVER_HOST = "127.0.0.1";
    private static final int SERVER_PORT = 8888;
    private static final int THREAD_COUNT = 10;
    private static final int TEST_DURATION_SECONDS = 3;

    private static final AtomicInteger totalRequests = new AtomicInteger(0);
    private static final AtomicInteger totalResponses = new AtomicInteger(0);
    private static final AtomicLong totalRT = new AtomicLong(0);

    public static void main(String[] args) throws Exception {
        log.info("========================================");
        log.info("Netty + Disruptor 压力测试开始");
        log.info("测试时长：{} 秒", TEST_DURATION_SECONDS);
        log.info("并发线程数：{}", THREAD_COUNT);
        log.info("========================================");

        EventLoopGroup group = new NioEventLoopGroup(THREAD_COUNT);
        CountDownLatch latch = new CountDownLatch(THREAD_COUNT);
        Channel[] channels = new Channel[THREAD_COUNT];
        long[] startTimes = new long[THREAD_COUNT];
        long[] endTimes = new long[THREAD_COUNT];

        // 预建连接
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline().addLast(new LengthFieldBasedFrameDecoder(1024 * 1024, 0, 4, 0, 4));
                            ch.pipeline().addLast(new LengthFieldPrepender(4));
                            ch.pipeline().addLast(new StringDecoder(CharsetUtil.UTF_8));
                            ch.pipeline().addLast(new StringEncoder(CharsetUtil.UTF_8));
                            ch.pipeline().addLast(new RespHandler(threadId));
                        }
                    });
            ChannelFuture f = bootstrap.connect(SERVER_HOST, SERVER_PORT).sync();
            channels[i] = f.channel();
        }

        // 等待连接就绪
        Thread.sleep(500);

        // 启动压测线程
        for (int i = 0; i < THREAD_COUNT; i++) {
            final int threadId = i;
            final Channel channel = channels[i];
            new Thread(() -> {
                startTimes[threadId] = System.currentTimeMillis();
                long endTime = startTimes[threadId] + TEST_DURATION_SECONDS * 1000L;
                int localCount = 0;
                try {
                    while (System.currentTimeMillis() < endTime) {
                        String msg = "PressureTest-" + threadId + "-" + System.currentTimeMillis();
                        long reqStart = System.nanoTime();
                        channel.writeAndFlush(msg);
                        totalRequests.incrementAndGet();
                        localCount++;
                        // 无延迟全速发送，测 Disruptor 接收能力
                        Thread.sleep(1);
                    }
                } catch (Exception e) {
                    log.error("线程 #{} 异常", threadId, e);
                }
                endTimes[threadId] = System.currentTimeMillis();
                log.info("线程 #{} 发送了 {} 个请求", threadId, localCount);
                latch.countDown();
            }).start();
        }

        latch.await();

        // 等一小段时间让 Disruptor 消费完
        Thread.sleep(2000);

        // 关闭连接
        for (Channel ch : channels) {
            ch.close();
        }
        group.shutdownGracefully();

        long totalTime = 0;
        for (int i = 0; i < THREAD_COUNT; i++) {
            totalTime = Math.max(totalTime, endTimes[i] - startTimes[i]);
        }
        long actualRequests = totalRequests.get();
        int responses = totalResponses.get();
        long avgRT = responses > 0 ? totalRT.get() / responses : 0;

        log.info("========================================");
        log.info("压力测试完成！");
        log.info("总发送请求数：{}", actualRequests);
        log.info("总耗时：{} ms", totalTime);
        log.info("QPS（发送）：{}", actualRequests * 1000.0 / totalTime);
        log.info("收到响应数：{}", responses);
        if (responses > 0) {
            log.info("平均响应时间：{} us", avgRT / 1000);
        }
        log.info("========================================");

        System.exit(0);
    }

    private static class RespHandler extends ChannelInboundHandlerAdapter {
        private final int threadId;

        RespHandler(int threadId) {
            this.threadId = threadId;
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) {
            totalResponses.incrementAndGet();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            ctx.close();
        }
    }
}
