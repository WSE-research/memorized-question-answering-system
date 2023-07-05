#
# Build stage
#
FROM openjdk:11 AS build

WORKDIR /home/app

COPY src ./src
COPY pom.xml .


#
# Package stage
#

FROM openjdk:11

# Add the service itself
COPY --from=build /home/app/target/fake-question-answering-system.jar /fake-question-answering-system.jar

ENTRYPOINT ["java", "-jar", "/fake-question-answering-system.jar"]
