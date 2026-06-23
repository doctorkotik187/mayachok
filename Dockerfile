# syntax=docker/dockerfile:1.2
FROM clojure:temurin-21-tools-deps AS build
WORKDIR /app

# Copy deps first for better layer caching
COPY deps.edn build.clj ./

# Pre-download deps with cache mount — rebuilds skip this if deps unchanged
RUN --mount=type=cache,target=/root/.m2 \
    clj -P

COPY src/ src/
COPY resources/ resources/
COPY env/prod/resources/ env/prod/resources/

RUN --mount=type=cache,target=/root/.m2 \
    clj -Sforce -T:build uber

FROM eclipse-temurin:21-jre-jammy
RUN useradd --system --create-home appuser
WORKDIR /app
COPY --from=build /app/target/mayachok-standalone.jar /app/mayachok.jar
RUN mkdir -p /app/data && chown appuser:appuser /app/data
USER appuser
EXPOSE ${PORT:-3000}
HEALTHCHECK --interval=30s --timeout=5s --retries=3 \
  CMD wget -qO- http://localhost:3000/api/health || exit 1
ENTRYPOINT exec java $JAVA_OPTS -jar /app/mayachok.jar
