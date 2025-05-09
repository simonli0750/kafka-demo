package news.api.repository;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import news.api.dto.NewsArticle;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

@Repository
@RequiredArgsConstructor
@Slf4j
public class NewsRepository {

  private final RedisTemplate<String, NewsArticle> redisTemplate;

  public Page<NewsArticle> findAll(Pageable pageable) {
    // Get all keys matching the pattern "article:*"
    Set<String> keys = redisTemplate.keys("article:*");
    if (keys == null || keys.isEmpty()) {
      log.info("No article keys found in Redis");
      return new PageImpl<>(Collections.emptyList(), pageable, 0);
    }

    log.info("Found " + keys.size() + " article keys in Redis");
    List<NewsArticle> articles = new ArrayList<>();

    // Get values for all keys - now directly as NewsArticle objects
    for (String key : keys) {
      NewsArticle article = redisTemplate.opsForValue().get(key);
      if (article != null) {
        articles.add(article);
      } else {
        log.warn("Null value found for key: " + key);
      }
    }

    // Apply sorting if specified
    if (pageable.getSort().isSorted()) {
      pageable.getSort().forEach(order -> {
        String property = order.getProperty();

        Comparator<NewsArticle> comparator = switch (property) {
          case "title" -> Comparator.comparing(NewsArticle::getTitle);
          case "pubDate" -> Comparator.comparing(NewsArticle::getPublishedAt);
          case "creator" -> Comparator.comparing(NewsArticle::getCreator);
          default -> Comparator.comparing(NewsArticle::getPublishedAt);
        };

        if (order.isDescending()) {
          comparator = comparator.reversed();
        }

        articles.sort(comparator);
      });
    } else {
      // Default sort by pubDate desc
      articles.sort(Comparator.comparing(NewsArticle::getPublishedAt).reversed());
    }

    // Apply pagination
    int start = (int) pageable.getOffset();
    int end = Math.min((start + pageable.getPageSize()), articles.size());

    if (start > articles.size()) {
      log.info("Requested page exceeds available articles");
      return new PageImpl<>(Collections.emptyList(), pageable, articles.size());
    }

    List<NewsArticle> pagedArticles = articles.subList(start, end);
    log.info("Returning page with " + pagedArticles.size() + " articles");
    return new PageImpl<>(pagedArticles, pageable, articles.size());
  }

  // Get a single article by ID
  public NewsArticle findById(String id) {
    String key = "article:" + id;
    return redisTemplate.opsForValue().get(key);
  }
}
