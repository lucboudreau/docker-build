package org.pentaho.build.buddy.bundles.rest;

import org.pentaho.build.buddy.bundles.api.orchestrator.Orchestrator;
import org.pentaho.build.buddy.bundles.api.result.LineHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by bryan on 2/26/16.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrchestratorRestService {
    private static final Logger logger = LoggerFactory.getLogger(OrchestratorRestService.class);
    private final Orchestrator orchestrator;

    public OrchestratorRestService(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @POST
    @Path("orchestrate")
    public StreamingOutput orchestrate(final Map map) {
        return new StreamingOutput() {
            @Override
            public void write(final OutputStream output) throws IOException, WebApplicationException {
                try (final OutputStreamWriter outputStreamWriter = new OutputStreamWriter(output)) {
                    try (final PrintWriter printWriter = new PrintWriter(outputStreamWriter, true)) {
                        final BlockingQueue<String> queue = new LinkedBlockingQueue<>();
                        final AtomicBoolean done = new AtomicBoolean(false);
                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    while (!done.get()) {
                                        String line;
                                        while ((line = queue.poll(100, TimeUnit.MILLISECONDS)) != null) {
                                            printWriter.write(line);
                                        }
                                    }
                                } catch (InterruptedException e) {
                                    logger.error(e.getMessage(), e);
                                }
                            }
                        });
                        thread.setDaemon(true);
                        thread.start();
                        try {
                            orchestrator.orchestrate(map, new LineHandler() {
                                @Override
                                public void handle(String line) {
                                    putStringOnQueue(queue, "stdout", line);
                                }

                                @Override
                                public void handle(Exception e) {
                                    putStackOnQueue(queue, "stdout", e);
                                }
                            }, new LineHandler() {
                                @Override
                                public void handle(String line) {
                                    putStringOnQueue(queue, "stderr", line);
                                }

                                @Override
                                public void handle(Exception e) {
                                    putStackOnQueue(queue, "stderr", e);
                                }
                            });
                        } finally {
                            done.set(true);
                        }
                    }
                }
            }
        };
    }

    private void putStringOnQueue(BlockingQueue<String> queue, String stream, String line) {
        try {
            queue.put(new Date() + " - " + stream + " - " + line + "\n");
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void putStackOnQueue(BlockingQueue<String> queue, String stream, Exception e) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            try {
                try (PrintWriter printWriter = new PrintWriter(outputStream)) {
                    e.printStackTrace(printWriter);
                }
            } finally {
                outputStream.close();
            }
            for (String stackElem : new String(outputStream.toByteArray(), Charset.forName("UTF-8")).split("\n")) {
                queue.put(new Date() + " - " + stream + " - " + stackElem + "\n");
            }
        } catch (Exception e1) {
            logger.error(e.getMessage(), e);
        }
    }
}
