package com.smart.watering.system.be.rest.dto;

import com.smart.watering.model.DeviceStatus;
import lombok.Data;

@Data
public class DeviceStatusUpdateRequest {
    private DeviceStatus status;
    private String reason;
}