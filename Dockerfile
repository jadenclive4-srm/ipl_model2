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
COPY --from=frontend-build /frontend/build/index.html ./backend/src/main/resources/static/
COPY --from=frontend-build /frontend/build/asset-manifest.json ./backend/src/main/resources/static/
COPY --from=frontend-build /frontend/build/static/ ./backend/src/main/resources/static/
COPY --from=frontend-build /frontend/build/logos/ ./backend/src/main/resources/static/logos/
COPY --from=frontend-build /frontend/build/backgrounds/ ./backend/src/main/resources/static/backgrounds/

WORKDIR /app/backend
RUN mvn clean package -DskipTests


# ---------------- RUN STAGE ----------------
FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app
COPY --from=backend-build /app/backend/target/*.jar app.jar

EXPOSE 8080
CMD ["sh", "-c", "java -jar app.jar --server.port=$PORT"]