package org.ptrades.flairhq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class FlairhqApplication {

	public static void main(String[] args) {
		SpringApplication.run(FlairhqApplication.class, args);
	}

}