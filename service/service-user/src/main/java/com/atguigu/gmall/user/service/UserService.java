package com.atguigu.gmall.user.service;

import com.atguigu.gmall.model.user.UserInfo;

/**
 * @author DuanYang
 * @create 2020-06-22 14:44
 */
public interface UserService {
    /**
     * 登录
     */
    UserInfo login(UserInfo userInfo);
}
