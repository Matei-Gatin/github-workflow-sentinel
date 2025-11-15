package com.github.Matei.sentinel.formatter;

import com.github.Matei.sentinel.model.MonitoringEvent;

/**
 * Strategy interface for formatting monitoring events.
 * Allows different output formats (console, JSON, CSV) without changing core logic.
 */
public interface EventFormatter {

    /**
     * Formats a monitoring event into a string representation.
     *
     * @param event the event to format
     * @return formatted string representation of the event
     */
    String format(MonitoringEvent event);
}
