# AI智能养老社区管理系统

基于 **Spring Boot + Vue3** 开发的前后端分离养老社区管理系统，围绕会员管理、健康档案、健康评测、体检预约、社区活动、积分管理、消息通知等业务场景，实现完整的社区养老服务流程。

在传统业务系统基础上接入 **Spring AI + DeepSeek**，实现 AI 健康咨询、AI 健康评测和 Function Calling 辅助体检预约等功能。

## ✨ 核心功能

- 会员管理与 RBAC 权限控制
- 健康档案与健康记录
- AI 健康咨询与健康评测
- AI 辅助体检预约
- 体检预约与报告管理
- 社区活动与报名
- 积分管理与 FIFO 扣减
- 消息通知
- 管理端数据看板

## 🛠️ 技术栈

**后端：** Java 25、Spring Boot 4.1、Spring Security、MyBatis、MySQL、Redis、JWT

**AI：** Spring AI、DeepSeek、SSE、Function Calling、Prompt Engineering

**前端：** Vue3、Vite、Element Plus、Vant、Pinia、Axios

**部署：** Docker Compose、Nginx

## 🔥 项目亮点

- 基于 Spring Security + JWT + Redis 实现双 Token 认证及 RBAC 权限控制
- 基于 MySQL 条件原子 UPDATE + 事务实现预约、活动报名并发控制，防止超卖
- 基于批次 + FIFO 模型设计积分账务，保证积分扣减与退还的一致性
- 基于 Spring AI + DeepSeek 实现 SSE 流式 AI 对话
- 通过 HealthContextBuilder 将用户健康档案、评测结果等业务数据注入 AI 上下文，实现个性化健康咨询
- 通过 Function Calling 实现 AI 调用体检套餐、时段查询及预约等业务能力
- Docker Compose 实现 MySQL、Redis、后端及前端服务容器化部署

## 📁 项目结构

```text
eldercare-system/
├── src/                    # Spring Boot 后端
├── web-admin/              # Vue3 管理端（Element Plus）
├── web-member/             # Vue3 会员端（Vant）
├── sql/                    # 建库建表脚本
├── docs/                   # 项目文档
├── docker-compose.yml      # 一键容器化编排
└── README.md
```

## ⚠️ 说明

AI 健康分析结果仅用于项目演示与技术研究，不构成医疗诊断或治疗建议。
