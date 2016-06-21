package org.openmrs.maven.plugins;

import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.metadata.ArtifactMetadataSource;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.utils.StringUtils;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.utility.DistroHelper;
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
     * handles installing modules on server
     */
    ModuleInstaller moduleInstaller;

    /**
     * handles distro-properties
     */
    DistroHelper distroHelper;

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
        this.distroHelper = other.distroHelper;
        initUtilities();
    }

    public void initUtilities() {
        if(versionsHelper == null){
            versionsHelper = new VersionsHelper(artifactFactory, mavenProject, mavenSession, artifactMetadataSource);
        }
        if(moduleInstaller==null){
            moduleInstaller = new ModuleInstaller(mavenProject, mavenSession, pluginManager, versionsHelper);
        }
        if(distroHelper == null){
            distroHelper = new DistroHelper(mavenProject, mavenSession, pluginManager);
        }
    }
    public void setGlobalContext(String openMRSPath, String interactiveMode){
        if(StringUtils.isNotBlank(openMRSPath)){
            Server.setServersPath(openMRSPath);
        }
        if(interactiveMode.equals("false")){
            wizard.setInteractiveMode(false);
        }
    }
}
