package com.github.matei.sentinel.util;


/**
 * Simple logging utility for console output with color support.
 * <p>
 * This utility provides three logging levels (INFO, WARN, ERROR) with
 * optional ANSI color formatting for better readability in terminal.
 * All output is sent to stderr to keep stdout clean for event data.
 * </p>
 *
 * <h2>Color Support</h2>
 * <ul>
 *   <li><b>INFO</b>: Blue (ℹ)</li>
 *   <li><b>WARN</b>: Yellow (⚠)</li>
 *   <li><b>ERROR</b>: Red (✖)</li>
 * </ul>
 *
 * <h2>Usage in CI/CD</h2>
 * For environments that don't support ANSI colors (e.g., CI logs),
 * call {@link #disableColor()} to use plain text formatting.
 *
 * <h2>Design Decision</h2>
 * We use a custom logger instead of Log4j/Logback because:
 * <ul>
 *   <li>Keeps dependencies minimal (no external logging framework)</li>
 *   <li>Sufficient for a simple CLI tool</li>
 *   <li>Easy to understand and customize</li>
 * </ul>
 *
 * <h2>Thread Safety</h2>
 * This class is thread-safe. The {@code colorEnabled} flag is not volatile,
 * but it's typically set once at startup before any concurrent access.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * // Enable color output (default)
 * Logger.info("Starting monitoring...");
 * Logger.warn("Rate limit approaching");
 * Logger.error("Authentication failed");
 *
 * // Disable colors for CI environment
 * Logger.disableColor();
 * Logger.info("Now using plain text");
 * }</pre>
 *
 * @since 1.0
 */
public class Logger
{
    private static boolean colorEnabled = true;

    /**
     * Logs an informational message
     */
    public static void info(String message)
    {
        if (colorEnabled)
        {
            System.err.println(Constants.ANSI_BLUE + "i " + message + Constants.ANSI_RESET);
        } else
        {
            System.err.println("[INFO] " + message);
        }
    }

    /**
     * Logs a warning message
     */
    public static void warn(String message)
    {
        if (colorEnabled)
        {
            System.err.println(Constants.ANSI_YELLOW + "⚠ " + message + Constants.ANSI_RESET);
        } else
        {
            System.err.println("[WARN] " + message);
        }
    }

    /**
     * Logs an error message
     */
    public static void error(String message)
    {
        if (colorEnabled)
        {
            System.err.println(Constants.ANSI_RED + "✖ " + message + Constants.ANSI_RESET);
        } else
        {
            System.err.println("[ERROR] " + message);
        }
    }

    /**
     * Disables colored output (useful for CI environments).
     */
    public static void disableColor()
    {
        colorEnabled = false;
    }
}
