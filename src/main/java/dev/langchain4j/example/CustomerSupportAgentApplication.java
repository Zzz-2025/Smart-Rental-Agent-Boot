package dev.langchain4j.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * =========================== 应用启动入口 ===========================
 *
 * 整个项目的"开关"——运行这个 main 方法，Spring Boot 就会自动启动：
 *   1. 内嵌 Tomcat Web 服务器（默认端口 8080）
 *   2. MySQL 数据库连接池（HikariCP）
 *   3. Redis 连接（Redisson，用于分布式锁和限流）
 *   4. LangChain4j AI 引擎（连接大语言模型）
 *
 * 然后用浏览器打开 http://localhost:8080 就能看到客服界面了。
 *
 * @SpringBootApplication 是一个组合注解，等于同时开启了：
 *   - @Configuration   （允许定义 Bean）
 *   - @EnableAutoConfiguration（根据依赖自动配置）
 *   - @ComponentScan   （扫描当前包及子包，发现 @Component/@Service 等）
 */
@SpringBootApplication
public class CustomerSupportAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerSupportAgentApplication.class, args);
    }
}
