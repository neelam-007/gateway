package com.l7tech.credential.wss;

import com.l7tech.message.SoapRequest;
import com.l7tech.common.util.HexUtils;
import com.l7tech.credential.PrincipalCredentials;

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
    public static void main(String[] args) throws Exception {
        String fileToParse = "/home/flascell/dev/wssSamples/wssebasic1.xml";
        // String fileToParse = "/home/flascell/dev/wssSamples/noheader.xml";
        if (args.length > 0) fileToParse = args[0];

        FileInputStream fis = new FileInputStream(fileToParse);
        byte[] fileContents = HexUtils.slurpStream(fis, 4096);
        fis.close();

        SoapRequest soapRequest = new SoapRequest(null);
        soapRequest.setRequestXml(new String(fileContents));

        WssBasicCredentialFinder credsFinder = new WssBasicCredentialFinder();
        PrincipalCredentials creds = credsFinder.findCredentials(soapRequest);

        if (creds != null) {
            System.out.println("creds found");
            System.out.println("user login = " + creds.getUser().getLogin());
            System.out.println("passwd = " + new String(creds.getCredentials()));
        }


    }
}
