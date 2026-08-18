FROM eclipse-temurin:25-jdk AS build

WORKDIR /workspace

COPY gradlew gradlew
COPY gradle gradle
COPY settings.gradle build.gradle ./
COPY src src

RUN chmod +x gradlew \
    && ./gradlew bootJar --no-daemon -x test \
    && cp "$(find build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -print -quit)" /workspace/app.jar

FROM eclipse-temurin:25-jre

WORKDIR /app

RUN groupadd --system --gid 10001 jexon \
    && useradd --system --uid 10001 --gid jexon --home-dir /app --shell /usr/sbin/nologin jexon \
    && mkdir -p /app/storage \
    && chown -R jexon:jexon /app

COPY --from=build --chown=jexon:jexon /workspace/app.jar /app/app.jar

USER jexon
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
