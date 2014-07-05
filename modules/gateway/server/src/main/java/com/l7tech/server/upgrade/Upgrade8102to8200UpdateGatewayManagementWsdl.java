package com.l7tech.server.upgrade;

public class Upgrade8102to8200UpdateGatewayManagementWsdl extends InternalServiceWsdlUpgradeTask{

    public Upgrade8102to8200UpdateGatewayManagementWsdl() {
        super("GatewayManagementAssertion",
                "Gateway Management",
                "file://__ssginternal/",
                "gateway-management-8_2_00.wsdl",
                "file://__ssginternal/gateway-management-8_0.wsdl",
                "com/l7tech/external/assertions/gatewaymanagement/server/serviceTemplate/");
    }
}
