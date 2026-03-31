# PowerFlow 全行业场景解决方案设计

## 概述

本文档为 PowerFlow 工作流引擎设计全行业测试场景，覆盖电商、金融、企业办公、社交内容、物联网IoT、公共服务六大行业共25个核心业务流程。

**验证目标：**
- 全部10种节点类型的组合使用
- 业务流程可流转性
- 数据在节点间的正确传递
- 异常处理和错误恢复

---

## 一、电商场景（5个核心流程）

### 1.1 智能客服路由

**业务描述：** 用户输入一句话，LLM低温(<0.3)解析成结构化JSON，根据intent路由到不同业务模块。

**节点组合：**
```
DATA_INPUT → LLM_CALL → BRANCH → [HTTP×4] → LLM_CALL → END
```

**验证重点：** LLM低温JSON输出 + 条件路由

**详细设计：**

| 步骤 | 节点ID | 节点类型 | 配置 |
|------|--------|----------|------|
| 1 | user_input | DATA_INPUT | 接收用户query |
| 2 | intent_recognition | LLM_CALL | temperature=0.2, prompt提取intent |
| 3 | route | BRANCH | 4个分支: search/recommend/inquiry/order |
| 4a | search_node | HTTP_REQUEST | 搜索商品API |
| 4b | recommend_node | HTTP_REQUEST | 商品推荐API |
| 4c | inquiry_node | HTTP_REQUEST | 库存查询API |
| 4d | order_node | HTTP_REQUEST | 下单处理API |
| 5 | format_result | LLM_CALL | 格式化输出 |

**输入示例：** "我想买一双Nike运动鞋，42码，黑色"

**预期LLM输出：**
```json
{
  "intent": "search",
  "entities": {"brand": "Nike", "category": "运动鞋", "size": "42"}
}
```

**测试用例：**
| ID | 输入 | intent | 预期路由 |
|----|------|--------|----------|
| TC-EC-01 | "想买Nike鞋" | search | search_node |
| TC-EC-02 | "推荐适合我的" | recommend | recommend_node |
| TC-EC-03 | "这件有货吗" | inquiry | inquiry_node |
| TC-EC-04 | "我要下单" | order | order_node |

---

### 1.2 商品搜索

**业务描述：** 用户搜索商品，支持多条件筛选，返回结构化商品列表。

**节点组合：**
```
DATA_PROCESSING → HTTP_REQUEST → DATA_PROCESSING → LLM_CALL → END
```

**验证重点：** HTTP调用 + 数据过滤 + 格式化

**详细设计：**

| 步骤 | 节点ID | 节点类型 | 配置 |
|------|--------|----------|------|
| 1 | build_query | DATA_PROCESSING | 构建搜索参数 |
| 2 | search_api | HTTP_REQUEST | POST /products/search |
| 3 | filter | DATA_PROCESSING | 过滤无库存商品 |
| 4 | format | LLM_CALL | 格式化商品列表 |

**测试用例：**
| ID | 输入 | 预期结果 |
|----|------|----------|
| TC-ES-01 | "Nike运动鞋" | 返回Nike相关商品 |
| TC-ES-02 | "不存在的商品" | 返回空列表或替代品 |
| TC-ES-03 | 搜索接口超时 | 显示超时错误，提示重试 |

---

### 1.3 个性化推荐

**业务描述：** 基于用户画像和行为历史，推荐感兴趣的商品。

**节点组合：**
```
HTTP × 2(并行) → LLM_CALL → HTTP(详情) × N(并行) → DATA_PROCESSING → LLM_CALL
```

**验证重点：** 并行执行 + 结果聚合

**详细设计：**

| 步骤 | 节点ID | 节点类型 | 配置 |
|------|--------|----------|------|
| 1 | get_profile | HTTP(并行1) | GET /users/{id}/profile |
| 2 | get_behaviors | HTTP(并行2) | GET /users/{id}/behaviors |
| 3 | generate_recommend | LLM_CALL | 基于用户偏好生成推荐 |
| 4 | parallel_detail | HTTP(并行) | 获取每个推荐商品的详情 |
| 5 | filter_stock | DATA_PROCESSING | 过滤无货商品 |
| 6 | format_result | LLM_CALL | 生成推荐理由 |

**测试用例：**
| ID | 输入 | 预期结果 |
|----|------|----------|
| TC-ER-01 | 新用户无历史 | 返回热销商品 |
| TC-ER-02 | 有浏览历史用户 | 返回相似商品 |
| TC-ER-03 | 全部无货 | 返回空，提示无货 |

---

### 1.4 库存查询

**业务描述：** 实时查询商品库存，支持多SKU并发查询。

**节点组合：**
```
DATA_PROCESSING → PARALLEL → DATA_PROCESSING → END
```

**验证重点：** 批量并发 + 数据合并

**详细设计：**

| 步骤 | 节点ID | 节点类型 | 配置 |
|------|--------|----------|------|
| 1 | parse_skus | DATA_PROCESSING | 解析SKU列表 |
| 2 | parallel_query | PARALLEL | 并行查询库存和价格 |
| 2a | query_inventory | HTTP | 批量库存API |
| 2b | query_price | HTTP | 批量价格API |
| 3 | merge | DATA_PROCESSING | 合并库存价格数据 |

**测试用例：**
| ID | 输入 | 预期结果 |
|----|------|----------|
| TC-EI-01 | 单个SKU | 返回精确库存 |
| TC-EI-02 | 批量10个SKU | 并行返回，3秒内完成 |
| TC-EI-03 | SKU不存在 | 返回"商品不存在" |
| TC-EI-04 | 部分无库存 | 有库存返回数量，无库存返回0 |

---

### 1.5 下单创建

**业务描述：** 用户下单创建，包含库存校验、价格计算、订单创建。

**节点组合：**
```
HTTP → CONDITION → [HTTP, HTTP] → HTTP → HTTP → END
```

**验证重点：** 条件分支 + 库存校验 + 订单创建

**详细设计：**

| 步骤 | 节点ID | 节点类型 | 配置 |
|------|--------|----------|------|
| 1 | check_stock | HTTP_REQUEST | POST /inventory/reserve |
| 2 | condition | CONDITION | 库存 sufficient == true/false |
| 3a | calc_price | HTTP_REQUEST | POST /orders/calculate |
| 3b | insufficient | DATA_PROCESSING | 返回"库存不足" |
| 4 | create_order | HTTP_REQUEST | POST /orders |
| 5 | send_notification | HTTP_REQUEST | POST /sms/send |

**测试用例：**
| ID | 场景 | 库存 | 预期结果 |
|----|------|------|----------|
| TC-EO-01 | 正常下单 | 充足 | 订单创建成功，返回订单号 |
| TC-EO-02 | 库存不足 | 库存=0 | 提示库存不足，不创建订单 |
| TC-EO-03 | 库存不足 | 库存<数量 | 提示库存不足数量 |

---

## 二、金融场景（4个核心流程）

### 2.1 风控评估

**业务描述：** 对贷款申请进行多维度风控评估，自动通过或拒绝。

**节点组合：**
```
HTTP → LLM_CALL → CONDITION → [HTTP, HTTP] → END
```

**验证重点：** 多维度评分 + 自动决策

**详细设计：**

| 步骤 | 节点ID | 节点类型 | 配置 |
|------|--------|----------|------|
| 1 | fetch_credit | HTTP_REQUEST | 获取征信数据 |
| 2 | risk_score | LLM_CALL | 综合评估风险评分 |
| 3 | decision | CONDITION | score >= 0.7 通过 / < 0.7 拒绝 |
| 4a | approve | HTTP_REQUEST | 发送通过通知 |
| 4b | reject | HTTP_REQUEST | 发送拒绝通知 |

---

### 2.2 贷款审批

**业务描述：** 复杂贷款审批流程，包含材料验证、信用评估、额度计算。

**节点组合：**
```
DATA_INPUT → HTTP → LLM_CALL → CONDITION → HTTP → TRY_CATCH → END
```

**验证重点：** 长事务 + 异常回滚

**详细设计：**

| 步骤 | 节点ID | 节点类型 | 配置 |
|------|--------|----------|------|
| 1 | submit | DATA_INPUT | 接收申请材料 |
| 2 | verify_material | HTTP_REQUEST | 材料真实性验证 |
| 3 | credit_eval | LLM_CALL | 信用评估 |
| 4 | amount_cond | CONDITION | 金额 > 50万需人工 |
| 5 | auto_approve | HTTP_REQUEST | 自动审批通过 |
| 5a | manual_review | HTTP_REQUEST | 人工审批 |
| 6 | try_execute | TRY_CATCH | 异常处理：额度回滚 |

---

### 2.3 反欺诈检测

**业务描述：** 实时检测交易欺诈，多维度并行分析。

**节点组合：**
```
HTTP → PARALLEL(LIST) → DATA_PROCESSING → CONDITION → END
```

**验证重点：** 实时并行检测

**详细设计：**

| 步骤 | 节点ID | 节点类型 | 配置 |
|------|--------|----------|------|
| 1 | get_transaction | HTTP_REQUEST | 获取交易信息 |
| 2 | parallel_check | PARALLEL | 并行检测 |
| 2a | check_amount | DATA_PROCESSING | 金额异常检测 |
| 2b | check_frequency | DATA_PROCESSING | 频率异常检测 |
| 2c | check_location | DATA_PROCESSING | 位置异常检测 |
| 2d | check_device | DATA_PROCESSING | 设备指纹检测 |
| 3 | fraud_decision | CONDITION | 任一异常则拦截 |
| 4 | alert | HTTP_REQUEST | 发送告警 |

---

### 2.4 账户审核

**业务描述：** 用户注册账户审核，调用子工作流进行复杂验证。

**节点组合：**
```
LLM_CALL → CONDITION → [HTTP, HTTP] → SUBWORKFLOW → END
```

**验证重点：** 子工作流嵌套

**详细设计：**

| 步骤 | 节点ID | 节点类型 | 配置 |
|------|--------|----------|------|
| 1 | extract_info | LLM_CALL | 提取用户提交信息 |
| 2 | basic_check | CONDITION | 信息完整/缺失 |
| 3a | verify_identity | HTTP_REQUEST | 身份认证API |
| 3b | request_info | HTTP_REQUEST | 要求补充信息 |
| 4 | deep_verify | SUBWORKFLOW | 调用子工作流: 复杂材料审核 |

---

## 三、企业办公场景（4个核心流程）

### 3.1 审批流

**业务描述：** 多级审批流程，支持加急和自动通过。

**节点组合：**
```
DATA_INPUT → CONDITION → [HTTP, HTTP] → SUBWORKFLOW → END
```

**验证重点：** 多级审批 + 状态流转

**详细设计：**

| 步骤 | 节点ID | 节点类型 | 配置 |
|------|--------|----------|------|
| 1 | receive | DATA_INPUT | 接收审批申请 |
| 2 | urgent_check | CONDITION | urgent == true / false |
| 3a | level1_approve | HTTP_REQUEST | 一级审批(主管) |
| 3b | level1_auto | HTTP_REQUEST | 自动通过(金额小) |
| 4 | level2 | SUBWORKFLOW | 二级审批子工作流 |

---

### 3.2 数据报表

**业务描述：** 从多个数据源并行采集数据，汇总生成报表。

**节点组合：**
```
HTTP × 3(并行) → FOREACH → DATA_PROCESSING → LLM_CALL → END
```

**验证重点：** 大数据聚合 + 格式化

**详细设计：**

| 步骤 | 节点ID | 节点类型 | 配置 |
|------|--------|----------|------|
| 1 | parallel_fetch | HTTP(并行×3) | 采集销售/库存/用户数据 |
| 2 | process_items | FOREACH | 遍历每个数据项处理 |
| 3 | aggregate | DATA_PROCESSING | 汇总统计 |
| 4 | generate_report | LLM_CALL | 生成报表摘要 |

---

### 3.3 任务分发

**业务描述：** 将任务批量分发给多个执行者。

**节点组合：**
```
DATA_INPUT → FOREACH → HTTP(循环) → END
```

**验证重点：** 批量任务分发

**详细设计：**

| 步骤 | 节点ID | 节点类型 | 配置 |
|------|--------|----------|------|
| 1 | get_tasks | DATA_INPUT | 获取任务列表 |
| 2 | foreach_dispatch | FOREACH | 遍历任务 |
| 3 | dispatch_single | HTTP_REQUEST | 分发单个任务 |

---

### 3.4 会议安排

**业务描述：** 智能安排会议，调用日程API检测冲突。

**节点组合：**
```
LLM_CALL → CONDITION → [HTTP, HTTP] → LLM_CALL → END
```

**验证重点：** 自然语言处理 + 日程API

**详细设计：**

| 步骤 | 节点ID | 节点类型 | 配置 |
|------|--------|----------|------|
| 1 | parse_request | LLM_CALL | 提取会议信息 |
| 2 | check_available | CONDITION | 有空闲时间/无 |
| 3a | book_room | HTTP_REQUEST | 预定会议室 |
| 3b | notify_conflict | HTTP_REQUEST | 通知时间冲突 |
| 4 | confirm | LLM_CALL | 生成确认消息 |

---

## 四、社交内容场景（4个核心流程）

### 4.1 内容审核

**业务描述：** 对用户发布的内容进行审核，分级处理。

**节点组合：**
```
HTTP → LLM_CALL → CONDITION → [HTTP, HTTP, HTTP] → END
```

**验证重点：** 文本/图片审核 + 分级处理

**详细设计：**

| 步骤 | 节点ID | 节点类型 | 配置 |
|------|--------|----------|------|
| 1 | get_content | HTTP_REQUEST | 获取待审核内容 |
| 2 | audit | LLM_CALL | 审核判断: 合规/疑似/违规 |
| 3 | decision | CONDITION | 三级分类 |
| 4a | publish | HTTP_REQUEST | 直接发布 |
| 4b | manual_review | HTTP_REQUEST | 人工复审 |
| 4c | block | HTTP_REQUEST | 拦截并通知用户 |

---

### 4.2 用户画像

**业务描述：** 聚合多维度数据构建用户画像。

**节点组合：**
```
HTTP × 4(并行) → DATA_PROCESSING → LLM_CALL → END
```

**验证重点：** 多源数据聚合

**详细设计：**

| 步骤 | 节点ID | 节点类型 | 配置 |
|------|--------|----------|------|
| 1 | parallel_fetch | HTTP(并行×4) | 基本信息/行为/兴趣/社交 |
| 2 | merge_profile | DATA_PROCESSING | 合并画像数据 |
| 3 | generate_desc | LLM_CALL | 生成画像描述 |

---

### 4.3 推荐分发

**业务描述：** 智能推荐内容并分发到多渠道。

**节点组合：**
```
HTTP → LLM_CALL → PARALLEL → HTTP → END
```

**验证重点：** 智能推荐 + 多渠道分发

**详细设计：**

| 步骤 | 节点ID | 节点类型 | 配置 |
|------|--------|----------|------|
| 1 | get_user | HTTP_REQUEST | 获取用户信息 |
| 2 | recommend | LLM_CALL | 生成推荐列表 |
| 3 | parallel_distribute | PARALLEL | 并行分发到各渠道 |
| 3a | push_feed | HTTP_REQUEST | 推荐信息流 |
| 3b | push_email | HTTP_REQUEST | 邮件推送 |
| 3c | push_sms | HTTP_REQUEST | 短信推送 |

---

### 4.4 舆情分析

**业务描述：** 采集社交媒体内容，分析热点话题。

**节点组合：**
```
HTTP → LLM_CALL × 3(并行) → DATA_PROCESSING → END
```

**验证重点：** 热点话题提取

**详细设计：**

| 步骤 | 节点ID | 节点类型 | 配置 |
|------|--------|----------|------|
| 1 | crawl | HTTP_REQUEST | 采集社交内容 |
| 2 | parallel_analyze | LLM_CALL(并行×3) | 情感/主题/热度分析 |
| 3 | aggregate | DATA_PROCESSING | 汇总分析结果 |

---

## 五、物联网IoT场景（4个核心流程）

### 5.1 设备管控

**业务描述：** 批量向IoT设备下发控制指令。

**节点组合：**
```
DATA_INPUT → FOREACH → HTTP(循环) → CONDITION → END
```

**验证重点：** 批量设备指令下发

**详细设计：**

| 步骤 | 节点ID | 节点类型 | 配置 |
|------|--------|----------|------|
| 1 | get_commands | DATA_INPUT | 获取控制指令列表 |
| 2 | foreach_device | FOREACH | 遍历设备 |
| 3 | send_command | HTTP_REQUEST | 发送指令到设备 |
| 4 | check_result | CONDITION | 成功/失败 |

---

### 5.2 数据采集

**业务描述：** 定时采集IoT设备数据，阈值告警。

**节点组合：**
```
HTTP(POLL) × N → DATA_PROCESSING → CONDITION → END
```

**验证重点：** 定时采集 + 阈值告警

**详细设计：**

| 步骤 | 节点ID | 节点类型 | 配置 |
|------|--------|----------|------|
| 1 | parallel_poll | HTTP(并行) | 并行采集多设备数据 |
| 2 | process | DATA_PROCESSING | 数据清洗整理 |
| 3 | check_threshold | CONDITION | 数值 > 阈值 |
| 4 | alert | HTTP_REQUEST | 触发告警 |

---

### 5.3 告警触发

**业务描述：** IoT告警自动处理，多级通知。

**节点组合：**
```
HTTP → CONDITION → [HTTP, HTTP, HTTP] → SUBWORKFLOW → END
```

**验证重点：** 多级告警 + 自动处理

**详细设计：**

| 步骤 | 节点ID | 节点类型 | 配置 |
|------|--------|----------|------|
| 1 | receive_alert | HTTP_REQUEST | 接收告警 |
| 2 | severity | CONDITION | 告警级别 |
| 3a | notify_l1 | HTTP_REQUEST | 通知相关人员 |
| 3b | notify_l2 | HTTP_REQUEST | 通知主管 |
| 3c | notify_l3 | HTTP_REQUEST | 紧急通知 |
| 4 | auto_handle | SUBWORKFLOW | 自动处理子工作流 |

---

### 5.4 远程诊断

**业务描述：** AI辅助远程设备诊断。

**节点组合：**
```
LLM_CALL → HTTP → LLM_CALL → CONDITION → END
```

**验证重点：** AI诊断 + API调用

**详细设计：**

| 步骤 | 节点ID | 节点类型 | 配置 |
|------|--------|----------|------|
| 1 | parse_symptom | LLM_CALL | 解析故障描述 |
| 2 | fetch_diagnosis | HTTP_REQUEST | 获取诊断历史 |
| 3 | diagnose | LLM_CALL | AI诊断建议 |
| 4 | has_solution | CONDITION | 有解决方案/无 |
| 5 | solution | HTTP_REQUEST | 执行解决方案 |

---

## 六、公共服务场景（4个核心流程）

### 6.1 表单处理

**业务描述：** 智能处理市民提交的表单申请。

**节点组合：**
```
DATA_INPUT → DATA_PROCESSING → CONDITION → [HTTP, HTTP] → END
```

**验证重点：** 表单验证 + 智能分流

**详细设计：**

| 步骤 | 节点ID | 节点类型 | 配置 |
|------|--------|----------|------|
| 1 | receive_form | DATA_INPUT | 接收表单数据 |
| 2 | validate | DATA_PROCESSING | 表单验证 |
| 3 | route | CONDITION | 业务类型分流 |
| 4a | handle_type_a | HTTP_REQUEST | 业务A处理 |
| 4b | handle_type_b | HTTP_REQUEST | 业务B处理 |

---

### 6.2 资质审核

**业务描述：** 复杂资质材料审核，调用子工作流。

**节点组合：**
```
HTTP → LLM_CALL → CONDITION → [SUBWORKFLOW, HTTP] → END
```

**验证重点：** 复杂材料审核

**详细设计：**

| 步骤 | 节点ID | 节点类型 | 配置 |
|------|--------|----------|------|
| 1 | fetch_material | HTTP_REQUEST | 获取资质材料 |
| 2 | pre_check | LLM_CALL | 预审评估 |
| 3 | pass_check | CONDITION | 通过/需详细审核 |
| 4a | quick_approve | HTTP_REQUEST | 快速通过 |
| 4b | deep_audit | SUBWORKFLOW | 详细审核子工作流 |

---

### 6.3 进度查询

**业务描述：** 市民查询业务办理进度。

**节点组合：**
```
DATA_INPUT → HTTP → DATA_PROCESSING → LLM_CALL → END
```

**验证重点：** 状态查询 + 自然语言回复

**详细设计：**

| 步骤 | 节点ID | 节点类型 | 配置 |
|------|--------|----------|------|
| 1 | receive_query | DATA_INPUT | 接收查询请求 |
| 2 | get_status | HTTP_REQUEST | 查询办理状态 |
| 3 | format_status | DATA_PROCESSING | 格式化状态信息 |
| 4 | generate_reply | LLM_CALL | 生成自然语言回复 |

---

### 6.4 投诉处理

**业务描述：** 智能分类投诉工单，自动派发处理。

**节点组合：**
```
DATA_INPUT → LLM_CALL → CONDITION → [HTTP, HTTP] → SUBWORKFLOW → END
```

**验证重点：** 智能分类 + 自动派发

**详细设计：**

| 步骤 | 节点ID | 节点类型 | 配置 |
|------|--------|----------|------|
| 1 | receive_complaint | DATA_INPUT | 接收投诉内容 |
| 2 | classify | LLM_CALL | 投诉分类 |
| 3 | urgency | CONDITION | 紧急程度 |
| 4a | dispatch_normal | HTTP_REQUEST | 普通派发 |
| 4b | dispatch_urgent | HTTP_REQUEST | 紧急派发 |
| 5 | track | SUBWORKFLOW | 处理进度跟踪 |

---

## 七、场景统计汇总

| 行业 | 场景数 | 节点类型覆盖 |
|------|--------|--------------|
| 电商 | 5 | DATA_INPUT, LLM_CALL, BRANCH, HTTP_REQUEST, CONDITION, PARALLEL, DATA_PROCESSING |
| 金融 | 4 | LLM_CALL, CONDITION, HTTP_REQUEST, TRY_CATCH, SUBWORKFLOW |
| 企业办公 | 4 | DATA_INPUT, HTTP_REQUEST, CONDITION, SUBWORKFLOW, FOREACH, LLM_CALL |
| 社交内容 | 4 | HTTP_REQUEST, LLM_CALL, CONDITION, PARALLEL, DATA_PROCESSING |
| 物联网IoT | 4 | DATA_INPUT, HTTP_REQUEST, FOREACH, CONDITION, LLM_CALL, SUBWORKFLOW |
| 公共服务 | 4 | DATA_INPUT, HTTP_REQUEST, LLM_CALL, CONDITION, SUBWORKFLOW, DATA_PROCESSING |
| **合计** | **25** | **全部10种节点类型** |

---

## 八、验证检查清单

### 8.1 节点类型验证

- [ ] DATA_INPUT - 输入节点正常工作
- [ ] DATA_PROCESSING - 数据处理和表达式计算
- [ ] LLM_CALL - LLM调用和Prompt模板
- [ ] HTTP_REQUEST - HTTP请求发送和响应处理
- [ ] CONDITION - 条件判断和分支路由
- [ ] BRANCH - 多分支路由
- [ ] PARALLEL - 并行执行
- [ ] FOREACH - 循环处理
- [ ] SUBWORKFLOW - 子工作流调用
- [ ] TRY_CATCH - 异常捕获
- [ ] RETRY - 重试机制

### 8.2 数据流转验证

- [ ] 节点间数据正确传递
- [ ] inputMapping 前缀剥离正常工作
- [ ] outputMapping 正确存储输出
- [ ] 上下文变量正确访问

### 8.3 异常处理验证

- [ ] HTTP 超时有重试
- [ ] LLM 返回格式错误可捕获
- [ ] 条件分支异常处理
- [ ] 子工作流异常传播
