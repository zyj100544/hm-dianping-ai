from abc import ABC, abstractmethod

from models.schemas import ReviewAuditMessage, ReviewAuditResultMessage


class LLMProvider(ABC):
    """LLM provider abstraction for pluggable model backends."""

    @abstractmethod
    def audit(self, review: ReviewAuditMessage) -> ReviewAuditResultMessage:
        ...
