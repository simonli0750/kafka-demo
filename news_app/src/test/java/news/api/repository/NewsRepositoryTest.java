package news.api.repository;

import news.api.dto.NewsArticle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NewsRepositoryTest {

  @Mock
  private RedisTemplate<String, NewsArticle> redisTemplate;

  @Mock
  private ValueOperations<String, NewsArticle> valueOps;

  @InjectMocks
  private NewsRepository newsRepository;

  private NewsArticle article1;
  private NewsArticle article2;
  private NewsArticle article3;
  private Set<String> articleKeys;

  @BeforeEach
  void setUp() {
    // Setup test articles
    article1 = new NewsArticle();
    article1.setId("1");
    article1.setTitle("Breaking News");
    article1.setCreator("John Doe");
    article1.setPublishedAt(LocalDateTime.now().minusHours(1));

    article2 = new NewsArticle();
    article2.setId("2");
    article2.setTitle("Weather Update");
    article2.setCreator("Jane Smith");
    article2.setPublishedAt(LocalDateTime.now().minusHours(2));

    article3 = new NewsArticle();
    article3.setId("3");
    article3.setTitle("Technology News");
    article3.setCreator("Bob Johnson");
    article3.setPublishedAt(LocalDateTime.now().minusHours(3));

    // Setup keys
    articleKeys = new HashSet<>();
    articleKeys.add("article:1");
    articleKeys.add("article:2");
    articleKeys.add("article:3");

    // Setup Redis template mock
    when(redisTemplate.opsForValue()).thenReturn(valueOps);
  }

  @Test
  void findAll_ShouldReturnAllArticlesSortedByPublishedAtDesc() {
    // Given
    Pageable pageable = PageRequest.of(0, 10);

    when(redisTemplate.keys("article:*")).thenReturn(articleKeys);
    when(valueOps.get("article:1")).thenReturn(article1);
    when(valueOps.get("article:2")).thenReturn(article2);
    when(valueOps.get("article:3")).thenReturn(article3);

    // When
    Page<NewsArticle> result = newsRepository.findAll(pageable);

    // Then
    assertEquals(3, result.getTotalElements());
    assertEquals(3, result.getContent().size());

    // Default sorting is by publishedAt desc, so article1 should be first
    assertEquals("1", result.getContent().get(0).getId());
    assertEquals("2", result.getContent().get(1).getId());
    assertEquals("3", result.getContent().get(2).getId());

    verify(redisTemplate).keys("article:*");
    verify(valueOps).get("article:1");
    verify(valueOps).get("article:2");
    verify(valueOps).get("article:3");
  }

  @Test
  void findAll_WithSortByTitle_ShouldReturnArticlesSortedByTitle() {
    // Given
    Pageable pageable = PageRequest.of(0, 10, Sort.by("title").ascending());

    when(redisTemplate.keys("article:*")).thenReturn(articleKeys);
    when(valueOps.get("article:1")).thenReturn(article1);
    when(valueOps.get("article:2")).thenReturn(article2);
    when(valueOps.get("article:3")).thenReturn(article3);

    // When
    Page<NewsArticle> result = newsRepository.findAll(pageable);

    // Then
    assertEquals(3, result.getTotalElements());

    // Sorted by title asc: Breaking News, Technology News, Weather Update
    assertEquals("Breaking News", result.getContent().get(0).getTitle());
    assertEquals("Technology News", result.getContent().get(1).getTitle());
    assertEquals("Weather Update", result.getContent().get(2).getTitle());
  }

  @Test
  void findAll_WithSortByCreatorDesc_ShouldReturnArticlesSortedByCreatorDesc() {
    // Given
    Pageable pageable = PageRequest.of(0, 10, Sort.by("creator").descending());

    when(redisTemplate.keys("article:*")).thenReturn(articleKeys);
    when(valueOps.get("article:1")).thenReturn(article1);
    when(valueOps.get("article:2")).thenReturn(article2);
    when(valueOps.get("article:3")).thenReturn(article3);

    // When
    Page<NewsArticle> result = newsRepository.findAll(pageable);

    // Then
    assertEquals(3, result.getTotalElements());

    // Sorted by creator desc: John Doe, Jane Smith, Bob Johnson
    assertEquals("John Doe", result.getContent().get(0).getCreator());
    assertEquals("Jane Smith", result.getContent().get(1).getCreator());
    assertEquals("Bob Johnson", result.getContent().get(2).getCreator());
  }

  @Test
  void findAll_WithPagination_ShouldReturnCorrectPage() {
    // Given
    Pageable pageable = PageRequest.of(1, 1); // Second page, 1 item per page

    when(redisTemplate.keys("article:*")).thenReturn(articleKeys);
    when(valueOps.get("article:1")).thenReturn(article1);
    when(valueOps.get("article:2")).thenReturn(article2);
    when(valueOps.get("article:3")).thenReturn(article3);

    // When
    Page<NewsArticle> result = newsRepository.findAll(pageable);

    // Then
    assertEquals(3, result.getTotalElements()); // Total elements
    assertEquals(1, result.getContent().size()); // Items on this page
    assertEquals(3, result.getTotalPages()); // Total pages
    assertEquals("2", result.getContent().get(0).getId()); // Second article on second page
  }

  @Test
  void findAll_WithNullValues_ShouldSkipNullArticles() {
    // Given
    Pageable pageable = PageRequest.of(0, 10);

    when(redisTemplate.keys("article:*")).thenReturn(articleKeys);
    when(valueOps.get("article:1")).thenReturn(article1);
    when(valueOps.get("article:2")).thenReturn(null); // Null article
    when(valueOps.get("article:3")).thenReturn(article3);

    // When
    Page<NewsArticle> result = newsRepository.findAll(pageable);

    // Then
    assertEquals(2, result.getTotalElements());
    assertEquals(2, result.getContent().size());

    // Should contain article1 and article3, but not article2
    assertTrue(result.getContent().stream().anyMatch(a -> a.getId().equals("1")));
    assertTrue(result.getContent().stream().anyMatch(a -> a.getId().equals("3")));
    assertFalse(result.getContent().stream().anyMatch(a -> a.getId().equals("2")));
  }

  @Test
  void findById_ShouldReturnArticle() {
    // Given
    when(valueOps.get("article:1")).thenReturn(article1);

    // When
    NewsArticle result = newsRepository.findById("1");

    // Then
    assertNotNull(result);
    assertEquals("1", result.getId());
    assertEquals("Breaking News", result.getTitle());
    verify(valueOps).get("article:1");
  }

  @Test
  void findById_WithNonExistentId_ShouldReturnNull() {
    // Given
    when(valueOps.get("article:999")).thenReturn(null);

    // When
    NewsArticle result = newsRepository.findById("999");

    // Then
    assertNull(result);
    verify(valueOps).get("article:999");
  }
}
