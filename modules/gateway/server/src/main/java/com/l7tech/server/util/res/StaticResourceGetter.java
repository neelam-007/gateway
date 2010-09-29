/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.util.res;

import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.policy.variable.Syntax;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.CausedIOException;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.policy.StaticResourceInfo;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.server.policy.ServerPolicyException;

import java.io.IOException;
import java.text.ParseException;
import java.util.Map;

/**
 * A ResourceGetter that owns a single statically-configured value for the resource, with no network
 * communication needed, and corresponding to {@link com.l7tech.policy.StaticResourceInfo}.
 *
 * If the Document returned by the StaticResourceInfo contains a variable, then the resource will be created each
 * time getResource() is called. Otherwise the resource is created during construction and is returned for each call to
 * getResource().
 */
class StaticResourceGetter<R, M> extends ResourceGetter<R, M> {
    private final ResourceObjectFactory<R> rof;
    private R userObject;
    private boolean hasVariable;
    private StaticResourceInfo staticResourceInfo;

    StaticResourceGetter(Assertion assertion,
                         StaticResourceInfo ri,
                         ResourceObjectFactory<R> rof,
                         Audit audit)
            throws ServerPolicyException
    {
        super(audit);
        this.rof = rof;

        staticResourceInfo = ri;
        String doc = ri.getDocument();
        if (doc == null) throw new ServerPolicyException(assertion, "Empty static document");
        hasVariable = Syntax.getReferencedNames(doc).length > 0;
        if(!hasVariable){
            try {
                R userObject = rof.createResourceObject(doc);
                if (userObject == null)
                    throw new ServerPolicyException(assertion, "Unable to create static user object: ResourceObjectFactory returned null");
                this.userObject = userObject;
            } catch (ParseException e) {
                throw new ServerPolicyException(assertion, "Unable to create static user object: " + ExceptionUtils.getMessage(e), e);
            }
        }
    }

    @Override
    public void close() {
        if(!hasVariable){
            //can only attempt to close a single user object with current design.
            rof.closeResourceObject( userObject );
        }

    }

    @Override
    public R getResource(M notUsed, Map<String,Object> vars) throws IOException {
        if(!hasVariable) return userObject;

        final String doc = ExpandVariables.process(staticResourceInfo.getDocument(), vars, audit);
        final R newUserObject;
        try {
            newUserObject = rof.createResourceObject(doc);
        } catch (ParseException e) {
            throw new CausedIOException(ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        }
        if(newUserObject == null){
            throw new IOException("Unable to create static user object: ResourceObjectFactory returned null");
        }
        return newUserObject;
    }
}
