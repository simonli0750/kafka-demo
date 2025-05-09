package news.api.controller;

import lombok.RequiredArgsConstructor;
import news.api.dto.NewsArticle;
import news.api.service.NewsService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

  private final NewsService newsService;

  @GetMapping
  public ResponseEntity<Page<NewsArticle>> getNews(
      @PageableDefault(size = 10, sort = "pubDate,desc") Pageable pageable) {
    Page<NewsArticle> newsPage = newsService.getAllNews(pageable);
    return ResponseEntity.ok(newsPage);
  }
}
