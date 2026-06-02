[![Build Status](https://github.com/openmrs/openmrs-sdk/actions/workflows/maven.yml/badge.svg)](https://github.com/openmrs/openmrs-sdk/actions/workflows/maven.yml)

It is a repository of OpenMRS Software Development Kit (SDK).

For more details visit [the project page](https://wiki.openmrs.org/display/docs/OpenMRS+SDK).

## Requirements
 * Maven 3.x
 
## Installation
In order to install the latest version of the sdk run: <br/>

`mvn org.openmrs.maven.plugins:openmrs-sdk-maven-plugin:setup-sdk`

## Usage
All commands can be run with `mvn openmrs-sdk:<command>`.

Run `mvn openmrs-sdk:help` for the list of supported commands.

## Layout

Please note that the SDK consists of numerous sub-modules that implement specific
parts of the SDK. The intention is that the SDK itself is relatively modular.

The most important sub-module is found in the `maven-plugin` directory. This is
the main SDK plugin and contains the code that is actually executed when running
SDK commands.

`docker-maven-plugin` contains the code that interacts with Docker directly. Primarily,
it is able to create and run Docker containers for OpenMRS databases.

`tomcat7-maven-plugin` contains the code to run OpenMRS in an embedded Tomcat 7 instance.
This is used for serving SDK versions running parts of the platform prior to 2.5.0.

`tomcat9-maven-plugin` contains the code to run OpenMRS in an embedded Tomcat 9 instance.
This is used for serving SDK versions running platform 2.5.0 or newer.

`integration-tests` contains the integration tests that are useful for fully testing SDK
features and ensuring they work.

The various `archetype-*` directories contains the actual archetypes used by the `create-project`
command.

## Development

To build the plugin run:

`mvn clean install`

To run a SNAPSHOT version of SDK, you need to specify groupId:artifactId:version for all SDK commands, e.g.

`mvn org.openmrs.maven.plugins:openmrs-sdk-maven-plugin:5.14.0-SNAPSHOT:setup`

You can also debug commands by creating maven run configurations for the SNAPSHOT version in your IDE 
and starting them in the debug mode.

## Testing

### Integration tests

Integration tests cover the SDK goals (e.g. `build-distro`, `setup`, `deploy`) without requiring Docker.
They run as part of the standard build:

```bash
mvn verify -Pintegration-tests
```

### Docker E2E tests

The `integration-tests` module contains a suite of end-to-end tests (`BuildDistroE2EIT`) that exercise
every supported OpenMRS platform version line (1.9.x through 2.8.x) by:

1. Running `build-distro` to generate a `Dockerfile` and `docker-compose.yml` for that version
2. Starting the generated docker compose stack
3. Polling `GET /openmrs/ws/rest/v1/systeminformation` (authenticated) until it returns JSON,
   confirming OpenMRS is fully initialised and webservices.rest has started
4. Asserting that the `addresshierarchy` module reports `started:true` via
   `GET /openmrs/ws/rest/v1/module/addresshierarchy`

These tests require a running Docker daemon and are intentionally excluded from the standard
`integration-tests` profile because each test can take several minutes (artifact download +
Docker image build + OpenMRS database initialisation).

**Run the full suite:**

```bash
mvn install -DskipTests                                    # install the SDK to the local Maven repo first
mvn verify -Pdocker-e2e-tests -pl integration-tests        # run all 13 version tests
```

**Run a single version:**

```bash
mvn verify -Pdocker-e2e-tests -pl integration-tests -Dit.test="BuildDistroE2EIT#platform_2_6_x"
```

**Browse the running instance while the test is paused:**

Adding `-Dbrowser` opens the running container's URL in your default browser and pauses the
test until you press Enter, so you can navigate around before the container is torn down:

```bash
mvn verify -Pdocker-e2e-tests -pl integration-tests -Dit.test="BuildDistroE2EIT#platform_2_6_x" -Dbrowser
```

**Enable debug logging:**

Adding `-DdockerDebug` loads `docker-compose.override.yml` alongside the generated
`docker-compose.yml`.  The override mounts `web/log4j.properties` (pre-2.5, log4j 1.x) and
`web/log4j2.xml` (2.5+, log4j 2.x) into the container via `JAVA_TOOL_OPTIONS`, overriding the
logging configuration bundled inside the WAR.  Edit the relevant file and restart to change
log levels without rebuilding the image:

```bash
mvn verify -Pdocker-e2e-tests -pl integration-tests -Dit.test="BuildDistroE2EIT#platform_2_6_x" -DdockerDebug
```

The available test method names map directly to version lines:

| Method | Platform version |
|---|---|
| `platform_1_09_x` | 1.9.12 |
| `platform_1_10_x` | 1.10.6 |
| `platform_1_11_x` | 1.11.9 |
| `platform_1_12_x` | 1.12.1 |
| `platform_2_0_x` | 2.0.8 |
| `platform_2_1_x` | 2.1.7 |
| `platform_2_2_x` | 2.2.1 |
| `platform_2_3_x` | 2.3.6 |
| `platform_2_4_x` | 2.4.7 |
| `platform_2_5_x` | 2.5.15 |
| `platform_2_6_x` | 2.6.16 |
| `platform_2_7_x` | 2.7.9 |
| `platform_2_8_x` | 2.8.6 |

Each test's distro properties file is at
`integration-tests/src/test/resources/integration-test/openmrs-distro-e2e-{version}.properties`
and can be used to reproduce the scenario manually — see the comments inside each file.

## Releasing

Before publishing a new release, go to JIRA at https://issues.openmrs.org/plugins/servlet/project-config/SDK/versions 
and add a next development version. Next move the version you have just added up or down to preserve the ordering. 
Finally, click the gear icon next to the version you want to release and select the `Release` link. 
If there are any issues, which are not yet closed for the release, you should see a prompt asking you what to do with 
them. If the issues have already been committed, close them. Otherwise, move the issues to the next development version.  

Releasing follows the standard release process using Bamboo.  In OpenMRS Bamboo, you should find the latest
green build that has completed that matches what you wish to release.
https://ci.openmrs.org/browse/SDK-SDK

On the left, you will find a "Release to Maven" step.  For information on this process, see the wiki:
https://wiki.openmrs.org/display/docs/Releasing+a+Module+from+Bamboo

## Usage statistics

OpenMRS SDK gathers anonymous [usage statistics](https://docs.google.com/spreadsheets/d/1yMcfBl10l32YxWtXneD0wJZh11-qaLBMAwQFC9JUogA/edit#gid=42570905), which are shared on request. Please contact the project lead to request read-only access.
