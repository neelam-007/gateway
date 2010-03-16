package com.l7tech.server.service;

import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.SimpleHttpClient;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.service.ServiceAdminPublic;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.url.HttpObjectCache;
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
        return resolveDocumentTarget(url, ServiceAdminPublic.DownloadDocumentType.WSDL);
    }


    public String resolveDocumentTarget(String url, ServiceAdminPublic.DownloadDocumentType docType) throws IOException {
        return resolveDocumentTarget(url, docType, null);
    }

    public String resolveDocumentTarget(String url, ServiceAdminPublic.DownloadDocumentType docType, String modAssClusterProperty) throws IOException {
        final ServerConfig serverConfig = ServerConfig.getInstance();

        final int maxSize;
        switch(docType){
            case SCHEMA:
                maxSize = serverConfig.getIntProperty(ServerConfig.PARAM_SCHEMA_CACHE_MAX_SCHEMA_SIZE, HttpObjectCache.DEFAULT_DOWNLOAD_LIMIT);
                break;
            case WSDL:
                maxSize = serverConfig.getIntProperty(ServerConfig.PARAM_WSDL_MAX_DOWNLOAD_SIZE, HttpObjectCache.DEFAULT_DOWNLOAD_LIMIT);
                break;
            case XSL:
                maxSize = serverConfig.getIntProperty(ServerConfig.PARAM_XSL_MAX_DOWNLOAD_SIZE, HttpObjectCache.DEFAULT_DOWNLOAD_LIMIT);
                break;
            case MOD_ASS:
                if(modAssClusterProperty == null || modAssClusterProperty.trim().isEmpty()) throw new IllegalArgumentException("modAssClusterProperty cannot be null or empty");
                maxSize = serverConfig.getIntProperty(ClusterProperty.asServerConfigPropertyName(modAssClusterProperty), HttpObjectCache.DEFAULT_DOWNLOAD_LIMIT);
                break;
            default:
                maxSize = HttpObjectCache.DEFAULT_DOWNLOAD_LIMIT;
        }

        final SimpleHttpClient simpleHttpClient = new SimpleHttpClient(httpClientFactory.createHttpClient(), maxSize);

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
