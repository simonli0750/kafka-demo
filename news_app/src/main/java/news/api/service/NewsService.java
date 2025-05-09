package news.api.service;

import lombok.RequiredArgsConstructor;
import news.api.dto.NewsArticle;
import news.api.repository.NewsRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NewsService {

  private final NewsRepository newsRepository;

  public Page<NewsArticle> getAllNews(Pageable pageable) {
    return newsRepository.findAll(pageable);
  }
}
