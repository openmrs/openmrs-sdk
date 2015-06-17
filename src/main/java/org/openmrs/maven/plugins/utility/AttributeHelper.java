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
    private static final String DEFAULT_SERVER_NAME_TMPL = "Define value for property 'serverId'";
    private static final String DEFAULT_SERVER_NAME_NEW_TMPL = "Define value for property 'serverId': (default: '%s')";

    private Prompter prompter;

    public AttributeHelper(Prompter prompter) {
        this.prompter = prompter;
    }

    public String promptForNewServerIfMissing(String omrsPath, String serverId) throws PrompterException {
        String defaultServerId = DEFAULT_SERVER_NAME;
        int indx = 0;
        while (new File(omrsPath, defaultServerId).exists()) {
            indx++;
            defaultServerId = DEFAULT_SERVER_NAME + String.valueOf(indx);
        }
        return promptForValueIfMissing(serverId, String.format(DEFAULT_SERVER_NAME_NEW_TMPL, defaultServerId), defaultServerId);
    }

    public String promptForServerIfMissing(String serverId) throws PrompterException {
        return promptForValueIfMissing(serverId, DEFAULT_SERVER_NAME_TMPL, "");
    }

    /**
     * Prompt for a value if it not set
     * @param value
     * @param text
     * @param defValue
     * @return value
     * @throws PrompterException
     */
    private String promptForValueIfMissing(String value, String text, String defValue) throws PrompterException {
        if (value != null) return value;
        String val = prompter.prompt(text);
        if (val.equals(EMPTY_STRING)) val = defValue;
        return val;
    }
}
