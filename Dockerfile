FROM maven:3.6-jdk-11 AS build
ARG APP_DIR="/home/app"
WORKDIR ${APP_DIR}
COPY pom.xml checkstyle.xml checkstyle-suppressions.xml ${APP_DIR}/
RUN mvn -f ${APP_DIR}/pom.xml verify clean --fail-never
COPY src ${APP_DIR}/src
RUN mvn -f ${APP_DIR}/pom.xml package -Dmaven.test.skip=true

FROM openjdk:11-jre-slim
VOLUME /tmp 
COPY --from=build /home/app/target/*.jar app.jar
CMD ["java","-Dspring.profiles.active=docker","-jar","/app.jar"]
