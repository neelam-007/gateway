/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.identity.fed;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * The SAML-related configuration for a {@link com.l7tech.server.identity.fed.FederatedIdentityProvider}.
 *
 * This gets serialized to XML for storage along with the {@link FederatedIdentityProviderConfig}.
 *
 * @author alex
 * @version $Revision$
 */
public class SamlConfig implements Serializable {
    private String nameQualifier;

    private boolean subjConfHolderOfKey = true;
    private boolean subjConfSenderVouches = false;

    private boolean nameIdX509SubjectName = true;
    private boolean nameIdEmail = false;
    private boolean nameIdWindowsDomain = false;
    private String nameIdWindowsDomainName;

    private List attributeStatementConfigs = Collections.EMPTY_LIST;

    public String toString() {
        StringBuffer sb = new StringBuffer("<SamlConfig ");
        sb.append("nameQualifier=\"").append(nameQualifier).append("\" ");
        sb.append("subjConfHolderOfKey=\"").append(subjConfHolderOfKey).append("\" ");
        sb.append("subjConfSenderVouches=\"").append(subjConfSenderVouches).append("\" ");
        sb.append("nameIdX509SubjectName=\"").append(nameIdX509SubjectName).append("\" ");
        sb.append("nameIdEmail=\"").append(nameIdEmail).append("\" ");
        sb.append("nameIdWindowsDomain=\"").append(nameIdWindowsDomain).append("\" ");
        sb.append("nameIdWindowsDomainName=\"").append(nameIdWindowsDomainName).append("\">\n");

        for ( Iterator i = attributeStatementConfigs.iterator(); i.hasNext(); ) {
            AttributeStatementConfig config = (AttributeStatementConfig) i.next();
            sb.append("  <AttributeStatementConfig ");
            sb.append("name=\"").append(config.name).append("\" ");
            sb.append("namespaceUri=\"").append(config.namespaceUri).append("\" ");
            sb.append("values=\"");
            for ( int j = 0; j < config.values.length; j++ ) {
                String value = config.values[j];
                sb.append("'").append(value).append("'");
                if ( j < config.values.length - 1 ) sb.append(", ");
            }
            sb.append("\"/>\n");
        }

        sb.append("</SamlConfig>");
        return sb.toString();
    }


    public String getNameQualifier() {
        return nameQualifier;
    }

    public void setNameQualifier( String nameQualifier ) {
        this.nameQualifier = nameQualifier;
    }

    public boolean isSubjConfHolderOfKey() {
        return subjConfHolderOfKey;
    }

    public void setSubjConfHolderOfKey( boolean subjConfHolderOfKey ) {
        this.subjConfHolderOfKey = subjConfHolderOfKey;
    }

    public boolean isSubjConfSenderVouches() {
        return subjConfSenderVouches;
    }

    public void setSubjConfSenderVouches( boolean subjConfSenderVouches ) {
        this.subjConfSenderVouches = subjConfSenderVouches;
    }

    public boolean isNameIdX509SubjectName() {
        return nameIdX509SubjectName;
    }

    public void setNameIdX509SubjectName( boolean nameIdX509SubjectName ) {
        this.nameIdX509SubjectName = nameIdX509SubjectName;
    }

    public boolean isNameIdEmail() {
        return nameIdEmail;
    }

    public void setNameIdEmail( boolean nameIdEmail ) {
        this.nameIdEmail = nameIdEmail;
    }

    public boolean isNameIdWindowsDomain() {
        return nameIdWindowsDomain;
    }

    public void setNameIdWindowsDomain( boolean nameIdWindowsDomain ) {
        this.nameIdWindowsDomain = nameIdWindowsDomain;
    }

    public String getNameIdWindowsDomainName() {
        return nameIdWindowsDomainName;
    }

    public void setNameIdWindowsDomainName( String nameIdWindowsDomainName ) {
        this.nameIdWindowsDomainName = nameIdWindowsDomainName;
    }

    public List getAttributeStatementConfigs() {
        return attributeStatementConfigs;
    }

    public void setAttributeStatementConfigs( List attributeStatementConfigs ) {
        this.attributeStatementConfigs = attributeStatementConfigs;
    }

    public class AttributeStatementConfig {
        public String getName() {
            return name;
        }

        public void setName( String name ) {
            this.name = name;
        }

        public String getNamespaceUri() {
            return namespaceUri;
        }

        public void setNamespaceUri( String namespaceUri ) {
            this.namespaceUri = namespaceUri;
        }

        public String[] getValues() {
            return values;
        }

        public void setValues( String[] values ) {
            this.values = values;
        }

        private String name;
        private String namespaceUri;
        private String[] values;
    }
}
