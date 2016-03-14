package org.pentaho.build.buddy.bundles.source.github;

import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.eclipse.egit.github.core.service.RepositoryService;

import java.io.IOException;
import java.util.Map;

import static org.pentaho.build.buddy.bundles.util.config.MapUtil.getStringOrThrow;

/**
 * Created by bryan on 3/11/16.
 */
public class GithubConfigData {
    public static final String ORGANIZATION = "Organization";
    public static final String REPOSITORY = "Repository";
    public static final String API_TOKEN = "ApiToken";
    public static final String PULL_REQUEST = "PullRequest";
    private final String apiToken;
    private final String organization;
    private final String repositoryName;
    private final String pullRequestNumber;
    private final GitHubClient gitHubClient;
    private final Repository repository;
    private final PullRequest pullRequest;

    public GithubConfigData(Map config) throws IOException {
        apiToken = getStringOrThrow(config, API_TOKEN);
        organization = getStringOrThrow(config, ORGANIZATION);
        repositoryName = getStringOrThrow(config, REPOSITORY);
        pullRequestNumber = getStringOrThrow(config, PULL_REQUEST);
        int id = Integer.parseInt(pullRequestNumber);

        gitHubClient = new GitHubClient();
        gitHubClient.setOAuth2Token(apiToken);

        RepositoryService repositoryService = new RepositoryService(gitHubClient);
        repository = repositoryService.getRepository(organization, repositoryName);

        PullRequestService pullRequestService = new PullRequestService(gitHubClient);
        pullRequest = pullRequestService.getPullRequest(repository, id);
    }

    public String getApiToken() {
        return apiToken;
    }

    public String getOrganization() {
        return organization;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public String getPullRequestNumber() {
        return pullRequestNumber;
    }

    public GitHubClient getGitHubClient() {
        return gitHubClient;
    }

    public Repository getRepository() {
        return repository;
    }

    public PullRequest getPullRequest() {
        return pullRequest;
    }
}
