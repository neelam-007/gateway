package com.l7tech.server.upgrade;

public class Upgrade70to71UpdateGatewayManagementWsdl extends InternalServiceWsdlUpgradeTask{

    public Upgrade70to71UpdateGatewayManagementWsdl() {
        super("GatewayManagementAssertion",
                "Gateway Management",
                "file://__ssginternal/",
                "gateway-management-7_1.wsdl",
                "file://__ssginternal/gateway-management-6_1_5.wsdl",
                "com/l7tech/external/assertions/gatewaymanagement/server/serviceTemplate/");
    }
}
