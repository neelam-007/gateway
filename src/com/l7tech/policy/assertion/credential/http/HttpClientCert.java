/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.credential.http;

import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.proxy.datamodel.SsgKeyStoreManager;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.credential.http.HttpClientCertCredentialFinder;
import com.l7tech.server.policy.assertion.credential.http.ServerHttpCredentialSource;
import org.apache.log4j.Category;

/**
 * @author alex
 * @version $Revision$
 */
public class HttpClientCert extends HttpCredentialSourceAssertion {
}
