package com.l7tech.server.event.system;

/**
 * Marker interface for "everyday" system events.
 *
 * <p>This marker identifies an SystemEvent as not worth auditing unless there are
 * important details. This is in contrast to basic SystemEvents which are always
 * audited.</p>
 *
 * @author Steve Jones
 */
public interface RoutineSystemEvent {
}
