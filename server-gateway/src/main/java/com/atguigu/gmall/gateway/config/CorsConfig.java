package com.atguigu.gmall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * @author DuanYang
 * @create 2020-06-10 11:14
 */
@Configuration //变成xml
public class CorsConfig {
    @Bean
    public CorsWebFilter corsWebFilter(){
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        //设置跨域属性
        corsConfiguration.addAllowedOrigin("*");
        corsConfiguration.setAllowCredentials(true);//将证书设置成true 是否可以从服务器中获取cookie
        //表示允许所有请求方法
        corsConfiguration.addAllowedMethod("*");
        //允许任何请求头信息
        corsConfiguration.addAllowedHeader("*");
        //创建source
        UrlBasedCorsConfigurationSource urlBasedCorsConfigurationSource = new UrlBasedCorsConfigurationSource();
        urlBasedCorsConfigurationSource.registerCorsConfiguration("/**",corsConfiguration);
        return new CorsWebFilter(urlBasedCorsConfigurationSource);
    }
}
