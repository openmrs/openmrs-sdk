package org.openmrs.maven.plugins.utility;

import lombok.Data;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;

/**
 * Component that allows access to the Maven components set within the current execution environment
 */
@Data
public class MavenEnvironment {
	private MavenProject mavenProject;
	private MavenSession mavenSession;
	private Settings settings;
	private ArtifactMetadataSource artifactMetadataSource;
	private ArtifactFactory artifactFactory;
	private BuildPluginManager pluginManager;
	private Wizard wizard;

	public ArtifactHelper getArtifactHelper() {
		return new ArtifactHelper(this);
	}

	public VersionsHelper getVersionsHelper() {
		return new VersionsHelper(artifactFactory, mavenProject, mavenSession, artifactMetadataSource);
	}
}
