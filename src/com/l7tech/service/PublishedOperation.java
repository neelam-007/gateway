/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.service;

import com.l7tech.policy.assertion.Assertion;

import java.util.List;

/**
 * @author alex
 * @version $Revision$
 */
public class PublishedOperation extends Operation {
    public PublishedOperation( String name, List inParams, Parameter returnParam ) {
        this(name,inParams,returnParam,null);
    }

    public PublishedOperation( String name, List inParams, Parameter returnParam, Assertion rootAssertion ) {
        super( name, inParams, returnParam );
        _rootAssertion = rootAssertion;
    }

    public PublishedOperation( String name, List inParams, List outParams, Parameter returnParam, String description ) {
        this( name, inParams, outParams, returnParam, description, null );
    }

    public PublishedOperation( String name, List inParams, List outParams, Parameter returnParam, String description, Assertion rootAssertion ) {
        super( name, inParams, outParams, returnParam, description );
        _rootAssertion = rootAssertion;
    }

    /** Default constructor. Only for Hibernate, don't call! */
    public PublishedOperation() { }

    public Assertion getRootAssertion() {
        return _rootAssertion;
    }

    public void setRootAssertion( Assertion rootAssertion ) {
        _rootAssertion = rootAssertion;
    }

    protected Assertion _rootAssertion;
}
