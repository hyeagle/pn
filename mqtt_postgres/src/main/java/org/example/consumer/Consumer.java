package org.example.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;

public class Consumer {
    private static MqttClient client;
    private static String broker;
    private static int port;
    private static String topic;
    private static String username;
    private static String password;
    private static String postgresHost;
    private static int postgresPort;
    private static String postgresDatabase;
    private static String postgresUsername;
    private static String postgresPassword;
    private static Connection postgresConn;
    private static ObjectMapper objectMapper = new ObjectMapper();

    public static void main(String[] args) {
        try {
            // 加载配置
            loadConfig();

            // 连接PostgreSQL
            connectPostgres();

            // 创建表
            createTable();

            // 初始化MQTT客户端
            initMqttClient();

            // 连接MQTT broker
            connect();

            // 订阅主题
            subscribe();

            // 保持运行
            System.out.println("Consumer started. Press Ctrl+C to stop.");
            Thread.currentThread().join();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // 断开连接
            disconnect();
            closePostgresConnection();
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

        // PostgreSQL配置（环境变量优先）
        postgresHost = getEnv("POSTGRES_HOST", config.getString("postgres.host"));
        postgresPort = getEnvInt("POSTGRES_PORT", config.getInt("postgres.port"));
        postgresDatabase = getEnv("POSTGRES_DATABASE", config.getString("postgres.database"));
        postgresUsername = getEnv("POSTGRES_USERNAME", config.getString("postgres.username"));
        postgresPassword = getEnv("POSTGRES_PASSWORD", config.getString("postgres.password"));
    }

    private static void connectPostgres() throws SQLException {
        String url = "jdbc:postgresql://" + postgresHost + ":" + postgresPort + "/" + postgresDatabase;
        postgresConn = DriverManager.getConnection(url, postgresUsername, postgresPassword);
        System.out.println("Connected to PostgreSQL database");
    }

    private static void createTable() throws SQLException {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS mqtt_messages (" +
                "id SERIAL PRIMARY KEY, " +
                "timestamp TEXT NOT NULL, " +
                "message TEXT NOT NULL, " +
                "value INTEGER NOT NULL, " +
                "received_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";

        try (Statement stmt = postgresConn.createStatement()) {
            stmt.execute(createTableSQL);
            System.out.println("Table created or already exists");
        }
    }

    private static void initMqttClient() throws MqttException {
        String clientId = "consumer-client-" + System.currentTimeMillis();
        MemoryPersistence persistence = new MemoryPersistence();
        client = new MqttClient("tcp://" + broker + ":" + port, clientId, persistence);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(false);
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
                // 处理接收到的消息
                String payload = new String(message.getPayload());
                System.out.println("Received message: " + payload);

                // 解析JSON
                Map<String, Object> messageMap = objectMapper.readValue(payload, Map.class);

                // 存储到数据库
                storeMessage(messageMap);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                // 消费者不需要处理发送完成事件
            }
        });
    }

    private static void connect() throws MqttException {
        client.connect();
        System.out.println("Connected to MQTT Broker: " + broker + ":" + port);
    }

    private static void subscribe() throws MqttException {
        client.subscribe(topic, 1);
        System.out.println("Subscribed to topic: " + topic);
    }

    private static void storeMessage(Map<String, Object> messageMap) throws SQLException {
        String insertSQL = "INSERT INTO mqtt_messages (timestamp, message, value) VALUES (?, ?, ?)";

        try (PreparedStatement pstmt = postgresConn.prepareStatement(insertSQL)) {
            pstmt.setString(1, (String) messageMap.get("timestamp"));
            pstmt.setString(2, (String) messageMap.get("message"));
            pstmt.setInt(3, ((Number) messageMap.get("value")).intValue());
            pstmt.executeUpdate();
            System.out.println("Message stored in database");
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

    private static void closePostgresConnection() {
        try {
            if (postgresConn != null && !postgresConn.isClosed()) {
                postgresConn.close();
                System.out.println("PostgreSQL connection closed");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}