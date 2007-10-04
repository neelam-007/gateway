package com.l7tech.common.security.kerberos;

import java.util.Map;

/**
 * Remote interface to check Kerberos configuration.
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public interface KerberosAdmin  {

    /**
     * Get the kerberos service principal name.
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
     * @return the SPN (e.g. http/gateway.qawin2003.com@QAWIN2003.COM)
     * @throws KerberosException if the log in fails.
     */
    public String getPrincipal() throws KerberosException;

    /**
     * Get the configured Keytab.
     *
     * <p>The Keytab contains all information available from the Gateway
     * Keytab file.</p>
     *
     * @return the Keytab or null if non is available.
     * @throws KerberosException if a Keytab is present but invalid.
     */
    public Keytab getKeytab() throws KerberosException;

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
    public Map getConfiguration();
}
