package com.l7tech.server.upgrade;

public class Upgrade71to80UpdateGatewayManagementWsdl extends InternalServiceWsdlUpgradeTask{

    public Upgrade71to80UpdateGatewayManagementWsdl() {
        super("GatewayManagementAssertion",
                "Gateway Management",
                "file://__ssginternal/",
                "gateway-management-8_0.wsdl",
                "file://__ssginternal/gateway-management-7_1.wsdl",
                "com/l7tech/external/assertions/gatewaymanagement/server/serviceTemplate/");
    }
}
