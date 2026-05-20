-- ============================================
-- Miles of Smiles 数据库表结构定义
-- ============================================
CREATE TABLE IF NOT EXISTS vehicles (
    license_plate VARCHAR(50) PRIMARY KEY COMMENT '车牌号码',
    vehicle_type VARCHAR(100) NOT NULL COMMENT '车辆型号',
    category VARCHAR(50) COMMENT '车型分类：SUV/MPV/轿车/越野SUV/新能源轿车等',
    seats INT DEFAULT 5 COMMENT '座位数',
    available_quantity INT NOT NULL DEFAULT 0 COMMENT '空闲数量',
    total_quantity INT NOT NULL DEFAULT 1 COMMENT '总数量',
    daily_rate DECIMAL(10,2) NOT NULL COMMENT '日租金'
);

CREATE TABLE IF NOT EXISTS bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_number VARCHAR(255) NOT NULL UNIQUE,
    booking_begin_date DATE,
    booking_end_date DATE,
    name VARCHAR(255),
    surname VARCHAR(255),
    employer_name VARCHAR(255),
    employer_phone VARCHAR(50),
    employer_id_number VARCHAR(50),
    license_plate VARCHAR(50),
    vehicle_type VARCHAR(100),
    rental_location VARCHAR(255),
    total_amount DECIMAL(10,2) DEFAULT 0 COMMENT '总金额'
);
