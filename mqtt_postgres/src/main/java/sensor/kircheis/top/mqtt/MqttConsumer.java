package sensor.kircheis.top.mqtt;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.stereotype.Component;
import sensor.kircheis.top.config.AppConfig;
import sensor.kircheis.top.service.SensorDataWriterService;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MqttConsumer {
    private final AppConfig config;
    private final SensorDataWriterService sensorDataWriterService;
    private MqttClient client;
    private ExecutorService executor;

    @PostConstruct
    public void connect() {
        try {
            doConnect();
        } catch (MqttException e) {
            throw new RuntimeException("Failed to connect to MQTT broker", e);
        }
    }

    private void doConnect() throws MqttException {
        String clientId = config.getMqttClientId();
        String brokerUrl = "tcp://" + config.getMqttBroker() + ":" + config.getMqttPort();
        MemoryPersistence persistence = new MemoryPersistence();
        client = new MqttClient(brokerUrl, clientId, persistence);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setUserName(config.getMqttUsername());
        options.setPassword(config.getMqttPassword().toCharArray());
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(20);
        options.setAutomaticReconnect(true);

        executor = Executors.newFixedThreadPool(1, r -> {
            Thread t = new Thread(r, "mqtt-msg-handler");
            t.setDaemon(true);
            return t;
        });

        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                log.warn("Connection lost: {} , auto-reconnect will retry", cause.getMessage());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                String payload = new String(message.getPayload());
                log.debug("Received message: {}", payload);
                executor.submit(() -> {
                    try {
                        sensorDataWriterService.save(payload);
                    } catch (Exception e) {
                        log.error("Failed to save message: {}", e.getMessage(), e);
                    }
                });
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
            }
        });

        client.connect(options);
        log.info("Connected to MQTT Broker: {}", brokerUrl);

        client.subscribe(config.getMqttTopic(), 1);
        log.info("Subscribed to topic: {}", config.getMqttTopic());
    }

    @PreDestroy
    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                log.info("Disconnected from MQTT Broker");
            }
        } catch (MqttException e) {
            log.error("Failed to disconnect from MQTT Broker", e);
        } finally {
            if (executor != null) {
                executor.shutdownNow();
            }
        }
    }
}
