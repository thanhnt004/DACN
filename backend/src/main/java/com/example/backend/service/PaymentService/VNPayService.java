package com.example.backend.service.PaymentService;

import com.example.backend.config.VNPayConfig;
import com.example.backend.excepton.BadRequestException;
import com.example.backend.model.order.Order;
import com.example.backend.model.payment.Payment;
import com.example.backend.util.VNPayUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class VNPayService {

    private final VNPayConfig vnPayConfig;

    @Value("${payment.vnPay.max_time}")
    private int maxPaymentTime; // in second

    private final String SUCCESS_CODE = "00";

    public String createPaymentUrl(Order order,
                                   HttpServletRequest request){
        List<Payment> payments = order.getPayments();
        Payment payment = payments.stream().filter(pm -> pm.getStatus() == Payment.PaymentStatus.PENDING).toList().getFirst();

        LocalDateTime expiredDate = payment.getExpireAt();
        Calendar now = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        LocalDateTime currentTime =LocalDateTime.now();
        if(currentTime.isAfter(expiredDate)){
            payment.setStatus(Payment.PaymentStatus.FAILED);
            order.setStatus(Order.OrderStatus.CANCELLED);
            throw new BadRequestException("Payment expired");
        }
        Map<String, String> params = vnPayConfig.getConfig();
        long diffInMillis = expiredDate.getMinute() - currentTime.getMinute();
        int remainingSeconds = (int) TimeUnit.MILLISECONDS.toSeconds(diffInMillis);
        int allowedTime = Math.min(remainingSeconds, maxPaymentTime);
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        String vnp_CreateDate = formatter.format(now.getTime());
        params.put("vnp_CreateDate", vnp_CreateDate);
        now.add(Calendar.SECOND, allowedTime);
        String vnp_ExpireDate = formatter.format(now.getTime());
        params.put("vnp_ExpireDate", vnp_ExpireDate);
        params.put("vnp_Amount", String.valueOf(payment.getAmount() * 100L));
        String ref = order.getId() + "-" + System.currentTimeMillis();
        params.put("vnp_TxnRef", ref);
        params.put("vnp_OrderInfo", "Thanh toan don hang: " + order.getId());
        String ipAddr = VNPayUtil.getIpAddress(request);
        params.put("vnp_IpAddr", ipAddr);
        String queryString = VNPayUtil.createPaymentUrl(params, true);
        String hashData = VNPayUtil.createPaymentUrl(params, false);
        String vnpSecureHash = VNPayUtil.hmacSHA512(vnPayConfig.getSecretKey(), hashData);
        queryString += "&vnp_SecureHash=" + vnpSecureHash;
        return vnPayConfig.getVnp_PayUrl() + "?" + queryString;
    }

    public boolean checkPayment(Order order, Map<String, String> params) {
        String code = params.get("vnp_ResponseCode");
        if(code.equals(SUCCESS_CODE)){
            long amount = Long.parseLong(params.get("vnp_Amount")) / 100L;
            Payment payment = order.getPayments().stream().filter(payment1 -> payment1.getStatus().equals(Payment.PaymentStatus.PENDING)).toList().getFirst();
            return amount == payment.getAmount();
        }
        else return false;
    }
}

