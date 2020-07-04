package com.atguigu.gmall.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

/**
 * @author DuanYang
 * @create 2020-07-04 14:48
 */
@Configuration
public class KeyResolverConfig {
    @Bean
    public KeyResolver ipKeyResolver(){
        //使用ip限流
        System.out.println("ip限流————————————————————————————");
        return exchange -> Mono.just(exchange.getRequest().getRemoteAddress().getHostName());
    }

    // @Bean
    // public KeyResolver userKeyResolver() {
    //    return exchange -> Mono.just(exchange.getRequest().getHeaders().get("token").get(0));
    // }
    //
    // @Bean
    // public KeyResolver apiKeyResolver() {
    //    return exchange -> Mono.just(exchange.getRequest().getPath().value());
    // }
}
