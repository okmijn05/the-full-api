package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling   // 이게 꼭 있어야 스케줄러가 돌음
public class TheFullApiApplication extends SpringBootServletInitializer{
	
	 @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(TheFullApiApplication.class);
    }
	
	public static void main(String[] args) {
		SpringApplication.run(TheFullApiApplication.class, args);
	}

}
