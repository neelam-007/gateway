/*
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.objectmodel;

import com.l7tech.common.util.CollectionUpdate;

/**
 * Provides a more discriminating <code>equals</code> method for
 * {@link ServiceHeader} objects. While {@link ServiceHeader#equals} is based on
 * ID only, this differentiator is based on all instance fields.
 *
 * <p>Default use of {@link ServiceHeader#equals} was the cause of
 * <a href="http://sarek/bugzilla/show_bug.cgi?id=4927">Bug 4927</a>.
 *
 * @since SecureSpan 5.0
 * @author rmak
 */
public class ServiceHeaderDifferentiator implements CollectionUpdate.Differentiator<ServiceHeader> {
    public boolean equals(ServiceHeader a, ServiceHeader b) {
        if (a == b) return true;
        if (a == null || b == null) {
            return a == b;
        }
        if (a.getName() == null ? b.getName() != null : !a.getName().equals(b.getName()))
            return false;
        if (a.getDescription() == null ? b.getDescription() != null : !a.getDescription().equals(b.getDescription()))
            return false;
        if (a.getStrId() == null ? b.getStrId() != null : !a.getStrId().equals(b.getStrId()))
            return false;
        if (a.getType() == null ? b.getType() != null : !a.getType().equals(b.getType()))
            return false;
        if (a.isDisabled() != b.isDisabled()) return false;
        if (a.isSoap() != b.isSoap()) return false;
        if (a.getDisplayName() == null ? b.getDisplayName() != null : !a.getDisplayName().equals(b.getDisplayName()))
            return false;

        return true;
    }

    public int hashCode(ServiceHeader o) {
        int result;
        result = (o.getType() == null ? 0 : o.getType().hashCode());
        result = 31 * result + (o.getDescription() == null ? 0 : o.getDescription().hashCode());
        result = 31 * result + (o.getStrId() == null ? 0 : o.getStrId().hashCode());
        result = 31 * result + (o.getName() == null ? 0 : o.getName().hashCode());
        result = 31 * result + (o.isSoap() ? 1 : 0);
        result = 31 * result + (o.isDisabled() ? 1 : 0);
        result = 31 * result + (o.getDisplayName() == null ? 0 : o.getDisplayName().hashCode());
        return result;
    }
}
