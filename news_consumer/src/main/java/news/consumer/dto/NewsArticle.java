package news.consumer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.time.LocalDateTime;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsArticle {
  @JsonProperty("guid")
  private String id;

  private String title;

  private String link;

  @JsonProperty("description")
  private String content;

  private String creator;

  @JsonProperty("pubDate")
  @JsonDeserialize(converter = DateToLocalDateTimeConverter.class)
  @JsonSerialize(converter = LocalDateTimeToDateConverter.class)
  private LocalDateTime publishedAt;

  private List<String> categories;

  private Media media;

  public static class DateToLocalDateTimeConverter extends StdConverter<Date, LocalDateTime> {
    @Override
    public LocalDateTime convert(Date value) {
      return value == null ? null :
          LocalDateTime.ofInstant(value.toInstant(), ZoneId.systemDefault());
    }
  }

  // Add this converter for serialization
  public static class LocalDateTimeToDateConverter extends StdConverter<LocalDateTime, Date> {
    @Override
    public Date convert(LocalDateTime value) {
      return value == null ? null :
          Date.from(value.atZone(ZoneId.systemDefault()).toInstant());
    }
  }

}
