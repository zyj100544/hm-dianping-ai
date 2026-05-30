import json
import logging
import threading

from openai import OpenAI

from config import (
    RABBITMQ_HOST, RABBITMQ_PORT, RABBITMQ_USER, RABBITMQ_PASS, RABBITMQ_VHOST,
    RECOMMEND_QUEUE, RECOMMEND_EXCHANGE, RECOMMEND_ROUTING_KEY,
    RECOMMEND_RESULT_QUEUE, RECOMMEND_RESULT_ROUTING_KEY,
    LLM_API_KEY, LLM_BASE_URL, LLM_MODEL, LLM_MAX_RETRIES, LLM_TIMEOUT,
)
from llm.recommend_prompt import SYSTEM_PROMPT, build_user_prompt

logger = logging.getLogger(__name__)


class RecommendConsumer:
    """Consumes recommend requests from RabbitMQ, calls LLM, publishes results."""

    def __init__(self, shutdown_event: threading.Event):
        self.client = OpenAI(api_key=LLM_API_KEY, base_url=LLM_BASE_URL, timeout=LLM_TIMEOUT)
        self.model = LLM_MODEL
        self._shutdown_event = shutdown_event
        self._thread: threading.Thread | None = None

    def start(self):
        self._thread = threading.Thread(target=self._run, daemon=True)
        self._thread.start()
        logger.info("Recommend consumer thread started")

    def _run(self):
        import pika
        import time

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

                channel.exchange_declare(exchange=RECOMMEND_EXCHANGE, exchange_type="direct", durable=True)
                channel.queue_declare(queue=RECOMMEND_QUEUE, durable=True)
                channel.queue_bind(queue=RECOMMEND_QUEUE, exchange=RECOMMEND_EXCHANGE, routing_key=RECOMMEND_ROUTING_KEY)
                channel.queue_declare(queue=RECOMMEND_RESULT_QUEUE, durable=True)
                channel.queue_bind(queue=RECOMMEND_RESULT_QUEUE, exchange=RECOMMEND_EXCHANGE, routing_key=RECOMMEND_RESULT_ROUTING_KEY)

                logger.info("Recommend consumer connected to RabbitMQ")

                for method, properties, body in channel.consume(RECOMMEND_QUEUE, auto_ack=True):
                    if self._shutdown_event.is_set():
                        break
                    try:
                        data = json.loads(body)
                        user_id = data["userId"]
                        profile = data["userProfile"]
                        candidates = data["candidates"]

                        logger.info(f"Processing recommend request: userId={user_id}, candidates={len(candidates)}")

                        user_prompt = build_user_prompt(profile, candidates)
                        recommendations = self._call_llm(user_prompt, user_id)

                        result = {
                            "userId": user_id,
                            "recommendations": recommendations,
                        }
                        channel.basic_publish(
                            exchange=RECOMMEND_EXCHANGE,
                            routing_key=RECOMMEND_RESULT_ROUTING_KEY,
                            body=json.dumps(result, ensure_ascii=False).encode("utf-8"),
                        )
                        logger.info(f"Recommend result published: userId={user_id}, count={len(recommendations)}")

                    except Exception as e:
                        logger.error(f"Error processing recommend: {e}", exc_info=True)

                connection.close()

            except pika.exceptions.AMQPConnectionError:
                logger.warning("RabbitMQ connection failed, retrying in 5s...")
                time.sleep(5)
            except Exception as e:
                logger.error(f"Recommend consumer error, retrying in 5s: {e}")
                time.sleep(5)

    def _call_llm(self, user_prompt: str, user_id: int) -> list:
        import time
        for attempt in range(LLM_MAX_RETRIES):
            try:
                response = self.client.chat.completions.create(
                    model=self.model,
                    messages=[
                        {"role": "system", "content": SYSTEM_PROMPT},
                        {"role": "user", "content": user_prompt},
                    ],
                    temperature=0.3,
                )
                raw = response.choices[0].message.content.strip()
                logger.info(f"LLM recommend response (attempt {attempt + 1}): {raw[:300]}")

                if raw.startswith("```"):
                    lines = raw.split("\n")
                    raw = "\n".join(lines[1:-1])

                data = json.loads(raw)
                return data.get("recommendations", [])

            except json.JSONDecodeError:
                logger.warning(f"Failed to parse LLM result (attempt {attempt + 1})")
            except Exception as e:
                logger.warning(f"LLM call failed (attempt {attempt + 1}): {e}")

        logger.error(f"LLM recommend failed after {LLM_MAX_RETRIES} retries")
        return []
