FROM code.ornl.gov:4567/rse/datastreams/ssm/backend/ssm-bats-rest-api/maven:3.6-openjdk-11-slim AS build
ARG APP_DIR="/home/app"
WORKDIR ${APP_DIR}
COPY pom.xml checkstyle.xml checkstyle-suppressions.xml ${APP_DIR}/
RUN mvn -f ${APP_DIR}/pom.xml verify clean --fail-never
COPY src ${APP_DIR}/src
RUN mvn -f ${APP_DIR}/pom.xml package -Dmaven.test.skip=true

FROM code.ornl.gov:4567/rse/datastreams/ssm/backend/ssm-bats-rest-api/openjdk:11-jre-slim
VOLUME /tmp
COPY --from=build /home/app/target/*.jar app.jar
EXPOSE 8080
CMD ["java","-Dspring.profiles.active=docker","-jar","/app.jar"]
