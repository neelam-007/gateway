package com.l7tech.manager.automator;

import com.l7tech.admin.AdminContext;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.util.CertUtils;
import com.l7tech.common.util.CausedIOException;
import com.l7tech.common.AsyncAdminMethods;
import com.l7tech.identity.IdentityAdmin;
import com.l7tech.identity.User;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.internal.InternalUser;

import java.io.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * Created by IntelliJ IDEA.
 * User: njordan
 * Date: 16-Apr-2008
 * Time: 4:45:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class TrustedKeyManager {
    private static final int DEFAULT_KEY_BITS = 1024;
    private static final int DEFAULT_EXPIRY_DAYS = 5 * 365;

    private TrustedCertAdmin trustedCertAdmin;
    private IdentityAdmin identityAdmin;

    /**
     * Creates a new instance of ClusterPropertyManager.
     *
     * @param adminContext
     */
    public TrustedKeyManager(AdminContext adminContext) {
        trustedCertAdmin = adminContext.getTrustedCertAdmin();
    }

    /**
     * Adds the provided certificate to the gateway's list of trusted certificates.
     * @param filename The name of the file containing the certificate to trust
     */
    public void addTrustedCertificate(String filename) {
        File certFile = new File(filename);
        if(!certFile.exists()) {
            System.out.println("The cert file (" + filename + ") does not exist");
        } else {
            try {
                InputStream is = new FileInputStream(certFile);
                Collection<? extends Certificate> certs = CertUtils.getFactory().generateCertificates(is);
                X509Certificate[] certChain = certs.toArray(new X509Certificate[0]);
                TrustedCert tc = new TrustedCert();
                tc.setCertificate(certChain[0]);
                tc.setName(certChain[0].getSubjectDN().toString().substring(3));
                tc.setSubjectDn(certChain[0].getSubjectDN().toString());
                tc.setTrustedForSsl(true);
                tc.setTrustedForSigningClientCerts(false);
                tc.setTrustedForSigningServerCerts(true);
                tc.setTrustedAsSamlIssuer(false);
                tc.setTrustedAsSamlAttestingEntity(false);
                tc.setVerifyHostname(true);
                tc.setTrustAnchor(true);
                tc.setRevocationCheckPolicyType(TrustedCert.PolicyUsageType.USE_DEFAULT);
                tc.setRevocationCheckPolicyOid(null);
                trustedCertAdmin.saveCert(tc);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Generates a new private key which will become the corresponding user's private key.
     * @param alias The login of the user that the key is for
     * @param dn The DN for the new private key
     */
    private void generateNewPrivateKey(String alias, String dn) {
        try {
            TrustedCertAdmin.KeystoreInfo keystoreInfo = null;
            for(TrustedCertAdmin.KeystoreInfo keystore : (List<TrustedCertAdmin.KeystoreInfo>)trustedCertAdmin.findAllKeystores(true)) {
                if(!keystore.readonly) {
                    keystoreInfo = keystore;
                    break;
                }
            }

            if(keystoreInfo == null) {
                System.err.println("!!!! Could not find keystore information.");
                return;
            }

            X509Certificate cert = null;
            AsyncAdminMethods.JobId jobId = trustedCertAdmin.generateKeyPair(keystoreInfo.id, alias, dn, DEFAULT_KEY_BITS, DEFAULT_EXPIRY_DAYS);
            for(int i = 0;i < 20;i++) {
                Thread.sleep(i * 100);
                String status = trustedCertAdmin.getJobStatus(jobId);
                if(status == null) {
                    System.err.println("!!!! Failed to check job status");
                    return;
                } else if(status.startsWith("inactive:")) {
                    AsyncAdminMethods.JobResult result = trustedCertAdmin.getJobResult(jobId);
                    if(result.throwableClassname != null) {
                        System.err.println("!!!! Job failed for generating a private key");
                        System.err.println("!!!! " + result.throwableClassname + ": " + result.throwableMessage);
                        return;
                    } else if(result.result == null) {
                        System.err.println("!!!! Failed to retrieve new private key");
                        return;
                    }

                    cert = (X509Certificate)result.result;
                    break;
                }
            }

            User user = identityAdmin.findUserByLogin(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID, alias);
            identityAdmin.revokeCert(user);
            user = identityAdmin.findUserByID(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID, user.getId());
            ((InternalUser)user).setCleartextPassword(new String("password"));
            identityAdmin.saveUser(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID, user, null);
            user = identityAdmin.findUserByID(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID, user.getId());
            identityAdmin.recordNewUserCert(user, cert);
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Imports the key from the provided file as a private key on the gateway.
     * @param filename The name of the file containing the key
     * @param alias The alias to use for the key
     * @param password The password for reading the file
     */
    private void importPrivateKey(String filename, String alias, String password) {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(new FileInputStream(filename), password.toCharArray());

            List aliases = Collections.list(keyStore.aliases());
            if (aliases.size() > 1 || aliases.isEmpty())
                throw new CausedIOException("PKCS12 has unsupported number of entries (must be 1)");

            String keyAlias = (String) aliases.get(0);
            if (!keyStore.isKeyEntry(keyAlias))
                throw new CausedIOException("PKCS12 entry '"+keyAlias+"' is not a key.");

            Certificate[] chain = keyStore.getCertificateChain(alias);
            if (chain==null || chain.length==0)
                throw new CausedIOException("PKCS12 entry '"+alias+"' missing does not contain a certificate chain.");

            List<X509Certificate> got = new ArrayList<X509Certificate>();
            for (Certificate cert : chain) {
                if (cert == null)
                    throw new IOException("PKCS12 entry '" + alias + "' contains a null certificate in its certificate chain.");
                if (!(cert instanceof X509Certificate))
                    throw new IOException("PKCS12 entry '" + alias + "' certificate chain contains a non-X.509 certificate.");
                got.add((X509Certificate)cert);
            }

            X509Certificate[] certificateChain = got.toArray(new X509Certificate[0]);
            PrivateKey privateKey = (PrivateKey)keyStore.getKey(keyAlias, password.toCharArray());

            byte[] pkcs8Bytes = privateKey.getEncoded();

            final String[] pemChain = new String[chain.length];
            for (int i = 0; i < certificateChain.length; i++) {
                X509Certificate cert = certificateChain[i];
                pemChain[i] = CertUtils.encodeAsPEM(cert);
            }

            TrustedCertAdmin.KeystoreInfo keystoreInfo = null;
            for(TrustedCertAdmin.KeystoreInfo keystore : (List<TrustedCertAdmin.KeystoreInfo>)trustedCertAdmin.findAllKeystores(true)) {
                if(!keystore.readonly) {
                    keystoreInfo = keystore;
                    break;
                }
            }

            if(keystoreInfo != null) {
                trustedCertAdmin.importKey(keystoreInfo.id, alias, pemChain, pkcs8Bytes);
            } else {
                System.err.println("!!!! Could not find keystore information.");
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Generates private keys for certain users, creates an account for the test machine and sets that user's
     * private key to the SSL key, and imports the specified private keys.
     * @param identityAdmin The identity admin reference to use
     */
    public void setupPrivateKeys(IdentityAdmin identityAdmin) {
        this.identityAdmin = identityAdmin;

        String[] aliases = Main.getProperties().getProperty("manager.automator.privateKeys.toCreate").split(",");
        for(String alias : aliases) {
            generateNewPrivateKey(alias, "CN=" + alias);
        }

        try {
            X509Certificate cert = trustedCertAdmin.getSSGSslCert();
            User user = identityAdmin.findUserByLogin(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID, Main.getProperties().getProperty("ssg.host"));
            identityAdmin.revokeCert(user);
            user = identityAdmin.findUserByID(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID, user.getId());
            ((InternalUser)user).setCleartextPassword("password");
            identityAdmin.saveUser(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID, user, null);
            user = identityAdmin.findUserByID(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID, user.getId());
            identityAdmin.recordNewUserCert(user, cert);
        } catch(Exception e) {
            e.printStackTrace();
        }

        Pattern pattern = Pattern.compile("manager\\.automator\\.privateKeys\\.toImport\\.([0-9]+)\\.file");
        for(Enumeration e = Main.getProperties().propertyNames();e.hasMoreElements();) {
            Object key = e.nextElement();
            if(!(key instanceof String)) {
                continue;
            }
            String propertyName = (String)key;
            Matcher matcher = pattern.matcher(propertyName);
            if(matcher.matches()) {
                String id = matcher.group(1);
                String filename = Main.getProperties().getProperty(propertyName);
                String alias = Main.getProperties().getProperty("manager.automator.privateKeys.toImport." + id + ".alias");
                String password = Main.getProperties().getProperty("manager.automator.privateKeys.toImport." + id + ".password");

                importPrivateKey(filename, alias, password);
            }
        }
    }
}
