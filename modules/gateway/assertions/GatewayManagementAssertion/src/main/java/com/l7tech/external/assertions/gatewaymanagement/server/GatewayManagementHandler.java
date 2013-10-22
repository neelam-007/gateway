package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.message.Message;
import org.w3c.dom.Document;

import java.security.Principal;
import java.util.*;
import java.util.logging.Logger;

/**
 */
public class GatewayManagementHandler {

    private static final Logger logger = Logger.getLogger( GatewayManagementHandler.class.getName() );

    // Parameters
    public static final String SELECTOR_MAP = "selectorMap"; // Map<String,String> (selector,value)
    public static final String PAYLOAD = "payload";



    /**
     * Handles the gateway management request
     * @param resourceType  resource type
     * @param parameters parameters to operate on
     * @param action the operation ( "GET", .... )
     * @param user current user
     * @return  response message containing the results
     * @throws Exception
     */
    public Document handle(String resourceType, Map<String,Object> parameters, String action, Principal user)throws UnsupportedOperationException{
        return XmlUtil.createEmptyDocument();
    }

}
