package com.smart.watering.system.be.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceHealthDTO {
    private BatteryLevel batteryLevel;
    private Connectivity connectivity;
}
