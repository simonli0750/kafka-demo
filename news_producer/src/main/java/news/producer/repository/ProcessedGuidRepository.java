package news.producer.repository;

import news.producer.entity.ProcessedGuid;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProcessedGuidRepository extends MongoRepository<ProcessedGuid, String> {
    boolean existsByGuid(String guid);
}
