# ---------------- BUILD STAGE ----------------
FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /app

# copy everything first
COPY . .

# build backend
WORKDIR /app/backend
RUN mvn clean package -DskipTests


# ---------------- RUN STAGE ----------------
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

# copy backend jar
COPY --from=build /app/backend/target/*.jar app.jar

# copy frontend build files
COPY --from=build /app/frontend22/build ./frontend22/build

EXPOSE 8080

# Render needs PORT env variable support
CMD ["sh", "-c", "java -jar app.jar --server.port=$PORT"]