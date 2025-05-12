package news.producer.job;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import news.producer.entity.ProcessedGuid;
import news.producer.parser.RssItemProcessor;
import news.producer.repository.ProcessedGuidRepository;
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
  private final ProcessedGuidRepository processedGuidRepository;

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
      SyndFeed rssFeed = parseFeed(response.getBody(), charset);
      final List<SyndEntry> entries = rssFeed.getEntries();
      final Set<String> processedGuids = new HashSet<>();
      for (SyndEntry entry : entries) {
        Map<String, Object> newsItem= RssItemProcessor.processRssItem(entry);
        ObjectMapper objectMapper = new ObjectMapper();
        String guid = (String) newsItem.get("guid");
        if (processedGuids.contains(guid) || processedGuidRepository.existsById(guid) ) {
          log.info("Skipping already processed item with GUID: {}", guid);
          continue;
        }
        kafkaTemplate.send(kafkaTopic, guid, objectMapper.writeValueAsString(newsItem));
        processedGuids.add(guid);
      }
      processedGuidRepository.saveAll(processedGuids.stream()
          .map(guid -> new ProcessedGuid(guid))
          .toList());
      log.info("Successfully sent RSS feed to Kafka topic: {}", kafkaTopic);
    }
    catch (Exception e) {
      log.error("Error fetching RSS feed or sending to Kafka: {}", e.getMessage());
    }
  }

  protected SyndFeed parseFeed(byte[] content, String charset) throws Exception {
    String rssFeed = new String(content, charset);
    final SyndFeedInput input = new SyndFeedInput();
    final InputSource source = new InputSource(new StringReader(rssFeed));
    return input.build(source);
  }

}
