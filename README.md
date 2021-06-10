[![Build Status](https://github.com/openmrs/openmrs-sdk/actions/workflows/maven/badge.svg)](https://github.com/openmrs/openmrs-sdk/actions/workflows/maven.yml)

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

`mvn org.openmrs.maven.plugins:openmrs-sdk-maven-plugin:4.0.1-SNAPSHOT:setup`

You can also debug commands by creating maven run configurations for the SNAPSHOT version in your IDE 
and starting them in the debug mode.

## Releasing

Before publishing a new release, go to JIRA at https://issues.openmrs.org/plugins/servlet/project-config/SDK/versions 
and add a next development version. Next move the version you have just added up or down to preseve the ordering. 
Finally click the gear icon next to the version you want to release and select the `Release` link. 
If there are any issues, which are not yet closed for the release, you should see a prompt asking you what to do with 
them. If the issues have already been committed, close them. Otherwise, move the issues to the next development version.  

Releasing follows the standard release process using Bamboo.  In OpenMRS Bamboo, you should find the latest
green build that has completed that matches what you wish to release.
https://ci.openmrs.org/browse/SDK-SDK

On the left, you will find a "Release to Maven" step.  For information on this process, see the wiki:
https://wiki.openmrs.org/display/docs/Releasing+a+Module+from+Bamboo

## Usage statistics

OpenMRS SDK gathers anonymous [usage statistics](https://docs.google.com/spreadsheets/d/1yMcfBl10l32YxWtXneD0wJZh11-qaLBMAwQFC9JUogA/edit#gid=42570905), which are shared on request. Please contact the project lead to request read-only access.
