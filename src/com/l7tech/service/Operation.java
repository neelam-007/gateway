/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service;

import java.util.List;

/**
 * Represents a SOAP Operation (i.e. a method) that can be called by clients.
 *
 * Shared by both PublishedService and ProtectedService.
 *
 * @author alex
 * @version $Revision$
 */
public class Operation {
    /**
     * Constructs a minimal Operation with a name, input Parameters and a return Parameter.
     *
     * @param name The name of the method represented by this Operation.
     * @param inParams A List of Parameters that are expected as inputs to the Operation.
     * @param returnParam A Parameter that represents the value returned by the Operation.
     */
    public Operation( String name, List inParams, Parameter returnParam ) {
        this( name, inParams, null, returnParam, null );
    }

    /**
     * Constructs an Operation fully.
     *
     * @param name The name of the method represented by this Operation.
     * @param inParams A List of Parameters that are expected as inputs to the Operation.
     * @param outParams A List of Parameters that are expected as outputs from the Operation.
     * @param returnParam A Parameter that represents the value returned by the Operation.
     * @param description A textual description
     */
    public Operation( String name, List inParams, List outParams, Parameter returnParam, String description ) {
        _name = name;
        _inParams = inParams;
        _outParams = outParams;
        _return = returnParam;
        _description = description;
    }

    /** Default constructor. Only for Hibernate, don't call! */
    public Operation() { }

    /** Gets the name of the method represented by this Operation. */
    public String getName() {
        return _name;
    }

    /** Sets the name of the method represented by this Operation. */
    public void setName(String name) {
        _name = name;
    }

    /** Retrieves the List of input parameters for this Operation. */
    public List getInParams() {
        return _inParams;
    }

    /** Sets the List of input parameters for this Operation. */
    public void setInParams(List inParams) {
        _inParams = inParams;
    }

    /** Gets this List of output parameters for this Operation. */
    public List getOutParams() {
        return _outParams;
    }

    /** Sets the List of output parameters for this Operation. */
    public void setOutParams(List outParams) {
        _outParams = outParams;
    }

    /** Gets the description of this Operation. */
    public String getDescription() {
        return _description;
    }

    /** Sets the description of this Operation. */
    public void setDescription(String description) {
        _description = description;
    }

    /** Gets the return Parameter. */
    public Parameter getReturn() {
        return _return;
    }

    /** Sets the return Parameter. */
    public void setReturn(Parameter aReturn) {
        _return = aReturn;
    }

    protected String _name;
    protected List _inParams;
    protected List _outParams;
    protected Parameter _return;
    protected String _description;
}
