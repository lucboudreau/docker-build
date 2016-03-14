package org.pentaho.build.buddy.util.shell;

import org.pentaho.build.buddy.bundles.api.result.LineHandler;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by bryan on 2/17/16.
 */
public class ShellUtil {
    public int execute(File directory, final LineHandler stdErrHandler, LineHandler stdOutHandler, final String stdIn, String... command) throws InterruptedException, ShellException {
        final Process process;
        try {
            process = new ProcessBuilder(command).directory(directory).start();
        } catch (IOException e) {
            throw new ShellException("Unable to execute " + Arrays.toString(command), e);
        }
        return monitorProcess(process, stdErrHandler, stdOutHandler, stdIn);
    }

    public int monitorProcess(final Process process, final LineHandler stdErrHandler, LineHandler stdOutHandler, final String stdIn) throws ShellException, InterruptedException {
        new Thread(new LineHandlerFeeder(process.getErrorStream(), stdErrHandler)).start();
        new Thread(new LineHandlerFeeder(process.getInputStream(), stdOutHandler)).start();
        final AtomicReference<IOException> outputException = new AtomicReference<>(null);
        Thread stdInThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (stdIn != null) {
                    try (OutputStreamWriter outputStreamWriter = new OutputStreamWriter(process.getOutputStream())) {
                        outputStreamWriter.write(stdIn);
                    } catch (IOException e) {
                        outputException.set(e);
                    }
                }
            }
        });
        stdInThread.start();
        stdInThread.join();
        IOException ioException = outputException.get();
        if (ioException != null) {
            throw new ShellException("Error writing to process stdin", ioException);
        }
        return process.waitFor();
    }

    public void executeAndCheck(File directory, final LineHandler stdErrHandler, LineHandler stdOutHandler, final String stdIn, String... command) throws InterruptedException, ShellException, IOException {
        int result = execute(directory, stdErrHandler, stdOutHandler, stdIn, command);
        if (result != 0) {
            throw new IOException("Exit code: " + result + " for command: " + Arrays.toString(command));
        }
    }

    public int executeCommandLineAndCheck(File directory, final LineHandler stdErrHandler, LineHandler stdOutHandler, final String stdIn, String commandLine) throws IOException, ShellException, InterruptedException {
        return execute(directory, stdErrHandler, stdOutHandler, stdIn, new String[]{"/bin/bash", "-c", commandLine});
    }
}
