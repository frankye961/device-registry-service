package com.smart.watering.system.be.enums;

import com.smart.watering.model.DeviceStatus;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public enum StatusEnum {
    DISCOVERED("DISCOVERED"),

    ACTIVE("ACTIVE"),

    SUSPENDED("SUSPENDED"),

    REVOKED("REVOKED");


    private final String value;

    public static StatusEnum fromValue(String value) {
        for (StatusEnum b : StatusEnum.values()) {
            if (b.value.equals(value)) {
                return b;
            }
        }
        throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
}
