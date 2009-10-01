package com.l7tech.server.upgrade;

import org.springframework.context.ApplicationContext;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.orm.hibernate3.HibernateCallback;
import org.hibernate.SessionFactory;
import org.hibernate.Session;
import org.hibernate.HibernateException;
import org.hibernate.Criteria;
import com.l7tech.identity.cert.CertEntryRow;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.common.io.CertUtils;

import java.sql.SQLException;
import java.security.cert.X509Certificate;

/**
 * Upgrade task to convert DNs to canonical form.
 */
public class Upgrade51To52CanonicalizeDNs implements UpgradeTask {

    @Override
    public void upgrade( final ApplicationContext applicationContext ) throws NonfatalUpgradeException, FatalUpgradeException {
        SessionFactory sessionFactory = (SessionFactory)applicationContext.getBean("sessionFactory");
        new HibernateTemplate(sessionFactory).execute( new HibernateCallback(){
            @Override
            public Object doInHibernate( final Session session ) throws HibernateException, SQLException {
                // Client certificate DNs
                Criteria clientCertCriteria = session.createCriteria( CertEntryRow.class );
                for ( Object certEntryRowObj : clientCertCriteria.list() ) {
                    if ( certEntryRowObj instanceof CertEntryRow ) {
                        CertEntryRow certEntryRow = (CertEntryRow) certEntryRowObj;
                        X509Certificate cert = certEntryRow.getCertificate();
                        certEntryRow.setSubjectDn( CertUtils.getSubjectDN(cert) );
                        certEntryRow.setIssuerDn( CertUtils.getIssuerDN(cert) );
                    }
                }

                // Trusted certificate DNs
                Criteria trustedCertCriteria = session.createCriteria( TrustedCert.class );
                for ( Object trustedCertObj : trustedCertCriteria.list() ) {
                    if ( trustedCertObj instanceof TrustedCert ) {
                        TrustedCert trustedCert = (TrustedCert) trustedCertObj;
                        X509Certificate cert = trustedCert.getCertificate();
                        trustedCert.setSubjectDn( CertUtils.getSubjectDN(cert) );
                        trustedCert.setIssuerDn( CertUtils.getIssuerDN(cert) );
                    }
                }

                return null;
            }
        } );
    }

}
