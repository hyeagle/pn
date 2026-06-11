package sensor.kircheis.top.mqtt;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.stereotype.Component;
import sensor.kircheis.top.config.AppConfig;

@Slf4j
@Component
@RequiredArgsConstructor
public class MqttPublisher {

    private final AppConfig config;
    private MqttClient client;

    @PostConstruct
    public void connect() {
        try {
            String clientId = config.getMqttClientId() + "-pub";
            String brokerUrl = "tcp://" + config.getMqttBroker() + ":" + config.getMqttPort();
            client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setUserName(config.getMqttUsername());
            options.setPassword(config.getMqttPassword().toCharArray());
            options.setConnectionTimeout(10);
            options.setKeepAliveInterval(20);
            options.setAutomaticReconnect(true);

            client.connect(options);
            log.info("MQTT Publisher connected to {}", brokerUrl);
        } catch (MqttException e) {
            log.error("Failed to connect MQTT Publisher: {}", e.getMessage(), e);
        }
    }

    /**
     * 向控制主题发布消息
     */
    public boolean publish(String payload) {
        try {
            if (client == null || !client.isConnected()) {
                log.warn("MQTT Publisher not connected, cannot publish");
                return false;
            }
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(1);
            client.publish(config.getMqttControlTopic(), message);
            log.info("Published to {} : {}", config.getMqttControlTopic(), payload);
            return true;
        } catch (MqttException e) {
            log.error("Failed to publish MQTT message: {}", e.getMessage(), e);
            return false;
        }
    }

    @PreDestroy
    public void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                log.info("MQTT Publisher disconnected");
            }
        } catch (MqttException e) {
            log.error("Failed to disconnect MQTT Publisher", e);
        }
    }
}
