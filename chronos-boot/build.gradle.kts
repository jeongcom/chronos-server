plugins { id("org.springframework.boot") }

val grpcVersion = "1.83.1"

dependencies {
    implementation(project(":chronos-domain"))
    implementation(project(":chronos-application"))
    implementation(project(":chronos-infrastructure"))
    implementation(project(":chronos-api"))
    implementation(project(":chronos-grpc"))
    implementation(project(":chronos-contract"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("io.lettuce:lettuce-core")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    implementation("io.grpc:grpc-netty-shaded:$grpcVersion")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
