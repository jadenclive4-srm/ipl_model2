# ---------------- BUILD STAGE ----------------
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