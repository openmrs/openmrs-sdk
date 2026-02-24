package org.openmrs.maven.plugins;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.openmrs.maven.plugins.cargo.CargoContainerRunner;
import org.openmrs.maven.plugins.utility.MavenEnvironment;
import org.openmrs.maven.plugins.utility.Wizard;

/**
 * Thin Mojo that delegates to {@link CargoContainerRunner} for running an
 * embedded servlet container. The correct container embed JARs (Tomcat / Jetty)
 * are provided by this module's POM dependencies; the container ID property
 * selects which one Cargo instantiates at runtime.
 */
@Mojo(name = "run-container", requiresProject = false)
public class RunContainerMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true)
	private MavenProject mavenProject;

	@Parameter(defaultValue = "${session}", readonly = true, required = true)
	private MavenSession mavenSession;

	@Parameter(defaultValue = "${settings}", readonly = true)
	private Settings settings;

	@Component
	private BuildPluginManager pluginManager;

	@Component
	private ArtifactMetadataSource artifactMetadataSource;

	@Component
	private ArtifactFactory artifactFactory;

	@Component
	private Wizard wizard;

	@Parameter(property = "serverId")
	private String serverId;

	@Parameter(property = "port")
	private Integer port;

	@Parameter(property = "watchApi")
	private Boolean watchApi;

	@Parameter(property = "containerId", required = true)
	private String containerId;

	@Override
	public void execute() throws MojoExecutionException {
		MavenEnvironment mavenEnvironment = new MavenEnvironment();
		mavenEnvironment.setMavenProject(mavenProject);
		mavenEnvironment.setMavenSession(mavenSession);
		mavenEnvironment.setSettings(settings);
		mavenEnvironment.setArtifactMetadataSource(artifactMetadataSource);
		mavenEnvironment.setArtifactFactory(artifactFactory);
		mavenEnvironment.setPluginManager(pluginManager);
		mavenEnvironment.setWizard(wizard);

		new CargoContainerRunner(containerId, serverId, port, watchApi, mavenEnvironment, wizard).run();
	}
}
