FROM code.ornl.gov:4567/rse/datastreams/ssm/backend/ssm-bats-rest-api/maven:3.6-openjdk-11-slim AS build
ARG APP_DIR="/home/app"
# provide comma-separated values of profile names. Should include and start with "docker" at minimum (i.e. "docker,dev", but not "dev,docker")
ARG PROFILE="docker"
WORKDIR ${APP_DIR}
COPY *.xml ${APP_DIR}/
RUN mvn -f ${APP_DIR}/pom.xml verify clean --fail-never
COPY src ${APP_DIR}/src
RUN mvn -f ${APP_DIR}/pom.xml -P ${PROFILE} package -Dmaven.test.skip=true

FROM code.ornl.gov:4567/rse/datastreams/ssm/backend/ssm-bats-rest-api/openjdk:11-jre-slim
VOLUME /tmp
COPY --from=build /home/app/target/*.jar app.jar
EXPOSE 8080
CMD ["java","-jar","/app.jar"]
