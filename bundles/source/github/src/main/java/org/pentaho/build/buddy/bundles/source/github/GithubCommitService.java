package org.pentaho.build.buddy.bundles.source.github;

import org.eclipse.egit.github.core.CommitStatus;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CommitService;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by bryan on 3/17/16.
 */
public class GithubCommitService extends CommitService {
    final String apiToken;

    public GithubCommitService(GitHubClient gitHubClient, String apiToken) {
        super(gitHubClient);
        this.apiToken = apiToken;
    }

    /**
     * Create status for commit SHA-1
     *
     * @param repository
     * @param sha
     * @param status
     * @return created status
     * @throws IOException
     */
    public CommitStatus createStatus(IRepositoryIdProvider repository,
                                     String sha, GithubCommitStatus status) throws IOException {
        return new Delegate(apiToken, status.getContext()).createStatus(repository, sha, status);
    }

    private static class Delegate extends CommitService {
        public Delegate(String apiToken, String context) {
            super(creatClient(apiToken, context));
        }

        private static GitHubClient creatClient(String apiToken, final String context) {
            GitHubClient gitHubClient = new GitHubClient() {
                @Override
                public <V> V post(String uri, Object paramsObj, Type type) throws IOException {
                    if (context != null) {
                        Map<String, String> params = new HashMap<>((Map) paramsObj);
                        params.put("context", context);
                        return super.post(uri, params, type);
                    } else {
                        return super.post(uri, paramsObj, type);
                    }
                }
            };
            gitHubClient.setOAuth2Token(apiToken);
            return gitHubClient;
        }
    }
}
