package ar.com.anepanet.planificator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class PlanificatorApplication {

	static {
		// Postgres rechaza America/Buenos_Aires que envía el driver JDBC
		System.setProperty("user.timezone", "UTC");
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}

	public static void main(String[] args) {
		SpringApplication.run(PlanificatorApplication.class, args);
	}
}
