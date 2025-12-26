-- 检查 PostGIS 版本，如果有返回结果，说明环境搭建成功
SELECT PostGIS_Full_Version();

-- 1. 启用 PostGIS 扩展 (必须先执行这一步)
CREATE EXTENSION IF NOT EXISTS postgis;

-- 2. 创建 Users 表
CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       username VARCHAR(255) NOT NULL,
                       preferences JSONB DEFAULT '{}'::jsonb, -- 存储用户偏好，如 {"fav_category": "spicy"}
                       created_at TIMESTAMP DEFAULT NOW()
);

-- 3. 创建 Merchants (商户) 表
CREATE TABLE merchants (
                           id BIGSERIAL PRIMARY KEY,
                           name VARCHAR(255) NOT NULL,
                           category VARCHAR(100),
                           avg_price INTEGER,                       -- 人均价格
                           rating DECIMAL(2, 1),                    -- 评分 (e.g., 4.5)
                           location GEOGRAPHY(POINT, 4326),         -- 核心：地理位置 (SRID 4326 是经纬度标准)
                           address VARCHAR(255),
                           tags TEXT[],                             -- 数组类型，存储标签，如 ['停车方便', '包间']
                           description TEXT,                        -- 用于生成向量的富文本
                           open_time VARCHAR(100),
                           created_at TIMESTAMP DEFAULT NOW(),
                           updated_at TIMESTAMP DEFAULT NOW()       -- 用于同步 ES
);

-- 4. 创建 Products (推荐菜) 表
CREATE TABLE products (
                          id BIGSERIAL PRIMARY KEY,
                          merchant_id BIGINT NOT NULL,
                          name VARCHAR(255) NOT NULL,
                          price DECIMAL(10, 2),
                          is_recommend BOOLEAN DEFAULT FALSE,      -- 是否招牌菜
                          CONSTRAINT fk_merchant_product FOREIGN KEY (merchant_id) REFERENCES merchants(id) ON DELETE CASCADE
);

-- 5. 创建 Reviews (评论) 表
CREATE TABLE reviews (
                         id BIGSERIAL PRIMARY KEY,
                         merchant_id BIGINT NOT NULL,
                         user_id BIGINT,                          -- 可以为空，如果支持匿名评论
                         content TEXT,
                         rating DECIMAL(2, 1),
                         created_at TIMESTAMP DEFAULT NOW(),
                         CONSTRAINT fk_merchant_review FOREIGN KEY (merchant_id) REFERENCES merchants(id) ON DELETE CASCADE,
                         CONSTRAINT fk_user_review FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL
);

-- ========================================================
-- 索引优化 (Performance Tuning)
-- ========================================================

-- 1. 空间索引 (GIST): 极速查找 "离我最近"
CREATE INDEX idx_merchants_location ON merchants USING GIST (location);

-- 2. 数组索引 (GIN): 极速查找包含特定标签的商户 (e.g., 包含 '包间')
CREATE INDEX idx_merchants_tags ON merchants USING GIN (tags);

-- 3. JSONB 索引: 查找特定用户偏好
CREATE INDEX idx_users_preferences ON users USING GIN (preferences);

-- 4. 常规外键索引
CREATE INDEX idx_products_merchant_id ON products(merchant_id);
CREATE INDEX idx_reviews_merchant_id ON reviews(merchant_id);

-- ========================================================
-- 触发器 (自动维护 updated_at)
-- ========================================================

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_merchants_modtime
    BEFORE UPDATE ON merchants
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 插入一条北京三里屯的数据
INSERT INTO merchants (name, category, avg_price, rating, location, address, tags, description)
VALUES (
           'Blue Frog 蓝蛙(三里屯店)',
           '西餐',
           168,
           4.6,
           ST_SetSRID(ST_MakePoint(116.4551, 39.9373), 4326), -- 注意：先经度(Lon)，后纬度(Lat)
           '朝阳区三里屯路19号院',
           ARRAY['汉堡', '露台', '约会'],
           '一家氛围轻松的西餐厅，主打美式汉堡和鸡尾酒，适合情侣约会。'
       );

-- 验证：查找距离当前位置（假设在天安门）5公里以内的商户
-- 天安门坐标: 116.3974, 39.9093
SELECT id, name,
       ST_Distance(location, ST_SetSRID(ST_MakePoint(116.3974, 39.9093), 4326)) as dist_meters
FROM merchants
WHERE ST_DWithin(location, ST_SetSRID(ST_MakePoint(116.3974, 39.9093), 4326), 10000);