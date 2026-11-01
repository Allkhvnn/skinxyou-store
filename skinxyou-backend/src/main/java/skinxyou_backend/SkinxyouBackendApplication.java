package skinxyou_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SkinxyouBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SkinxyouBackendApplication.class, args);
	}

}
