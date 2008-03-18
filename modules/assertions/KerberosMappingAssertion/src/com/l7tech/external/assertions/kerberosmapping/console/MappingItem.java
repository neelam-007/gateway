package com.l7tech.external.assertions.kerberosmapping.console;

/**
 * A mapping between a Kerberos REALM and a User Principal Name Suffix.
 *
 * <p>In Active Directory the realm would be <b>DOMAIN.COM</b> and a typical
 * User Principal Name suffix would be <b>domain.com</b>.</p>
 *
 * @author steve
 */
public class MappingItem {

    //- PUBLIC

    public MappingItem( final String realm,
                        final String upnSuffix ) {
        this.realm = realm;
        this.upnSuffix = upnSuffix;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm( final String realm ) {
        this.realm = realm;
    }

    public String getUpnSuffix() {
        return upnSuffix;
    }

    public void setUpnSuffix( final String upnSuffix ) {
        this.upnSuffix = upnSuffix;
    }

    //- PRIVATE

    private String realm;
    private String upnSuffix;
}
