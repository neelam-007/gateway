/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.fed;

/**
 * @author alex
 * @version $Revision$
 */
public class VirtualGroup extends FederatedGroup {
    private String samlDomain;
    private String samlEmailSuffix;
    private String x500PartialDn;
}
