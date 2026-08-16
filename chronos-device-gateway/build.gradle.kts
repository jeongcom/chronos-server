plugins { id("org.springframework.boot") }

val grpcVersion = "1.83.1"
val nettyVersion = "4.2.17.Final"

dependencies {
    implementation(project(":chronos-contract"))

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("io.netty:netty-buffer:$nettyVersion")
    implementation("io.netty:netty-codec:$nettyVersion")
    implementation("io.netty:netty-common:$nettyVersion")
    implementation("io.netty:netty-handler:$nettyVersion")
    implementation("io.netty:netty-transport:$nettyVersion")

    implementation("io.grpc:grpc-netty-shaded:$grpcVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")
    implementation("io.grpc:grpc-protobuf:$grpcVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.netty:netty-transport:$nettyVersion")
}
