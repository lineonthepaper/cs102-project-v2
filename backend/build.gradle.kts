plugins {
    id("org.springframework.boot") version "3.3.4"
    id("io.spring.dependency-management") version "1.1.6"
    java
}

group = "com.smartattendance"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")

    // Lombok
    compileOnly("org.projectlombok:lombok:1.18.34")
    annotationProcessor("org.projectlombok:lombok:1.18.34")

    // MapStruct for auto-generating DTO mappings
    implementation("org.mapstruct:mapstruct:1.5.5.Final")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")

    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    
    // MapStruct for tests
    testCompileOnly("org.projectlombok:lombok:1.18.34")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.34")
    testAnnotationProcessor("org.mapstruct:mapstruct-processor:1.5.5.Final")

    // OpenCV with native libraries for all platforms (Windows, macOS, Linux)
    implementation("org.openpnp:opencv:4.9.0-0")

    // Batch Importing and Exporting 
    implementation("org.apache.commons:commons-csv:1.10.0")
    implementation("org.apache.poi:poi-ooxml:5.2.5")

    implementation(files("lib/djl.jar"))

    implementation(platform("ai.djl:bom:0.34.0"))

    implementation("ai.djl.pytorch:pytorch-engine")
    implementation("ai.djl.pytorch:pytorch-native-cpu")
    implementation("ai.djl.pytorch:pytorch-jni")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    environment("DATABASE_URL", System.getenv("DATABASE_URL") ?: "")
    environment("DATABASE_USERNAME", System.getenv("DATABASE_USERNAME") ?: "")
    environment("DATABASE_PASSWORD", System.getenv("DATABASE_PASSWORD") ?: "")
    environment("SUPABASE_URL", System.getenv("SUPABASE_URL") ?: "")
    environment("SUPABASE_SERVICE_ROLE_KEY", System.getenv("SUPABASE_SERVICE_ROLE_KEY") ?: "")
    environment("JWT_SECRET", System.getenv("JWT_SECRET") ?: "")
}
