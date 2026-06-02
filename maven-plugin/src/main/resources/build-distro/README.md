# Distribution

This directory contains the OpenMRS distribution artifacts and Docker configuration.

## Artifacts

| Path | Contents |
|---|---|
| `web/openmrs_core/openmrs.war` | OpenMRS core WAR |
| `web/openmrs_modules/` | OpenMRS modules (`.omod` files) |
| `web/openmrs_owas/` | Open Web Apps |
| `web/openmrs_spa/` | Single Page Application frontend |
| `web/openmrs_config/` | Initializer configuration files |
| `web/openmrs-distro.properties` | Full list of components and versions |

To regenerate this distribution:
```
mvn openmrs-sdk:build-distro -Ddistro=openmrs-distro.properties
```
Add `-Dreset` to overwrite any files you have customised.
