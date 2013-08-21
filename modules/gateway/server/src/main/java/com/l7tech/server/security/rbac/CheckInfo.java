/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.rbac;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.objectmodel.Entity;

import java.io.Serializable;

/**
 * @author alex
*/
class CheckInfo {
    CheckInfo(String methodName, EntityType[] checkTypes, OperationType checkOperation,
              MethodStereotype stereotype, int checkRelevantArg, String otherOperationName)
    {
        this.methodName = methodName;
        this.types = checkTypes;
        this.operation = checkOperation;
        this.stereotype = stereotype;
        this.relevantArg = checkRelevantArg;
        this.otherOperationName = otherOperationName;
    }

    final String methodName;
    final EntityType[] types;
    final MethodStereotype stereotype;
    final int relevantArg;
    String otherOperationName;

    OperationType operation;
    private CheckBefore before;
    private CheckAfter after;
    Serializable id = null;
    Entity entity = null;
    Iterable<Entity> entities = null;


    CheckBefore getBefore() {
        return before;
    }

    void setBefore(CheckBefore before) {
        this.before = before;
    }

    CheckAfter getAfter() {
        return after;
    }

    void setAfter(CheckAfter after) {
        this.after = after;
    }
}
