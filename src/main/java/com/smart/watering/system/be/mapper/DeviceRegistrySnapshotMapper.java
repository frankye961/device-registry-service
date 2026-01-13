package com.smart.watering.system.be.mapper;

import com.smart.watering.model.*;
import org.mapstruct.*;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Mapper
public interface DeviceRegistrySnapshotMapper {

    @Mapping(target = "type", constant = "DEVICE_REGISTRY_SNAPSHOT")
    @Mapping(target = "version", constant = "1")

    @Mapping(target = "messageId", expression = "java(messageId)")
    @Mapping(target = "ts", expression = "java(publishedAt)")

    @Mapping(target = "device", source = "event.device")
    @Mapping(target = "status", expression = "java(status)")
    @Mapping(target = "telemetryMeta", source = "event")
    @Mapping(target = "tags", expression = "java(tagsFrom(event))")

    // If your outbound schema still has "source", ignore it:
    @Mapping(target = "source", ignore = true)
    DeviceRegistrySnapshotEvent toSnapshot(
            IoTPlantEvent event,
            @Context DeviceStatus status,
            @Context String messageId,
            @Context Date publishedAt
    );

    // Nested mappings
    DeviceRegistryDevice mapDevice(DeviceMeta device);

    @Mapping(target = "lastSeenAt", source = "ts")
    @Mapping(target = "batteryMv", source = "device.batteryMv")
    @Mapping(target = "rssi", source = "device.rssi")
    DeviceTelemetryMeta mapTelemetryMeta(IoTPlantEvent event);

    // Tags (optional)
    default Map<String, String> tagsFrom(IoTPlantEvent event) {
        if (event.getZone() == null || event.getZone().getZoneId() == null) {
            return Map.of(); // results in {}
        }
        return Map.of("zoneHint", event.getZone().getZoneId());
    }
}
