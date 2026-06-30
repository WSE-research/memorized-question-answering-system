FROM eclipse-temurin:25-jre

ARG JAR_FILE

# Add the service itself
COPY target/${JAR_FILE} /memorized-question-answering-system.jar


ENTRYPOINT ["java", "-jar", "/memorized-question-answering-system.jar"]
