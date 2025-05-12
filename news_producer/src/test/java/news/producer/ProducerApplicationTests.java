package news.producer;

import news.producer.repository.ProcessedGuidRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class ProducerApplicationTests {

	@Mock
	private ProcessedGuidRepository processedGuidRepository;

	@Test
	void contextLoads() {
	}

}
