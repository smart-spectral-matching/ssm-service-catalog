# BATS Microservice

This is a Spring Boot REST Web Service for [BATS](https://github.com/jayjaybillings/bats)(Basic Artifact Tracking System).

## Getting Started (with Docker)

To build the image with `<image_name>` set to `nds/bats` for example below, use:
```
docker build -t nds/bats -f dockerfiles/Dockerfile.bats_microservice .
```

To startup a container instance of this image, use:
```
docker run -p 8080:8080 nds/bats
```

Then, the web service is up and running at `localhost:8080`


#### Development Setup (in Eclipse)

"Skeleton" requirements:
1) JDK 1.8 or later
2) Maven 3.2+
3) Installation of [Docker](https://docs.docker.com/install/)

