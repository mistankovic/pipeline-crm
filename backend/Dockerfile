# Built in one image, run in another: the runtime carries a JRE and a jar, not a JDK, Maven and
# a source tree.

FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src

# Dependencies first, so a source change does not re-download the internet.
COPY pom.xml .
COPY domain/pom.xml domain/
COPY application/pom.xml application/
COPY adapter-persistence/pom.xml adapter-persistence/
COPY adapter-web/pom.xml adapter-web/
COPY bootstrap/pom.xml bootstrap/
COPY .mvn .mvn
RUN mvn -B -q dependency:go-offline -DskipTests || true

COPY . .
# The image is built from a source tree that has already passed `mvn verify` in CI. Running the
# gates again here would need Docker-in-Docker for the Testcontainers tests, which is a worse
# trade than building the artefact the pipeline already vouched for.
RUN mvn -B -q package -DskipTests -Dpit.skip=true -Dcrap.skip=true -Dcoverage.skip=true \
    -Dcheckstyle.skip=true -Dcpd.skip=true -Denforcer.skip=true

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN adduser -D -H pipelinecrm
USER pipelinecrm
WORKDIR /app
COPY --from=build /src/bootstrap/target/pipelinecrm.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
