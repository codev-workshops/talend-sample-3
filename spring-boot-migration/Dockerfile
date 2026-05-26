FROM eclipse-temurin:17-jre

WORKDIR /app

COPY target/*.jar app.jar

ENV JAVA_OPTS=""

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
