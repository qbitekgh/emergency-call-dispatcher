package pl.kubit.emergency_call_dispatcher;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class EmergencyCallDispatcherApplicationTests {

	@Test
	void contextLoads() {
	}

}
