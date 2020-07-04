package com.atguigu.gmall.user.client;

import com.atguigu.gmall.model.user.UserAddress;
import com.atguigu.gmall.user.client.impl.UserDegradeFeignClient;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * @author DuanYang
 * @create 2020-06-27 15:06
 */
@FeignClient(name = "service-user",fallback = UserDegradeFeignClient.class)
public interface UserFeignClient {
    /**
     * 根据用户Id 查询用户的收货地址列表
     */
    @GetMapping("api/user/inner/findUserAddressListByUserId/{userId}")
    List<UserAddress> findUserAddressListByUserId(@PathVariable("userId")String userId);
}
