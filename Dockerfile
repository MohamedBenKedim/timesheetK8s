FROM openjdk:8-jdk-alpine
EXPOSE 8083
COPY target/timesheet2021-3.0.jar /app/timesheet.jar
ENTRYPOINT ["java","-jar","timesheet.jar"]
