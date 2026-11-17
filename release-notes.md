## RanaSeek v1.0.0

一个 Android 上的 DeepSeek 全家桶客户端。UI 参考 DeepSeek 移动端风格，**自行实现**；API Key 由用户自己填写，直连 DeepSeek 开放平台。

> ⚠️ 非官方第三方客户端，与 DeepSeek（深度求索）无任何官方关联。

### 功能

- **流式对话**：Messages API（`/anthropic/v1/messages`）+ SSE，逐字输出、可随时停止
- **深度思考**：思考过程时间线（思考/搜索/浏览合并展示，可折叠），带耗时
- **联网搜索**：`deepseek_web_search` / `deepseek_read_url`，正文带引用与来源链接
- **图片理解**：相册多选图片 → 压缩 JPEG → base64 内联
- **文件解析**：PDF/Word 上传到 Files API，以 document 块引用
- **语音输入**：录音 → `/v1/tools/asr` → 文本填入
- **朗读**：`/v1/tools/tts` → 播放，可停止（缓存复用）
- **会话管理**：置顶 / 重命名 / 多选删除 / KMP 搜索历史
- **压缩上下文**：整段对话总结成摘要，UI 保留历史、请求只带摘要
- **Token 统计**：每条回答底部显示输入/输出/思考/缓存
- **外观**：浅色/深色/跟随系统、聊天背景（裁剪+虚化）、纯色/亚克力气泡、亚克力局部磨砂（可调模糊/底色/饱和度）
- **输入框**：Markdown 实时预览开关、多行输入限高滚动、横滑模式按钮

### 安装

1. 下载 `app-release.apk` 并安装（覆盖安装保留已有机记录）
2. 打开 App → 侧边栏头像 → **设置 → API Key** → 填入你的 `sk-...` → 测试连接 → 保存
3. 可选：全局人设、外观设置

### 数据说明

- API Key、会话记录、人设、背景图**只保存在本地设备**
- 会话存于 `files/sessions.json`，覆盖保存前自动备份为 `sessions.backup.json`
- 除你自己配置的 DeepSeek API 外，不会上传任何数据

### 构建

JDK 21 + Gradle 9.9.1 + Android SDK (compileSdk 37)。

```bash
gradle assembleRelease
```

### License

MIT
