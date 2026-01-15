package com.smart.watering.system.be.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceTelemetrySnapshotDTO {
    private Integer batteryMv;
    private Integer rssi;
    private Instant lastSeen;

    private DeviceHealthDTO health;

    // optional convenience notes (derived)
    private List<String> notes;
}
