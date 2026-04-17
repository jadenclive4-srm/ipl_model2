FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# copy everything
COPY . .

# go into backend folder
WORKDIR /app/backend

# build jar
RUN mvn clean package -DskipTests

# -----------------------------

FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# copy built jar
COPY --from=build /app/backend/target/*.jar app.jar

EXPOSE 8080

# DEBUG + START
CMD ["sh", "-c", "echo '=== FILES ===' && ls -l && echo '=== STARTING APP ===' && java -jar app.jar --server.port=8080"]# ---------------- BUILD STAGE ----------------
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

# copy everything first
COPY . .

# build inside correct folder (ONLY if backend exists)
WORKDIR /app/backend

RUN mvn clean package -DskipTests


# ---------------- RUN STAGE ----------------
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# copy jar
COPY --from=build /app/backend/target/*.jar app.jar

EXPOSE 8080

# Render needs PORT env variable support
CMD ["sh", "-c", "java -jar app.jar --server.port=$PORT"]