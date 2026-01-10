package com.smart.watering.system.be.mapper;

import com.smart.watering.model.DeviceMeta;
import com.smart.watering.system.be.database.model.Device;
import org.mapstruct.Mapper;
import org.springframework.stereotype.Component;

@Mapper
public interface DeviceEventMapper {
    Device mapDeviceMetaToDevice(DeviceMeta device);
}
