FROM openjdk:12-alpine
ENV export $(cat .env | xargs)
ARG JAR_FILE=target/*.jar
RUN echo $JAR_FILE
COPY ${JAR_FILE} app.jar

ENTRYPOINT ["java","-jar","/app.jar"]