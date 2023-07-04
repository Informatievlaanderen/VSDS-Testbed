package be.vlaanderen.ldes.gitb;

import com.gitb.core.LogLevel;

/**
 * Convenience interface to describe an implementation that will add entries to the test session log.
 */
public interface TestBedLogger {

    /**
     * Log a message on the test session log.
     *
     * @param message The message to log.
     * @param level The severity level.
     */
    void log(String message, LogLevel level);

}
