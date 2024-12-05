package org.openmrs.maven.plugins;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.openmrs.maven.plugins.utility.OwaHelper;
import org.openmrs.maven.plugins.model.Project;
import org.openmrs.maven.plugins.utility.SDKConstants;
import org.openmrs.maven.plugins.utility.XmlHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

/**
 * Enhance an existing module by adding a feature
 */
@Mojo(name = "add-feature", requiresProject = false)
public class AddFeature extends AbstractTask {

	public static final String OPEN_WEB_APP = "Open Web App";

	public static final List<String> OPTIONS;
	static {
		OPTIONS = new ArrayList<>(1);
		OPTIONS.add(OPEN_WEB_APP);
	}

	/**
	 * feature user wants to add
	 */
	@Parameter(property = "feature")
	private String feature;

	@Override
	public void executeTask() throws MojoExecutionException {
		if (Project.hasProject(new File(System.getProperty("user.dir")))) {
			feature = wizard.promptForMissingValueWithOptions("What feature would you like to add?", feature, "feature",
					OPTIONS, null, null);
			if (feature.equals(OPEN_WEB_APP) || feature.equals("owa")) {
				addOwaSubmodule();
			} else {
				throw new MojoExecutionException(
						"Adding feature " + feature + " is not available. Available features: " + OPTIONS);
			}
		} else {
			throw new MojoExecutionException(
					"No project found in this directory. Please enter project's main directory and run this command again");
		}
	}

	private void addOwaSubmodule() throws MojoExecutionException {
		OwaHelper owaHelper = new OwaHelper(getMavenEnvironment());
		owaHelper.setInstallationDir(new File(mavenProject.getBasedir(), "owa"));
		owaHelper.createOwaProject();

		//apply changes to config.xml and main pom.xml
		wizard.showMessage("Modifying pom.xml files...");
		XmlHelper.modifyXml(new File(mavenProject.getBasedir(), "omod" + File.separator + "pom.xml"),
				"archetype-submodule-owa/omod.pom.xml");
		XmlHelper.modifyXml(new File(mavenProject.getBasedir(),
				"omod" + File.separator + "src" + File.separator + "main" + File.separator + "resources" + File.separator
						+ "config.xml"), "archetype-submodule-owa/config.xml");

		//run archetype to create skeleton configuration for maven owa submodule
		Properties properties = new Properties();
		properties.setProperty("artifactId", "owa");
		properties.setProperty("moduleArtifactId", mavenProject.getArtifactId());
		properties.setProperty("groupId", mavenProject.getGroupId());
		properties.setProperty("moduleGroupId", mavenProject.getGroupId());
		properties.setProperty("package", "owa");
		properties.setProperty("moduleVersion", mavenProject.getVersion());
		properties.setProperty("version", mavenProject.getVersion());
		mavenSession.getUserProperties().putAll(properties);

		wizard.showMessage("Creating OWA submodule...");
		executeMojo(
				plugin(
						groupId(SDKConstants.ARCHETYPE_PLUGIN_GROUP_ID),
						artifactId(SDKConstants.ARCHETYPE_PLUGIN_ARTIFACT_ID),
						version(SDKConstants.ARCHETYPE_PLUGIN_VERSION)
				),
				goal("generate"), configuration(
						element("interactiveMode", "false"),
						element("archetypeArtifactId", "openmrs-sdk-archetype-submodule-owa"),
						element("archetypeGroupId", "org.openmrs.maven.archetypes"),
						element("archetypeVersion", SDKConstants.getSDKInfo().getVersion())
				),
				executionEnvironment(mavenProject, mavenSession, pluginManager)
		);
	}
}
