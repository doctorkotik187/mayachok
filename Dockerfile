FROM clojure:temurin-21 AS build

WORKDIR /app
COPY deps.edn build.clj ./
COPY src/ src/
COPY resources/ resources/
COPY env/prod/resources/ env/prod/resources/

RUN clj -Sforce -T:build uber

FROM eclipse-temurin:21-jre-jammy

RUN useradd --system --create-home appuser
WORKDIR /app

COPY --from=build /app/target/mayachok-standalone.jar /app/mayachok.jar

RUN mkdir -p /app/data && chown appuser:appuser /app/data

USER appuser

EXPOSE 3000

HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost:3000/api/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/mayachok.jar"]
