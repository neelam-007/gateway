package com.l7tech.server.service;

import com.l7tech.common.http.GenericHttpClientFactory;
import com.l7tech.common.http.GenericHttpRequestParams;
import com.l7tech.common.http.HttpConstants;
import com.l7tech.common.http.SimpleHttpClient;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.service.ServiceAdmin;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.url.HttpObjectCache;
import com.l7tech.util.Config;
import com.l7tech.util.ConfigFactory;

import java.io.IOException;
import java.net.*;

/**
 * WSDL document access refactored from ServiceAdminImpl in 5.3
 */
public class ServiceDocumentResolver {                                                                                           

    //- PUBLIC

    public ServiceDocumentResolver( final GenericHttpClientFactory httpClientFactory ) {
        this.httpClientFactory = httpClientFactory;
    }

    public String resolveWsdlTarget(String url) throws IOException {
        return resolveDocumentTarget(url, ServiceAdmin.DownloadDocumentType.WSDL);
    }


    public String resolveDocumentTarget(String url, ServiceAdmin.DownloadDocumentType docType) throws IOException {
        return resolveDocumentTarget(url, docType, null);
    }

    public String resolveDocumentTarget(String url, ServiceAdmin.DownloadDocumentType docType, String modAssClusterProperty) throws IOException {
        final Config config = ConfigFactory.getCachedConfig();

        final int defaultMaxSize = config.getIntProperty( ServerConfigParams.PARAM_DOCUMENT_DOWNLOAD_MAXSIZE, HttpObjectCache.DEFAULT_DOWNLOAD_LIMIT);
        final int maxSize;
        switch(docType){
            case SCHEMA:
                maxSize = config.getIntProperty( ServerConfigParams.PARAM_SCHEMA_CACHE_MAX_SCHEMA_SIZE, defaultMaxSize);
                break;
            case WSDL:
                maxSize = config.getIntProperty( ServerConfigParams.PARAM_WSDL_MAX_DOWNLOAD_SIZE, defaultMaxSize);
                break;
            case XSL:
                maxSize = config.getIntProperty( ServerConfigParams.PARAM_XSL_MAX_DOWNLOAD_SIZE, defaultMaxSize);
                break;
            case MOD_ASS:
                if(modAssClusterProperty == null || modAssClusterProperty.trim().isEmpty()) throw new IllegalArgumentException("modAssClusterProperty cannot be null or empty");
                maxSize = config.getIntProperty(ClusterProperty.asServerConfigPropertyName(modAssClusterProperty), defaultMaxSize);
                break;
            default:
                maxSize = defaultMaxSize;
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
        if ( httpResponse.getStatus() < HttpConstants.STATUS_OK || httpResponse.getStatus() >= HttpConstants.STATUS_MULTIPLE_CHOICES ) {
            throw new IOException( "HTTP response failed with status " + httpResponse.getStatus() );
        }
        
        return httpResponse.getString();
    }

    //- PRIVATE
    
    private final GenericHttpClientFactory httpClientFactory;
}
