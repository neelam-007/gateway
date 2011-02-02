package com.l7tech.security.cert;

import com.l7tech.common.TestDocuments;
import com.l7tech.common.io.AliasNotFoundException;
import com.l7tech.common.io.CertUtils;
import com.l7tech.security.xml.SignerInfo;
import com.l7tech.util.FixedCallable;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class TestKeysLoader {
    /**
     * Load a number of private keys that are all in their own PKCS#12 file, named following a similar naming convention,
     * using the same password.
     *
     * @param prefix  pathname prefix, including any prefix for the filename portion, ie "foo/bar/baz/testkey_".  Required.
     * @param suffix  pathname suffix, typically just a file extension, ie ".p12".  Required.
     * @param password password for all PKCS#12 files.  Required.
     * @param aliases list of names of individual files to load.  Each pathname will be constructed as prefix + alias + suffix and loaded as a PKCS#12 keystore.
     * @return an array of SignerInfo in the same order as the aliases were given.  Never null.  May be empty only if an empty aliases array was provided.
     * @throws java.io.IOException if one of the files cannot be opened or read.
     * @throws java.security.GeneralSecurityException if there is an error reading a keystore file.
     * @throws com.l7tech.common.io.AliasNotFoundException if one of the keystores does not contain exactly one suitable private key entry.
     */
    public static SignerInfo[] loadPrivateKeys(String prefix, String suffix, char[] password, String... aliases) throws IOException, GeneralSecurityException, AliasNotFoundException {
        List<SignerInfo> ret = new ArrayList<SignerInfo>();
        for (String alias : aliases) {
            if (alias == null)
                continue;
            InputStream stream = TestDocuments.getInputStream(prefix + alias + suffix);
            KeyStore.PrivateKeyEntry pke = CertUtils.loadPrivateKey(new FixedCallable<InputStream>(stream), "PKCS12", password, null, password);
            SignerInfo si = new SignerInfo(pke.getPrivateKey(), (X509Certificate[]) pke.getCertificateChain());
            ret.add(si);
        }
        return ret.toArray(new SignerInfo[ret.size()]);
    }
}
