package com.l7tech.logging;

import com.l7tech.common.util.UptimeMetrics;
import java.rmi.RemoteException;
import com.l7tech.common.util.UptimeMetrics;

import java.io.IOException;
import java.rmi.RemoteException;

/*
 * Test stub for Log Admin interface
 *
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

public class LogAdminStub implements LogAdmin {

    public String[] getSystemLog(int offset, int size) throws RemoteException {
        String[] logs = {};

        // return nothing as this function is not used by Log Panel
        return logs;
    }

    public String[] getSystemLog(long startMsgNumber, long endMsgNumber, int size) throws RemoteException {

        String[] logs = {
            "13|20031023 10:55:40.921|FINEEST|com.l7tech.server.AuthenticatableHttpServlet|authenticateRequestBasic|Authentication success for user pang on identity provider: Internal Identity Provider",
            "12|20031023 10:55:40.828|SEVERE|com.l7tech.identity.ldap.AbstractLdapUserManagerServer|findByLogin|nothing has cn= pang",
            "11|20031023 10:55:40.781|SEVERE|com.l7tech.identity.ldap.AbstractLdapUserManagerServer|findByLogin|nothing has cn= pang",
            "10|20031023 10:55:40.546|FINE|com.l7tech.server.policy.assertion.credential.http.ServerHttpBasic|findCredentials|Found HTTP Basic credentials for user pang",
            "9|20031023 10:55:40.531|WARNING|com.l7tech.server.AuthenticatableHttpServlet|policyAllowAnonymous|Policy does not allow anonymous requests.",
            "8|20031023 10:55:28.812|WARNING|com.l7tech.server.AuthenticatableHttpServlet|policyAllowAnonymous|Policy does not allow anonymous requests.",
            "7|20031023 10:55:28.328|INFO|com.l7tech.server.policy.assertion.ServerSslAssertion|checkRequest|SSL required but not present",
            "6|20031023 10:55:27.718|FINER|com.l7tech.server.MessageProcessor|processMessage|Resolved service #7929856",
            "5|20031023 10:55:18.171|INFO|com.l7tech.server.util.UptimeMonitor|runMonitorThread|Uptime monitor thread is starting",
            "4|20031023 10:55:18.140|INFO|com.l7tech.server.util.UptimeMonitor|findUptime|Using uptime executable: c:/cygwin/bin/uptime",
            "3|20031023 10:52:50.187|INFO|com.l7tech.server.BootServlet|init|Layer 7 SecureSpan Suite HEAD build 1103, built 20031023104142 by fpang at data.l7tech.com",
            "2|20031023 10:52:44.093|FINE|com.l7tech.server.ServerConfig|getProperty|Checking System property com.l7tech.server.keystorePropertiesPath",
            "1|20031023 10:52:44.062|FINE|com.l7tech.server.ServerConfig|getProperty|Checking JNDI property java:comp/env/ServiceResolvers",
            "0|20031023 10:52:43.890|FINE|com.l7tech.server.ServerConfig|getProperty|Checking System property com.l7tech.server.serviceResolvers"
        };

        String[] result = {};

        System.out.println("startMsgNumber is: " + startMsgNumber);
         System.out.println("endMsgNumber is: " + endMsgNumber);
        // The following algorithm works only the messages in the logs array are numbered in consecutive descending order.
        if(startMsgNumber == -1 && endMsgNumber == -1)
        {
            if(size >= logs.length){
              result = new String[logs.length];
            }
            else{
              result = new String[size];
            }
            for(int i=0; i < result.length; i++){
                    result[i] = logs[i];
            }
        }
        else if (startMsgNumber >= 0 && endMsgNumber == -1) {
            if (startMsgNumber < logs.length) {
                if (size >= startMsgNumber) {
                    result = new String[(int)startMsgNumber];
                } else {
                    result = new String[size];
                }
                for (int i = 0; i < result.length; i++) {
                    result[i] = logs[logs.length - (int) startMsgNumber + i];
                }
            }
        }
        else{
            // other cases are not supported
        }

        return result;
    }

    public String[] getSystemLog(String nodeid, long startMsgNumber, long endMsgNumber, int size) throws RemoteException {
        return getSystemLog(startMsgNumber, endMsgNumber, size);
    }

    public UptimeMetrics getUptime() throws RemoteException {
        long time = System.currentTimeMillis() - (25*60*60000 + 60000); // server uptime is 1 day 1 hour 1 minute

        // Statistics panel shows the server up time as indicated above and the avg load (1 minute): 1.43
        // the " 11:22:20  up 28 days, 18:57," is the hardware uptime which is not shown in Statistics panel
        UptimeMetrics um = new UptimeMetrics(" 11:22:20  up 28 days, 18:57,  1 user,  load average: 1.43, 8.33, 0.12\n", time);
        return um;
    }
}