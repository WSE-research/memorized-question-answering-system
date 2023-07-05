FROM openjdk:11

# Add the service itself
ADD target/fake-question-answering-system.jar /fake-question-answering-system.jar

ENTRYPOINT ["java", "-jar", "/fake-question-answering-system.jar"]
