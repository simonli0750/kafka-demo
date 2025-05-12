package news.producer.entity;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "processedGuids")
@Data
@RequiredArgsConstructor
public class ProcessedGuid {
  @Id
  private final String guid;
}
