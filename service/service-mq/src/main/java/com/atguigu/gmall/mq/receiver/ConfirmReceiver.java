package com.atguigu.gmall.mq.receiver;

import com.rabbitmq.client.Channel;
import lombok.SneakyThrows;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;


/**
 * @author DuanYang
 * @create 2020-06-28 16:19
 */
@Component
@Configuration
public class ConfirmReceiver {

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "queue.confirm", autoDelete = "false"),
            exchange = @Exchange(value = "exchange.confirm", autoDelete = "true"),
            key = {"routing.confirm"}))
    @SneakyThrows
    public void process(Message message, Channel channel) {
        System.out.println("RabbitListener:\t" + new String(message.getBody()));

        try {
            //制造异常
            int i = 1 / 0;
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
        } catch (Exception e) {
            System.out.println("出现异常 = " + e.getMessage());
            if (message.getMessageProperties().getRedelivered()) {
                System.out.println("异常已处理，拒接接收");
                channel.basicReject(message.getMessageProperties().getDeliveryTag(), false);
            } else {
                System.out.println("消息即将再次返回队列处理");
                // 参数二：是否批量， 参数三：为是否重新回到队列，true重新入队
                channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
            }
        }
    }
}
