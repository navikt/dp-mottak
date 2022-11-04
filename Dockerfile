FROM ghcr.io/navikt/baseimages/temurin:17

COPY mediator/build/libs/*.jar app.jar
