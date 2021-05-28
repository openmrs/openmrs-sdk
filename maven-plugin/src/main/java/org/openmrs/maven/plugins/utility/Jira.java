package org.openmrs.maven.plugins.utility;

import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.JiraRestClientFactory;
import com.atlassian.jira.rest.client.auth.AnonymousAuthenticationHandler;
import com.atlassian.jira.rest.client.domain.Issue;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;

import java.net.URI;
import java.net.URISyntaxException;

public class Jira {

    private static final String OPENMRS_ISSUES_URI = "https://issues.openmrs.org";

    public Issue getIssue(String issueId) {
        JiraRestClientFactory factory = new AsynchronousJiraRestClientFactory();
        URI jiraServerUri = null;
        try {
            jiraServerUri = new URI(OPENMRS_ISSUES_URI);
        } catch (URISyntaxException e) {
            throw new RuntimeException(OPENMRS_ISSUES_URI+" is invalid URI", e);
        }
        JiraRestClient client = factory.create(jiraServerUri, new AnonymousAuthenticationHandler());
        return client.getIssueClient().getIssue(issueId).claim();
    }

    public String getJiraUrl() {
        return OPENMRS_ISSUES_URI;
    }
}
