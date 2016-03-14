package org.pentaho.build.buddy.bundles.orchestrator;

import org.apache.commons.io.FileUtils;
import org.pentaho.build.buddy.bundles.api.build.BuildCommands;
import org.pentaho.build.buddy.bundles.api.build.BuildRunner;
import org.pentaho.build.buddy.bundles.api.build.CommandBuilder;
import org.pentaho.build.buddy.bundles.api.orchestrator.OrchestrationResult;
import org.pentaho.build.buddy.bundles.api.orchestrator.Orchestrator;
import org.pentaho.build.buddy.bundles.api.output.OutputAnalysis;
import org.pentaho.build.buddy.bundles.api.output.OutputAnalyzer;
import org.pentaho.build.buddy.bundles.api.output.OutputSeverity;
import org.pentaho.build.buddy.bundles.api.result.LineHandler;
import org.pentaho.build.buddy.bundles.api.source.SourceRetrievalResult;
import org.pentaho.build.buddy.bundles.api.source.SourceRetriever;
import org.pentaho.build.buddy.bundles.api.status.StatusUpdater;
import org.pentaho.build.buddy.util.template.FTLUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by bryan on 2/17/16.
 */
public class OrchestratorImpl implements Orchestrator {
    private static final Logger logger = LoggerFactory.getLogger(OrchestratorImpl.class);
    private final List<SourceRetriever> sourceRetrievers;
    private final List<CommandBuilder> commandBuilders;
    private final List<BuildRunner> buildRunners;
    private final Map<OutputAnalyzer, OutputAnalyzerWrapper> outputAnalyzers;
    private final List<StatusUpdater> statusUpdaters;
    private final ExecutorService executorService;
    private final FTLUtil ftlUtil;
    private final ConfigurationEnricher configurationEnricher;

    public OrchestratorImpl(List<SourceRetriever> sourceRetrievers, List<CommandBuilder> commandBuilders, List<BuildRunner> buildRunners, ConfigurationEnricher configurationEnricher, List<StatusUpdater> statusUpdaters) {
        this(sourceRetrievers, commandBuilders, buildRunners, statusUpdaters, Executors.newCachedThreadPool(new ThreadFactory() {
            private final AtomicLong threadNum = new AtomicLong(1);
            private final Set<Long> availNums = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());

            /**
             * We want to reuse thread names as the thread name will be substituted into the maven and ivy cache folder names.  Reuse is good here but not at the same time.
             * @param r
             * @return
             */
            @Override
            public Thread newThread(final Runnable r) {
                Long num = null;
                while (num == null) {
                    try {
                        Long next = availNums.iterator().next();
                        if (availNums.remove(next)) {
                            num = next;
                        }
                    } catch (NoSuchElementException e) {
                        num = threadNum.getAndIncrement();
                    }
                }
                final Long finalNum = num;
                Thread thread = new Thread() {

                    @Override
                    public void run() {
                        try {
                            r.run();
                        } finally {
                            availNums.add(finalNum);
                        }
                    }
                };
                thread.setName("thread-" + finalNum);
                thread.setDaemon(true);
                return thread;
            }
        }), new FTLUtil(OrchestratorImpl.class), configurationEnricher);
    }

    public OrchestratorImpl(List<SourceRetriever> sourceRetrievers, List<CommandBuilder> commandBuilders, List<BuildRunner> buildRunners, List<StatusUpdater> statusUpdaters, ExecutorService executorService, FTLUtil ftlUtil, ConfigurationEnricher configurationEnricher) {
        this.sourceRetrievers = sourceRetrievers;
        this.commandBuilders = commandBuilders;
        this.buildRunners = buildRunners;
        this.statusUpdaters = statusUpdaters;
        this.configurationEnricher = configurationEnricher;
        this.outputAnalyzers = new HashMap<>();
        this.executorService = executorService;
        this.ftlUtil = ftlUtil;
        logger.debug("Orchestrator created");
    }

    @Override
    public OrchestrationResult orchestrate(Map config, final LineHandler stdoutLineHandler, final LineHandler stderrLineHandler) {
        config = configurationEnricher.enrich(config, SourceRetriever.SOURCE_RETRIEVER);
        Map retrieverConfig = (Map) config.get(SourceRetriever.SOURCE_RETRIEVER);
        SourceRetriever sourceRetriever = null;
        for (SourceRetriever potentialSourceRetriever : sourceRetrievers) {
            if (potentialSourceRetriever.getId().equals(retrieverConfig.get(SourceRetriever.SOURCE_CONTROL_TYPE))) {
                sourceRetriever = potentialSourceRetriever;
                break;
            }
        }
        if (sourceRetriever == null) {
            String errorMessage = "Unable to find source retriever for config " + retrieverConfig;
            stderrLineHandler.handle(errorMessage);
            return new OrchestrationResultImpl(OrchestrationResult.Status.ERROR, errorMessage);
        }

        SourceRetrievalResult sourceRetrievalResult;
        stdoutLineHandler.handle("Retrieving source with " + sourceRetriever);
        try {
            sourceRetrievalResult = sourceRetriever.retrieveSource(retrieverConfig);
        } catch (IOException e) {
            String message = "Unable to retrieve source code, shutting down";
            stderrLineHandler.handle(message);
            stderrLineHandler.handle(e);
            return new OrchestrationResultImpl(OrchestrationResult.Status.ERROR, message + ": " + e.getMessage());
        }

        config = configurationEnricher.enrich(config, StatusUpdater.STATUS_UPDATER);
        Map statusUpdaterConfig = (Map) config.get(StatusUpdater.STATUS_UPDATER);
        StatusUpdater statusUpdater = null;
        for (StatusUpdater potentialStatusUpdater : statusUpdaters) {
            if (potentialStatusUpdater.getId().equals(statusUpdaterConfig.get(StatusUpdater.STATUS_UPDATER_TYPE))) {
                statusUpdater = potentialStatusUpdater;
                break;
            }
        }

        if (statusUpdater != null) {
            try {
                statusUpdater.onStart(statusUpdaterConfig);
            } catch (IOException e) {
                String errorMessage = "Unable to update status" + e.getMessage();
                stderrLineHandler.handle(errorMessage);
                stderrLineHandler.handle(e);
            }
        }

        OrchestrationResult orchestrationResult = doOrchestrate(config, stdoutLineHandler, stderrLineHandler, sourceRetrievalResult);

        if (statusUpdater != null) {
            try {
                statusUpdater.onStop(statusUpdaterConfig, orchestrationResult);
            } catch (IOException e) {
                String errorMessage = "Unable to update status" + e.getMessage();
                stderrLineHandler.handle(errorMessage);
                stderrLineHandler.handle(e);
            }
        }
        return orchestrationResult;
    }

    private OrchestrationResult doOrchestrate(Map config, final LineHandler stdoutLineHandler, final LineHandler stderrLineHandler, final SourceRetrievalResult sourceRetrievalResult) {
        try {
            config = configurationEnricher.enrich(config, CommandBuilder.COMMAND_BUILDER);
            Map commandBuilderConfig = (Map) config.get(CommandBuilder.COMMAND_BUILDER);
            CommandBuilder commandBuilder = null;
            for (CommandBuilder builder : commandBuilders) {
                if (builder.getId().equals(commandBuilderConfig.get(CommandBuilder.BUILD_TOOL))) {
                    commandBuilder = builder;
                    break;
                }
            }

            stdoutLineHandler.handle("Building command with " + commandBuilder);
            final BuildCommands buildCommands;
            try {
                buildCommands = commandBuilder.buildCommands(sourceRetrievalResult, commandBuilderConfig);
            } catch (IOException e) {
                String errorMessage = "Error building command: ";
                stderrLineHandler.handle(errorMessage);
                stderrLineHandler.handle(e);
                return new OrchestrationResultImpl(OrchestrationResult.Status.ERROR, errorMessage + e.getMessage());
            }

            BuildRunner buildRunner = null;
            config = configurationEnricher.enrich(config, BuildRunner.BUILD_RUNNER);
            final Map buildRunnerConfig = (Map) config.get(BuildRunner.BUILD_RUNNER);
            for (BuildRunner runner : buildRunners) {
                if (runner.canHandle(buildRunnerConfig)) {
                    buildRunner = runner;
                    break;
                }
            }

            if (buildRunner == null) {
                String errorMessage = "Couldn't find build runner for config " + buildRunnerConfig;
                stderrLineHandler.handle(errorMessage);
                return new OrchestrationResultImpl(OrchestrationResult.Status.ERROR, errorMessage);
            }

            final BuildRunner finalBuildRunner = buildRunner;
            Future<Integer> baseBuildFuture = executorService.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    try {
                        return finalBuildRunner.runBuild(sourceRetrievalResult.getBaseDir(), buildCommands, buildRunnerConfig, new PrefixLineHandler(stdoutLineHandler, "base: "), new PrefixLineHandler(stderrLineHandler, "base: "));
                    } catch (IOException e) {
                        stderrLineHandler.handle("Error running base build.");
                        stderrLineHandler.handle(e);
                        return 42;
                    }
                }
            });

            Future<Integer> headBuildFuture = executorService.submit(new Callable<Integer>() {
                @Override
                public Integer call() throws Exception {
                    try {
                        return finalBuildRunner.runBuild(sourceRetrievalResult.getHeadDir(), buildCommands, buildRunnerConfig, new PrefixLineHandler(stdoutLineHandler, "head: "), new PrefixLineHandler(stderrLineHandler, "head: "));
                    } catch (IOException e) {
                        stderrLineHandler.handle("Error running head build.");
                        stderrLineHandler.handle(e);
                        return 42;
                    }
                }
            });

            int baseResult;
            try {
                baseResult = baseBuildFuture.get();
            } catch (Exception e) {
                stderrLineHandler.handle("Error waiting on base result");
                stderrLineHandler.handle(e);
                baseResult = 42;
            }

            if (baseResult != 0) {
                String errorMessage = "Base build failed with return code " + baseResult;
                stderrLineHandler.handle(errorMessage);
                return new OrchestrationResultImpl(OrchestrationResult.Status.ERROR, errorMessage);
            }

            int headResult;
            try {
                headResult = headBuildFuture.get();
            } catch (Exception e) {
                stderrLineHandler.handle("Error waiting on head result");
                stderrLineHandler.handle(e);
                headResult = 42;
            }

            if (headResult != 0) {
                String errorMessage = "Head build failed with return code " + headResult;
                stderrLineHandler.handle(errorMessage);
                return new OrchestrationResultImpl(OrchestrationResult.Status.ERROR, errorMessage);
            }

            List<String> outputs = new ArrayList<>();
            try {
                outputs.add(ftlUtil.render("buildCommands.ftl", "build", buildCommands).trim());
            } catch (IOException e) {
                String errorMessage = "Error generating build command report " + e.getMessage();
                stderrLineHandler.handle(errorMessage);
                stderrLineHandler.handle(e);
                return new OrchestrationResultImpl(OrchestrationResult.Status.ERROR, errorMessage);
            }

            List<OutputSeverity> outputSeverities = new ArrayList<>();
            List<OutputAnalyzerWrapper> sortedAnalyzers = new ArrayList<>();
            synchronized (outputAnalyzers) {
                sortedAnalyzers.addAll(outputAnalyzers.values());
            }
            Collections.sort(sortedAnalyzers);
            for (OutputAnalyzerWrapper outputAnalyzerWrapper : sortedAnalyzers) {
                OutputAnalyzer outputAnalyzer = outputAnalyzerWrapper.getOutputAnalyzer();
                try {
                    OutputAnalysis outputAnalysis = outputAnalyzer.analyzeOutput(sourceRetrievalResult, stdoutLineHandler, stderrLineHandler);
                    outputSeverities.add(outputAnalysis.getOutputSeverity());
                    String trim = outputAnalysis.getReport().trim();
                    outputs.add(trim);
                } catch (Exception e) {
                    String errorMessage = "Error analyzing output with " + outputAnalyzer + ": ";
                    stderrLineHandler.handle(errorMessage);
                    stderrLineHandler.handle(e);
                    return new OrchestrationResultImpl(OrchestrationResult.Status.ERROR, errorMessage + e.getMessage());
                }
            }

            outputSeverities.add(OutputSeverity.INFO);

            OrchestrationResult.Status status;
            OutputSeverity severity = OutputSeverity.max(outputSeverities);
            switch (severity) {
                case ERROR:
                    status = OrchestrationResult.Status.ERROR;
                    break;
                case WARNING:
                    status = OrchestrationResult.Status.WARN;
                    break;
                case INFO:
                    status = OrchestrationResult.Status.GOOD;
                    break;
                default:
                    return new OrchestrationResultImpl(OrchestrationResult.Status.ERROR, "Unmatched output severity of: " + severity);
            }

            StringBuilder report = new StringBuilder();
            for (String output : outputs) {
                report.append(output);
                report.append("\n\n");
            }

            String reportString = report.toString().trim();
            for (String outputLine : reportString.split("\n")) {
                stdoutLineHandler.handle(outputLine);
            }
            return new OrchestrationResultImpl(status, reportString);
        } finally {
            try {
                FileUtils.deleteDirectory(sourceRetrievalResult.getBaseDir());
            } catch (IOException e) {
                stderrLineHandler.handle(e);
            }
            try {
                FileUtils.deleteDirectory(sourceRetrievalResult.getHeadDir());
            } catch (IOException e) {
                stderrLineHandler.handle(e);
            }
        }
    }

    public void analyzerAdded(OutputAnalyzer outputAnalyzer, Map props) {
        if (outputAnalyzer != null) {
            synchronized (outputAnalyzers) {
                Object ranking = props.get("service.ranking");
                if (ranking == null) {
                    ranking = 50;
                }
                if (!(ranking instanceof Number)) {
                    ranking = Integer.parseInt(String.valueOf(ranking));
                }
                outputAnalyzers.put(outputAnalyzer, new OutputAnalyzerWrapper(outputAnalyzer, ((Number) ranking).intValue()));
            }
        }
    }

    public void analyzerRemoved(OutputAnalyzer outputAnalyzer) {
        if (outputAnalyzer != null) {
            synchronized (outputAnalyzers) {
                outputAnalyzers.remove(outputAnalyzer);
            }
        }
    }
}
