# SSM Catalog Service

This service is the data catalog via a REST API.

## Getting Started

First, you need to [login to the GitHub Container Registry](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry)

The fastest way to spin up a working setup is to use `docker-compose.yml` top-level file:

```
docker compose up
```

You can use both "bare metal" and docker during testing

### Seed data

After the REST API and Fuseki server are running,
you can seed collections from the test resources directory to upload via POST by running:

```
bash bin/post-seed-data.sh
```

This will upload the files to `localhost:8080`.
If you would like to change the server IP:port,
you can also pass in a different one via the command line:
```
bash bin/post-seed-data.sh <server:port>
```

If Keycloak security is turned on, navigate to localhost:8080/token in a web browser to get a token.
```
bash bin/post-seed-data.sh <server:port> <token>
```

The files uploaded are hard-coded into the script.

### Linting

To lint the code via [checkstyle](https://checkstyle.sourceforge.io/), run:
```
mvn validate
```

### Testing

You can run the full test suite via:

```
mvn clean docker:build verify
```

The `docker:build` is necessary to build the Fuseki docker container for integration tests

#### Testing with VSCode

You can run individual tests with VSCode IDE.

One thing is you need to spin up a Fuseki server for the database.

You can do so via:
```
docker pull fuseki
docker run -p 3030:3030 fuseki
```

### Coverage Report

To generate a coverage report using [jacoco](https://www.jacoco.org/jacoco/) run:

```
mvn site
```

Then, the report will be available via the HTML file:
```
target/site/jacoco/index.html
```

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

#### BATS REST API + Fuseki server + Keycloak

You can use docker compose to spin up a container for both services (REST API + Fuseki server + Keycloak):

```
docker compose up
```

Sample Docker images for Keycloak and Keto can be found in the Deployments repo and must be built seperately. To activate the OIDC authentication, set the values in the appropriate application-foo.properties file appropriately use the sample values in the base application.properties file.

If you want to run the stack locally, instead run

```
docker-compose up -f docker-compose-local.yml
```

This will launch with a configuration suited to all services being launched and accessed from localhost.

#### Testing

To run tests, use:

```
docker build -f src/main/docker/Dockerfile.ssm_bats_rest_api -t ssm-bats-test .
docker run --net=host -v /var/run/docker.sock:/var/run/docker.sock ssm-bats-test
```

### Documentation

The REST API documentaion is below

### Quickstart

Below is a quick example of how to upload and retrieve data via the API.

#### Pre-requisite
For an example [JSON-LD](https://json-ld.org/) file for a model,
you can us a PH measurement from the [SciData](https://github.com/stuchalk/scidata) repository:
```
wget https://raw.githubusercontent.com/stuchalk/scidata/master/examples/ph.jsonld
```

#### API Example

Once running, you can create a collection:
```
curl -X POST --data '{"title":"test"}' -H "Content-Type: application/json" "http://localhost:8080/api/collections"
```

Given a JSON-LD file, you can upload this as a model to the collection.
With the returned collection title, you can then upload a model to this collection:
```
curl -X POST "http://localhost:8080/api/collections/<collection title>/models" -H "Content-Type: application/json" -d "@ph.jsonld"
```

With the returned model UUID, you can retrieve the model via:
```
curl -X GET "http://localhost:8080/api/collections/<collection title>/models/<model uuid>"
```

In order to get a full list of collection titles, run:
```
curl -X GET "http://localhost:8080/api/collections"
```

In order to get a full list of model UUIDs within a given collection, run:
```
curl -X GET "http://localhost:8080/api/collections/<collection title>/models/uuids"
```

### Swagger Docs of REST API

[Swagger](https://swagger.io/) is used to document the REST API

To access the documentation, after spinning up the application, navigate to:
```
<url>:<port>/api/swagger-ui/
```

If you would like to access the machine-readable API docs:
```
<url>:<port>/api/v3/api-docs
```
