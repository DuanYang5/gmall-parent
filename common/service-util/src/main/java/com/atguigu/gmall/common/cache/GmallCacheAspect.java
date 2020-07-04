package com.atguigu.gmall.common.cache;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.RedisConst;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * @author DuanYang
 * @create 2020-06-16 16:19
 */
@Component
@Aspect
public class GmallCacheAspect {
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;

    @Around("@annotation(com.atguigu.gmall.common.cache.GmallCache)")
    public Object cacheAroundAdvice(ProceedingJoinPoint point) throws Throwable {
        Object result = null;
        //获取传递的参数
        Object[] args = point.getArgs();
        //获取方法上的注解
        MethodSignature signature = (MethodSignature) point.getSignature();
        GmallCache gmallCache = signature.getMethod().getAnnotation(GmallCache.class);
        //获取注解的value
        String prefix = gmallCache.prefix();

        String key = prefix + Arrays.asList(args).toString();
        //获取缓存中返回的数据
        result = cacheHint(signature,key);

        if (result!=null){
            return result;
        }
        //缓存为空
        RLock lock = redissonClient.getLock(key+":lock");
        try {
            boolean res = lock.tryLock(RedisConst.SKULOCK_EXPIRE_PX2, RedisConst.SKULOCK_EXPIRE_PX1, TimeUnit.SECONDS);
            if (res){
                //查询数据库中数据
                //proceed 调用方法体 ，point.getArgs()获取当前对象的形参参数
                result = point.proceed(point.getArgs());
                if (result==null){
                    Object o = new Object();
                    redisTemplate.opsForValue().set(key, JSON.toJSONString(o),RedisConst.SKUKEY_TEMPORARY_TIMEOUT,TimeUnit.SECONDS);
                    return o;
                }
                redisTemplate.opsForValue().set(key,JSON.toJSONString(result),RedisConst.SKUKEY_TIMEOUT,TimeUnit.SECONDS);
                return result;
            }else {
                Thread.sleep(1000);
                return cacheHint(signature,key);
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            lock.unlock();
        }
        return result;
    }

    private Object cacheHint(MethodSignature signature, String key) {
        //根据key获取缓存数据
        String cache = (String) redisTemplate.opsForValue().get(key);
        if (StringUtils.isNotEmpty(cache)){
            //获取返回值数据类型
            Class returnType = signature.getReturnType();
            return JSON.parseObject(cache,returnType);
        }
        return null;
    }
}
