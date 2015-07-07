# openmrs-contrib-sdk-maven-plugin
A maven plugin for the OpenMRS 2.x SDK

## Requirements
 * Maven 3.x
 
## Installation
For installing sdk, just run: <br/>

```mvn org.openmrs.maven.plugins:openmrs-sdk-maven-plugin:setup-sdk```

## List of supported commands:
All commands can be executed by `mvn openmrs-sdk:<command>`

* `create-module` - Create OpenMRS module <br/>
Options: none (only interactive mode)
* `create-platform-module` - Create OpenMRS platform module <br/>
Options: none (only interactive mode)
* `setup-platform` - Setup platform server <br/>
Options: serverId, version, dbDriver, dbUri, dbUser, dbPassword
* `setup` - Setup server with modules <br/>
Options: serverId, version, dbDriver, dbUri, dbUser, dbPassword
* `install-module` - Install selected module to server <br/>
Options: serverId, groupId, artifactId, version
* `uninstall-module` - Uninstall selected module from server <br/>
Options: serverId, groupId, artifactId
* `upgrade` - Upgrade server or platform server to selected version <br/>
Options: serverId, version
* `upgrade-platform` - Upgrade platform server to selected version <br/>
Options: serverId, version
* `reset` - Reset server to default state (and drop db data) <br/>
Options: serverId, full
* `delete` - Delete server <br/>
Options: serverId
* `run` - Start server <br/>
Options: serverId