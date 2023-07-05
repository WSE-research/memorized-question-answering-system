package eu.wseresearch.fakequestionansweringsystem;

import eu.wseresearch.fakequestionansweringsystem.triplestoreconnector.TripleStoreConnector;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class ApplicationTests {
	TripleStoreConnector tripleStoreConnector;
	FakeQuestionAnsweringSystem fakeQuestionAnsweringSystem;

	public ApplicationTests(
			@Autowired TripleStoreConnector tripleStoreConnector,
			@Autowired FakeQuestionAnsweringSystem fakeQuestionAnsweringSystem
	) {
		this.tripleStoreConnector = tripleStoreConnector;
		this.fakeQuestionAnsweringSystem = fakeQuestionAnsweringSystem;
	}

	@Test
	void contextLoads() {
		assertNotNull(this.tripleStoreConnector);
		assertNotNull(this.fakeQuestionAnsweringSystem);
	}

}
