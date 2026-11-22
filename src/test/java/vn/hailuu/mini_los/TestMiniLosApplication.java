package vn.hailuu.mini_los;

import org.springframework.boot.SpringApplication;

public class TestMiniLosApplication {

	public static void main(String[] args) {
		SpringApplication.from(MiniLosApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
