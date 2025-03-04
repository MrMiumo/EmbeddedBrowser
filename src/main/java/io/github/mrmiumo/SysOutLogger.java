package io.github.mrmiumo;

/**
 * Simple logger using System out to print its content. This logger
 * can be used to debug or when no other logger can be provided.
 */
public class SysOutLogger implements BasicLogger {

    /**
     * Creates a new logger that only use standard output. Can be
     * useful if no longer is provided or during development.
     */
    public SysOutLogger() {}

    @Override
    public void info(String message) {
        System.out.println("[INFO]  - " + message);
    }

    @Override
    public void warn(String message) {
        System.out.println("[WARN]  - " + message);
    }

    @Override
    public void error(String message) {
        System.out.println("[ERROR] - " + message);
    }

}
