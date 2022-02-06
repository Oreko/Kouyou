FROM openjdk:8-alpine

COPY target/uberjar/kouyou.jar /kouyou/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/kouyou/app.jar"]
