package io.quarkus.runtime;

/**
 * A class that exposes the command line arguments
 */
public class CommandLineArguments {

    private volatile static String[] commandLineArguments;

    public static void setCommandLineArguments(String[] params) {
        if (commandLineArguments != null) {
            //this method should only be called once, however it will be called multiple
            //times in dev mode so we just ignore multiple calls
            //this is to ensure extensions do not attempt to abuse this somehow
            return;
        }
        commandLineArguments = params;
    }

    /**
     *
     * @return The parameters that were passed on the command line
     */
    public static String[] getCommandLineArguments() {
        if (commandLineArguments == null) {
            return new String[0];
        }
        return commandLineArguments.clone();
    }
}
