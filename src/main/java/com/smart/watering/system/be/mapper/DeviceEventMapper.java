package com.smart.watering.system.be.mapper;

import com.smart.watering.model.DeviceMeta;
import com.smart.watering.system.be.database.model.Device;
import com.smart.watering.system.be.rest.dto.DeviceDTO;
import com.smart.watering.system.be.rest.dto.DeviceTelemetrySnapshotDTO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mapper
public interface DeviceEventMapper {
    Device mapDeviceMetaToDevice(DeviceMeta device);

    @Mappings({
            @Mapping(target = "telemetry", expression = "java(mapTelemetry(device))"),
            @Mapping(target = "createdAt", ignore = true), // not present in Device
            @Mapping(target = "updatedAt", ignore = true), // not present in Device
            @Mapping(target = "version", ignore = true),   // not present in Device
            @Mapping(target = "tags", ignore = true)       // not present in Device
    })
    DeviceDTO mapDeviceToDeviceDTO(Device device);

    /**
     * Flat → nested telemetry mapping
     */
    default DeviceTelemetrySnapshotDTO mapTelemetry(Device device) {
        if (device == null) {
            return null;
        }

        return DeviceTelemetrySnapshotDTO.builder()
                .batteryMv(device.getBatteryMv())
                .rssi(device.getRssi())
                .lastSeen(device.getLastSeen() != null
                        ? device.getLastSeen().toInstant()
                        : null)
                .build();
    }
}
