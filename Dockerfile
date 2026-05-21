FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY build/libs/*.jar app.jar

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]