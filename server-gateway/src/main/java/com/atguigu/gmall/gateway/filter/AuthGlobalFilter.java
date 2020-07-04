package com.atguigu.gmall.gateway.filter;

import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.common.result.ResultCodeEnum;
import com.atguigu.gmall.common.util.IpUtil;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author DuanYang
 * @create 2020-06-23 9:25
 */
@Component
public class AuthGlobalFilter implements GlobalFilter {

    private AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Value("${authUrls.url}")
    private String authUrls;

    @Autowired
    private RedisTemplate redisTemplate;
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        //获取请求对象
        ServerHttpRequest request = exchange.getRequest();
        //获取路径
        String path = request.getURI().getPath();
        //判断是否内部接口
        if (antPathMatcher.match("/**/inner/**",path)){
            //响应
            ServerHttpResponse response = exchange.getResponse();
            return out(response, ResultCodeEnum.PERMISSION);
        }
        //防止盗用token
        String userId = getUserId(request);
        String userTempId = getUserTempId(request);


        if ("-1".equals(userId)){
            ServerHttpResponse response = exchange.getResponse();
            return out(response, ResultCodeEnum.PERMISSION);
        }
        //用户登录认证
        if (antPathMatcher.match("/api/**/auth/**",path)){
            if (StringUtils.isEmpty(userId)){
                ServerHttpResponse response = exchange.getResponse();
                return out(response, ResultCodeEnum.LOGIN_AUTH);
            }
        }

        for (String authUrl : authUrls.split(",")){
            //用户访问页面包含拦截页面，但用户没有登录
            if (path.indexOf(authUrl)!=-1&&StringUtils.isEmpty(userId)){
                ServerHttpResponse response = exchange.getResponse();
                //发送状态：需重定向
                response.setStatusCode(HttpStatus.SEE_OTHER);
                response.getHeaders().set(HttpHeaders.LOCATION,"http://www.gmall.com/login.html?originUrl="+request.getURI());
                //返回设置项
                return response.setComplete();
            }
        }
        if(StringUtils.isNotEmpty(userId) || StringUtils.isNotEmpty(userTempId)) {
            if(StringUtils.isNotEmpty(userId)) {
                request.mutate().header("userId", userId).build();
            }
            if(StringUtils.isNotEmpty(userTempId)) {
                request.mutate().header("userTempId", userTempId).build();
            }
            //将现在的request 变成 exchange对象
            return chain.filter(exchange.mutate().request(request).build());
        }
        return chain.filter(exchange);
    }

    private String getUserTempId(ServerHttpRequest request){
        String userTempId = "";
        List<String> list = request.getHeaders().get("userTempId");
        if (null!=list){
            userTempId = list.get(0);
        }else {
            HttpCookie cookie = request.getCookies().getFirst("userTempId");
            if (null!=cookie){
                userTempId = URLDecoder.decode(cookie.getValue());
            }
        }
        return userTempId;
    }

    private String getUserId(ServerHttpRequest request) {
        //用户存储在缓存 格式 key=user:login:token
        //token 一个在cookie 一个在header
        String token = "";
        List<String> list = request.getHeaders().get("token");
        if (null!=list){
            //从header中获取token
            token = list.get(0);
        }else {
            //否则从cookie获取token
            HttpCookie cookie = request.getCookies().getFirst("token");
            if (null!=cookie){
                token = URLDecoder.decode(cookie.getValue());
            }
        }
        if (StringUtils.isNotEmpty(token)){
            String userKey = "user:login:" + token;
            String userJson = (String)redisTemplate.opsForValue().get(userKey);
            JSONObject jsonObject = JSONObject.parseObject(userJson);
            String ip = jsonObject.getString("ip");
            String curIp = IpUtil.getGatwayIpAddress(request);
            //校验ip地址是否相同，防止token盗用
            if (ip.equals(curIp)){
                return jsonObject.getString("userId");
            }else {
                return "-1";
            }
        }
        return null;
    }

    /**
     * 信息提示
     */
    private Mono<Void> out(ServerHttpResponse response, ResultCodeEnum resultCodeEnum) {
        Result<Object> result = Result.build(null, resultCodeEnum);
        byte[] bytes = JSONObject.toJSONString(result).getBytes(StandardCharsets.UTF_8);
        DataBuffer wrap = response.bufferFactory().wrap(bytes);

        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        return response.writeWith(Mono.just(wrap));
    }
}
