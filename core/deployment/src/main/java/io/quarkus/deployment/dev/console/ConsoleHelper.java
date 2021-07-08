package io.quarkus.deployment.dev.console;

import java.io.IOException;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.aesh.readline.tty.terminal.TerminalConnection;
import org.aesh.terminal.Connection;

import io.quarkus.deployment.console.ConsoleConfig;
import io.quarkus.deployment.dev.testing.TestConfig;
import io.quarkus.dev.console.BasicConsole;
import io.quarkus.dev.console.QuarkusConsole;
import io.quarkus.dev.console.RedirectPrintStream;
import io.quarkus.runtime.console.ConsoleRuntimeConfig;
import io.quarkus.runtime.util.ColorSupport;

public class ConsoleHelper {

    public static synchronized void installConsole(TestConfig config, ConsoleConfig consoleConfig,
            ConsoleRuntimeConfig consoleRuntimeConfig, io.quarkus.runtime.logging.ConsoleConfig logConfig) {
        if (QuarkusConsole.installed) {
            return;
        }
        boolean colorEnabled = ColorSupport.isColorEnabled(consoleRuntimeConfig, logConfig);
        QuarkusConsole.installed = true;
        //if there is no color we need a basic console
        Consumer<String> consumer = System.out::print;
        if (System.console() != null) {
            consumer = System.console().writer()::print;
        }
        if (config.basicConsole.orElse(consoleConfig.basic) || !colorEnabled) {
            QuarkusConsole.INSTANCE = new BasicConsole(colorEnabled,
                    !config.disableConsoleInput.orElse(consoleConfig.disableInput), consumer);
        } else {
            try {
                new TerminalConnection(new Consumer<Connection>() {
                    @Override
                    public void accept(Connection connection) {
                        if (connection.supportsAnsi()) {
                            QuarkusConsole.INSTANCE = new AeshConsole(connection,
                                    !config.disableConsoleInput.orElse(consoleConfig.disableInput));
                        } else {
                            LinkedBlockingDeque<Integer> queue = new LinkedBlockingDeque<>();
                            connection.openNonBlocking();
                            connection.setStdinHandler(new Consumer<int[]>() {
                                @Override
                                public void accept(int[] ints) {
                                    for (int i : ints) {
                                        queue.add(i);
                                    }
                                }
                            });
                            connection.setCloseHandler(new Consumer<Void>() {
                                @Override
                                public void accept(Void unused) {
                                    queue.add(-1);
                                }
                            });
                            QuarkusConsole.INSTANCE = new BasicConsole(colorEnabled,
                                    !config.disableConsoleInput.orElse(consoleConfig.disableInput),
                                    connection::write, new Supplier<Integer>() {
                                        @Override
                                        public Integer get() {
                                            try {
                                                return queue.takeFirst();
                                            } catch (InterruptedException e) {
                                                throw new RuntimeException(e);
                                            }
                                        }
                                    });
                        }
                    }
                });
            } catch (IOException e) {
                QuarkusConsole.INSTANCE = new BasicConsole(colorEnabled,
                        !config.disableConsoleInput.orElse(consoleConfig.disableInput), consumer);
            }
        }
        RedirectPrintStream ps = new RedirectPrintStream();
        System.setOut(ps);
        System.setErr(ps);
    }
}
