package com.l7tech.skunkworks.http;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;


/**
 * A simple http client that sends http headers and displays response's payload
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 15, 2007<br/>
 */
public class HeaderSender {
    //public static final String URL = "http://soong:8080/test/blahness?param1=value1";
    public static final String URL = "http://soong:8080/sms";
    
    public static void main(String[] args) throws Exception {
        HttpClient client = new HttpClient();
        PostMethod post = new PostMethod(URL);
        post.addRequestHeader("header1", "header1value");
        post.addRequestHeader("soapaction", "getsomeaction");

        int res = client.executeMethod(post);
        System.out.println("Post retulted in status " + res);
        System.out.println("Response payload: " + new String(post.getResponseBody()));
    }
}
