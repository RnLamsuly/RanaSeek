# DeepSeek 全家桶应用 API —— Android 客户端集成规格（Kotlin + Jetpack Compose）

> 依据 DeepSeek 官方文档中文站（api-docs.deepseek.com/zh-cn/）整理。文中 JSON 均为**文档示例**，已注明。
> 文档未覆盖处统一写「文档未说明」，不做推测。整理时间：2026-11-17。

## 1. 基础信息：端点、鉴权、超时

### 1.1 端点
| 协议 | 端点 | 来源 |
| --- | --- | --- |
| Messages（Anthropic 格式） | `POST https://api.deepseek.com/anthropic/v1/messages`（兼容路径 `/anthropic/messages`；SDK `base_url` 用 `https://api.deepseek.com/anthropic`） | [build_a_chatbot](https://api-docs.deepseek.com/zh-cn/guides/build_a_chatbot) · [create-message](https://api-docs.deepseek.com/zh-cn/api/create-message) |
| Responses | `POST https://api.deepseek.com/responses` | [build_a_chatbot](https://api-docs.deepseek.com/zh-cn/guides/build_a_chatbot) |
| Chat Completions | `POST https://api.deepseek.com/chat/completions`（beta 续写 `/beta/chat/completions`） | 同上 · [create-chat-completion](https://api-docs.deepseek.com/zh-cn/api/create-chat-completion) |
| 模型列表 | `GET https://api.deepseek.com/models` | [list-models](https://api-docs.deepseek.com/zh-cn/api/list-models) |
| 上传文件 | `POST https://api.deepseek.com/files` | [create-file](https://api-docs.deepseek.com/zh-cn/api/create-file) |
| TTS | `POST /v1/tools/tts`；流式 `wss://api.deepseek.com/v1/tools/tts` | [tts](https://api-docs.deepseek.com/zh-cn/guides/tts) |
| ASR | `POST https://api.deepseek.com/v1/tools/asr` | [asr](https://api-docs.deepseek.com/zh-cn/guides/asr) |

### 1.2 鉴权
- Messages API：**同时**支持 `x-api-key: <KEY>` 与 `Authorization: Bearer <KEY>`；两者都给时**优先 `Authorization`**（[create-message](https://api-docs.deepseek.com/zh-cn/api/create-message)）。
- 其余端点（Chat Completions / Responses / `/models` / `/files` / TTS / ASR）：`Authorization: Bearer <API_KEY>`。
- `anthropic-version`（如 `2023-06-01`）：SDK 自动发送，服务端目前忽略。
- `anthropic-beta: files-api-2025-04-14`：**仅在**用 `source.type=file` 引用已上传文件时必须带。

### 1.3 模型
- 推荐 `deepseek-flash`（支持图片输入）；另有 `deepseek-v4-pro`（**不支持**图片输入）。
- Messages API 接受 Claude 别名：`claude-opus*`→`deepseek-v4-pro`，其它 `claude*`→`deepseek-flash`（响应 `model` 保留传入名）。来源：[create-message](https://api-docs.deepseek.com/zh-cn/api/create-message)、[vision](https://api-docs.deepseek.com/zh-cn/guides/vision)

### 1.4 超时（文档给出的硬性时限）
| 场景 | 时限 | 来源 |
| --- | --- | --- |
| 文档上传 | **120 秒**内完成，否则 `408` | [document_parsing](https://api-docs.deepseek.com/zh-cn/guides/document_parsing) |
| 文档解析 | 提交后 **120 秒**，超时 `504`；**解析完成才返回响应**，故客户端与代理超时应设为「上传+解析」总时长 | 同上 |
| 单次对话请求 | 文档示例统一 60 秒（`--max-time 60` / `timeout=60.0`） | [create-message](https://api-docs.deepseek.com/zh-cn/api/create-message) 等 |
| ASR 上传 | **10 分钟**内完成 | [asr](https://api-docs.deepseek.com/zh-cn/guides/asr) |
| 会话标题 | 回答结束后最多再等 **5 秒** | [conversation_title](https://api-docs.deepseek.com/zh-cn/guides/conversation_title) |
| 流式保活 | 无新内容时发 `: keep-alive` 注释（读超时按此设置） | [web_tools](https://api-docs.deepseek.com/zh-cn/guides/web_tools) |

## 2. 三协议差异与选择
三种协议都提供联网搜索、文件输入、阅读链接、安全风控、标题；差异只在报文形态（[build_a_chatbot](https://api-docs.deepseek.com/zh-cn/guides/build_a_chatbot)、[web_tools](https://api-docs.deepseek.com/zh-cn/guides/web_tools)）。

| 维度 | Messages（推荐） | Responses | Chat Completions |
| --- | --- | --- | --- |
| 内容块 | `content` 块数组，逐块 SSE | `output` item 列表 | `delta.content`/`delta.reasoning_content` + 扩展字段 |
| 思考 | `thinking` 块 + `signature` | `reasoning` item（`reasoning_text`，不返回 `encrypted_content`） | `reasoning_content` |
| 联网工具 | `server_tool_use` / `web_search_tool_result` / `web_fetch_tool_result` | `web_search_call` item（`action`/`sources`/`page`） | `message.server_tool_calls` + `message.annotations` |
| 引用 | `text.citations` + `citations_delta` | `output_text` 的 `url_citation` | `annotations`（`url_citation`，`start_index`/`end_index` 按 Unicode 码点） |
| 流结束 | `message_stop`，**不发 `data: [DONE]`** | 事件流 | `data: [DONE]` |
| 拒答 | `stop_reason:"refusal"`+`stop_details` | `status:"incomplete"`+`reason:"content_filter"` | `finish_reason:"content_filter"` |
| 继续生成 | 支持（末条 assistant 结尾） | **不支持** | 仅 beta + `prefix:true` |
| `latest_reminder` | 支持 | **不支持** | 支持 |

**选择建议**：需要展示思考折叠、搜索卡片、引用角标并原样回传块的 Chatbot → **Messages API 最贴合**（其块结构与网页端 UI 一一对应，见 `build_a_chatbot`「渲染响应」表）；已有 OpenAI 体系则用 Chat Completions，但引用要按 `annotations` 区间自行切词。

### 2.1 会被忽略的参数（勿依赖）
声明 `deepseek_web_search`/`deepseek_read_url`、`safety_check=high` 或开启 `title` 时（[build_a_chatbot](https://api-docs.deepseek.com/zh-cn/guides/build_a_chatbot)）：
- Messages：`temperature`、`top_p`、`top_k`、`stop_sequences`、`thinking.budget_tokens`、`preserve_thinking`、`tool_choice.disable_parallel_tool_use`、`metadata`（`user_id` 除外）
- Responses：`temperature`、`top_p`、`reasoning.summary`、`text.verbosity`、`include`、`max_tool_calls`、`truncation`、`metadata`、`parallel_tool_calls`、`store`
- Chat Completions：`temperature`、`top_p`、`frequency_penalty`、`presence_penalty`、`stop`、`seed`、`n`、`preserve_thinking`、`parallel_tool_calls`

`max_tokens`（Responses 为 `max_output_tokens`）照常生效，限制的是**一轮中每一步推理**的输出长度，不是整轮总量。

## 3. Messages API 请求要点
（来源：[create-message](https://api-docs.deepseek.com/zh-cn/api/create-message)）
- `model`、`messages`（≥1 条）必填；API **无状态**，每轮传完整历史；系统提示用顶层 `system`（字符串 / 文本块数组 / `null`）。
- `max_tokens`：HTTP 请求可选（SDK 必填），含思考 token；超上限被截到上限；输入+输出不能超过上下文窗口。
- `stream: true` 返回带事件名 SSE，以 `message_stop` 结束，**不发 `data: [DONE]`**。
- `stop_sequences`：≤16 个、每个 ≤512 字节（UTF-8），命中时 `stop_reason=stop_sequence`。
- `metadata.user_id`：仅支持 `user_id`，建议英文/数字/`_`/`-`，≤512 字节，不含隐私。
- 兼容但忽略：`container`、`mcp_servers`、`service_tier`、`top_k`。错误结构：`{"type":"error","error":{"type","message"}}`。

文档示例（流式 + 联网工具 + 安全 + 标题）：
```json
{"model": "deepseek-flash", "thinking": {"type": "enabled"}, "stream": true,
 "tools": [{"type": "deepseek_web_search", "name": "deepseek_web_search"},
           {"type": "deepseek_read_url", "name": "deepseek_read_url"}],
 "safety_check": {"level": "high"}, "title": {"enabled": true},
 "messages": [{"role": "user", "content": "DeepSeek V4.1 有哪些更新？"}]}
```

## 4. 流式 SSE 事件与 delta 类型
（来源：[create-message](https://api-docs.deepseek.com/zh-cn/api/create-message)、[web_tools](https://api-docs.deepseek.com/zh-cn/guides/web_tools)）**正常事件名与 JSON `type` 相同**：

| 事件 | 关键字段 |
| --- | --- |
| `message_start` | `message`（`id/type/role/model/content/stop_reason=null/stop_sequence/usage`） |
| `content_block_start` | `index`（从 0）、`content_block`（完整块） |
| `content_block_delta` | `index`、`delta`（见下表） |
| `content_block_stop` | `index` |
| `message_delta` | `delta`（`stop_reason`、`stop_sequence`、可选 `stop_details`、可选 `title`）、`usage`（整轮累计） |
| `message_stop` | 正常结束标志 |
| `ping` | 心跳，忽略 |

**异常**：生成失败以 `error` 事件结束（`{"type":"error","error":{...},"usage":{...}}`），之后**没有** `message_delta`/`message_stop`；超时可能收到一条**无事件名**、只含 `error` 的 data 事件；流开始后的错误也可能直接断连。**未收到 `message_stop` 即为不完整**。

| `delta.type` | 字段 | 含义 |
| --- | --- | --- |
| `text_delta` | `text` | 追加到当前 text 块 |
| `thinking_delta` | `thinking` | 追加到当前 thinking 块 |
| `signature_delta` | `signature` | 思考块完成时的签名 |
| `input_json_delta` | `partial_json` | 工具入参 JSON 片段，须在 `content_block_stop` 后拼接解析 |
| `citations_delta` | `citation`（`web_search_result_location`） | 给当前 text 块追加一条引用 |

文档示例（节选）：
```text
message_start
content_block_start index:0 {"type":"thinking",...} → thinking_delta… → signature_delta → content_block_stop
content_block_start index:1 {"type":"server_tool_use",...} → content_block_stop
content_block_start index:2 {"type":"web_search_tool_result",...} → content_block_stop
content_block_start index:15 {"type":"text","text":""} → text_delta… → citations_delta… → content_block_stop
message_delta {"delta":{"stop_reason":"end_turn","stop_sequence":null,"title":"..."},"usage":{...}}
message_stop
```
实现要点（文档原文）：`server_tool_use` 与两种结果块的内容**一次性**出现在 `content_block_start`，不分段下发；开启思考时 `message_start` 后立即开一个可能为空的 thinking 块；`message_start.usage` 只含第一步，**最终用量以 `message_delta` 为准**（累计值）；text 块带引用后正文另起新块 → 角标按「块+块内偏移」维护。

## 5. 内容块 JSON 结构（Messages）
（来源：[web_tools](https://api-docs.deepseek.com/zh-cn/guides/web_tools)、[create-message](https://api-docs.deepseek.com/zh-cn/api/create-message)）

**5.1 `thinking`**：`{"type":"thinking","thinking":"思考文本","signature":"不透明签名"}`；`signature` 回传须保留（输入签名目前不校验）。

**5.2 `server_tool_use`**

| 字段 | 含义 |
| --- | --- |
| `type` | `"server_tool_use"` |
| `id` | 调用 ID（`srvtoolu_...`），与结果块 `tool_use_id` 对应 |
| `name` | `deepseek_web_search`（search/open_page/find_in_page）或 `deepseek_read_url`（read_link） |
| `input.action` | `search` / `open_page` / `find_in_page` / `read_link`；其他键：`queries`、`url`、`pattern` |
| `caller` | 固定 `{"type":"direct"}` |
| `kind` | `search`、`deep_search`、`read_link`、`image_search`、`deep_image_search`，**回传原样保留** |

文档示例：`{"type":"server_tool_use","id":"srvtoolu_s_00_3f2a9c...","name":"deepseek_web_search","input":{"action":"search","queries":["DeepSeek V4.1"]},"caller":{"type":"direct"},"kind":"search"}`

**5.3 `web_search_tool_result`**（文档示例，压缩排版）
```json
{"type":"web_search_tool_result","tool_use_id":"srvtoolu_s_00_3f2a9c...","kind":"search",
 "content":[{"type":"web_search_result","title":"...","url":"https://example.com/news",
  "encrypted_content":"...","page_age":"2026-01-02T03:04:05+00:00","snippet":"...",
  "site_name":"Example News","site_icon":"https://cdn.deepseek.com/site-icons/example.com"}]}
```
- `content` 可为空数组；失败时 `[{"type":"web_search_tool_result_error","error_code":...}]`，`error_code` ∈ `invalid_tool_input`/`unavailable`/`max_uses_exceeded`/`too_many_requests`/`query_too_long`/`request_too_large`。
- `snippet`/`site_name`/`site_icon` 为展示扩展字段（可能 `null`，**回传被忽略**）；`page_age` 为 RFC 3339 UTC 或 `null`。
- 图片搜索结果多 `image_url`（原站地址，仅展示，可能不存在/不可访问），`queries` 为空数组。

**5.4 `web_fetch_tool_result`**（文档示例，压缩排版）
```json
{"type":"web_fetch_tool_result","tool_use_id":"srvtoolu_rl_01_9c1d...","kind":"read_link",
 "content":{"type":"web_fetch_result","url":"https://example.com/a",
  "content":{"type":"document","title":"页面标题",
   "source":{"type":"text","media_type":"text/plain","data":""}},"published_at":"2026-01-01"},
 "signature":"q3J0b1RmZ1c4cE1x..."}
```
- **网页正文不返回**，`data` 恒 `""`；`read_link` 正文加密在 `signature`（深度思考搜索打开的网页**没有** `signature`）。
- 块内 `url`/`title`/`data`/`published_at` 仅展示，回传前修改无影响。
- 失败时 `content` 为 `{"type":"web_fetch_tool_result_error","error_code":...}` 且**不带** `signature`；`error_code` ∈ `url_not_accessible` / `url_not_allowed`（含未通过内容安全）/ `unavailable`（服务不可用、深度搜索打开失败、调用未完成）。

**5.5 `text` / `citations` / `citations_delta`**
```json
{"type":"text","text":"上海今天多云。","citations":[{"type":"web_search_result_location",
 "url":"https://example.com/weather","title":"...","encrypted_index":"","cited_text":"上海今天多云。"}]}
```
```json
{"type":"citations_delta","citation":{"type":"web_search_result_location","url":"https://api-docs.deepseek.com/news/news261017/","title":"...","encrypted_index":"","cited_text":"..."}}
```
`type` 恒为 `web_search_result_location`；`encrypted_index` 恒 `""`；`title` 可能 `null`；`cited_text` 是**当前块截至该引用处的回答文本**，不是来源摘录（勿当原文高亮）；同一块内同一 URL 最多一次。

## 6. 多轮对话回传规则（关键）
（来源：[web_tools](https://api-docs.deepseek.com/zh-cn/guides/web_tools)「多轮对话」、[create-message](https://api-docs.deepseek.com/zh-cn/api/create-message)）
1. 每轮带完整历史，**把上一轮 assistant 回复原样回传**，保留 `kind` 与 `signature`。
2. `encrypted_content`（搜索结果加密内容）**要原样保留**；`snippet`/`site_name`/`site_icon` 会被忽略。
3. `web_fetch_tool_result.signature` **必须原样回传**，服务端据此把网页拼回该轮用户消息末尾；无有效 `signature` 时网页不拼回，但请求仍正常执行。
4. 搜索结果、打开的网页、历史轮次思考**不进后续上下文**，只保留回答正文。
5. `citations` 在请求中传入时**被忽略** → 角标需客户端本地持久化（存「文本块→引用列表」映射用于渲染）。
6. 带联网工具块的历史消息，可发给**没有**声明这些工具的请求。
7. 工具调用后继续：回传**完整** assistant 内容（含思考块与工具调用块）+ 对应 `tool_result`；服务端可依调用 ID 恢复遗漏思考，恢复不了返回 `400`。
8. 继续生成：以未完成 `assistant` 结尾，响应只含新生成部分；**用过联网工具的回复不能续写**（`400`），需重发。
9. 历史中 `file_id` 文档内容**完整保留**；超长返回 `400`，不静默截断。

回传示例（文档示例，压缩排版）：
```json
{"role":"assistant","content":[
 {"type":"server_tool_use","id":"srvtoolu_rl_00_0efde247...","name":"deepseek_read_url",
  "input":{"action":"read_link","url":"https://example.com"},"caller":{"type":"direct"},"kind":"read_link"},
 {"type":"web_fetch_tool_result","tool_use_id":"srvtoolu_rl_00_0efde247...",
  "content":{"type":"web_fetch_result","url":"https://example.com","content":{"type":"document",
   "source":{"type":"text","media_type":"text/plain","data":""},"title":"Example Domain"},"published_at":null},
  "kind":"read_link","signature":"tQU8k+8Ko7MY5XDAvJd0..."},
 {"type":"text","text":"example.com 是专门留给文档示例使用的域名。"}]}
```
> 存储建议：assistant 原始块 JSON 整段落库，展示层另存渲染模型，避免回传丢字段。

## 7. 工具开关、安全审核、标题、元信息
**7.1 联网工具声明**（[web_tools](https://api-docs.deepseek.com/zh-cn/guides/web_tools)）
```jsonc
// Messages：type + name 都要写
"tools": [{"type":"deepseek_web_search","name":"deepseek_web_search"},
          {"type":"deepseek_read_url","name":"deepseek_read_url"}]
// Responses / Chat Completions：只写 type
"tools": [{"type":"deepseek_web_search"},{"type":"deepseek_read_url"}]
```
- 是否联网由模型判断，客户端**不执行**工具；搜索与抓取都在服务端。
- `tool_choice` **不能指定**这两个工具（`400: tool_choice: deepseek_web_search cannot be forced`）；`none` 关闭全部工具（含阅读链接）。
- 同一工具重复声明、与其它服务端工具同用、带 `allowed_domains`/`blocked_domains`/`user_location` → 均 `400`。
- 联网搜索（关思考）与深度思考搜索（开思考）共用 `deepseek_web_search`，`kind` 分别 `search`/`deep_search`；深度搜索最多约 20 次工具调用。
- 阅读链接：只识别**最后一条**用户消息中的 URL，去重，最多前 10 个；开思考且声明了 `deepseek_web_search` 时走深度思考搜索、不单独阅读链接。

**7.2 `safety_check`**（[safety](https://api-docs.deepseek.com/zh-cn/guides/safety)）：`{"safety_check":{"level":"high"}}`
- `level` 仅 `low`/`high`（`low`＝不传）；非法值 `400`。可与联网工具同用或单独用（单独用时 `tool_choice` 只能 `auto`/`none`）。
- 审核输入（含 `tool_result`）与输出；输入未过 → `400 Content Exists Risk`。
- 开启后**输出结束不立即关连接**：还会对完整输出再审核一次，连接保持打开，之后仍可能以拒答结束 → 客户端要支持「已流式显示后改为拒答」。

**7.3 `title`**（[conversation_title](https://api-docs.deepseek.com/zh-cn/guides/conversation_title)）：`{"title":{"enabled":true}}`
- 位置：Messages 非流式顶层 `title`、流式 `message_delta.delta.title`；Chat Completions `message.title` / 最后 chunk 的 `delta.title`；Responses `response.title`。
- 不一定返回（拒答/出错时没有），需兜底；建议只在首次请求开启；不计费；最多等 5 秒。

**7.4 `latest_reminder`**（[latest_reminder](https://api-docs.deepseek.com/zh-cn/guides/latest_reminder)）
- `{"role":"latest_reminder","content":"2026-11-09,星期一,新加坡,Web,中文"}`；格式 `<date>,<weekday>,<location>,<platform>,<language>`，`location` 可空可含逗号，`platform` ∈ `App`/`Web`。
- 必须**紧跟其所属 `user` 消息之前**（否则 `400`），不能插在 `tool_use` 与其 `tool_result` 之间；`content` 必须字符串（否则 `422`）。
- Messages 与 Chat Completions 支持，**Responses 不支持**；不回传旧的不报错，但降低缓存命中。

## 8. 思考模式字段
（来源：[thinking_mode](https://api-docs.deepseek.com/zh-cn/guides/thinking_mode)、[create-message](https://api-docs.deepseek.com/zh-cn/api/create-message)）

| 用途 | Messages | Chat Completions | Responses |
| --- | --- | --- | --- |
| 开关 | `thinking.type`: `enabled`/`disabled`/`adaptive`（按 enabled 处理） | `thinking.type`: `enabled`/`disabled` | `reasoning.effort:"none"` 关闭 |
| 强度 | `output_config.effort` | `reasoning_effort` | `reasoning.effort` |

- **默认开启思考，默认强度 `high`**；显式 `thinking.type` 优先于 effort；`budget_tokens` 兼容字段、**被忽略**。
- 档位：`none`/`minimal`/`low`/`medium`/`high`/`xhigh`/`ultra`/`max`（Messages 枚举含全部）；Chat Completions 枚举 `none/low/high/max`。
- 映射：`minimal→low`、`medium→high`、`xhigh→high`、`ultra→max`。
- 思考模式不支持 `temperature`、`presence_penalty`、`frequency_penalty`（不报错但不生效）；`top_p`：`deepseek-flash` 思考模式仅 0.95–1.0 生效（<0.95 按 0.95），非思考恒 1.0；`deepseek-v4-pro` 思考模式忽略传入值。
- `reasoning_content` 回传：请求**带 `tools`** 时必须回传历史思考（否则 `400`）；**不带 `tools`** 时忽略、不拼入上下文。

## 9. 模型列表与能力字段
`GET /models` + `Authorization: Bearer`（[list-models](https://api-docs.deepseek.com/zh-cn/api/list-models)）

| 字段 | 含义 |
| --- | --- |
| `id`/`object`/`owned_by` | 必填；`object` 恒 `"model"` |
| `name` | 展示名，如 `DeepSeek-V4.1-Flash` |
| `context_window` | 上下文窗口（输入+输出）总容量，示例 `1048576` |
| `max_output_tokens` | 单次输出上限（`max_tokens` 可接受最大值），示例 `393216` |
| `input_modalities` | `["text","image"]`；`deepseek-v4-pro` 为 `["text"]` |
| `output_modalities` | `["text"]` |
| `effort.supported_levels` | 开思考时可用档位（展示顺序；不含关闭用的 `none`） |
| `effort.default_level` | 未指定强度时的默认档位，必属 `supported_levels` |
| `api_capabilities.anthropic_messages.system_prompt_update` | `leading-only`（只有开头 system 生效）/ `in-history`（历史最新 system 替代此前全部） |

文档示例（节选，压缩排版）：
```json
{"object":"list","data":[{"id":"deepseek-flash","object":"model","owned_by":"deepseek",
 "name":"DeepSeek-V4.1-Flash","context_window":1048576,"max_output_tokens":393216,
 "input_modalities":["text","image"],"output_modalities":["text"],
 "effort":{"supported_levels":["low","high","max"],"default_level":"high"},
 "api_capabilities":{"anthropic_messages":{"system_prompt_update":"in-history"}}}]}
```
> 客户端做法：按 `input_modalities` 决定是否显示上传图片，按 `effort.supported_levels` 生成强度选择器，按 `context_window` 估算历史截断阈值。

## 10. 文件上传与文件输入
（来源：[document_parsing](https://api-docs.deepseek.com/zh-cn/guides/document_parsing)、[create-file](https://api-docs.deepseek.com/zh-cn/api/create-file)、[vision](https://api-docs.deepseek.com/zh-cn/guides/vision)）

**10.1 上传**：`POST /files`，`multipart/form-data`，`purpose` + `file`；**`purpose` 必须在 `file` 字段之前**（否则 `400`）。

| `purpose` | 用途 |
| --- | --- |
| `user_data` | 图片；纯文本/md/csv/json/代码；XLSX/XLS/EPUB/MOBI（转 Markdown）。**这类上传不计费** |
| `text_recognition` | 文档只提取文字，图表照片被丢弃（按页计费） |
| `rich_layout_recognition` | 文字 + 裁出的图（图表/照片/示意图）按原位置交给模型（按页计费） |

示例（文档示例）：`curl https://api.deepseek.com/files -H "Authorization: Bearer <KEY>" -F purpose="text_recognition" -F file="@report.pdf"` → 响应 `{id:"file-api-...",object:"file",bytes,created_at,filename,purpose[,expires_at]}`。
可选过期：`expires_after[anchor]=created_at` + `expires_after[seconds]`（3600–2592000）；不传则永久有效。
限制：PDF/Word/PPT/图片（按内容判断）、≤100 MiB（超 `413`）、≤1000 页、上传 ≤120 秒（`408`）、解析 ≤120 秒（`504`）、每账号并发 15（`429`）；上传失败不保存不计费；**原始文件与解析结果都不支持下载**。

**10.2 对话中引用（Messages）**，需请求头 `anthropic-beta: files-api-2025-04-14`：
```json
{"role":"user","content":[{"type":"document","source":{"type":"file","file_id":"file-api-xxxxxxxxxxxxxxxx"}},
 {"type":"text","text":"总结这份文档的要点。"}]}
```
- 每请求最多引用 **20 个文档**、文字合计 ≤8 MiB；同一 `file_id` 重复引用按多次计；超出 `400`。
- 需解析的文档**只能**用 `file_id`（不能传内容或 URL）；这类文件**当图片用会 `400`**。
- 内联文本（无需上传）：`{"type":"document","source":{"type":"text","media_type":"text/plain","data":"文档内容"}}`。

**10.3 图片输入**
- Messages：`{"type":"image","source":{"type":"base64","media_type":"image/jpeg","data":"<BASE64>"}}`，或 `source.type=url`，或 `source.type=file`（带 `anthropic-beta`）。
- Chat Completions：`{"type":"image_url","image_url":{"url":"...","detail":"low|high|original|auto"}}`，或 `{"type":"file","file_id":"..."}` / `{"type":"file","file_data":"data:image/jpeg;base64,...","filename":"image.jpg"}`（二者互斥）。
- 限制：JPEG/PNG/GIF/WebP；URL ≤8192 字符；请求体 ≤48 MiB；单图 ≤32 MiB（base64/URL）或 ≤64 MiB（`file_id`）；单请求 ≤600 张；单边 ≤8192 像素；每张图 token 上限约 1024。
- `assistant` 消息带图 → `400`；`deepseek-v4-pro` 不支持图片。

## 11. 拒答（refusal）与错误处理
（来源：[safety](https://api-docs.deepseek.com/zh-cn/guides/safety)、[create-message](https://api-docs.deepseek.com/zh-cn/api/create-message)）

**11.1 拒答（Messages）**：`{"stop_reason":"refusal","stop_details":{"type":"refusal","category":null,"explanation":null}}`
- 流式：若拒答前没下发过 `text` 块，会在 `message_delta` 前**补发一个空 text 块**；非流式：思考与回答被清空，`content` 保留已执行工具块 + 一个空 text 块。
- 图片未过审时 `stop_details.explanation` 指出位置，如 `The following images were blocked by content moderation: messages.1.content.0`。
- 其他协议：Chat Completions `finish_reason:"content_filter"`、`content:""`、无 `annotations`、保留 `server_tool_calls`；Responses `status:"incomplete"` + `reason:"content_filter"`，`output` 只留 `web_search_call`。
- **已流式下发的文本无法撤回** → 删掉已展示回答、换成自己的拒答文案；拒答原因不返回；未开 `safety_check` 但声明了联网工具也可能被拒答；拒答前 token 照常计费。

**11.2 `stop_reason`（Messages）**

| 值 | 含义 |
| --- | --- |
| `end_turn` | 完成 |
| `max_tokens` | 达最大长度被截断，或搜索/阅读链接后上下文用尽 |
| `stop_sequence` | 命中停止序列 |
| `tool_use` | 需客户端执行工具 |
| `refusal` | 内容审核拒答 |

**11.3 错误**：`{"type":"error","error":{"type":"invalid_request_error","message":"messages: at least one message is required"}}`
`error.type` 与 HTTP 状态：`invalid_request_error`(400/422)、`authentication_error`(401)、`billing_error`(402)、`permission_error`(403)、`not_found_error`(404)、`request_too_large`(413)、`rate_limit_error`(429)、`timeout_error`(408/504)、`overloaded_error`(503)、`api_error`(500 及其他)。
**生成失败**（HTTP 已是 200）：Messages 以 `error` 事件结束、无 `message_delta`/`message_stop`；非流式响应体为 `{"type":"error","error":{...},"usage":{...}}`。**状态码 200 时 SDK 不抛异常，必须自检响应体 `error`**；失败前已执行的推理/搜索/阅读照常计费。
常见 400：`Content Exists Risk`（输入命中审核）、工具重复声明、`tool_choice` 强制联网工具、`safety_check.level` 非法、续写使用过联网工具的回复。

## 12. TTS / ASR
**12.1 TTS**（[tts](https://api-docs.deepseek.com/zh-cn/guides/tts)）
- 整段：`POST https://api.deepseek.com/v1/tools/tts`，JSON：`text`（必填，≤10000 计费字符，请求体 ≤64 KiB）、`voice_id`（`stella` 女声默认 / `tide` 男声）、`format`（`wav` 默认 / `pcm`）、`filter_markdown`（默认 `true`）；音频边合成边返回，`pcm` 为 24 kHz/16 bit/单声道。
- 流式：`wss://api.deepseek.com/v1/tools/tts`，查询参数 `voice_id`/`format`（默认 `pcm`，亦可 `opus`，无容器）/`filter_markdown`；鉴权用 `Authorization` 头或子协议 `Sec-WebSocket-Protocol: tts, bearer.<API_KEY>`。
- 文本帧 `{"event":"text","text":"..."}` → `{"event":"finish_text"}`；音频走二进制帧；结束帧 `{"event":"finish","code":"success|invalid_input|content_filter|input_timeout|service_error","message":"..."}`。
- 限制：单次 ≤30 分钟、≤10000 计费字符、60 秒无文本结束、服务端每 25 秒 ping；不支持调语速、不支持中断后续传。
- 错误：`400`（无可读文本/未过审 `Content Exists Risk`）、`429`、`503`、`504`；已开始返回音频后失败会断流，应视为失败。计费：每 10000 字符 3 元（限时 1.5 元），汉字算 2 字符。

**12.2 ASR**（[asr](https://api-docs.deepseek.com/zh-cn/guides/asr)）
- `POST https://api.deepseek.com/v1/tools/asr`，`multipart/form-data` 的 `file` 字段；**一次发送完整音频，不支持流式**。
- 格式：16 bit PCM WAV（8000/16000/22050/24000/32000/44100/48000 Hz）或 Ogg Opus；单/双声道（双声道只识别左声道）；≤8 MiB、≤120 秒、上传 ≤10 分钟。
- 响应（文档示例）：`{"request_id":"...","text":"上海今天天气怎么样？","duration_ms":3200}`
- 错误：`400`（格式不符/缺 `file`）、`408`、`413`、`429`、`503`、`504`；计费每小时 0.78 元（限时 0.39 元），按秒、不足 1 秒按 1 秒；语种自动识别，无参数。

## 13. 计费相关字段（usage）
（来源：[web_tools](https://api-docs.deepseek.com/zh-cn/guides/web_tools)、[create-message](https://api-docs.deepseek.com/zh-cn/api/create-message)、[create-chat-completion](https://api-docs.deepseek.com/zh-cn/api/create-chat-completion)）

| Messages 字段 | 含义 |
| --- | --- |
| `input_tokens` | 未命中缓存的输入 token |
| `cache_read_input_tokens` | 命中缓存的输入 token |
| `cache_creation_input_tokens` | **恒为 0** |
| `output_tokens` | 输出 token（含思考）；`message_delta` 中为**累计值** |
| `output_tokens_details.thinking_tokens` | 其中的思考 token（开思考时返回） |
| `output_tokens_details.safety_tokens` | 安全审核 token（仅 `safety_check=high`，已含在 `output_tokens`） |
| `service_tier` | 恒 `"standard"` |
| `server_tool_use.web_search_requests` / `web_fetch_requests` | 结果块数量（含错误块），**仅展示，不是计费次数** |
| `server_tool_use.tool_search` / `tool_fetch` / `tool_image_search` | **计费**的搜索 / 抓取 / 图片搜索次数 |

Chat Completions：`prompt_tokens`（=`prompt_cache_hit_tokens`+`prompt_cache_miss_tokens`）、`completion_tokens`、`total_tokens`、`prompt_tokens_details.cached_tokens`、`completion_tokens_details.reasoning_tokens`/`.safety_tokens`、`server_tool_use.{tool_search,tool_fetch,tool_image_search}`。
工具计费细则：网页搜索按一次调用返回结果包含的**查询词条数**计（失败/无结果不计）；图片搜索每张图 1 次（框选区域每个另计）；阅读链接按成功抓取网页数计（含被替换为 `url_not_allowed`/`unavailable` 的网页）；深度搜索打开网页只在实际联网抓取时计费，页内查找不计费；安全审核 token 按输出 token 计费，输入被拦返回 400 时输入 token 仍按缓存命中价计费。

## 14. 客户端实现清单（由文档推导，非文档结论）
1. SSE 解析：聚合 `event:`/`data:`；`data` 可能是不带事件名的裸错误对象；`: keep-alive` 当心跳；**以 `message_stop` 判定完整**。
2. 存储：assistant 原始块 JSON（`signature`/`encrypted_content`/`kind`）与渲染模型分开存，回传用原始 JSON。
3. 渲染：按 `index` 维护块序列；thinking 可折叠；`server_tool_use` 与结果块按 `tool_use_id` 配对（顺序不保证）；text 块按 `citations_delta` 顺序插角标。
4. 拒答：收到 `refusal` 清空已展示回答并改用自定义拒答文案；注意 `safety_check=high` 的「流结束后仍可能审核」。
5. 上传：`purpose` 在 `file` 之前；上传/解析超时按 ≥240 秒设。
6. 首轮开 `title`，之后关闭；拿不到标题用首条用户消息前 N 字兜底。

### 文档未说明
- Android/Kotlin 官方 SDK：文档只给 curl、Anthropic Python SDK、OpenAI Python SDK 示例。
- 完整错误码总表（`quick_start/error_codes`）本次未抓取，本文只收录 Messages 侧 `error.type`↔HTTP 状态映射与已知 400 文案。
- TTS 语速控制、ASR 流式、文件下载：文档明确「不支持 / 不返回」，无对应字段。
