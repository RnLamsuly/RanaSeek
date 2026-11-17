# RanaSeek

一条 Android 上的 DeepSeek 全家桶客户端：UI 参考 DeepSeek 移动端风格，API Key 由用户自己填写，直连 DeepSeek 开放平台。

> **非官方声明**：本项目是第三方客户端，与 DeepSeek（深度求索）无任何官方关联。界面为参考官方设计**自行实现**，未复制官方代码、反编译官方 APK 或使用其素材。
>
> **数据说明**：API Key、会话记录、全局人设、聊天背景全部只保存在**本地设备**，这些数据不会上传到任何服务器。
>
> **开源协议**：MIT（见 [LICENSE](LICENSE)）。

## 功能

| 能力 | 说明 |
| --- | --- |
| 流式对话 | Messages API（`/anthropic/v1/messages`）+ SSE，逐字输出、可随时停止 |
| 深度思考 | 思考块折叠展示，带耗时；`thinking.type` 跟随「深度思考」开关 |
| 联网搜索 | `deepseek_web_search` / `deepseek_read_url` 工具，搜索卡片可展开，正文带引用与来源链接 |
| 会话标题 | 首轮请求开启 `title`，自动生成会话标题 |
| 内容安全 | `safety_check: high`，拒答时清空内容并提示 |
| 图片理解 | 相册选图 → 压缩 JPEG → base64 内联，`deepseek-flash` 读图（首页/对话页均可） |
| 文件解析 | 上传到 `/files`（PDF/Word 走 `text_recognition`），以 `document` 块引用（首页/对话页均可） |
| 语音输入 | AudioRecord 录 WAV → `/v1/tools/asr` → 文字填入输入框（首页/对话页均可） |
| 朗读 | `/v1/tools/tts`（stella 女声）→ MediaPlayer 播放，可停止 |
| 全局人设 | 设置页填写，作为 system 提示词 |
| 会话管理 | 本地 JSON 持久化，侧边栏按「今天/更早」分组，长按删除 |
| 消息操作 | 长按自己消息：浮动菜单（复制/修改/分享）；助手消息底部：复制 / 重新生成 / 朗读 |
| Token 统计 | 每条回答底部显示本轮 token 消耗（输入/输出/思考/缓存） |
| 压缩上下文 | 加号菜单 →「压缩上下文」：把整段对话总结成一条摘要替换历史，节省上下文 |
| 主题 | 浅色 / 深色 / 跟随系统 |
| 聊天背景 | 本地选图 → 手动裁剪（缩放拖动）→ 虚化 0~25；首页与对话页共用 |
| 消息样式 | 纯色 / 亚克力气泡；助手气泡可开关（关闭为纯文本） |
| 其他 | Markdown 渲染、取消发送、API Key 测试连接、长回答渐隐滚动 |

## 构建

环境：JDK 21、Gradle 9.9.1、Android SDK（platform 37 / build-tools 37）。

```powershell
# debug
gradle assembleDebug

# release（当前用 debug 签名，方便直接安装）
gradle assembleRelease
```

产物：
- `app/build/outputs/apk/debug/app-debug.apk`
- `app/build/outputs/apk/release/app-release.apk`（约 3 MB，R8 压缩）

## 首次使用

1. 打开 App → 侧边栏头像 → 设置
2. `API Key` → 填入 `sk-...` → 点「测试连接」确认 → 保存
3. 可选：`全局人设`、`外观`（主题/背景/气泡风格）

## 目录结构

```
app/src/main/java/com/ranaseek/app
├── data
│   ├── api        SSE 读取（OkHttp）
│   ├── audio      AudioRecord 录音 → WAV
│   ├── image      背景图/聊天图存取、压缩
│   ├── model      消息与内容块模型
│   ├── repo       Messages API 客户端（对话/ASR/TTS/Files）
│   ├── session    会话本地存储
│   └── settings   DataStore 设置
└── ui
    ├── chat       对话页 + ChatViewModel
    ├── home       首页（含侧边栏）
    ├── settings   设置 / 外观 / 人设
    ├── components 输入面板、气泡、Markdown、头像等
    └── theme      配色与主题
```

## 说明

- API Key 只保存在本机 DataStore，不经过任何第三方服务器。
- 本项目未包含账号/登录体系，所有数据（会话、图片、设置）都在本机。
- 参考文档：<https://api-docs.deepseek.com/zh-cn/guides/build_a_chatbot>
