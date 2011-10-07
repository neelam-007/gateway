package com.l7tech.common.log;


import java.io.Serializable;
import java.util.logging.Filter;

/**
 * Interface for Serializable logging Filters.
 *
 * <p>Implementations should expect to be persisted and restored with an
 * updated version of their class (i.e.. persisted across versions), so
 * should probable set a serialVersionUID, etc.</p>
 *
 * <p>It is expected that implementations will be immutable.</p>
 */
public interface SerializableFilter extends Filter, Serializable {
}
