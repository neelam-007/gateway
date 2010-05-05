/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion;

import java.util.logging.Level;

import static com.l7tech.policy.assertion.AssertionMetadata.*;

/**
 * @author mike
 */
public class AuditAssertion extends Assertion {
    private Level level;
    private boolean saveRequest = false;
    private boolean saveResponse = false;
    private boolean changeSaveRequest = true;
    private boolean changeSaveResponse = true;

    /**
     * Create a new AuditAssertion with the default level.
     */
    public AuditAssertion() {
        level = Level.WARNING;
    }

    /**
     * Create a new AuditAssertion with the specified level name.
     * @param level
     * @throws IllegalArgumentException
     */
    public AuditAssertion(String level) throws IllegalArgumentException {
        super();
        setLevel(level);
    }

    public AuditAssertion(Level level, boolean changeSaveRequest, boolean saveRequest, boolean changeSaveResponse, boolean saveResponse) {
        this.level = level;
        this.saveRequest = saveRequest;
        this.saveResponse = saveResponse;
        this.changeSaveRequest = changeSaveRequest;
        this.changeSaveResponse = changeSaveResponse;
    }

    /**
     * Get the name of the current level.
     * @return the level name.  Never null.
     */
    public String getLevel() {
        if (level == null)
            throw new IllegalStateException("level is currently null");
        return level.getName();
    }

    /**
     * Set the Level, using its string name.
     * @param level the non-localized name of the Level.  May not be null.
     * @throws IllegalArgumentException if the Level is null or isn't a valid Level name.
     */
    public void setLevel(String level) throws IllegalArgumentException {
        if (level == null)
            throw new IllegalArgumentException("Level may not be null");
        for ( int i = 0; i < ALLOWED_LEVELS.length; i++ ) {
            if (level.equals(ALLOWED_LEVELS[i])) {
                this.level = Level.parse(level);
                return;
            }
        }
        throw new IllegalArgumentException("Unsupported level: " + level);
    }

    public boolean isSaveRequest() {
        return saveRequest;
    }

    public void setSaveRequest(boolean saveRequest) {
        this.saveRequest = saveRequest;
    }

    public boolean isSaveResponse() {
        return saveResponse;
    }

    public void setSaveResponse(boolean saveResponse) {
        this.saveResponse = saveResponse;
    }

    public boolean isChangeSaveRequest() {
        return changeSaveRequest;
    }

    public void setChangeSaveRequest(boolean changeSaveRequest) {
        this.changeSaveRequest = changeSaveRequest;
    }

    public boolean isChangeSaveResponse() {
        return changeSaveResponse;
    }

    public void setChangeSaveResponse(boolean changeSaveResponse) {
        this.changeSaveResponse = changeSaveResponse;
    }

    public static final String[] ALLOWED_LEVELS = new String[] {
        Level.FINEST.getName(),
        Level.FINER.getName(),
        Level.FINE.getName(),
        Level.INFO.getName(),
        Level.WARNING.getName(),
    };

    private final static String baseName = "Audit Messages in Policy";

    final static AssertionNodeNameFactory policyNameFactory = new AssertionNodeNameFactory<AuditAssertion>(){
        @Override
        public String getAssertionName( final AuditAssertion assertion, final boolean decorate) {
            return baseName;
        }
    };

    @Override
    public AssertionMetadata meta() {
        DefaultAssertionMetadata meta = defaultMeta();

        meta.put(PALETTE_FOLDERS, new String[]{"audit"});

        meta.put(SHORT_NAME, baseName);
        meta.put(DESCRIPTION, "Set the audit level for the policy and optionally record request and response messages.");

        meta.put(PALETTE_NODE_ICON, "com/l7tech/console/resources/Edit16.gif");

        meta.put(POLICY_NODE_NAME_FACTORY, policyNameFactory);

        meta.put(PROPERTIES_ACTION_CLASSNAME, "com.l7tech.console.action.AuditAssertionPropertiesAction");
        meta.put(PROPERTIES_ACTION_NAME, "Audit Properties");
        return meta;
    }
}
