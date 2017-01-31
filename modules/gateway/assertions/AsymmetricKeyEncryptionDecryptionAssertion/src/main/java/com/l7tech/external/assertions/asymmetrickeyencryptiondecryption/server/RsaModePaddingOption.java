package com.l7tech.external.assertions.asymmetrickeyencryptiondecryption.server;

import javax.crypto.Cipher;

/**
 * Created with IntelliJ IDEA.
 * User: abjorge
 * Date: 21/06/13
 * Time: 10:57 AM
 * To change this template use File | Settings | File Templates.
 *
 * We now use the algorithm string to allow for extra padding options that might be supported by bc providers
 */
@Deprecated
public enum RsaModePaddingOption {
    NO_MODE_NO_PADDING ("None"),
    ECB_NO_PADDING ("ECB/No padding"),
    ECB_PKCS1_PADDING ("ECB/PKCS1 padding"),
    ECP_OAEP_WITH_SHA1_AND_MDG1_PADDING ("ECB/OAEP with SHA-1 and MGF-1 padding");

    private String displayName = "";
    private Cipher cipher = null;

    private RsaModePaddingOption(String _displayname) {
        displayName = _displayname;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
