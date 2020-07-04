package com.atguigu.gmall.user.service.impl;

import com.atguigu.gmall.model.user.UserInfo;
import com.atguigu.gmall.user.mapper.UserInfoMapper;
import com.atguigu.gmall.user.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

/**
 * @author DuanYang
 * @create 2020-06-22 14:46
 */
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserInfoMapper userInfoMapper;

    /**
     * 登录
     */
    @Override
    public UserInfo login(UserInfo userInfo) {
        QueryWrapper<UserInfo> wrapper = new QueryWrapper<>();
        wrapper.eq("login_name",userInfo.getLoginName());
        String md5Pwd = DigestUtils.md5DigestAsHex(userInfo.getPasswd().getBytes());
        wrapper.eq("passwd",md5Pwd);
        UserInfo info = userInfoMapper.selectOne(wrapper);
        if (null!=info){
            return info;
        }else {
            return null;
        }

    }
}
