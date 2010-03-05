package com.l7tech.server.service;

import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.SimpleHttpClient;
import com.l7tech.server.util.HttpClientFactory;
import java.io.IOException;
import java.net.*;

/**
 * WSDL document access refactored from ServiceAdminImpl in 5.3
 */
public class ServiceDocumentResolver {                                                                                           

    //- PUBLIC

    public ServiceDocumentResolver( final HttpClientFactory httpClientFactory) {
        this.httpClientFactory = httpClientFactory;
    }

    public String resolveWsdlTarget(String url) throws IOException {

        //10mb limit on all downloads
        final SimpleHttpClient simpleHttpClient = new SimpleHttpClient(httpClientFactory.createHttpClient(), 10 * 1024 * 1024);

        final URL urlTarget = new URL(url);
        final GenericHttpRequestParams requestParams = new GenericHttpRequestParams(urlTarget);
        // support for passing username and password in the url from the ssm
        final String userinfo = urlTarget.getUserInfo();
        if (userinfo != null && userinfo.indexOf(':') > -1) {
            String login = userinfo.substring(0, userinfo.indexOf(':'));
            String passwd = userinfo.substring(userinfo.indexOf(':') + 1, userinfo.length());
            requestParams.setPasswordAuthentication(new PasswordAuthentication(login, passwd.toCharArray()));
        }

        final SimpleHttpClient.SimpleHttpResponse httpResponse = simpleHttpClient.get(requestParams);
        return httpResponse.getString();
    }

    //- PRIVATE
    
    private final HttpClientFactory httpClientFactory;
}
