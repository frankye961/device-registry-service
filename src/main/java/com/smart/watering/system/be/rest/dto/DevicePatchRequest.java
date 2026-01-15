package com.smart.watering.system.be.rest.dto;

import lombok.Data;
import java.util.List;

@Data
public class DevicePatchRequest {
    private String deviceName;
    private String model;
    private String fw;
    private List<String> tags;
}
