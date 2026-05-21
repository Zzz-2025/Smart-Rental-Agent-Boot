# Miles of Smiles — AI 智能租车客服

基于 LangChain4j + Spring Boot 的智能租车客服系统。AI 坐席 **Roger** 通过调用工具直接操作 MySQL 数据库，支持订单查询、创建、取消、延期、智能推荐，以及基于 RAG 的政策问答。

## 功能

- 查询 / 创建 / 取消 / 延期订单（身份核验）
- 智能车辆推荐（按人数、场景、预算匹配）
- RAG 知识库问答（保险、还车规则、服务条款）
- 上传图片提取文字（视觉模型）
- Redis 分布式锁防超卖 + IP 限流

## 技术栈

| 组件 | 技术 |
|------|------|
| 语言 | Java 17 |
| 框架 | Spring Boot 3.4.2 |
| AI | LangChain4j 1.14.0-beta24 |
| LLM | OpenAI 兼容接口（MiniMax-M2.5 / Qwen-VL-Max） |
| 嵌入模型 | AllMiniLmL6V2（本地 ONNX） |
| 数据库 | MySQL 8.0 + MyBatis 3.0.4 |
| 缓存/锁 | Redisson (Redis 7.2) |
| 前端 | 单页 HTML/CSS/JS |

## 项目结构

```
src/main/java/dev/langchain4j/example/
  CustomerSupportAgentApplication.java       -- 启动入口
  CustomerSupportAgent.java                  -- AI Agent 接口
  CustomerSupportAgentConfiguration.java     -- RAG / 对话记忆配置
  config/
    RedissonConfig.java                      -- Redis 客户端（分布式锁 + 限流）
  controller/
    CustomerSupportAgentController.java      -- REST 控制器
  tools/
    BookingTools.java                        -- 订单工具（Agent 可调用）
    VehicleTools.java                        -- 车辆工具（Agent 可调用）
  service/
    BookingService.java                      -- 订单业务逻辑
    VehicleService.java                      -- 车辆业务逻辑
  mapper/
    BookingMapper.java / VehicleMapper.java  -- MyBatis DAO
  model/
    Booking.java / Vehicle.java / ...
src/main/resources/
  application.properties                     -- 全局配置
  schema.sql / data.sql                      -- 建表 DDL + 种子数据
  miles-of-smiles-terms-of-use.txt           -- RAG 知识库
  mapper/
    BookingMapper.xml / VehicleMapper.xml    -- SQL 映射
  static/index.html                          -- 聊天界面
```

## 架构

```
浏览器 (index.html)
  │  POST /customerSupportAgent
  ▼
CustomerSupportAgentController
  │  Redisson RRateLimiter（IP 限流，10次/分钟）
  ▼
CustomerSupportAgent (@AiService)
  │  SystemMessage 定义人设与规则
  │  TokenWindowChatMemory（5000 token 滑动窗口）
  │  ContentRetriever（RAG 检索本地知识库）
  ▼
LangChain4j → LLM（MiniMax-M2.5 / DashScope）
  │  路由 @Tool 调用
  ▼
BookingTools / VehicleTools
  │  Redisson RLock（按车牌加锁，防超卖）
  ▼
BookingService → MyBatis Mapper → MySQL（同步写入）
```

## 快速开始

### 1. 启动中间件（Docker）

项目依赖 **MySQL** 和 **Redis**，推荐使用 `docker-compose.yml` 一键启动：

```bash
docker-compose up -d
```

或手动启动：

```bash
# MySQL
docker run -d --name rental-mysql \
  -e MYSQL_ROOT_PASSWORD=123456 \
  -e MYSQL_DATABASE=Planetitle \
  -p 3306:3306 \
  mysql:8.0

# Redis
docker run -d --name rental-redis \
  -p 6379:6379 \
  redis:7.2-alpine
```

### 2. 修改配置

编辑 `src/main/resources/application.properties`：

```properties
# MySQL
spring.datasource.url=jdbc:mysql://localhost:3306/Planetitle?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai&characterEncoding=utf-8
spring.datasource.username=root
spring.datasource.password=123456

# Redis
spring.data.redis.host=localhost
spring.data.redis.port=6379

# LLM（阿里云 DashScope）
langchain4j.open-ai.chat-model.api-key=你的API密钥
langchain4j.open-ai.chat-model.base-url=https://dashscope.aliyuncs.com/compatible-mode/v1
langchain4j.open-ai.chat-model.model-name=MiniMax-M2.5
```

### 3. 启动应用

在 IDEA 中运行 `CustomerSupportAgentApplication`，或：

```bash
./mvnw spring-boot:run
```

启动后自动执行 `schema.sql` 和 `data.sql`，建表并填充示例数据。

### 4. 访问

浏览器打开 `http://localhost:8080/`，即可与 Roger 对话。

## 下单流程（防超卖）

```
用户发起下单请求
  → AI 调用 createBooking 工具
    → Redis 分布式锁（按车牌加锁）
      → 查询车辆库存
      → 校验 available_quantity > 0
      → 扣减库存（CAS UPDATE）
      → 同步写入订单到 MySQL
    → 释放锁
  → 返回结果给用户
```

## 切换 LLM

`application.properties` 中改 3 行即可切换模型：

```properties
# 示例：DeepSeek
langchain4j.open-ai.chat-model.api-key=sk-your-key
langchain4j.open-ai.chat-model.base-url=https://api.deepseek.com/v1
langchain4j.open-ai.chat-model.model-name=deepseek-chat
```

> 切换非 OpenAI 模型时，还需修改 `CustomerSupportAgentConfiguration.java` 中的 `TokenCountEstimator` 和 `CustomerSupportAgentController.java` 中的视觉模型名。详见配置文件中注释。

## API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/customerSupportAgent` | 发送消息 |
| POST | `/customerSupportAgent/upload` | 上传图片 |

## 测试

```bash
./mvnw test
```

集成测试使用 LLM-as-Judge 模式，覆盖查询、创建、取消、RAG、边界拒绝等场景。
