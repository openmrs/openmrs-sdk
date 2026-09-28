
## Docker Image

The `web/` directory is the Docker build context.  Build the image locally:
```
docker build -t <username>/openmrs-<distro>:latest web/
```
The base image tag is set by the `BASE_IMAGE_TAG` build arg, which defaults to the tag chosen when the
distro was built.  Override it to build from another variant of the same base image, e.g. a different Java version:
```
docker build --build-arg BASE_IMAGE_TAG=2.8.9-amazoncorretto-8 -t <username>/openmrs-<distro>:latest-java8 web/
```
Push to Docker Hub for use in test environments or production:
```
docker push <username>/openmrs-<distro>:latest
```
