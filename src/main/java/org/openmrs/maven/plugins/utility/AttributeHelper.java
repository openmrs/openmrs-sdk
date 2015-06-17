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

    public String promptForNewServerIfMissing(String omrsPath, String serverId) throws PrompterException {
        String defaultServerId = DEFAULT_SERVER_NAME;
        int indx = 0;
        while (new File(omrsPath, defaultServerId).exists()) {
            indx++;
            defaultServerId = DEFAULT_SERVER_NAME + String.valueOf(indx);
        }
        return promptForValueIfMissing(serverId, String.format(DEFAULT_VALUE_TMPL_WITH_DEFAULT, "serverId", defaultServerId), defaultServerId);
    }

    public String promptForServerIfMissing(String serverId) throws PrompterException {
        return promptForValueIfMissing(serverId, String.format(DEFAULT_VALUE_TMPL, "serverId"), EMPTY_STRING);
    }

    public String promptFotArtifactIfMissing(String artifactId) throws PrompterException {
        return promptForValueIfMissing(artifactId, String.format(DEFAULT_VALUE_TMPL, "artifactId"), EMPTY_STRING);
    }

    public String promptForVersionIfMissing(String version) throws PrompterException {
        return promptForValueIfMissing(version, String.format(DEFAULT_VALUE_TMPL, "version"), EMPTY_STRING);
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
