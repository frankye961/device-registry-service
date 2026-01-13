package com.smart.watering.system.be;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.watering.model.*;
import com.smart.watering.system.be.event.TelemetryReadingEvent;
import com.smart.watering.system.be.service.DeviceRegistryService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TelemetryReadingEventTest {

    private static final String DLQ_BINDING = "telemetryDlq-out-0";

    @Test
    void ingestTelemetryData_validJson_emitsOutboundMessage_withKeyHeader() throws Exception {
        ObjectMapper om = new ObjectMapper();
        DeviceRegistryService service = mock(DeviceRegistryService.class);
        StreamBridge streamBridge = mock(StreamBridge.class);

        TelemetryReadingEvent sut = new TelemetryReadingEvent(om, service, streamBridge);

        String deviceId = "b43a45349704";
        IoTPlantEvent inbound = sampleInboundEvent(deviceId);
        String json = om.writeValueAsString(inbound);

        Message<String> inboundMsg = MessageBuilder.withPayload(json)
                .setHeader("mqttTopic", "test/topic")
                .build();

        DeviceRegistrySnapshotEvent snapshot = sampleSnapshot(deviceId);
        when(service.elaborateDeviceInfo(any(IoTPlantEvent.class))).thenReturn(Mono.just(snapshot));

        StepVerifier.create(sut.ingestTelemetryData().apply(Flux.just(inboundMsg)))
                .assertNext(out -> {
                    assertTrue(out.getPayload() instanceof DeviceRegistrySnapshotEvent);
                    DeviceRegistrySnapshotEvent payload = (DeviceRegistrySnapshotEvent) out.getPayload();
                    assertEquals(deviceId, payload.getDevice().getDeviceId());

                    assertEquals(deviceId, out.getHeaders().get(KafkaHeaders.KEY));
                })
                .verifyComplete();

        verify(streamBridge, never()).send(anyString(), any());
        verify(service, times(1)).elaborateDeviceInfo(any(IoTPlantEvent.class));
    }

    @Test
    void ingestTelemetryData_serviceReturnsEmpty_emitsNothing() throws Exception {
        ObjectMapper om = new ObjectMapper();
        DeviceRegistryService service = mock(DeviceRegistryService.class);
        StreamBridge streamBridge = mock(StreamBridge.class);

        TelemetryReadingEvent sut = new TelemetryReadingEvent(om, service, streamBridge);

        String deviceId = "b43a45349704";
        String json = om.writeValueAsString(sampleInboundEvent(deviceId));

        when(service.elaborateDeviceInfo(any(IoTPlantEvent.class))).thenReturn(Mono.empty());

        StepVerifier.create(sut.ingestTelemetryData().apply(Flux.just(MessageBuilder.withPayload(json).build())))
                .verifyComplete();

        verify(streamBridge, never()).send(anyString(), any());
        verify(service, times(1)).elaborateDeviceInfo(any(IoTPlantEvent.class));
    }

    @Test
    void ingestTelemetryData_parseError_sendsToDlq_andEmitsNothing() {
        ObjectMapper om = new ObjectMapper();
        DeviceRegistryService service = mock(DeviceRegistryService.class);
        StreamBridge streamBridge = mock(StreamBridge.class);

        when(streamBridge.send(anyString(), any())).thenReturn(true);

        TelemetryReadingEvent sut = new TelemetryReadingEvent(om, service, streamBridge);

        String badJson = "{ NOT_JSON";
        Message<String> inboundMsg = MessageBuilder.withPayload(badJson).build();

        StepVerifier.create(sut.ingestTelemetryData().apply(Flux.just(inboundMsg)))
                .verifyComplete();

        verify(service, never()).elaborateDeviceInfo(any());

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(streamBridge, times(1)).send(eq(DLQ_BINDING), captor.capture());

        String raw = extractRawPayloadFromDlqSent(captor.getValue());
        assertEquals(badJson, raw);
    }

    @Test
    void ingestTelemetryData_serviceError_sendsToDlq_andEmitsNothing() throws Exception {
        ObjectMapper om = new ObjectMapper();
        DeviceRegistryService service = mock(DeviceRegistryService.class);
        StreamBridge streamBridge = mock(StreamBridge.class);

        when(streamBridge.send(anyString(), any())).thenReturn(true);

        TelemetryReadingEvent sut = new TelemetryReadingEvent(om, service, streamBridge);

        String deviceId = "b43a45349704";
        String json = om.writeValueAsString(sampleInboundEvent(deviceId));

        when(service.elaborateDeviceInfo(any(IoTPlantEvent.class)))
                .thenReturn(Mono.error(new RuntimeException("boom-service")));

        Message<String> inboundMsg = MessageBuilder.withPayload(json).build();

        StepVerifier.create(sut.ingestTelemetryData().apply(Flux.just(inboundMsg)))
                .verifyComplete();

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(streamBridge, times(1)).send(eq(DLQ_BINDING), captor.capture());

        String raw = extractRawPayloadFromDlqSent(captor.getValue());
        assertEquals(json, raw);
    }

    // -------------------------
    // Helpers
    // -------------------------

    private static IoTPlantEvent sampleInboundEvent(String deviceId) {
        IoTPlantEvent e = new IoTPlantEvent();
        e.setType(IoTPlantEvent.TypeEnum.IOT_PLANT_EVENT);
        e.setVersion(1);
        e.setMessageId(UUID.randomUUID().toString());
        e.setCorrelationId(null);
        e.setTs(Date.from(Instant.now()));

        DeviceMeta d = new DeviceMeta();
        d.setDeviceId(deviceId);
        d.setDeviceName("Balcony Arduino");
        d.setModel("Arduino UNO R4 WiFi");
        d.setFw("1.0.3");
        d.setBatteryMv(4914);
        d.setRssi(-66);

        e.setDevice(d);
        return e;
    }

    private static DeviceRegistrySnapshotEvent sampleSnapshot(String deviceId) {
        DeviceRegistrySnapshotEvent ev = new DeviceRegistrySnapshotEvent();
        ev.setType(DeviceRegistrySnapshotEvent.TypeEnum.DEVICE_REGISTRY_SNAPSHOT);
        ev.setVersion(1);
        ev.setMessageId(UUID.randomUUID().toString());
        ev.setCorrelationId(null);
        ev.setTs(Date.from(Instant.now()));
        ev.setStatus(DeviceStatus.valueOf("ACTIVE"));

        ev.setDevice(DeviceRegistryDevice.builder()
                .deviceId(deviceId)
                .deviceName("Balcony Arduino")
                .model("Arduino UNO R4 WiFi")
                .fw("1.0.3")
                .build());

        return ev;
    }

    private static String extractRawPayloadFromDlqSent(Object sent) {
        // Case A: you send a Message<?> to StreamBridge (current implementation)
        if (sent instanceof Message<?> m) {
            Object payload = m.getPayload();

            // If you sent the inbound Message<String>, payload itself is a Message
            if (payload instanceof Message<?> innerMsg) {
                Object innerPayload = innerMsg.getPayload();
                if (innerPayload instanceof String s) return s;
                fail("Inner DLQ message payload is not String: " + innerPayload.getClass());
            }

            // Or you sent the raw payload directly as Message<String>
            if (payload instanceof String s) return s;

            fail("DLQ message payload is neither Message nor String: " + payload.getClass());
        }

        // Case B: you sent raw payload directly (possible with another overload)
        if (sent instanceof String s) return s;

        fail("Unexpected DLQ sent type: " + sent.getClass());
        return null;
    }
}
