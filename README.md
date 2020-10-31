# BATS Microservice

This is a Spring Boot REST Web Service for [BATS](https://github.com/jayjaybillings/bats)(Basic Artifact Tracking System).

## Getting Started

The fastest way to spin up a working setup is to use docker-compose top-level file:

```
docker-compose up
```

You can use both "bare metal" and docker during testing

### Testing

You can run the full test suite via:

```
mvn clean docker:build verify
```

The `docker:build` is necessary to build the Fuseki docker container for integration tests

### Using Docker

#### Just the BATS REST API
To build the image with `<image_name>` set to `ssm-bats-rest-api` for example below, use:

```
docker build -t ssm-bats-rest-api .
```

To startup a container instance of this image, use:

```
docker run -p 8080:8080 ssm-bats-rest-api
```

Then, the web service is up and running at `localhost:8080`

#### BATS REST API + Fuseki server

You can use docker compose to spin up a container for both services (REST API + Fuseki server):

```
docker-compose up
```

#### Testing

To run tests, use:

```
docker build -f src/main/docker/Dockerfile.ssm_bats_rest_api -t ssm-bats-test .
docker run --net=host -v /var/run/docker.sock:/var/run/docker.sock ssm-bats-test
```

### Documentation of REST API

[Swagger](https://swagger.io/) is used to document the REST API

To access the documentation, after spinning up the application, navigate to:
```
<url>:8080/swagger-ui.html
```
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
