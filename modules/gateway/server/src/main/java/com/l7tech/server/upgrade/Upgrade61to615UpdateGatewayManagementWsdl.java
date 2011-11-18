package com.l7tech.server.upgrade;

/**
 * Updated task for 6.1.5 Gateway Management internal services
 */
public class Upgrade61to615UpdateGatewayManagementWsdl extends InternalServiceWsdlUpgradeTask {

    public Upgrade61to615UpdateGatewayManagementWsdl() {
        super( "GatewayManagementAssertion",
               "Gateway Management",
               "file://__ssginternal/",
               "gateway-management-6_1_5.wsdl",
               "file://__ssginternal/gateway-management.wsdl",
               "com/l7tech/external/assertions/gatewaymanagement/server/serviceTemplate/"
        );
    }

}
