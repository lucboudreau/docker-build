package org.pentaho.build.buddy.bundles.source.github;

import org.eclipse.egit.github.core.CommitStatus;

/**
 * Created by bryan on 3/17/16.
 */
public class GithubCommitStatus extends CommitStatus {
    private String context;

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}
