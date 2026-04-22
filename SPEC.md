# 项目规格说明书 - test-Disruptor

## 基本信息

| 项目名 | test-Disruptor |
|--------|---------------|
| 路径 | /Users/junw/Documents/GitHub/test-Disruptor |
| 简介 | Disruptor 高并发队列技术测试项目 |
| 技术栈 | Java 17 + Spring Boot 3.1.5 + Disruptor 3.4.4 + Lombok |
| License | MIT |

## 功能概述

- Disruptor 高性能队列功能演示
- 生产者-消费者模型实现
- 事件驱动架构
- 高并发消息处理

## 项目结构

```
src/main/java/wo1261931780/testDisruptor/
├── TestDisruptorApplication.java  # 启动类
├── config/
│   ├── MQManager.java             # 消息队列管理器
│   ├── BeanManager.java           # Bean 管理器
│   └── HelloEventHandler.java     # 事件处理器
├── model/
│   └── MessageModel.java          # 消息模型
├── service/
│   ├── DisruptorMqService.java    # 服务接口
│   └── Impl/
│       └── DisruptorMqServiceImpl.java  # 服务实现
└── factory/
    └── HelloEventFactory.java     # 事件工厂
```

## 核心组件

| 组件 | 说明 |
|------|------|
| Event (MessageModel) | 事件/消息载体 |
| EventFactory (HelloEventFactory) | 事件工厂 |
| EventHandler (HelloEventHandler) | 事件处理器 |
| Disruptor | 核心协调器 |

## 编译问题

**编译问题：Lombok @Slf4j 生成的 log 变量及 @Setter 生成的 setter 方法在编译时丢失**

错误信息示例：
```
cannot find symbol: variable log
cannot find symbol: method setMessage(java.lang.String)
```

原因分析：
- Lombok 注解处理器未正常工作
- @Slf4j 生成的 log 变量编译时不存在
- @Setter 生成的 setter 方法未被生成
- 可能原因：Lombok 版本与 JDK 不兼容 / annotation processor 配置缺失

建议解决方案：
1. 升级 Lombok 至最新稳定版
2. 在 pom.xml 中配置 maven-compiler-plugin 的 annotationProcessorPaths
3. 执行 `mvn clean compile` 清理重编译
4. 确认 IDE 安装了对应版本的 Lombok 插件

## Disruptor 性能优势

| 特性 | 传统阻塞队列 | Disruptor |
|------|-------------|-----------|
| 吞吐量 | ~500万/s | ~600万/s |
| 延迟 | 微秒级 | 纳秒级 |
| 锁机制 | 加锁 | 无锁（CAS） |
| 内存分配 | 频繁GC | 环形缓冲区复用 |

## 应用场景

- 金融交易系统
- 高频交易处理
- 日志收集系统
- 消息推送服务
- 实时数据分析

## 环境要求

- JDK 17+
- Maven 3.6+
