"""FastAPI AI services - called by Java backend via Feign."""
import json
import logging
from typing import List, Optional

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
from openai import OpenAI

from config import LLM_API_KEY, LLM_BASE_URL, LLM_MODEL, LLM_MAX_RETRIES, LLM_TIMEOUT

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="AI Services")
client = OpenAI(api_key=LLM_API_KEY, base_url=LLM_BASE_URL, timeout=LLM_TIMEOUT)

SEARCH_SYSTEM_PROMPT = """你是一个智能搜索助手。用户会用自然语言描述需求，你需要从候选店铺列表中找出最匹配的店铺。
请严格按以下JSON格式返回，不要输出任何其他内容：
{"results":[{"shopId":1,"relevance":0.95},{"shopId":2,"relevance":0.8}]}
relevance为0-1之间的匹配度，按匹配度从高到低排序，最多返回10个。如果没有匹配的店铺，返回{"results":[]}。"""

BLOG_ASSIST_SYSTEM_PROMPT = """你是一个大众点评风格的探店笔记写手。用户会告诉你店铺名称和要求，你需要撰写一篇生动有趣的探店笔记。

写作风格：
- 第一人称体验分享，语气自然亲切
- 重点描述：环境氛围、招牌菜品、服务感受、性价比
- 适当使用emoji增加可读性
- 分段清晰，有标题感
- 字数控制在200-500字
- 像真实用户的分享，不要像广告

请直接返回笔记内容，不要加任何前缀说明。"""


class ShopItem(BaseModel):
    id: int
    name: str
    typeName: str
    avgPrice: Optional[int] = None
    score: Optional[float] = None
    comments: Optional[int] = None
    area: Optional[str] = None


class SearchRequest(BaseModel):
    query: str
    shops: List[ShopItem]


class ResultItem(BaseModel):
    shopId: int
    relevance: float


class SearchResponse(BaseModel):
    results: List[ResultItem]


def build_user_prompt(query: str, shops: List[ShopItem]) -> str:
    lines = ["用户搜索：" + query, "", "候选店铺列表："]
    for s in shops:
        lines.append(
            f"{s.id}. {s.name}，{s.typeName}，位于{s.area or ''}，"
            f"人均{s.avgPrice or '-'}元，评分{s.score or '-'}，{s.comments or 0}条评价"
        )
    return "\n".join(lines)


def call_llm_search(query: str, shops: List[ShopItem]) -> List[dict]:
    user_prompt = build_user_prompt(query, shops)
    for attempt in range(1, LLM_MAX_RETRIES + 1):
        try:
            logger.info("LLM search attempt %d, query=%s, shops=%d", attempt, query, len(shops))
            resp = client.chat.completions.create(
                model=LLM_MODEL,
                temperature=0.1,
                messages=[
                    {"role": "system", "content": SEARCH_SYSTEM_PROMPT},
                    {"role": "user", "content": user_prompt},
                ],
            )
            content = resp.choices[0].message.content.strip()
            if content.startswith("```"):
                content = content.replace("```json", "").replace("```", "").strip()
            data = json.loads(content)
            return data.get("results", [])
        except Exception as e:
            logger.warning("LLM search attempt %d failed: %s", attempt, e)
            if attempt >= LLM_MAX_RETRIES:
                raise
    return []


@app.post("/api/semantic-search", response_model=SearchResponse)
def semantic_search(req: SearchRequest):
    results = call_llm_search(req.query, req.shops)
    return SearchResponse(results=[ResultItem(**r) for r in results])


class BlogAssistRequest(BaseModel):
    action: str  # "expand" | "polish" | "generate"
    shopName: str = ""
    draft: str = ""
    keywords: str = ""


class BlogAssistResponse(BaseModel):
    content: str


def build_blog_assist_prompt(req: BlogAssistRequest) -> str:
    if req.action == "expand":
        return (
            f"我在写一篇关于「{req.shopName}」的探店笔记，目前的内容是：\n\n"
            f"{req.draft}\n\n"
            f"请帮我扩写这篇笔记，补充更多细节描述（环境、菜品、服务等），保持第一人称体验分享的风格。"
        )
    elif req.action == "polish":
        return (
            f"我在写一篇关于「{req.shopName}」的探店笔记，目前的内容是：\n\n"
            f"{req.draft}\n\n"
            f"请帮我润色这篇笔记，让语言更生动自然，更像真实用户的分享，修正错别字和不通顺的地方。"
        )
    else:
        keywords_hint = f"重点突出：{req.keywords}" if req.keywords else ""
        return (
            f"请帮我写一篇关于「{req.shopName}」的探店笔记。{keywords_hint}\n\n"
            f"要求：内容真实自然，从环境、菜品、服务、性价比等方面展开，"
            f"像普通用户在大众点评分享自己的真实体验。"
        )


def call_llm_blog_assist(req: BlogAssistRequest) -> str:
    user_prompt = build_blog_assist_prompt(req)
    for attempt in range(1, LLM_MAX_RETRIES + 1):
        try:
            logger.info("LLM blog assist attempt %d, action=%s, shop=%s", attempt, req.action, req.shopName)
            resp = client.chat.completions.create(
                model=LLM_MODEL,
                temperature=0.7,
                messages=[
                    {"role": "system", "content": BLOG_ASSIST_SYSTEM_PROMPT},
                    {"role": "user", "content": user_prompt},
                ],
            )
            return resp.choices[0].message.content.strip()
        except Exception as e:
            logger.warning("LLM blog assist attempt %d failed: %s", attempt, e)
            if attempt >= LLM_MAX_RETRIES:
                raise HTTPException(status_code=500, detail=f"AI生成失败: {e}")
    return ""


@app.post("/api/blog-assist", response_model=BlogAssistResponse)
def blog_assist(req: BlogAssistRequest):
    content = call_llm_blog_assist(req)
    return BlogAssistResponse(content=content)


@app.get("/health")
def health():
    return {"status": "ok"}


if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
