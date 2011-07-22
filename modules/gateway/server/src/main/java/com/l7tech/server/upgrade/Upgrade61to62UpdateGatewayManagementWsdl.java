package com.l7tech.server.upgrade;

/**
 * Updated task for 6.2 Gateway Management internal services
 */
public class Upgrade61to62UpdateGatewayManagementWsdl extends InternalServiceWsdlUpgradeTask {

    public Upgrade61to62UpdateGatewayManagementWsdl() {
        super( "GatewayManagementAssertion",
               "Gateway Management",
               "file://__ssginternal/",
               "gateway-management-6_2.wsdl",
               "file://__ssginternal/gateway-management.wsdl",
               "com/l7tech/external/assertions/gatewaymanagement/server/serviceTemplate/"
        );
    }

}
