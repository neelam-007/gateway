package com.l7tech.skunkworks.http;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.Header;


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
    public static final String URL = "http://soong:8080/testb";
    
    public static void main(String[] args) throws Exception {
        HttpClient client = new HttpClient();
        PostMethod post = new PostMethod(URL);
        post.addRequestHeader("header1", "header1value");
        post.addRequestHeader("soapaction", "getsomeaction");

        post.addParameter("param2", "param2value");
        post.addParameter("param3", "param3value");
        post.addParameter("param4", "param4value");
        post.addParameter("param5", "param5value");

        /*post.setRequestBody("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
                "        <soapenv:Body>\n" +
                "                <sendSms xmlns=\"http://www.csapi.org/schema/parlayx/sms/send/v1_0/local\">\n" +
                "                        <destAddressSet/>\n" +
                "                        <senderName>sender</senderName>\n" +
                "                        <charging>charging</charging>\n" +
                "                        <message>message</message>\n" +
                "                </sendSms>\n" +
                "        </soapenv:Body>\n" +
                "</soapenv:Envelope>");*/

        int res = client.executeMethod(post);
        System.out.println("Post retulted in status " + res);
        Header[] rheaders = post.getResponseHeaders();
        System.out.println("Response Headers:");
        for (Header h : rheaders) {
            System.out.println("\t" + h.getName() + " : " + h.getValue());
        }
        System.out.println("Response payload: " + new String(post.getResponseBody()));
    }
}
