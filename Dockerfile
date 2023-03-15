FROM openjdk:17-alpine
WORKDIR /usr/src/main
ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} callisto-balance-calculator.jar
ENTRYPOINT ["java","-jar","callisto-balance-calculator.jar"]
EXPOSE 9090