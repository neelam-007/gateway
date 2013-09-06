package com.l7tech.gateway.common.admin;

import com.l7tech.gateway.common.security.rbac.MethodStereotype;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.kerberos.KerberosException;
import com.l7tech.kerberos.KeyTabEntryInfo;
import com.l7tech.objectmodel.EntityType;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static com.l7tech.gateway.common.security.rbac.MethodStereotype.UNCHECKED_WIDE_OPEN;

/**
 * Remote interface to check Kerberos configuration.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
@Secured
@Administrative
@Transactional(propagation= Propagation.SUPPORTS)
public interface KerberosAdmin  {

    /**
     * Validate the kerberos service principal name.
     *
     * <p>This method will attempt a login on the Gateway to check that the
     * Kerberos configuration is good.</p>
     *
     * <p>The name is either from the Keytab of from the ticket cache.</p>
     *
     * <p>Note that if this method fails and there is no Keytab then it is
     * probably the case that Kerberos is not configured (so not really an
     * error)</p>
     *
     * @param principle
     * @return the SPN (e.g. http/gateway.qawin2003.com@QAWIN2003.COM)
     * @throws com.l7tech.kerberos.KerberosException if the log in fails.
     */
    @Secured(stereotype=UNCHECKED_WIDE_OPEN)
    public void validatePrincipal(String principle) throws KerberosException ;

    /**
     * Get the configured Keytab.
     *
     * <p>The Keytab contains all information available from the Gateway
     * Keytab file.</p>
     *
     * @return the Keytab or null if non is available.
     * @throws KerberosException if a Keytab is present but invalid.
     */
    @Secured(stereotype=UNCHECKED_WIDE_OPEN)
    public List<KeyTabEntryInfo> getKeyTabEntryInfos() throws KerberosException;

    /**
     * Get the Kerberos configuration for the SSG.
     *
     * <p>The returned map should contain:</p>
     *
     * <ul>
     *   <li>kdc - The Kerberos key distribution center hostname or IP address</li>
     *   <li>realm - The Kerberos REALM</li>
     * </ul>
     *
     * <p>If this information is not available the keys should not be in the Map.</p>
     *
     * @return The configuration map (could be empty but should not be NULL)
     */
    @Secured(stereotype=UNCHECKED_WIDE_OPEN)
    public Map<String,String> getConfiguration();

    /**
     * Install a new keytab file.
     *
     * @param data The data for the keytab file.
     * @throws KerberosException if an error occurred.
     */
    @Secured(types= EntityType.CLUSTER_PROPERTY,stereotype= MethodStereotype.SET_PROPERTY_BY_UNIQUE_ATTRIBUTE)
    @Transactional(propagation= Propagation.REQUIRED, rollbackFor=Throwable.class)
    public void installKeytab( byte[] data ) throws KerberosException;

    /**
     * Delete the keytab file.
     *
     * @throws KerberosException if an error occurred.
     */
    @Secured(types= EntityType.CLUSTER_PROPERTY,stereotype= MethodStereotype.SET_PROPERTY_BY_UNIQUE_ATTRIBUTE)
    @Transactional(propagation= Propagation.REQUIRED, rollbackFor=Throwable.class)
    public void deleteKeytab() throws KerberosException;

    /**
     * Set validate required when loading the keytab file
     * @param validate True for validate is required, false for validate is not required
     */
    @Secured(types= EntityType.CLUSTER_PROPERTY,stereotype= MethodStereotype.SET_PROPERTY_BY_UNIQUE_ATTRIBUTE)
    @Transactional(propagation= Propagation.REQUIRED, rollbackFor=Throwable.class)
    public void setKeytabValidate(boolean validate) throws KerberosException;

    /**
     * Retrieve validate required when loading the keytab file
     * @return True for validate is required, false for validate is not required.
     */
    @Secured(stereotype=UNCHECKED_WIDE_OPEN)
    public boolean getKeytabValidate() throws KerberosException;

    /**
     * Check to see if the referral is enabled
     * @return True when enable.
     */
    @Secured(stereotype=UNCHECKED_WIDE_OPEN)
    boolean isReferralEnabled();





}
