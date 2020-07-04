package com.atguigu.gmall.payment.controller;

import com.alipay.api.AlipayApiException;
import com.alipay.api.internal.util.AlipaySignature;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.enums.PaymentStatus;
import com.atguigu.gmall.model.enums.PaymentType;
import com.atguigu.gmall.model.payment.PaymentInfo;
import com.atguigu.gmall.payment.config.AlipayConfig;
import com.atguigu.gmall.payment.service.AlipayService;
import com.atguigu.gmall.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author DuanYang
 * @create 2020-06-29 16:55
 */
@Controller
@RequestMapping("/api/payment/alipay")
public class AlipayController {

    @Autowired
    private AlipayService alipayService;
    @Autowired
    private PaymentService paymentService;

    @RequestMapping("submit/{orderId}")
    @ResponseBody
    public String submitAlipay(@PathVariable("orderId")Long orderId){
        String form = null;
        try {
            form = alipayService.alipay(orderId);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }

        return form ;
    }
    //同步回调
    @RequestMapping("callback/return")
    public String callbackReturn(){
        //重定向到展示订单页面
        return "redirect:"+ AlipayConfig.return_order_url;
    }

    //异步回调
    @RequestMapping("callback/notify")
    @ResponseBody
    public String callbackNotify(@RequestParam Map<String, String> paramsMap){
        String tradeStatus = paramsMap.get("trade_status");
        String outTradeNo = paramsMap.get("out_trade_no");
        String appId = paramsMap.get("app_id");
        String totalAmount = paramsMap.get("total_amount");
        boolean signVerified = false;
        try {
            //调用SDK验证签名
            signVerified = AlipaySignature.rsaCheckV1(paramsMap, AlipayConfig.alipay_public_key, AlipayConfig.charset, AlipayConfig.sign_type);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        if(signVerified){
            // TODO 验签成功后，按照支付结果异步通知中的描述，对支付结果中的业务内容进行二次校验，校验成功后在response中返回success并继续商户自身业务处理，校验失败返回failure
            if ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus)){
                //验签 out_trade_no相同  订单状态必须是未支付
                PaymentInfo paymentInfo = paymentService.getPaymentInfo(outTradeNo, PaymentType.ALIPAY.name());
                if (paymentInfo.getPaymentStatus().equals(PaymentStatus.PAID.name()) ||
                        paymentInfo.getPaymentStatus().equals(PaymentStatus.ClOSED.name())){
                    return "failure";
                }
                // total_amount相等
                if (!paramsMap.get("total_amount").equals(paymentInfo.getTotalAmount().toString())){
                    return "failure";
                }
                //支付成功 修改数据库
                paymentService.paySuccess(outTradeNo,PaymentType.ALIPAY.name(),paramsMap);
                return "success";
            }
        }else{
            // TODO 验签失败则记录异常日志，并在response中返回failure.
            return "failure";
        }
        return "failure";
    }


    //退款
    //http://localhost:8205/api/payment/alipay/refund/20
    @RequestMapping("refund/{orderId}")
    @ResponseBody
    public Result refund(@PathVariable("orderId")Long orderId){
        boolean flag = alipayService.refund(orderId);
        return Result.ok(flag);
    }

    // 根据订单Id关闭订单
    @GetMapping("closePay/{orderId}")
    @ResponseBody
    public boolean closePay(@PathVariable("orderId")Long orderId){
        return alipayService.closePay(orderId);
    }

    // 查看是否有交易记录
    @RequestMapping("checkPayment/{orderId}")
    @ResponseBody
    public Boolean checkPayment(@PathVariable Long orderId){
        return alipayService.checkPayment(orderId);
    }

    @GetMapping("getPaymentInfo/{outTradeNo}")
    @ResponseBody
    public PaymentInfo getPaymentInfo(@PathVariable String outTradeNo){
        return paymentService.getPaymentInfo(outTradeNo, PaymentType.ALIPAY.name());
    }
}
