package com.smart.watering.system.be.rest.dto;

import com.smart.watering.model.DeviceStatus;
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
public class DeviceDTO {
    private String deviceId;
    private String deviceName;
    private String model;
    private String fw;

    private DeviceStatus status;
    private List<String> tags;

    private DeviceTelemetrySnapshotDTO telemetry;

    private Instant createdAt;
    private Instant updatedAt;

    /**
     * Used for ETag / optimistic locking.
     * If you don’t persist a version, you can derive it from updatedAt, but a numeric version is cleaner.
     */
    private long version;
}