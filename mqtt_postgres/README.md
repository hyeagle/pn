# MQTT to PostgreSQL

MQTT 消息消费与持久化服务。Sender 定时向 MQTT Broker 发送 JSON 消息，Consumer 订阅主题并将消息存储到 PostgreSQL。

## 环境要求

- JDK 8+
- Maven 3.6+
- Docker（用于容器化部署）
- MQTT Broker（如 EMQX、Mosquitto）
- PostgreSQL

## 配置说明

配置文件位于 `src/main/resources/application.properties`：

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| mqtt.broker | MQTT Broker 地址 | localhost |
| mqtt.port | MQTT Broker 端口 | 1883 |
| mqtt.topic | 订阅/发布的主题 | test/topic |
| mqtt.username | MQTT 用户名 | admin |
| mqtt.password | MQTT 密码 | 1qaz@WSX |
| postgres.host | PostgreSQL 地址 | localhost |
| postgres.port | PostgreSQL 端口 | 5432 |
| postgres.database | 数据库名 | mydb |
| postgres.username | 数据库用户名 | postgres |
| postgres.password | 数据库密码 | 123456 |

## 本地运行

### 1. 修改配置

编辑 `src/main/resources/application.properties`，填入实际的 MQTT 和 PostgreSQL 连接信息。

### 2. 编译打包

```bash
mvn clean package -DskipTests
```

### 3. 运行 Consumer

```bash
java -cp target/mqtt_postgres.jar org.example.consumer.Consumer
```

> Consumer 和 Sender 需要分别在不同的终端窗口运行。

## Docker 部署

### 1. 构建镜像

```bash
docker build -f Dockerfile -t mqtt-consumer .
```

### 2. 运行容器

```bash
docker run -d --name consumer mqtt-consumer
```

### 3. 自定义配置

在 Docker 环境中，默认配置文件无法直接使用。有以下几种方式处理：

**方式一：挂载配置文件**

准备一个修改好地址的 `application.properties`，运行时挂载到 jar 同级目录：

```bash
docker run -d \
  -v /path/to/application.properties:/app/application.properties:ro \
  --name consumer \
  mqtt-consumer
```

**方式二：使用环境变量（推荐）**

通过 `-e` 参数传入环境变量，优先级高于配置文件，未设置的环境变量自动回退到默认值：

```bash
# 启动 Consumer
docker run -d --name consumer \
  -e MQTT_BROKER=your-mqtt-host \
  -e MQTT_PORT=1883 \
  -e MQTT_TOPIC=test/topic \
  -e MQTT_USERNAME=admin \
  -e MQTT_PASSWORD=1qaz@WSX \
  -e POSTGRES_HOST=your-postgres-host \
  -e POSTGRES_PORT=5432 \
  -e POSTGRES_DATABASE=mydb \
  -e POSTGRES_USERNAME=postgres \
  -e POSTGRES_PASSWORD=123456 \
  mqtt-consumer
```

支持的环境变量：

| 环境变量 | 说明 | 默认值 |
|----------|------|--------|
| MQTT_BROKER | MQTT Broker 地址 | localhost |
| MQTT_PORT | MQTT Broker 端口 | 1883 |
| MQTT_TOPIC | 订阅/发布的主题 | test/topic |
| MQTT_USERNAME | MQTT 用户名 | admin |
| MQTT_PASSWORD | MQTT 密码 | 1qaz@WSX |
| POSTGRES_HOST | PostgreSQL 地址 | localhost |
| POSTGRES_PORT | PostgreSQL 端口 | 5432 |
| POSTGRES_DATABASE | 数据库名 | mydb |
| POSTGRES_USERNAME | 数据库用户名 | postgres |
| POSTGRES_PASSWORD | 数据库密码 | 123456 |

## 数据库表结构

Consumer 启动时会自动创建 `mqtt_messages` 表：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | SERIAL | 自增主键 |
| timestamp | TEXT | 消息时间戳 |
| message | TEXT | 消息内容 |
| value | INTEGER | 消息数值 |
| received_at | TIMESTAMP | 接收时间（自动生成） |

## 消息格式

Sender 发送的 JSON 格式：

```json
{
  "timestamp": "2026-04-09T12:00:00.000",
  "message": "Hello from MQTT sender",
  "value": 42
}
```
