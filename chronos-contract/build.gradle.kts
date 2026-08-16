plugins {
    `java-library`
    id("com.google.protobuf")
}

val grpcVersion = "1.83.1"
val protobufVersion = "4.32.0"

dependencies {
    api("com.google.protobuf:protobuf-java:$protobufVersion")
    api("io.grpc:grpc-protobuf:$grpcVersion")
    api("io.grpc:grpc-stub:$grpcVersion")
    compileOnly("javax.annotation:javax.annotation-api:1.3.2")
}

protobuf {
    protoc { artifact = "com.google.protobuf:protoc:$protobufVersion" }
    plugins {
        create("grpc") { artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion" }
    }
    generateProtoTasks {
        all().configureEach { plugins { create("grpc") } }
    }
}
