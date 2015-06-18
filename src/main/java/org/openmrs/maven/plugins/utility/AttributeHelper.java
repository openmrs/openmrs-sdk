package org.openmrs.maven.plugins.utility;

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
}
