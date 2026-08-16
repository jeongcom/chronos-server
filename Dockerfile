FROM gradle:9.7.0-jdk25 AS build
WORKDIR /workspace
COPY . .
RUN gradle :chronos-boot:bootJar --no-daemon

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /workspace/chronos-boot/build/libs/chronos-boot-*.jar app.jar
EXPOSE 8080 9090
ENTRYPOINT ["java","-jar","/app/app.jar"]
