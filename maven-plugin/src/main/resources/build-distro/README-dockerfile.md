
## Docker Image

The `web/` directory is the Docker build context.  Build the image locally:
```
docker build -t <username>/openmrs-<distro>:latest web/
```
The base image tag is built from two build args, which default to the values chosen when the distro was built:
`BASE_IMAGE_VERSION` (e.g. `2.8.9`) and `BASE_IMAGE_VARIANT` (e.g. `amazoncorretto-8`, appended with a dash
when not empty).  Override `BASE_IMAGE_VARIANT` to build from another variant of the same base image, e.g. a
different Java version, or set it to empty to use the default variant:
```
docker build --build-arg BASE_IMAGE_VARIANT=amazoncorretto-8 -t <username>/openmrs-<distro>:latest-java8 web/
```
Push to Docker Hub for use in test environments or production:
```
docker push <username>/openmrs-<distro>:latest
```
