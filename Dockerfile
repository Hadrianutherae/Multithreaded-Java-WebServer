# syntax=docker/dockerfile:1

FROM maven:3-openjdk-8-slim

RUN mkdir /usr/src/main
COPY . /usr/src/main
WORKDIR /usr/src/main
RUN mvn clean package -DskipTests
FROM openjdk:8-jre-alpine
RUN mkdir /project
COPY --from=0 /usr/src/main/target/uber-JavaWebServer-1.0-SNAPSHOT.jar /project/
WORKDIR /project
CMD java -jar uber-JavaWebServer-1.0-SNAPSHOT.jar "/" "65535"