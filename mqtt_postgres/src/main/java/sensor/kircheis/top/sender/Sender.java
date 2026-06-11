package sensor.kircheis.top.sender;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import sensor.kircheis.top.dto.SensorData;
import sensor.kircheis.top.dto.SensorMessage;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
public class Sender {
    private static MqttClient client;
    private static String broker;
    private static int port;
    private static String topic;
    private static String username;
    private static String password;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        try {
            loadConfig();
            initMqttClient();
            connect();
            sendMessages();
        } catch (Exception e) {
            log.error("Sender failed", e);
        } finally {
            disconnect();
        }
    }

    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    private static int getEnvInt(String key, int defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isEmpty()) ? Integer.parseInt(value) : defaultValue;
    }

    private static void loadConfig() throws Exception {
        Configurations configs = new Configurations();
        PropertiesConfiguration config = configs.properties("application.properties");

        broker = getEnv("MQTT_BROKER", config.getString("mqtt.broker"));
        port = getEnvInt("MQTT_PORT", config.getInt("mqtt.port"));
        topic = getEnv("MQTT_TOPIC", config.getString("mqtt.topic"));
        username = getEnv("MQTT_USERNAME", config.getString("mqtt.username"));
        password = getEnv("MQTT_PASSWORD", config.getString("mqtt.password"));
    }

    private static void initMqttClient() throws MqttException {
        String clientId = "sender-" + System.currentTimeMillis();
        MemoryPersistence persistence = new MemoryPersistence();
        client = new MqttClient("tcp://" + broker + ":" + port, clientId, persistence);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setUserName(username);
        options.setPassword(password.toCharArray());
        options.setConnectionTimeout(10);
        options.setKeepAliveInterval(20);

        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                log.warn("Connection lost: {}", cause.getMessage());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                log.debug("Delivery complete");
            }
        });
    }

    private static void connect() throws MqttException {
        client.connect();
        log.info("Connected to MQTT Broker: {}:{}", broker, port);
    }

    private static void sendMessages() throws Exception {
        while (true) {
            List<SensorData> dataList = new ArrayList<>();
            dataList.add(new SensorData("sensor_001", "temperature", 25.6 + Math.random() * 10, "C"));
            dataList.add(new SensorData("sensor_002", "humidity", 50.0 + Math.random() * 20, "%"));

            SensorMessage sensorMessage = new SensorMessage(
                    "device_001", System.currentTimeMillis() / 1000, 20, new BigDecimal("1.1"), new BigDecimal("1.1"), dataList);

            String jsonMessage = objectMapper.writeValueAsString(sensorMessage);

            MqttMessage message = new MqttMessage(jsonMessage.getBytes());
            message.setQos(1);
            message.setRetained(false);

            client.publish(topic, message);
            log.debug("Sent message: {}", jsonMessage);

            TimeUnit.SECONDS.sleep(1);
        }
    }

    private static void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                log.info("Disconnected from MQTT Broker");
            }
        } catch (MqttException e) {
            log.error("Failed to disconnect from MQTT Broker", e);
        }
    }
}
