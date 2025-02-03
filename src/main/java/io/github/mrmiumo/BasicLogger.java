package io.github.mrmiumo;

/**
 * Interface that allows any basic logger to be provided to Update4J.
 * To use your own logger, you may need to write a new class following
 * the "Adapter" pattern.
 * @author Miumo
 */
public interface BasicLogger {
    
    /**
     * Append a new line in the log file for a INFO level message.
     * INFO contains interesting events as user logs, ...
     * @param message to log.
     */
    public void info(String message);

    /**
     * Append a new line in the log file for a WARN level message.
     * WARN contains errors that are correctly handled by the program
     * @param message to log.
     */
    public void warn(String message);

    /**
     * Append a new line in the log file for a ERROR level message.
     * ERROR contains events that are not desirable but does not
     *     prevent the application from running fine.
     * @param message to log.
     */
    public void error(String message);
}
