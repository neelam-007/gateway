package com.l7tech.server.identity.cert

import com.l7tech.identity.User
import com.l7tech.identity.cert.{CertEntryRow, ClientCertManager}
import com.l7tech.identity.cert.ClientCertManager.CertInfo

import collection.JavaConversions

import javax.security.auth.x500.X500Principal
import java.security.cert.{X509Certificate, Certificate}
import java.math.BigInteger

/**
  */
object MockClientCertManager {
  private class CertInfoImpl( entry: CertEntryRow ) extends CertInfo {
    def getLogin = entry.getLogin
    def getProviderId = entry.getProvider
    def getUserId = entry.getUserId
  }
}
/** Mock client certificate manager that supports look up
  */
class MockClientCertManager( entries: Seq[CertEntryRow] ) extends ClientCertManager {
  def findAll() = JavaConversions.seqAsJavaList(entries.map(new MockClientCertManager.CertInfoImpl(_)))

  def findByIssuerAndSerial(issuer: X500Principal, serial: BigInteger) = null

  def findBySki(ski: String) = JavaConversions.seqAsJavaList(entries.filter(entry => ski.equals(entry.getSki)))

  def findByThumbprint(thumbprint: String) = JavaConversions.seqAsJavaList(entries.filter(entry => thumbprint.equals(entry.getThumbprintSha1)))

  def findBySubjectDn(subjectDn: X500Principal) = JavaConversions.seqAsJavaList(entries.filter(entry => subjectDn.getName.equals(entry.getSubjectDn)))

  def forbidCertReset(user: User) {}

  def getUserCert(user: User) = entries.find(entry => entry.getUserId.equals(user.getId) && entry.getProvider==user.getProviderId).map(_.getCertificate).orNull

  def isCertPossiblyStale(userCert: X509Certificate) = false

  def recordNewUserCert(user: User, cert: Certificate, oldCertWasStale: Boolean) {}

  def revokeUserCert(user: User) {}

  def revokeUserCertIfIssuerMatches(user: User, issuer: X500Principal) = false

  def userCanGenCert(user: User, requestCert: Certificate) = false
}