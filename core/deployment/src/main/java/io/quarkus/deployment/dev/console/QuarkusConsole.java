package io.quarkus.deployment.dev.console;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.aesh.readline.tty.terminal.TerminalConnection;
import org.aesh.terminal.Connection;

import io.quarkus.deployment.TestConfig;

public abstract class QuarkusConsole {

    protected final ArrayDeque<InputHolder> inputHandlers = new ArrayDeque<>();

    public static volatile QuarkusConsole INSTANCE = new BasicConsole(false, false, System.out);

    private static volatile boolean installed;

    protected volatile Predicate<String> outputFilter;

    public synchronized void pushInputHandler(InputHandler inputHandler) {
        InputHolder holder = inputHandlers.peek();
        if (holder != null) {
            holder.setEnabled(false);
        }
        holder = createHolder(inputHandler);
        inputHandler.promptHandler(holder);
        holder.setEnabled(true);
        inputHandlers.push(holder);
    }

    public void popInputHandler() {
        InputHolder holder = inputHandlers.pop();
        holder.setEnabled(false);
        holder = inputHandlers.peek();
        if (holder != null) {
            holder.setEnabled(true);
        }
    }

    public abstract InputHolder createHolder(InputHandler inputHandler);

    public abstract void write(String s);

    public abstract void write(byte[] buf, int off, int len);

    public static synchronized void installConsole(TestConfig config) {
        if (installed) {
            return;
        }
        installed = true;
        if (config.basicConsole) {
            INSTANCE = new BasicConsole(config.disableColor, true, System.out);
        } else {
            try {
                new TerminalConnection(new Consumer<Connection>() {
                    @Override
                    public void accept(Connection connection) {
                        if (connection.supportsAnsi()) {
                            INSTANCE = new AeshConsole(connection);
                            RedirectPrintStream ps = new RedirectPrintStream();
                            System.setOut(ps);
                            System.setErr(ps);
                        } else {
                            connection.close();
                            INSTANCE = new BasicConsole(config.disableColor, true, System.out);
                        }

                    }
                });
            } catch (IOException e) {
                INSTANCE = new BasicConsole(config.disableColor, true, System.out);
            }
        }
    }

    protected String stripAnsiCodes(String s) {
        if (s == null) {
            return null;
        }
        s = s.replaceAll("\\u001B\\[(.*?)[a-zA-Z]", "");
        return s;
    }

    public void setOutputFilter(Predicate<String> logHandler) {
        this.outputFilter = logHandler;
    }

    protected static abstract class InputHolder implements InputHandler.ConsoleStatus {
        final InputHandler handler;
        volatile boolean enabled;
        String prompt;
        String status;

        protected InputHolder(InputHandler handler) {
            this.handler = handler;
        }

        public InputHolder setEnabled(boolean enabled) {
            this.enabled = enabled;
            if (enabled) {
                setStatus(status);
                setPrompt(prompt);
            }
            return this;
        }

        @Override
        public void setPrompt(String prompt) {
            this.prompt = prompt;
            if (enabled) {
                setPromptMessage(prompt);
            }
        }

        protected abstract void setPromptMessage(String prompt);

        @Override
        public void setStatus(String status) {
            this.status = status;
            if (enabled) {
                setStatusMessage(status);
            }
        }

        protected abstract void setStatusMessage(String status);
    }
}
