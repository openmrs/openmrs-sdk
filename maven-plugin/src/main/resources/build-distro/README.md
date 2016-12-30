# Description
Run openmrs distribution <distro> and mysql as disposable docker containers.
This is not intended to be used in production, but it can be used in test environments
or locally.

## Requirements
  - Docker engine
  - Docker compose

## Running it

To start both containers:
```
$ docker-compose up
```

Application will be accessible on http://localhost:8080/openmrs.


Use _CTRL + C_ to stop it all containers. But make sure to destroy containers to delete any
left overs volumes and data when doing changes to the docker configuration and images:
```
$ docker-compose down
```

## Customisations

The `docker-compose.yml` is an example and should be customised.


### Generate demo data / database from scratch
If you remove the _dbdump_ volume from _mysql_, the database will be empty.
Changing variables _DB_CREATE_TABLES_, _DB_ADD_DEMO_DATA_ and _DB_AUTO_UPDATE_
on openmrs installation will create tables and demo data.

A new dbdump can be taken after that, if desired:
  - `docker exec -it <container_db_id> bash`
  - `mysqldump --user=openmrs --password=openmrs openmrs > /tmp/dump.sql`
  - `docker cp <container_db_id>:/tmp/dump.sql .` to copy it to your machine

### Keep database from previous runs

By default, mysql data will not be persisted between docker runs.
If desired, uncomment the volume _openmrs-referenceapplication-mysql-data_ in mysql
container.


## Deploying <distro>-docker-image to dockerhub

The image in '<distro>-docker-image' can be built and push to dockerhub, to be used in test environments:

```
$ cd <distro>-docker-image
$ docker build -t <username>/<distro>-docker-image:latest .
$ docker push <username>/<distro>-docker-image:latest
```

If the iamge is pushed to dockerhub, `docker-compose.yml` can be modified to use that image
instead of building the new image always. 

## Other similar docker images and relevant links
- <https://wiki.openmrs.org/display/RES/Demo+Data>
- <https://wiki.openmrs.org/display/docs/Installing+OpenMRS+on+Docker>
- <https://github.com/tusharsoni/OpenMRS-Docker/blob/master/Dockerfile>
- <https://github.com/chaityabshah/openmrs-core-docker/blob/master/Dockerfile>
- <https://github.com/bmamlin/openmrs-core-docker/blob/master/Dockerfile>
