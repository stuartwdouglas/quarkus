package io.quarkus.runtime;

import org.jboss.logging.Logger;

import io.quarkus.launcher.QuarkusLauncher;

/**
 * The entry point for applications that use a main method.
 */
public class Quarkus {

    private static final Logger log = Logger.getLogger(Quarkus.class);

    private static volatile Application application;

    public static void start(Class<? extends QuarkusApplication> quarkusApplication, String... args) {
        try {
            //production mode path
            //we already have an application, run it directly
            Class<? extends Application> appClass = (Class<? extends Application>) Class.forName(Application.APP_CLASS_NAME);
            application = appClass.newInstance();
            application.run(quarkusApplication, args);
            System.exit(application.getExitCode());
        } catch (ClassNotFoundException e) {
            //ignore, this happens when running in dev mode
        } catch (IllegalAccessException | InstantiationException e) {
            throw new RuntimeException("Unable to create application", e);
        } catch (Exception e) {
            //TODO: exception mappers
            log.error("Error running Quarkus", e);
            System.exit(1);
        }

        //dev mode path, i.e. launching from the IDE

        //some trickery, get the class that has invoked us, and use this to figure out the
        //classes root
        String callingClass = Thread.currentThread().getStackTrace()[2].getClassName();
        QuarkusLauncher.launch(callingClass);

    }

    /**
     * Starts a quarkus application, that will run until it either recieves a signal (e.g. user presses ctrl+c)
     * or one of the exit methods is called.
     * 
     * @param args The command line parameters
     */
    public static void start(String... args) {
        start(null, args);
    }

    /**
     * Exits the application in an async manner. Calling this method
     * will initiate the Quarkus shutdown process, and then immediately return.
     * 
     * @param code The exit code. This may be overridden if an exception occurs on shutdown
     */
    public static void exit(int code) {

    }

    /**
     * Method that will block until the Quarkus shutdown process is initiated.
     */
    public static void waitForExit() {

   }
}
