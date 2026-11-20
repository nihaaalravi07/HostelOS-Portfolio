# HostelOS production image: React build + Spring Boot API in ONE container, ONE port.
# Build:  docker build -t hostelos .
# Run:    docker run -p 8080:8080 -e DATABASE_URL=postgresql://user:pass@host/db -e SITE_PASSCODE=... hostelos

# ---- 1. Build the React app --------------------------------------------------
FROM node:22-bookworm-slim AS web
WORKDIR /web
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# ---- 2. Build the Spring Boot jar, with the React build inside it ------------
FROM maven:3.9-eclipse-temurin-21 AS api
WORKDIR /app
COPY backend/pom.xml ./
COPY backend/src ./src
COPY --from=web /web/dist ./src/main/resources/static
RUN mvn -B -q -DskipTests package

# ---- 3. Small runtime image --------------------------------------------------
FROM eclipse-temurin:21-jre
RUN useradd --system --create-home hostelos
WORKDIR /app
COPY --from=api /app/target/hostelos-backend-1.0.0.jar app.jar
USER hostelos

# Free hosting gives ~512 MB RAM: cap the heap, use the cheap GC, start fast, and use Indian time so
# "today's meals", cutoffs and the midnight rollover match the people using the demo.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=65 -XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k -XX:+ExitOnOutOfMemoryError -Duser.timezone=Asia/Kolkata"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
