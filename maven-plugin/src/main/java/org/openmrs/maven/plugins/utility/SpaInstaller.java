package org.openmrs.maven.plugins.utility;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

public class SpaInstaller {

    private NodeHelper nodeHelper;

    private MavenSession mavenSession;

    private MavenProject mavenProject;

    private BuildPluginManager pluginManager;

    public SpaInstaller(MavenProject mavenProject,
                        MavenSession mavenSession,
                        BuildPluginManager pluginManager) {
        this.mavenProject = mavenProject;
        this.mavenSession = mavenSession;
        this.pluginManager = pluginManager;
        this.nodeHelper = new NodeHelper(mavenProject, mavenSession, pluginManager);
    }

    public void build() throws MojoExecutionException {
        nodeHelper.runNpx("openmrs@latest build --target dist/");
    }

}
