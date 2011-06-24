package com.l7tech.security.xml;

import com.l7tech.util.DomUtils;
import com.l7tech.util.NamespaceFactory;
import com.l7tech.util.SyspropUtil;
import com.l7tech.xml.soap.SoapUtil;
import org.w3c.dom.Element;

import javax.security.auth.x500.X500Principal;
import java.security.cert.X509Certificate;

/**
 * Represents a KeyInfo element using KeyInfo/[SecurityTokenReference/]KeyName.
 */
public class KeyNameKeyInfoDetails extends KeyInfoDetails {

    //- PUBLIC

    public KeyNameKeyInfoDetails( final X509Certificate certificate,
                                  final boolean includeStr ) {
        this.certificate = certificate;
        this.includeStr = includeStr;
    }

    @Override
    public Element populateExistingKeyInfoElement( final NamespaceFactory nsf,
                                                   final Element keyInfo ) {
        final Element x509DataParent;
        if ( includeStr ) {
            x509DataParent = createStr(nsf, keyInfo);
        } else {
            x509DataParent = keyInfo;
        }
        final Element keyNameElement = DomUtils.createAndAppendElementNS( x509DataParent, "KeyName", SoapUtil.DIGSIG_URI, "ds" );
        keyNameElement.setTextContent( rfc2253KeyName ?
                certificate.getSubjectX500Principal().getName( X500Principal.RFC2253 ) :
                certificate.getSubjectDN().getName() );
        return keyInfo;    }

    @Override
    public boolean isX509ValueReference() {
        return true;
    }

    //- PRIVATE

    private static final boolean rfc2253KeyName = SyspropUtil.getBoolean( "com.l7tech.security.xml.rfc2253KeyName", true );

    private final X509Certificate certificate;
    private final boolean includeStr;

}
