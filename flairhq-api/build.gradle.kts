plugins {
    java

	id("org.springframework.boot") version "3.5.15"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.asciidoctor.jvm.convert") version "3.3.2"
}

group = "org.ptrades"
version = "0.0.1-SNAPSHOT"

// Apply a specific Java toolchain to ease working on different environments.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
	compileOnly {
		extendsFrom(configurations.annotationProcessor.get())
	}
}

repositories {
	mavenCentral()
}

extra["snippetsDir"] = file("build/generated-snippets")
extra["springCloudVersion"] = "2025.0.0"

// Security patches for transitive deps pinned by the Spring Boot 3.5.15 BOM.
// Drop these once a Boot release ships the fixed versions itself.
extra["jackson-bom.version"] = "2.21.5" // SNYK-JAVA-COMFASTERXMLJACKSONCORE-17972608
extra["logback.version"] = "1.5.36" // SNYK-JAVA-CHQOSLOGBACK-17675439
extra["tomcat.version"] = "10.1.56" // SNYK-JAVA-ORGAPACHETOMCATEMBED-17732890, -17733746

dependencies {
	// Spring Boot
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-batch")
	implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
	implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
	implementation("org.springframework.boot:spring-boot-starter-security")
	implementation("org.springframework.boot:spring-boot-starter-web")

	implementation("org.springframework.cloud:spring-cloud-starter-circuitbreaker-resilience4j")
	implementation("org.springframework.session:spring-session-data-mongodb")

	// Swagger API documentation
	// Stay on the 2.x line: springdoc 3.x targets Spring Boot 4, this project is on Boot 3.5.
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.17")

	runtimeOnly("io.micrometer:micrometer-registry-prometheus")

	// Lombok annotations
	compileOnly("org.projectlombok:lombok")
	annotationProcessor("org.projectlombok:lombok")

	// MapStruct (annotation processor must come after Lombok)
	implementation("org.mapstruct:mapstruct:1.6.3")
	annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
	annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.springframework.batch:spring-batch-test")
	testImplementation("org.springframework.restdocs:spring-restdocs-mockmvc")
	testImplementation("org.springframework.security:spring-security-test")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

dependencyManagement {
	imports {
		mavenBom("org.springframework.cloud:spring-cloud-dependencies:${property("springCloudVersion")}")
	}
	// Entries here override the imported BOMs. Note that resolutionStrategy.force does NOT:
	// the dependency-management plugin reapplies managed versions after forces are evaluated.
	dependencies {
		dependency("org.apache.commons:commons-lang3:3.20.0") // SNYK-JAVA-ORGAPACHECOMMONS-10734078
		// Spring Cloud's BOM downgrades this below the version Boot manages.
		dependency("org.springframework.retry:spring-retry:2.0.13") // SNYK-JAVA-ORGSPRINGFRAMEWORKRETRY-17260993
	}
}

configurations.all {
	resolutionStrategy {
		// Unmanaged by any BOM, so a force is the right tool here.
		force("org.bouncycastle:bcprov-jdk18on:1.85")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.test {
	outputs.dir(project.extra["snippetsDir"]!!)
}

tasks.asciidoctor {
	inputs.dir(project.extra["snippetsDir"]!!)
	dependsOn(tasks.test)
}
