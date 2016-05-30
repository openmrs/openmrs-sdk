package org.openmrs.maven.plugins.utility;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;

/**
 * Class for attribute helper functions
 */
@Component(role=Wizard.class)
public class DefaultWizard implements Wizard {
    private static final String EMPTY_STRING = "";
    private static final String NONE = "(none)";
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
     * @param omrsPath
     * @param serverId
     * @return
     * @throws PrompterException
     */
    @Override
    public String promptForNewServerIfMissing(String omrsPath, String serverId) {
        String defaultServerId = DEFAULT_SERVER_NAME;
        int indx = 0;
        while (new File(omrsPath, defaultServerId).exists()) {
            indx++;
            defaultServerId = DEFAULT_SERVER_NAME + String.valueOf(indx);
        }
        return promptForValueIfMissingWithDefault(serverId, "serverId", defaultServerId);
    }

    /**
     * Prompt for a value if it not set, and default value is set
     * @param value
     * @param parameterName
     * @param defValue
     * @return value
     * @throws PrompterException
     */
    @Override
    public String promptForValueIfMissingWithDefault(String value, String parameterName, String defValue) {
        if (value != null) return value;
        String textToShow = null;
        // check if there no default value
        if (defValue.equals(EMPTY_STRING)) textToShow = String.format(DEFAULT_VALUE_TMPL, parameterName);
        else textToShow = String.format(DEFAULT_VALUE_TMPL_WITH_DEFAULT, parameterName, defValue);
        String val = prompt(textToShow);
        if (val.equals(EMPTY_STRING)) val = defValue;
        return val;
    }

    private String prompt(String textToShow){
        try {
            return prompter.prompt(textToShow);
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
        return promptForValueIfMissingWithDefault(value, parameterName, EMPTY_STRING);
    }

    /**
     * Print dialog Yes/No
     * @param text - text to display
     * @return
     */
    @Override
    public boolean dialogYesNo(String text) {
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
    public File getServerPath(String serverId, String failureMessage) throws MojoFailureException {
        File omrsHome = new File(System.getProperty("user.home"), SDKConstants.OPENMRS_SERVER_PATH);
        String resultServerId = null;
        List<String> servers = getListOf5RecentServers();
        resultServerId = promptForValueWithDefaultList(serverId, "serverId", servers);
        if (resultServerId.equals(NONE)) {
            throw new MojoFailureException(INVALID_SERVER);
        }
        File serverPath = new File(omrsHome, resultServerId);
        if (!serverPath.exists()) {
            throw new MojoFailureException(failureMessage);
        }
        return serverPath;
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
            ServerConfig properties = ServerConfig.loadServerConfig(server);
            if (properties.getParam(SDKConstants.PROPERTY_SERVER_ID) != null) return propertiesFile.getParentFile();
        }
        return null;
    }

    /**
     * Get server with default failure message
     * @param serverId
     * @return
     * @throws MojoFailureException
     */
    @Override
    public File getServerPath(String serverId) throws MojoFailureException {
        return getServerPath(serverId, DEFAULT_FAIL_MESSAGE);
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
