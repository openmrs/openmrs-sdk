package org.openmrs.maven.plugins.utility;

import org.codehaus.plexus.components.interactivity.Prompter;
import org.codehaus.plexus.components.interactivity.PrompterException;

import java.io.File;

/**
 * Class for static attribute helper functions
 */
public class AttributeHelper {
    private static final String EMPTY_STRING = "";
    private static final String DEFAULT_SERVER_NAME = "server";
    private static final String DEFAULT_SERVER_NAME_TMPL = "Define value for property 'serverId': (default: '%s')";

    public static String makeServerId(Prompter prompter, String omrsPath, String serverId) throws PrompterException {
        String defaultServerId = DEFAULT_SERVER_NAME;
        int indx = 0;
        while (new File(omrsPath, defaultServerId).exists()) {
            indx++;
            defaultServerId = DEFAULT_SERVER_NAME + String.valueOf(indx);
        }
        return AttributeHelper.makeValue(prompter, serverId, String.format(DEFAULT_SERVER_NAME_TMPL, defaultServerId), defaultServerId);
    }

    private static String makeValue(Prompter prompter, String value, String text, String defValue) throws PrompterException {
        if (value != null) return value;
        String val = prompter.prompt(text);
        if (val.equals(EMPTY_STRING)) val = defValue;
        return val;
    }
}
