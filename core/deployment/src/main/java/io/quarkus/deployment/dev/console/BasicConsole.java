package io.quarkus.deployment.dev.console;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;

import org.jboss.logging.Logger;

import io.quarkus.runtime.logging.LoggingSetupRecorder;

class BasicConsole extends QuarkusConsole {

    private final Logger log = Logger.getLogger(BasicConsole.class);

    final PrintStream printStream;
    final boolean noColor;

    BasicConsole(boolean noColor, boolean inputSupport, PrintStream printStream) {
        this.noColor = noColor;
        this.printStream = printStream;
        if (inputSupport) {
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            int val = System.in.read();
                            if (val == -1) {
                                return;
                            }
                            InputHolder handler = inputHandlers.peek();
                            if (handler != null) {
                                handler.handler.handleInput(new int[] { val });
                            }
                        } catch (IOException e) {
                            log.error("Failed to read user input", e);
                            return;
                        }
                    }

                }
            }, "Quarkus Terminal Reader");
            t.setDaemon(true);
            t.start();
        }
    }

    @Override
    public InputHolder createHolder(InputHandler inputHandler) {
        return new InputHolder(inputHandler) {
            @Override
            protected void setPromptMessage(String prompt) {
                if (prompt == null) {
                    return;
                }
                write("\n" + prompt + "\n");
            }

            @Override
            protected void setStatusMessage(String status) {
                if (status == null) {
                    return;
                }
                write("\n" + status + "\n");
            }
        };
    }

    @Override
    public void write(String s) {
        if (noColor || !LoggingSetupRecorder.hasColorSupport()) {
            printStream.print(stripAnsiCodes(s));
        } else {
            printStream.print(s);
        }

    }

    @Override
    public void write(byte[] buf, int off, int len) {
        write(new String(buf, off, len, Charset.defaultCharset()));
    }
}
