FROM eclipse-temurin:25-jdk AS builder
WORKDIR /workspace

COPY gradlew gradlew.bat build.gradle settings.gradle ./
COPY gradle ./gradle
COPY src ./src

RUN chmod +x ./gradlew && ./gradlew --no-daemon clean installApp

FROM eclipse-temurin:25-jre
WORKDIR /opt/jpos

COPY --from=builder /workspace/build/install/jpos-template-card/ ./

EXPOSE 8080

ENTRYPOINT ["./bin/q2"]
