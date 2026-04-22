# ---------------- BUILD STAGE ----------------
FROM node:18 AS frontend-build

WORKDIR /frontend
COPY frontend22/package*.json ./
RUN npm install
COPY frontend22/ .
RUN npm run build


FROM maven:3.9.9-eclipse-temurin-17 AS backend-build

WORKDIR /app

COPY backend ./backend

# copy built frontend into Spring Boot static folder
COPY --from=frontend-build /frontend/build ./backend/src/main/resources/static/

WORKDIR /app/backend
RUN mvn clean package -DskipTests


# ---------------- RUN STAGE ----------------
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app
COPY --from=backend-build /app/backend/target/*.jar app.jar

EXPOSE 8080
CMD ["sh", "-c", "java -jar app.jar --server.port=$PORT"]