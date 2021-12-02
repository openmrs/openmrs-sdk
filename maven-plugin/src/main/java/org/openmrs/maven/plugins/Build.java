package org.openmrs.maven.plugins;

import org.apache.commons.lang.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.model.Project;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

@Mojo(name = "build", requiresProject = false)
public class Build extends AbstractServerTask {

	@Parameter(property = "buildOwa", defaultValue = "true")
	private boolean buildOwa;

	@Parameter(property = "npmVersion")
	protected String npmVersion;

	@Parameter(property = "nodeVersion")
	protected String nodeVersion;

	public Build() {
	}

	public Build(AbstractServerTask other) {
		super(other);
	}

	public Build(AbstractTask other) {
		super(other);
	}

	public Build(AbstractServerTask other, String serverId) {
		super(other);
		this.serverId = serverId;
	}

	@Override
	public void executeTask() throws MojoExecutionException, MojoFailureException {
		boolean projectDetected = false;
		boolean buildExecuted = false;

		//if user specified serverId, omit checking directory for projects
		if (StringUtils.isBlank(serverId)) {
			//check if there's owa project in current dir
			File configFile = new File(mavenProject.getBasedir(), "webpack.config.js");

			if (configFile.exists() && buildOwa) {
				projectDetected = true;
				buildNpmProject();
				buildExecuted = true;
			} else {
				//check if there's maven project in current dir
				File userDir = new File(System.getProperty("user.dir"));
				if (Project.hasProject(userDir)) {
					Project project = Project.loadProject(userDir);
					String artifactId = project.getArtifactId();
					String groupId = project.getGroupId();
					String version = project.getVersion();
					if ((artifactId != null) && (groupId != null) && version != null) {
						projectDetected = true;
						boolean buildMavenProject = wizard.promptYesNo(String.format(
								"Maven artifact %s:%s:%s detected in this directory, would you like to build it?",
								groupId, artifactId, version)
						);
						if (buildMavenProject) {
							try {
								buildProject(project);
								buildExecuted = true;
							}
							catch (Exception e) {
								throw new MojoExecutionException("Failed to build project", e);
							}
						}
					}
				}
			}
		}

		//no project found, start default workflow
		if (!projectDetected) {
			buildWatchedProjects();
		}
		//found owa or maven project, but didn't build
		else if (!buildExecuted) {
			boolean buildWatched = wizard.promptYesNo("Do you want to build all watched projects instead?");
			if (buildWatched) {
				buildWatchedProjects();
			} else {
				wizard.showMessage("Task aborted");
			}
		}
		//otherwise just finish
	}

	private void buildWatchedProjects() throws MojoExecutionException, MojoFailureException {
		Server server = getServer();

		if (!server.hasWatchedProjects()) {
			wizard.showMessage("There are no watched projects for " + serverId + " server.");
			return;
		}

		buildCoreIfWatched(server);
		for (Project project : server.getWatchedProjectsToBuild()) {
			buildProject(project);
		}

		deployWatchedProjects(server);
	}

	private void buildCoreIfWatched(Server server) throws MojoExecutionException, MojoFailureException {
		for (Project project : server.getWatchedProjects()) {
			if (project.isOpenmrsCore()) {
				buildProject(project);
				return;
			}
		}
	}

	protected void buildNpmProject() throws MojoExecutionException {
		wizard.showMessage("Building NPM project...");

		boolean useSystemNode = owaHelper.resolveNodeAndNpm(nodeVersion, npmVersion, null);

		owaHelper.installNodeModules(useSystemNode);

		runNpmBuild(useSystemNode);
	}

	private void runNpmBuild(boolean useSystemNode) throws MojoExecutionException {
		List<String> args = Arrays.asList("run", "build");

		if (useSystemNode) {
			owaHelper.runSystemNpmCommandWithArgs(args);
		} else {
			owaHelper.runLocalNpmCommandWithArgs(args);
		}
	}

	/**
	 * Deploy all watched modules to server
	 *
	 * @param server
	 * @throws MojoExecutionException
	 */
	private void deployWatchedProjects(Server server) throws MojoExecutionException {
		Set<Project> watchedProject = server.getWatchedProjects();
		for (Project module : watchedProject) {
			Project project = Project.loadProject(new File(module.getPath()));
			if (project.isOpenmrsModule()) {
				new Deploy(this).deployModule(project.getGroupId(), project.getArtifactId(), project.getVersion(), server);
			} else if (project.isOpenmrsCore()) {
				new ServerUpgrader(this).upgradePlatform(server, project.getVersion());
			}
		}
	}

	/**
	 * Run "mvn clean install -DskipTests" command in the given directory
	 */
	public void buildProject(Project project) throws MojoExecutionException, MojoFailureException {
		Properties properties = new Properties();
		properties.put("skipTests", "true");

		InvocationRequest request = new DefaultInvocationRequest();
		request.setGoals(Collections.singletonList("clean install"))
				.setProperties(properties)
				.setShowErrors(mavenSession.getRequest().isShowErrors())
				.setOffline(mavenSession.getRequest().isOffline())
				.setLocalRepositoryDirectory(mavenSession.getRequest().getLocalRepositoryPath())
				.setUpdateSnapshots(mavenSession.getRequest().isUpdateSnapshots())
				.setShowVersion(true)
				.setBaseDirectory(new File(project.getPath()));

		Invoker invoker = new DefaultInvoker();
		InvocationResult result;
		try {
			result = invoker.execute(request);
		}
		catch (MavenInvocationException e) {
			throw new MojoExecutionException("Failed to build project in directory: " + project.getPath(), e);
		}

		if (result.getExitCode() != 0) {
			throw new MojoFailureException("Failed building project in " + project.getPath(),
					result.getExecutionException());
		}
	}
}
