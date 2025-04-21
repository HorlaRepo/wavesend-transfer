package com.shizzy.moneytransfer.kafka;

import com.shizzy.moneytransfer.dto.ScheduledTransferNotification;
import com.shizzy.moneytransfer.dto.TransactionNotification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationProducer implements KafkaNotificationProducer {

    private static final Logger logger = LoggerFactory.getLogger(NotificationProducer.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendNotification(String topic, TransactionNotification transactionNotification) {
        logger.info("Sending transaction notification with body = < {} >", transactionNotification);
        kafkaTemplate.send(topic, transactionNotification);
    }
    
    @Override
    public <T> void sendNotification(String topic, T notification) {
        logger.info("Sending notification of type {} with body = < {} >", 
                notification.getClass().getSimpleName(), notification);
        kafkaTemplate.send(topic, notification);
    }
}

