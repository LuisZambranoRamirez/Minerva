FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw \
    && ./mvnw --batch-mode dependency:go-offline

COPY src/ src/
RUN ./mvnw --batch-mode -Dmaven.test.skip=true package \
    && cp target/minerva-0.0.1-SNAPSHOT.jar /workspace/app.jar

FROM eclipse-temurin:25-jre AS runtime

WORKDIR /app
COPY --from=build --chown=10001:10001 /workspace/app.jar /app/app.jar

USER 10001:10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
