package news.producer.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndContentImpl;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndEntryImpl;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.feed.synd.SyndFeedImpl;
import news.producer.parser.RssItemProcessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RssToKafkaJobTest {

  @Mock
  private RestTemplate restTemplate;

  @Mock
  private KafkaTemplate<String, String> kafkaTemplate;

  @InjectMocks
  private RssToKafkaJob rssToKafkaJob;

  @Captor
  private ArgumentCaptor<String> topicCaptor;

  @Captor
  private ArgumentCaptor<String> keyCaptor;

  @Captor
  private ArgumentCaptor<String> valueCaptor;

  private final String RSS_URL = "https://example.com/rss";
  private final String KAFKA_TOPIC = "news-topic";
  private final String CHARSET = "UTF-8";

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(rssToKafkaJob, "rssUrl", RSS_URL);
    ReflectionTestUtils.setField(rssToKafkaJob, "kafkaTopic", KAFKA_TOPIC);
    ReflectionTestUtils.setField(rssToKafkaJob, "charset", CHARSET);
  }

  @Test
  void shouldSuccessfullyFetchAndSendToKafka() throws Exception {
    // Given
    String rssFeedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><rss version=\"2.0\"><channel><item><title>Test Title</title><link>http://example.com/article</link><description>Test Description</description><guid>123456</guid><pubDate>Thu, 01 Jan 2023 12:00:00 GMT</pubDate></item></channel></rss>";
    byte[] responseBody = rssFeedXml.getBytes(StandardCharsets.UTF_8);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_XML);

    ResponseEntity<byte[]> responseEntity = new ResponseEntity<>(responseBody, headers, HttpStatus.OK);

    when(restTemplate.exchange(
        eq(RSS_URL),
        eq(HttpMethod.GET),
        isNull(),
        eq(byte[].class)
    )).thenReturn(responseEntity);

    // Mock RssItemProcessor with a static mock
    MockedStatic<RssItemProcessor> mockedProcessor = mockStatic(RssItemProcessor.class);
    Map<String, Object> processedItem = Map.of(
        "guid", "123456",
        "title", "Test Title",
        "link", "http://example.com/article",
        "description", "Test Description",
        "pubDate", "Thu, 01 Jan 2023 12:00:00 GMT"
    );

    mockedProcessor.when(() -> RssItemProcessor.processRssItem(any(SyndEntry.class)))
        .thenReturn(processedItem);

    when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(null);

    // When
    rssToKafkaJob.fetchRssAndSendKafka();

    // Then
    verify(restTemplate).exchange(eq(RSS_URL), eq(HttpMethod.GET), isNull(), eq(byte[].class));
    verify(kafkaTemplate).send(topicCaptor.capture(), keyCaptor.capture(), valueCaptor.capture());

    // Verify the values sent to Kafka
    assert topicCaptor.getValue().equals(KAFKA_TOPIC);
    assert keyCaptor.getValue().equals("123456");

    // Verify JSON string contains expected values
    String jsonValue = valueCaptor.getValue();
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> sentItem = mapper.readValue(jsonValue, Map.class);

    assert sentItem.get("guid").equals("123456");
    assert sentItem.get("title").equals("Test Title");

    mockedProcessor.close();
  }

  @Test
  void shouldHandleCharsetFromResponseHeaders() throws Exception {
    // Given
    String rssFeedXml = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><rss version=\"2.0\"><channel><item><title>Test Title</title><link>http://example.com/article</link><description>Test Description</description><guid>123456</guid><pubDate>Thu, 01 Jan 2023 12:00:00 GMT</pubDate></item></channel></rss>";
    byte[] responseBody = rssFeedXml.getBytes(StandardCharsets.ISO_8859_1);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(new MediaType("application", "xml", StandardCharsets.ISO_8859_1));

    ResponseEntity<byte[]> responseEntity = new ResponseEntity<>(responseBody, headers, HttpStatus.OK);

    when(restTemplate.exchange(
        eq(RSS_URL),
        eq(HttpMethod.GET),
        isNull(),
        eq(byte[].class)
    )).thenReturn(responseEntity);

    // Mock RssItemProcessor
    MockedStatic<RssItemProcessor> mockedProcessor = mockStatic(RssItemProcessor.class);
    Map<String, Object> processedItem = Map.of(
        "guid", "123456",
        "title", "Test Title"
    );

    mockedProcessor.when(() -> RssItemProcessor.processRssItem(any(SyndEntry.class)))
        .thenReturn(processedItem);

    // When
    rssToKafkaJob.fetchRssAndSendKafka();

    // Then
    verify(restTemplate).exchange(eq(RSS_URL), eq(HttpMethod.GET), isNull(), eq(byte[].class));
    verify(kafkaTemplate).send(anyString(), anyString(), anyString());

    // Verify charset was updated from headers
    assert ReflectionTestUtils.getField(rssToKafkaJob, "charset").equals("ISO-8859-1");

    mockedProcessor.close();
  }

  @Test
  void shouldHandleMultipleRssEntries() throws Exception {
    // Given
    String rssFeedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><rss version=\"2.0\"><channel><item><title>Article 1</title><guid>id1</guid></item><item><title>Article 2</title><guid>id2</guid></item></channel></rss>";
    byte[] responseBody = rssFeedXml.getBytes(StandardCharsets.UTF_8);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_XML);

    ResponseEntity<byte[]> responseEntity = new ResponseEntity<>(responseBody, headers, HttpStatus.OK);

    when(restTemplate.exchange(
        eq(RSS_URL),
        eq(HttpMethod.GET),
        isNull(),
        eq(byte[].class)
    )).thenReturn(responseEntity);

    // Mock RssItemProcessor
    MockedStatic<RssItemProcessor> mockedProcessor = mockStatic(RssItemProcessor.class);

    Map<String, Object> processedItem1 = Map.of("guid", "id1", "title", "Article 1");
    Map<String, Object> processedItem2 = Map.of("guid", "id2", "title", "Article 2");

    mockedProcessor.when(() -> RssItemProcessor.processRssItem(argThat(entry ->
            ((SyndEntry)entry).getTitle().equals("Article 1"))))
        .thenReturn(processedItem1);

    mockedProcessor.when(() -> RssItemProcessor.processRssItem(argThat(entry ->
            ((SyndEntry)entry).getTitle().equals("Article 2"))))
        .thenReturn(processedItem2);

    // When
    rssToKafkaJob.fetchRssAndSendKafka();

    // Then
    verify(restTemplate).exchange(eq(RSS_URL), eq(HttpMethod.GET), isNull(), eq(byte[].class));
    verify(kafkaTemplate, times(2)).send(anyString(), anyString(), anyString());

    mockedProcessor.close();
  }

  @Test
  void shouldHandleRestTemplateException() {
    // Given
    when(restTemplate.exchange(
        eq(RSS_URL),
        eq(HttpMethod.GET),
        isNull(),
        eq(byte[].class)
    )).thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

    // When
    rssToKafkaJob.fetchRssAndSendKafka();

    // Then
    verify(restTemplate).exchange(eq(RSS_URL), eq(HttpMethod.GET), isNull(), eq(byte[].class));
    verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
  }

  @Test
  void shouldHandleRssParsingException() throws Exception {
    // Given
    String invalidRssFeedXml = "This is not valid XML";
    byte[] responseBody = invalidRssFeedXml.getBytes(StandardCharsets.UTF_8);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_XML);

    ResponseEntity<byte[]> responseEntity = new ResponseEntity<>(responseBody, headers, HttpStatus.OK);

    when(restTemplate.exchange(
        eq(RSS_URL),
        eq(HttpMethod.GET),
        isNull(),
        eq(byte[].class)
    )).thenReturn(responseEntity);

    // When
    rssToKafkaJob.fetchRssAndSendKafka();

    // Then
    verify(restTemplate).exchange(eq(RSS_URL), eq(HttpMethod.GET), isNull(), eq(byte[].class));
    verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
  }

  @Test
  void shouldHandleKafkaException() throws Exception {
    // Given
    String rssFeedXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><rss version=\"2.0\"><channel><item><title>Test Title</title><guid>123456</guid></item></channel></rss>";
    byte[] responseBody = rssFeedXml.getBytes(StandardCharsets.UTF_8);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_XML);

    ResponseEntity<byte[]> responseEntity = new ResponseEntity<>(responseBody, headers, HttpStatus.OK);

    when(restTemplate.exchange(
        eq(RSS_URL),
        eq(HttpMethod.GET),
        isNull(),
        eq(byte[].class)
    )).thenReturn(responseEntity);

    // Mock RssItemProcessor
    MockedStatic<RssItemProcessor> mockedProcessor = mockStatic(RssItemProcessor.class);
    Map<String, Object> processedItem = Map.of("guid", "123456", "title", "Test Title");

    mockedProcessor.when(() -> RssItemProcessor.processRssItem(any(SyndEntry.class)))
        .thenReturn(processedItem);

    when(kafkaTemplate.send(anyString(), anyString(), anyString()))
        .thenThrow(new RuntimeException("Kafka error"));

    // When
    rssToKafkaJob.fetchRssAndSendKafka();

    // Then
    verify(restTemplate).exchange(eq(RSS_URL), eq(HttpMethod.GET), isNull(), eq(byte[].class));
    verify(kafkaTemplate).send(anyString(), anyString(), anyString());

    mockedProcessor.close();
  }
}
