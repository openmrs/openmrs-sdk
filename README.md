[![Build Status](https://travis-ci.org/openmrs/openmrs-sdk.svg?branch=master)](https://travis-ci.org/openmrs/openmrs-sdk)

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

## Development

To build the plugin run:

`mvn clean install`

To run a SNAPSHOT version of SDK, you need to specify groupId:artifactId:version for all SDK commands, e.g.

`mvn org.openmrs.maven.plugins:openmrs-sdk-maven-plugin:2.1.3-SNAPSHOT:setup-platform`

You can also debug commands by creating maven run configurations for the SNAPSHOT version in your IDE and starting them in the debug mode.

## Releasing

Create [a new tag](https://github.com/openmrs/openmrs-sdk/releases) named in the following format: `release.version_new.dev.version-SNAPSHOT` e.g. `3.1.3_3.1.4-SNAPSHOT` (the underscore separates release and development versions).

A new Travis-CI build will be triggered, which will create a new tag named after the release version and set a new development version in the master branch. 

The tag will be then built and deployed to [Bintray](https://bintray.com/openmrs/maven/openmrs-sdk). You will have to sign-in and [sync with Maven Central](https://bintray.com/openmrs/maven/openmrs-sdk#central). 

