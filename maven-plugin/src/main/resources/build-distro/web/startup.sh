#!/bin/bash -eux

DB_CREATE_TABLES=${DB_CREATE_TABLES:-false}
DB_ADD_DEMO_DATA=${DB_ADD_DEMO_DATA:-false}
DB_AUTO_UPDATE=${DB_AUTO_UPDATE:-false}
DEBUG=${DEBUG:-false}

cat > /usr/local/tomcat/openmrs-server.properties << EOF
install_method=auto
connection.url=jdbc\:mysql\://${DB_HOST}\:3306/${DB_DATABASE}?autoReconnect\=true&sessionVariables\=storage_engine\=InnoDB&useUnicode\=true&characterEncoding\=UTF-8
connection.username=${DB_USERNAME}
connection.password=${DB_PASSWORD}
has_current_openmrs_database=true
create_database_user=false
module_web_admin=true
create_tables=${DB_CREATE_TABLES}
add_demo_data=${DB_ADD_DEMO_DATA}
auto_update_database=${DB_AUTO_UPDATE}
EOF

echo "------  Starting distribution -----"
cat /root/openmrs-distro.properties
echo "-----------------------------------"

# wait for mysql to initialise
/usr/local/tomcat/wait-for-it.sh --timeout=3600 ${DB_HOST}:3306

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
