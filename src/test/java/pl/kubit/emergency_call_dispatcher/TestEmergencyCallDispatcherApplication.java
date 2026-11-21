package pl.kubit.emergency_call_dispatcher;

import org.springframework.boot.SpringApplication;

public class TestEmergencyCallDispatcherApplication {

	public static void main(String[] args) {
		SpringApplication.from(EmergencyCallDispatcherApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
