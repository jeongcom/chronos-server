plugins {
    java
    id("org.springframework.boot") version "4.1.0" apply false
    id("com.google.protobuf") version "0.10.0" apply false
}

allprojects {
    group = "com.chronos"
    version = "0.1.0-SNAPSHOT"
}

subprojects {
    apply(plugin = "java")

    dependencies {
        add("implementation", platform("org.springframework.boot:spring-boot-dependencies:4.1.0"))
        add("testImplementation", platform("org.springframework.boot:spring-boot-dependencies:4.1.0"))
    }

    java {
        toolchain { languageVersion.set(JavaLanguageVersion.of(25)) }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
    }

    tasks.withType<Test>().configureEach { useJUnitPlatform() }
}
