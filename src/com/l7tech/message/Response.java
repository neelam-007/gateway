/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.message;

import com.l7tech.policy.assertion.AssertionResult;

import java.io.*;
import java.util.Iterator;

/**
 * @author alex
 */
public interface Response extends Message {
    Reader getResponseReader() throws IOException;
    void addResult( AssertionResult result );
    Iterator results();
}
