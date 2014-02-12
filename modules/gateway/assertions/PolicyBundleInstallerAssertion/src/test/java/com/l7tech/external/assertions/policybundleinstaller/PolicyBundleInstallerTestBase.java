package com.l7tech.external.assertions.policybundleinstaller;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.bundle.BundleInfo;
import com.l7tech.server.event.wsman.PolicyBundleEvent;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.bundle.BundleResolver;
import com.l7tech.util.Functions;
import com.l7tech.util.IOUtils;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;

public abstract class PolicyBundleInstallerTestBase {
    protected final static String TEST_BUNDLE_BASE_NAME = "/com/l7tech/external/assertions/policybundleinstaller/bundles";
    protected final static String OAUTH_TEST_BUNDLE_BASE_NAME = "/com/l7tech/external/assertions/policybundleinstaller/bundles/oauthtest";
    protected final static String OAUTH_TEST_BUNDLE_ID = "4e321ca1-83a0-4df5-8216-c2d2bb36067d";
    protected final static String SIMPLE_TEST_BUNDLE_BASE_NAME = "/com/l7tech/external/assertions/policybundleinstaller/bundles/simpletest";
    protected final static String SIMPLE_TEST_BUNDLE_ID = "33b16742-d62d-4095-8f8d-4db707e9ad51";

    /**
     * This is a canned response useful for faking a create ID - don't use to verify types, message ids etc
     */
    protected final static String CANNED_CREATE_ID_RESPONSE_TEMPLATE = "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"\n" +
            "    xmlns:mdo=\"http://schemas.wiseman.dev.java.net/metadata/messagetypes\"\n" +
            "    xmlns:mex=\"http://schemas.xmlsoap.org/ws/2004/09/mex\"\n" +
            "    xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\"\n" +
            "    xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\"\n" +
            "    xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\"\n" +
            "    xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"\n" +
            "    xmlns:wsmeta=\"http://schemas.dmtf.org/wbem/wsman/1/wsman/version1.0.0.a/default-addressing-model.xsd\"\n" +
            "    xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "    <env:Header>\n" +
            "        <wsa:Action env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">http://schemas.xmlsoap.org/ws/2004/09/transfer/CreateResponse</wsa:Action>\n" +
            "        <wsa:MessageID env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">uuid:ce1f79e3-479d-4602-a93c-230bfe0f6050</wsa:MessageID>\n" +
            "        <wsa:RelatesTo xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">uuid:73442d63-37ee-4908-8d18-c635e327d515</wsa:RelatesTo>\n" +
            "        <wsa:To env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:To>\n" +
            "    </env:Header>\n" +
            "    <env:Body>\n" +
            "        <wxf:ResourceCreated xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
            "            <wsa:Address env:mustUnderstand=\"true\">https://localhost:9443/wsman/</wsa:Address>\n" +
            "            <wsa:ReferenceParameters>\n" +
            "                <wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/folders</wsman:ResourceURI>\n" +
            "                <wsman:SelectorSet>\n" +
            "                    <wsman:Selector Name=\"id\">{0}</wsman:Selector>\n" +
            "                </wsman:SelectorSet>\n" +
            "            </wsa:ReferenceParameters>\n" +
            "        </wxf:ResourceCreated>\n" +
            "    </env:Body>\n" +
            "</env:Envelope>";

    protected final static String CANNED_ENUMERATE_WITH_FILTER_AND_EPR_RESPONSE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"+
            "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"\n"+
            "    xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\"\n"+
            "    xmlns:mdo=\"http://schemas.wiseman.dev.java.net/metadata/messagetypes\"\n"+
            "    xmlns:mex=\"http://schemas.xmlsoap.org/ws/2004/09/mex\"\n"+
            "    xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\"\n"+
            "    xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\"\n"+
            "    xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\"\n"+
            "    xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"\n"+
            "    xmlns:wsmeta=\"http://schemas.dmtf.org/wbem/wsman/1/wsman/version1.0.0.a/default-addressing-model.xsd\"\n"+
            "    xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n"+
            "    <env:Header>\n"+
            "        <wsa:Action env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">http://schemas.xmlsoap.org/ws/2004/09/enumeration/EnumerateResponse</wsa:Action>\n"+
            "        <wsman:TotalItemsCountEstimate>56</wsman:TotalItemsCountEstimate>\n"+
            "        <wsa:MessageID env:mustUnderstand=\"true\">uuid:580d6add-30b5-468f-be33-cbe2112ce845</wsa:MessageID>\n"+
            "        <wsa:RelatesTo>uuid:53418e52-f8a1-4c8e-94b7-a3f4fe33d8f3</wsa:RelatesTo>\n"+
            "        <wsa:To env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:To>\n"+
            "    </env:Header>\n"+
            "    <env:Body>\n"+
            "        <wsen:EnumerateResponse>\n"+
            "            <wsen:Expires>2147483647-12-31T23:59:59.999-14:00</wsen:Expires>\n"+
            "            <wsen:EnumerationContext>55bd9e59-9dc7-43c9-8d6a-4e2c2f940659</wsen:EnumerationContext>\n"+
            "            <wsman:Items>\n"+
            "                <wsman:Item>\n"+
            "                    <wsman:XmlFragment>\n"+
            "                        <l7:Name>OAuth</l7:Name>\n"+
            "                    </wsman:XmlFragment>\n"+
            "                    <wsa:EndpointReference>\n"+
            "                        <wsa:Address env:mustUnderstand=\"true\">http://127.0.0.1:80/wsman</wsa:Address>\n"+
            "                        <wsa:ReferenceParameters>\n"+
            "                            <wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/folders</wsman:ResourceURI>\n"+
            "                            <wsman:SelectorSet>\n"+
            "                                <wsman:Selector Name=\"id\">{0}</wsman:Selector>\n"+
            "                            </wsman:SelectorSet>\n"+
            "                        </wsa:ReferenceParameters>\n"+
            "                    </wsa:EndpointReference>\n"+
            "                </wsman:Item>\n"+
            "            </wsman:Items>\n"+
            "            <wsman:EndOfSequence/>\n"+
            "        </wsen:EnumerateResponse>\n"+
            "    </env:Body>\n"+
            "</env:Envelope>";

    protected static final String alreadyExistsResponse = "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"\n" +
            "    xmlns:mdo=\"http://schemas.wiseman.dev.java.net/metadata/messagetypes\"\n" +
            "    xmlns:mex=\"http://schemas.xmlsoap.org/ws/2004/09/mex\"\n" +
            "    xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\"\n" +
            "    xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\"\n" +
            "    xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\"\n" +
            "    xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"\n" +
            "    xmlns:wsmeta=\"http://schemas.dmtf.org/wbem/wsman/1/wsman/version1.0.0.a/default-addressing-model.xsd\"\n" +
            "    xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "    <env:Header>\n" +
            "        <wsa:Action env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">http://schemas.dmtf.org/wbem/wsman/1/wsman/fault</wsa:Action>\n" +
            "        <wsa:MessageID env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">uuid:7486aa54-f144-4656-badf-16d9570fc37d</wsa:MessageID>\n" +
            "        <wsa:RelatesTo xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">uuid:6a947b0a-415d-490d-a1d1-fcf57a2ba329</wsa:RelatesTo>\n" +
            "        <wsa:To env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:To>\n" +
            "    </env:Header>\n" +
            "    <env:Body>\n" +
            "        <env:Fault xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
            "            <env:Code>\n" +
            "                <env:Value>env:Sender</env:Value>\n" +
            "                <env:Subcode>\n" +
            "                    <env:Value>wsman:AlreadyExists</env:Value>\n" +
            "                </env:Subcode>\n" +
            "            </env:Code>\n" +
            "            <env:Reason>\n" +
            "                <env:Text xml:lang=\"en-US\">The sender attempted to create a resource which already exists.</env:Text>\n" +
            "            </env:Reason>\n" +
            "            <env:Detail>\n" +
            "                <env:Text xml:lang=\"en-US\">(folder, name)  must be unique</env:Text>\n" +
            "            </env:Detail>\n" +
            "        </env:Fault>\n" +
            "    </env:Body>\n" +
            "</env:Envelope>";

    protected static final String FILTER_NO_RESULTS = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"\n" +
            "    xmlns:mdo=\"http://schemas.wiseman.dev.java.net/metadata/messagetypes\"\n" +
            "    xmlns:mex=\"http://schemas.xmlsoap.org/ws/2004/09/mex\"\n" +
            "    xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\"\n" +
            "    xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\"\n" +
            "    xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\"\n" +
            "    xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"\n" +
            "    xmlns:wsmeta=\"http://schemas.dmtf.org/wbem/wsman/1/wsman/version1.0.0.a/default-addressing-model.xsd\"\n" +
            "    xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "    <env:Header>\n" +
            "        <wsa:Action env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">http://schemas.xmlsoap.org/ws/2004/09/enumeration/EnumerateResponse</wsa:Action>\n" +
            "        <wsman:TotalItemsCountEstimate xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">135</wsman:TotalItemsCountEstimate>\n" +
            "        <wsa:MessageID env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">uuid:19c6ab84-e9e7-4bf1-8114-b2d74ba41f6d</wsa:MessageID>\n" +
            "        <wsa:RelatesTo xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">uuid:cdf8352f-eb06-4d90-a728-c7fa7560adca</wsa:RelatesTo>\n" +
            "        <wsa:To env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:To>\n" +
            "    </env:Header>\n" +
            "    <env:Body>\n" +
            "        <wsen:EnumerateResponse xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
            "            <wsen:Expires>2147483647-12-31T23:59:59.999-14:00</wsen:Expires>\n" +
            "            <wsen:EnumerationContext>85bc6f2a-3c00-45df-87fa-056f623d0dd8</wsen:EnumerationContext>\n" +
            "            <wsman:Items/>\n" +
            "            <wsman:EndOfSequence/>\n" +
            "        </wsen:EnumerateResponse>\n" +
            "    </env:Body>\n" +
            "</env:Envelope>\n";

    protected int nextOid = 1000000;

    protected Functions.Nullary<Boolean> getCancelledCallback(final PolicyBundleEvent bundleEvent) {
        return new Functions.Nullary<Boolean>() {
            @Override
            public Boolean call() {
                return bundleEvent.isCancelled();
            }
        };
    }

    @NotNull
    protected BundleResolver getBundleResolver(final String bundleBaseName, final String bundleId){
        final Map<String, Map<String, Document>> bundleToItemAndDocMap = new HashMap<>();
        bundleToItemAndDocMap.put(bundleId, getItemsToDocs(bundleBaseName));

        return new BundleResolver() {
            @Override
            public Document getBundleItem(@NotNull String bundleId, @NotNull BundleItem bundleItem, boolean allowMissing) throws UnknownBundleException, BundleResolverException {
                final Map<String, Document> itemToDocMap = bundleToItemAndDocMap.get(bundleId);
                return itemToDocMap.get(bundleItem.getFileName());
            }

            @NotNull
            @Override
            public List<BundleInfo> getResultList() {
                return Arrays.asList(new BundleInfo(bundleId, "1.0", "Name", "Desc"));
            }

            @Override
            public void setInstallationPrefix(@Nullable String installationPrefix) {
                // do nothing
            }
        };
    }

    @NotNull
    protected BundleResolver getBundleResolver(){
        return getBundleResolver(OAUTH_TEST_BUNDLE_BASE_NAME, OAUTH_TEST_BUNDLE_ID);
    }

    private Document getDocumentFromResource(String resource) {
        final URL resourceUrl = getClass().getResource(resource);
        final byte[] bytes;
        try {
            bytes = IOUtils.slurpUrl(resourceUrl);
            return XmlUtil.parse(new ByteArrayInputStream(bytes));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Map<String, Document> getItemsToDocs(final String bundleBaseName) {
        final Map<String, Document> itemsToDocs = new HashMap<>();

        itemsToDocs.put("Folder.xml", getDocumentFromResource(bundleBaseName + "/Folder.xml"));
        itemsToDocs.put("Service.xml", getDocumentFromResource(bundleBaseName + "/Service.xml"));
        itemsToDocs.put("TrustedCertificate.xml", getDocumentFromResource(bundleBaseName + "/TrustedCertificate.xml"));
        itemsToDocs.put("Assertion.xml", getDocumentFromResource(bundleBaseName + "/Assertion.xml"));
        itemsToDocs.put("Policy.xml", getDocumentFromResource(bundleBaseName + "/Policy.xml"));
        itemsToDocs.put("EncapsulatedAssertion.xml", getDocumentFromResource(bundleBaseName + "/EncapsulatedAssertion.xml"));

        return itemsToDocs;
    }

    protected Map<String, Goid> getFolderIds() {
        // fake the folder ids
        return new HashMap<String, Goid>(){
            @Override
            public Goid get(Object key) {
                try{
                    return Goid.parseGoid(key.toString());
                } catch(IllegalArgumentException e) {
                    return new Goid(0, Long.valueOf(key.toString()));
                }
            }

            @Override
            public boolean containsKey(Object key) {
                return true;
            }
        };
    }

    protected Map<String, String> getPolicyGuids() {
        return new HashMap<String, String>(){
            @Override
            public String get(Object key) {
                return UUID.randomUUID().toString();
            }

            @Override
            public boolean containsKey(Object key) {
                return true;
            }
        };
    }


    protected Pair<AssertionStatus, Document> cannedIdResponse(Document requestXml) {
        try {
            System.out.println(XmlUtil.nodeToFormattedString(requestXml));
            final String format = MessageFormat.format(CANNED_CREATE_ID_RESPONSE_TEMPLATE, String.valueOf(new Goid(0,nextOid++)));
            final Document parse = XmlUtil.parse(format);
            return new Pair<>(AssertionStatus.NONE, parse);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void setResponse(PolicyEnforcementContext context, String response) {
        try {
            setResponse(context, XmlUtil.parse(response));
        } catch (SAXException e) {
            throw new RuntimeException(e);
        }
    }

    protected static void setResponse(PolicyEnforcementContext context, Document response) {
        final Message responseMsg = context.getResponse();
        responseMsg.initialize(response);
    }


}
