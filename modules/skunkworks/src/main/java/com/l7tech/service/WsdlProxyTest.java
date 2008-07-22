package com.l7tech.service;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.HttpURLConnection;

/**
 * LAYER 7 TECHNOLOGIES, INC
 *
 * User: flascell
 * Date: Sep 16, 2003
 * Time: 2:11:32 PM
 * $Id$
 *
 *
 */
public class WsdlProxyTest {
    public static void main (String[] args) throws Exception {
        System.setProperty("javax.net.ssl.trustStore", System.getProperties().getProperty("user.home") + File.separator + ".l7tech" + File.separator + "trustStore");
        System.setProperty("javax.net.ssl.trustStorePassword", "password");

        //URL url = new URL("https://riker:8443/ssg/wsdl?serviceoid=4980736");
        URL url = new URL("https://riker:8443/ssg/wsil");
        //URL url = new URL("https://riker:8443/ssg/wsdl");
        //URL url = new URL("https://riker:8443/ssg/wsdl?serviceoid=5177347");

        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoOutput(true);
        // authentication for ssgadmin/ssgadminpasswd Basic c3NnYWRtaW46c3NnYWRtaW5wYXNzd2Q=
        // authentication for flascell/blahblah Basic Zmxhc2NlbGw6YmxhaGJsYWg=
        connection.setRequestProperty("Authorization", "Basic Zmxhc2NlbGw6YmxhaGJsYWg=");


        int res  = connection.getResponseCode();
        System.out.println("Response: " + res + "\n" + connection.getResponseMessage());

        InputStream is = connection.getInputStream();
        byte[] buf = new byte[2048];
        int read = is.read(buf);
        StringBuffer input = new StringBuffer();
        while (read > 0) {

            input.append(new String(buf, 0, read));
            read = is.read(buf);
        }
        System.out.println(input.toString());

    }
}
