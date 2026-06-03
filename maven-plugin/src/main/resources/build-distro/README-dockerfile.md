
## Docker Image

The `web/` directory is the Docker build context.  Build the image locally:
```
docker build -t <username>/openmrs-<distro>:latest web/
```
Push to Docker Hub for use in test environments or production:
```
docker push <username>/openmrs-<distro>:latest
```
