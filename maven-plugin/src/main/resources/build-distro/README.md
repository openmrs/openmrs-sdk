# Description
Run openmrs distribution <distro> and mysql as docker containers.

## Requirements
  - Docker engine
  - Docker compose

## Development

To start both containers:
```
$ docker-compose up
```

Application will be accessible on http://localhost:8080/openmrs.

Note: if you are using Docker Toolbox you need to replace `localhost` with the IP address of your docker machine,
which you can get by running:
```
$ docker-machine url
```

Use _CTRL + C_ to stop all containers.

If you made any changes (modified modules/owas/war) to the distro run:
```
$ docker-compose up --build
```

If you want to destroy containers and delete any left over volumes and data when doing changes to the docker
configuration and images run:
```
$ docker-compose down -v
```

In the development mode the OpenMRS server is run in a debug mode and exposed at port 1044. You can change the port by
setting the DEBUG_PORT environment property or by editing the `.evn` file before starting up containers.

Similarly MySQL is exposed at port 3306 and can be customized by setting the MYSQL_PORT property.

## Production

To start containers in production:
```
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up
```

Application will be accessible on http://localhost/openmrs.

Note that in contrary to the development mode the OpenMRS server is exposed on port 80 instead of 8080.
No other ports are exposed in the production mode.

## Customisations

The `docker-compose.yml` is an example and can be customised. The next time you run openmrs-sdk:build-distro, it will
not modify your docker files, but update war and modules if needed. If you want SDK to recreate your docker files,
run:
```
$ mvn openmrs-sdk:build-distro -Dreset
```

### Customizing initial database

If you want to build a distribution with a database in a certain state you can pass a db dump to the build-distro goal:
```
$ mvn openmrs-sdk:build-distro -DdbSql=initial_db.sql
```

## Deploying <distro> to dockerhub

The image in '<distro>' can be built and pushed to dockerhub, to be used in test environments or production:

```
$ cd <distro>
$ docker build -t <username>/openmrs-<distro>:latest .
$ docker push <username>/openmrs-<distro>:latest
```

If the image is pushed to dockerhub, `docker-compose.yml` can be modified to use that image
instead of building the new image.

## Other similar docker images and relevant links
- <https://wiki.openmrs.org/display/RES/Demo+Data>
- <https://wiki.openmrs.org/display/docs/Installing+OpenMRS+on+Docker>
- <https://github.com/tusharsoni/OpenMRS-Docker/blob/master/Dockerfile>
- <https://github.com/chaityabshah/openmrs-core-docker/blob/master/Dockerfile>
- <https://github.com/bmamlin/openmrs-core-docker/blob/master/Dockerfile>
