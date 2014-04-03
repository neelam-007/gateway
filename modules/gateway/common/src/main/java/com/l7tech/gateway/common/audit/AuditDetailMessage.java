/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */
package com.l7tech.gateway.common.audit;

import java.util.logging.Level;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;

/**
 * A record pertaining to the string table for AuditDetail records
 */
public class AuditDetailMessage {
    private static final Set<Hint> NO_HINTS = Collections.emptySet();
    //- PUBLIC

    public AuditDetailMessage(int id, Level level, String message) {
        this(id, level, message, NO_HINTS);
    }

    public AuditDetailMessage(int id, Level level, String message, boolean saveRequest, boolean saveResponse) {
        this(id, level, message, buildHints(saveRequest, saveResponse));
    }

    public AuditDetailMessage(AuditDetailMessage message, Level newLevel) {
        this(message.id, newLevel, message.message, message.hints);
    }

    public int getId() {
        return id;
    }

    public String getLevelName() {
        return level.getName();
    }

    public Level getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public Set<Hint> getHints() {
        return hints;
    }

    public static class Hint {
        private String hintId;

        private Hint(String hintId) {
            this.hintId = hintId;
        }

        public static Hint getHint(String hintId) {
            if(hintId==null) throw new IllegalArgumentException("hintId must not be null");
            return new Hint(hintId.intern());
        }

        public static Hint getHint(Hint hint) {
            if(hint ==null) throw new IllegalArgumentException("hint must not be null");
            return new Hint(hint.hintId.intern());
        }

        public boolean equals(Object obj) {
            boolean equal = false;

            if(obj == this) {
                equal = true;
            }
            else if(obj instanceof Hint) {
                Hint other = (Hint) obj;
                equal = this.hintId.equals(other.hintId);
            }

            return equal;
        }

        public int hashCode() {
            return 13 & this.hintId.hashCode();
        }
    }

    //- PRIVATE

    private final int id;
    private final Level level;
    private final String message;
    private final Set<Hint> hints;

    private AuditDetailMessage(int id, Level level, String message, Set<Hint> hints) {
        this.id = id;
        this.level = level;
        this.message = message;
        this.hints = hints;
    }

    private static Set<Hint> buildHints(boolean saveRequest, boolean saveResponse) {
        Set<Hint> hints = NO_HINTS;

        if(saveRequest || saveResponse) {
            hints = new HashSet<Hint>(2);
            if(saveRequest) hints.add(Hint.getHint("MessageProcessor.saveRequest"));
            if(saveResponse) hints.add(Hint.getHint("MessageProcessor.saveResponse"));
            hints = Collections.unmodifiableSet(hints);
        }

        return hints;
    }
}
