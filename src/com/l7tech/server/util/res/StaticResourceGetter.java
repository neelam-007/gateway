/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.util.res;

import com.l7tech.common.audit.Audit;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.xml.ElementCursor;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.server.policy.ServerPolicyException;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * A ResourceGetter that owns a single statically-configured value for the resource, with no network
 * communication needed, and corresponding to {@link com.l7tech.policy.StaticResourceInfo}.
 */
class StaticResourceGetter<R> extends ResourceGetter<R> {
    private static final Logger logger = Logger.getLogger(StaticResourceGetter.class.getName());
    private final ResourceObjectFactory<R> rof;
    private final R userObject;

    StaticResourceGetter(Assertion assertion,
                         StaticResourceInfo ri,
                         ResourceObjectFactory<R> rof,
                         Audit audit)
            throws ServerPolicyException
    {
        super(audit);
        this.rof = rof;

        String doc = ri.getDocument();
        if (doc == null) throw new ServerPolicyException(assertion, "Empty static document");
        try {
            R userObject = rof.createResourceObject(doc);
            if (userObject == null)
                throw new ServerPolicyException(assertion, "Unable to create static user object: ResourceObjectFactory returned null");
            this.userObject = userObject;
        } catch (ParseException e) {
            throw new ServerPolicyException(assertion, "Unable to create static user object: " + ExceptionUtils.getMessage(e), e);
        }
    }

    @Override
    public void close() {
        rof.closeResourceObject( userObject );
    }

    @Override
    public R getResource(ElementCursor message, Map vars) throws IOException {
        return userObject;
    }
}
