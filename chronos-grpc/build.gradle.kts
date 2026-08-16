val grpcVersion = "1.83.1"
dependencies {
    implementation(project(":chronos-domain"))
    implementation(project(":chronos-application"))
    implementation(project(":chronos-contract"))
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")
    implementation("org.springframework:spring-context")
}
