/**
 * Copyright (C) 2006 Layer 7 Technologies Inc.
 */
package com.l7tech.server.security.rbac;

import com.l7tech.common.security.rbac.EntityType;
import com.l7tech.common.security.rbac.OperationType;
import com.l7tech.common.security.rbac.MethodStereotype;
import com.l7tech.objectmodel.Entity;

/**
 * @author alex
*/
class CheckInfo {
    CheckInfo(String mname, EntityType[] checkTypes, OperationType checkOperation,
              MethodStereotype stereotype, int checkRelevantArg, String otherOperationName)
    {
        this.mname = mname;
        this.types = checkTypes;
        this.operation = checkOperation;
        this.stereotype = stereotype;
        this.relevantArg = checkRelevantArg;
        this.otherOperationName = otherOperationName;
    }

    final String mname;
    final EntityType[] types;
    final MethodStereotype stereotype;
    final int relevantArg;
    final String otherOperationName;

    OperationType operation;
    CheckBefore before;
    CheckAfter after;
    long oid = Entity.DEFAULT_OID;
    Entity entity = null;
}
