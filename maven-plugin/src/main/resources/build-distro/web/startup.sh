#!/bin/bash -eux

OMRS_CONFIG_CREATE_TABLES=${OMRS_CONFIG_CREATE_TABLES:-false}
OMRS_CONFIG_AUTO_UPDATE_DATABASE=${OMRS_CONFIG_AUTO_UPDATE_DATABASE:-false}
OMRS_CONFIG_MODULE_WEB_ADMIN=${OMRS_CONFIG_MODULE_WEB_ADMIN:-true}
DEBUG=${DEBUG:-false}

RUNTIME_PROPS=/usr/local/tomcat/.OpenMRS/openmrs-runtime.properties

cat > /usr/local/tomcat/openmrs-server.properties << EOF
install_method=auto
connection.url=jdbc\:mysql\://${OMRS_CONFIG_CONNECTION_SERVER}\:3306/${OMRS_CONFIG_CONNECTION_DATABASE}?autoReconnect\=true&sessionVariables\=default_storage_engine\=InnoDB&useUnicode\=true&characterEncoding\=UTF-8
connection.username=${OMRS_CONFIG_CONNECTION_USERNAME}
connection.password=${OMRS_CONFIG_CONNECTION_PASSWORD}
has_current_openmrs_database=true
create_database_user=false
OMRS_CONFIG_MODULE_WEB_ADMIN=${OMRS_CONFIG_MODULE_WEB_ADMIN}
create_tables=${OMRS_CONFIG_CREATE_TABLES}
auto_update_database=${OMRS_CONFIG_AUTO_UPDATE_DATABASE}
EOF

echo "------  Starting distribution -----"
cat /root/openmrs-distro.properties
echo "-----------------------------------"

# wait for mysql to initialise
/usr/local/tomcat/wait-for-it.sh --timeout=3600 ${OMRS_CONFIG_CONNECTION_SERVER}:3306

if [ "${DEBUG}" = "true" ]; then
    export JPDA_ADDRESS="1044"
    export JPDA_TRANSPORT=dt_socket
fi

if [ ! -f "${RUNTIME_PROPS}" ]; then
    # openmrs-runtime.properties does not exist yet — run the auto-install wizard
    # to create the database schema and write the file.
    #
    # Why two steps are needed:
    # When runtime.properties is absent, OpenMRS runs startOpenmrs() in a
    # background thread (via the InitializationFilter wizard).  By the time that
    # thread fires, the Spring DispatcherServlet has already initialised with an
    # empty handler mapping and will not reinitialise after modules register.
    #
    # When runtime.properties IS present, OpenMRS calls startOpenmrs()
    # synchronously from Listener.contextInitialized(), so modules register
    # BEFORE the DispatcherServlet initialises — all URL mappings are found
    # on first scan and everything works.
    echo "No runtime properties found — running auto-install wizard to create schema..."

    if [ "${DEBUG}" = "true" ]; then
        /usr/local/tomcat/bin/catalina.sh jpda run &
    else
        /usr/local/tomcat/bin/catalina.sh run &
    fi

    # trigger the wizard
    sleep 15
    curl -L http://localhost:8080/openmrs/ > /dev/null 2>&1 || true

    # wait for the wizard to complete and write openmrs-runtime.properties
    echo "Waiting for OpenMRS auto-install to complete (watching for ${RUNTIME_PROPS})..."
    ELAPSED=0
    while [ ! -f "${RUNTIME_PROPS}" ] && [ "${ELAPSED}" -lt 1800 ]; do
        sleep 5
        ELAPSED=$((ELAPSED + 5))
    done

    if [ ! -f "${RUNTIME_PROPS}" ]; then
        echo "ERROR: Auto-install timed out — runtime properties never written."
        exit 1
    fi

    echo "Auto-install complete. Stopping Tomcat before clean restart..."
    # Graceful stop with 60s timeout so in-progress background threads (Hibernate
    # sessions, module startup) can finish cleanly before the JVM exits.  A forced
    # kill can leave MySQL sessions in a state that causes "Session is closed!"
    # errors on the next startup.
    /usr/local/tomcat/bin/catalina.sh stop 60 2>/dev/null || \
    /usr/local/tomcat/bin/catalina.sh stop -force 2>/dev/null || true
    sleep 5
    echo "Restarting Tomcat with runtime properties present for synchronous startup..."
fi

# Start Tomcat — either the only startup (runtime.properties already existed) or
# the second startup after auto-install (runtime.properties now present).
if [ "${DEBUG}" = "true" ]; then
    /usr/local/tomcat/bin/catalina.sh jpda run &
else
    /usr/local/tomcat/bin/catalina.sh run &
fi

wait ${!}
