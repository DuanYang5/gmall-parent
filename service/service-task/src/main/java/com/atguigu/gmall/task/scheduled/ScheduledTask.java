package com.atguigu.gmall.task.scheduled;

import com.atguigu.gmall.common.constant.MqConst;
import com.atguigu.gmall.common.service.RabbitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**编写定时任务
 * @author DuanYang
 * @create 2020-07-03 10:16
 */
//开启定时任务
@EnableScheduling
@Component
public class ScheduledTask {
    @Autowired
    private RabbitService rabbitService;

    //分 时 日 月 周
    //每天凌晨1点 0 0 1 * * ?
    //每隔30秒发送定时消息
    @Scheduled(cron = "0/30 * * * * ?")
    public void taskActivity(){
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_TASK,MqConst.ROUTING_TASK_1,"");

    }
    @Scheduled(cron = "0 0 18 * * ?")
    public void task18(){
        rabbitService.sendMessage(MqConst.EXCHANGE_DIRECT_TASK,MqConst.ROUTING_TASK_18,"");
    }

}
