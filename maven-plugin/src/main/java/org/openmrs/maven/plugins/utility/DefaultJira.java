package org.openmrs.maven.plugins.utility;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClientFactory;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.auth.AnonymousAuthenticationHandler;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import org.apache.maven.plugin.MojoExecutionException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class DefaultJira implements Jira {

    private static final String OPENMRS_ISSUES_URI = "https://issues.openmrs.org";

    @Override
    public Issue getIssue(String issueId) throws MojoExecutionException {
        JiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
        URI jiraServerUri;
        try {
            jiraServerUri = new URI(OPENMRS_ISSUES_URI);
        } catch (URISyntaxException e) {
            throw new MojoExecutionException(OPENMRS_ISSUES_URI + " is not a valid URI " + e.getMessage(), e);
        }
        
        try (JiraRestClient client = factory.create(jiraServerUri, new AnonymousAuthenticationHandler())) {
            return client.getIssueClient().getIssue(issueId).claim();
        }
        catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }

    @Override
    public String getJiraUrl() {
        return OPENMRS_ISSUES_URI;
    }
}
