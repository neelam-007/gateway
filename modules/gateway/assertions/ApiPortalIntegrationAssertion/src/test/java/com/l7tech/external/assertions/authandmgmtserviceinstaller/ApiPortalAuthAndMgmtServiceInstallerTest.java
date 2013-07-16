package com.l7tech.external.assertions.authandmgmtserviceinstaller;

import com.l7tech.util.DomUtils;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.net.URL;
import java.util.List;

import static com.l7tech.external.assertions.authandmgmtserviceinstaller.ApiPortalAuthAndMgmtServiceInstallerConstants.*;
import static com.l7tech.external.assertions.authandmgmtserviceinstaller.ApiPortalAuthAndMgmtServicePolicyResolverUtils.*;
import static org.junit.Assert.assertTrue;

/**
 * @author ghuang
 */
public class ApiPortalAuthAndMgmtServiceInstallerTest {
    private static final String[] LEFT_COMMENT_NAMES_USED_IN_MAIN_POLICY = new String[] {
        LEFT_COMMENT_NAME_PREFIX,
        LEFT_COMMENT_NAME_SSG_LDAP_PROVIDER_NAME,
        LEFT_COMMENT_NAME_MGMT_BASE_DN,
        LEFT_COMMENT_NAME_MGMT_USER_ACCOUNT_CONTROL,
        LEFT_COMMENT_NAME_MGMT_GROUP_NAME,
        LEFT_COMMENT_NAME_MGMT_GROUP_NAME_FOR_ORGADMIN,
        LEFT_COMMENT_NAME_MGMT_USER_DN_CREATE_FORMAT,
        LEFT_COMMENT_NAME_MGMT_USER_DN_MEMBER_FORMAT,
        LEFT_COMMENT_NAME_MGMT_GROUP_DN,
        LEFT_COMMENT_NAME_MGMT_USER_SEARCH_FILTER,
        LEFT_COMMENT_NAME_MGMT_GROUP_ATTRIBUTE,
        LEFT_COMMENT_NAME_MGMT_USERNAME_ATTRIBUTE,
        LEFT_COMMENT_NAME_MGMT_PASSWORD_ATTRIBUTE,
        LEFT_COMMENT_NAME_MGMT_OBJECTCLASS,
        LEFT_COMMENT_NAME_MGMT_HAS_UID_ATTRIBUTES,
        LEFT_COMMENT_NAME_AUTHENTICATION_PROVIDER,
        LEFT_COMMENT_NAME_MANAGEMENT_HANDLER,
        LEFT_COMMENT_NAME_MGMT_QUERY_LDAP
    };

    private static final String[] LEFT_COMMENT_NAMES_USED_IN_MSAD_LDAP_AUTH_SECTION_POLICY = new String[] {
        LEFT_COMMENT_NAME_AUTH_MSAD_LDAP_SEARCH_FILTER
    };

    private static final String[] LEFT_COMMENT_NAMES_USED_IN_OPEN_LDAP_AUTH_SECTION_POLICY = new String[] {
        LEFT_COMMENT_NAME_AUTH_OPEN_LDAP_USER_SEARCH_FILTER,
        LEFT_COMMENT_NAME_AUTH_OPEN_LDAP_GROUP_SEARCH_FILTER,
    };


    @Test
    public void testBundleFileAndPolicyFilesExisting() throws Exception {

        URL fileResource = getClass().getResource(INSTALLER_BUNDLE_INFO_FILE_PATH);
        assertTrue(INSTALLER_BUNDLE_INFO_FILE_PATH + " found", fileResource != null);

        String policyFilePath = "/" + POLICY_RESOURCE_BASE_NAME;
        fileResource = getClass().getResource(policyFilePath + AUTHENTICATION_MAIN_POLICY_FILE_NAME);
        assertTrue(policyFilePath + AUTHENTICATION_MAIN_POLICY_FILE_NAME + " found", fileResource != null);

        fileResource = getClass().getResource(policyFilePath + MSAD_LDAP_AUTH_SECTION_POLICY_FILE_NAME);
        assertTrue(policyFilePath + MSAD_LDAP_AUTH_SECTION_POLICY_FILE_NAME + " found", fileResource != null);

        fileResource = getClass().getResource(policyFilePath + OPEN_LDAP_AUTH_SECTION_POLICY_FILE_NAME);
        assertTrue(policyFilePath + OPEN_LDAP_AUTH_SECTION_POLICY_FILE_NAME + " found", fileResource != null);
    }

    @Test
    public void testVerifySearchingLeftCommentNames() throws Exception {
        Document authPolicyDocument = readPolicyFile(POLICY_RESOURCE_BASE_NAME + AUTHENTICATION_MAIN_POLICY_FILE_NAME);

        for (String leftCommentName: LEFT_COMMENT_NAMES_USED_IN_MAIN_POLICY) {
            final List<Element> foundComments = findElements(authPolicyDocument.getDocumentElement(), ".//L7p:value[@stringValue='" + leftCommentName + "']", getNamespaceMap());
            boolean found = false;
            for (Element foundComment : foundComments) {
                // verify it's a left comment
                final Element entryParent = (Element) foundComment.getParentNode();
                final Node keyElm = DomUtils.findFirstChildElement(entryParent);
                final Node stringValue = keyElm.getAttributes().getNamedItem("stringValue");
                if ("LEFT.COMMENT".equals(stringValue.getNodeValue())) {
                    found = true;
                }
            }
            assertTrue("The Left Comment: " + leftCommentName + " found", found);
        }

        authPolicyDocument = readPolicyFile(POLICY_RESOURCE_BASE_NAME + MSAD_LDAP_AUTH_SECTION_POLICY_FILE_NAME);
        for (String leftCommentName: LEFT_COMMENT_NAMES_USED_IN_MSAD_LDAP_AUTH_SECTION_POLICY) {
            final List<Element> foundComments = findElements(authPolicyDocument.getDocumentElement(), ".//L7p:value[@stringValue='" + leftCommentName + "']", getNamespaceMap());
            boolean found = false;
            for (Element foundComment : foundComments) {
                // verify it's a left comment
                final Element entryParent = (Element) foundComment.getParentNode();
                final Node keyElm = DomUtils.findFirstChildElement(entryParent);
                final Node stringValue = keyElm.getAttributes().getNamedItem("stringValue");
                if ("LEFT.COMMENT".equals(stringValue.getNodeValue())) {
                    found = true;
                }
            }
            assertTrue("The Left Comment: " + leftCommentName + " found", found);
        }

        authPolicyDocument = readPolicyFile(POLICY_RESOURCE_BASE_NAME + OPEN_LDAP_AUTH_SECTION_POLICY_FILE_NAME);
        for (String leftCommentName: LEFT_COMMENT_NAMES_USED_IN_OPEN_LDAP_AUTH_SECTION_POLICY) {
            final List<Element> foundComments = findElements(authPolicyDocument.getDocumentElement(), ".//L7p:value[@stringValue='" + leftCommentName + "']", getNamespaceMap());
            boolean found = false;
            for (Element foundComment : foundComments) {
                // verify it's a left comment
                final Element entryParent = (Element) foundComment.getParentNode();
                final Node keyElm = DomUtils.findFirstChildElement(entryParent);
                final Node stringValue = keyElm.getAttributes().getNamedItem("stringValue");
                if ("LEFT.COMMENT".equals(stringValue.getNodeValue())) {
                    found = true;
                }
            }
            assertTrue("The Left Comment: " + leftCommentName + " found", found);
        }
    }
}