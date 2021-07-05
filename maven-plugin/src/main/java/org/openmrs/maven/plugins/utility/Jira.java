package org.openmrs.maven.plugins.utility;

import com.atlassian.jira.rest.client.api.domain.Issue;
import org.apache.maven.plugin.MojoExecutionException;

public interface Jira {

	Issue getIssue(String issueId) throws MojoExecutionException;

	String getJiraUrl();
}
