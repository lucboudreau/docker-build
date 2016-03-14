package org.pentaho.build.buddy.util.shell;

import org.pentaho.build.buddy.bundles.api.result.LineHandler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Created by bryan on 2/17/16.
 */
public class LineHandlerFeeder implements Runnable {
    private final InputStream stream;
    private final LineHandler handler;

    public LineHandlerFeeder(InputStream stream, LineHandler handler) {
        this.stream = stream;
        this.handler = handler;
    }

    @Override
    public void run() {
        try (InputStreamReader inputStreamReader = new InputStreamReader(stream)) {
            try (BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    if (handler != null) {
                        handler.handle(line);
                    }
                }
            } catch (IOException e) {
                if (handler != null) {
                    handler.handle(e);
                }
            }
        } catch (IOException e) {
            if (handler != null) {
                handler.handle(e);
            }
        }
    }
}
