FROM maven:3.6-openjdk-11-slim AS build
ARG APP_DIR="/home/app"
# To see which profiles are activated, look at the profile groups in 'src/main/resources/application.properties'
# You should only provide one profile to this argument
ARG PROFILE="localdocker"
WORKDIR ${APP_DIR}
COPY *.xml ${APP_DIR}/
RUN mvn -f ${APP_DIR}/pom.xml verify clean --fail-never
COPY src ${APP_DIR}/src
RUN mvn -f ${APP_DIR}/pom.xml -P ${PROFILE} package -Dmaven.test.skip=false

FROM openjdk:11-jre-slim
COPY --from=build /home/app/target/*.jar app.jar
EXPOSE 8080
CMD ["java","-jar","/app.jar"]
