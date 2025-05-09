package news.consumer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import news.consumer.dto.NewsArticle;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.kafka.support.Acknowledgment;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NewsConsumerServiceTest {

  @Mock
  private RedisTemplate<String, NewsArticle> redisTemplate;

  @Mock
  private ObjectMapper objectMapper;

  @Mock
  private Acknowledgment acknowledgment;

  @Mock
  private ValueOperations<String, NewsArticle> valueOperations;

  @InjectMocks
  private NewsConsumerService newsConsumerService;

  private NewsArticle recentArticle;
  private NewsArticle oldArticle;
  private String recentArticleJson;
  private String oldArticleJson;

  @BeforeEach
  void setUp() throws Exception {
    // Setup recent article (within 72 hours)
    recentArticle = new NewsArticle();
    recentArticle.setId("recent-123");
    recentArticle.setTitle("Recent News");
    recentArticle.setPublishedAt(LocalDateTime.now().minus(24, ChronoUnit.HOURS));

    // Setup old article (older than 72 hours)
    oldArticle = new NewsArticle();
    oldArticle.setId("old-456");
    oldArticle.setTitle("Old News");
    oldArticle.setPublishedAt(LocalDateTime.now().minus(96, ChronoUnit.HOURS));

    // JSON representations
    recentArticleJson = "{\"id\":\"recent-123\",\"title\":\"Recent News\"}";
    oldArticleJson = "{\"id\":\"old-456\",\"title\":\"Old News\"}";
  }

  @Test
  void shouldProcessAndAcknowledgeBatchSuccessfully() throws Exception {
    // Given
    ConsumerRecord<String, String> record1 = new ConsumerRecord<>("news", 0, 0, "key1", recentArticleJson);
    List<ConsumerRecord<String, String>> records = Arrays.asList(record1);

    when(objectMapper.readValue(recentArticleJson, NewsArticle.class)).thenReturn(recentArticle);
    when(redisTemplate.hasKey("article:recent-123")).thenReturn(false);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations); // Add this here

    // When
    newsConsumerService.consume(records, acknowledgment);

    // Then
    verify(valueOperations).set("article:recent-123", recentArticle); // Update this line
    verify(redisTemplate).expire("article:recent-123", 24, TimeUnit.HOURS);
    verify(acknowledgment).acknowledge();
  }

  @Test
  void shouldSkipOldArticles() throws Exception {
    // Given
    ConsumerRecord<String, String> record = new ConsumerRecord<>("news", 0, 0, "key1", oldArticleJson);
    List<ConsumerRecord<String, String>> records = Arrays.asList(record);

    when(objectMapper.readValue(oldArticleJson, NewsArticle.class)).thenReturn(oldArticle);

    // When
    newsConsumerService.consume(records, acknowledgment);

    // Then
    verify(redisTemplate, never()).opsForValue();
    verify(acknowledgment).acknowledge();
  }

  @Test
  void shouldSkipAlreadyProcessedArticles() throws Exception {
    // Given
    ConsumerRecord<String, String> record = new ConsumerRecord<>("news", 0, 0, "key1", recentArticleJson);
    List<ConsumerRecord<String, String>> records = Arrays.asList(record);

    when(objectMapper.readValue(recentArticleJson, NewsArticle.class)).thenReturn(recentArticle);
    when(redisTemplate.hasKey("article:recent-123")).thenReturn(true);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);

    // When
    newsConsumerService.consume(records, acknowledgment);

    // Then
    verify(redisTemplate.opsForValue(), never()).set(anyString(), any(NewsArticle.class));
    verify(redisTemplate, never()).expire(anyString(), anyLong(), any());
    verify(acknowledgment).acknowledge();
  }

  @Test
  void shouldNotAcknowledgeWhenProcessingFails() throws Exception {
    // Given
    ConsumerRecord<String, String> record = new ConsumerRecord<>("news", 0, 0, "key1", recentArticleJson);
    List<ConsumerRecord<String, String>> records = Arrays.asList(record);

    when(objectMapper.readValue(recentArticleJson, NewsArticle.class)).thenThrow(new RuntimeException("Processing error"));

    // When
    newsConsumerService.consume(records, acknowledgment);

    // Then
    verify(acknowledgment, never()).acknowledge();
  }

  @Test
  void shouldProcessMultipleRecordsSuccessfully() throws Exception {
    // Given
    ConsumerRecord<String, String> record1 = new ConsumerRecord<>("news", 0, 0, "key1", recentArticleJson);

    NewsArticle anotherArticle = new NewsArticle();
    anotherArticle.setId("another-789");
    anotherArticle.setTitle("Another News");
    anotherArticle.setPublishedAt(LocalDateTime.now().minus(12, ChronoUnit.HOURS));
    String anotherArticleJson = "{\"id\":\"another-789\",\"title\":\"Another News\"}";

    ConsumerRecord<String, String> record2 = new ConsumerRecord<>("news", 0, 1, "key2", anotherArticleJson);
    List<ConsumerRecord<String, String>> records = Arrays.asList(record1, record2);

    when(objectMapper.readValue(recentArticleJson, NewsArticle.class)).thenReturn(recentArticle);
    when(objectMapper.readValue(anotherArticleJson, NewsArticle.class)).thenReturn(anotherArticle);
    when(redisTemplate.hasKey("article:recent-123")).thenReturn(false);
    when(redisTemplate.hasKey("article:another-789")).thenReturn(false);
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);

    // When
    newsConsumerService.consume(records, acknowledgment);

    // Then
    verify(redisTemplate.opsForValue()).set("article:recent-123", recentArticle);
    verify(redisTemplate).expire("article:recent-123", 24, TimeUnit.HOURS);
    verify(redisTemplate.opsForValue()).set("article:another-789", anotherArticle);
    verify(redisTemplate).expire("article:another-789", 24, TimeUnit.HOURS);
    verify(acknowledgment).acknowledge();
  }

  @Test
  void shouldStopProcessingOnFirstError() throws Exception {
    // Given
    ConsumerRecord<String, String> record1 = new ConsumerRecord<>("news", 0, 0, "key1", recentArticleJson);
    String invalidJson = "{invalid-json}";
    ConsumerRecord<String, String> record2 = new ConsumerRecord<>("news", 0, 1, "key2", invalidJson);
    List<ConsumerRecord<String, String>> records = Arrays.asList(record1, record2);

    when(objectMapper.readValue(recentArticleJson, NewsArticle.class)).thenReturn(recentArticle);
    when(redisTemplate.hasKey("article:recent-123")).thenReturn(false);
    when(objectMapper.readValue(invalidJson, NewsArticle.class)).thenThrow(new RuntimeException("Invalid JSON"));
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);

    // When
    newsConsumerService.consume(records, acknowledgment);

    // Then
    verify(redisTemplate.opsForValue()).set("article:recent-123", recentArticle);
    verify(acknowledgment, never()).acknowledge();
  }
}
