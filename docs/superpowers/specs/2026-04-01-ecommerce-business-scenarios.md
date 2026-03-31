# 电商MVP业务场景工作流设计

## 一、核心业务流程总览

```
用户输入 → 意图识别(L LM) → 路由分发 → [搜索/推荐/库存/订单] → 结果处理
```

---

## 二、场景1：智能客服路由（传统企业场景）

### 2.1 业务背景
用户输入一句话，LLM低温(<0.3)解析成结构化JSON，根据intent路由到不同业务模块。

### 2.2 工作流设计

```
[开始]
   ↓
[用户输入节点] - type: DATA_INPUT - 接收用户query
   ↓
[意图识别LLM节点] - type: LLM_CALL
   - prompt: 从用户输入"{input}"中提取意图，返回JSON格式：{"intent":"search|recommend|inquiry|order","entities":{}}
   - temperature: 0.2
   ↓
[条件分支节点] - type: BRANCH
   - branches: [
     {"name": "搜索商品", "expression": "#context.intent == 'search'", "nextNodeId": "search_node"},
     {"name": "商品推荐", "expression": "#context.intent == 'recommend'", "nextNodeId": "recommend_node"},
     {"name": "库存查询", "expression": "#context.intent == 'inquiry'", "nextNodeId": "inventory_node"},
     {"name": "下单", "expression": "#context.intent == 'order'", "nextNodeId": "order_node"}
   ]
   ↓
[搜索商品HTTP节点] / [商品推荐HTTP节点] / [库存查询HTTP节点] / [下单处理HTTP节点]
   ↓
[结果汇总LLM节点] - 格式化输出
   ↓
[结束]
```

### 2.3 输入输出示例

**输入：** "我想买一双Nike运动鞋，42码，黑色"
**LLM输出JSON：**
```json
{
  "intent": "search",
  "entities": {
    "brand": "Nike",
    "category": "运动鞋",
    "size": "42",
    "color": "黑色"
  }
}
```

**后续节点流转：**
- intent=search → 搜索商品节点 → Nike官网API搜索
- intent=recommend → 推荐节点 → 个性化推荐API
- intent=inquiry → 库存节点 → ERP库存API
- intent=order → 订单节点 → OMS下单API

---

## 三、场景2：商品搜索工作流

### 3.1 业务背景
用户搜索商品，支持多条件筛选，返回结构化商品列表。

### 3.2 工作流设计

```
[开始]
   ↓
[构建搜索请求节点] - 组装搜索参数
   - config.expression: "SELECT * FROM products WHERE #input.category AND #input.brand"
   ↓
[搜索商品HTTP节点] - type: HTTP_REQUEST
   - url: https://api.ecommerce.com/v1/products/search
   - method: POST
   - body: {
       "query": "#context.searchQuery",
       "filters": "#context.filters",
       "page": 1,
       "pageSize": 20
     }
   - outputKey: searchResult
   ↓
[过滤库存节点] - type: DATA_PROCESSING
   - config.expression: "#searchResult.items.filter(x => x.stock > 0)"
   ↓
[排序节点] - type: DATA_PROCESSING
   - config.expression: "#filteredItems.sortBy(x => x.relevance, 'desc')"
   ↓
[格式化结果节点] - type: LLM_CALL
   - prompt: 将商品列表格式化为友好的展示文本
   ↓
[结束]
```

### 3.3 测试用例

| ID | 输入 | 预期结果 |
|----|------|---------|
| TC-S-01 | 搜索"Nike运动鞋" | 返回Nike相关商品列表 |
| TC-S-02 | 搜索"不存在的商品" | 返回空列表或推荐替代品 |
| TC-S-03 | 搜索接口超时 | 显示超时错误，提示重试 |

---

## 四、场景3：个性化推荐工作流

### 4.1 业务背景
基于用户画像和行为历史，推荐感兴趣的商品。

### 4.2 工作流设计

```
[开始]
   ↓
[获取用户画像节点] - HTTP调用用户中心
   - url: https://api.ecommerce.com/v1/users/{userId}/profile
   - outputKey: userProfile
   ↓
[获取行为历史节点] - HTTP调用行为日志
   - url: https://api.ecommerce.com/v1/users/{userId}/behaviors
   - outputKey: behaviorHistory
   ↓
[构建推荐请求节点] - type: DATA_PROCESSING
   - 组装推荐算法入参
   ↓
[推荐算法LLM节点] - type: LLM_CALL
   - prompt: 基于用户"{userProfile.name}"的偏好{userProfile.preferences}和浏览历史{behaviorHistory}，推荐5个商品
   - temperature: 0.7
   ↓
[获取推荐商品详情节点] - 并行HTTP调用
   - 并行查询每个推荐商品的库存、价格信息
   ↓
[过滤无货节点] - type: DATA_PROCESSING
   - 移除库存为0的商品
   ↓
[格式化推荐结果节点] - type: LLM_CALL
   - prompt: 生成自然语言推荐理由
   ↓
[结束]
```

### 4.3 测试用例

| ID | 输入 | 预期结果 |
|----|------|---------|
| TC-R-01 | 新用户无历史 | 返回热销/热门商品 |
| TC-R-02 | 有浏览历史用户 | 返回与历史相似的商品 |
| TC-R-03 | 推荐商品全部无货 | 过滤后返回空，给出提示 |

---

## 五、场景4：库存查询工作流

### 5.1 业务背景
实时查询商品库存，支持多SKU并发查询。

### 5.2 工作流设计

```
[开始]
   ↓
[解析商品ID列表节点] - type: DATA_PROCESSING
   - 从输入提取SKU列表
   ↓
[并行查询库存节点] - type: PARALLEL
   - strategy: AND (全部完成)
   - branches: [
     {"name": "查询库存", "nodeIds": ["inventory_query_1", "inventory_query_2", ...]},
     {"name": "查询价格", "nodeIds": ["price_query_1", "price_query_2", ...]}
   ]
   ↓
[子节点: 查询库存HTTP] - 并行执行
   - url: https://api.erp.com/v1/inventory/batch
   - body: {"skus": ["SKU001", "SKU002", "SKU003"]}
   ↓
[子节点: 查询价格HTTP] - 并行执行
   - url: https://api.erp.com/v1/products/prices
   - body: {"skus": ["SKU001", "SKU002", "SKU003"]}
   ↓
[合并库存价格节点] - type: DATA_PROCESSING
   - 将库存和价格数据合并
   ↓
[格式化库存结果节点]
   ↓
[结束]
```

### 5.3 测试用例

| ID | 输入 | 预期结果 |
|----|------|---------|
| TC-I-01 | 单个SKU查询 | 返回精确库存数量 |
| TC-I-02 | 批量10个SKU查询 | 并行返回，3秒内完成 |
| TC-I-03 | SKU不存在 | 返回"商品不存在"而非报错 |
| TC-I-04 | 部分SKU无库存 | 有库存返回数量，无库存返回0 |

---

## 六、场景5：下单创建工作流

### 6.1 业务背景
用户下单创建，包含库存校验、价格计算、订单创建。

### 6.2 工作流设计

```
[开始]
   ↓
[库存校验节点] - type: HTTP_REQUEST
   - url: https://api.erp.com/v1/inventory/reserve
   - method: POST
   - body: {"sku": "#input.sku", "quantity": "#input.quantity"}
   ↓
[条件库存校验] - type: CONDITION
   - conditions: [
     {"expression": "#reserveResult.sufficient == true", "nextNodeId": "calc_price"},
     {"expression": "#reserveResult.sufficient == false", "nextNodeId": "库存不足"}
   ]
   ↓
[计算价格节点] - type: HTTP_REQUEST
   - url: https://api.erp.com/v1/orders/calculate
   - body: {"sku": "#input.sku", "quantity": "#input.quantity"}
   ↓
[创建订单节点] - type: HTTP_REQUEST
   - url: https://api.oms.com/v1/orders
   - method: POST
   - body: {
       "userId": "#input.userId",
       "items": [{"sku": "#input.sku", "quantity": "#input.quantity"}],
       "totalAmount": "#calcResult.total",
       "paymentMethod": "#input.payment"
     }
   ↓
[发送确认通知节点] - type: HTTP_REQUEST
   - url: https://api.notify.com/v1/sms/send
   - body: {"mobile": "#user.mobile", "template": "order_created"}
   ↓
[结束]
```

### 6.3 测试用例

| ID | 场景 | 输入 | 预期结果 |
|----|------|------|---------|
| TC-O-01 | 正常下单 | 库存充足 | 订单创建成功，返回订单号 |
| TC-O-02 | 库存不足 | 库存=0 | 提示库存不足，不创建订单 |
| TC-O-03 | 库存不足 | 库存<购买数量 | 提示库存不足数量 |
| TC-O-04 | 价格计算 | 多SKU多数量 | 正确计算总价和优惠 |
| TC-O-05 | 订单创建失败 | OMS接口超时 | 库存回滚，提示重试 |

---

## 七、场景6：完整用户购物流程

### 7.1 端到端工作流

```
[开始]
   ↓
[用户输入: "我想买Nike球鞋42码"]
   ↓
[意图识别LLM] → intent="search"
   ↓
[搜索商品工作流] → 返回商品列表
   ↓
[用户选择: 商品A]
   ↓
[库存查询] → 库存充足
   ↓
[获取用户详情] → 已登录，有收货地址
   ↓
[价格计算] → 原价 ¥699，会员价 ¥649
   ↓
[用户确认: "确认下单"]
   ↓
[创建订单] → 订单号: ORD202604010001
   ↓
[扣减库存] → 库存从10减为9
   ↓
[发送通知] → 短信: 您的订单已创建...
   ↓
[结束: 订单创建成功]
```

### 7.2 跨系统数据流

| 步骤 | 系统 | 数据 |
|------|------|------|
| 意图识别 | AI平台 | user_query → intent |
| 商品搜索 | 搜索服务 | query → products[] |
| 库存查询 | ERP系统 | sku → stock |
| 用户信息 | 用户中心 | userId → userProfile |
| 价格计算 | 订单系统 | items → price |
| 订单创建 | OMS | orderInfo → orderId |
| 库存扣减 | ERP系统 | reserve → stock-1 |
| 消息通知 | 通知服务 | mobile → sms |

---

## 八、节点模板配置

### 8.1 LLM模板

**意图识别模板（低温）**
```
名称: 意图识别(低温)
供应商: OpenAI
模型: gpt-4
Temperature: 0.2
Prompt模板: 从用户输入"{input}"中提取意图...
```

**结果格式化模板**
```
名称: 结果格式化
供应商: OpenAI
模型: gpt-4
Temperature: 0.5
Prompt模板: 将以下商品列表格式化为友好的展示文本...
```

### 8.2 HTTP模板

**商品搜索API**
```
名称: 搜索商品API
Method: POST
URL: https://api.ecommerce.com/v1/products/search
Headers: {"Authorization": "Bearer #env.API_KEY"}
Timeout: 5000
```

**库存查询API**
```
名称: 库存查询API
Method: POST
URL: https://api.erp.com/v1/inventory/query
Headers: {"X-ERP-Key": "#env.ERP_KEY"}
Timeout: 3000
```

**订单创建API**
```
名称: 创建订单API
Method: POST
URL: https://api.oms.com/v1/orders
Headers: {"X-OMS-Token": "#env.OMS_TOKEN"}
Timeout: 10000
```

---

## 九、验证检查清单

### 9.1 流程可流转性验证

- [ ] 意图识别能正确分类search/recommend/inquiry/order
- [ ] 条件分支能根据LLM输出路由到正确分支
- [ ] 串行节点按顺序执行
- [ ] 并行节点同时执行，提高效率
- [ ] 循环节点能处理批量数据

### 9.2 数据流转验证

- [ ] 前置节点输出能作为后续节点输入
- [ ] #context.xxx 能正确访问上下文变量
- [ ] #input.xxx 能正确获取外部输入
- [ ] 变量传递不丢失

### 9.3 异常处理验证

- [ ] HTTP超时有重试机制
- [ ] 库存不足有友好提示
- [ ] LLM输出格式错误能捕获
- [ ] 部分节点失败不影响其他并行节点

---

## 十、测试报告模板

| 工作流名称 | 测试场景 | 测试输入 | 预期输出 | 实际输出 | 结果 | Bug |
|-----------|---------|---------|---------|---------|------|-----|
| 智能客服路由 | 搜索商品意图 | "想买Nike鞋" | intent=search | | PASS/FAIL | |
| 商品搜索 | 正常搜索 | "Nike运动鞋" | 返回商品列表 | | PASS/FAIL | |
| ... | | | | | | |
