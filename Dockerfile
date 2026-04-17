FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# copy everything
COPY . .

# go into backend folder (IMPORTANT)
WORKDIR /app/backend

RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app

# copy built jar
COPY --from=build /app/backend/target/*.jar app.jar

EXPOSE 8081
ENTRYPOINT ["sh","-c","java -jar app.jar --server.port=$PORT"]