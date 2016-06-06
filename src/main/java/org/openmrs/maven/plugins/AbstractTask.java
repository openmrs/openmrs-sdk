package org.openmrs.maven.plugins;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.project.MavenProject;
import org.openmrs.maven.plugins.utility.ModuleInstaller;
import org.openmrs.maven.plugins.utility.VersionsHelper;
import org.openmrs.maven.plugins.utility.Wizard;

/**
 *
 */
public abstract class AbstractTask extends AbstractMojo {

    /**
     * The project currently being build.
     *
     * @parameter expression="${project}"
     * @required
     */
    MavenProject mavenProject;

    /**
     * The current Maven session.
     *
     * @parameter expression="${session}"
     * @required
     */
    MavenSession mavenSession;

    /**
     * The artifact metadata source to use.
     *
     * @component
     * @required
     * @readonly
     */
    ArtifactMetadataSource artifactMetadataSource;

    /**
     * @component
     * @required
     */
    ArtifactFactory artifactFactory;

    /**
     * The Maven BuildPluginManager component.
     *
     * @component
     * @required
     */
    BuildPluginManager pluginManager;

    /**
     * @required
     * @component
     */
    Wizard wizard;

    /**
     * wizard for resolving artifact available versions
     */
    VersionsHelper versionsHelper;

    /**
     *
     */
    ModuleInstaller moduleInstaller;

    public AbstractTask(){}

    public AbstractTask(AbstractTask other) {
        this.mavenProject = other.mavenProject;
        this.mavenSession = other.mavenSession;
        this.wizard = other.wizard;
        this.pluginManager = other.pluginManager;
        this.artifactFactory = other.artifactFactory;
        this.artifactMetadataSource = other.artifactMetadataSource;
        this.moduleInstaller = other.moduleInstaller;
        this.versionsHelper = other.versionsHelper;
        if(versionsHelper == null){
            versionsHelper = new VersionsHelper(artifactFactory, mavenProject, mavenSession, artifactMetadataSource);
        }
        if(moduleInstaller==null){
            moduleInstaller = new ModuleInstaller(mavenProject, mavenSession, pluginManager, versionsHelper);
        }
    }
}
