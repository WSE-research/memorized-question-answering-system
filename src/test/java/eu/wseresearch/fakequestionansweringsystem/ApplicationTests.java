package eu.wseresearch.fakequestionansweringsystem;

import eu.wseresearch.fakequestionansweringsystem.triplestoreconnector.TripleStoreConnector;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class ApplicationTests {
	@MockBean
	TripleStoreConnector tripleStoreConnector;
	FakeQuestionAnsweringSystem fakeQuestionAnsweringSystem;

	public ApplicationTests(
			@Autowired FakeQuestionAnsweringSystem fakeQuestionAnsweringSystem
	) {
		this.fakeQuestionAnsweringSystem = fakeQuestionAnsweringSystem;
	}

	@Test
	void contextLoads() {
		assertNotNull(this.tripleStoreConnector);
		assertNotNull(this.fakeQuestionAnsweringSystem);
	}

}
