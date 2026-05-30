package com.hmdp.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String SECKILL_ORDER_EXCHANGE = "seckill.order.exchange";
    public static final String SECKILL_ORDER_QUEUE = "seckill.order.queue";
    public static final String SECKILL_ORDER_ROUTING_KEY = "seckill.order";

    public static final String REVIEW_AUDIT_EXCHANGE = "review.audit.exchange";
    public static final String REVIEW_AUDIT_QUEUE = "review.audit.queue";
    public static final String REVIEW_AUDIT_ROUTING_KEY = "review.audit";
    public static final String REVIEW_AUDIT_RESULT_QUEUE = "review.audit.result.queue";
    public static final String REVIEW_AUDIT_RESULT_ROUTING_KEY = "review.audit.result";

    public static final String RECOMMEND_EXCHANGE = "recommend.exchange";
    public static final String RECOMMEND_QUEUE = "recommend.queue";
    public static final String RECOMMEND_ROUTING_KEY = "recommend.request";
    public static final String RECOMMEND_RESULT_QUEUE = "recommend.result.queue";
    public static final String RECOMMEND_RESULT_ROUTING_KEY = "recommend.result";

    @Bean
    public DirectExchange seckillOrderExchange() {
        return new DirectExchange(SECKILL_ORDER_EXCHANGE);
    }

    @Bean
    public Queue seckillOrderQueue() {
        return new Queue(SECKILL_ORDER_QUEUE, true);
    }

    @Bean
    public Binding seckillOrderBinding(Queue seckillOrderQueue, DirectExchange seckillOrderExchange) {
        return BindingBuilder.bind(seckillOrderQueue).to(seckillOrderExchange).with(SECKILL_ORDER_ROUTING_KEY);
    }

    @Bean
    public DirectExchange reviewAuditExchange() {
        return new DirectExchange(REVIEW_AUDIT_EXCHANGE);
    }

    @Bean
    public Queue reviewAuditQueue() {
        return new Queue(REVIEW_AUDIT_QUEUE, true);
    }

    @Bean
    public Binding reviewAuditBinding(Queue reviewAuditQueue, DirectExchange reviewAuditExchange) {
        return BindingBuilder.bind(reviewAuditQueue).to(reviewAuditExchange).with(REVIEW_AUDIT_ROUTING_KEY);
    }

    @Bean
    public Queue reviewAuditResultQueue() {
        return new Queue(REVIEW_AUDIT_RESULT_QUEUE, true);
    }

    @Bean
    public Binding reviewAuditResultBinding(Queue reviewAuditResultQueue, DirectExchange reviewAuditExchange) {
        return BindingBuilder.bind(reviewAuditResultQueue).to(reviewAuditExchange).with(REVIEW_AUDIT_RESULT_ROUTING_KEY);
    }

    @Bean
    public DirectExchange recommendExchange() {
        return new DirectExchange(RECOMMEND_EXCHANGE);
    }

    @Bean
    public Queue recommendQueue() {
        return new Queue(RECOMMEND_QUEUE, true);
    }

    @Bean
    public Binding recommendBinding(Queue recommendQueue, DirectExchange recommendExchange) {
        return BindingBuilder.bind(recommendQueue).to(recommendExchange).with(RECOMMEND_ROUTING_KEY);
    }

    @Bean
    public Queue recommendResultQueue() {
        return new Queue(RECOMMEND_RESULT_QUEUE, true);
    }

    @Bean
    public Binding recommendResultBinding(Queue recommendResultQueue, DirectExchange recommendExchange) {
        return BindingBuilder.bind(recommendResultQueue).to(recommendExchange).with(RECOMMEND_RESULT_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.getJavaTypeMapper().addTrustedPackages("com.hmdp.dto");
        return converter;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }
}
