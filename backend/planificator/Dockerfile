# PRODUCCIÓN — API multi-arch (linux/amd64 + linux/arm64)
# El JAR se compila en BUILDPLATFORM; la JRE final sigue TARGETPLATFORM.
FROM --platform=$BUILDPLATFORM eclipse-temurin:17-jdk AS build
WORKDIR /app
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src ./src
RUN ./mvnw -B -q -DskipTests package && mv target/*.jar /app/app.jar

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN groupadd --system app && useradd --system --gid app --no-create-home app
COPY --from=build /app/app.jar ./app.jar
USER app
EXPOSE 8080
ENV JAVA_OPTS="-Xms256m -Xmx512m -Duser.timezone=UTC"
ENV PLANIFICATOR_ENV=production
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
