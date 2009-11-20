package com.l7tech.external.assertions.xmlsec.server;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.xmlsec.NonSoapCheckVerifyResultsAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.Functions;
import com.l7tech.util.TextUtils;
import com.l7tech.xml.InvalidXpathException;
import com.l7tech.xml.xpath.XpathExpression;
import static org.junit.Assert.*;
import org.junit.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 *
 */
public class ServerNonSoapCheckVerifyResultsAssertionTest {
    static final String RSA_SHA1 = "http://www.w3.org/2000/09/xmldsig#rsa-sha1";
    static final String ECDSA_SHA384 = "http://www.w3.org/2001/04/xmldsig-more#ecdsa-sha384";
    static final String SHA384 = "http://www.w3.org/2001/04/xmldsig-more#sha384";
    static final String SHA1 = "http://www.w3.org/2000/09/xmldsig#sha1";

    Document pets;
    Element cat;
    Element dog;
    Element snake;
    Message message;
    PolicyEnforcementContext pec;
    NonSoapCheckVerifyResultsAssertion ass;
    X509Certificate alice;
    X509Certificate bob;
    Set<X509Certificate> knownCerts;

    void select(Element... elements) {
        // Set xpath expression to examine elements /pets/cat|/pets/dog  (if called with cat,dog as arguments)

        List<String> names = Functions.map(Arrays.asList(elements), new Functions.Unary<String, Element>() {
            @Override
            public String call(Element element) {
                return "/pets/" + element.getNodeName();
            }
        });

        ass.setXpathExpression(new XpathExpression(TextUtils.join("|", names).toString()));
    }

    void multi() {
        ass.setAllowMultipleSigners(true);
    }

    void nomulti() {
        ass.setAllowMultipleSigners(false);
    }

    void elementsVerified(Element... elements) {
        pec.setVariable("p.elementsVerified", elements);
    }

    void sigMethods(String... signatureMethodUris) {
        pec.setVariable("p.signatureMethodUris", signatureMethodUris);
    }

    void allSigMethods(String signatureMethodUri) throws NoSuchVariableException {
        String[] uris = new String[elementsVerifiedCount()];
        Arrays.fill(uris, signatureMethodUri);
        sigMethods(uris);
    }

    void digMethods(String... digestMethodUris) {
        pec.setVariable("p.digestMethodUris", digestMethodUris);
    }

    void allDigMethods(String digestMethodUri) throws NoSuchVariableException {
        String[] uris = new String[elementsVerifiedCount()];
        Arrays.fill(uris, digestMethodUri);
        digMethods(uris);
    }

    void signers(X509Certificate... signingCertificates) {
        pec.setVariable("p.signingCertificates", signingCertificates);
    }

    int arraySize(String varname) throws NoSuchVariableException {
        Object[] array = (Object[]) pec.getVariable(varname);
        return array.length;
    }

    int elementsVerifiedCount() throws NoSuchVariableException {
        return arraySize("p.elementsVerified");
    }

    void allowSig(String... uris) {
        ass.setPermittedSignatureMethodUris(uris);
    }

    void allowDig(String... uris) {
        ass.setPermittedDigestMethodUris(uris);
    }

    @Before
    public void setUp() throws Exception {
        // Signer certs
        alice = NonSoapXmlSecurityTestUtils.getTestKey().getCertificate();
        bob = NonSoapXmlSecurityTestUtils.getDataKey().getCertificate();
        knownCerts = ServerNonSoapCheckVerifyResultsAssertion.makeCertCollector();
        knownCerts.addAll(Arrays.asList(alice, bob));

        // Test document, elements, Message, and PEC
        pets = XmlUtil.createEmptyDocument("pets", null, null);
        final Element doc = pets.getDocumentElement();
        cat = XmlUtil.createAndAppendElement(doc, "cat");
        cat.setAttribute("color", "black");
        dog = XmlUtil.createAndAppendElement(doc, "dog");
        snake = XmlUtil.createAndAppendElement(doc, "snake");
        message = new Message(pets);
        pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext(message, new Message());

        // Default assertion bean config for these tests
        ass = new NonSoapCheckVerifyResultsAssertion();
        ass.setVariablePrefix("p");
        select(cat);
        nomulti();
        allowSig(ECDSA_SHA384, RSA_SHA1);
        allowDig(SHA1, SHA384);

        // Results table
        elementsVerified(cat, snake, dog, snake);
        sigMethods(ECDSA_SHA384, ECDSA_SHA384, RSA_SHA1, RSA_SHA1);
        digMethods(SHA384, SHA384, SHA1, SHA1);
        signers(alice, alice, bob, bob);
    }

    void expectResult(AssertionStatus expectedResult, X509Certificate... expectedCredentials) throws Exception {
        ServerNonSoapCheckVerifyResultsAssertion sass = new ServerNonSoapCheckVerifyResultsAssertion(ass, null, null);
        AssertionStatus result = sass.checkRequest(pec);
        assertEquals(expectedResult, result);

        List<LoginCredentials> creds = pec.getAuthenticationContext(pec.getRequest()).getCredentials();
        TreeSet<X509Certificate> certs = ServerNonSoapCheckVerifyResultsAssertion.makeCertCollector();
        for (LoginCredentials cred : creds) {
            Object paylod = cred.getPayload();
            if (paylod instanceof X509Certificate) {
                certs.add((X509Certificate) paylod);
            }
        }
        TreeSet<X509Certificate> dontWantCerts = ServerNonSoapCheckVerifyResultsAssertion.makeCertCollector();
        dontWantCerts.addAll(knownCerts);
        for (X509Certificate wantCert : expectedCredentials) {
            assertTrue("Expected certificate wasn't gathered", certs.contains(wantCert));
            dontWantCerts.remove(wantCert);
        }
        dontWantCerts.retainAll(certs);
        assertTrue("Unexpected certificate was gathered", dontWantCerts.isEmpty());
    }

    @Test
    public void testSingleSignatureOfSingleElement() throws Exception {
        nomulti();
        expectResult(AssertionStatus.NONE, alice);
    }

    @Test
    public void testSingleSignatureOfOtherSingleElement() throws Exception {
        nomulti();
        select(dog);
        expectResult(AssertionStatus.NONE, bob);
    }

    @Test
    public void testNullSigner() throws Exception {
        signers(new X509Certificate[]{null});       
        expectResult(AssertionStatus.SERVER_ERROR);
    }

    @Test
    public void testNotEnoughSigners() throws Exception {
        select(dog);
        signers(bob);
        expectResult(AssertionStatus.SERVER_ERROR);
    }

    @Test
    public void testNotEnoughDigestMethods() throws Exception {
        select(dog);
        digMethods(SHA1);
        expectResult(AssertionStatus.SERVER_ERROR);
    }

    @Test
    public void testNotEnoughSignatureMethods() throws Exception {
        select(dog);
        sigMethods(RSA_SHA1);
        expectResult(AssertionStatus.SERVER_ERROR);
    }

    @Test
    public void testJustEnoughSigners() throws Exception {
        signers(bob);
        expectResult(AssertionStatus.NONE, bob);
    }

    @Test
    public void testJustEnoughDigestMethods() throws Exception {
        digMethods(SHA1);
        expectResult(AssertionStatus.NONE, alice);
    }

    @Test
    public void testJustEnoughSignatureMethods() throws Exception {
        sigMethods(RSA_SHA1);
        expectResult(AssertionStatus.NONE, alice);
    }

    @Test
    public void testElementsHaveNoCommonSigningCert_disallowMulti() throws Exception {
        nomulti();
        select(cat, dog);
        expectResult(AssertionStatus.FALSIFIED);
    }

    @Test
    public void testSingleSignatureOfMultipleElements_disallowMulti() throws Exception {
        elementsVerified(cat, snake, dog);
        signers(alice, alice, alice);
        allSigMethods(ECDSA_SHA384);
        allDigMethods(SHA384);

        select(cat, snake);
        nomulti();
        expectResult(AssertionStatus.NONE, alice);
    }

    @Test
    public void testMultipleSignaturesOfSingleElement_disallowMulti() throws Exception {
        elementsVerified(cat, cat);
        signers(alice, bob);
        allSigMethods(ECDSA_SHA384);
        allDigMethods(SHA384);

        select(cat);
        nomulti();
        expectResult(AssertionStatus.FALSIFIED);
    }

    @Test
    public void testMultipleSignaturesOfMultipleElements_disallowMulti() throws Exception {
        elementsVerified(cat, snake, dog, snake);
        signers(alice, alice, bob, bob);
        allSigMethods(ECDSA_SHA384);
        allDigMethods(SHA384);

        select(cat, dog, snake);
        nomulti();
        expectResult(AssertionStatus.FALSIFIED);
    }

    @Test
    public void testElementsHaveNoCommonSigningCert() throws Exception {
        select(cat, dog);
        multi();
        expectResult(AssertionStatus.FALSIFIED);
    }

    @Test
    public void testSingleSignatureOfMultipleElements() throws Exception {
        elementsVerified(cat, snake, dog);
        signers(alice, alice, alice);
        allSigMethods(ECDSA_SHA384);
        allDigMethods(SHA384);

        select(cat, snake);
        multi();
        expectResult(AssertionStatus.NONE, alice);
    }

    @Test
    public void testMultipleSignaturesOfSingleElement() throws Exception {
        elementsVerified(cat, cat);
        signers(alice, bob);
        allSigMethods(ECDSA_SHA384);
        allDigMethods(SHA384);

        select(cat);
        multi();
        expectResult(AssertionStatus.NONE, alice, bob);        
    }

    @Test
    public void testMultipleSignaturesOfMultipleElements() throws Exception {
        elementsVerified(cat, snake, dog, snake);
        signers(alice, alice, bob, bob);
        allSigMethods(ECDSA_SHA384);
        allDigMethods(SHA384);

        select(cat, dog, snake);
        multi();
        expectResult(AssertionStatus.FALSIFIED); // No signer in common to all 3
    }

    @Test
    public void testDisallowedDigestMethod() throws Exception {
        elementsVerified(cat, snake);
        signers(alice, alice);
        sigMethods(RSA_SHA1, RSA_SHA1);
        digMethods(SHA384, SHA384);

        allowDig(SHA1);

        select(cat, snake);
        nomulti();
        expectResult(AssertionStatus.FALSIFIED);
    }

    @Test
    public void testDisallowedSignatureMethod() throws Exception {
        elementsVerified(cat, snake);
        signers(alice, alice);
        sigMethods(RSA_SHA1, RSA_SHA1);
        digMethods(SHA384, SHA384);

        allowSig(ECDSA_SHA384);

        select(cat, snake);
        nomulti();
        expectResult(AssertionStatus.FALSIFIED);
    }

    @Test
    public void testXpathResultElementNotPresentInVerifyResults() throws Exception {
        elementsVerified(cat, dog);
        signers(alice, alice);
        allSigMethods(ECDSA_SHA384);
        allDigMethods(SHA384);

        select(snake);
        nomulti();
        expectResult(AssertionStatus.FALSIFIED);
    }

    @Test(expected = InvalidXpathException.class)
    public void testInvalidXpath() throws Exception {
        ass.setXpathExpression(new XpathExpression("/blj[[35[[]2452//53]"));
        expectResult(null);
    }

    @Test
    public void testXpathResultBoolean() throws Exception {
        ass.setXpathExpression(new XpathExpression("1=1"));
        expectResult(AssertionStatus.FAILED);
    }

    @Test
    public void testXpathResultAttribute() throws Exception {
        ass.setXpathExpression(new XpathExpression("/pets/cat/@color"));
        expectResult(AssertionStatus.FAILED);
    }

    @Test
    public void testXpathResultEmptyNodeSet() throws Exception {
        ass.setXpathExpression(new XpathExpression("/pets/nonexistent"));
        expectResult(AssertionStatus.FALSIFIED);
    }

    @Test
    public void testX509EncodedFormComparator() throws Exception {
        X509Certificate dataCert = NonSoapXmlSecurityTestUtils.getDataKey().getCertificate();
        X509Certificate testCert = alice;

        TreeSet<X509Certificate> collector = ServerNonSoapCheckVerifyResultsAssertion.makeCertCollector();
        assertEquals(0, collector.size());
        collector.add(dataCert);
        assertEquals(1, collector.size());
        collector.add(dataCert);
        assertEquals(1, collector.size());
        collector.add(testCert);
        assertEquals(2, collector.size());
        collector.add(dataCert);
        assertEquals(2, collector.size());
        collector.add(testCert);
        assertEquals(2, collector.size());
        collector.remove(dataCert);
        assertEquals(1, collector.size());
        assertTrue(collector.contains(testCert));
        assertFalse(collector.contains(dataCert));
        collector.remove(testCert);
        assertTrue(collector.isEmpty());
    }
}
