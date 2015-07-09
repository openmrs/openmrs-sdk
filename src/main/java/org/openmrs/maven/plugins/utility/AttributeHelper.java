package org.openmrs.maven.plugins.utility;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;

import java.io.File;

/**
 * Class for attribute helper functions
 */
public class AttributeHelper {
    private static final String EMPTY_STRING = "";
    private static final String DEFAULT_SERVER_NAME = "server";
    private static final String DEFAULT_VALUE_TMPL = "Define value for property '%s'";
    private static final String DEFAULT_VALUE_TMPL_WITH_DEFAULT = "Define value for property '%s': (default: '%s')";
    private static final String DEFAULT_FAIL_MESSAGE = "Server with such serverId is not exists";
    private static final String YESNO = " [Y/N]";

    private Prompter prompter;

    public AttributeHelper(Prompter prompter) {
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
    public String promptForNewServerIfMissing(String omrsPath, String serverId) throws PrompterException {
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
    public String promptForValueIfMissingWithDefault(String value, String parameterName, String defValue) throws PrompterException {
        if (value != null) return value;
        String textToShow = null;
        // check if there no default value
        if (defValue.equals(EMPTY_STRING)) textToShow = String.format(DEFAULT_VALUE_TMPL, parameterName);
        else textToShow = String.format(DEFAULT_VALUE_TMPL_WITH_DEFAULT, parameterName, defValue);
        String val = prompter.prompt(textToShow);
        if (val.equals(EMPTY_STRING)) val = defValue;
        return val;
    }

    /**
     * Prompt for a value if it not set, and default value is NOT set
     * @param value
     * @param parameterName
     * @return
     * @throws PrompterException
     */
    public String promptForValueIfMissing(String value, String parameterName) throws PrompterException {
        return promptForValueIfMissingWithDefault(value, parameterName, EMPTY_STRING);
    }

    /**
     * Print dialog Yes/No
     * @param text - text to display
     * @return
     */
    public boolean dialogYesNo(String text) throws PrompterException {
        String yesNo = prompter.prompt(text.concat(YESNO));
        return yesNo.equals("") || yesNo.toLowerCase().equals("y");
    }

    /**
     * Check if value is submit
     * @param value
     * @return
     */
    public boolean checkYes(String value) {
        String val = value.toLowerCase();
        return val.equals("true") || val.equals("yes");
    }

    /**
     * Get path to server by serverId and prompt if missing
     * @return
     * @throws MojoFailureException
     */
    public File getServerPath(String serverId, String failureMessage) throws MojoFailureException {
        File omrsHome = new File(System.getProperty("user.home"), SDKConstants.OPENMRS_SERVER_PATH);
        String resultServerId = null;
        try {
            resultServerId = promptForValueIfMissing(serverId, "serverId");
        } catch (PrompterException e) {
            throw new MojoFailureException(e.getMessage());
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
    public File getCurrentServerPath() throws MojoExecutionException {
        File currentFolder = new File(System.getProperty("user.dir"));
        File current = new File(currentFolder, SDKConstants.OPENMRS_SERVER_PROPERTIES);
        File parent = new File(currentFolder.getParent(), SDKConstants.OPENMRS_SERVER_PROPERTIES);
        File propertiesFile = null;
        if (current.exists()) propertiesFile = current;
        else if (parent.exists()) propertiesFile = parent;
        if (propertiesFile != null) {
            PropertyManager properties = new PropertyManager(propertiesFile.getPath());
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
    public File getServerPath(String serverId) throws MojoFailureException {
        return getServerPath(serverId, DEFAULT_FAIL_MESSAGE);
    }
}
