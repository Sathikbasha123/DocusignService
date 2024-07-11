# First stage - Building the application
FROM openjdk:19-jdk-alpine3.16 AS builder
COPY /target/classes/* /
COPY /target/*.jar  app.jar

# Second stage - Running the application
FROM openjdk:11-jre-stretch
COPY --from=builder app.jar app.jar
EXPOSE 8085 9010

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]