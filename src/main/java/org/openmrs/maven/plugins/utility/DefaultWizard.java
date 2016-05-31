package org.openmrs.maven.plugins.utility;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;
import org.openmrs.maven.plugins.model.Server;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.*;
import java.util.Map.Entry;

/**
 * Class for attribute helper functions
 */
@Component(role=Wizard.class)
public class DefaultWizard implements Wizard {
    private static final String EMPTY_STRING = "";
    private static final String NONE = "(none)";
    private static final String DEFAULT_CHOICE_TMPL = "Which one do You choose? [";
    private static final String DEFAULT_OPTION_TMPL = "%d) %s";
    private static final String DEFAULT_OTHER_OPTION_TMPL = "%d) Other";
    private static final String DEFAULT_SERVER_NAME = "server";
    private static final String DEFAULT_VALUE_TMPL = "Define value for property '%s'";
    private static final String DEFAULT_VALUE_TMPL_WITH_DEFAULT = "Define value for property '%s': (default: '%s')";
    private static final String DEFAULT_FAIL_MESSAGE = "Server with such serverId is not exists";
    private static final String INVALID_SERVER = "Invalid server Id";
    private static final String YESNO = " [Y/n]";

    @Requirement
    Prompter prompter;

    public DefaultWizard(){};

    public DefaultWizard(Prompter prompter) {
        this.prompter = prompter;
    }

    /**
     * Prompt for serverId, and get default serverId which is not exists,
     * if serverId is not set before
     *
     * @param server @return
     * @throws PrompterException
     */
    @Override
    public void promptForNewServerIfMissing(Server server) {
        String defaultServerId = DEFAULT_SERVER_NAME;
        int indx = 0;
        while (new File(Server.getDefaultServersPath(), defaultServerId).exists()) {
            indx++;
            defaultServerId = DEFAULT_SERVER_NAME + String.valueOf(indx);
        }
        String serverId =  promptForValueIfMissingWithDefault("Specify server id (-D%s) (default: '%s')", server.getServerId(), "serverId", defaultServerId);
        server.setServerId(serverId);
    }



    /**
     * Prompt for a value if it not set, and default value is set
     *
     * @param message
     * @param value
     * @param parameterName
     * @param defValue
     * @return value
     * @throws PrompterException
     */
    @Override
    public String promptForValueIfMissingWithDefault(String message, String value, String parameterName, String defValue) {
        if (value != null) {
            return value;
        }
        String textToShow = null;
        // check if there no default value
        if (defValue.equals(EMPTY_STRING)){
            textToShow = String.format(message != null ? message : DEFAULT_VALUE_TMPL, parameterName);
        }
        else {
            textToShow = String.format(message != null? message : DEFAULT_VALUE_TMPL_WITH_DEFAULT, parameterName, defValue);
        }
        String val = prompt(textToShow);
        if (val.equals(EMPTY_STRING)) {
            val = defValue;
        }
        return val;
    }

    private String promptForMissingValueWithOptions(String message, String value, String parameterName, List<String> options){
        if (value != null) {
            return value;
        }
        String question = String.format(message != null? message : DEFAULT_VALUE_TMPL_WITH_DEFAULT, parameterName, options.get(0));
        StringBuilder choiceBuilder = new StringBuilder(DEFAULT_CHOICE_TMPL);

        showMessage(question);
        System.out.println("");
        int i = 1;
        for(String option : options){
            System.out.println(String.format(DEFAULT_OPTION_TMPL, i, option));
            choiceBuilder.append((i)+"/");
            i++;
        }
        System.out.println(String.format(DEFAULT_OTHER_OPTION_TMPL, i));
        choiceBuilder.append((i));
        String val = prompt(choiceBuilder.toString()+"]");
        if (val.equals(EMPTY_STRING)) {
            return options.get(0);
        } else {
            if(StringUtils.isNumeric(val)){
                if(Integer.parseInt(val)==i){
                    return prompt(String.format(DEFAULT_VALUE_TMPL, parameterName));
                } else if(Integer.parseInt(val)<i){
                    return options.get(Integer.parseInt(val)-1);
                }
            }
            System.out.println("\nPlease insert valid option number!");
            return promptForMissingValueWithOptions(message, value, parameterName, options);
        }
    }

    private String prompt(String textToShow){
        try {
            return prompter.prompt(textToShow);
        } catch (PrompterException e) {
            throw new RuntimeException(e);
        }
    }
    public void showMessage(String textToShow){
        try {
            prompter.showMessage(textToShow);
        } catch (PrompterException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Prompt for a value with list of proposed values
     * @param value
     * @param parameterName
     * @param values
     * @return value
     * @throws PrompterException
     */
    @Override
    public String promptForValueWithDefaultList(String value, String parameterName, List<String> values) {
        if (value != null) return value;
        String defaultValue = values.size() > 0 ? values.get(0) : NONE;
        final String text = DEFAULT_VALUE_TMPL_WITH_DEFAULT + " (possible: %s)";
        String val = prompt(String.format(text, parameterName, defaultValue, StringUtils.join(values.toArray(), ", ")));
        if (val.equals(EMPTY_STRING)) val = defaultValue;
        return val;
    }
    @Override
    public String promptForValueWithDefaultList(String value, String parameterName, String defaultValue, List<String> values) {
        if (value != null) return value;
        final String text = DEFAULT_VALUE_TMPL_WITH_DEFAULT + " (possible: %s)";
        String val = prompt(String.format(text, parameterName, defaultValue, StringUtils.join(values.toArray(), ", ")));
        if (val.equals(EMPTY_STRING)) val = defaultValue;
        return val;
    }

    /**
     * Prompt for a value if it not set, and default value is NOT set
     * @param value
     * @param parameterName
     * @return
     * @throws PrompterException
     */
    @Override
    public String promptForValueIfMissing(String value, String parameterName) {
        return promptForValueIfMissingWithDefault(null, value, parameterName, EMPTY_STRING);
    }

    /**
     * Print dialog Yes/No
     * @param text - text to display
     * @return
     */
    @Override
    public boolean promptYesNo(String text) {
        String yesNo = null;
        yesNo = prompt(text.concat(YESNO));
        return yesNo.equals("") || yesNo.toLowerCase().equals("y");
    }

    /**
     * Check if value is submit
     * @param value
     * @return
     */
    @Override
    public boolean checkYes(String value) {
        String val = value.toLowerCase();
        return val.equals("true") || val.equals("yes");
    }

    /**
     * Get path to server by serverId and prompt if missing
     * @return
     * @throws MojoFailureException
     */
    @Override
    public String promptForExistingServerIdIfMissing(String serverId) {
        File omrsHome = new File(System.getProperty("user.home"), SDKConstants.OPENMRS_SERVER_PATH);
        List<String> servers = getListOf5RecentServers();
        serverId = promptForValueWithDefaultList(serverId, "serverId", servers);
        if (serverId.equals(NONE)) {
            throw new RuntimeException(INVALID_SERVER);
        }
        File serverPath = new File(omrsHome, serverId);
        if (!serverPath.exists()) {
            throw new RuntimeException("There is no server with the given server id. Please create it first using openmrs-sdk:setup.");
        }
        return serverId;
    }

    @Override
    public void promptForPlatformVersionIfMissing(Server server, List<String> versions) {
        String version = promptForMissingValueWithOptions("You can install the following versions of a platform",
                server.getVersion(), "version", versions);
        server.setVersion(version);
    }

    @Override
    public void promptForDistroVersionIfMissing(Server server) {
        List<String> options = Arrays.asList(
                "2.1",
                "2.2",
                "2.3",
                "2.4-SNAPSHOT");

        String version = promptForMissingValueWithOptions ("You can install the following versions of distribution",
                server.getVersion(), "version", options);
        server.setVersion(version);
    }

    /**
     * old prompt for db settings
     * @param server
     */
    @Deprecated
    public void promptForDbSettingsIfMissing(Server server) {
        //set driver
        String dbDriver = promptForValueIfMissingWithDefault("Please specify database (-D%s) (default: '%s')",
                server.getDbDriver(), "dbDriver", "mysql");
        String defaultUri = SDKConstants.URI_MYSQL;
        if ((dbDriver.equals("postgresql")) || (dbDriver.equals(SDKConstants.DRIVER_POSTGRESQL))) {
            server.setDbDriver(SDKConstants.DRIVER_POSTGRESQL);
            defaultUri = SDKConstants.URI_POSTGRESQL;
        }
        else if ((dbDriver.equals("h2")) || (dbDriver.equals(SDKConstants.DRIVER_H2))) {
            server.setDbDriver(SDKConstants.DRIVER_H2);
            defaultUri = SDKConstants.URI_H2;
        }
        else if (dbDriver.equals("mysql")) {
            server.setDbDriver(SDKConstants.DRIVER_MYSQL);
        }
        else server.setParam(Server.PROPERTY_DB_DRIVER, server.getDbDriver());
        //set uri
        String dbUri = promptForValueIfMissingWithDefault(
                "Please specify database uri (-D%s) (default: '%s')",
                server.getDbUri(), "dbUri", defaultUri);
        if (dbUri.startsWith("jdbc:mysql:")) {
            dbUri = addMySQLParamsIfMissing(dbUri);
        }
        server.setDbUri(dbUri);
    }

    @Override
    public void promptForDbDistro(Server server) {
        server.setDbDriver(SDKConstants.DRIVER_MYSQL);
        String dbUri = promptForValueIfMissingWithDefault(
                "The distribution requires MySQL database. Please specify database uri (-D%s) (default: '%s')",
                server.getDbUri(), "dbUri", SDKConstants.URI_MYSQL);
        if (dbUri.startsWith("jdbc:mysql:")) {
            dbUri = addMySQLParamsIfMissing(dbUri);
        }
        server.setDbUri(dbUri);
        promptForDbCredentialsIfMissing(server);
    }

    @Override
    public void promptForDbPlatform(Server server) {
        boolean h2 = promptYesNo(
                "Would you like to use the h2 database (-DdbDriver) (note that some modules do not support it)?");
        if(h2){
            server.setDbDriver(SDKConstants.DRIVER_H2);
            String dbUri = promptForValueIfMissingWithDefault(
                    "Please specify database uri (-D%s) (default: '%s')",
                    server.getDbUri(), "dbUri", SDKConstants.URI_H2);
            server.setDbUri(dbUri);
            promptForDbCredentialsIfMissing(server);
        }
    }

    @Override
    public void promptForDbCredentialsIfMissing(Server server) {
        String defaultUser = "root";
        String user = promptForValueIfMissingWithDefault(
                "Please specify database username (-D%s) (default: '%s')",
                server.getDbUser(), "dbUser", defaultUser);
        server.setDbUser(user);
        //set password
        String dbPassword = promptForValueIfMissingWithDefault(
                "Please specify database password (-D%s) (default: '')",
                server.getDbPassword(), "dbPassword", "");
        server.setDbPassword(dbPassword);
    }

    /**
     * Check if we are currenly inside "server" folder and get path
     * @return
     */
    @Override
    public File getCurrentServerPath() throws MojoExecutionException {
        File currentFolder = new File(System.getProperty("user.dir"));
        File openmrsHome = new File(System.getProperty("user.home"), SDKConstants.OPENMRS_SERVER_PATH);
        File current = new File(currentFolder, SDKConstants.OPENMRS_SERVER_PROPERTIES);
        File parent = new File(currentFolder.getParent(), SDKConstants.OPENMRS_SERVER_PROPERTIES);
        File propertiesFile = null;
        if (current.exists()) propertiesFile = current;
        else if (parent.exists()) propertiesFile = parent;
        if (propertiesFile != null) {
            File server = propertiesFile.getParentFile();
            if (!server.getParentFile().equals(openmrsHome)) return null;
            Server properties = Server.loadServer(server);
            if (properties.getParam(Server.PROPERTY_SERVER_ID) != null) return propertiesFile.getParentFile();
        }
        return null;
    }

    /**
     * Get 5 last modified servers
     * @return
     */
    @Override
    public List<String> getListOf5RecentServers() {
        final int count = 5;
        String home = System.getProperty("user.home");
        File openMRS = new File(home, SDKConstants.OPENMRS_SERVER_PATH);
        Map<Long, String> sortedMap = new TreeMap<Long, String>(Collections.reverseOrder());
        File [] list = (openMRS.listFiles() == null) ? new File[0] : openMRS.listFiles();
        for (File f: list) {
            if (f.isDirectory()) sortedMap.put(f.lastModified(), f.getName());
        }
        int length = sortedMap.size() < count ? sortedMap.size() : count;
        return new ArrayList<String>(sortedMap.values()).subList(0, length);
    }

    @Override
    public String addMySQLParamsIfMissing(String dbUri) {
		Map<String, String> mysqlParams = new LinkedHashMap<String, String>();
		mysqlParams.put("autoReconnect", "true");
		mysqlParams.put("sessionVariables", "storage_engine=InnoDB");
		mysqlParams.put("useUnicode", "true");
		mysqlParams.put("characterEncoding", "UTF-8");

		int querySeparator = dbUri.indexOf("?");

		String query = querySeparator > 0 ? dbUri.substring(querySeparator + 1) : null;
		if (query != null) {
			String[] params = query.split("&");
			for (String param : params) {
				int valueSeparator = param.indexOf("=");
			    try {
					String key = valueSeparator > 0 ? URLDecoder.decode(param.substring(0, valueSeparator), "UTF-8") : param;
					String value = valueSeparator > 0 ? URLDecoder.decode(param.substring(valueSeparator + 1), "UTF-8") : "";
					mysqlParams.put(key, value);
				}
				catch (UnsupportedEncodingException e) {
					throw new RuntimeException(e);
				}
			}
		}

		StringBuilder newUri = new StringBuilder(querySeparator > 0 ? dbUri.substring(0, querySeparator) : dbUri);
		newUri.append("?");
		for (Entry<String, String> param : mysqlParams.entrySet()) {
			try {
				newUri.append(URLEncoder.encode(param.getKey(), "UTF-8")).append("=").append(URLEncoder.encode(param.getValue(), "UTF-8")).append("&");
			}
			catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}
		dbUri = newUri.toString();
		dbUri = dbUri.substring(0, dbUri.length() - 1);
		return dbUri;
	}
}
