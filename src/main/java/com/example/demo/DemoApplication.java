package com.example.demo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.example.demo.mapper")
@SpringBootApplication
public class DemoApplication {

    /**
     * Spring Boot 项目启动入口。
     * 运行这个方法后，Spring 会创建容器、加载配置、扫描 Bean，并启动内置 Web 服务器。
     */
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }

}
