package org.openmrs.maven.plugins.utility;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.openmrs.maven.plugins.model.Artifact;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * Class for reading pom.xml
 */
public class Project {
	
	private Model model;
	
	public Project(String groupId, String artifactId, String version, String path) {
		model = new Model();
		model.setGroupId(groupId);
		model.setArtifactId(artifactId);
		model.setVersion(version);
		if (path != null) {
			model.setPomFile(new File(path, "pom.xml"));
		}
	}
	
	private Project(Model model) {
		this.model = model;
	}
	
	public static boolean hasProject(File dir) {
		File pom = new File(dir, "pom.xml");
		return pom.exists();
	}
	
	public static Project loadProject(File dir) throws MojoExecutionException {
		if (!hasProject(dir)) {
			throw new IllegalArgumentException("Project at " + dir.getAbsolutePath() + " does not exist");
		}
		
		File pom = new File(dir, "pom.xml");
		FileReader reader = null;
		try {
			reader = new FileReader(pom);
			Model model = new MavenXpp3Reader().read(reader);
			reader.close();
			model.setPomFile(pom);
			return new Project(model);
		}
		catch (IOException e) {
			throw new MojoExecutionException(e.getMessage());
		}
		catch (XmlPullParserException e) {
			throw new MojoExecutionException(e.getMessage());
		}
		finally {
			IOUtils.closeQuietly(reader);
		}
	}
	
	public String getPath() {
		if (model.getPomFile() != null) {
			return model.getPomFile().getParentFile().getAbsolutePath();
		} else {
			return null;
		}
	}
	
	/**
	 * Get parent property
	 * 
	 * @return
	 */
	public Parent getParent() {
		return model.getParent();
	}


	
	/**
	 * Get artifactId
	 * 
	 * @return
	 */
	public String getArtifactId() {
		return model.getArtifactId();
	}
	
	/**
	 * Get groupId
	 * 
	 * @return
	 */
	public String getGroupId() {
		return model.getGroupId();
	}
	
	/**
	 * Get version
	 * 
	 * @return
	 */
	public String getVersion() {
		return model.getVersion();
	}
	
	public boolean matches(Project project) {
		EqualsBuilder eq = new EqualsBuilder();
		if (project.getGroupId() != null) {
			eq.append(getGroupId(), project.getGroupId());
		}
		if (project.getArtifactId() != null) {
			eq.append(getArtifactId(), project.getArtifactId());
		}
		if (project.getVersion() != null) {
			eq.append(getVersion(), project.getVersion());
		}
		if (project.getPath() != null) {
			eq.append(getPath(), project.getPath());
		}
		return eq.isEquals();
	}
	
	/**
	 * Return true if current pom related to omod
	 * 
	 * @return
	 */
	public boolean isOpenmrsModule() {
		return Artifact.GROUP_MODULE.equals(model.getGroupId());
	}
	
	@Override
	public String toString() {
		return new ToStringBuilder(this).append("groupId", getGroupId()).append("artifactId", getArtifactId())
		        .append("version", getVersion()).append("path", getPath()).toString();
	}
}
