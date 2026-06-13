-- ============================================================
-- 模拟用户数据初始化脚本（PostgreSQL）
-- 目标表: gra_project.sys_user
-- 说明:
-- 1) 使用 pgcrypto 生成 BCrypt 哈希，明文密码统一为: 123456
-- 2) 按 username 做幂等写入，可重复执行
-- ============================================================

SET search_path TO gra_project, public;

-- 生成密码哈希需要该扩展
CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO gra_project.sys_user (
    username,
    password_hash,
    role,
    email,
    phone,
    gender,
    age,
    region,
    organization,
    bio,
    avatar_url,
    created_at,
    updated_at
)
VALUES
    (
        'admin',
        crypt('123456', gen_salt('bf', 10)),
        1,
        'admin@weather.local',
        '13900000001',
        1,
        28,
        '四川省/成都市/双流区',
        '系统管理部',
        '系统默认管理员账号',
        '/uploads/avatars/default-admin.png',
        NOW(),
        NOW()
    ),
    (
        'liujie',
        crypt('123456', gen_salt('bf', 10)),
        0,
        'liujie@example.com',
        '13900000002',
        1,
        18,
        '四川省/成都市/双流区',
        '成都信息工程大学',
        '我是学生',
        '/uploads/avatars/default-user-1.png',
        NOW() - INTERVAL '30 days',
        NOW()
    ),
    (
        'wangmin',
        crypt('123456', gen_salt('bf', 10)),
        0,
        'wangmin@example.com',
        '13900000003',
        2,
        24,
        '北京市/北京市/海淀区',
        '国家气象中心',
        '关注短临预报与数据质量控制',
        '/uploads/avatars/default-user-2.png',
        NOW() - INTERVAL '20 days',
        NOW()
    ),
    (
        'zhangqi',
        crypt('123456', gen_salt('bf', 10)),
        0,
        'zhangqi@example.com',
        '13900000004',
        1,
        26,
        '广东省/广州市/天河区',
        '华南区域中心',
        '负责站点运维与观测数据管理',
        '/uploads/avatars/default-user-3.png',
        NOW() - INTERVAL '15 days',
        NOW()
    ),
    (
        'chenyu',
        crypt('123456', gen_salt('bf', 10)),
        0,
        'chenyu@example.com',
        '13900000005',
        2,
        22,
        '江苏省/南京市/鼓楼区',
        '南京信息工程大学',
        '研究气象机器学习应用',
        '/uploads/avatars/default-user-4.png',
        NOW() - INTERVAL '12 days',
        NOW()
    ),
    (
        'sunhao',
        crypt('123456', gen_salt('bf', 10)),
        0,
        'sunhao@example.com',
        '13900000006',
        1,
        30,
        '上海市/上海市/浦东新区',
        '城市气象服务中心',
        '负责业务系统集成与服务发布',
        '/uploads/avatars/default-user-5.png',
        NOW() - INTERVAL '8 days',
        NOW()
    ),
    (
        'zhoulin',
        crypt('123456', gen_salt('bf', 10)),
        0,
        'zhoulin@example.com',
        '13900000007',
        0,
        27,
        '湖北省/武汉市/洪山区',
        '区域气象台',
        '关注天气过程统计分析',
        '/uploads/avatars/default-user-6.png',
        NOW() - INTERVAL '5 days',
        NOW()
    ),
    (
        'yangfan',
        crypt('123456', gen_salt('bf', 10)),
        0,
        'yangfan@example.com',
        '13900000008',
        1,
        25,
        '浙江省/杭州市/西湖区',
        '数据研发组',
        '负责数据处理与接口开发',
        '/uploads/avatars/default-user-7.png',
        NOW() - INTERVAL '2 days',
        NOW()
    )
ON CONFLICT (username)
DO UPDATE SET
    role = EXCLUDED.role,
    email = EXCLUDED.email,
    phone = EXCLUDED.phone,
    gender = EXCLUDED.gender,
    age = EXCLUDED.age,
    region = EXCLUDED.region,
    organization = EXCLUDED.organization,
    bio = EXCLUDED.bio,
    avatar_url = EXCLUDED.avatar_url,
    updated_at = NOW();

-- 查看导入结果
-- SELECT user_id, username, role, email, phone, created_at
-- FROM gra_project.sys_user
-- ORDER BY user_id;
