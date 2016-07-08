package org.openmrs.maven.plugins.utility;

import com.atlassian.jira.rest.client.domain.Issue;

public interface Jira {
    Issue getIssue(String issueId);
}
