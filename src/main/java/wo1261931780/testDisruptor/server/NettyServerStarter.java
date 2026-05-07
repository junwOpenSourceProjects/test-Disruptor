package wo1261931780.testDisruptor.server;

import lombok.extern.slf4j.Slf4j;
import wo1261931780.testDisruptor.service.DisruptorMqService;
import wo1261931780.testDisruptor.service.Impl.DisruptorMqServiceImpl;
import wo1261931780.testDisruptor.config.BeanManager;

/**
 * Netty服务器启动器
 * 在Spring Boot启动后运行
 */
@Slf4j
public class NettyServerStarter {

    public static void startServer() {
        try {
            // 获取Disruptor服务
            DisruptorMqService disruptorMqService = BeanManager.getBean(DisruptorMqServiceImpl.class);
            NettyServerHandler.setDisruptorMqService(disruptorMqService);

            // 启动Netty服务器
            log.info("启动Netty服务器...");
            NettyServer.start();
        } catch (InterruptedException e) {
            log.error("Netty服务器启动失败：{}", e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
