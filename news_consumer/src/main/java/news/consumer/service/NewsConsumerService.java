package news.consumer.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import news.consumer.dto.NewsArticle;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.springframework.boot.autoconfigure.jms.AcknowledgeMode;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.TopicPartition;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Slf4j
@RequiredArgsConstructor
public class NewsConsumerService {

  private final RedisTemplate<String, NewsArticle> redisTemplate;

  private final ObjectMapper objectMapper;

  @KafkaListener(
      topics = "${spring.kafka.topic}",
      groupId = "${spring.kafka.consumer.group-id}",
      containerFactory = "kafkaManualAckListenerContainerFactory"
  )
  public void consume(List<ConsumerRecord<String, String>> records, Acknowledgment acknowledgment) {
    log.info("Received batch of {} messages", records.size());
    boolean allSuccessful = true;

    for (ConsumerRecord<String, String> record : records) {
      try {
        processRecord(record);
      } catch (Exception e) {
        log.error("Error processing record: " + e.getMessage(), e);
        allSuccessful = false;
        break;  // Stop processing on first error
      }
    }

    if (allSuccessful) {
      acknowledgment.acknowledge();
      log.info("Successfully processed and acknowledged batch of {} messages", records.size());
    } else {
      log.warn("Batch had errors, not acknowledging. Will be redelivered.");
    }
  }

  private void processRecord(ConsumerRecord<String, String> record) throws Exception {
    // Your existing processing logic with idempotency checks
    NewsArticle article = objectMapper.readValue(record.value(), NewsArticle.class);

    // Skip if article is older than 72 hours
    if (article.getPublishedAt().isBefore(LocalDateTime.now().minus(72, ChronoUnit.HOURS))) {
      log.info("Skipping old article: " + article.getTitle());
      return;
    }

    final String redisKey = "article:" + article.getId();
    Boolean keyExists = redisTemplate.hasKey(redisKey);
    if (keyExists != null && keyExists) {
      log.info("Article already exists in Redis: " + article.getTitle());
      return;
    }

    // Save to Redis with 24-hour TTL
    redisTemplate.opsForValue().set(redisKey, article);
    redisTemplate.expire(redisKey, 24, TimeUnit.HOURS);

    log.info("Processed and saved article: " + article.getTitle());
  }
}
