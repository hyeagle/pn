# Sensor Consumer

IoT 传感器数据消费与持久化服务。订阅 MQTT Broker 上行消息，将传感器数据写入 PostgreSQL 按日分区表，同时提供设备管理的 REST API 和控制指令下行能力。

## 核心功能

- **MQTT 消费**：订阅 `mga/pub` 主题，接收传感器上报数据
- **数据持久化**：自动创建 PostgreSQL 按日分区表（`sensor_data_yyyyMMdd`），批量写入传感器数据
- **设备管理**：提供 REST API 进行设备 CRUD、分页查询，以及 relay / power 控制指令下发
- **控制下行**：通过 `mga/sub` 主题将控制指令发布到 MQTT Broker，下发至设备
- **定时任务**：每天 0 点自动创建次日的分区表和索引
- **数据模拟**：`Sender` 可以独立运行，模拟传感器设备每秒发送一条消息

## 技术栈

- Java 17 + Spring Boot 2.7 + JdbcTemplate
- Eclipse Paho MQTT 客户端
- PostgreSQL 15（原生分区表 + HikariCP 连接池）
- Docker 多阶段构建

## 消息格式

```json
{
  "did": "device_001",
  "ts": 1749499200,
  "ba": 20,
  "lat": 1.1,
  "lon": 1.1,
  "dt": [
    { "sid": "sensor_001", "mtc": "temperature", "v": 25.6, "u": "C" },
    { "sid": "sensor_002", "mtc": "humidity", "v": 50.0, "u": "%" }
  ]
}
```

## 数据库表结构

启动时自动创建以下表：

| 表名 | 说明 |
|------|------|
| `sensor_data` | 传感器数据父表（按 ts 做 RANGE 分区） |
| `sensor_data_yyyyMMdd` | 按日分区表，每天 0 点自动创建次日分区 |
| `device_info` | 设备信息表 |
| `sensor_config` | 传感器配置表 |
| `sys_user` | 用户表 |

## REST API

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/devices?page=1&size=10` | 分页查询设备 |
| POST | `/api/devices` | 添加设备 |
| PUT | `/api/devices/{deviceId}` | 更新设备 |
| POST | `/api/devices/{deviceId}/control` | 下发控制指令 |

## 构建 Docker 镜像

```bash
docker build -t kircheis/sensor:latest .
```

多阶段构建：先通过 Maven 编译打包，再将 jar 复制到 JRE 镜像中运行，非 root 用户启动。

## Docker Compose 一键部署

```bash
docker-compose up -d
```

包含完整服务栈：EMQX (MQTT Broker)、PostgreSQL、Grafana、本服务。

