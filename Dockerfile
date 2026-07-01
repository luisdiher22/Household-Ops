# Multi-stage build: compiles the React app and the Spring Boot jar together
# (frontend-maven-plugin fetches its own Node, no separate Node stage needed),
# then ships just the runtime jar in a slim JRE image.

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY backend backend
COPY frontend frontend
WORKDIR /workspace/backend
RUN mvn -B clean package -Pbundle-frontend -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/backend/target/app.jar app.jar
EXPOSE 8090
ENTRYPOINT ["java", "-jar", "app.jar"]
