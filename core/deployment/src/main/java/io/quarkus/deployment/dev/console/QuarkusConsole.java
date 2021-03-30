package io.quarkus.deployment.dev.console;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.function.Consumer;

import org.aesh.readline.tty.terminal.TerminalConnection;
import org.aesh.readline.util.LoggerUtil;
import org.aesh.terminal.Attributes;
import org.aesh.terminal.Connection;
import org.aesh.terminal.tty.Size;
import org.aesh.terminal.utils.ANSI;

public class QuarkusConsole implements Consumer<Connection> {

    public static volatile QuarkusConsole INSTANCE;

    private final ArrayDeque<InputHolder> inputHandlers = new ArrayDeque<>();

    private Size size;
    private Attributes attributes;
    private Connection connection;

    private String statusMessage;
    private String promptMessage;
    private int totalStatusLines = 0;
    private String emptyLine;
    private int lastWriteCursorX;

    @Override
    public synchronized void accept(Connection connection) {
        INSTANCE = this;
        this.connection = connection;
        RedirectPrintStream ps = new RedirectPrintStream(this);
        System.err.close();
        System.setOut(ps);
        System.setErr(ps);
        LoggerUtil.doLog();
        connection.openNonBlocking();
        setup(connection);
    }

    public synchronized QuarkusConsole setStatusMessage(String statusMessage) {
        clearStatusMessages();
        int newLines = countLines(statusMessage) + countLines(promptMessage);
        if (statusMessage == null) {
            if (promptMessage != null) {
                newLines += 2;
            }
        } else if (promptMessage == null) {
            newLines += 2;
        } else {
            newLines += 3;
        }
        if (newLines > totalStatusLines) {
            for (int i = 0; i < newLines - totalStatusLines; ++i) {
                connection.write("\n");
            }
        }
        this.statusMessage = statusMessage;
        this.totalStatusLines = newLines;
        printStatusAndPrompt();
        return this;
    }

    public synchronized void pushInputHandler(InputHandler inputHandler) {
        InputHolder holder = inputHandlers.peek();
        if (holder != null) {
            holder.setEnabled(false);
        }
        holder = new InputHolder(inputHandler);
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

    private synchronized QuarkusConsole setPromptMessage(String promptMessage) {
        clearStatusMessages();
        int newLines = countLines(statusMessage) + countLines(promptMessage);
        if (statusMessage == null) {
            if (promptMessage != null) {
                newLines += 2;
            }
        } else if (promptMessage == null) {
            newLines += 2;
        } else {
            newLines += 3;
        }
        if (newLines > totalStatusLines) {
            for (int i = 0; i < newLines - totalStatusLines; ++i) {
                connection.write("\n");
            }
        }
        this.promptMessage = promptMessage;
        this.totalStatusLines = newLines;
        printStatusAndPrompt();
        return this;
    }

    private synchronized void end(Connection conn) {
        conn.write(ANSI.MAIN_BUFFER);
        conn.write(ANSI.CURSOR_SHOW);
        conn.setAttributes(attributes);
    }

    private void setup(Connection conn) {
        size = conn.size();
        // Ctrl-C ends the game
        conn.setSignalHandler(event -> {
            switch (event) {
                case INT:
                    //todo: why does async exit not work here
                    //Quarkus.asyncExit();
                    //end(conn);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            System.exit(0);
                        }
                    }).start();
                    break;
            }
        });
        // Keyboard handling
        conn.setStdinHandler(keys -> {
            InputHolder handler = inputHandlers.peek();
            if (handler != null) {
                handler.handler.handleInput(keys);
            }
        });

        StringBuilder line = new StringBuilder();
        for (int i = 0; i < size.getWidth(); ++i) {
            line.append(" ");
        }
        emptyLine = line.toString();

        conn.setCloseHandler(close -> end(conn));
        conn.setSizeHandler(size -> setup(conn));

        //switch to alternate buffer
        //conn.write(ANSI.ALTERNATE_BUFFER);
        //conn.write(ANSI.CURSOR_HIDE);

        attributes = conn.enterRawMode();

        printStatusAndPrompt();
    }

    /**
     * prints the status messages
     * <p>
     * this will overwrite the bottom part of the screen
     * callers are responsible for writing enough newlines to
     * preserve any console history they want.
     */
    private void printStatusAndPrompt() {
        if (totalStatusLines == 0) {
            return;
        }

        clearStatusMessages();
        gotoLine(size.getHeight() - totalStatusLines);
        connection.write("\n--\n");
        if (statusMessage != null) {
            connection.write(statusMessage);
            if (promptMessage != null) {
                connection.write("\n");
            }
        }
        if (promptMessage != null) {
            connection.write(promptMessage);
        }
    }

    private void clearStatusMessages() {
        gotoLine(size.getHeight() - totalStatusLines);
        for (int i = 0; i <= totalStatusLines; ++i) {
            connection.write(emptyLine);
        }
    }

    private StringBuilder gotoLine(StringBuilder builder, int line) {
        return builder.append("\033[").append(line).append(";").append(0).append("H");
    }

    private void gotoLine(int line) {
        connection.write("\033[" + line + ";" + 0 + "H");
    }

    int countLines(String s) {
        return countLines(s, 0);
    }

    String strip(String s) {
        if (s == null) {
            return null;
        }
        s = s.replaceAll("\\u001B\\[39m", "");
        s = s.replaceAll("\\u001B\\[38(.*?)m", "");
        return s;
    }

    int countLines(String s, int cursorPos) {
        if (s == null) {
            return 0;
        }
        s = strip(s);
        int lines = 0;
        int curLength = cursorPos;
        for (int i = 0; i < s.length(); ++i) {
            if (s.charAt(i) == '\n') {
                lines++;
                curLength = 0;
            } else if (curLength++ == size.getWidth()) {
                lines++;
                curLength = 0;
            }
        }
        return lines;
    }

    public synchronized void write(String s) {
        clearStatusMessages();
        int cursorPos = lastWriteCursorX;
        gotoLine(size.getHeight());
        String stripped = strip(s);
        int lines = countLines(s, cursorPos);
        int trailing = 0;
        int index = stripped.lastIndexOf("\n");
        if (index == -1) {
            trailing = stripped.length();
        } else {
            trailing = stripped.length() - index - 1;
        }

        int newCursorPos;
        if (lines == 0) {
            newCursorPos = trailing + cursorPos;
        } else {
            newCursorPos = trailing;
        }

        if (cursorPos > 1 && lines == 0) {
            connection.write(s);
            lastWriteCursorX = newCursorPos;
            //partial line, just write it
            return;
        }
        if (lines == 0) {
            lines++;
        }
        //move the existing content up by the number of lines
        int appendLines = cursorPos > 1 ? lines - 1 : lines;
        for (int i = 0; i < appendLines; ++i) {
            connection.write("\n");
        }
        StringBuilder builder = new StringBuilder();
        builder.append("\033[").append(size.getHeight() - totalStatusLines - lines).append(";").append(0).append("H");
        connection.write(builder.toString());
        connection.write(s);
        lastWriteCursorX = newCursorPos;
        printStatusAndPrompt();

    }

    public void write(byte[] buf, int off, int len) {
        write(new String(buf, off, len, connection.outputEncoding()));
    }

    public static void main(String[] args) throws IOException {
        new TerminalConnection(new QuarkusConsole());
    }

    class InputHolder implements Consumer<String> {
        final InputHandler handler;
        volatile boolean enabled;
        String prompt;

        private InputHolder(InputHandler handler) {
            this.handler = handler;
        }

        @Override
        public void accept(String s) {
            if (enabled) {
                setPromptMessage(s);
            }
            prompt = s;
        }

        public InputHolder setEnabled(boolean enabled) {
            this.enabled = enabled;
            if (enabled) {
                accept(prompt);
            }
            return this;
        }
    }
}
