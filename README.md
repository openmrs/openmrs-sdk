# openmrs-contrib-sdk-maven-plugin
A maven plugin for the OpenMRS SDK

## List of supported commands:

All commands can be executed by `mvn openmrs-sdk:<command>`

* `create-module` - Create module for OpenMRS 2.x <br/>
Options: none (only interactive mode)
* `create-platform-module` - Create module for OpenMRS 1.x <br/>
Options: none (only interactive mode)
* `setup-platform` - Setup server OpenMRS 1.x <br/>
Options: serverId, version, dbDriver, dbUri, dbUser, dbPassword
* `setup` - Setup server OpenMRS 2.x with modules <br/>
Options: serverId, version, dbDriver, dbUri, dbUser, dbPassword
* `install-module` - Install selected module to server <br/>
Options: serverId, groupId, artifactId, version
* `uninstall-module` - Uninstall selected module from server <br/>
Options: serverId, groupId, artifactId
* `run` - Start server <br/>
Options: serverId