import os


RABBITMQ_HOST = os.getenv("RABBITMQ_HOST", "rabbitmq")
RABBITMQ_PORT = int(os.getenv("RABBITMQ_PORT", "5672"))
RABBITMQ_USER = os.getenv("RABBITMQ_USER", "hmall")
RABBITMQ_PASS = os.getenv("RABBITMQ_PASS", "123")
RABBITMQ_VHOST = os.getenv("RABBITMQ_VHOST", "/hmall")

REVIEW_AUDIT_EXCHANGE = "review.audit.exchange"
REVIEW_AUDIT_QUEUE = "review.audit.queue"
REVIEW_AUDIT_ROUTING_KEY = "review.audit"
REVIEW_AUDIT_RESULT_QUEUE = "review.audit.result.queue"
REVIEW_AUDIT_RESULT_ROUTING_KEY = "review.audit.result"

RECOMMEND_EXCHANGE = "recommend.exchange"
RECOMMEND_QUEUE = "recommend.queue"
RECOMMEND_ROUTING_KEY = "recommend.request"
RECOMMEND_RESULT_QUEUE = "recommend.result.queue"
RECOMMEND_RESULT_ROUTING_KEY = "recommend.result"

LLM_API_KEY = os.getenv("LLM_API_KEY", "")
LLM_BASE_URL = os.getenv("LLM_BASE_URL", "https://api.deepseek.com")
LLM_MODEL = os.getenv("LLM_MODEL", "deepseek-chat")

LLM_MAX_RETRIES = 3
LLM_TIMEOUT = 30
