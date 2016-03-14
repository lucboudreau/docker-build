package org.pentaho.build.buddy.bundles.rest;

import org.pentaho.build.buddy.bundles.api.orchestrator.Orchestrator;
import org.pentaho.build.buddy.bundles.api.result.LineHandler;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;
import java.util.Date;
import java.util.Map;

/**
 * Created by bryan on 2/26/16.
 */
@Path("/")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrchestratorRestService {
    private final Orchestrator orchestrator;

    public OrchestratorRestService(Orchestrator orchestrator) {
        this.orchestrator = orchestrator;
    }

    @POST
    @Path("orchestrate")
    public StreamingOutput orchestrate(final Map map) {
        return new StreamingOutput() {
            @Override
            public void write(OutputStream output) throws IOException, WebApplicationException {
                try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(output)) {
                    try (final PrintWriter printWriter = new PrintWriter(outputStreamWriter){
                        long lastFlush = System.currentTimeMillis();
                        @Override public void write(String s, int off, int len) {
                            super.write(s, off, len);
                            long currentTimeMillis = System.currentTimeMillis();
                            if (currentTimeMillis - lastFlush > 1000) {
                                super.flush();
                                lastFlush = currentTimeMillis;
                            }
                        }
                    }) {
                        orchestrator.orchestrate(map, new LineHandler() {
                            @Override
                            public void handle(String line) {
                                printWriter.write(new Date() + " - stdout - " + line + "\n");
                            }

                            @Override
                            public void handle(Exception e) {
                                e.printStackTrace(printWriter);
                            }
                        }, new LineHandler() {
                            @Override
                            public void handle(String line) {
                                printWriter.write(new Date() + " - stderr - " + line + "\n");
                            }

                            @Override
                            public void handle(Exception e) {
                                e.printStackTrace(printWriter);
                            }
                        });
                    }
                }
            }
        };
    }
}
