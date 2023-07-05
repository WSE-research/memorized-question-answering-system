FROM openjdk:11

ARG JAR_FILE

# Add the service itself
COPY target/${JAR_FILE} /fake-question-answering-system.jar


ENTRYPOINT ["java", "-jar", "/fake-question-answering-system.jar"]
