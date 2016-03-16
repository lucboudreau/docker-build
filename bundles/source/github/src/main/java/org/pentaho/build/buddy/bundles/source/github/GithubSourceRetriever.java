package org.pentaho.build.buddy.bundles.source.github;

import org.pentaho.build.buddy.bundles.api.orchestrator.OrchestrationResult;
import org.pentaho.build.buddy.bundles.api.result.LineHandler;
import org.pentaho.build.buddy.bundles.api.source.SourceRetrievalResult;
import org.pentaho.build.buddy.bundles.api.source.SourceRetriever;
import org.pentaho.build.buddy.bundles.api.status.StatusUpdater;
import org.pentaho.build.buddy.util.shell.LoggingLineHandler;
import org.pentaho.build.buddy.util.shell.ShellUtil;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Level;
import org.eclipse.egit.github.core.*;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.IssueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.pentaho.build.buddy.bundles.util.config.MapUtil.getStringOrNull;

/**
 * Created by bryan on 2/17/16.
 */
public class GithubSourceRetriever implements SourceRetriever, StatusUpdater {
    public static final String GITHUB = "github";
    public static final String BRANCH = "Branch";
    public static final String WINGMAN_URL = "WingmanUrl";
    private static final Logger logger = LoggerFactory.getLogger(GithubSourceRetriever.class);
    private final ShellUtil shellUtil;
    private final File cloneDir;

    public GithubSourceRetriever() {
        this(new ShellUtil(), new File(new File(System.getProperty("karaf.home")), "source"));
    }

    public GithubSourceRetriever(ShellUtil shellUtil, File cloneDir) {
        this.shellUtil = shellUtil;
        this.cloneDir = cloneDir;
        cloneDir.mkdirs();
    }

    private static String getStringOrThrow(Map config, String field) throws IOException {
        Object o = config.get(field);
        if (o == null) {
            throw new IOException(field + " cannot be null");
        }
        return o.toString();
    }

    @Override
    public SourceRetrievalResult retrieveSource(Map config, LineHandler stdoutLineHandler, LineHandler stderrLineHandler) throws IOException {
        GithubConfigData githubConfigData = new GithubConfigData(config);

        PullRequest pullRequest = githubConfigData.getPullRequest();
        PullRequestMarker base = pullRequest.getBase();
        PullRequestMarker head = pullRequest.getHead();

        config.put(BRANCH, base.getRef());

        Repository repository = githubConfigData.getRepository();
        CommitService commitService = new CommitService(githubConfigData.getGitHubClient());
        final RepositoryCommitCompare compareInfo = commitService.compare(repository, base.getSha(), head.getSha());
        RepositoryCommitCompare reverseCompareInfo = commitService.compare(repository, head.getSha(), base.getSha());

        String repositoryName = githubConfigData.getRepositoryName();
        String id = githubConfigData.getPullRequestNumber();
        String baseName = repositoryName + "-base-" + id + "-" + UUID.randomUUID().toString();
        String headName = repositoryName + "-head-" + id + "-" + UUID.randomUUID().toString();
        final File baseFile = new File(cloneDir, baseName);
        final File headFile = new File(cloneDir, headName);
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    FileUtils.deleteDirectory(baseFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                try {
                    FileUtils.deleteDirectory(headFile);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        try {
            URL url = new URL(repository.getCloneUrl());
            String repositoryUrl = url.getProtocol() + "://" + githubConfigData.getApiToken() + "@" + url.getHost() + url.getPath();
            shellUtil.executeAndCheck(cloneDir, stderrLineHandler, stdoutLineHandler, null, new String[]{"git", "clone", "--depth=" + (reverseCompareInfo.getAheadBy() + 10), "--branch", base.getRef(), repositoryUrl, baseName});
            FileUtils.copyDirectory(baseFile, headFile);
            shellUtil.executeAndCheck(headFile, stderrLineHandler, stdoutLineHandler, null, new String[]{"git", "fetch", "--depth=" + (pullRequest.getCommits() + 10), "origin", "pull/" + id + "/head:pullRequest"});
            shellUtil.executeAndCheck(headFile, stderrLineHandler, stdoutLineHandler, null, new String[]{"git", "merge", "--no-edit", "--no-ff", "pullRequest"});
            FileUtils.deleteDirectory(new File(baseFile, ".git"));
            FileUtils.deleteDirectory(new File(headFile, ".git"));
        } catch (Exception e) {
            try {
                FileUtils.deleteDirectory(baseFile);
            } catch (Exception e2) {
                logger.error("Unable to delete " + baseFile, e2);
            }
            try {
                FileUtils.deleteDirectory(headFile);
            } catch (Exception e2) {
                logger.error("Unable to delete " + headFile, e2);
            }
            throw new IOException(e);
        }

        return new SourceRetrievalResult() {
            @Override
            public File getBaseDir() {
                return baseFile;
            }

            @Override
            public File getHeadDir() {
                return headFile;
            }

            @Override
            public List<String> getChangedFiles() {
                List<CommitFile> files = compareInfo.getFiles();
                List<String> diffFiles = new ArrayList<>(files.size());
                for (CommitFile file : files) {
                    diffFiles.add(file.getFilename());
                }
                return diffFiles;
            }
        };
    }

    @Override
    public void onStart(Map config) throws IOException {
        GithubConfigData githubConfigData = new GithubConfigData(config);
        CommitService commitService = new CommitService(githubConfigData.getGitHubClient());
        CommitStatus status = new CommitStatus();
        status.setState(CommitStatus.STATE_PENDING);
        status.setDescription("Wingman build initiated");
        String wingmanUrl = getStringOrNull(config, WINGMAN_URL);
        if (wingmanUrl != null) {
            status.setTargetUrl(wingmanUrl);
        }
        commitService.createStatus(githubConfigData.getRepository(), githubConfigData.getPullRequest().getHead().getSha(), status);
    }

    @Override
    public void onStop(Map config, OrchestrationResult orchestrationResult) throws IOException {
        GithubConfigData githubConfigData = new GithubConfigData(config);
        new IssueService(githubConfigData.getGitHubClient()).createComment(githubConfigData.getRepository(), Integer.parseInt(githubConfigData.getPullRequestNumber()), orchestrationResult.getReport());
        CommitStatus status = new CommitStatus();
        switch (orchestrationResult.getStatus()) {
            case GOOD:
                status.setState(CommitStatus.STATE_SUCCESS);
                break;
            case WARN:
                status.setState(CommitStatus.STATE_FAILURE);
                break;
            case ERROR:
                status.setState(CommitStatus.STATE_ERROR);
                break;
            default:
                throw new IOException("Unrecognized status: " + orchestrationResult.getStatus());
        }
        CommitService commitService = new CommitService(githubConfigData.getGitHubClient());
        commitService.createStatus(githubConfigData.getRepository(), githubConfigData.getPullRequest().getHead().getSha(), status);
    }

    @Override
    public String getId() {
        return GITHUB;
    }
}
