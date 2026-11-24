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
# Tuned for Render's free instance (512 MB RAM, a fraction of one CPU):
#  - heap sized from the container limit, low-overhead Serial GC, smaller thread stacks
#  - TieredStopAtLevel=1: only the fast C1 JIT, so compilation doesn't starve startup of CPU
#  - classic TLS key-exchange groups that every server (incl. Supabase's pooler) supports
# NOTE: a JAVA_TOOL_OPTIONS variable set in the Render dashboard REPLACES this line.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k -XX:TieredStopAtLevel=1 -XX:+ExitOnOutOfMemoryError -Djdk.tls.namedGroups=x25519,secp256r1,secp384r1"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
