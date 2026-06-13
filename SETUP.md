# 项目配置说明

## 后端配置

### 1. 数据库配置

复制配置模板：
```bash
cd back_end/src/main/resources
cp application.properties.example application.properties
```

### 2. 修改 application.properties

编辑 `application.properties` 文件，填入你的实际配置：

#### 数据库配置
```properties
spring.datasource.password=你的数据库密码
```

#### JWT 密钥配置
生成一个随机密钥（至少32字符）：
```properties
jwt.secret=你生成的随机密钥
```

#### 邮箱 SMTP 配置

如果使用 QQ 邮箱：
1. 登录 QQ 邮箱 → 设置 → 账户
2. 开启 SMTP 服务，获取授权码
3. 填入配置：
```properties
spring.mail.username=你的QQ邮箱
spring.mail.password=你的SMTP授权码
```

#### Redis 配置（如果设置了密码）
```properties
spring.data.redis.password=你的Redis密码
```

#### HDFS 配置
```properties
hdfs.uri=hdfs://你的HDFS地址:8020
hdfs.user=你的HDFS用户名
```

### 3. 安全提醒

⚠️ **切勿将 `application.properties` 提交到 Git 仓库！**

该文件已在 `.gitignore` 中排除，包含敏感信息：
- 数据库密码
- JWT 密钥
- 邮箱授权码
- 其他私密配置

仅提交 `application.properties.example` 模板文件。

## 前端配置

前端配置文件同理，参考 `front_end/.env.example` 创建本地配置。
