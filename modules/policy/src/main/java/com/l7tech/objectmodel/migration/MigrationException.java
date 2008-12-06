package com.l7tech.objectmodel.migration;

import com.l7tech.objectmodel.ObjectModelException;

import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * @author jbufu
 */
public class MigrationException extends ObjectModelException {

    private static final Logger logger = Logger.getLogger(MigrationException.class.getName());

    private MigrationErrors errors = new MigrationErrors();

    public MigrationException() {}

    public MigrationException(MigrationErrors errors) {
        this.errors = errors;
    }

    public MigrationException(String message) {
        super(message);
    }

    public MigrationException(String message, MigrationErrors errors) {
        super(message);
        this.errors = errors;
    }

    public MigrationException(Throwable cause) {
        super(cause);
    }

    public MigrationException(String message, Throwable cause) {
        super(message, cause);
    }

    public MigrationErrors getErrors() {
        return errors;
    }

    /**
     * Holds a collection of errors (exceptions).
     *
     * Usefull for collecting all errors resulted from operations performed on multiple entities or headers
     * (to avoid multiple trial-and-error roundtrips).
     */
    public static class MigrationErrors {

        private Map<Object,Set<MigrationException>> errors = new HashMap<Object, Set<MigrationException>>();

        public void add(Object source, MigrationException e) {
            logger.log(Level.WARNING, e.getMessage());
            Set<MigrationException> errorsForSource = errors.get(source);
            if (errorsForSource == null) {
                errorsForSource = new HashSet<MigrationException>();
                errors.put(source, errorsForSource);
            }
            errorsForSource.add(e);
        }

        public void addAll(MigrationErrors other) {
            if (this == other) return;
            for (Object source : other.errors.keySet()) {
                for (MigrationException e : other.errors.get(source)) {
                    add(source, e);
                }
            }
        }

        public boolean isEmpty() {
            return errors.isEmpty();
        }
    }

}
