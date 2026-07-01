package org.openmrs.maven.plugins.utility;

import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Test;
import org.openmrs.maven.plugins.model.Artifact;

import java.io.File;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

public class ArtifactHelperTest {

	@Test
	public void verifyFilesArtifactsMapsCoordinates() {
		Artifact artifact = new Artifact("xforms-omod", "5.0.0", Artifact.GROUP_MODULE, "jar");
		File file = new File("/tmp/xforms-omod-5.0.0.jar");

		Xpp3Dom artifactDom = artifactItem(artifact, file);

		assertThat(artifactDom.getChild("file").getValue(), equalTo(file.getAbsolutePath()));
		assertThat(artifactDom.getChild("groupId").getValue(), equalTo(Artifact.GROUP_MODULE));
		assertThat(artifactDom.getChild("artifactId").getValue(), equalTo("xforms-omod"));
		assertThat(artifactDom.getChild("version").getValue(), equalTo("5.0.0"));
		assertThat(artifactDom.getChild("type").getValue(), equalTo("jar"));
		assertThat(artifactDom.getChild("classifier"), nullValue());
	}

	@Test
	public void verifyFilesArtifactsIncludesClassifierWhenPresent() {
		Artifact artifact = new Artifact("openmrs-webapp", "2.6.0", Artifact.GROUP_WEB, "jar");
		artifact.setClassifier("classes");

		Xpp3Dom artifactDom = artifactItem(artifact, new File("/tmp/openmrs-webapp-2.6.0.jar"));

		assertThat(artifactDom.getChild("classifier").getValue(), equalTo("classes"));
	}

	@Test
	public void verifyFilesArtifactsWrapsEachArtifactByDestFileName() {
		Artifact a = new Artifact("uiframework-omod", "3.6", Artifact.GROUP_MODULE, "jar");
		Artifact b = new Artifact("uicommons-omod", "1.7", Artifact.GROUP_MODULE, "jar");
		File dir = new File("/tmp/modules");

		Xpp3Dom artifacts = ArtifactHelper.verifyFilesArtifacts(java.util.Arrays.asList(a, b), dir).toDom();

		assertThat(artifacts.getName(), equalTo("artifacts"));
		assertThat(artifacts.getChildren("artifact").length, equalTo(2));
		assertThat(artifacts.getChildren("artifact")[0].getChild("file").getValue(),
				equalTo(new File(dir, a.getDestFileName()).getAbsolutePath()));
	}

	@Test
	public void isOpenmrsArtifactMatchesOpenmrsGroupsOnly() {
		assertThat(ArtifactHelper.isOpenmrsArtifact(new Artifact("openmrs-webapp", "2.6.0", Artifact.GROUP_OPENMRS)), equalTo(true));
		assertThat(ArtifactHelper.isOpenmrsArtifact(new Artifact("xforms-omod", "5.0.0", Artifact.GROUP_MODULE)), equalTo(true));
		assertThat(ArtifactHelper.isOpenmrsArtifact(new Artifact("h2", "2.1.214", Artifact.GROUP_H2)), equalTo(false));
		assertThat(ArtifactHelper.isOpenmrsArtifact(new Artifact("evil", "1.0", "org.openmrsfoo")), equalTo(false));
	}

	private Xpp3Dom artifactItem(Artifact artifact, File file) {
		return ArtifactHelper.artifactItem(artifact, file).toDom();
	}
}