FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /workspace
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q -B dependency:go-offline
COPY src ./src
RUN ./mvnw -q -B -DskipTests package && \
    mv target/pastebinclone-*.jar /workspace/app.jar

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /workspace/app.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
