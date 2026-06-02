
## Docker Compose

### Development

Start all containers (builds the image on first run):
```
docker compose up
```
Application is accessible at http://localhost:8080/openmrs.

Rebuild after changing modules, OWAs, or WAR:
```
docker compose up --build
```
Stop and remove containers and volumes:
```
docker compose down -v
```
The debug port 1044 and MySQL port 3306 are exposed in development mode.
Customise them in the `.env` file.

### Production

```
docker compose -f docker-compose.yml -f docker-compose.prod.yml up
```
Application is accessible at http://localhost/openmrs (port 80).
No debug or database ports are exposed in production mode.

### Debug logging

Load the override file to mount a custom log4j configuration at runtime:
```
docker compose -f docker-compose.yml -f docker-compose.override.yml up --build
```
Edit `web/log4j.properties` (pre-2.5 platforms) or `web/log4j2.xml` (2.5+)
and restart to change log levels without rebuilding the image.

### Customising the initial database

Pass a SQL dump to seed the database on first run:
```
mvn openmrs-sdk:build-distro -DdbSql=initial_db.sql
```
