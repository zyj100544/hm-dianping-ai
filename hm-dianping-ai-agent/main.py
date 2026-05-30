import io
import logging
import signal
import sys
import threading

from consumer.rabbitmq_consumer import ReviewAuditConsumer
from consumer.recommend_consumer import RecommendConsumer

if sys.platform == "win32":
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8")
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding="utf-8")

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
    stream=sys.stdout,
)
logger = logging.getLogger(__name__)


def main():
    shutdown_event = threading.Event()

    audit_consumer = ReviewAuditConsumer()
    audit_consumer.start()

    recommend_consumer = RecommendConsumer(shutdown_event)
    recommend_consumer.start()

    def handle_signal(signum, frame):
        logger.info(f"Received signal {signum}, shutting down...")
        shutdown_event.set()

    signal.signal(signal.SIGINT, handle_signal)
    signal.signal(signal.SIGTERM, handle_signal)

    shutdown_event.wait()
    audit_consumer.stop()
    logger.info("All services stopped")


if __name__ == "__main__":
    main()

