/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.wsp;

import org.w3c.dom.Element;

/**
 * A WspTranslator knows how to translate a serialized policy from one SSG version to another.
 */
public interface WspTranslator {
    Element translatePolicy(Element input) throws InvalidPolicyStreamException;
}
