SYSTEM_PROMPT = """\
你是智能推荐助手，基于用户的历史行为数据，从候选商铺中为其推荐最可能感兴趣的5个商铺。

推荐策略：
1. 优先推荐与用户偏好类型（常去的商铺类型）相同的商铺
2. 考虑价格带匹配：如果用户常去中低价位，优先推荐相近价位
3. 适当引入1-2个新类型商铺，避免信息茧房，但要确保与新类型有合理的关联
4. 排除用户已经评论过的商铺（避免重复推荐）
5. 推荐理由要具体，提到为什么适合这个用户

请仅返回纯JSON（不要markdown代码块），格式如下：
{"recommendations":[{"shopId":1,"shopName":"店名","shopType":"类型","avgPrice":100,"score":45,"reason":"15字以内推荐理由"},...]}
"""


def build_user_prompt(user_profile: dict, candidates: list) -> str:
    parts = []

    commented = user_profile.get("commentedShops", [])
    if commented:
        parts.append("## 用户评论过的商铺")
        for item in commented:
            score_str = f"评分{item['score']}/5" if item.get("score") else ""
            parts.append(f"- {item['shopName']}（{item['shopType']}）{score_str}")

    liked = user_profile.get("likedShops", [])
    if liked:
        parts.append("## 用户发布的探店笔记涉及的商铺")
        for item in liked:
            parts.append(f"- {item['shopName']}（{item['shopType']}）")

    parts.append("## 候选商铺列表")
    for c in candidates:
        parts.append(
            f"- id={c['shopId']} {c['shopName']}（{c['shopType']}）"
            f"均价{c['avgPrice']}元 评分{c['score']}/50 商圈:{c['area']}"
        )

    parts.append("请返回推荐结果JSON：")
    return "\n".join(parts)
