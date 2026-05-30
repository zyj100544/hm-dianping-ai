import json
import logging
import threading

import pika

from config import (
    RABBITMQ_HOST, RABBITMQ_PORT, RABBITMQ_USER, RABBITMQ_PASS, RABBITMQ_VHOST,
    REVIEW_AUDIT_QUEUE, REVIEW_AUDIT_EXCHANGE, REVIEW_AUDIT_ROUTING_KEY,
    REVIEW_AUDIT_RESULT_QUEUE, REVIEW_AUDIT_RESULT_ROUTING_KEY,
)
from llm.openai_provider import OpenAICompatibleProvider
from models.schemas import ReviewAuditMessage

logger = logging.getLogger(__name__)


class ReviewAuditConsumer:
    """Consumes audit messages from RabbitMQ and runs LLM-based review."""

    def __init__(self):
        self.llm = OpenAICompatibleProvider()
        self._shutdown_event = threading.Event()
        self._thread: threading.Thread | None = None

    def get_shutdown_event(self) -> threading.Event:
        return self._shutdown_event

    def start(self):
        self._thread = threading.Thread(target=self._run, daemon=True)
        self._thread.start()
        logger.info("RabbitMQ consumer thread started")

    def stop(self):
        self._shutdown_event.set()
        logger.info("RabbitMQ consumer stopped")

    def _run(self):
        while not self._shutdown_event.is_set():
            try:
                credentials = pika.PlainCredentials(RABBITMQ_USER, RABBITMQ_PASS)
                params = pika.ConnectionParameters(
                    host=RABBITMQ_HOST, port=RABBITMQ_PORT,
                    virtual_host=RABBITMQ_VHOST, credentials=credentials,
                    heartbeat=60,
                )
                connection = pika.BlockingConnection(params)
                channel = connection.channel()

                channel.exchange_declare(exchange=REVIEW_AUDIT_EXCHANGE, exchange_type="direct", durable=True)
                channel.queue_declare(queue=REVIEW_AUDIT_QUEUE, durable=True)
                channel.queue_bind(queue=REVIEW_AUDIT_QUEUE, exchange=REVIEW_AUDIT_EXCHANGE, routing_key=REVIEW_AUDIT_ROUTING_KEY)
                channel.queue_declare(queue=REVIEW_AUDIT_RESULT_QUEUE, durable=True)
                channel.queue_bind(queue=REVIEW_AUDIT_RESULT_QUEUE, exchange=REVIEW_AUDIT_EXCHANGE, routing_key=REVIEW_AUDIT_RESULT_ROUTING_KEY)

                logger.info("Connected to RabbitMQ, waiting for audit messages...")

                for method, properties, body in channel.consume(REVIEW_AUDIT_QUEUE, auto_ack=True):
                    if self._shutdown_event.is_set():
                        break
                    try:
                        data = json.loads(body)
                        review = ReviewAuditMessage(
                            commentId=data["commentId"],
                            content=data["content"],
                            score=data["score"],
                            shopId=data["shopId"],
                            shopName=data.get("shopName", ""),
                            shopTypeName=data.get("shopTypeName", ""),
                            userId=data["userId"],
                        )
                        logger.info(f"Received audit message: commentId={review.commentId}")

                        result = self.llm.audit(review)

                        result_json = json.dumps({
                            "commentId": result.commentId,
                            "risk": result.risk,
                            "reason": result.reason,
                            "flags": result.flags,
                        }, ensure_ascii=False)
                        channel.basic_publish(
                            exchange=REVIEW_AUDIT_EXCHANGE,
                            routing_key=REVIEW_AUDIT_RESULT_ROUTING_KEY,
                            body=result_json.encode("utf-8"),
                        )

                        logger.info(f"Audit result published: commentId={result.commentId}, risk={result.risk}")

                    except Exception as e:
                        logger.error(f"Error processing audit message: {e}", exc_info=True)

                connection.close()

            except pika.exceptions.AMQPConnectionError:
                logger.warning("RabbitMQ connection failed, retrying in 5s...")
                self._sleep(5)
            except Exception as e:
                logger.error(f"Unexpected consumer error, retrying in 5s: {e}")
                self._sleep(5)

    @staticmethod
    def _sleep(seconds: int):
        import time
        time.sleep(seconds)
