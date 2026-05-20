package dev.langchain4j.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 应用启动入口。Spring Boot 自动配置 MyBatis、LangChain4j 等组件。
 */
@SpringBootApplication
public class CustomerSupportAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerSupportAgentApplication.class, args);
    }
}
