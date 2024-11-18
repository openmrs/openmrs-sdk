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
	ArtifactMetadataSource artifactMetadataSource;
	ArtifactFactory artifactFactory;
	BuildPluginManager pluginManager;
	Wizard wizard;
}
