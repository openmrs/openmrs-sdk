package org.openmrs.maven.plugins;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.utility.SDKConstants;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import java.util.ArrayList;
import java.util.List;

import static org.twdata.maven.mojoexecutor.MojoExecutor.artifactId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.configuration;
import static org.twdata.maven.mojoexecutor.MojoExecutor.element;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executeMojo;
import static org.twdata.maven.mojoexecutor.MojoExecutor.executionEnvironment;
import static org.twdata.maven.mojoexecutor.MojoExecutor.goal;
import static org.twdata.maven.mojoexecutor.MojoExecutor.groupId;
import static org.twdata.maven.mojoexecutor.MojoExecutor.plugin;
import static org.twdata.maven.mojoexecutor.MojoExecutor.version;

@Mojo(name = "help", requiresProject = false)
public class Help extends AbstractTask {

    public void executeTask() throws MojoExecutionException {
        List<MojoExecutor.Element> configuration = new ArrayList<>(3);
        Artifact sdkArtifact = SDKConstants.getSDKInfo();
        configuration.add(element("groupId", sdkArtifact.getGroupId()));
        configuration.add(element("artifactId", sdkArtifact.getArtifactId()));
        configuration.add(element("version", sdkArtifact.getVersion()));
        configuration.add(element("detail", "true"));
        executeMojo(plugin(groupId(SDKConstants.HELP_PLUGIN_GROUP_ID), artifactId(SDKConstants.HELP_PLUGIN_ARTIFACT_ID), version(SDKConstants.HELP_PLUGIN_VERSION)), goal("describe"), configuration(configuration.toArray(new MojoExecutor.Element[0])), executionEnvironment(mavenProject, mavenSession, pluginManager));
    }
}
