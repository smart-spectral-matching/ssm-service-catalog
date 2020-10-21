# BATS Microservice

This is a Spring Boot REST Web Service for [BATS](https://github.com/jayjaybillings/bats)(Basic Artifact Tracking System).

## Getting Started (with Docker)

### Just the BATS REST API
To build the image with `<image_name>` set to `nds/bats` for example below, use:

```
docker build -t ssm-bats-rest-api -f dockerfiles/Dockerfile.ssm_bats_rest_api .
```

To startup a container instance of this image, use:

```
docker run -p 8080:8080 ssm-bats-rest-api
```

Then, the web service is up and running at `localhost:8080`

### BATS REST API + Fuseki server

You can use the `local` development docker compose file to spin up container for both services:

```
docker-compose -f docker-compose.local.yml up
```

### Testing

To run tests, use:

```
docker-compose -f docker-compose.test.yml run ssm-bats-rest-api up
```

or

```
docker-compose -f docker-compose.test.yml run ssm-bats-rest-api mvn test
```

### Development Setup 

"Skeleton" requirements:
1) JDK 1.8 or later
2) Maven 3.2+
3) Installation of [Docker](https://docs.docker.com/install/)

#### Theia IDE

If running remotely, you can use [Theia IDE](https://theia-ide.org/) via a Docker container by running the following
in this project directly on the remote machine:

```
docker run -uroot -it --init -p 3000:3000 -v "$(pwd):/home/project:cached" theiaide/theia:next
```

The IDE will be accessible via a browser at `http://<remote machine>:3000`.
You can change the port by modifying the `-p` option in the docker command (i.e. `-p 3030:3000` for port 3030).

Links:
 - [Theia Full Docker Image DockerHub repo](https://hub.docker.com/r/theiaide/theia-full)
 - [Theia Full Docker Image GitHub repo](https://github.com/theia-ide/theia-apps/tree/master/theia-full-docker)

NOTE: We run the 'full' image since it has Java support.
