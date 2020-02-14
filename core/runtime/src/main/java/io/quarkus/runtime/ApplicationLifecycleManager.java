package io.quarkus.runtime;

import java.util.Set;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.LockSupport;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.CDI;

import org.graalvm.nativeimage.ImageInfo;
import org.jboss.logging.Logger;
import org.wildfly.common.lock.Locks;

import com.oracle.svm.core.OS;
import io.quarkus.runtime.graal.DiagnosticPrinter;
import sun.misc.Signal;
import sun.misc.SignalHandler;

/**
 * Manages the lifecycle of a Quarkus application.
 *
 * The {@link Application} class is responsible for starting and stopping the application,
 * but nothing else. This class can be used to run both persistent application that will run
 * till they receive a signal, and command mode applications that will run until the main method
 * returns.
 *
 * This class is static, there can only every be a single application instance running at any time.
 *
 */
public class ApplicationLifecycleManager {


    private ApplicationLifecycleManager() {

    }

    // WARNING: do not inject a logger here, it's too early: the log manager has not been properly set up yet

    private static final String DISABLE_SIGNAL_HANDLERS = "DISABLE_SIGNAL_HANDLERS";

    //guard for all state
    private static final Lock stateLock = Locks.reentrantLock();
    private static final Condition stateCond = stateLock.newCondition();

    private static int exitCode = -1;
    private static boolean shutdownRequested;
    private static Application application;
    private static boolean signalSetup;

    public static final void run(Application application, String[] args) {
        run(null, args);
    }

    public static final void run(Application application, Class<? extends QuarkusApplication> quarkusApplication, String[] args) {
        try {
            stateLock.lock();
            try {
                if (!ImageInfo.inImageRuntimeCode() && System.getenv(DISABLE_SIGNAL_HANDLERS) == null) {
                    final SignalHandler handler = new SignalHandler() {
                        @Override
                        public void handle(Signal signal) {
                            System.exit(signal.getNumber() + 0x80);
                        }
                    };
                    final SignalHandler quitHandler = new SignalHandler() {
                        @Override
                        public void handle(Signal signal) {
                            DiagnosticPrinter.printDiagnostics(System.out);
                        }
                    };
                    handleSignal("INT", handler);
                    handleSignal("TERM", handler);
                    // the HUP and QUIT signals are not defined for the Windows OpenJDK implementation:
                    // https://hg.openjdk.java.net/jdk8u/jdk8u-dev/hotspot/file/7d5c800dae75/src/os/windows/vm/jvm_windows.cpp
                    if (OS.getCurrent() == OS.WINDOWS) {
                        handleSignal("BREAK", quitHandler);
                    } else {
                        handleSignal("HUP", handler);
                        handleSignal("QUIT", quitHandler);
                    }
                }
                ApplicationLifecycleManager.application = application;
            } finally {
                stateLock.unlock();
            }

            final ShutdownHookThread shutdownHookThread = new ShutdownHookThread();
            Runtime.getRuntime().addShutdownHook(shutdownHookThread);
            application.start(args);
            //now we are started, we either run the main application or just wait to exit
            try {
                if (quarkusApplication != null) {
                    Set<Bean<?>> beans = CDI.current().getBeanManager().getBeans(quarkusApplication, new Any.Literal());
                    Bean<?> bean = null;
                    for (Bean<?> i : beans) {
                        if (i.getBeanClass() == quarkusApplication) {
                            bean = i;
                            break;
                        }
                    }
                    QuarkusApplication instance;
                    if (bean == null) {
                        instance = quarkusApplication.newInstance();
                    } else {
                        CreationalContext<?> ctx = CDI.current().getBeanManager().createCreationalContext(bean);
                        instance = (QuarkusApplication) CDI.current().getBeanManager().getReference(bean,
                                quarkusApplication, ctx);
                    }
                    int result = -1;
                    try {
                        result = instance.run(args);//TODO: argument filtering?
                    } finally {
                        stateLock.lock();
                        try {
                            //now we exit
                            if (exitCode == -1 && result != -1) {
                                exitCode = result;
                            }
                            shutdownRequested = true;
                            stateCond.signalAll();
                        } finally {
                            stateLock.unlock();
                        }
                    }
                } else {
                    while (!shutdownRequested) {
                        Thread.interrupted();
                        LockSupport.park(mainThread);
                    }
                }
            } catch (Exception e) {
                Logger.getLogger(Application.class).error("Error running Quarkus application", e);
                exitCode = 1;
            } finally {
                application.stop();
            }
        } finally {
            exit();
        }
    }

    public static int getExitCode() {
        return exitCode == -1 ? 0 : exitCode;
    }

    public  static void exit(int code) {
        stateLock.lock();
        try {
            if (shutdownRequested) {
                return;
            }
            if (code != -1) {
                exitCode = code;
            }
            shutdownRequested = true;
            stateCond.signalAll();
            LockSupport.unpark(mainThread);
        } finally {
            stateLock.unlock();
        }
    }

    public void waitForExit() {
        stateLock.lock();
        try {
            while (!shutdownRequested) {
                stateCond.awaitUninterruptibly();
            }
        } finally {
            stateLock.unlock();
        }
    }

    static class ShutdownHookThread extends Thread {

        ShutdownHookThread() {
            super("Shutdown thread");
            setDaemon(false);
        }

        @Override
        public void run() {
            stateLock.lock();
            shutdownRequested = true;
            try {
                stateCond.signalAll();
                Application.currentApplication().
            } finally {
                stateLock.unlock();
            }
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    private void exit() {
        stateLock.lock();
        try {
            System.out.flush();
            System.err.flush();
            stateCond.signalAll();
            // code beyond this point may not run
        } finally {
            stateLock.unlock();
        }
    }


    private static void handleSignal(final String signal, final SignalHandler handler) {
        try {
            Signal.handle(new Signal(signal), handler);
        } catch (IllegalArgumentException ignored) {
            // Do nothing
        }
    }
}
