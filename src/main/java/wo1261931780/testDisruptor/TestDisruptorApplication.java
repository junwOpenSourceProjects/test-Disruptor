package wo1261931780.testDisruptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import wo1261931780.testDisruptor.server.NettyServerStarter;

/**
 * @author 64234
 */
@Slf4j
@SpringBootApplication
public class TestDisruptorApplication {

    public static void main(String[] args) {
        SpringApplication.run(TestDisruptorApplication.class, args);
        // Spring Boot启动后，启动Netty服务器
        log.info("Spring Boot启动完成，开始启动Netty服务器...");
        NettyServerStarter.startServer();
    }
}
