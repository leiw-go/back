# Claude Code 介绍

> Claude Code 是 Anthropic 推出的官方命令行工具（CLI），将强大的 Claude 大语言模型集成到终端中，帮助开发者高效完成代码编写、调试、重构、文档撰写、测试等软件工程任务。

---

## 📑 目录

- [什么是 Claude Code](#什么是-claude-code)
- [核心特性](#核心特性)
- [安装方式](#安装方式)
- [快速开始](#快速开始)
- [常用命令](#常用命令)
- [斜杠命令（Slash Commands）](#斜杠命令slash-commands)
- [可用 Skills 能力](#可用-skills-能力)
- [Agent 类型](#agent-类型)
- [核心工具](#核心工具)
- [配置文件](#配置文件)
- [最佳实践](#最佳实践)
- [常见使用场景](#常见使用场景)
- [资源链接](#资源链接)

---

## 什么是 Claude Code

Claude Code 是 Anthropic 官方发布的 **AI 编程助手 CLI 工具**，它将 Claude 大模型（包括 Claude Opus 4.8、Sonnet 5、Haiku 4.5 等）的智能直接带入你的终端和 IDE。

通过 Claude Code，你可以：

- 🤖 在终端中与 Claude 对话，让它帮你编写或修改代码
- 🔍 自动理解整个代码库的结构与上下文
- 🛠️ 执行 Git 命令、运行测试、操作文件
- 📦 使用 MCP（Model Context Protocol）扩展工具能力
- 🪝 通过 Hooks 自定义自动化行为
- 🧠 使用 Skill 与 Agent 完成复杂的多步骤任务

它不仅是"代码补全工具"，更像是一位**全天候在线的结对编程伙伴**。

---

## 核心特性

| 特性 | 说明 |
|------|------|
| **终端原生集成** | 直接在命令行使用，无需切换 IDE |
| **深度代码理解** | 能扫描整个项目，理解文件结构和上下文 |
| **文件操作能力** | 能读取、编辑、创建本地文件 |
| **命令执行** | 可执行 Shell 命令运行测试、构建、部署等 |
| **Git 工作流** | 自动处理 commit、branch、PR 等操作 |
| **MCP 协议扩展** | 通过 Model Context Protocol 接入自定义工具 |
| **Skills 机制** | 提供丰富的预制技能（绘图、代码审查、研究等） |
| **多 Agent 协同** | 可启动子 Agent 并行处理独立任务 |
| **Hooks 钩子** | 在特定事件前后自动触发自定义行为 |
| **跨平台** | 支持 macOS、Windows、Linux |

---

## 安装方式

### 方式一：npm 全局安装（推荐）

```bash
npm install -g @anthropic-ai/claude-code
```

### 方式二：macOS / Linux 原生安装

```bash
curl -fsSL https://claude.ai/install.sh | bash
```

### 方式三：Windows PowerShell

```powershell
irm https://claude.ai/install.ps1 | iex
```

安装完成后，使用 `claude` 命令启动：

```bash
claude
```

首次启动会引导你登录 Anthropic 账号（支持 Google 账号登录）。

---

## 快速开始

### 进入项目目录

```bash
cd my-project
claude
```

### 一句话完成需求

```bash
claude "帮我写一个 Java 的二分查找函数"
```

### 让它修复 Bug

```bash
claude "为什么我的测试用例 LoginTest.testLogin 一直失败？"
```

### 自动创建 Pull Request

```bash
claude "提交当前改动并创建一个 PR"
```

### 使用 Fast 模式

```bash
/fast    # 切换到 Claude Opus 快速输出模式（仅 Opus 4.8/4.7）
```

---

## 常用命令

| 命令 | 作用 |
|------|------|
| `claude` | 启动交互式会话 |
| `claude "..."` | 启动并执行一次性任务 |
| `claude -p "..."` | 非交互式执行（适合脚本调用） |
| `claude --continue` | 继续上次的会话 |
| `claude --resume` | 恢复历史会话 |
| `claude --model` | 指定使用的模型（如 claude-opus-4-8） |
| `/help` | 查看内置命令帮助 |
| `/clear` | 清除当前会话上下文 |
| `/exit` | 退出会话 |

### 常用快捷键

| 快捷键 | 功能 |
|--------|------|
| `Shift + Tab` | 切换"规划模式"（Plan Mode） |
| `Ctrl + C` | 中断当前操作 |
| `↑ / ↓` | 浏览历史命令 |
| `@` | 引用文件作为上下文 |

---

## 斜杠命令（Slash Commands）

斜杠命令是 Claude Code 中的内置快捷方式，常用的包括：

| 命令 | 说明 |
|------|------|
| `/init` | 初始化一个项目的 `CLAUDE.md` 文档 |
| `/review` | 审查 GitHub Pull Request |
| `/code-review` | 审查当前 diff 的代码质量与潜在 Bug |
| `/security-review` | 对当前分支进行安全审查 |
| `/simplify` | 简化代码（合并重复、提升抽象） |
| `/verify` | 端到端验证代码改动是否生效 |
| `/config` | 打开配置面板（主题、模型等） |
| `/loop` | 周期性执行某个任务（如 `/loop 5m /fix-tests`） |
| `/fast` | 切换到快速模式 |
| `/memory` | 查看和编辑持久化记忆 |
| `/design-sync` | 同步本地组件库与云端设计系统 |
| `/workflows` | 查看正在运行的多 Agent 工作流 |

---

## 可用 Skills 能力

Skills 是 Claude Code 中**预制的专家能力**，通过 `Skill` 工具按需调用。

| Skill 名称 | 用途 |
|------------|------|
| **deep-research** | 多源检索 + 事实核查，生成深度研究报告 |
| **dataviz** | 图表与数据可视化设计系统 |
| **update-config** | 修改 `settings.json` 的 harness 配置 |
| **verify** | 端到端验证代码改动 |
| **code-review** | 代码审查（正确性、复用、效率） |
| **simplify** | 代码精简优化 |
| **fewer-permission-prompts** | 自动收紧权限提示，提升体验 |
| **loop** | 周期性执行任务 |
| **claude-api** | Claude API / Anthropic SDK 速查 |
| **run** | 启动并驱动当前项目应用 |
| **init** | 初始化 CLAUDE.md |
| **review** | 审查 GitHub PR |
| **security-review** | 安全审查 |

调用方式：

```text
请使用 deep-research 帮我调研一下 RAG 技术的最新进展
```

---

## Agent 类型

Claude Code 支持多种 Agent，每种有不同的角色和工具集：

| Agent 类型 | 适用场景 |
|------------|----------|
| **claude** | 通用对话与编码（默认） |
| **claude-code-guide** | Claude Code / Claude API / Agent SDK 相关问答 |
| **Explore** | 只读搜索，跨多个文件/目录扫描 |
| **general-purpose** | 复杂多步骤任务的研究与执行 |
| **Plan** | 设计实现方案、架构权衡分析 |
| **statusline-setup** | 配置状态栏 UI |

### 并行启动多个 Agent

```text
并行启动 3 个 Explore agent：
1. 搜索 src/main 下所有 Java 类
2. 搜索 src/test 下所有测试用例
3. 搜索所有 pom.xml 依赖
```

---

## 核心工具

Claude Code 中的工具调用是**模型驱动的**，常用工具包括：

| 工具 | 用途 |
|------|------|
| **Read / Write / Edit** | 读写本地文件 |
| **Glob / Grep** | 文件和内容搜索 |
| **Bash** | 执行 Shell 命令 |
| **Agent** | 启动子 Agent |
| **Skill** | 调用 Skill |
| **WebFetch / WebSearch** | 网络检索与抓取 |
| **TodoWrite / TaskCreate** | 任务列表管理 |
| **CronCreate / CronDelete** | 定时任务调度 |
| **EnterPlanMode** | 进入规划模式 |
| **NotebookEdit** | 编辑 Jupyter Notebook |

---

## 配置文件

Claude Code 的配置主要通过 `settings.json` 与 `CLAUDE.md` 完成。

### 1. 用户级配置

- macOS / Linux: `~/.claude/settings.json`
- Windows: `%USERPROFILE%\.claude\settings.json`

### 2. 项目级配置

```
项目根/
└── .claude/
    ├── settings.json         # 项目配置
    ├── settings.local.json   # 本地配置（不提交）
    └── agents/               # 自定义 Agent 定义
```

### 3. CLAUDE.md

放在项目根目录或 `~/.claude/CLAUDE.md`，为 Claude 提供项目背景信息。

```markdown
# 项目说明

这是一个 Spring Boot 项目，使用 MySQL + Redis。

## 构建命令
./mvnw clean package

## 测试命令
./mvnw test

## 代码规范
遵循阿里巴巴 Java 开发手册
```

---

## 最佳实践

### ✅ 推荐做法

1. **提供清晰的上下文**：用 `@filename` 引用关键文件
2. **明确目标与约束**：`"用 stream API 重构这个方法，不要改变外部行为"`
3. **分步骤执行**：复杂任务先用 EnterPlanMode 规划
4. **善用 CLAUDE.md**：把项目约定沉淀到文件里
5. **使用 Git 工作流**：让 Claude 处理 commit、branch、PR
6. **保留可逆性**：删除/覆盖前先确认
7. **审查 Diff**：所有改动最终还是要人类把关

### ❌ 注意事项

1. 不要让它处理**敏感凭据**（生产环境密钥等）
2. 不要在未审查 diff 前直接 `git push --force`
3. 避免在没有上下文的情况下提问
4. 长期任务记得开启 `--continue`

---

## 常见使用场景

### 场景 1：从零开发新功能

```text
请帮我在 Spring Boot 中添加一个 JWT 鉴权的拦截器，
要求：
1. 自动从 Header 解析 token
2. 校验失败返回 401
3. 编写对应的单元测试
```

### 场景 2：定位 Bug

```text
我的应用启动时报错 "Port 8080 was already in use"，
帮我分析项目中有哪些地方可能占用该端口，并给出解决方案
```

### 场景 3：批量重构

```text
把所有 controller 里的 @Autowired 字段注入
改成构造器注入，保留原有注释
```

### 场景 4：自动生成测试

```text
为 UserService 这个类的所有 public 方法生成 JUnit5 测试用例，
覆盖正常路径与至少 2 个异常路径
```

### 场景 5：发布版本

```text
1. 把当前所有改动 commit
2. 创建一个名为 release/v1.2.0 的分支并 push
3. 在 GitHub 上发起 PR 到 main
```

### 场景 6：与 GitLab/Jenkins 集成

结合 `Jenkinsfile` 和 `Dockerfile.host`，Claude Code 可以：

- 编写 CI/CD Pipeline
- 撰写 Dockerfile 多阶段构建
- 调整 `pom.xml` 依赖版本

---

## 模型选择

Claude Code 默认使用最新的高性能模型。你也可以显式指定：

| 模型 ID | 定位 |
|---------|------|
| `claude-fable-5` | 最新旗舰 Fable 5，最强能力 |
| `claude-opus-4-8` | Opus 4.8，复杂任务首选 |
| `claude-sonnet-5` | Sonnet 5，性能与速度平衡 |
| `claude-haiku-4-5-20251001` | Haiku 4.5，速度快、成本低 |

```bash
claude --model claude-opus-4-8 "请帮我优化这个 SQL 查询"
```

---

## 进阶玩法

### 1. 规划模式（Plan Mode）

按 `Shift + Tab` 进入规划模式，Claude 会**先制定方案**再执行，适合复杂改动。

### 2. Hooks 自动化

在 `settings.json` 中配置 PreToolUse / PostToolUse Hook：

```json
{
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Edit|Write",
        "hooks": [
          { "type": "command", "command": "npm run lint:fix" }
        ]
      }
    ]
  }
}
```

### 3. 自定义 Agent

在 `.claude/agents/my-agent.md` 中定义：

```markdown
---
name: db-expert
description: 数据库设计与 SQL 优化专家
tools: Bash, Read, Grep
---

你是一位资深的 DBA，专注于 MySQL 性能调优...
```

### 4. MCP 集成

通过 MCP 协议接入数据库、GitHub、Slack 等外部服务：

```json
{
  "mcpServers": {
    "github": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-github"],
      "env": { "GITHUB_TOKEN": "..." }
    }
  }
}
```

---

## 资源链接

- 🌐 官网：https://claude.com/code
- 📖 官方文档：https://docs.anthropic.com/en/docs/claude-code
- 🛠️ GitHub 仓库：https://github.com/anthropics/claude-code
- 💬 支持论坛：https://support.anthropic.com
- 📝 Changelog：https://docs.anthropic.com/en/release-notes/claude-code
- 🧩 MCP 服务器列表：https://github.com/anthropics/mcp-servers

---

## 一句话总结

> **Claude Code = Claude 大模型 + 终端 + 文件系统 + Git + 工具扩展 + Skills + Agents**，
> 让 AI 成为你终端里随叫随到的编程搭子 🚀

---

*文档生成时间：2026-07-07*
