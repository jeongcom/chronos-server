dependencies {
    implementation(project(":chronos-domain"))
    implementation(project(":chronos-application"))
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework:spring-jdbc")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.springframework.data:spring-data-redis")
    implementation("tools.jackson.core:jackson-databind")
    runtimeOnly("org.postgresql:postgresql")
}
