package com.l7tech.credential.wss;

import com.l7tech.message.SoapRequest;
import com.l7tech.message.HttpSoapRequest;
import com.l7tech.message.HttpSoapResponse;
import com.l7tech.message.SoapResponse;
import com.l7tech.common.util.HexUtils;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.wss.WssBasic;
import com.l7tech.server.policy.assertion.credential.wss.ServerWssBasic;

import java.io.FileInputStream;

/**
 * User: flascell
 * Date: Aug 12, 2003
 * Time: 10:16:44 AM
 * $Id$
 *
 * Tests WsseBasicCredentialFinder against a file containing a soap request with a password in it (or not)
 */
public class WsseBasicCredentialFinderTest {
    // todo
}
