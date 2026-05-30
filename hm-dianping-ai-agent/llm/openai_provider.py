import json
import logging
from typing import List

from openai import OpenAI

from config import LLM_API_KEY, LLM_BASE_URL, LLM_MODEL, LLM_MAX_RETRIES, LLM_TIMEOUT
from llm.base import LLMProvider
from llm.prompt import SYSTEM_PROMPT, build_user_prompt
from models.schemas import ReviewAuditMessage, ReviewAuditResultMessage

logger = logging.getLogger(__name__)


class OpenAICompatibleProvider(LLMProvider):
    """LLM provider using OpenAI-compatible API. Supports OpenAI, DeepSeek, Qwen, etc."""

    def __init__(self):
        self.client = OpenAI(api_key=LLM_API_KEY, base_url=LLM_BASE_URL, timeout=LLM_TIMEOUT)
        self.model = LLM_MODEL

    def audit(self, review: ReviewAuditMessage) -> ReviewAuditResultMessage:
        user_prompt = build_user_prompt(review.content, review.score, review.shopName, review.shopTypeName)

        for attempt in range(LLM_MAX_RETRIES):
            try:
                response = self.client.chat.completions.create(
                    model=self.model,
                    messages=[
                        {"role": "system", "content": SYSTEM_PROMPT},
                        {"role": "user", "content": user_prompt},
                    ],
                    temperature=0.1,
                )
                raw = response.choices[0].message.content.strip()
                logger.info(f"LLM response (attempt {attempt + 1}): {raw[:200]}")

                result = self._parse(raw, review.commentId)
                if result:
                    return result

            except Exception as e:
                logger.warning(f"LLM call failed (attempt {attempt + 1}): {e}")

        logger.error(f"LLM audit failed after {LLM_MAX_RETRIES} retries, falling back to MEDIUM")
        return ReviewAuditResultMessage(
            commentId=review.commentId,
            risk="MEDIUM",
            reason="AI审核服务异常，标记为人工审核",
            flags=["ai_error"],
        )

    def _parse(self, raw: str, comment_id: int) -> ReviewAuditResultMessage | None:
        try:
            # strip markdown code fences if present
            if raw.startswith("```"):
                lines = raw.split("\n")
                raw = "\n".join(lines[1:-1])
            data = json.loads(raw)
            risk = data.get("risk", "MEDIUM").upper()
            if risk not in ("LOW", "MEDIUM", "HIGH"):
                risk = "MEDIUM"
            return ReviewAuditResultMessage(
                commentId=comment_id,
                risk=risk,
                reason=data.get("reason", "审核完成"),
                flags=data.get("flags", []),
            )
        except json.JSONDecodeError as e:
            logger.warning(f"Failed to parse LLM response as JSON: {e}")
            return None
