package news.producer.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import java.io.StringReader;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import news.producer.parser.RssItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.xml.sax.InputSource;

@Component
@RequiredArgsConstructor
@Slf4j
public class RssToKafkaJob {

  private final RestTemplate restTemplate;
  private final KafkaTemplate<String, String> kafkaTemplate;

  @Value("${spring.rss.url}")
  private String rssUrl;

  @Value("${spring.kafka.topic}")
  private String kafkaTopic;

  @Value("${spring.rss.charset}")
  private String charset;

  @Scheduled(fixedRateString ="${spring.rss.fetch-rate}", initialDelay=1000)
  public void fetchRssAndSendKafka() {
    log.info("Start Fetching RSS feed from URL: {}", rssUrl);
    try {
      ResponseEntity<byte[]> response = restTemplate.exchange(
          rssUrl,
          HttpMethod.GET,
          null,
          byte[].class
      );
      HttpHeaders headers = response.getHeaders();
      MediaType contentType = headers.getContentType();
      if (contentType != null && contentType.getCharset() != null) {
        charset = contentType.getCharset().name();
      }
      String rssFeed = new String(response.getBody(), charset);
      final SyndFeedInput input = new SyndFeedInput();
      final InputSource source = new InputSource(new StringReader(rssFeed));
      final SyndFeed feed = input.build(source);
      final List<SyndEntry> entries = feed.getEntries();
      for (SyndEntry entry : entries) {
        Map<String, Object> newsItem= RssItemProcessor.processRssItem(entry);
        ObjectMapper objectMapper = new ObjectMapper();
        kafkaTemplate.send(kafkaTopic, (String) newsItem.get("guid"), objectMapper.writeValueAsString(newsItem));
      }
      log.info("Successfully sent RSS feed to Kafka topic: {}", kafkaTopic);
    }
    catch (Exception e) {
      log.error("Error fetching RSS feed or sending to Kafka: {}", e.getMessage());
    }
  }

}
