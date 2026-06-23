FROM clojure:temurin-21-tools-deps AS build
COPY . /app
WORKDIR /app
RUN make uberjar

FROM eclipse-temurin:21-jre-jammy
COPY --from=build /app/target/mayachok-standalone.jar /app/mayachok.jar
EXPOSE 3000
ENTRYPOINT ["java", "-jar", "/app/mayachok.jar"]
