package com.shizzy.moneytransfer.kafka;

/**
 * Generic interface for Kafka notification producers that can send different types of notifications
 */
public interface KafkaNotificationProducer {
    
    /**
     * Send a notification of any type to the specified Kafka topic
     * 
     * @param topic The Kafka topic to send the notification to
     * @param notification The notification object to send
     * @param <T> The type of notification
     */
    <T> void sendNotification(String topic, T notification);
}