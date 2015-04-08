package org.openmrs.sdk2.omrs;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import static org.twdata.maven.mojoexecutor.MojoExecutor.*;

/**
 * Goal which calls jetty:run.
 *
 * @goal run
 * 
 * @requiresDependencyResolution test
 * 
 * @phase process-sources
 */
public class MyMojo extends AbstractMojo {
	/**
	 * The project currently being build.
	 *
	 * @parameter expression="${project}"
	 * @required
	 */
	private MavenProject mavenProject;

	/**
	 * The current Maven session.
	 *
	 * @parameter expression="${session}"
	 * @required
	 */
	private MavenSession mavenSession;

	/**
	 * The Maven BuildPluginManager component.
	 *
	 * @component
	 * @required
	 */
	private BuildPluginManager pluginManager;

	public void execute() throws MojoExecutionException {
		executeMojo(
				plugin(groupId("org.mortbay.jetty"),
						artifactId("maven-jetty-plugin"), version("6.1.10")),
				goal("run"),
				configuration(
						element(name("webXml"),
								"/${env.OPENMRS_HOME}/webapp/target/jetty/WEB-INF/web.xml"),
						element(name("webAppConfig"),
								element(name("contextPath"), "/openmrs"),
								element(name("overrideDescriptor"),
										"/${env.OPENMRS_HOME}/webapp/src/test/resources/override-web.xml"),
								element(name("extraClasspath"),
										"/${env.OPENMRS_HOME}/webapp/target/classes;"
										+ "/${env.OPENMRS_HOME}/api/target/classes;"
										+ "/${env.OPENMRS_HOME}/web/target/classes")),
						element(name("scanIntervalSeconds"), "0")

				),
				executionEnvironment(mavenProject, mavenSession, pluginManager));
	}
}
