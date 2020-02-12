package io.quarkus.runtime;

/**
 * The parameters that were passed to the application on the command line,
 * with no filtering applied.
 */
public interface RawCommandLineArguments {

    String[] getArguments();

}
