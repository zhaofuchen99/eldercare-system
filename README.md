# AI智能养老社区管理系统

基于 **Spring Boot + Vue3** 开发的智能养老社区管理系统，采用前后端分离架构，覆盖会员管理、健康档案、健康评测、体检预约、社区活动、积分管理、消息通知等核心业务。

在传统养老业务基础上接入 **DeepSeek 大语言模型**，实现 AI 健康咨询、AI 健康评测和 AI 辅助预约等功能。

## ✨ 核心功能

- 👤 会员管理与 RBAC 权限控制
- 🏥 健康档案与健康记录
- 📋 AI 健康评测与分析
- 💬 AI 个性化健康咨询
- 📅 AI 辅助体检预约
- 🏃 社区活动管理
- ⭐ 积分管理
- 🔔 消息通知
- 📊 数据统计与管理看板

## 🤖 AI能力

基于用户健康档案和评测结果动态构建上下文，通过 **Spring AI + DeepSeek** 实现个性化 AI 服务。

```text
用户问题
   ↓
获取健康数据
   ↓
HealthContextBuilder
   ↓
Prompt + 用户上下文
   ↓
DeepSeek
   ↓
个性化回答
```

同时通过 **Function Calling / Tool Calling** 实现 AI 调用体检预约等业务接口。

## 🛠️ 技术栈

**后端：** Java、Spring Boot、MyBatis、Spring Security、JWT、Redis、MySQL

**AI：** Spring AI、DeepSeek、Prompt Engineering、Function Calling

**前端：** Vue3、Vite、Element Plus、Axios、Pinia

## 📁 项目结构

```text
AI-Elderly-Care/
├── eldercare-server/       # Spring Boot 后端
├── eldercare-web-admin/    # Vue3 管理端
├── eldercare-web-member/   # Vue3 会员端
├── docs/                   # 项目文档
└── README.md
```

## 🚀 项目亮点

- Spring Security + JWT + Redis 实现认证授权
- RBAC 权限模型实现细粒度权限控制
- Redis 用于缓存及 Token 状态管理
- HealthContextBuilder 实现用户健康数据上下文构建
- Function Calling 实现 AI 与业务系统交互
- 前后端分离，具备完整的企业级项目结构

> ⚠️ AI 健康建议仅用于项目演示，不构成医疗诊断或治疗建议。
