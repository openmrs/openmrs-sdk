package org.openmrs.maven.plugins.utility;

/**
 * Utility class for OS specific tasks
 */
public class OS {

    private String getOsName() {
        return System.getProperty("os.name", "unknown");
    }

    private boolean isWindows() {
        return (getOsName().toLowerCase().indexOf("windows") >= 0);
    }

    private boolean isLinux() {
        return getOsName().toLowerCase().indexOf("linux") >= 0;
    }

    private boolean isUnix() {
        final String os = getOsName().toLowerCase();
        if ((os.indexOf("sunos") >= 0) || (os.indexOf("linux") >= 0)) return true;
        if (isMac() && (System.getProperty("os.version", "").startsWith("10."))) return true;
        return false;
    }

    private boolean isMac() {
        final String os = getOsName().toLowerCase();
        return os.startsWith("mac") || os.startsWith("darwin");
    }

    /**
     * Execute command on current OS
     * @param command
     * @return
     * @throws Exception
     */
    public Process executeCommand(String command) throws Exception {
        Process p = null;
        // basically, cmd is windows shell, for other OS we can get SHELL env variable
        String shell = isWindows() ? "cmd /c " : System.getenv("SHELL").concat(" -c ");
        String resultCommand =shell.concat(command);
        p = Runtime.getRuntime().exec(resultCommand);
        p.waitFor();
        return p;
    }
}
