package com.converter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * @author Evan
 */
@SpringBootApplication
public class ConverterApplication extends SpringBootServletInitializer {
    public static void main(final String[] args) {
        SpringApplication.run(ConverterApplication.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(final SpringApplicationBuilder builder) {
        return builder.sources(ConverterApplication.class);
    }
}
