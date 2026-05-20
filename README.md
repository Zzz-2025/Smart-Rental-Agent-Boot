# Smart Rental Agent Boot

基于 LangChain4j 和 Spring Boot 构建的 AI 智能租车客服示例项目。演示如何将大语言模型（LLM）与 Java 后端（数据库、REST API、持久化）深度集成，打造一个完整的、接近生产级水平的 AI 客服应用。

## 项目背景

本项目为虚拟租车公司 **"Miles of Smiles"** 构建了一位名为 **Roger** 的 AI 客服坐席。Roger 能够：

- 🔍 查询已有订单
- 📝 创建新订单
- ❌ 取消订单（含确认流程）
- 📅 订单延期/续租（含车辆可用性校验）
- 🚗 根据用户需求智能推荐车辆
- 📄 基于 RAG 回答租车条款相关问题
- 🖼️ 识别用户上传的图片内容（如订单截图）

## 技术栈

| 组件 | 技术 | 版本 |
|---|---|---|
| 语言 | Java | 17 |
| 框架 | Spring Boot | 3.4.2 |
| 构建 | Maven | -- |
| AI 框架 | LangChain4j | 1.14.0-beta24 |
| LLM | OpenAI 兼容接口（MiniMax-M2.5 / Qwen-VL-Max） | -- |
| 嵌入模型 | AllMiniLmL6V2（本地 ONNX 运行） | -- |
| 数据库 | MySQL | -- |
| ORM | MyBatis | 3.0.4 |
| 测试 | JUnit 5 + Mockito + AssertJ + LLM-as-Judge | -- |
| 前端 | 单页 HTML/CSS/JS | -- |

## 项目结构

```
src/
  main/
    java/dev/langchain4j/example/
      CustomerSupportAgentApplication.java    -- Spring Boot 启动入口
      CustomerSupportAgent.java               -- AI Agent 接口（@AiService）
      CustomerSupportAgentConfiguration.java  -- Spring 配置（RAG、对话记忆等）
      controller/
        CustomerSupportAgentController.java   -- REST 控制器
      tools/
        BookingTools.java                     -- 订单相关工具（Agent 可调用）
        VehicleTools.java                     -- 车辆相关工具（Agent 可调用）
      service/
        BookingService.java                   -- 订单业务逻辑
        VehicleService.java                   -- 车辆业务逻辑
      mapper/
        BookingMapper.java / VehicleMapper.java -- MyBatis DAO
      model/
        Booking.java / Vehicle.java ...       -- 实体类
    resources/
      application.properties                  -- 配置文件
      schema.sql / data.sql                   -- 建表 DDL + 种子数据
      miles-of-smiles-terms-of-use.txt        -- RAG 知识库文档
      static/index.html                       -- 聊天 UI
  test/
    java/dev/langchain4j/example/
      CustomerSupportAgentIT.java             -- 集成测试（LLM-as-Judge）
```

## 架构

```
浏览器 (index.html)
    │  GET/POST /customerSupportAgent
    ▼
CustomerSupportAgentController
    │
    ▼
CustomerSupportAgent (@AiService)
    │  SystemMessage 定义 Roger 的人设与规则
    │  TokenWindowChatMemory (5000 tokens 滑动窗口)
    │  ContentRetriever (RAG 检索)
    │
    ▼
LangChain4j AI Runtime
    │  路由 @Tool 调用
    │  调用 LLM
    ▼
BookingTools / VehicleTools
    │
    ▼
BookingService / VehicleService
    │
    ▼
MyBatis Mapper → MySQL
```

## 快速开始

### 前置条件

- JDK 17+
- MySQL 数据库

### 1. 创建数据库

```sql
CREATE DATABASE Planetitle CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 2. 修改配置

编辑 `src/main/resources/application.properties`：

```properties
# 数据库
spring.datasource.url=jdbc:mysql://localhost:3306/Planetitle?...
spring.datasource.username=你的用户名
spring.datasource.password=你的密码

# LLM API（兼容 OpenAI 接口格式）
langchain4j.open-ai.chat-model.api-key=你的API密钥
langchain4j.open-ai.chat-model.base-url=https://dashscope.aliyuncs.com/compatible-mode/v1
langchain4j.open-ai.chat-model.model-name=MiniMax-M2.5
```

默认使用阿里云 DashScope 作为 LLM 后端，也可替换为任意 OpenAI 兼容接口（OpenAI、Azure OpenAI、Ollama 等）。

### 3. 启动应用

```bash
./mvnw spring-boot:run
```

应用启动后自动执行 `schema.sql` 和 `data.sql`，创建表结构并填充示例数据。

### 4. 访问

浏览器打开 `http://localhost:8080/`，即可与 Roger 对话。

## 功能详解

### 身份核验

查看订单详情或取消订单前，Agent 会要求提供**订单号 + 雇主手机号（或雇主身份证号）**进行身份验证。

### 智能车辆推荐

Roger 可根据用户描述的需求（人数、场景、预算）匹配车辆，并针对用户痛点进行推荐。例如：

> 用户："我们一家六口要去自驾游，有什么推荐吗？"
>
> Roger 会查询 7 座以上 MPV/SUV，按日租金排序，列出选项并附带推荐理由。

### 订单延期

4 步流程：身份核验 → 检查目标时段车辆是否可用 → 执行延期 → 确认新还车日期及额外费用。

### RAG 知识库

Agent 可回答租车条款相关问题（取消政策、预订规则等）。嵌入模型 `AllMiniLmL6V2` 在本地运行，无需外部 API 调用。

### 图片识别

支持上传 PNG/JPEG/GIF/WebP/BMP 图片（最大 5MB）。系统将图片发送至视觉模型（Qwen-VL-Max）进行文字提取，提取结果作为对话上下文供 Agent 处理。

### 领域边界

Agent 拒绝回答与业务无关的问题（如"帮我写段代码"、"今天天气怎么样"），始终保持客服角色。

## API 端点

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/customerSupportAgent?userMessage=...&sessionId=...` | 发送文本消息 |
| POST | `/customerSupportAgent/upload` | 上传图片（multipart/form-data） |

## 运行测试

```bash
./mvnw test
```

集成测试使用 **LLM-as-Judge** 模式：通过独立的 Judge 模型评估 Agent 的回答是否满足语义条件，相比字符串匹配更灵活、更准确。

测试覆盖场景：
- 成功查询订单 / 不存在的订单
- 信息不足时追问
- 取消订单流程（双轮对话）
- 问候语与身份问题
- 取消政策 RAG 检索
- 非业务问题拒绝
- Token 用量监控
