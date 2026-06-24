package org.openmrs.maven.plugins;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openmrs.maven.plugins.model.Artifact;
import org.openmrs.maven.plugins.model.Distribution;
import org.openmrs.maven.plugins.model.DistroProperties;
import org.openmrs.maven.plugins.model.Server;
import org.openmrs.maven.plugins.model.UpgradeDifferential;
import org.openmrs.maven.plugins.utility.ConfigurationInstaller;
import org.openmrs.maven.plugins.utility.ContentHelper;
import org.openmrs.maven.plugins.utility.Wizard;

import java.io.File;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

public class ServerUpgraderTest {

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder();

	ServerUpgrader upgrader = new ServerUpgrader(mock(AbstractServerTask.class));
	UpgradeDifferential differential = null;

	ServerUpgrader upgraderWithMocks;
	ConfigurationInstaller mockConfigInstaller;
	ContentHelper mockContentHelper;
	Wizard mockWizard;

	@Before
	public void setUpMockUpgrader() {
		Deploy parentTask = new Deploy();
		mockConfigInstaller = mock(ConfigurationInstaller.class);
		mockContentHelper = mock(ContentHelper.class);
		mockWizard = mock(Wizard.class);
		parentTask.configurationInstaller = mockConfigInstaller;
		parentTask.contentHelper = mockContentHelper;
		parentTask.wizard = mockWizard;
		upgraderWithMocks = new ServerUpgrader(parentTask);
	}

	@Test
	public void calculateUpdateDifferential_shouldCalculateArtifactsToAdd() {
		Server server = server(properties());
		Properties p = new Properties();
		p.put("war.openmrs", "2.5.9");
		p.put("omod.owa", "1.5");
		p.put("owa.openmrs-owa-sysadmin", "1.2");
		p.put("content.hiv", "1.0.0");
		p.put("config.referenceapplication", "3.0.0");
		p.put("spa.artifactId", "spaArtifactId");
		p.put("spa.frontendModules.@openmrs/esm-login-app", "5.6.0");
		Distribution distribution = distribution(p);
		differential = upgrader.calculateUpdateDifferential(server, distribution);
		assertNumChanges(differential.getWarChanges(), 1, 0, 0, 0);
		assertNumChanges(differential.getModuleChanges(), 1, 0, 0, 0);
		assertNumChanges(differential.getContentChanges(), 1, 0, 0, 0);
		assertNumChanges(differential.getConfigChanges(), 1, 0, 0, 0);
		assertNumChanges(differential.getSpaArtifactChanges(), 1, 0, 0, 0);
		assertNumChanges(differential.getSpaBuildChanges(), 1, 0, 0);
		assertModuleAdded(differential, "owa", "1.5");
	}

	@Test
	public void calculateUpdateDifferential_shouldCalculateArtifactsToRemove() {
		Properties p = new Properties();
		p.put("war.openmrs", "2.5.9");
		p.put("omod.owa", "1.5");
		p.put("owa.openmrs-owa-sysadmin", "1.2");
		p.put("content.hiv", "1.0.0");
		p.put("config.referenceapplication", "3.0.0");
		p.put("spa.artifactId", "spaArtifactId");
		p.put("spa.frontendModules.@openmrs/esm-login-app", "5.6.0");
		Server server = server(p);
		Distribution distribution = distribution(properties());
		differential = upgrader.calculateUpdateDifferential(server, distribution);
		assertNumChanges(differential.getWarChanges(), 0, 1, 0, 0);
		assertNumChanges(differential.getModuleChanges(), 0, 1, 0, 0);
		assertNumChanges(differential.getContentChanges(), 0, 1, 0, 0);
		assertNumChanges(differential.getConfigChanges(), 0, 1, 0, 0);
		assertNumChanges(differential.getSpaArtifactChanges(), 0, 1, 0, 0);
		assertNumChanges(differential.getSpaBuildChanges(), 0, 1, 0);
		assertModuleRemoved(differential, "owa", "1.5");
	}

	@Test
	public void calculateUpdateDifferential_shouldCalculateArtifactsToUpgrade() {
		Properties serverProperties = new Properties();
		serverProperties.put("war.openmrs", "1.11.5");
		serverProperties.put("omod.owa", "1.5");
		serverProperties.put("owa.openmrs-owa-sysadmin", "1.2");
		serverProperties.put("content.hiv", "1.0.0");
		serverProperties.put("config.referenceapplication", "3.0.0");
		serverProperties.put("spa.artifactId", "refapp");
		serverProperties.put("spa.groupId", "org.openmrs.frontend");
		serverProperties.put("spa.version", "1.0.0");
		serverProperties.put("spa.frontendModules.@openmrs/esm-login-app", "5.6.0");
		Server server = server(serverProperties);
		Properties distroProperties = new Properties();
		distroProperties.put("war.openmrs", "2.5.9");
		distroProperties.put("omod.owa", "1.6");
		distroProperties.put("owa.openmrs-owa-sysadmin", "2.0");
		distroProperties.put("content.hiv", "1.1.0");
		distroProperties.put("config.referenceapplication", "3.1.0");
		distroProperties.put("spa.artifactId", "refapp");
		distroProperties.put("spa.groupId", "org.openmrs.frontend");
		distroProperties.put("spa.version", "1.1.0");
		distroProperties.put("spa.frontendModules.@openmrs/esm-login-app", "5.7.0");
		Distribution distribution = distribution(distroProperties);
		differential = upgrader.calculateUpdateDifferential(server, distribution);
		assertNumChanges(differential.getWarChanges(), 0, 0, 1, 0);
		assertNumChanges(differential.getModuleChanges(), 0, 0, 1, 0);
		assertNumChanges(differential.getContentChanges(), 0, 0, 1, 0);
		assertNumChanges(differential.getConfigChanges(), 0, 0, 1, 0);
		assertNumChanges(differential.getSpaArtifactChanges(), 0, 0, 1, 0);
		assertNumChanges(differential.getSpaBuildChanges(), 0, 0, 1);
		assertModuleUpgraded(differential, "owa", "1.5", "1.6");
	}

	@Test
	public void calculateUpdateDifferential_shouldCalculateArtifactsToDowngrade() {
		Properties serverProperties = new Properties();
		serverProperties.put("war.openmrs", "2.5.9");
		serverProperties.put("omod.owa", "1.6");
		serverProperties.put("owa.openmrs-owa-sysadmin", "2.0");
		serverProperties.put("content.hiv", "1.1.0");
		serverProperties.put("config.referenceapplication", "3.1.0");
		serverProperties.put("spa.artifactId", "refapp");
		serverProperties.put("spa.groupId", "org.openmrs.frontend");
		serverProperties.put("spa.version", "1.1.0");
		serverProperties.put("spa.frontendModules.@openmrs/esm-login-app", "5.7.0");
		Server server = server(serverProperties);
		Properties distroProperties = new Properties();
		distroProperties.put("war.openmrs", "1.11.5");
		distroProperties.put("omod.owa", "1.5");
		distroProperties.put("owa.openmrs-owa-sysadmin", "1.2");
		distroProperties.put("content.hiv", "1.0.0");
		distroProperties.put("config.referenceapplication", "3.0.0");
		distroProperties.put("spa.artifactId", "refapp");
		distroProperties.put("spa.groupId", "org.openmrs.frontend");
		distroProperties.put("spa.version", "1.0.0");
		distroProperties.put("spa.frontendModules.@openmrs/esm-login-app", "5.6.0");
		Distribution distribution = distribution(distroProperties);
		differential = upgrader.calculateUpdateDifferential(server, distribution);
		assertNumChanges(differential.getWarChanges(), 0, 0, 0, 1);
		assertNumChanges(differential.getModuleChanges(), 0, 0, 0, 1);
		assertNumChanges(differential.getContentChanges(), 0, 0, 0, 1);
		assertNumChanges(differential.getConfigChanges(), 0, 0, 0, 1);
		assertNumChanges(differential.getSpaArtifactChanges(), 0, 0, 0, 1);
		assertNumChanges(differential.getSpaBuildChanges(), 0, 0, 1);
		assertModuleDowngraded(differential, "owa", "1.6", "1.5");
	}

	@Test
	public void calculateUpdateDifferential_shouldNotUpgradeIfNoChanges() {
		Properties serverProperties = new Properties();
		serverProperties.put("war.openmrs", "2.5.9");
		serverProperties.put("omod.owa", "1.6");
		serverProperties.put("owa.openmrs-owa-sysadmin", "2.0");
		serverProperties.put("content.hiv", "1.1.0");
		serverProperties.put("config.referenceapplication", "3.1.0");
		serverProperties.put("spa.artifactId", "refapp");
		serverProperties.put("spa.groupId", "org.openmrs.frontend");
		serverProperties.put("spa.version", "1.1.0");
		serverProperties.put("spa.frontendModules.@openmrs/esm-login-app", "5.7.0");
		Server server = server(serverProperties);
		Properties distroProperties = new Properties();
		distroProperties.putAll(serverProperties);
		Distribution distribution = distribution(distroProperties);
		differential = upgrader.calculateUpdateDifferential(server, distribution);
		assertNumChanges(differential.getWarChanges(), 0, 0, 0, 0);
		assertNumChanges(differential.getModuleChanges(), 0, 0, 0, 0);
		assertNumChanges(differential.getContentChanges(), 0, 0, 0, 0);
		assertNumChanges(differential.getConfigChanges(), 0, 0, 0, 0);
		assertNumChanges(differential.getSpaArtifactChanges(), 0, 0, 0, 0);
		assertNumChanges(differential.getSpaBuildChanges(), 0, 0, 0);
	}

	@Test
	public void calculateUpdateDifferential_shouldUpgradeSnapshots() {
		Properties serverProperties = new Properties();
		serverProperties.put("war.openmrs", "2.5.9-SNAPSHOT");
		serverProperties.put("omod.owa", "1.6-SNAPSHOT");
		serverProperties.put("owa.openmrs-owa-sysadmin", "2.0-SNAPSHOT");
		serverProperties.put("content.hiv", "1.1.0-SNAPSHOT");
		serverProperties.put("config.referenceapplication", "3.1.0-SNAPSHOT");
		serverProperties.put("spa.artifactId", "refapp");
		serverProperties.put("spa.groupId", "org.openmrs.frontend");
		serverProperties.put("spa.version", "1.1.0-SNAPSHOT");
		serverProperties.put("spa.frontendModules.@openmrs/esm-login-app", "next");
		Server server = server(serverProperties);
		Properties distroProperties = new Properties();
		distroProperties.putAll(serverProperties);
		Distribution distribution = distribution(distroProperties);
		differential = upgrader.calculateUpdateDifferential(server, distribution);
		assertNumChanges(differential.getWarChanges(), 0, 0, 1, 0);
		assertNumChanges(differential.getModuleChanges(), 0, 0, 1, 0);
		assertNumChanges(differential.getContentChanges(), 0, 0, 1, 0);
		assertNumChanges(differential.getConfigChanges(), 0, 0, 1, 0);
		assertNumChanges(differential.getSpaArtifactChanges(), 0, 0, 1, 0);
		assertNumChanges(differential.getSpaBuildChanges(), 0, 0, 1);
	}

	public void assertNumChanges(UpgradeDifferential.ArtifactChanges artifactChanges, int added, int removed, int upgraded, int downgraded) {
		assertThat(artifactChanges.getAddedArtifacts(), hasSize(added));
		assertThat(artifactChanges.getRemovedArtifacts(), hasSize(removed));
		assertThat(artifactChanges.getUpgradedArtifacts().keySet(), hasSize(upgraded));
		assertThat(artifactChanges.getDowngradedArtifacts().keySet(), hasSize(downgraded));
		assertThat(artifactChanges.hasChanges(), equalTo((added + removed + upgraded + downgraded) > 0));
	}

	public void assertNumChanges(UpgradeDifferential.PropertyChanges propertyChanges, int added, int removed, int changed) {
		assertThat(propertyChanges.getAddedProperties().keySet(), hasSize(added));
		assertThat(propertyChanges.getRemovedProperties().keySet(), hasSize(removed));
		assertThat(propertyChanges.getChangedProperties().keySet(), hasSize(changed));
		assertThat(propertyChanges.hasChanges(), equalTo((added + removed + changed) > 0));
	}

	public void assertModuleAdded(UpgradeDifferential differential, String moduleId, String version) {
		assertThat(differential.getModuleChanges().getAddedArtifacts(), hasItem(new Artifact(moduleId + "-omod", version)));
	}

	public void assertModuleRemoved(UpgradeDifferential differential, String moduleId, String version) {
		assertThat(differential.getModuleChanges().getRemovedArtifacts(), hasItem(new Artifact(moduleId + "-omod", version)));
	}

	public void assertModuleUpgraded(UpgradeDifferential differential, String moduleId, String fromVersion, String toVersion) {
		assertThat(differential.getModuleChanges().getUpgradedArtifacts().keySet(), hasItem(new Artifact(moduleId + "-omod", fromVersion)));
		assertThat(differential.getModuleChanges().getUpgradedArtifacts().values(), hasItem(new Artifact(moduleId + "-omod", toVersion)));
	}

	public void assertModuleDowngraded(UpgradeDifferential differential, String moduleId, String fromVersion, String toVersion) {
		assertThat(differential.getModuleChanges().getDowngradedArtifacts().keySet(), hasItem(new Artifact(moduleId + "-omod", fromVersion)));
		assertThat(differential.getModuleChanges().getDowngradedArtifacts().values(), hasItem(new Artifact(moduleId + "-omod", toVersion)));
	}
	
	@Test
	public void updateConfigAndContent_shouldInstallConfigAndContentWhenDifferentialShowsChanges() throws Exception {
		Properties serverProps = properties(
				"config.old", "1.0.0",
				"content.old", "1.0.0"
		);
		Server server = serverWithDir(serverProps);

		Properties distroProps = properties(
				"config.referenceapplication", "3.0.0",
				"config.referenceapplication.groupId", "org.openmrs.distro",
				"content.hiv", "1.0.0",
				"content.hiv.groupId", "org.openmrs.content"
		);
		DistroProperties distroProperties = new DistroProperties(distroProps);
		UpgradeDifferential diff = upgraderWithMocks.calculateUpdateDifferential(server, distribution(distroProps));

		upgraderWithMocks.updateConfigAndContent(server, distroProperties, diff);

		verify(mockConfigInstaller).installToServer(eq(server), eq(distroProperties));
		verify(mockContentHelper).installBackendConfig(eq(distroProperties), any(File.class));
		assertThat(server.getConfigArtifacts(), hasSize(1));
		assertThat(server.getContentPackageArtifacts(), hasSize(1));
	}

	@Test
	public void updateConfigAndContent_shouldSkipInstallWhenDifferentialShowsNoChanges() throws Exception {
		Properties sameProps = properties(
				"config.referenceapplication", "3.0.0",
				"config.referenceapplication.groupId", "org.openmrs.distro",
				"content.hiv", "1.0.0",
				"content.hiv.groupId", "org.openmrs.content"
		);
		Server server = server(sameProps);
		DistroProperties distroProperties = new DistroProperties(sameProps);
		UpgradeDifferential diff = upgraderWithMocks.calculateUpdateDifferential(server, distribution(sameProps));

		upgraderWithMocks.updateConfigAndContent(server, distroProperties, diff);

		verifyNoInteractions(mockConfigInstaller);
		verifyNoInteractions(mockContentHelper);
	}

	Server serverWithDir(Properties properties) throws Exception {
		File dir = tempFolder.newFolder();
		new File(dir, DistroProperties.DISTRO_FILE_NAME).createNewFile();
		return new Server(dir, properties);
	}

	Distribution distribution(Properties properties) {
		Distribution distribution = new Distribution();
		distribution.setEffectiveProperties(new DistroProperties(properties));
		return distribution;
	}

	Server server(Properties properties) {
		return new Server(null, properties);
	}
	
	Properties properties(String...keyValuePairs) {
		Properties properties = new Properties();
		for (int i=0; i<keyValuePairs.length; i+=2) {
			properties.setProperty(keyValuePairs[i], keyValuePairs[i+1]);
		}
		return properties;
	}
}
