/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.gateway.config.client.beans;

/** @author alex */
public interface Repeatable {
    // this: EditableConfigurationBean =>

    /** Set the index of this Configurable */
    void setIndex(int index);
    
    /** Get the index of this Configurable */
    int getIndex();
}
