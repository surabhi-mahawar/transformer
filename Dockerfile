FROM maven:3.6.0-jdk-11-slim AS build
ENV HOME=/home/app
RUN mkdir -p $HOME
WORKDIR $HOME
ADD pom.xml $HOME

ADD /src $HOME/src
RUN mvn package -DskipTests=true

# Package stage
FROM openjdk:11-jre-slim
RUN apt update && apt upgrade
RUN apt install curl -y
ENV HOME=/home/app
WORKDIR $HOME
COPY --from=build $HOME/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
