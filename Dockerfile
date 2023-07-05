FROM openjdk:11

# Add the service itself
ARG JAR_FILE
ADD target/${JAR_FILE} /fake-question-answering-system.jar

ENTRYPOINT ["java", "-jar", "/fake-question-answering-system.jar"]
