package io.quarkus.runtime;

import java.util.function.Consumer;

import org.jboss.logging.Logger;

import io.quarkus.bootstrap.app.CuratedApplication;
import io.quarkus.launcher.QuarkusLauncher;

/**
 * The entry point for applications that use a main method.
 */
public class Quarkus {

    //WARNING: this is too early to inject a logger
    //private static final Logger log = Logger.getLogger(Quarkus.class);

    /**
     * Runs a quarkus application, that will run until the provided {@link QuarkusApplication} has completed.
     *
     * @param quarkusApplication The application to run, or null
     * @param args The command line parameters
     */
    public static void run(Class<? extends QuarkusApplication> quarkusApplication, String... args) {
        run(quarkusApplication, new Consumer<Integer>() {
            @Override
            public void accept(Integer integer) {
                System.exit(integer);
            }
        }, args);
    }

    /**
     * Runs a quarkus application, that will run until the provided {@link QuarkusApplication} has completed.
     *
     * @param quarkusApplication The application to run, or null
     * @param exitHandler The handler that is called with the exit code when the application has finished
     * @param args The command line parameters
     */
    public static void run(Class<? extends QuarkusApplication> quarkusApplication, Consumer<Integer> exitHandler,
            String... args) {
        try {
            //production mode path
            //we already have an application, run it directly
            Class<? extends Application> appClass = (Class<? extends Application>) Class.forName(Application.APP_CLASS_NAME);
            Application application = appClass.newInstance();
            application.run(quarkusApplication, args);
            exitHandler.accept(application.getExitCode());
            return;
        } catch (ClassNotFoundException e) {
            //ignore, this happens when running in dev mode
        } catch (Exception e) {
            //TODO: exception mappers
            Logger.getLogger(Quarkus.class).error("Error running Quarkus", e);
            exitHandler.accept(1);
            return;
        }

        //dev mode path, i.e. launching from the IDE
        //this is not the quarkus:dev path as it will augment before
        //calling this method

        //some trickery, get the class that has invoked us, and use this to figure out the
        //classes root
        String callingClass = Thread.currentThread().getStackTrace()[2].getClassName();
        CuratedApplication app = QuarkusLauncher.launch(callingClass);

    }

    /**
     * Starts a quarkus application, that will run until it either receives a signal (e.g. user presses ctrl+c)
     * or one of the exit methods is called.
     *
     * This method does not return, as System.exit() is called after the application is finished.
     * 
     * @param args The command line parameters
     */
    public static void run(String... args) {
        run(null, args);
    }

    /**
     * Exits the application in an async manner. Calling this method
     * will initiate the Quarkus shutdown process, and then immediately return.
     *
     * This method will unblock the {@link #waitForExit()} method.
     *
     * Note that if the main thread is executing a Quarkus application this will only take
     * effect if {@link #waitForExit()} has been called, otherwise the application will continue
     * to execute (i.e. this does not initiate the shutdown process, it just signals the main
     * thread that the application is done so that shutdown can run when the main thread returns).
     *
     * The error code supplied here will override the value returned from the main application.
     * 
     * @param code The exit code. This may be overridden if an exception occurs on shutdown
     */
    public static void asyncExit(int code) {
        Application.currentApplication().exit(code);
    }

    /**
     * Exits the application in an async manner. Calling this method
     * will initiate the Quarkus shutdown process, and then immediately return.
     *
     * This method will unblock the {@link #waitForExit()} method.
     *
     * Note that if the main thread is executing a Quarkus application this will only take
     * effect if {@link #waitForExit()} has been called, otherwise the application will continue
     * to execute (i.e. this does not initiate the shutdown process, it just signals the main
     * thread that the application is done so that shutdown can run when the main thread returns).
     *
     */
    public static void asyncExit() {
        Application.currentApplication().exit(-1);
    }

    /**
     * Method that will block until the Quarkus shutdown process is initiated.
     *
     * Note that this unblocks as soon as the shutdown process starts, not after it
     * has finished.
     *
     * {@link QuarkusApplication} implementations that wish to run some logic on startup, and
     * then run should call this method.
     */
    public static void waitForExit() {
        Application.currentApplication().waitForExit();

    }
}
