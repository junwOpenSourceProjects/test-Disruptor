package wo1261931780.testDisruptor.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import lombok.extern.slf4j.Slf4j;
import wo1261931780.testDisruptor.service.DisruptorMqService;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Netty业务处理器 - 集成Disruptor
 * 将Netty接收到的消息通过Disruptor队列处理
 */
@Slf4j
public class NettyServerHandler extends SimpleChannelInboundHandler<String> {

    private static DisruptorMqService disruptorMqService;
    private static final AtomicLong totalRequests = new AtomicLong(0);

    public static void setDisruptorMqService(DisruptorMqService service) {
        disruptorMqService = service;
    }

    public static long getTotalRequests() {
        return totalRequests.get();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        totalRequests.incrementAndGet();
        log.debug("收到消息：{}", msg);

        // 通过Disruptor处理消息
        if (disruptorMqService != null) {
            disruptorMqService.sayHelloMq(msg);
        }

        // 发送响应
        String response = "Echo: " + msg;
        ctx.writeAndFlush(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("连接异常：{}", cause.getMessage());
        ctx.close();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("新连接：{}", ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        log.info("连接断开：{}", ctx.channel().remoteAddress());
    }
}
