# syntax=docker/dockerfile:1
FROM eclipse-temurin:21-jdk-jammy AS source
WORKDIR /workspace/backend
COPY backend/.mvn/ .mvn/
COPY backend/mvnw backend/pom.xml ./
COPY backend/config/ config/
RUN chmod +x mvnw
COPY backend/src/ src/
COPY frontend/ /workspace/frontend/
COPY database/ /workspace/database/

FROM source AS verify
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw --batch-mode --no-transfer-progress verify

FROM source AS build
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw --batch-mode --no-transfer-progress -DskipTests package

FROM eclipse-temurin:21-jre-jammy AS runtime
WORKDIR /app/backend
RUN mkdir -p /app/uploads/menu && chown -R 10001:10001 /app/uploads
COPY --from=build --chown=10001:10001 /workspace/backend/target/*.jar app.jar
COPY --from=build --chown=10001:10001 /workspace/database/schema.sql /app/database/schema.sql
COPY --from=build --chown=10001:10001 /workspace/database/demo-data.sql /app/database/demo-data.sql
USER 10001:10001
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
