package com.l7tech.server.util;

import org.junit.Test;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.message.Message;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.xml.SoapFaultLevel;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.util.Pair;

/**
 *
 */
public class SoapFaultManagerTest {

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    @Test
    public void testSoap12ExceptionFault() throws Exception {
        SoapFaultManager sfm = new SoapFaultManager();
        String fault = sfm.constructExceptionFault( new RuntimeException("Something went wrong"), getSoap12PEC(false) );
        System.out.println(fault);
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    @Test
    public void testSoap11ExceptionFault() throws Exception {
        SoapFaultManager sfm = new SoapFaultManager();
        String fault = sfm.constructExceptionFault( new RuntimeException("Something went wrong"), getSoap11PEC(false) );
        System.out.println(fault);
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    @Test
    public void testSoap12PolicyVersionFault() throws Exception {
        SoapFaultManager sfm = new SoapFaultManager();
        String fault = sfm.constructExceptionFault( new RuntimeException("Something went wrong"), getSoap12PEC(true) );
        System.out.println(fault);
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    @Test
    public void testSoap11PolicyVersionFault() throws Exception {
        SoapFaultManager sfm = new SoapFaultManager();
        String fault = sfm.constructExceptionFault( new RuntimeException("Something went wrong"), getSoap11PEC(true) );
        System.out.println(fault);
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    @Test
    public void testSoap12GenericFault() throws Exception {
        SoapFaultManager sfm = new SoapFaultManager();
        SoapFaultLevel level = new SoapFaultLevel();
        level.setLevel(SoapFaultLevel.GENERIC_FAULT);
        Pair<ContentTypeHeader,String> fault = sfm.constructReturningFault( level, getSoap12PEC(false) );
        System.out.println(fault);
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    @Test
    public void testSoap11GenericFault() throws Exception {
        SoapFaultManager sfm = new SoapFaultManager();
        SoapFaultLevel level = new SoapFaultLevel();
        level.setLevel(SoapFaultLevel.GENERIC_FAULT);
        Pair<ContentTypeHeader, String> fault = sfm.constructReturningFault( level, getSoap11PEC(false) );
        System.out.println(fault);
    }

    private PolicyEnforcementContext getSoap12PEC(final boolean wrongPolicyVersion) {
        PolicyEnforcementContext pec = new PolicyEnforcementContext( new Message(), new Message() ){
            @Override
            public Object getVariable(String name) throws NoSuchVariableException {
                return "http://myservicehost/myservice";
            }

            @Override
            public boolean isRequestClaimingWrongPolicyVersion() {
                return wrongPolicyVersion;
            }
        };
        PublishedService service = new PublishedService();
        service.setWsdlXml(WSDL_WAREHOUSE_SOAP12);
        pec.setService(service);

        return pec;
    }

    private PolicyEnforcementContext getSoap11PEC(final boolean wrongPolicyVersion) {
        return new PolicyEnforcementContext( new Message(), new Message() ){
            @Override
            public Object getVariable(String name) throws NoSuchVariableException {
                return "http://myservicehost/myservice";
            }

            @Override
            public boolean isRequestClaimingWrongPolicyVersion() {
                return wrongPolicyVersion;
            }
        };
    }

    private static final String WSDL_WAREHOUSE_SOAP12 =
            "<wsdl:definitions name=\"Warehouse\" targetNamespace=\"http://warehouse.acme.com/ws\" xmlns:http=\"http://schemas.xmlsoap.org/wsdl/http/\" xmlns:mime=\"http://schemas.xmlsoap.org/wsdl/mime/\" xmlns:s=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap12=\"http://schemas.xmlsoap.org/wsdl/soap12/\" xmlns:soapenc=\"http://schemas.xmlsoap.org/soap/encoding/\" xmlns:tm=\"http://microsoft.com/wsdl/mime/textMatching/\" xmlns:tns=\"http://warehouse.acme.com/ws\" xmlns:wsdl=\"http://schemas.xmlsoap.org/wsdl/\">\n" +
            "    <wsdl:types>\n" +
            "        <s:schema elementFormDefault=\"qualified\" targetNamespace=\"http://warehouse.acme.com/ws\">\n" +
            "            <s:element name=\"listProducts\">\n" +
            "                <s:complexType>\n" +
            "                    <s:sequence>\n" +
            "                        <s:element maxOccurs=\"1\" minOccurs=\"1\" name=\"delay\" type=\"s:int\"/>\n" +
            "                    </s:sequence>\n" +
            "                </s:complexType>\n" +
            "            </s:element>\n" +
            "            <s:element name=\"listProductsResponse\">\n" +
            "                <s:complexType>\n" +
            "                    <s:sequence>\n" +
            "                        <s:element maxOccurs=\"1\" minOccurs=\"0\" name=\"listProductsResult\" type=\"tns:ArrayOfProductListHeader\"/>\n" +
            "                    </s:sequence>\n" +
            "                </s:complexType>\n" +
            "            </s:element>\n" +
            "            <s:complexType name=\"ArrayOfProductListHeader\">\n" +
            "                <s:sequence>\n" +
            "                    <s:element maxOccurs=\"unbounded\" minOccurs=\"0\" name=\"ProductListHeader\" nillable=\"true\" type=\"tns:ProductListHeader\"/>\n" +
            "                </s:sequence>\n" +
            "            </s:complexType>\n" +
            "            <s:complexType name=\"ProductListHeader\">\n" +
            "                <s:sequence>\n" +
            "                    <s:element maxOccurs=\"1\" minOccurs=\"0\" name=\"productName\" type=\"s:string\"/>\n" +
            "                    <s:element maxOccurs=\"1\" minOccurs=\"1\" name=\"productId\" type=\"s:long\"/>\n" +
            "                </s:sequence>\n" +
            "            </s:complexType>\n" +
            "        </s:schema>\n" +
            "    </wsdl:types>\n" +
            "    <wsdl:message name=\"listProductsSoapIn\">\n" +
            "        <wsdl:part element=\"tns:listProducts\" name=\"parameters\"/>\n" +
            "    </wsdl:message>\n" +
            "    <wsdl:message name=\"listProductsSoapOut\">\n" +
            "        <wsdl:part element=\"tns:listProductsResponse\" name=\"parameters\"/>\n" +
            "    </wsdl:message>\n" +
            "    <wsdl:portType name=\"WarehouseSoap\">\n" +
            "        <wsdl:operation name=\"listProducts\">\n" +
            "            <wsdl:input message=\"tns:listProductsSoapIn\"/>\n" +
            "            <wsdl:output message=\"tns:listProductsSoapOut\"/>\n" +
            "        </wsdl:operation>\n" +
            "    </wsdl:portType>\n" +
            "    <wsdl:binding name=\"WarehouseSoap12\" type=\"tns:WarehouseSoap\">\n" +
            "        <soap12:binding transport=\"http://schemas.xmlsoap.org/soap/http\"/>\n" +
            "        <wsdl:operation name=\"listProducts\">\n" +
            "            <soap12:operation soapAction=\"http://warehouse.acme.com/ws/listProducts\" style=\"document\"/>\n" +
            "            <wsdl:input>\n" +
            "                <soap12:body use=\"literal\"/>\n" +
            "            </wsdl:input>\n" +
            "            <wsdl:output>\n" +
            "                <soap12:body use=\"literal\"/>\n" +
            "            </wsdl:output>\n" +
            "        </wsdl:operation>\n" +
            "    </wsdl:binding>\n" +
            "    <wsdl:service name=\"Warehouse\">\n" +
            "        <wsdl:port binding=\"tns:WarehouseSoap12\" name=\"WarehouseSoap12\">\n" +
            "            <soap12:address location=\"http://www.layer7tech.com:8888/WarehouseService.asmx\"/>\n" +
            "        </wsdl:port>\n" +
            "    </wsdl:service>\n" +
            "</wsdl:definitions>";
}
