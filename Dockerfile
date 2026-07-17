FROM gradle:9.6.1-jdk21 AS build
WORKDIR /home/gradle/src

COPY --chown=gradle:gradle gradlew settings.gradle build.gradle ./
COPY --chown=gradle:gradle gradle/ gradle/
RUN ./gradlew --no-daemon dependencies

COPY --chown=gradle:gradle src src
RUN gradle bootJar --no-daemon -x test

FROM bellsoft/liberica-openjre-alpine:21
EXPOSE 8080

COPY --from=build /home/gradle/src/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]