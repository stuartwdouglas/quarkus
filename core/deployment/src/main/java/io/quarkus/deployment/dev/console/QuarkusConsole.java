package io.quarkus.deployment.dev.console;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.Consumer;

import org.aesh.readline.tty.terminal.TerminalConnection;
import org.aesh.readline.util.LoggerUtil;
import org.aesh.terminal.Attributes;
import org.aesh.terminal.Connection;
import org.aesh.terminal.tty.Size;
import org.aesh.terminal.utils.ANSI;

import io.quarkus.runtime.Quarkus;

public class QuarkusConsole implements Consumer<Connection> {

    public static volatile QuarkusConsole INSTANCE;

    private Size size;
    private Attributes attributes;
    private Connection connection;

    private String statusMessage;
    private String promptMessage;
    private int totalStatusLines = 0;
    private String emptyLine;

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
        int newLines = countLines(statusMessage) + countLines(promptMessage) + 3;
        if (newLines > totalStatusLines) {
            for (int i = 0; i < newLines - totalStatusLines; ++i) {
                write("\n");
            }
        }
        this.statusMessage = statusMessage;
        this.totalStatusLines = newLines;
        printStatusAndPrompt();
        return this;
    }

    public synchronized QuarkusConsole setPromptMessage(String promptMessage) {
        int newLines = countLines(statusMessage) + countLines(promptMessage) + 3;
        if (newLines > totalStatusLines) {
            for (int i = 0; i < newLines - totalStatusLines; ++i) {
                write("\n");
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
                    Quarkus.asyncExit();
                    end(conn);
                    System.exit(0);
                    break;
            }
        });
        // Keyboard handling
        conn.setStdinHandler(keys -> {
            System.out.println("got: " + Arrays.toString(keys));
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
     *
     * this will overwrite the bottom part of the screen
     * callers are responsible for writing enough newlines to
     * preserve any console history they want.
     */
    private void printStatusAndPrompt() {
        if (totalStatusLines == 0) {
            return;
        }

        gotoLine(size.getHeight() - totalStatusLines);
        for (int i = 0; i < totalStatusLines; ++i) {
            connection.write(emptyLine);
        }
        gotoLine(size.getHeight() - totalStatusLines);
        connection.write("\n--\n");
        if (statusMessage != null) {
            connection.write(statusMessage);
            connection.write("\n");
        }
        if (promptMessage != null) {
            connection.write(promptMessage);
        }
    }

    private StringBuilder gotoLine(StringBuilder builder, int line) {
        return builder.append("\033[").append(line).append(";").append(0).append("H");
    }

    private void gotoLine(int line) {
        connection.write("\033[" + line + ";" + 0 + "H");
    }

    int countLines(String s) {
        if (s == null) {
            return 0;
        }
        s = s.replaceAll("\\u001B\\[39m", "");
        s = s.replaceAll("\\u001B\\[38(.*?)m", "");
        int lines = 0;
        int curLength = 0;
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
        gotoLine(size.getHeight());
        int lines = countLines(s);
        //move the existing content up by the number of lines
        for (int i = 0; i < lines; ++i) {
            connection.write("\n");
        }
        StringBuilder builder = new StringBuilder();
        builder.append("\033[").append(size.getHeight() - totalStatusLines - lines).append(";").append(0).append("H");
        connection.write(builder.toString());
        connection.write(s);
        printStatusAndPrompt();

    }

    public void write(byte[] buf, int off, int len) {
        write(new String(buf, off, len, connection.outputEncoding()));
    }

    enum Direction {
        UP('A'),
        DOWN('B'),
        RIGHT('C'),
        LEFT('D');

        private final char ansi;

        Direction(char ansi) {
            this.ansi = ansi;
        }

        char ansi() {
            return ansi;
        }
    }

    public static void main(String[] args) throws IOException {
        new TerminalConnection(new QuarkusConsole());
    }
}
