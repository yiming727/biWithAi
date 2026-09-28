# biWithAi — AI 智能 BI 分析平台

上传一份数据文件，用自然语言描述你想分析什么，AI 自动产出可视化图表和分析结论 —— 无需写 SQL，也无需手动拖拽配置图表。

本仓库为前后端分离的单体项目：`bi-backend`（Spring Boot）+ `bi-frontend`（React / Ant Design Pro）。

## 功能

| 功能 | 说明 |
| --- | --- |
| 智能分析（同步） | 上传数据 + 填写分析目标，请求内同步返回图表配置与结论 |
| 智能分析（异步） | 提交后进入消息队列，前端轮询任务状态与结果 |
| 我的图表 | 分页查看、编辑、删除自己生成的图表 |
| 用户模块 | 注册、登录、注销、权限校验 |
| 文件上传 | 原始数据文件上传与校验（大小上限 10MB） |
| 接口文档 | 基于 Knife4j / OpenAPI 的在线文档 |

## 技术栈

**后端**

- Java 8、Spring Boot 2.7.2
- MyBatis-Plus 3.5.2 + MySQL
- Redis（Spring Session 分布式会话、Redisson 分布式限流）
- RabbitMQ（异步生成图表的消息队列）
- Elasticsearch（`post_es_mapping.json`，用于帖子检索，当前配置默认关闭）
- Knife4j 4.4（OpenAPI 文档）、EasyExcel、Hutool、Lombok
- AI 接入：DeepSeek 接口 / Yucongming Java SDK
- 腾讯云 COS（对象存储）、微信公众平台 SDK

**前端**

- React 18、TypeScript
- Ant Design Pro 6 / Ant Design 5、UmiJS Max 4
- ECharts（`echarts-for-react`）
- API 客户端由后端 Swagger 自动生成（`src/services/swagger/`）

## 目录结构

```
biWithAi/
├── bi-backend/                              # Spring Boot 后端
│   ├── sql/
│   │   ├── create_table.sql                 # 建库建表（user、chart）
│   │   └── post_es_mapping.json             # Elasticsearch 索引 mapping
│   ├── doc/swagger.png
│   └── src/main/
│       ├── java/com/yiming/springbootinit/
│       │   ├── MainApplication.java         # 启动类
│       │   ├── controller/                  # ChartController 为业务核心
│       │   ├── service/                     # 业务逻辑
│       │   ├── manager/                     # AI 调用封装
│       │   ├── bizmq/  mq/                  # 消息队列生产与消费
│       │   ├── job/                         # 定时任务（cycle / once）
│       │   ├── esdao/                       # Elasticsearch 数据访问
│       │   ├── aop/  annotation/            # 鉴权、日志等切面
│       │   ├── config/  constant/  utils/
│       │   └── model/                       # entity / dto / vo / enums
│       └── resources/                       # application*.yml
└── bi-frontend/                             # React 前端
    ├── config/                              # 路由、代理、构建配置
    └── src/
        ├── pages/
        │   ├── AddChart/                    # 智能分析（同步）
        │   ├── AddChartAsync/               # 智能分析（异步）
        │   ├── MyChart/                     # 我的图表
        │   └── User/Login/                  # 登录
        ├── services/swagger/                # 由后端接口生成的客户端
        └── app.tsx                          # 运行时配置（含 baseURL）
```

## 快速开始

### 环境要求

| 依赖 | 版本 / 说明 |
| --- | --- |
| JDK | 8 |
| Maven | 3.6+，或直接使用仓库自带的 `mvnw` |
| Node.js | >= 18 |
| pnpm | 见下方「约定」 —— 本项目统一使用 pnpm |
| MySQL | 8.x |
| Redis | 用于分布式会话与限流 |
| RabbitMQ | 仅异步（MQ）模式需要 |

### 1. 初始化数据库

```bash
mysql -u root -p < bi-backend/sql/create_table.sql
```

该脚本目前创建 `user` 与 `chart` 两张表。

### 2. 配置后端

编辑 `bi-backend/src/main/resources/application.yml`，把所有标注 `# todo 需替换配置` 的项替换为真实值：MySQL、Redis、RabbitMQ、微信、COS，以及 AI 接口的 `apiKey`。

其中 `spring.redis` 与 `spring.elasticsearch` 需先填好配置、再取消注释；不启用 ES 时保持注释即可。

> **不要把替换了真实密钥的配置文件提交到仓库**，详见下方「安全约定」。

### 3. 启动后端

```bash
cd bi-backend
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

- 服务地址：<http://localhost:8101/api> （`server.port=8101`，`context-path=/api`）
- 接口文档：<http://localhost:8101/api/doc.html>

### 4. 启动前端

```bash
cd bi-frontend
pnpm install
pnpm dev
```

访问 <http://localhost:8000>。可用的页面路由：

- `/add_chart` 智能分析（同步）
- `/add_chart_async` 智能分析（异步）
- `/my_chart` 我的图表
- `/user/login` 登录

前端请求地址硬编码在 `bi-frontend/src/app.tsx`（`baseURL: "http://localhost:8101"`）。后端地址变更时需同步修改这里。

## 核心接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/chart/gen` | 同步生成图表，直接返回结果 |
| POST | `/api/chart/gen/async` | 异步生成（线程池） |
| POST | `/api/chart/gen/async/mq` | 异步生成（RabbitMQ） |
| POST | `/api/chart/my/list/page` | 分页获取当前用户的图表 |
| POST | `/api/chart/delete` | 删除图表 |
| POST | `/api/user/register`、`/api/user/login` | 注册 / 登录 |

完整接口见 <http://localhost:8101/api/doc.html>。

## 安全约定

`application*.yml` 中的数据库密码、AI Key、COS 密钥等目前是**占位符**（`123456` / `xxx`）。需要注意的是：

- 一旦把这些位置换成真实凭据并提交，它们会**永久留在 git 历史中**，之后删除文件也无法移除，必须重写历史并轮换密钥。
- 提交前建议自查一遍：

  ```bash
  git grep -inE 'password|secret|apiKey|accessKey' -- '*.yml' '*.properties' '*.xml'
  ```

- 生产凭据推荐改用环境变量注入（`password: ${DB_PASSWORD}`），或把 `application-prod.yml` 排除出版本控制、仓库内只保留一份 `application-prod.yml.example` 模板。

## 开发约定

- **前端依赖统一用 pnpm。** 仓库版本控制的是 `bi-frontend/pnpm-lock.yaml`，而 `bi-frontend/.gitignore` 中忽略了 `package-lock.json`。若有人执行 `npm install`，生成的锁文件不会被提交，本地依赖版本会与仓库记录悄悄偏离。
- **怀疑「本地说已同步、远端却没有」时**，用 `git ls-remote origin` 判断。它会实际连接远端，而 `git status` 依据的本地引用可能是过期的：

  ```bash
  git ls-remote origin    # 有输出 = 确实推送成功
  ```
