FROM openjdk:11.0-jre-slim

# Add tools which help debugging
RUN apt-get update \
    && apt-get install -y curl bash \
    && rm -rf /var/lib/apt/lists/*

# Unpack distribution tar
ADD build/distributions/testing-framework-1.0-SNAPSHOT.tar /

WORKDIR /testing-framework

ENTRYPOINT ["/testing-framework-1.0-SNAPSHOT/bin/testing-framework"]