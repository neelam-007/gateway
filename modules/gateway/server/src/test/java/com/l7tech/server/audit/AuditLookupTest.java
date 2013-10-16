package com.l7tech.server.audit;

import com.l7tech.common.io.CertUtils;
import com.l7tech.gateway.common.audit.*;
import com.l7tech.server.util.CompressedStringType;
import org.junit.Before;
import org.junit.Test;

import java.security.cert.X509Certificate;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Test for external audits signature checking.   Uses data from supported database types (db2, mysql, sql server, oracle)
 * and accounts for different handling of default and null values ( null vs "" )
 */
public class AuditLookupTest {

    @Test
    public void mysqlAdminSignaturePre80()throws Exception {
        AuditRecord  record = ExternalAuditsUtils.makeAuditRecord(
                "335684da-8a99-4f7c-be92-2b515eb68924",//id
                "48b5a4bc60fd4db191ddd5258eab0a35",// nodeid
                1381365159583L, // time
                "admin", // type ,
                "800", // auditLevel
                "" , // name
                "User logged out" , // message
                "127.0.0.1",// ip_addr,
                "admin",// userName,
                "3",// userId ,
                "-2",// providerOid ,
                "LbxYEYp7d0yOEf4iPTZJa/xOvgBllXfFUV3oAc8aiivIXODL75Y0tXrFaz9VTJ6tNgDRO56GuwVA51GQpKngIHJLdJG3xT5knqreyf5k0THn5e7dbPwM3E4ZE1DW9tm3tcQ6clY8EYimf+FIyHn/eLAfvg/S7hxrb/tcJiK+eT3uMRUEWBlLA7OPQjEg3TYIxCK66+5K3A/j+jHN6NGBTrE3DHltms2fUpX+jSQ/mVi5vP+s+uloNGJKaeAmfDYPWcsEhuarubKqmHeoHyeHCsXWA305cMiMkO3swE06miUXm5NadZbXTbXqAaOWEYWXrWXsWLuLBlZynR1Me5zRAw==",// signature,
                "<none>",// entityClass ,
                "0",// entityId ,
                null,// status ,
                "",// requestId ,
                "",// serviceGoid ,
                "",// operationName ,
                null,// authenticated ,
                "",// authenticationType,
                null,// requestLength ,
                null,// responseLength ,
                new byte[0],// requestZip ,
                new byte[0],// responseZip,
                null,// responseStatus,
                null,// latency,
                null,// componentId,
                "X",// action,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    " <audit xmlns=\"http://l7tech.com/audit/rec\"/>\n" +
                    " "// properties
        );
        assertTrue("Invalid signature",verifyAudit(record, true));
    }

    @Test
    public void mysqlSystemSignaturePre80()throws Exception {
        AuditRecord  record = ExternalAuditsUtils.makeAuditRecord(
                "c2c43230-1457-4167-b41c-a64e57465329",//id
                "48b5a4bc60fd4db191ddd5258eab0a35",// nodeid
                1381363878767L, // time
                "system", // type ,
                "800", // auditLevel
                "Audit Sink Configuration" , // name
                "Audit Sink Policy started" , // message
                "10.7.49.167",// ip_addr,
                "",// userName,
                "",// userId ,
                "-1",// providerOid ,
                "dtLA84iyrqQGCISxZ/TrlhoyxvHH8NlJz5gKCjl0tutkXxvOs0rB0OUGrrRAgvNzxc3dY8JVuaVm8AovGrK/53yq65XGe14dStY+agaTqL6leP8D6QaLRydzNDaFicPs8NU8upva8G3dT/JG6wBlQb0n501RQTxjVzjz9+EeP73CUe28/J0irRT5GtTnpuYlEgl7AsVAxe/iAFEfBTzPkf0QOsHXP8KJ51VU/747b3uUUPxp6yT2oX8WmlkOvnHNnqWCuTYcDZtQCczPRVykRiV6DRAb8qN0cMzPHr5Zo61Kya1dArYUhkyXpyl9N+2h2ML91WvktYqmxsRb8OOMtQ==",// signature,
                "",// entityClass ,
                "",// entityId ,
                null,// status ,
                "",// requestId ,
                "",// serviceGoid ,
                "",// operationName ,
                null,// authenticated ,
                "",// authenticationType,
                null,// requestLength ,
                null,// responseLength ,
                new byte[0],// requestZip ,
                new byte[0],// responseZip,
                null,// responseStatus,
                null,// latency,
                1280010,// componentId,
                "Properties Evaluation",// action,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        " <audit xmlns=\"http://l7tech.com/audit/rec\"/>\n" +
                        " "// properties
        );
        assertTrue("Invalid signature",verifyAudit(record, true));
    }

    @Test
    public void mysqlMessageSignaturePre80()throws Exception {
        byte[] requestZip = CompressedStringType.compress("<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\" xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><env:Header><wsa:Action env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/enumeration/Enumerate</wsa:Action><wsa:ReplyTo><wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:MessageID env:mustUnderstand=\"true\">uuid:a6107937-b572-4cab-a801-5c695b962ac0</wsa:MessageID><wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To><wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/jdbcConnections</wsman:ResourceURI><wsman:RequestTotalItemsCountEstimate/></env:Header><env:Body><wsen:Enumerate/></env:Body></env:Envelope>");
        byte[] responseZip = CompressedStringType.compress("<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:mdo=\"http://schemas.wiseman.dev.java.net/metadata/messagetypes\" xmlns:mex=\"http://schemas.xmlsoap.org/ws/2004/09/mex\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\" xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wsmeta=\"http://schemas.dmtf.org/wbem/wsman/1/wsman/version1.0.0.a/default-addressing-model.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><env:Header><wsa:Action xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\" env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/enumeration/EnumerateResponse</wsa:Action><wsman:TotalItemsCountEstimate xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">2</wsman:TotalItemsCountEstimate><wsa:MessageID xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\" env:mustUnderstand=\"true\">uuid:d4efb1c5-85a4-43a2-b59b-18b86dedf524</wsa:MessageID><wsa:RelatesTo xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">uuid:a6107937-b572-4cab-a801-5c695b962ac0</wsa:RelatesTo><wsa:To xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\" env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:To></env:Header><env:Body><wsen:EnumerateResponse xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\"><wsen:Expires>2147483647-12-31T23:59:59.999-14:00</wsen:Expires><wsen:EnumerationContext>d7f7f03b-bb55-4685-a458-6c15394dac0c</wsen:EnumerationContext></wsen:EnumerateResponse></env:Body></env:Envelope>");
        AuditRecord  record = ExternalAuditsUtils.makeAuditRecord(
                "819bffcf-316f-4741-9727-1333e00c9367",//id
                "48b5a4bc60fd4db191ddd5258eab0a35",// nodeid
                1381365122277L, // time
                "message", // type ,
                "900", // auditLevel
                "Gateway Management Service [/wsman]" , // name
                "Message processed successfully" , // message
                "127.0.0.1",// ip_addr,
                "admin",// userName,
                "3",// userId ,
                "-1",// providerOid ,
                "OC4Yt+gViP9YUQM8kBLag+36V4S5hrQxUZ8kKQTyzL3NYCmCDELyrn9fv9fBCDuiAhbEu7ljZOdcpYgdpi5NIYBjifgaxQ5CO7ZtAT3amjgcjA1Sc1qEx9hy3y95iqB61FRCIKJ6mc0OXk2Hcj0MV2gUAtMlGpxxgsVg14GZ+dBx2QirP7ssYROXHlMN3Qa9kHF7iohYaCU76huiTgG1y/PdH4B72LyKkjBQfbtRpJAlUk/b3h8MZ1lrsLmxs98BPy91Og1simxQRjhQBNt682B14bWCwIfCP4Mk066KJW9i8kJHuiTBiXccTG1hjud622gnULF77RKJW3qrrQihQA==",// signature,
                "",// entityClass ,
                "",// entityId ,
                0,// status ,
                "000001419fb347ef-13",// requestId ,
                "229376",// serviceGoid ,
                "EnumerateOp",// operationName ,
                Boolean.TRUE,// authenticated ,
                "HTTP Basic",// authenticationType,
                1053,// requestLength ,
                1712,// responseLength ,
                requestZip,// requestZip ,
                responseZip,// responseZip,
                0,// responseStatus,
                0,// latency,
                null,// componentId,
                "",// action,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        " <audit xmlns=\"http://l7tech.com/audit/rec\"/>\n" +
                        " "// properties
        );
        AuditDetail detail = ExternalAuditsUtils.makeAuditDetail(
                1, // index
                "819bffcf-316f-4741-9727-1333e00c9367", // audit_oid
                1381365121601L , //time
                0, //componentId
                0, //ordinal
                4104, //messageId
                "", //message
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        " <detail xmlns=\"http://l7tech.com/audit/detail\">\n" +
                        "     <params>\n" +
                        "         <param>admin</param>\n" +
                        "     </params>\n" +
                        " </detail>\n" +
                        " " //properties
                );
        Set<AuditDetail> details = record.getDetails();
        details.add(detail);
        record.setDetails(details);
        assertTrue("Invalid signature", verifyAudit(record, true));
    }

    @Test
    public void mysqlAdminSignature()throws Exception {
        AuditRecord  record = ExternalAuditsUtils.makeAuditRecord(
                "cb00bf91-b42a-4c5c-bb21-6c14df249983",//id
                "48b5a4bc60fd4db191ddd5258eab0a35",// nodeid
                1381442836350L, // time
                "admin", // type ,
                "800", // auditLevel
                "Policy for service #7b2861ec5ead45da03a6f268cebded80, Gateway Management Service" , // name
                "Policy #7b2861ec5ead45da03a6f268cebded82 (Policy for service #7b2861ec5ead45da03a6f268cebded80, Gateway Management Service) updated (changed xml)" , // message
                "127.0.0.1",// ip_addr,
                "admin",// userName,
                "00000000000000000000000000000003",// userId ,
                "0000000000000000fffffffffffffffe",// providerOid ,
                "g1Tr7ENQM+M83b/5zjrm221f+ly971F4e3mXaqpwd5mHQONb9xJvaX8aZ9c1O0h06sQ9I2MA7wSz/ve6tobspallxh3pKoUaJQJpaihc2jn/JYf2OK15aH2WGT6PkUPxOe4emQC0DgpHNnlR3JKkfMTsN6GgHcsUagJjnZ+VycqJgR3rWhMovY81xSUsjnnyAexNMrEjWzD1uBiy3QrZJr4o6lADQhAW2PSE2JdfFt+zilsmkAqQ76SWwTxiChgSFDukLS3xa++b+sS+QAeea1sjkcCx+YiQrZs8tV8pigC2VwdrI6D3eVqUm9SyUaG0mtOKwJR5NX9Gh1ojopz2+w==",// signature,
                "com.l7tech.policy.Policy",// entityClass ,
                "7b2861ec5ead45da03a6f268cebded82",// entityId ,
                null,// status ,
                "",// requestId ,
                "",// serviceGoid ,
                "",// operationName ,
                null,// authenticated ,
                "",// authenticationType,
                null,// requestLength ,
                null,// responseLength ,
                new byte[0],// requestZip ,
                new byte[0],// responseZip,
                null,// responseStatus,
                null,// latency,
                null,// componentId,
                "U",// action,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        " <audit xmlns=\"http://l7tech.com/audit/rec\"/>\n" +
                        " "// properties
        );
        assertTrue("Invalid signature",verifyAudit(record, false));
    }

    @Test
    public void mysqlSystemSignature()throws Exception {
        AuditRecord  record = ExternalAuditsUtils.makeAuditRecord(
                "6d1b7f78-3c67-44cc-a09e-3751ec84d2c8",//id
                "48b5a4bc60fd4db191ddd5258eab0a35",// nodeid
                1381443205056L, // time
                "system", // type ,
                "900", // auditLevel
                "Password Policy Service" , // name
                "Password requirements are below STIG minimum for Internal Identity Provider" , // message
                "10.7.49.167",// ip_addr,
                "",// userName,
                "",// userId ,
                "-1",// providerOid ,
                "V2WWttbujCl1VsMbsvfXDsKbi0dhRsKT3Wg+b3vsHEYIprgt4yl50X8Lo+TY5LjSoyeys4rilKS64G4F/4VhsH+Dkfo07ni7h6GiQ5RHv0iyAkUmGyv2rEYF/qy3KRA5RJenpITb0wQZz7KOAJ8W1wjwBKCKf/sep6TyndzkH6aaBw23vMDehltSj0PUxix5czy4WHTeLVXlMp7MZGkUegFB7tszKF7ld6Al1HG7HtKMGkGFUqSSp5obzqCG+yZPdhWeHXCr8zVKJ7drnkQFmM/jsHKYr0yiF0QRfNQ+jYgQGE1dLlYLbjoE0V2wvRdxktArAOvIvGLOZw31o/Sv1g==",// signature,
                "",// entityClass ,
                "",// entityId ,
                null,// status ,
                "",// requestId ,
                "",// serviceGoid ,
                "",// operationName ,
                null,// authenticated ,
                "",// authenticationType,
                null,// requestLength ,
                null,// responseLength ,
                new byte[0],// requestZip ,
                new byte[0],// responseZip,
                null,// responseStatus,
                null,// latency,
                1290050,// componentId,
                "Password Policy Validation",// action,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        " <audit xmlns=\"http://l7tech.com/audit/rec\"/>\n" +
                        " "// properties
        );
        assertTrue("Invalid signature",verifyAudit(record, false));
    }

    @Test
    public void mysqlMessageSignature()throws Exception {
        byte[] requestZip = CompressedStringType.compress("<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\" xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><env:Header><wsa:Action env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/enumeration/Enumerate</wsa:Action><wsa:ReplyTo><wsa:Address env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:Address></wsa:ReplyTo><wsa:MessageID env:mustUnderstand=\"true\">uuid:e7fa3003-6432-463e-94e3-79d9647ffb66</wsa:MessageID><wsa:To env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:To><wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/jdbcConnections</wsman:ResourceURI><wsman:RequestTotalItemsCountEstimate/></env:Header><env:Body><wsen:Enumerate/></env:Body></env:Envelope>");
        byte[] responseZip = CompressedStringType.compress("<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\" xmlns:mdo=\"http://schemas.wiseman.dev.java.net/metadata/messagetypes\" xmlns:mex=\"http://schemas.xmlsoap.org/ws/2004/09/mex\" xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\" xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\" xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\" xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\" xmlns:wsmeta=\"http://schemas.dmtf.org/wbem/wsman/1/wsman/version1.0.0.a/default-addressing-model.xsd\" xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><env:Header><wsa:Action xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\" env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/09/enumeration/EnumerateResponse</wsa:Action><wsman:TotalItemsCountEstimate xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">4</wsman:TotalItemsCountEstimate><wsa:MessageID xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\" env:mustUnderstand=\"true\">uuid:4c1ce098-9ba3-4168-ac15-b920ea91b630</wsa:MessageID><wsa:RelatesTo xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">uuid:e7fa3003-6432-463e-94e3-79d9647ffb66</wsa:RelatesTo><wsa:To xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\" env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:To></env:Header><env:Body><wsen:EnumerateResponse xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\"><wsen:Expires>2147483647-12-31T23:59:59.999-14:00</wsen:Expires><wsen:EnumerationContext>dabc2830-6d17-47c7-b3c1-b95643c8ca4d</wsen:EnumerationContext></wsen:EnumerateResponse></env:Body></env:Envelope>");
        AuditRecord  record = ExternalAuditsUtils.makeAuditRecord(
                "7fcf128f-cff6-4554-aa13-c6c06521e06f",//id
                "48b5a4bc60fd4db191ddd5258eab0a35",// nodeid
                1381442842331L, // time
                "message", // type ,
                "900", // auditLevel
                "Gateway Management Service [/wsman]" , // name
                "Message processed successfully" , // message
                "127.0.0.1",// ip_addr,
                "admin",// userName,
                "00000000000000000000000000000003",// userId ,
                "",// providerOid ,
                "pQ0UGBUzSEqd7zYl1ARkK6sslk8iGbKyDh5rQ2j9fPPbFZQvsAXpvI+yobGYZo+DPW+5gASnkABN/8UmiXFUjbiEhvJh/7XdcACcarNb2/j+oPRspz8FGP9bEqVgYuWC5GlbwMRI3oKYptPhdmQ5AervRtq7d8KWM8PuUeKmVBltI7KyVPY+YEbRTwNLvxVrrqPiGLfTu7H/DoIKeZeCqhQIA3kHbc1IfggSRP/P61W3t2RTz2k9fxUiwtEKrwzr95dZrn7xyL5IBs6hDl4Nv7XQukqAqjF2YIKS1MAc5p3JTB/4vFZbGZEmwLwknKY+rgM/6extRaNuhkyRKCPiUQ==",// signature,
                "",// entityClass ,
                "",// entityId ,
                0,// status ,
                "00000141a452f17d-ab",// requestId ,
                "7b2861ec5ead45da03a6f268cebded80",// serviceGoid ,
                "EnumerateOp",// operationName ,
                Boolean.TRUE,// authenticated ,
                "HTTP Basic",// authenticationType,
                1053,// requestLength ,
                1712,// responseLength ,
                requestZip,// requestZip ,
                responseZip,// responseZip,
                0,// responseStatus,
                0,// latency,
                null,// componentId,
                "",// action,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        " <audit xmlns=\"http://l7tech.com/audit/rec\"/>\n" +
                        " "// properties
        );
        AuditDetail detail = ExternalAuditsUtils.makeAuditDetail(
                1, // index
                "7fcf128f-cff6-4554-aa13-c6c06521e06f", // audit_oid
                1381365121601L , //time
                0, //componentId
                0, //ordinal
                4104, //messageId
                "", //message
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        " <detail xmlns=\"http://l7tech.com/audit/detail\">\n" +
                        "     <params>\n" +
                        "         <param>admin</param>\n" +
                        "     </params>\n" +
                        " </detail>\n" +
                        " " //properties
        );
        Set<AuditDetail> details = record.getDetails();
        details.add(detail);
        record.setDetails(details);
        assertTrue("Invalid signature", verifyAudit(record, false));
    }

    @Test
    public void sqlServerSystemSignaturePre80()throws Exception {
        AuditRecord  record = ExternalAuditsUtils.makeAuditRecord(
                "82a5b34f-8550-4137-a472-07ab3f833ed4",//id
                "48b5a4bc60fd4db191ddd5258eab0a35",// nodeid
                1381449624347L, // time
                "system", // type ,
                "800", // auditLevel
                "Audit Sink Configuration" , // name
                "Internal Audit System started" , // message
                "10.7.49.167",// ip_addr,
                "",// userName,
                "",// userId ,
                "-1",// providerOid ,
                "mtClMFKUY8/+r/hiqPCUKYOREqMtXc5qb93TFOAp3nkYqHo+frgwakilLdU4XYtVLf/QrS2Rr1nnjbw6qDJVdeDakaTTjbFHCVhoR7cYHFoAvuSsz3dQwj3r3DcIfjb27BP2wI/qfBkSTVlUUgCfi7lz8S1tK1WE4M5eklARrempJD51Fls7H1cSTs84TwjVkd75u1YGidkJl3GcUI1GJu/wRBOG39Ooub884o4yL5W3nC2ci2pA1Zbsz7c3/RXYCuCa4RXroyl+w7jQmaHul51gTVZJzCGThfedAfGgCAKTLNhL+jU1HmuQ0YrXY9N10wa0TpniD/vSpfnOCR4jfQ==",// signature,
                "",// entityClass ,
                "",// entityId ,
                null,// status ,
                "",// requestId ,
                "",// serviceGoid ,
                "",// operationName ,
                null,// authenticated ,
                "",// authenticationType,
                null,// requestLength ,
                null,// responseLength ,
                new byte[0],// requestZip ,
                new byte[0],// responseZip,
                null,// responseStatus,
                null,// latency,
                1280010,// componentId,
                "Properties Evaluation",// action,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        " <audit xmlns=\"http://l7tech.com/audit/rec\"/>\n" +
                        " "// properties
        );
        assertTrue("Invalid signature",verifyAudit(record, true));
    }


    @Test
    public void sqlServerAdminSignaturePre80()throws Exception {
        AuditRecord  record = ExternalAuditsUtils.makeAuditRecord(
                "422f1139-5b1e-4376-9041-df22876248a4",//id
                "48b5a4bc60fd4db191ddd5258eab0a35",// nodeid
                1381448599338L, // time
                "admin", // type ,
                "800", // auditLevel
                "audit.acknowledge.highestTime" , // name
                "ClusterProperty #32774 (audit.acknowledge.highestTime) updated (changed value)" , // message
                "127.0.0.1",// ip_addr,
                "admin",// userName,
                "3",// userId ,
                "-2",// providerOid ,
                "GQWmigQgJtQPEc+SwW+ZF+wvETJ6bovLOG/3AvezbBA/0yINnBKrWcfm78lVrvBe0nbTcsrBbIH2viQcl+qA0EhTvzys3h2gTF1Ckyw9Y6B0JjiRsNDLIS6Vo98de7WBFRuRU5eiV9hQ2vsgksR3RIur7el0nmCyOLaIh+YSzF8vdMZEQ+wTbyVCk5RpinTLA9viRLAfLlRuPVy4zXbqN383xGKFzKgDDRu1i3cMXuNd0fWRoL/bQGVVVkufueOfQ9SdYB/HzNXyGAXW4bCXCpGo8vQT78y+mo+nhfsD4gQvd9xXWz2F8I2PRW0PXkD5Nxk5edzWYfXmftlUe8kanA==",// signature,
                "com.l7tech.gateway.common.cluster.ClusterProperty",// entityClass ,
                "32774",// entityId ,
                null,// status ,
                "",// requestId ,
                "",// serviceGoid ,
                "",// operationName ,
                null,// authenticated ,
                "",// authenticationType,
                null,// requestLength ,
                null,// responseLength ,
                new byte[0],// requestZip ,
                new byte[0],// responseZip,
                null,// responseStatus,
                null,// latency,
                null,// componentId,
                "U",// action,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        " <audit xmlns=\"http://l7tech.com/audit/rec\"/>\n" +
                        " "// properties
        );
        assertTrue("Invalid signature",verifyAudit(record, true));
    }

    @Test
    public void sqlServerMessageSignaturePre80()throws Exception {
        byte[] responseZip = CompressedStringType.compress("<asdf>hihi</asdf>");
        AuditRecord  record = ExternalAuditsUtils.makeAuditRecord(
                "3c69f322-fbfe-42e2-b1d7-f01ff338c65d",//id
                "48b5a4bc60fd4db191ddd5258eab0a35",// nodeid
                1381514167017L, // time
                "message", // type ,
                "900", // auditLevel
                "test [/test]" , // name
                "Message processed successfully" , // message
                "127.0.0.1",// ip_addr,
                "",// userName,
                "",// userId ,
                "-1",// providerOid ,
                "PHYR26MwtUmIopqDH3u+nMUcb4kW89yDzmd6B38qOYV61eQJ5w1O9eQLfaLlmI65T+fuukROt21DLcezjQFp4HIBQjdifB0/5A7ufOS3+CYCqtWH6ROz4d2R58hHlV7xS8CnBYz7MlKB8krL96vxw+ff4gkcIHWk6AXnENwoCmaMhHJeqql+50zPK2mEk7esJa534TmIwar7P25Ha+NF7SIAJ7qXlqXrUqSooyKjaBQ7HGmDX/ElrMEDlkaxsHRQX7Ya7RioM5+3L7Y/oDDMJ10xqIM4J3w0WgJsaTZNMdc3nPB0RomEEDKLU8wgwCGJ4LWsvyVrCMGKKct+YsEvoA==",// signature,
                "",// entityClass ,
                "",// entityId ,
                0,// status ,
                "00000141a8a3f2de-33",// requestId ,
                "229377",// serviceGoid ,
                "",// operationName ,
                Boolean.FALSE,// authenticated ,
                "",// authenticationType,
                0,// requestLength ,
                17,// responseLength ,
                new byte[0],// requestZip ,
                responseZip,// responseZip,
                200,// responseStatus,
                0,// latency,
                null,// componentId,
                "",// action,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        " <audit xmlns=\"http://l7tech.com/audit/rec\"/>\n" +
                        " "// properties
        );
        AuditDetail detail = ExternalAuditsUtils.makeAuditDetail(
                1, // index
                "3c69f322-fbfe-42e2-b1d7-f01ff338c65d", // audit_oid
                1381514167016L , //time
                0, //componentId
                0, //ordinal
                -4, //messageId
                "", //message
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?> <detail xmlns=\"http://l7tech.com/audit/detail\">     <params>         <param>awgewaggewaegwaagew</param>     </params> </detail> " //properties
        );
        Set<AuditDetail> details = record.getDetails();
        details.add(detail);
        record.setDetails(details);
        assertTrue("Invalid signature", verifyAudit(record, true));
    }

    @Test
    public void sqlServerSystemSignature()throws Exception {
        AuditRecord  record = ExternalAuditsUtils.makeAuditRecord(
                "fc649153-7c04-4461-94fb-c674ea2155fd",//id
                "48b5a4bc60fd4db191ddd5258eab0a35",// nodeid
                1381530387614L, // time
                "system", // type ,
                "800", // auditLevel
                "Email Listeners" , // name
                "Connected to email server (qwert)" , // message
                "10.7.49.167",// ip_addr,
                "",// userName,
                "",// userId ,
                "",// providerOid ,
                "ePMF9xlOWd4o/RIDJrd1J66uvwtPAnNjSuyn+P4l4a7Hyf8s8phXJ563nMGqP5Z43qEBrhWn8sOiAgxXUfAXf/b1q+UgKS80cuL/yqF8ctEMl2JbkeOYRj0k0EH3m134Oy4BFIkuta38HMcap4gBhOOb2NypAsQOivztAa+fRf0yfmGkaFcLKpW7n9FVPH3yl3n2lFqIfZH9nFHpJcNXQlQl7TqWrk93Xz9KgAPbQxIVIYrHGhsFR1pjEA00s4w6QQQR4PN0pCvynRhGezit8bTPN65cQbbH75FNtCnsAoArBZCPzzejOyJNhL+sF7LvHY2vk8i3lT7RpzIR2j6oeQ==",// signature,
                "",// entityClass ,
                "",// entityId ,
                null,// status ,
                "",// requestId ,
                "",// serviceGoid ,
                "",// operationName ,
                null,// authenticated ,
                "",// authenticationType,
                null,// requestLength ,
                null,// responseLength ,
                new byte[0],// requestZip ,
                new byte[0],// responseZip,
                null,// responseStatus,
                null,// latency,
                1275000,// componentId,
                "Connect",// action,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        " <audit xmlns=\"http://l7tech.com/audit/rec\"/>\n" +
                        " "// properties
        );
        assertTrue("Invalid signature",verifyAudit(record, false));
    }


    @Test
    public void sqlServerAdminSignature()throws Exception {
        AuditRecord  record = ExternalAuditsUtils.makeAuditRecord(
                "c960f745-1d89-4d80-9597-e93f1119916d",//id
                "48b5a4bc60fd4db191ddd5258eab0a35",// nodeid
                1381530263320L, // time
                "admin", // type ,
                "800", // auditLevel
                "audit.acknowledge.highestTime" , // name
                "ClusterProperty #b9003b53ecf8b0cb0000000000008006 (audit.acknowledge.highestTime) updated (changed value)" , // message
                "127.0.0.1",// ip_addr,
                "admin",// userName,
                "00000000000000000000000000000003",// userId ,
                "0000000000000000fffffffffffffffe",// providerOid ,
                "R0OX9510j8Gvh/MOfl3RdhGNWP6k865KJFsSBaqu0QZBcRaPKZH612dzrBUciBMN7VmZRxu331prmi2QOuFeRM7q071CeIjuem+MLWm6wg87DYibpD2bGq+owYDKjUtmS+yemCxKW8DPxu0m84l4mYcqvprcd0WUO6Xjo7J/UW/aldj13/KLkWwKhs+gYx6oGKOO48dooYSQtEXGd20+KGw+DV0fITl7rry47IhCv6B8dJUB6AO4EMyhhdWbq/aG2D37f9IVbCjXQybZuhRYbaVYNpp+vjuU0hSXher2TcB6wlYeyjQvJyH2iIOj5BmEhMl95qWVuEjzhzFr8P+G1w==",// signature,
                "com.l7tech.gateway.common.cluster.ClusterProperty",// entityClass ,
                "b9003b53ecf8b0cb0000000000008006",// entityId ,
                null,// status ,
                "",// requestId ,
                "",// serviceGoid ,
                "",// operationName ,
                null,// authenticated ,
                "",// authenticationType,
                -1,// requestLength ,
                -1,// responseLength ,
                new byte[0],// requestZip ,
                new byte[0],// responseZip,
                null,// responseStatus,
                null,// latency,
                null,// componentId,
                "U",// action,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        " <audit xmlns=\"http://l7tech.com/audit/rec\"/>\n" +
                        " "// properties
        );
        assertTrue("Invalid signature",verifyAudit(record, false));
    }

    @Test
    public void sqlServerMessageSignature()throws Exception {
        byte[] responseZip = CompressedStringType.compress("<qwer>AAA<\\qwer>");
        AuditRecord  record = ExternalAuditsUtils.makeAuditRecord(
                "3c69f322-fbfe-42e2-b1d7-f01ff338c65d",//id
                "48b5a4bc60fd4db191ddd5258eab0a35",// nodeid
                1381530270017L, // time
                "message", // type ,
                "900", // auditLevel
                "test [/test]" , // name
                "Message processed successfully" , // message
                "127.0.0.1",// ip_addr,
                "",// userName,
                "",// userId ,
                "",// providerOid ,
                "kBlVLY2xTg4JUW3T9oSRFpueciwxPY20z0UMgPJAYM62NbXVHCGPc2T6K8rrK0f9j+DUvbCQcAsEJsH8/o7DKPv4egyjo+n3MMlrfF8uK2UVQajdw5Ul7IeOJipPs7fEB+B/z7fYe7a664VxujRD9Da+7RV2+hqsEL3O9u1PblgEmuWl872SJM9BQHgZ2n+Z5C7lHv+3edVf9976nta3ka9a8hS5FrTjuE6kLWrzNbxRkKkcGHgMoGKRrQYQ4E8X8LP4ddgqvaUiamYXbNDOUjFHY0GAPrWDDJCSAqhTNLen3jPEkF/EAzD/3IVnsGcPJvq7CvUTBaiPjc4s1KbmIQ==",// signature,
                "",// entityClass ,
                "",// entityId ,
                0,// status ,
                "00000141a95153de-f8",// requestId ,
                "b5157238ab4922f40000000000038001",// serviceGoid ,
                "",// operationName ,
                Boolean.FALSE,// authenticated ,
                "",// authenticationType,
                0,// requestLength ,
                16,// responseLength ,
                new byte[0],// requestZip ,
                responseZip,// responseZip,
                402,// responseStatus,
                0,// latency,
                null,// componentId,
                "",// action,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        " <audit xmlns=\"http://l7tech.com/audit/rec\"/>\n" +
                        " "// properties
        );
        AuditDetail detail1 = ExternalAuditsUtils.makeAuditDetail(
                0, // index
                "1ce64cdf-e745-4dc8-ac96-c552c220df53", // audit_oid
                1381530270015L , //time
                0, //componentId
                0, //ordinal
                -4, //messageId
                "", //message
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<detail xmlns=\"http://l7tech.com/audit/detail\">\n    <params>\n        <param>awgewag,,,,gewaegw,aa,gew:/:;/\\\\::</param>\n    </params>\n</detail>\n" //properties
        );
        AuditDetail detail2 = ExternalAuditsUtils.makeAuditDetail(
                1, // index
                "1ce64cdf-e745-4dc8-ac96-c552c220df53", // audit_oid
                1381530270016L , //time
                0, //componentId
                1, //ordinal
                4104, //messageId
                "", //message
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<detail xmlns=\"http://l7tech.com/audit/detail\">\n    <params>\n        <param>admin</param>\n    </params>\n</detail>\n" //properties
        );
        Set<AuditDetail> details = record.getDetails();
        details.add(detail1);
        details.add(detail2);
        record.setDetails(details);
        assertTrue("Invalid signature", verifyAudit(record, false));
    }

    @Test
    public void oracleSystemSignaturePre80()throws Exception {
        AuditRecord  record = ExternalAuditsUtils.makeAuditRecord(
                "19ef1606-ff36-4964-9962-38d88fe9aa09",//id
                "48b5a4bc60fd4db191ddd5258eab0a35",// nodeid
                1381514916927L, // time
                "system", // type ,
                "500", // auditLevel
                "Server" , // name
                "Server Starting" , // message
                "10.7.49.167",// ip_addr,
                null,// userName,
                null,// userId ,
                "-1",// providerOid ,
                "HeZZnXQ0QG72OZm5JIm522YY4+mBP8MEDCha88ohq0yQgj/t4uId3sOkOvUpZE3fckCsTr9vUg5ZK5rt+pkpG0v0PS7rSXuTH++lQO1wccvaKC2M7vHMKK8Rcn4UnihmelhKvIDoTLJIZktnpfVh0OYck9ZAu0gGTQXh1sa1lRR651o7JEbvWkTyDw0UHq2egmMCMYf4YMBGfDtQb58vSAEe2PExLmYBDsg9H7s8cWfZZ8rIhpYYOrcaOdDklCla+IvgccaAILiCF0MiEs9DUTnA+tErgHWLiz7vTs5j56V1CjS4i5vSx0WOD0+X+0al6tKhvwZDqUSubi93rXB8NA==",// signature,
                null,// entityClass ,
                null,// entityId ,
                null,// status ,
                null,// requestId ,
                null,// serviceGoid ,
                null,// operationName ,
                null,// authenticated ,
                null,// authenticationType,
                -1,// requestLength ,
                -1,// responseLength ,
                new byte[0],// requestZip ,
                new byte[0],// responseZip,
                null,// responseStatus,
                null,// latency,
                1200000,// componentId,
                "Starting",// action,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        " <audit xmlns=\"http://l7tech.com/audit/rec\"/>\n" +
                        " "// properties
        );
        assertTrue("Invalid signature",verifyAudit(record, true));
    }

    @Test
    public void oracleAdminSignaturePre80()throws Exception {
        AuditRecord  record = ExternalAuditsUtils.makeAuditRecord(
                "3030985c-4abc-49c3-b24e-c8295c615675",//id
                "48b5a4bc60fd4db191ddd5258eab0a35",// nodeid
                1381515249346L, // time
                "admin", // type ,
                "800", // auditLevel
                "Policy for service #229377, test" , // name
                "Policy #262145 (Policy for service #229377, test) updated (changed xml)" , // message
                "127.0.0.1",// ip_addr,
                "admin",// userName,
                "3",// userId ,
                "-2",// providerOid ,
                "VtTiBwXerzuK1YcmWvW6gm4e8rx/TmmOpZKLNmkmH2vN1Mp9L3TTlSnRNJF0T+j4MZZL3j7jghNni22TtRBvIXj3+3yhYdR5ur2EbFgimJHTfytKRERbCKbk2MBmKVetZa4VtPVILu9IGcEacNW+cdmMCTYHr/kuvMjGZ4+UYIp3sO8R+VWIf+SKCQ5Qvj3/Qbl4RqbxuLnxZlRdB0n3VkJASirv5Iwg4S9EagNOBAIywbA/9I7iwFRYenjuc08A20IaUy+nQg3E6n24xyweGoMOs4W8M0TtRLZf9xBCFRNZ2E11yDbRbUxs4ZeyoV/RTCS/lpQpInYvLNsQQVgtMA==",// signature,
                "com.l7tech.policy.Policy",// entityClass ,
                "262145",// entityId ,
                null,// status ,
                null,// requestId ,
                null,// serviceGoid ,
                null,// operationName ,
                null,// authenticated ,
                null,// authenticationType,
                -1,// requestLength ,
                -1,// responseLength ,
                new byte[0],// requestZip ,
                new byte[0],// responseZip,
                null,// responseStatus,
                null,// latency,
                null,// componentId,
                "U",// action,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        " <audit xmlns=\"http://l7tech.com/audit/rec\"/>\n" +
                        " "// properties
        );
        assertTrue("Invalid signature",verifyAudit(record, true));
    }


    @Test
    public void oracleMessageSignaturePre80()throws Exception {
        byte[] responseZip = CompressedStringType.compress("<asdf>hihi</asdf>");
        AuditRecord  record = ExternalAuditsUtils.makeAuditRecord(
                "b9eda451-2556-4d27-a51b-360ded862970",//id
                "48b5a4bc60fd4db191ddd5258eab0a35",// nodeid
                1381515239690L, // time
                "message", // type ,
                "900", // auditLevel
                "test [/test]" , // name
                "Message processed successfully" , // message
                "127.0.0.1",// ip_addr,
                null,// userName,
                null,// userId ,
                "-1",// providerOid ,
                "iDhF419Gnc7knTxtwxdLuwfdY6j+RIWVleTbk/mgoAEfViLGNDczbz42vIzabAwE4rrjaYKh1C9q+sHngp14WDc/sFeuBwy06dvRW5RMMdOsOO4/s9Ru31yHz2RmbFPcn3mh331kWhCHROGuO7dkQnGk0Jl0WcQSGN3TWvD8zLpBP+h5iGLmyHfOrbVpGrI2lCiE/2jzb2hU40aSEujkt0aLo8Ds52gC+1C0+yRyAyZv3QNZwQ0NwyegY8PPEhkhhdtoZhPhD6pPlvfdaT+HEWyjELSvbXwPbWNanCuijCQeuMByOFpwzmPzMXQRhjonhX3b3s3uhNIAZqV52sC/GA==",// signature,
                null,// entityClass ,
                null,// entityId ,
                0,// status ,
                "00000141a8b3fc17-11",// requestId ,
                "229377",// serviceGoid ,
                null,// operationName ,
                Boolean.FALSE,// authenticated ,
                null,// authenticationType,
                0,// requestLength ,
                17,// responseLength ,
                new byte[0],// requestZip ,
                responseZip,// responseZip,
                200,// responseStatus,
                0,// latency,
                null,// componentId,
                "",// action,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        " <audit xmlns=\"http://l7tech.com/audit/rec\"/>\n" +
                        " "// properties
        );
        AuditDetail detail = ExternalAuditsUtils.makeAuditDetail(
                1, // index
                "b9eda451-2556-4d27-a51b-360ded862970", // audit_oid
                1381515239690L , //time
                0, //componentId
                0, //ordinal
                -4, //messageId
                "", //message
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<detail xmlns=\"http://l7tech.com/audit/detail\">\n    <params>\n        <param>awgewaggewaegwaagew</param>\n    </params>\n</detail>\n" //properties
        );
        Set<AuditDetail> details = record.getDetails();
        details.add(detail);
        record.setDetails(details);
        assertTrue("Invalid signature", verifyAudit(record, true));
    }

    @Test
    public void oracleAdminSignature()throws Exception {
        AuditRecord  record = ExternalAuditsUtils.makeAuditRecord(
                "dd5f01e3-6f18-4e50-96ed-2eaf1ce08c40",//id
                "48b5a4bc60fd4db191ddd5258eab0a35",// nodeid
                1381529372888L, // time
                "admin", // type ,
                "800", // auditLevel
                "[Internal Audit Lookup Policy]" , // name
                "Policy #732ecdee0e8743970000000000040003 ([Internal Audit Lookup Policy]) updated (changed xml)" , // message
                "127.0.0.1",// ip_addr,
                "admin",// userName,
                "00000000000000000000000000000003",// userId ,
                "0000000000000000fffffffffffffffe",// providerOid ,
                "Xrx+9SzbiMArF9xzwDyFOiiAsuftyX6k72mLKiA3zkzmmNScKr5Ryjw3/t47tGc2AYOOypwb/84WgK8B3PP+ovsDjz133xSmd6acWhY1+znx4T4CVZ/m6/11ojXUCqQeZowmAxx+9gwFnnqXzQ3prOJaoJYwMds0zNpllt5ex5J0UKHrI+1X9XavTUI/QkCT8dAn1d/I/J+vayv+A0jAiVPVA5n1jePWWCNW7Bwg1U6COsFZuB1m/Zh8hg5BLZ2Ez093PaQuhrpKaqNFTP8NhuhS7gTHKqFjlKqzEEwyGtYbe3/q8L9BYHatSxo0IVssVvImyRf2pgel/qDfD9sv4w==",// signature,
                "com.l7tech.policy.Policy",// entityClass ,
                "732ecdee0e8743970000000000040003",// entityId ,
                null,// status ,
                null,// requestId ,
                null,// serviceGoid ,
                null,// operationName ,
                null,// authenticated ,
                null,// authenticationType,
                -1,// requestLength ,
                -1,// responseLength ,
                new byte[0],// requestZip ,
                new byte[0],// responseZip,
                null,// responseStatus,
                null,// latency,
                null,// componentId,
                "U",// action,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        " <audit xmlns=\"http://l7tech.com/audit/rec\"/>\n" +
                        " "// properties
        );
        assertTrue("Invalid signature",verifyAudit(record, false));
    }


    @Test
    public void oracleSystemSignature()throws Exception {
        AuditRecord  record = ExternalAuditsUtils.makeAuditRecord(
                "3030985c-4abc-49c3-b24e-c8295c615675",//id
                "48b5a4bc60fd4db191ddd5258eab0a35",// nodeid
                1381529353911L, // time
                "system", // type ,
                "800", // auditLevel
                "Email Listeners" , // name
                "Connected to email server (qwert)" , // message
                "10.7.49.167",// ip_addr,
                null,// userName,
                null,// userId ,
                "-1",// providerOid ,
                "rzEJXBkKl2mawEQT8gyvA4gTMKsFRoLkp78HM3DgqGkdOZrqHiwBelTayCbfZRgm9W6NnaWpi2zTXk/lKGamfIyplR0oKXR5WddjyxC4T2rzaltSyln4mgrImngj6AupRI344omUc3WaF1Q4R3LsM+0ZUTBXcEk6jjTJiMBBMiTfF9+go+xu7plh4mpTxRKZINwJMvdKy3JEdwd3J1wGVCvQ8QSg3+NayNbIqmNkXYVcyEKTjB4Dd7rG+esfU3bdNXwtt0uKYxIjR4Pj0f0XN7AHBiAFQZuMNCDrXYLfAmNsDfgY2nKC5XyMcbd5VIEfa0m4Z4UBmX6Z5gPvzHK3Ow==",// signature,
                null,// entityClass ,
                null,// entityId ,
                null,// status ,
                null,// requestId ,
                null,// serviceGoid ,
                null,// operationName ,
                null,// authenticated ,
                null,// authenticationType,
                -1,// requestLength ,
                -1,// responseLength ,
                new byte[0],// requestZip ,
                new byte[0],// responseZip,
                null,// responseStatus,
                null,// latency,
                1275000,// componentId,
                "Connect",// action,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        " <audit xmlns=\"http://l7tech.com/audit/rec\"/>\n" +
                        " "// properties
        );
        assertTrue("Invalid signature",verifyAudit(record, false));
    }


    @Test
    public void oracleMessageSignature()throws Exception {
        byte[] responseZip = CompressedStringType.compress("<qwer>AAA<\\qwer>");
        AuditRecord  record = ExternalAuditsUtils.makeAuditRecord(
                "b9eda451-2556-4d27-a51b-360ded862970",//id
                "48b5a4bc60fd4db191ddd5258eab0a35",// nodeid
                1381529379937L, // time
                "message", // type ,
                "900", // auditLevel
                "test [/test]" , // name
                "Message processed successfully" , // message
                "127.0.0.1",// ip_addr,
                null,// userName,
                null,// userId ,
                "-1",// providerOid ,
                "EMxBGzIHf5fHf013VUzuoFcw5mttCqAkasODW4nfXBLnlrLM8pEsjXL2CiY/GmMcI4STfpvTI541rfUjKjPQ5kOyKFuD7sU1FaVzs47ut3p4Q43EaaZcphoe+xIikIQ1+gyz/ZqPrpzAOOiThf8PFORpQ7l6w/3sDeuxdWze9qnn609lptT1XjVCEukecOVYLhH7cyDAcCwj/YzHgO24MqEJQVCGc+ArinBkJdQSV9ayygOx8BLupKpMaY50dVZGmAymoJqie0u/6etf33NljJEjx2pMm4qnXsj9U2VdaNn5zoAhLrH2v+rqHEPFqxrCNDxlX93KpXYhBmBgz5fyrQ==",// signature,
                null,// entityClass ,
                null,// entityId ,
                0,// status ,
                "00000141a95153de-83",// requestId ,
                "b5157238ab4922f40000000000038001",// serviceGoid ,
                null,// operationName ,
                Boolean.FALSE,// authenticated ,
                null,// authenticationType,
                0,// requestLength ,
                16,// responseLength ,
                new byte[0],// requestZip ,
                responseZip,// responseZip,
                402,// responseStatus,
                0,// latency,
                null,// componentId,
                "",// action,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        " <audit xmlns=\"http://l7tech.com/audit/rec\"/>\n" +
                        " "// properties
        );
        AuditDetail detail1 = ExternalAuditsUtils.makeAuditDetail(
                0, // index
                "b9eda451-2556-4d27-a51b-360ded862970", // audit_oid
                1381529379935L , //time
                0, //componentId
                0, //ordinal
                -4, //messageId
                "", //message
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<detail xmlns=\"http://l7tech.com/audit/detail\">\n    <params>\n        <param>awgewag,,,,gewaegw,aa,gew:/:;/\\\\::</param>\n    </params>\n</detail>\n" //properties
        );
        AuditDetail detail2 = ExternalAuditsUtils.makeAuditDetail(
                1, // index
                "b9eda451-2556-4d27-a51b-360ded862970", // audit_oid
                1381529379935L , //time
                0, //componentId
                1, //ordinal
                4104, //messageId
                "", //message
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<detail xmlns=\"http://l7tech.com/audit/detail\">\n    <params>\n        <param>admin</param>\n    </params>\n</detail>\n" //properties
        );
        Set<AuditDetail> details = record.getDetails();
        details.add(detail1);
        details.add(detail2);
        record.setDetails(details);
        assertTrue("Invalid signature", verifyAudit(record, false));
    }

    @Test
    public void db2SystemSignaturePre80()throws Exception {
        AuditRecord  record = ExternalAuditsUtils.makeAuditRecord(
                "23ffedc9-a89f-4dec-8d07-59d779ea6579",//id
                "48b5a4bc60fd4db191ddd5258eab0a35",// nodeid
                1381524564637L, // time
                "system", // type ,
                "800", // auditLevel
                "Email Listeners" , // name
                "Connected to email server (dg)" , // message
                "10.7.49.167",// ip_addr,
                "",// userName,
                "",// userId ,
                "-1",// providerOid ,
                "noPJlhQA6d0M61/+yqgp8tSWxK5eylBQ/YUWIBZ0aq/GWN3H2c/C1UDR0taTlt6PKLW6SEuHobLB17WqGcpwVo4VgdKysI0J2UK0bMHKG8Kt1JwuxZ5GzHvXOBD2Ku6ZX8+Bm8Ngg5b/A31d9SiqxEo+pEM+JDSxeOeeyaKHdz5EA+BMgkAqUzZmAbBq4zlNTQ5oa+HnjR3BQrnVZ6ZAWxSXuqKRHqI3EOBx8x16qYMytwiEanaA17088ik0Xv4+f9I3M6GqQUgb2fNbbjfFnMYGvQycDilqX2ofmBcLD8c9pCZZ3Agsf0jV4Z5O5YLasPIGSRByVTcDFm5BFA6YIA==",// signature,
                "",// entityClass ,
                "",// entityId ,
                null,// status ,
                "",// requestId ,
                "",// serviceGoid ,
                "",// operationName ,
                null,// authenticated ,
                "",// authenticationType,
                -1,// requestLength ,
                -1,// responseLength ,
                new byte[0],// requestZip ,
                new byte[0],// responseZip,
                null,// responseStatus,
                null,// latency,
                1275000,// componentId,
                "Connect",// action,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        " <audit xmlns=\"http://l7tech.com/audit/rec\"/>\n" +
                        " "// properties
        );
        assertTrue("Invalid signature",verifyAudit(record, true));
    }


    @Test
    public void db2ServerAdminSignaturePre80()throws Exception {
        AuditRecord  record = ExternalAuditsUtils.makeAuditRecord(
                "3030985c-4abc-49c3-b24e-c8295c615675",//id
                "48b5a4bc60fd4db191ddd5258eab0a35",// nodeid
                1381523482380L, // time
                "admin", // type ,
                "800", // auditLevel
                "dg" , // name
                "EmailListener #2097152 (dg) created" , // message
                "127.0.0.1",// ip_addr,
                "admin",// userName,
                "3",// userId ,
                "-2",// providerOid ,
                "lv7rU/IcfO+QyRY9dJ1aeTtnr7GjcYbbkjR95VvsFDC3awecOgLce9HlBjmm+vO0R1dEA7wLu6ECYBcPRGlskMxg+8sk9z2woOOfuoKAIaySnQBfmLWW5RU3w9av2KX/mWAWoKmns9QLeA6rukQvuNnTPoIZRpiEqHEw/LLTpRycyiGRrtLX+qxsDeZM8OO5Phsnuy8Ws2CM1sGNau2S7gz6cH6BTCuTAYjTEq3Ly1MAMDcOyvHtA5MJe1wX8RDzet9MsCu5xYWUEtHCSI8BOtFHlQqxle+raDbVjoNpZFYXQor3N/O2l4B/Zccg7po2fWvMWHziollL3LiWNiuEKg==",// signature,
                "com.l7tech.gateway.common.transport.email.EmailListener",// entityClass ,
                "2097152",// entityId ,
                null,// status ,
                "",// requestId ,
                "",// serviceGoid ,
                "",// operationName ,
                null,// authenticated ,
                "",// authenticationType,
                -1,// requestLength ,
                -1,// responseLength ,
                new byte[0],// requestZip ,
                new byte[0],// responseZip,
                null,// responseStatus,
                null,// latency,
                null,// componentId,
                "C",// action,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        " <audit xmlns=\"http://l7tech.com/audit/rec\"/>\n" +
                        " "// properties
        );
        assertTrue("Invalid signature",verifyAudit(record, true));
    }


    @Test
    public void db2ServerMessageSignaturePre80()throws Exception {
        byte[] responseZip = CompressedStringType.compress("<asdf>hihi</asdf>");
        AuditRecord  record = ExternalAuditsUtils.makeAuditRecord(
                "2c9be48a-df8e-4a0d-9b72-067ae030ce9d",//id
                "48b5a4bc60fd4db191ddd5258eab0a35",// nodeid
                1381523731255L, // time
                "message", // type ,
                "900", // auditLevel
                "test [/test]" , // name
                "Message processed successfully" , // message
                "127.0.0.1",// ip_addr,
                "",// userName,
                "",// userId ,
                "-1",// providerOid ,
                "pMWQ+T9cZIBBFydeTu9Xek7UJ+2kdkT1NZndRVgrw4ougMo2wukMtnpWAXfJUnOrlLyAqsUkoBbuKc6Xz/f+lyDF/hVPoQZlDMq+31tOO7+1r2t9yTKaPPl81eSFyzb7iKnSjn5bTEicKKaYIxaXImRqFNp29uG71baj8rXVjH9VOrpmmnvub4DpqjPAAUJUjOJx8dta9YwOs9ks57EsX75hDWf+tomI0MM46foFjtcLorSqCIAJRXZ9y1mHjzvVgcC92fD7fInZvNMQz0ZDydndO2f05xQfTpLRPIWmfXYAYeK8/p55n2Bsxv78VnlvEU+OV96aP+0UTLT2wN56ow==",// signature,
                "",// entityClass ,
                "",// entityId ,
                0,// status ,
                "00000141a8e5382b-65",// requestId ,
                "229377",// serviceGoid ,
                "",// operationName ,
                Boolean.FALSE,// authenticated ,
                "",// authenticationType,
                0,// requestLength ,
                17,// responseLength ,
                new byte[0],// requestZip ,
                responseZip,// responseZip,
                200,// responseStatus,
                0,// latency,
                null,// componentId,
                "",// action,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        " <audit xmlns=\"http://l7tech.com/audit/rec\"/>\n" +
                        " "// properties
        );
        AuditDetail detail = ExternalAuditsUtils.makeAuditDetail(
                1, // index
                "2c9be48a-df8e-4a0d-9b72-067ae030ce9d", // audit_oid
                1381523731253L , //time
                0, //componentId
                0, //ordinal
                -4, //messageId
                "", //message
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<detail xmlns=\"http://l7tech.com/audit/detail\">\n    <params>\n        <param>awgewaggewaegwaagew</param>\n    </params>\n</detail>\n" //properties
        );
        Set<AuditDetail> details = record.getDetails();
        details.add(detail);
        record.setDetails(details);
        assertTrue("Invalid signature", verifyAudit(record, true));
    }

    @Test
    public void db2SystemSignature()throws Exception {
        AuditRecord  record = ExternalAuditsUtils.makeAuditRecord(
                "ff5e89ca-edd8-496d-a8cd-3bac95e7d188",//id
                "48b5a4bc60fd4db191ddd5258eab0a35",// nodeid
                1381528192779L, // time
                "system", // type ,
                "800", // auditLevel
                "Email Listeners" , // name
                "Connected to email server (qwert)" , // message
                "10.7.49.167",// ip_addr,
                "",// userName,
                "",// userId ,
                "",// providerOid ,
                "Rvk5Wn8Q2XHA7d2vq1DbDFoDqDcB8D4AUSv3fP3oueVb3eo0KNuXm+rWqr7DIsdEQ+q4oRyxtvk7blJtbF7ZLYldJp9NhyaQWVOu3EUbW8QA+jPFl+gZvU7qzsMh4QHWgA19sH3NQQ1M7/kBJ7+WismzxqWBqjJvj5tm25Kb1jnx2gRcXqOkxSN+pgTYnmr/1USbwhIc2kpExk7ZhgIQ3SJBz4NBu3fT2PP0+rDWRCTwEseuLcvMVZfGWB7rICWt3iJQIOrd96EoEHxfRzvnpUq8mV4UgeCuShwuVrODo4iSPM36wC6A7ioc1UmdhzKAp8OjCe3weHpshGXuj2HGTg==",// signature,
                "",// entityClass ,
                "",// entityId ,
                null,// status ,
                "",// requestId ,
                "",// serviceGoid ,
                "",// operationName ,
                null,// authenticated ,
                "",// authenticationType,
                -1,// requestLength ,
                -1,// responseLength ,
                new byte[0],// requestZip ,
                new byte[0],// responseZip,
                null,// responseStatus,
                null,// latency,
                1275000,// componentId,
                "Connect",// action,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        " <audit xmlns=\"http://l7tech.com/audit/rec\"/>\n" +
                        " "// properties
        );
        assertTrue("Invalid signature",verifyAudit(record, false));
    }


    @Test
    public void db2ServerAdminSignature()throws Exception {
        AuditRecord  record = ExternalAuditsUtils.makeAuditRecord(
                "965954db-5836-44d5-bcae-d5ab0c838aae",//id
                "48b5a4bc60fd4db191ddd5258eab0a35",// nodeid
                1381527822179L, // time
                "admin", // type ,
                "800", // auditLevel
                "qwert" , // name
                "EmailListener #7eb12b2c2e5c3bba725bb9577cad21fc (qwert) updated (changed active)" , // message
                "127.0.0.1",// ip_addr,
                "admin",// userName,
                "00000000000000000000000000000003",// userId ,
                "0000000000000000fffffffffffffffe",// providerOid ,
                "GHHHyGG4khoSxQN0BBznfvUCMF4J/bZEmKFLUEgyjkAMER+dRya+BwVbutwkZ2mIgONE2OqD6vfSSZj2ep3ZVaRKvRtkFZGJ/l4n1snd+S7v9dXArJWM7Do6GBXLYizEX90d0B4PkNR8hhI4QqDNDg8KQFavIJvVqoOF6YZpwxDRj/nhbg1aIuVzdeUpIh2Z7yFtkvkZhZOHaZeX4POEWtmV4Ds7ajhIn7Y/5/0sd9yBu+jkMiV9srGhBjv24NWvaWQSC/Eq38U/kk77vTBRJgDl00luz7ClbRPQEjqHbENIfTOMIKR1jGEaaO8swbfp6Rz8Fv2QjnB/16ywHdwVdg==",// signature,
                "com.l7tech.gateway.common.transport.email.EmailListener",// entityClass ,
                "7eb12b2c2e5c3bba725bb9577cad21fc",// entityId ,
                null,// status ,
                "",// requestId ,
                "",// serviceGoid ,
                "",// operationName ,
                null,// authenticated ,
                "",// authenticationType,
                -1,// requestLength ,
                -1,// responseLength ,
                new byte[0],// requestZip ,
                new byte[0],// responseZip,
                null,// responseStatus,
                null,// latency,
                null,// componentId,
                "U",// action,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        " <audit xmlns=\"http://l7tech.com/audit/rec\"/>\n" +
                        " "// properties
        );
        assertTrue("Invalid signature",verifyAudit(record, false));
    }


    @Test
    public void db2ServerMessageSignature()throws Exception {
        byte[] responseZip = CompressedStringType.compress("<qwer>AAA<\\qwer>");
        AuditRecord  record = ExternalAuditsUtils.makeAuditRecord(
                "f9a5f160-94b3-4233-8348-a20ea522a43a",//id
                "48b5a4bc60fd4db191ddd5258eab0a35",// nodeid
                1381527764962L, // time
                "message", // type ,
                "900", // auditLevel
                "test [/test]" , // name
                "Message processed with HTTP error code" , // message
                "127.0.0.1",// ip_addr,
                "",// userName,
                "",// userId ,
                "",// providerOid ,
                "Jfkb+LCWQW2mUu4SIJ6JQkV13257wwjIyxZRPR5JGVOs8N/GoezSyYTR9VV1HSsrEX6HilY46qK6sDBsoRaVKtkhmL4hExpLH4MKqZZV143qwHBK07IrFmiaV0514vQbQkI4oxXARO4DqpzvFbzpZk+zpKpBzlk0flgU589tnQw5qoyTsHPFtfG1HFaL9afpVgbvh9kzxgNTjMCgwQpjr6yvAjFO9siNxHxDdct76RC5Xi5YblZ4zxcOIWkcgf3tM77EwK3czz/fVEE/SLo4WO/K/Wehn0S3XAdMuiBbBeJz5a7VT/ARgN6DyRPXOnqGCYaSdHbQh+f7einCJpGMhw==",// signature,
                "",// entityClass ,
                "",// entityId ,
                0,// status ,
                "00000141a95153de-1d",// requestId ,
                "b5157238ab4922f40000000000038001",// serviceGoid ,
                "",// operationName ,
                Boolean.FALSE,// authenticated ,
                "",// authenticationType,
                0,// requestLength ,
                16,// responseLength ,
                new byte[0],// requestZip ,
                responseZip,// responseZip,
                402,// responseStatus,
                0,// latency,
                null,// componentId,
                "",// action,
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        " <audit xmlns=\"http://l7tech.com/audit/rec\"/>\n" +
                        " "// properties
        );
        AuditDetail detail1 = ExternalAuditsUtils.makeAuditDetail(
                0, // index
                "f9a5f160-94b3-4233-8348-a20ea522a43a", // audit_oid
                1381527764751L , //time
                0, //componentId
                0, //ordinal
                -4, //messageId
                "", //message
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<detail xmlns=\"http://l7tech.com/audit/detail\">\n    <params>\n        <param>awgewag,,,,gewaegw,aa,gew:/:;/\\\\::</param>\\n    </params>\n</detail>\n" //properties
        );  AuditDetail detail2 = ExternalAuditsUtils.makeAuditDetail(
                1, // index
                "f9a5f160-94b3-4233-8348-a20ea522a43a", // audit_oid
                1381527764756L , //time
                0, //componentId
                1, //ordinal
                4104, //messageId
                "", //message
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<detail xmlns=\"http://l7tech.com/audit/detail\">\n    <params>\n        <param>admin</param>\n    </params>\n</detail>\n" //properties
        );
        Set<AuditDetail> details = record.getDetails();
        details.add(detail1);
        details.add(detail2);
        record.setDetails(details);
        assertTrue("Invalid signature", verifyAudit(record, false));
    }


    private static X509Certificate cert;

    @Before
    public void init() throws Exception {
        String serverCertB64 = "-----BEGIN CERTIFICATE-----\n" +
                "MIIDBTCCAe2gAwIBAgIJAKoOGPxRawITMA0GCSqGSIb3DQEBDAUAMCAxHjAcBgNVBAMTFXd5bm5l\n" +
                "LXBjLmw3dGVjaC5sb2NhbDAeFw0xMzA5MTYyMjU0MTRaFw0yMzA5MTQyMjU0MTRaMCAxHjAcBgNV\n" +
                "BAMTFXd5bm5lLXBjLmw3dGVjaC5sb2NhbDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEB\n" +
                "ALrZmtaoj79lrJb5caZ+ZWr8nqJXi1TpxdIkmsTqswwYaTEriIVPYVWai45JtUyeKkIo/WPYEleK\n" +
                "iZAWGNDvqbEdyx4AlBsqTOKw0Dp5ZFaxZPpmcyZfpnbhbacfLZNY4gR2sgh0P3iDV6RcR4LCTRX3\n" +
                "JepnWe1i09WA21bhruOdJZeXL/S1dQgP7+iWDtjTrkEmwxdxHBf/RHqsfpARD3eL77pUcvEtTUGl\n" +
                "l6c7JCzAegwPUys9n5JadfjJ5ZlaOnaOhwBr1g8E9tO8SXwo0PM6cwWyg72c8iE8UGYxrXLNUhCz\n" +
                "u8CEhmMjEpCaG8zYR6o1b8Dz7T8hBgUkTizbUrECAwEAAaNCMEAwHQYDVR0OBBYEFAPBfTWbzvF/\n" +
                "4yMwpEhOAZfg+y4vMB8GA1UdIwQYMBaAFAPBfTWbzvF/4yMwpEhOAZfg+y4vMA0GCSqGSIb3DQEB\n" +
                "DAUAA4IBAQBDjV9ijaQAVubjyMexXWE6fktENeCGBRpjqMOAKR7yXQd1mvZHN3+jSSpCAXjCGPpM\n" +
                "x45V9ou8iHN4Aw3CpxPAPxF8iJnlUXlBLXR4QxRYVv+mMfD09IVrfS47D0ktKfWJwLsMXULPIWka\n" +
                "eJu3bYECZ98TMuXpi7EWuIGTQmIJ2ooSvrZwlj4NNwMCr2ETOb1f3KJ5ME4mbYUkcy2JLEDqqkoj\n" +
                "pZNDLC4Tzk5SmqLs8zUfwUvp7TkC6cAD+Zt6LWJ7wvILu5dS7Ng6bq2l5+Uz4lyKlKPct0BsYFPo\n" +
                "X1Mw90lboqF7x65/qK5fxxs/yMuetBFGjZ7uuqV9EyjhAmq/\n" +
                "-----END CERTIFICATE-----\n";

        cert = CertUtils.decodeCert(serverCertB64.getBytes());
    }

    static private boolean verifyAudit(AuditRecord record, boolean isPre80) throws Exception {
        return new AuditRecordVerifier(cert).verifySignatureOfDigest(record.getSignature(), isPre80? record.computeOldIdSignatureDigest(): record.computeSignatureDigest());
    }
}
