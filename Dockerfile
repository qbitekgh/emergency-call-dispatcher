# Multi-stage build: the JDK and Maven are needed only to compile the jar,
# so the final image contains just a JRE and the application.

# ---- Stage 1: build the jar ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Copy the build files first and download dependencies in a separate layer.
# Docker reuses this layer as long as pom.xml does not change, so editing code does not re-download everything.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
# Tests are skipped here: they need Docker for Testcontainers, which is not available inside an image build.
# They run in CI and locally with "mvnw verify".
RUN ./mvnw -B -q package -DskipTests

# ---- Stage 2: run the jar ----
FROM eclipse-temurin:21-jre
WORKDIR /app

# Do not run as root: if the application were compromised, the attacker would not get root in the container.
RUN groupadd --system app && useradd --system --gid app --no-create-home app

COPY --from=build /workspace/target/*.jar app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
