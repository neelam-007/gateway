/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.policy.assertion.credential.http;

import com.l7tech.credential.CredentialFinderException;
import com.l7tech.credential.http.HttpDigestCredentialFinder;
import com.l7tech.message.Request;
import com.l7tech.message.Response;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.proxy.datamodel.PendingRequest;
import com.l7tech.proxy.datamodel.Ssg;
import com.l7tech.server.policy.assertion.credential.http.ServerHttpCredentialSource;

/**
 * @author alex
 * @version $Revision$
 */
public class HttpDigest extends HttpCredentialSourceAssertion {
}
