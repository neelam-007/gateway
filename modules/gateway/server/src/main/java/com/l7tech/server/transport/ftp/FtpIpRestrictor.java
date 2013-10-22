package com.l7tech.server.transport.ftp;

import java.net.InetAddress;

//import org.apache.ftpserver.interfaces.IpRestrictor;
import org.apache.ftpserver.ftplet.FtpException;

/**
 * Unrestrictive IpRestrictor implementation.
 *
 * @author Steve Jones
 */
public class FtpIpRestrictor {

    public boolean hasPermission(InetAddress inetAddress) throws FtpException {
        return true;
    }

    public Object[][] getPermissions() throws FtpException {
        return null;
    }

    public void setPermissions(Object[][] objects) throws FtpException {
    }
}
