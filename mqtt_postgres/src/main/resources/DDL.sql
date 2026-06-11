-- 1. 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    user_id VARCHAR(50) PRIMARY KEY,
    username VARCHAR(50) NOT NULL,
    password VARCHAR(100) NOT NULL,
    create_time BIGINT DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000
);

-- 2. 设备表（含电量快照）
CREATE TABLE IF NOT EXISTS device_info (
    device_id VARCHAR(50) PRIMARY KEY,
    user_id VARCHAR(50) NOT NULL,
    device_name VARCHAR(100) NOT NULL,
    relay BOOLEAN DEFAULT false,
    power BOOLEAN DEFAULT false,
    battery INT,
    update_time BIGINT DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000
);

-- 3. 传感器元数据表
CREATE TABLE IF NOT EXISTS sensor_config (
    sensor_id VARCHAR(50) PRIMARY KEY,
    device_id VARCHAR(50) NOT NULL,
    metric VARCHAR(50) NOT NULL,
    metric_name VARCHAR(50) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    create_time BIGINT DEFAULT EXTRACT(EPOCH FROM NOW()) * 1000
);

-- 4. 建父表
CREATE TABLE sensor_data (
    id BIGINT NOT NULL DEFAULT nextval('sensor_data_id_seq'),
    device_id VARCHAR(50) NOT NULL,
    sensor_id VARCHAR(50) NOT NULL,
    metric VARCHAR(50) NOT NULL,
    value NUMERIC(10,2) NOT NULL,
    ts BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT now()
) PARTITION BY RANGE (ts);

-- 5. 共享序列
CREATE SEQUENCE IF NOT EXISTS sensor_data_id_seq;

-- 6. 分表模板（替换 YYYYMMDD 为实际日期，如 20260410）
CREATE TABLE IF NOT EXISTS sensor_data_YYYYMMDD (
    id BIGSERIAL PRIMARY KEY,
    device_id VARCHAR(50) NOT NULL,
    sensor_id VARCHAR(50) NOT NULL,
    metric VARCHAR(50) NOT NULL,
    value NUMERIC(10,2) NOT NULL,
    ts BIGINT NOT NULL,
    create_time TIMESTAMP DEFAULT now()
);

-- 索引优化（适配大数据量查询）
CREATE INDEX IF NOT EXISTS idx_device_metric_ts_YYYYMMDD ON sensor_data_YYYYMMDD (device_id, metric, ts);
CREATE INDEX IF NOT EXISTS idx_sensor_ts_YYYYMMDD ON sensor_data_YYYYMMDD (sensor_id, ts);

-- 7. 把现有表转为分区并绑定
-- 6月9号的分区，边界值用秒级时间戳
ALTER TABLE sensor_data ATTACH PARTITION sensor_data_20260609
  FOR VALUES FROM (1749499200) TO (1749585600);