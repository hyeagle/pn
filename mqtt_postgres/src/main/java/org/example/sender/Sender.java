package org.example.sender;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class Sender {
    private static MqttClient client;
    private static String broker;
    private static int port;
    private static String topic;
    private static String username;
    private static String password;
    private static ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        try {
            // 加载配置
            loadConfig();

            // 初始化MQTT客户端
            initMqttClient();

            // 连接MQTT broker
            connect();

            // 发送消息
            sendMessages();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 断开连接
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

        // MQTT配置（环境变量优先）
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
                System.out.println("Connection lost: " + cause.getMessage());
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                // 发送者不需要处理消息
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                System.out.println("Delivery complete");
            }
        });
    }

    private static void connect() throws MqttException {
        client.connect();
        System.out.println("Connected to MQTT Broker: " + broker + ":" + port);
    }

    private static void sendMessages() throws Exception {
        while (true) {
            for (int i = 0; i < 100; i++) {
                // 生成消息
                Map<String, Object> messageMap = new HashMap<>();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
                messageMap.put("timestamp", sdf.format(new Date()));
                messageMap.put("message", "Hello from MQTT sender");
                messageMap.put("value", System.currentTimeMillis() % 100);

                // 转换为JSON
                String jsonMessage = objectMapper.writeValueAsString(messageMap);

                // 创建MQTT消息
                MqttMessage message = new MqttMessage(jsonMessage.getBytes());
                message.setQos(1);
                message.setRetained(false);

                // 发布消息
                client.publish(topic, message);
                System.out.println("Sent message: " + jsonMessage);
            }

            // 等待1秒
            TimeUnit.SECONDS.sleep(1);
        }
    }

    private static void disconnect() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                System.out.println("Disconnected from MQTT Broker");
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }
}