# SmartBite

SmartBite 旨在打造一个基于“地理位置 + 语义理解”的下一代外卖导购平台。 本阶段（v0.1）聚焦于**导购（Discovery）**核心能力，暂不包含交易支付与履约配送，但为未来扩展预留接口。

### 1.时序图

![](D:\javaWorkSpace\SmartBite\Seq_Diagram.png)

### 2.ER图

![](D:\javaWorkSpace\SmartBite\ER_Diagram.png)

### 3.api接口

### 接口定义：智能搜索对话

* **URL**: `POST /api/v1/chat/completions`
* **Content-Type**: `application/json`
* **Accept**: `application/json`

#### 1. 请求参数 (Request)

```json
{
  "session_id": "a1b2-c3d4-e5f6",        // 必填：会话ID，用于 Redis 串联多轮对话
  "user_id": "10086",                     // 必填：用户ID，用于个性化推荐
  "message": "适合约会的西餐，不要太贵的",  // 必填：用户的自然语言输入
  "user_location": {                      // 必填：用户当前经纬度，用于计算距离
    "lat": 39.9042,
    "lon": 116.4074
  },
  "filters": {                            // 选填：前端显式传来的过滤器（如果有）
    "category": "西餐",
    "price_max": 500,
    "sort_by": "distance"                 // 排序策略：distance | rating | price
  }
}
```

#### 2. 成功响应 (Response - 200 OK)

后端执行完 RAG 流程（检索 + 生成）后，返回如下结构：

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "session_id": "a1b2-c3d4-e5f6",

    // 1. AI 生成的完整回复文本 (Markdown格式)
    "answer": "为您推荐 **Blue Frog 蓝蛙** 和 **Tiago**。\n\n**蓝蛙**位于三里屯，环境比较轻松，人均价格在150元左右，非常适合情侣约会。他们的汉堡和鸡尾酒很出名。\n\n**Tiago** 则更偏向正宗意式风味，评分高达 4.8 分，距离您只有 1.2km。",

    // 2. 结构化商户数据 (用于前端渲染卡片/地图)
    "sources": [
      {
        "id": 101,
        "name": "Blue Frog 蓝蛙(三里屯店)",
        "category": "西餐",
        "avg_price": 168,
        "rating": 4.6,
        "cover_image": "https://img.meituan.com/xxx.jpg",
        "tags": ["露台", "汉堡", "氛围好"],
        "distance_km": 2.5,          // 后端计算好的距离
        "location": { "lat": 39.93, "lon": 116.45 }
      },
      {
        "id": 205,
        "name": "Tiago Home Kitchen",
        "category": "意大利菜",
        "avg_price": 190,
        "rating": 4.8,
        "cover_image": "https://img.meituan.com/yyy.jpg",
        "tags": ["约会圣地", "意面"],
        "distance_km": 1.2,
        "location": { "lat": 39.91, "lon": 116.42 }
      }
    ],

    // 3. 元数据 (可选，用于调试或统计)
    "usage": {
      "prompt_tokens": 350,
      "completion_tokens": 120,
      "total_tokens": 470
    }
  }
}

```
