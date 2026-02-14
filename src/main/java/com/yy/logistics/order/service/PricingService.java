package com.yy.logistics.order.service;

import com.yy.logistics.order.dto.OrderCreateRequest;
import com.yy.logistics.order.dto.PricingQuoteRequest;
import com.yy.logistics.order.dto.PricingQuoteResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class PricingService {

    private static final BigDecimal VOLUME_DIVISOR = new BigDecimal("6000");
    private static final BigDecimal BASE_WEIGHT = new BigDecimal("1.00");
    private static final BigDecimal BASE_FEE = new BigDecimal("12.00");
    private static final BigDecimal STEP_WEIGHT = new BigDecimal("0.50");
    private static final BigDecimal STEP_FEE = new BigDecimal("2.00");
    private static final BigDecimal EXPRESS_FEE = new BigDecimal("6.00");
    private static final BigDecimal REMOTE_FEE = new BigDecimal("8.00");
    private static final BigDecimal INSURED_RATE = new BigDecimal("0.005");
    private static final BigDecimal MIN_INSURED_FEE = new BigDecimal("1.00");

    private static final List<String> REMOTE_KEYWORDS = List.of(
            "西藏", "新疆", "青海", "内蒙古", "甘肃", "宁夏"
    );

    public PricingQuoteResponse quote(PricingQuoteRequest request) {
        return calculate(
                request.receiverAddr(),
                request.weight(),
                request.volume(),
                request.serviceType(),
                request.insuredAmount()
        );
    }

    public PricingQuoteResponse quoteForOrder(OrderCreateRequest request) {
        return calculate(
                request.receiverAddr(),
                request.weight(),
                request.volume(),
                request.serviceType(),
                request.insuredAmount()
        );
    }

    private PricingQuoteResponse calculate(
            String receiverAddr,
            BigDecimal weight,
            BigDecimal volume,
            Integer serviceType,
            BigDecimal insuredAmount
    ) {
        BigDecimal actualWeight = normalize(weight);
        BigDecimal volumeWeight = normalize(volume)
                .divide(VOLUME_DIVISOR, 3, RoundingMode.HALF_UP);
        BigDecimal chargeWeight = roundUpToHalf(actualWeight.max(volumeWeight));

        BigDecimal continueFee = BigDecimal.ZERO;
        if (chargeWeight.compareTo(BASE_WEIGHT) > 0) {
            BigDecimal extraWeight = chargeWeight.subtract(BASE_WEIGHT);
            long steps = extraWeight
                    .divide(STEP_WEIGHT, 0, RoundingMode.CEILING)
                    .longValue();
            continueFee = STEP_FEE.multiply(BigDecimal.valueOf(steps));
        }

        BigDecimal serviceFee = (serviceType != null && serviceType == 2) ? EXPRESS_FEE : BigDecimal.ZERO;
        BigDecimal remoteFee = isRemote(receiverAddr) ? REMOTE_FEE : BigDecimal.ZERO;

        BigDecimal insuredFee = BigDecimal.ZERO;
        if (insuredAmount != null && insuredAmount.compareTo(BigDecimal.ZERO) > 0) {
            insuredFee = insuredAmount.multiply(INSURED_RATE).setScale(2, RoundingMode.HALF_UP);
            if (insuredFee.compareTo(MIN_INSURED_FEE) < 0) {
                insuredFee = MIN_INSURED_FEE;
            }
        }

        BigDecimal total = BASE_FEE
                .add(continueFee)
                .add(serviceFee)
                .add(remoteFee)
                .add(insuredFee)
                .setScale(2, RoundingMode.HALF_UP);

        String ruleDesc = "计费规则：首重1kg/12元，续重每0.5kg/2元；体积重=体积/6000；加急+6元；偏远地区+8元；保价费率0.5%。";

        return new PricingQuoteResponse(
                actualWeight.setScale(2, RoundingMode.HALF_UP),
                volumeWeight.setScale(2, RoundingMode.HALF_UP),
                chargeWeight.setScale(2, RoundingMode.HALF_UP),
                BASE_FEE,
                continueFee.setScale(2, RoundingMode.HALF_UP),
                serviceFee.setScale(2, RoundingMode.HALF_UP),
                remoteFee.setScale(2, RoundingMode.HALF_UP),
                insuredFee.setScale(2, RoundingMode.HALF_UP),
                total,
                ruleDesc
        );
    }

    private BigDecimal normalize(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value.max(BigDecimal.ZERO);
    }

    private boolean isRemote(String receiverAddr) {
        if (receiverAddr == null) {
            return false;
        }
        return REMOTE_KEYWORDS.stream().anyMatch(receiverAddr::contains);
    }

    private BigDecimal roundUpToHalf(BigDecimal value) {
        BigDecimal stepCount = value.divide(STEP_WEIGHT, 0, RoundingMode.CEILING);
        return stepCount.multiply(STEP_WEIGHT);
    }
}
