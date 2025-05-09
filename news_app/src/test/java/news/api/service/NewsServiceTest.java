package news.api.service;

import news.api.dto.NewsArticle;
import news.api.repository.NewsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NewsServiceTest {

  @Mock
  private NewsRepository newsRepository;

  @InjectMocks
  private NewsService newsService;

  private List<NewsArticle> articles;
  private Page<NewsArticle> articlePage;

  @BeforeEach
  void setUp() {
    // Create sample articles
    NewsArticle article1 = new NewsArticle();
    article1.setId("1");
    article1.setTitle("Test Article 1");
    article1.setPublishedAt(LocalDateTime.now().minusHours(1));

    NewsArticle article2 = new NewsArticle();
    article2.setId("2");
    article2.setTitle("Test Article 2");
    article2.setPublishedAt(LocalDateTime.now().minusHours(2));

    articles = Arrays.asList(article1, article2);
  }

  @Test
  void getAllNews_ShouldReturnPageOfArticles() {
    // Given
    Pageable pageable = PageRequest.of(0, 10, Sort.by("publishedAt").descending());
    articlePage = new PageImpl<>(articles, pageable, articles.size());

    when(newsRepository.findAll(pageable)).thenReturn(articlePage);

    // When
    Page<NewsArticle> result = newsService.getAllNews(pageable);

    // Then
    assertSame(articlePage, result);
    assertEquals(2, result.getContent().size());
    verify(newsRepository).findAll(pageable);
  }

  @Test
  void getAllNews_WithDifferentPageable_ShouldPassPageableToRepository() {
    // Given
    Pageable pageable = PageRequest.of(1, 5, Sort.by("title").ascending());
    articlePage = new PageImpl<>(articles, pageable, articles.size());

    when(newsRepository.findAll(pageable)).thenReturn(articlePage);

    // When
    Page<NewsArticle> result = newsService.getAllNews(pageable);

    // Then
    assertSame(articlePage, result);
    verify(newsRepository).findAll(pageable);
  }
}
