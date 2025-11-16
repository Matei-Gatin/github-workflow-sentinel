package com.github.matei.sentinel.formatter;

import com.github.matei.sentinel.model.MonitoringEvent;

/**
 * Strategy interface for formatting {@link MonitoringEvent}s into output strings.
 * <p>
 * This interface allows different output formats to be plugged in without changing
 * the core monitoring logic. Implementations can produce console output, JSON, CSV,
 * or any other desired format.
 * </p>
 *
 * <h2>Design Pattern</h2>
 * This interface implements the <b>Strategy Pattern</b>, allowing the output format
 * to be chosen at runtime by injecting different implementations.
 *
 * <h2>Thread Safety</h2>
 * Implementations should be stateless and thread-safe, as they may be called
 * concurrently from multiple threads.
 *
 * <h2>Usage Example</h2>
 * <pre>{@code
 * EventFormatter formatter = new ConsoleEventFormatter();
 * MonitoringEvent event = new MonitoringEvent(...);
 * String output = formatter.format(event);
 * System.out.println(output);
 * }</pre>
 *
 * @see ConsoleEventFormatter
 * @see MonitoringEvent
 * @since 1.0
 */
public interface EventFormatter
{
    /**
     * Formats a monitoring event into a human-readable or machine-parseable string.
     * <p>
     * The format of the output depends on the implementation. For example:
     * <ul>
     *   <li>{@link ConsoleEventFormatter}: Human-readable console output</li>
     *   <li>JsonEventFormatter: JSON object string (hypothetical)</li>
     *   <li>CsvEventFormatter: CSV row (hypothetical)</li>
     * </ul>
     * </p>
     *
     * <p>Implementations should handle all fields gracefully, including optional
     * fields that may be {@code null} (e.g., job name, step name, duration).</p>
     *
     * @param event the monitoring event to format, must not be {@code null}
     * @return the formatted string, never {@code null}
     * @throws IllegalArgumentException if {@code event} is {@code null}
     */
    String format(MonitoringEvent event);
}
