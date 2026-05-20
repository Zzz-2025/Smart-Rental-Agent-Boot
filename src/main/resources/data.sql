-- ============================================
-- 车辆数据（真实车型 + 座位数 + 分类）
-- ============================================
REPLACE INTO vehicles (license_plate, vehicle_type, category, seats, available_quantity, total_quantity, daily_rate) VALUES
('粤A11223', '丰田汉兰达', 'SUV', 5, 2, 3, 380),
('粤A88888', '比亚迪海豹', '新能源轿车', 5, 4, 5, 280),
('沪B99999', '别克GL8', 'MPV', 7, 1, 2, 450),
('川A12345', '本田CR-V', 'SUV', 5, 3, 4, 320),
('陕A56988', '坦克300', '越野SUV', 5, 1, 2, 400),
('浙C51266', '大众朗逸', '轿车', 5, 5, 6, 200),
('京A20199', '奔驰E级', '豪华轿车', 5, 0, 1, 600),
('苏B30777', '比亚迪宋PLUS', '混动SUV', 5, 2, 3, 280),
('鄂A44400', '传祺M8', 'MPV', 7, 1, 2, 400),
('闽A70511', '丰田卡罗拉', '轿车', 5, 7, 8, 180),
('渝A80922', '哈弗H6', 'SUV', 5, 3, 4, 260),
('粤B10001', '日产轩逸', '轿车', 5, 10, 10, 150),
('沪C20002', '理想L8', '新能源SUV', 6, 5, 5, 420),
('京D30003', '丰田埃尔法', '豪华MPV', 7, 2, 2, 800);

-- ============================================
-- 订单数据
-- ============================================
REPLACE INTO bookings (booking_number, booking_begin_date, booking_end_date, name, surname,
                                employer_name, employer_phone, employer_id_number,
                                license_plate, vehicle_type, rental_location, total_amount)
VALUES ('MS-777', '2025-12-13', '2025-12-31', 'John', 'Doe',
        'John Doe', '13245685212', '441521200110121456',
        '粤A11223', '丰田汉兰达', '洛圣都租车行', 5400);

REPLACE INTO bookings (booking_number, booking_begin_date, booking_end_date, name, surname,
                                employer_name, employer_phone, employer_id_number,
                                license_plate, vehicle_type, rental_location, total_amount)
VALUES ('MS-888', '2026-01-15', '2026-01-20', '建国', '李',
        '李建国', '13866997788', '440101199005192367',
        '粤A88888', '比亚迪海豹', '广州市天河租车总店', 1500);

REPLACE INTO bookings (booking_number, booking_begin_date, booking_end_date, name, surname,
                                employer_name, employer_phone, employer_id_number,
                                license_plate, vehicle_type, rental_location, total_amount)
VALUES ('MS-999', '2026-03-01', '2026-03-10', '浩宇', '王',
        '王浩宇', '13955668899', '310102199208164521',
        '沪B99999', '别克GL8', '上海市浦东租车中心', 2700);

REPLACE INTO bookings (booking_number, booking_begin_date, booking_end_date, name, surname,
                                employer_name, employer_phone, employer_id_number,
                                license_plate, vehicle_type, rental_location, total_amount)
VALUES ('MS-123', '2026-03-07', '2026-03-12', '晓雅', '林',
        '林晓雅', '13722336655', '510104199503227891',
        '川A12345', '本田CR-V', '成都市成华租车门店', 1500);

REPLACE INTO bookings (booking_number, booking_begin_date, booking_end_date, name, surname,
                                employer_name, employer_phone, employer_id_number,
                                license_plate, vehicle_type, rental_location, total_amount)
VALUES ('MS-569', '2026-07-01', '2026-07-10', '沐阳', '陈',
        '陈沐阳', '13611223344', '610103199307085628',
        '陕A56988', '坦克300', '西安市雁塔租车服务站', 2700);

REPLACE INTO bookings (booking_number, booking_begin_date, booking_end_date, name, surname,
                                employer_name, employer_phone, employer_id_number,
                                license_plate, vehicle_type, rental_location, total_amount)
VALUES ('MS-512', '2026-07-01', '2026-07-10', '子轩', '刘',
        '刘子轩', '13500996677', '330106199406158942',
        '浙C51266', '大众朗逸', '温州市鹿城租车分行', 2700);

REPLACE INTO bookings (booking_number, booking_begin_date, booking_end_date, name, surname,
                                employer_name, employer_phone, employer_id_number,
                                license_plate, vehicle_type, rental_location, total_amount)
VALUES ('MS-201', '2026-04-05', '2026-04-12', '伟', '张',
        '张伟', '13812345678', '110105198802154321',
        '京A20199', '奔驰E级', '北京市朝阳租车中心', 2100);

REPLACE INTO bookings (booking_number, booking_begin_date, booking_end_date, name, surname,
                                employer_name, employer_phone, employer_id_number,
                                license_plate, vehicle_type, rental_location, total_amount)
VALUES ('MS-307', '2026-05-20', '2026-05-28', '敏', '刘',
        '刘敏', '13998765432', '320106199111037654',
        '苏B30777', '比亚迪宋PLUS', '南京市鼓楼租车门店', 2400);

REPLACE INTO bookings (booking_number, booking_begin_date, booking_end_date, name, surname,
                                employer_name, employer_phone, employer_id_number,
                                license_plate, vehicle_type, rental_location, total_amount)
VALUES ('MS-444', '2026-06-10', '2026-06-18', '俊', '杨',
        '杨俊', '13776543210', '420102199407221987',
        '鄂A44400', '传祺M8', '武汉市汉口租车服务站', 2400);

REPLACE INTO bookings (booking_number, booking_begin_date, booking_end_date, name, surname,
                                employer_name, employer_phone, employer_id_number,
                                license_plate, vehicle_type, rental_location, total_amount)
VALUES ('MS-705', '2026-08-15', '2026-08-22', '丽', '黄',
        '黄丽', '13665432109', '350103199209056432',
        '闽A70511', '丰田卡罗拉', '福州市台江租车分行', 2100);

REPLACE INTO bookings (booking_number, booking_begin_date, booking_end_date, name, surname,
                                employer_name, employer_phone, employer_id_number,
                                license_plate, vehicle_type, rental_location, total_amount)
VALUES ('MS-809', '2026-09-01', '2026-09-08', '凯', '周',
        '周凯', '13554321098', '500105199012289876',
        '渝A80922', '哈弗H6', '重庆市南岸租车总店', 2100);
