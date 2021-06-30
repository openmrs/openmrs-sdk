package org.openmrs.maven.plugins;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.ho.yaml.YamlDecoder;

import java.io.EOFException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

@Mojo(name = "help", requiresProject = false)
public class Help extends AbstractTask {

    private static final String HELP_FILE = "help.yaml";
    private static final String DESC_MESSAGE = "Description: ";
    private static final String INFO = "OpenMRS SDK %s";
    private static final String WIKI = "https://wiki.openmrs.org/display/docs/OpenMRS+SDK";

    public void executeTask() throws MojoExecutionException {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(HELP_FILE);
        YamlDecoder dec = new YamlDecoder(stream);
        HelpFormatter formatter = new HelpFormatter();
        formatter.setLeftPadding(4);
        formatter.setDescPadding(8);
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> keys = (Map<String, Object>) dec.readObject();
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> l = (List<Map<String, Object>>) keys.get("help");
            String printVersion = String.format(INFO, keys.get("version"));
            String printWikiLink = "For more info, see SDK documentation: " + WIKI;
            PrintWriter writer = new PrintWriter(System.out);
            writer.println();
            writer.println();
            writer.println(printVersion);
            writer.println();
            writer.println(printWikiLink);
            writer.println();
            writer.println();
            writer.flush();
            if (l == null) {
                throw new MojoExecutionException("Error during reading help data");
            }
            for (Map<String, Object> o: l) {
                Options options = new Options();
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> params = (List<Map<String, Object>>) o.get("options");
                String header = (o.get("desc") != null) ? o.get("desc").toString() : "None";
                header = DESC_MESSAGE.concat(header);
                if (params != null) {
                    for (Map<String, Object> x: params) {
                        String name = x.get("name").toString();
                        String desc = x.get("desc").toString();
                        if ((name == null) || (desc == null)) throw new MojoExecutionException("Error in help file structure");
                        options.addOption(new Option(name, desc));
                    }
                }
                formatter.printHelp(o.get("name").toString(), header, options, "");
                writer.println();
                writer.flush();
            }
            writer.flush();
        } catch (EOFException e) {
            throw new MojoExecutionException(e.getMessage());
        } finally {
            dec.close();
        }
    }
}
