package ru.outofrange.as2.config;

import org.apache.camel.CamelContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.annotation.PostConstruct;

@Configuration
@PropertySource({"classpath:application.properties"})
@ComponentScan(basePackages = {"ru.outofrange.as2", "ru.outofrange.cert"})
public class CamelConfig {

    @Autowired
    CamelContext camelContext;

    //@Value("${camelServerPath}")
    //String contextPath;

    @PostConstruct
    public void afterConstruct() {
    }


}
