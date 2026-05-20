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
      miles-of-smiles-terms-of-use.txt        -- RAG 知识库文档（服务条款）
      docs/
        insurance_policy.txt                  -- RAG 知识库（保险理赔政策）
        return_rules.txt                      -- RAG 知识库（还车计费规则）
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

## 切换大模型

本项目与 LLM 的耦合点分布在配置、Token 计数、视觉识别三处，切换模型时需要逐项处理。

### 一、改配置即可（聊天模型）

`application.properties` 中 3 个值，同时修改即可切换聊天模型：

```properties
# 示例：切换为 DeepSeek
langchain4j.open-ai.chat-model.api-key=sk-your-deepseek-key
langchain4j.open-ai.chat-model.base-url=https://api.deepseek.com/v1
langchain4j.open-ai.chat-model.model-name=deepseek-chat

# 示例：切换为本地 Ollama
# langchain4j.open-ai.chat-model.api-key=ollama
# langchain4j.open-ai.chat-model.base-url=http://localhost:11434/v1
# langchain4j.open-ai.chat-model.model-name=qwen2.5:7b
```

只要目标服务兼容 OpenAI 的 `/v1/chat/completions` 协议，改这 3 行即可，**不需要改任何 Java 代码**。

### 二、必须改代码（Token 计数器）

> 文件：`CustomerSupportAgentConfiguration.java`

```java
// 当前写死了 GPT-4o-mini 的 tokenizer
@Bean
TokenCountEstimator tokenCountEstimator() {
    return new OpenAiTokenCountEstimator(GPT_4_O_MINI);
}
```

这个 Bean 用于：
1. 计算每条消息的 token 数量
2. `TokenWindowChatMemory` 根据它来截断对话记忆（当前设置 max 5000 token）

**切换非 OpenAI 模型时必须修改：**

| 目标模型 | 改法 |
|---|---|
| 另一个 OpenAI 模型 | 改 `OpenAiTokenCountEstimator` 构造函数参数，如 `GPT_4_O` |
| 非 OpenAI 模型（DeepSeek / Qwen / GLM 等） | 换成一个相近 tokenizer 的 `TokenCountEstimator` 实现，或使用近似算法。部分社区有 `DeepSeekTokenCountEstimator` 等实现可直接替换 |
| 不想精确计算 | 可改用字符数估算：`token -> token.length() / 4`（中英文混合场景大致可用），但对话记忆截断会不够精准 |

如果不改，后果是对话记忆窗口控制不准，可能提前截断或超出 LLM 上下文限制，但不影响功能正常运行。

### 三、必须改代码（视觉模型）

> 文件：`CustomerSupportAgentController.java`

```java
// 当前写死了通义千问视觉模型
this.visionModel = OpenAiChatModel.builder()
        .apiKey(apiKey)
        .baseUrl(baseUrl)
        .modelName("qwen-vl-max")  // ← 这里
        ...
        .build();
```

这个模型仅在**图片上传**功能中使用（用户上传订单截图，提取图片中的文字）。

| 场景 | 做法 |
|---|---|
| 不需要图片功能 | 直接删除这段构建代码和相关上传端点即可 |
| 目标服务有视觉模型 | 改 `modelName` 为目标视觉模型名 |
| 目标服务无视觉模型 | 去掉上传端点，并在前端隐藏图片按钮 |

### 四、不改即可正常工作的

| 组件 | 原因 |
|---|---|
| `AllMiniLmL6V2EmbeddingModel` | 本地 ONNX 模型，运行在 JVM 内，不依赖外部 API |
| `InMemoryEmbeddingStore` | 纯内存向量库，与 LLM 无关 |
| `BookingTools` / `VehicleTools` | 查 MySQL 数据库，不经过 LLM |
| `ContentRetriever`（RAG） | 只依赖本地嵌入模型 + 内存向量库，完全离线 |
| MyBatis / 数据库 | 与模型无关 |

### 五、提示词调优（建议但不必须）

`CustomerSupportAgent.java` 中的 `@SystemMessage` 是针对 GPT 系模型编写的。不同模型对提示词的理解存在差异：

- **Claude**：对 XML 标签结构响应更好，可能需要将规则用 `<rule>` 包裹
- **DeepSeek**：中文理解能力强，通常无需大改
- **Qwen / GLM**：中文原生支持好，但 tool-calling 指令遵循度有差异

切换后建议跑一遍集成测试（`mvn verify`），观察 tool 调用准确率和 RAG 回答质量，按需微调提示词。

### 六、完整切换检查清单

```
□ 1. 改 application.properties 中 3 个 LLM 配置（api-key / base-url / model-name）
□ 2. 改 CustomerSupportAgentConfiguration.java 中 TokenCountEstimator
□ 3. 评估是否需要图片功能：
      需要  → 改 CustomerSupportAgentController.java 中 visionModel 的 modelName
      不需要 → 删除上传端点和 visionModel 构建代码
□ 4. 启动应用，对话测试
□ 5. （可选）跑集成测试，按需微调 @SystemMessage 提示词
```

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
