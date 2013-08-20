/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.server.identity.mapping;

import com.l7tech.identity.Identity;
import com.l7tech.identity.User;
import com.l7tech.identity.Group;
import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.objectmodel.AttributeHeader;
import com.l7tech.identity.mapping.IdentityMapping;

import java.text.MessageFormat;

/**
 * @author alex
 */
abstract class DefaultAttributeExtractor<MT extends IdentityMapping> implements AttributeExtractor {
    protected final Object[] EMPTY = new Object[0];

    protected MT mapping;

    protected DefaultAttributeExtractor(MT mapping) {
        this.mapping = mapping;
    }

    public Object[] extractValues(Identity identity) {
        AttributeHeader header = mapping.getAttributeConfig().getHeader();
        if (header == AttributeHeader.ID) return a(identity.getId());
        if (header == AttributeHeader.PROVIDER_GOID) return a(identity.getProviderId());
        if (header == AttributeHeader.NAME) return a(identity.getName());

        if (identity instanceof User) {
            User user = (User) identity;
            if (header == AttributeHeader.SUBJECT_DN) return a(user.getSubjectDn());
            if (header == AttributeHeader.EMAIL) return a(user.getEmail());
            if (header == AttributeHeader.LOGIN) return a(user.getLogin());
            if (header == AttributeHeader.FIRST_NAME) return a(user.getFirstName());
            if (header == AttributeHeader.LAST_NAME) return a(user.getLastName());
            if (header == AttributeHeader.DEPARTMENT) return a(user.getDepartment());
            return EMPTY;
        } else if (identity instanceof Group) {
            Group group = (InternalGroup) identity;
            if (header == AttributeHeader.DESCRIPTION) return a(group.getDescription());
            return EMPTY;
        } else {
            throw new IllegalArgumentException(MessageFormat.format("Identity is a {0}, not a User or Group", identity == null ? "null" : identity.getClass().getSimpleName()));
        }
    }

    protected Object[] a(Object toWrap) {
        return new Object[] { toWrap };
    }
}
