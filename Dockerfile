# ---- Build stage -----------------------------------------------------------
FROM maven:3.9.6-eclipse-temurin-21-jammy AS build
WORKDIR /app
# Download dependencies first so this layer is cached when only source code changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline || true
COPY src ./src
# Tests run in GitHub Actions (they need Docker for PostgreSQL); skip them here to keep deploys fast.
RUN mvn -B -q package -DskipTests

# ---- Run stage -------------------------------------------------------------
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# Run as an unprivileged user, not root.
RUN useradd --system --uid 1001 linkly
COPY --from=build /app/target/linkly-0.0.1-SNAPSHOT.jar app.jar
USER linkly
# Render's free instance has 512 MB RAM: size the heap from the container limit,
# use the low-overhead Serial GC and smaller thread stacks.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
