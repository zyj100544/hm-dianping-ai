from dataclasses import dataclass, field
from typing import List, Optional


@dataclass
class ReviewAuditMessage:
    commentId: int
    content: str
    score: int
    shopId: int
    shopName: str = ""
    shopTypeName: str = ""
    userId: int = 0


@dataclass
class ReviewAuditResultMessage:
    commentId: int
    risk: str  # LOW / MEDIUM / HIGH
    reason: str
    flags: List[str] = field(default_factory=list)
