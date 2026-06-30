package eu.wseresearch.memorizedquestionansweringsystem;

import eu.wseresearch.memorizedquestionansweringsystem.triplestoreconnector.TripleStoreConnector;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class ApplicationTests {
	@MockitoBean
	TripleStoreConnector tripleStoreConnector;
	MemQASystem memQASystem;

	public ApplicationTests(
			@Autowired MemQASystem memQASystem
	) {
		this.memQASystem = memQASystem;
	}

	@Test
	void contextLoads() {
		assertNotNull(this.tripleStoreConnector);
		assertNotNull(this.memQASystem);
	}

}
