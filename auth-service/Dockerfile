# =============================================================
# auth-service — Multi-stage build
# Stage 1: compile with Maven + JDK 21
# Stage 2: lean runtime with JRE 21 Alpine (~100 MB final image)
# =============================================================

# ---- Stage 1: Build ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Cache dependencies separately from source (invalidated only when pom.xml changes)
COPY pom.xml .
RUN mvn dependency:go-offline -B -q

COPY src ./src
RUN mvn package -DskipTests -B -q

# ---- Stage 2: Runtime ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S fincore && adduser -S fincore -G fincore

COPY --from=build /app/target/*.jar app.jar

USER fincore
EXPOSE 8081

ENTRYPOINT ["java", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
