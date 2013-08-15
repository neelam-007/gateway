package com.l7tech.server.identity.cert

import com.l7tech.server.security.cert.TestCertValidationProcessor
import com.l7tech.common.TestDocuments
import com.l7tech.identity.cert.CertEntryRow
import com.l7tech.policy.assertion.credential.LoginCredentials
import com.l7tech.policy.assertion.SslAssertion
import com.l7tech.gateway.common.audit.{Audit, TestAudit}
import com.l7tech.objectmodel.FindException
import com.l7tech.identity.{MissingCredentialsException, InvalidClientCertificateException, User, UserBean}
import com.l7tech.security.token.http.{HttpBasicToken, HttpClientCertToken}
import com.l7tech.policy.assertion.credential.http.HttpBasic
import com.l7tech.server.identity.AuthenticationResult
import com.l7tech.security.types.{CertificateValidationResult, CertificateValidationType}
import com.l7tech.xml.saml.SamlAssertionV1
import com.l7tech.policy.assertion.xmlsec.RequireWssSaml
import com.l7tech.common.io.XmlUtil
import com.l7tech.server.security.cert.CertValidationProcessor.Facility
import com.l7tech.objectmodel.Goid

import org.junit.{Assert, Test}

import java.security.cert.X509Certificate

/** Unit tests for CertificateAuthenticator
  */
class CertificateAuthenticatorTest {

  /** Test successful authentication with lookup of certificate for user
    */
  @Test
  def testAuthenticationWithCertLookup() {
    assertAliceResult(authenticate( LoginCredentials.makeLoginCredentials( new HttpClientCertToken(TestDocuments.getWssInteropAliceCert), classOf[SslAssertion]) ))
  }

  /** Test successful authentication from SAML with lookup of certificate for user
    */
  @Test
  def testAuthenticationFromSamlWithCertLookup() {
    assertAliceResult(authenticate( LoginCredentials.makeLoginCredentials( new SamlAssertionV1(XmlUtil.parse(samlToken).getDocumentElement, null), classOf[RequireWssSaml]) ))
  }

  /** Test successful authentication with specified (external) certificate for user
    */
  @Test
  def testAuthenticationWithExternalCert() {
    assertAliceResult(
      authenticateExternal(
        LoginCredentials.makeLoginCredentials( new HttpClientCertToken(TestDocuments.getWssInteropAliceCert), classOf[SslAssertion]),
        TestDocuments.getWssInteropAliceCert
      )
    )
  }

  /** Test failure due to user lookup error
    */
  @Test(expected = classOf[InvalidClientCertificateException])
  def testAuthenticationWithCertLookupFailure() {
    val certificateAuthenticator = new CertificateAuthenticator( new MockClientCertManager( certEntryRows ){
      override def getUserCert(user: User) = throw new FindException
    }, new TestCertValidationProcessor() )

    authenticate(
      LoginCredentials.makeLoginCredentials( new HttpClientCertToken(TestDocuments.getWssInteropAliceCert), classOf[SslAssertion]),
      certificateAuthenticator )
  }

  /** Test failure due to invalid credential format
    */
  @Test(expected = classOf[MissingCredentialsException])
  def testAuthenticationWithoutCert() {
    authenticate( LoginCredentials.makeLoginCredentials( new HttpBasicToken("Alice","password".toCharArray), classOf[HttpBasic]) )
  }

  /** Test failure due to wrong certificate for user
    */
  @Test(expected = classOf[InvalidClientCertificateException])
  def testAuthenticationWrongCert() {
    authenticate( LoginCredentials.makeLoginCredentials( new HttpClientCertToken(TestDocuments.getWssInteropBobCert), classOf[SslAssertion]) )
  }

  /** Test failure due to certificate validation failure
    */
  @Test(expected = classOf[InvalidClientCertificateException])
  def testAuthenticationInvalidCert() {
    val certificateAuthenticator = new CertificateAuthenticator( new MockClientCertManager( certEntryRows ), new TestCertValidationProcessor(){
      override def check(certificatePath: Array[X509Certificate], minimumValidationType: CertificateValidationType, requestedValidationType: CertificateValidationType, facility: Facility, auditor: Audit) = CertificateValidationResult.REVOKED
    } )
    authenticate( LoginCredentials.makeLoginCredentials( new HttpClientCertToken(TestDocuments.getWssInteropAliceCert), classOf[SslAssertion]), certificateAuthenticator )
  }

  private def authenticate( credentials: LoginCredentials,
                            certificateAuthenticator: CertificateAuthenticator = authenticator
                          ) : AuthenticationResult = {
    certificateAuthenticator.authenticateX509Credentials(
      credentials,
      user(new Goid(0,1),"00000000000000000000000000000001","Alice"),
      CertificateValidationType.CERTIFICATE_ONLY,
      new TestAudit() )
  }

  private def authenticateExternal( credentials: LoginCredentials,
                                    certificate: X509Certificate,
                                    certificateAuthenticator: CertificateAuthenticator = authenticator
                                  ) : AuthenticationResult = {
    certificateAuthenticator.authenticateX509Credentials(
      credentials,
      certificate,
      user(new Goid(0,1),"00000000000000000000000000000001","Alice"),
      CertificateValidationType.CERTIFICATE_ONLY,
      new TestAudit(),
      false )
  }

  private def authenticator = new CertificateAuthenticator( new MockClientCertManager( certEntryRows ), new TestCertValidationProcessor() )

  private def assertAliceResult(result: AuthenticationResult) {
    Assert.assertNotNull("Result", result);
    Assert.assertNotNull("Result cert", result.getAuthenticatedCert)
    Assert.assertNotNull("Result user", result.getUser)
    Assert.assertEquals("Result user provider", new Goid(0,1), result.getUser.getProviderId)
    Assert.assertEquals("Result user id", "00000000000000000000000000000001", result.getUser.getId)
  }

  private def user(  providerId: Goid, userId: String, login: String ) : User = {
    val user = new UserBean( providerId, login );
    user.setUniqueIdentifier( userId )
    user
  }

  private def entry( providerId: Goid, userId: String, login: String, certificate: X509Certificate ) : CertEntryRow = {
    val entry = new CertEntryRow();
    entry.setProvider( providerId )
    entry.setUserId( userId )
    entry.setLogin( login )
    entry.setCertificate( certificate )
    entry
  }

  private def certEntryRows : Seq[CertEntryRow] = {
    Seq(
      entry(new Goid(0,1),"00000000000000000000000000000001","Alice",TestDocuments.getWssInteropAliceCert),
      entry(new Goid(0,1),"00000000000000000000000000000002","Bob",TestDocuments.getWssInteropBobCert)
    )
  }

  private val samlToken = "<saml:Assertion AssertionID=\"SamlAssertion-2414d0358170b20c36ebe5a3328602f0\" IssueInstant=\"2005-08-17T23:56:20.609Z\" Issuer=\"data.l7tech.com\" MajorVersion=\"1\" MinorVersion=\"1\" xmlns:saml=\"urn:oasis:names:tc:SAML:1.0:assertion\"> <saml:Conditions NotBefore=\"2005-08-17T23:56:00.000Z\" NotOnOrAfter=\"2005-08-18T00:01:00.000Z\"/> <saml:AuthenticationStatement AuthenticationMethod=\"urn:ietf:rfc:3075\">  <saml:Subject> <saml:NameIdentifier Format=\"urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName\">CN=Alice</saml:NameIdentifier> <saml:SubjectConfirmation> <saml:ConfirmationMethod>urn:oasis:names:tc:SAML:1.0:cm:holder-of-key</saml:ConfirmationMethod> <xd:KeyInfo xmlns:xd=\"http://www.w3.org/2000/09/xmldsig#\"> <xd:X509Data> <xd:X509Certificate>MIIDDDCCAfSgAwIBAgIQM6YEf7FVYx/tZyEXgVComTANBgkqhkiG9w0BAQUFADAwMQ4wDAYDVQQKDAVPQVNJUzEeMBwGA1UEAwwVT0FTSVMgSW50ZXJvcCBUZXN0IENBMB4XDTA1MDMxOTAwMDAwMFoXDTE4MDMxOTIzNTk1OVowQjEOMAwGA1UECgwFT0FTSVMxIDAeBgNVBAsMF09BU0lTIEludGVyb3AgVGVzdCBDZXJ0MQ4wDAYDVQQDDAVBbGljZTCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAoqi99By1VYo0aHrkKCNT4DkIgPL/SgahbeKdGhrbu3K2XG7arfD9tqIBIKMfrX4Gp90NJa85AV1yiNsEyvq+mUnMpNcKnLXLOjkTmMCqDYbbkehJlXPnaWLzve+mW0pJdPxtf3rbD4PS/cBQIvtpjmrDAU8VsZKT8DN5Kyz+EZsCAwEAAaOBkzCBkDAJBgNVHRMEAjAAMDMGA1UdHwQsMCowKKImhiRodHRwOi8vaW50ZXJvcC5iYnRlc3QubmV0L2NybC9jYS5jcmwwDgYDVR0PAQH/BAQDAgSwMB0GA1UdDgQWBBQK4l0TUHZ1QV3V2QtlLNDm+PoxiDAfBgNVHSMEGDAWgBTAnSj8wes1oR3WqqqgHBpNwkkPDzANBgkqhkiG9w0BAQUFAAOCAQEABTqpOpvW+6yrLXyUlP2xJbEkohXHI5OWwKWleOb9hlkhWntUalfcFOJAgUyH30TTpHldzx1+vK2LPzhoUFKYHE1IyQvokBN2JjFO64BQukCKnZhldLRPxGhfkTdxQgdf5rCK/wh3xVsZCNTfuMNmlAM6lOAg8QduDah3WFZpEA0s2nwQaCNQTNMjJC8tav1CBr6+E5FAmwPXP7pJxn9Fw9OXRyqbRA4v2y7YpbGkG2GI9UvOHw6SGvf4FRSthMMO35YbpikGsLix3vAsXWWi4rwfVOYzQK0OFPNi9RMCUdSH06m9uLWckiCxjos0FQODZE9l4ATGy9s9hNVwryOJTw==</xd:X509Certificate> </xd:X509Data> </xd:KeyInfo> </saml:SubjectConfirmation>  </saml:Subject>  <saml:SubjectLocality DNSAddress=\"Data.l7tech.com\" IPAddress=\"192.168.1.154\"/> </saml:AuthenticationStatement></saml:Assertion>";
}