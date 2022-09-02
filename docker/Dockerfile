# Build the CLI for the given java version
ARG JAVA_VERSION=17

# OpenJDK image is deprecated, see https://hub.docker.com/_/openjdk
# So use OpenJDK image from AWS
FROM maven:3.8.6-amazoncorretto-${JAVA_VERSION} as builder
COPY . /src
WORKDIR /src
RUN mvn clean install

ARG JAVA_VERSION
FROM amazoncorretto:${JAVA_VERSION}-alpine
COPY --from=builder /src/cli/target/cli-1.0-jar-with-dependencies.jar /sokrates.jar
WORKDIR /repo
ENTRYPOINT ["java", "-jar", "/sokrates.jar"]
