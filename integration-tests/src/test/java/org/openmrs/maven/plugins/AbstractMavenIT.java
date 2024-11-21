package org.openmrs.maven.plugins;


import lombok.Getter;
import lombok.Setter;
import org.junit.Before;
import org.openmrs.maven.plugins.utility.MavenEnvironment;

import java.io.File;

@Getter @Setter
public abstract class AbstractMavenIT extends AbstractSdkIT {

	MavenEnvironment mavenEnvironment = null;

	@Override
	@Before
	public void setup() throws Exception {
		super.setup();
		mavenEnvironment = null;
	}

	@Override
	protected void addTestResources() throws Exception {
		includePomFile("invokeIT", "pom.xml");
	}

	protected void executeTest(MavenTestFunction testFunction) throws Exception {
		StackTraceElement invoker = Thread.currentThread().getStackTrace()[2];
		String className = invoker.getClassName();
		String testMethod = invoker.getMethodName();
		if (mavenEnvironment == null) {
			addTaskParam("className", className);
			addTaskParam("methodName", testMethod);
			addTaskParam(BATCH_ANSWERS, getAnswers());
			addTaskParam("testMode", "true");
			String plugin = resolveSdkArtifact();
			verifier.executeGoal(plugin + ":" + InvokeMethod.NAME);
		}
		else {
			testFunction.executeTest();
		}
	}

	protected File getMavenTestDirectory() {
		return new File(mavenEnvironment.getMavenProject().getBuild().getDirectory());
	}

	/**
	 * Simple interface that encapsulates a test that should be evaluated by tests that use this Mojo
	 */
	public interface MavenTestFunction {
		void executeTest() throws Exception;
	}
}
