package org.openmrs.maven.plugins;

import com.atlassian.jira.rest.client.domain.Issue;
import com.google.inject.internal.util.Lists;
import org.apache.maven.model.Scm;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.AdditionalAnswers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.maven.plugins.git.GitHelper;
import org.openmrs.maven.plugins.git.GithubPrRequest;
import org.openmrs.maven.plugins.utility.Jira;
import org.openmrs.maven.plugins.utility.Wizard;

import java.io.File;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Strict.class)
public class PullRequestTest {

    PullRequest pullRequest;

    @Mock
    GitHelper gitHelper;

    @Mock
    Jira jira;

    @Mock
    Wizard wizard;

    @Mock
    Issue issue;

    @Mock
    Repository repository;

    @Mock
    MavenProject mavenProject;

    @Mock
    Scm scm;

    RevCommit commit1;
    RevCommit commit2;
    private final String issueId ="SDK-100";
    private final String username = "test_user";
    private final String password= "test_password";
    private final String reponame= "test_reponame";
    private org.eclipse.egit.github.core.PullRequest mockPR;

    /**
     * basic setup, 2 commits, one with invalid message, base for mocking configuration of other tests
     * @throws Exception
     */
    @Before
    public void setup() throws Exception{
        pullRequest = new PullRequest();

        //mock maven project
        pullRequest.mavenProject = mavenProject;
        when(mavenProject.getBasedir()).thenReturn(new File("false/path"));
        when(mavenProject.getScm()).thenReturn(scm);
        when(scm.getUrl()).thenReturn("openmrs/"+ reponame);

        //mock git integration
        pullRequest.gitHelper = gitHelper;
        when(gitHelper.getLocalRepository(anyString())).thenReturn(repository);
        when(repository.getBranch()).thenReturn("master");
        //doReturn syntax here to avoid org.mockito.exceptions.misusing.WrongTypeOfReturnValue exceptions
        doReturn(false).when(gitHelper).checkIfUncommitedChanges(any(Git.class));
        doReturn(Lists.newArrayList(commit1, commit2)).when(gitHelper)
                .getCommitDifferential(any(Git.class), anyString(), anyString());
        doReturn(Lists.newArrayList("test", issueId +" test")).when(gitHelper)
                .getCommitDifferentialMessages(any(Git.class), anyString(), anyString());

        mockPR = new org.eclipse.egit.github.core.PullRequest();
        mockPR.setHtmlUrl("http://github/openmrs/fake_repo");
        doReturn(mockPR).when(gitHelper).openPullRequest(any(GithubPrRequest.class));

        //mock wizard
        pullRequest.wizard = wizard;
        when(wizard.promptForValueIfMissing(anyString(), anyString())).then(AdditionalAnswers.returnsFirstArg());
        when(wizard.promptForPasswordIfMissing(anyString(), anyString())).then(AdditionalAnswers.returnsFirstArg());
        when(wizard.promptYesNo(anyString())).thenReturn(true);

        //mock jira integration
        when(issue.getSummary()).thenReturn("TEST ISSUE");
        when(issue.getKey()).thenReturn(issueId);
        pullRequest.jira = jira;
        when(jira.getIssue(issueId)).thenReturn(issue);

        pullRequest.issueId = issueId;
        pullRequest.username = username;
        pullRequest.personalAccessToken = password;
        pullRequest.branch = "master";
    }

    @Test
    public void should_workWithoutErrors() throws Exception {
        //basic setup, just check if helper methods called
        pullRequest.executeTask();
        verify(gitHelper, atLeastOnce()).addIssueIdIfMissing(any(Git.class), eq(issueId), eq(2));
        verify(gitHelper, atLeastOnce()).squashLastCommits(any(Git.class), eq(2));
        verify(gitHelper, atLeastOnce()).push(any(Git.class), eq(username), eq(password), anyString(), anyString(), eq(true));
        verify(jira, atLeastOnce()).getIssue(issueId);
    }

    @Test(expected = MojoExecutionException.class)
    public void should_throwIfNoIssue() throws Exception {
        when(jira.getIssue(issueId)).thenReturn(null);
        pullRequest.executeTask();
    }

    @Test(expected = MojoExecutionException.class)
    public void should_throwIfUncommitedChanges() throws Exception {
        doReturn(true).when(gitHelper).checkIfUncommitedChanges(any(Git.class));
        pullRequest.executeTask();
    }

    @Test
    public void should_notSquashIfOneCommit() throws Exception {
        doReturn(Lists.newArrayList(commit1)).when(gitHelper)
                .getCommitDifferential(any(Git.class), anyString(), anyString());
        pullRequest.executeTask();
        verify(gitHelper, never()).squashLastCommits(any(Git.class), anyInt());
    }

    @Test
    public void should_notOpenPrIfAlreadyExists() throws Exception {
        when(gitHelper.getPullRequestIfExists(anyString(), anyString(), anyString())).thenReturn(mockPR);
        pullRequest.executeTask();
        verify(gitHelper, never()).openPullRequest(any(GithubPrRequest.class));
    }

    @Test
    public void should_buildProperPullRequest() throws Exception {
        GithubPrRequest pr = new GithubPrRequest.Builder()
                .setBase("master")
                .setHead(username+":"+issueId)
                .setUsername(username)
                .setPassword(password)
                .setDescription("https://issues.openmrs.org/browse/"+ issueId +"\n\n"+"null")
                .setTitle("SDK-100 TEST ISSUE")
                .setRepository(reponame)
                .build();

        pullRequest.executeTask();
        verify(gitHelper).openPullRequest(eq(pr));
    }
}
