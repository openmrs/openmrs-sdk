#!/bin/bash -eux

OMRS_CONFIG_CREATE_TABLES=${OMRS_CONFIG_CREATE_TABLES:-false}
OMRS_CONFIG_AUTO_UPDATE_DATABASE=${OMRS_CONFIG_AUTO_UPDATE_DATABASE:-false}
OMRS_CONFIG_MODULE_WEB_ADMIN=${OMRS_CONFIG_MODULE_WEB_ADMIN:-true}
DEBUG=${DEBUG:-false}

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

if [ $DEBUG ]; then
    export JPDA_ADDRESS="1044"
    export JPDA_TRANSPORT=dt_socket
fi

# start tomcat in background
/usr/local/tomcat/bin/catalina.sh jpda run &

# trigger first filter to start data importation
sleep 15
curl -L http://localhost:8080/openmrs/ > /dev/null
sleep 15

# bring tomcat process to foreground again
wait ${!}
