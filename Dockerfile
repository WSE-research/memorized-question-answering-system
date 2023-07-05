#
# Build stage
#
FROM maven:3.8.6-eclipse-temurin-11-alpine AS build

WORKDIR /home/app

COPY src ./src
COPY Dockerfile .
COPY pom.xml .

RUN mvn -f ./pom.xml clean package -X

#
# Package stage
#
FROM openjdk:11 AS package

ARG JAR_FILE

# Add the service itself
COPY --from=build /home/app/target/${JAR_FILE} /fake-question-answering-system.jar

ENTRYPOINT ["java", "-jar", "/fake-question-answering-system.jar"]
