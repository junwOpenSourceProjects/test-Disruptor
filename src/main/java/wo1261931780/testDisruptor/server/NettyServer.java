package wo1261931780.testDisruptor.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;
import lombok.extern.slf4j.Slf4j;

/**
 * Netty服务器 - 基于Reactor模型
 * 对比传统IO，Netty的Reactor模型：
 * - Boss Group: 1-2线程，负责接收连接
 * - Worker Group: N线程，负责处理IO事件
 */
@Slf4j
public class NettyServer {

    private static final int PORT = 8888;

    public static void start() throws InterruptedException {
        // 1. 创建Boss和Worker线程组
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            // 2. 配置服务器
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();

                            // 3.1 解决TCP粘包/拆包
                            pipeline.addLast(new LengthFieldBasedFrameDecoder(
                                    1024 * 1024, 0, 4, 0, 4));
                            pipeline.addLast(new LengthFieldPrepender(4));

                            // 3.2 字符串编解码
                            pipeline.addLast(new StringDecoder(CharsetUtil.UTF_8));
                            pipeline.addLast(new StringEncoder(CharsetUtil.UTF_8));

                            // 3.3 业务处理器
                            pipeline.addLast(new NettyServerHandler());
                        }
                    });

            // 4. 绑定端口并启动
            ChannelFuture future = bootstrap.bind(PORT).sync();
            log.info("Netty服务器启动成功，监听端口：{}", PORT);

            // 5. 等待服务器关闭
            future.channel().closeFuture().sync();
        } finally {
            // 6. 优雅关闭
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
