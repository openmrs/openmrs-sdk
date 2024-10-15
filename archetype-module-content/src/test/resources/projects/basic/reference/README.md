# <u>Content Package Template</u>

This is a template repo to be used to set up and share Content Packages.

The contents of a typical Content Package are:
* **Configuration**
    * This folder holds [Initializer compatible configuration metadata]([url](https://github.com/mekomsolutions/openmrs-module-initializer/blob/main/README.md)) that make up the content package. For example, in the /config directory, this includes:
        * **Forms** (in /ampathforms)
        * **Concepts** (in [/ocl]([url](https://github.com/mekomsolutions/openmrs-module-initializer/blob/main/README.md#:~:text=Open%20Concept%20Lab%20(ZIP%20Files))), [/concepts]([url](https://github.com/mekomsolutions/openmrs-module-initializer/blob/main/readme/concepts.md)))
        * **Programmatic Metadata** such as:
            * Programs (in [/programs]([url](https://github.com/mekomsolutions/openmrs-module-initializer/blob/main/readme/prog.md)))
            * Encounter types (in [/encountertypes]([url](https://github.com/mekomsolutions/openmrs-module-initializer/blob/main/readme/et.md)))
            * Workflows (in [/programworkflows]([url](https://github.com/mekomsolutions/openmrs-module-initializer/blob/main/readme/prog.md)))
            * Identifiers and other metadata
* **content.properties File**
    * Contents: This file specifies the required ESMs and OMODs (frontend modules and backend modules) that make up the Content Package.
    * Importance:
        * The content.properties file is important because when Implementers add this Content Package to their distribution, the content.properties file will automatically be read and compared with their exitisting distro.properties file.
        * An automatic distro Build Helper Tool then fetches the content package's information and extracts the content into the Implementation's distro.properties file.
        * **Dependencies** are especially important here, as the Build Helper Tool will add any dependencies from the Content Package into an Implementation's distro.properties file.

Running Spotless
----------------
This project uses Spotless for code formatting. Spotless is embedded in the build process, so when you run `mvn clean package`, Spotless will automatically format your code according to the project's style guidelines.

If you want to run Spotless separately, you can use the following Maven commands:

To apply the formatting:

    mvn spotless:apply

This will automatically format your code according to the project's style guidelines. It's recommended to run this command before committing your changes.

To check if your code adheres to the style guidelines without making any changes, you can run:

    mvn spotless:check

If this command reports any violations, you can then run `mvn spotless:apply` to fix them.

Remember, in most cases, you don't need to run these commands separately as Spotless will run automatically during the build process with `mvn clean package`.
