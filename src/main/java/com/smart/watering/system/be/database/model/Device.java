package com.smart.watering.system.be.database.model;

import com.smart.watering.system.be.enums.StatusEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Date;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Document(value = "device")
public class Device {

    @Id
    private String deviceId;
    private String deviceName;
    private String model;
    private String fw;
    private Integer batteryMv;
    private Integer rssi;
    private Instant lastSeen;
    private StatusEnum status;
}
