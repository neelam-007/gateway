/**
 * Copyright (C) 2008, Layer 7 Technologies Inc.
 * User: darmstrong
 * Date: Jun 3, 2009
 * Time: 2:45:58 PM
 */
package com.l7tech.policy.wsp;

/**
 * Where ever a TypeMapping implementation accepts paraemters representing the parameterized types used by the
 * Object being mapped, implement this interface so that this can be determined generically and the classes of the
 * mapped objects parameterized types retrieved
 *
 * Any find code which looks to match a TypeMapping to a Type, should check if the TypeMapping being considered is an
 * instance of this interface and if so it should call getMappedObjectsParameterizedClasses() and use the
 * Class [] representing the parameterized types as part of the find logic
 */
public interface ParameterizedMapping {

    /**
     * Get the classs of the parameterized types used by the Object being mapped
     * @return Class [] the classs of the parameterized types used by the Object being mapped. These
     * classes MUST be in the order defined by the mapped class
     */
    public Class [] getMappedObjectsParameterizedClasses();

}
