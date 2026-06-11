package sensor.kircheis.top.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.fluent.Configurations;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Getter
@AllArgsConstructor
public class AppConfig {

    // MQTT
    private final String mqttBroker;
    private final int mqttPort;
    private final String mqttClientId;
    private final String mqttTopic;
    private final String mqttControlTopic;
    private final String mqttUsername;
    private final String mqttPassword;

    // thread
    private final int threadCount;

    @Configuration
    public static class Loader {
        @Bean
        public AppConfig appConfig() throws Exception {
            Configurations configs = new Configurations();
            PropertiesConfiguration config = configs.properties("application.properties");

            return new AppConfig(
                    env("MQTT_BROKER", config.getString("mqtt.broker")),
                    envInt("MQTT_PORT", config.getInt("mqtt.port")),
                    env("MQTT_CLIENT_ID", config.getString("mqtt.client-id", "consumer-client")),
                    env("MQTT_TOPIC", config.getString("mqtt.topic")),
                    env("MQTT_CONTROL_TOPIC", config.getString("mqtt.control-topic", "mga/sub")),
                    env("MQTT_USERNAME", config.getString("mqtt.username")),
                    env("MQTT_PASSWORD", config.getString("mqtt.password")),
                    envInt("THREAD_COUNT", config.getInt("thread.count", 1))
            );
        }

        private static String env(String key, String defaultValue) {
            String value = System.getenv(key);
            return (value != null && !value.isEmpty()) ? value : defaultValue;
        }

        private static int envInt(String key, int defaultValue) {
            String value = System.getenv(key);
            return (value != null && !value.isEmpty()) ? Integer.parseInt(value) : defaultValue;
        }
    }
}
