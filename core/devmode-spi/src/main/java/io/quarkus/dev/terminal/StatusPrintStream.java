package io.quarkus.dev.terminal;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Locale;

public class StatusPrintStream extends PrintStream {

    private String statusString;
    private final PrintStream underlying;

    public static final StatusPrintStream INSTANCE;

    static {
        INSTANCE = new StatusPrintStream(System.out);
        System.setOut(INSTANCE);
    }

    public static void init() {

    }

    StatusPrintStream(PrintStream underlying) {
        super(underlying);
        this.underlying = underlying;
    }

    public void setStatusString(String s) {
        if (statusString != null) {
            clearStatus();
            for (int i = 0; i < statusString.length(); ++i) {
                underlying.print(" ");
            }
            clearStatus();
        }
        statusString = s;
        underlying.print(statusString);
    }

    @Override
    public void write(int b) {
        clearStatus();
        underlying.write(b);
        writeStatus();
    }

    private void writeStatus() {
        if (statusString != null) {
            underlying.print(statusString);
        }
    }

    private void clearStatus() {
        if (statusString != null) {
            underlying.print("\r");
        }
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        clearStatus();
        underlying.write(buf, off, len);
        writeStatus();
    }

    //@Overide
    public void write(byte[] buf) throws IOException {
        clearStatus();
        underlying.write(buf, 0, buf.length);
        writeStatus();
    }

    //@Override
    public void writeBytes(byte[] buf) {
        clearStatus();
        underlying.write(buf, 0, buf.length);
        writeStatus();
    }

    @Override
    public void print(boolean b) {
        clearStatus();
        underlying.print(b);
        writeStatus();
    }

    @Override
    public void print(char c) {
        clearStatus();
        underlying.print(c);
        writeStatus();
    }

    @Override
    public void print(int i) {
        clearStatus();
        underlying.print(i);
        writeStatus();
    }

    @Override
    public void print(long l) {
        clearStatus();
        underlying.print(l);
        writeStatus();
    }

    @Override
    public void print(float f) {
        clearStatus();
        underlying.print(f);
        writeStatus();
    }

    @Override
    public void print(double d) {
        clearStatus();
        underlying.print(d);
        writeStatus();
    }

    @Override
    public void print(char[] s) {
        clearStatus();
        underlying.print(s);
        writeStatus();
    }

    @Override
    public void print(String s) {
        clearStatus();
        underlying.print(s);
        writeStatus();
    }

    @Override
    public void print(Object obj) {
        clearStatus();
        underlying.print(obj);
        writeStatus();
    }

    @Override
    public void println() {
        clearStatus();
        underlying.println();
        writeStatus();
    }

    @Override
    public void println(boolean x) {
        clearStatus();
        underlying.println(x);
        writeStatus();
    }

    @Override
    public void println(char x) {
        clearStatus();
        underlying.println(x);
        writeStatus();
    }

    @Override
    public void println(int x) {
        clearStatus();
        underlying.println(x);
        writeStatus();
    }

    @Override
    public void println(long x) {
        clearStatus();
        underlying.println(x);
        writeStatus();
    }

    @Override
    public void println(float x) {
        clearStatus();
        underlying.println(x);
        writeStatus();
    }

    @Override
    public void println(double x) {
        clearStatus();
        underlying.println(x);
        writeStatus();
    }

    @Override
    public void println(char[] x) {
        clearStatus();
        underlying.println(x);
        writeStatus();
    }

    @Override
    public void println(String x) {
        clearStatus();
        underlying.println(x);
        writeStatus();
    }

    @Override
    public void println(Object x) {
        clearStatus();
        underlying.println(x);
        writeStatus();
    }

    @Override
    public PrintStream printf(String format, Object... args) {
        try {
            clearStatus();
            return underlying.printf(format, args);
        } finally {
            writeStatus();
        }
    }

    @Override
    public PrintStream printf(Locale l, String format, Object... args) {
        try {
            clearStatus();
            return underlying.printf(l, format, args);
        } finally {
            writeStatus();
        }
    }

    @Override
    public PrintStream format(String format, Object... args) {
        try {
            clearStatus();
            return underlying.format(format, args);
        } finally {
            writeStatus();
        }
    }

    @Override
    public PrintStream format(Locale l, String format, Object... args) {
        try {
            clearStatus();
            return underlying.format(l, format, args);
        } finally {
            writeStatus();
        }
    }

    @Override
    public PrintStream append(CharSequence csq) {
        try {
            clearStatus();
            return underlying.append(csq);
        } finally {
            writeStatus();
        }
    }

    @Override
    public PrintStream append(CharSequence csq, int start, int end) {
        try {
            clearStatus();
            return underlying.append(csq, start, end);
        } finally {
            writeStatus();
        }
    }

    @Override
    public PrintStream append(char c) {
        try {
            clearStatus();
            return underlying.append(c);
        } finally {
            writeStatus();
        }
    }
}
