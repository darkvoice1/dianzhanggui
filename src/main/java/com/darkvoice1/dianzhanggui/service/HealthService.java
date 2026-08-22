package com.darkvoice1.dianzhanggui.service;

import org.springframework.stereotype.Service;

/** 提供应用健康状态相关的业务能力。 */
@Service
public class HealthService {

    /** 获取当前应用的健康状态。 */
    public HealthStatus getStatus() {
        // 当前最小工程只返回应用自身已启动的状态。
        return new HealthStatus("UP");
    }

    /** 表示应用健康状态的数据对象。 */
    public record HealthStatus(String status) {
    }
}
