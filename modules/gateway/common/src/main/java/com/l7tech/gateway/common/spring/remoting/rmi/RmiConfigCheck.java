package com.l7tech.gateway.common.spring.remoting.rmi;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.Collections;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import org.springframework.beans.factory.InitializingBean;
import com.l7tech.util.SyspropUtil;

/**
 * Bean that checks RMI configuration.
 *
 * <p>If any issues are found, they are logged as warnings.</p>
 *
 * @author Steve Jones
 */
public class RmiConfigCheck implements InitializingBean {

    //- PUBLIC

    /**
     * Bean constructor
     */
    public RmiConfigCheck() {
    }

    public void afterPropertiesSet() {
        checkConfig();
    }

    //- PRIVATE

    private static final String SYSPROP_RMI_HOSTNAME = "java.rmi.server.hostname";

    private static final Logger logger = Logger.getLogger(RmiConfigCheck.class.getName());

    private void checkConfig() {
        // If the RMI hostname is set ensure it resolves to a local address
        String rmiHostName = SyspropUtil.getProperty(SYSPROP_RMI_HOSTNAME);
        if (rmiHostName == null) {
            logger.log(Level.CONFIG, "RMI host name is not set (java.rmi.server.hostname)");
        } else {
            if (logger.isLoggable(Level.CONFIG))
                logger.config( "RMI host name is '"+rmiHostName+"'.");

            InetAddress hostAddress = null;
            try {
                hostAddress = InetAddress.getByName(rmiHostName);
            }
            catch(UnknownHostException uhe) {
                logger.log(Level.WARNING, "Cannot resolve RMI host name '"+rmiHostName+"' (java.rmi.server.hostname)", uhe);
            }

            if (hostAddress != null) {
                try {
                    boolean found = false;
                    done:
                    for(NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                        for(InetAddress inetAddress : Collections.list(networkInterface.getInetAddresses())) {
                            if (inetAddress.equals(hostAddress)) {
                                found = true;
                                break done;
                            }
                        }
                    }
                    if (!found) {
                        logger.log(Level.WARNING, "RMI host name does not resolve to any local address '"+rmiHostName+"'.");
                    }
                } catch(SocketException se) {
                    logger.log(Level.WARNING, "Cannot get network interfaces to check RMI host name.", se);
                }
            }
        }
    }
}
