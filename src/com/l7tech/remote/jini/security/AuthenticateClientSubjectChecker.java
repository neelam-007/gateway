package com.l7tech.remote.jini.security;

import com.sun.jini.discovery.ClientSubjectChecker;

import javax.security.auth.Subject;
import java.security.Principal;
import java.util.Iterator;
import java.util.logging.Logger;

/**
 * Implementation of {@link ClientSubjectChecker} that approves or
 * rejects client subjects based on whether or not they are authenticated
 * clients.
 * *
 * @author  <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version $Revision$, $Date$
 */
public class AuthenticateClientSubjectChecker
  implements ClientSubjectChecker {
    private final Logger logger = Logger.getLogger(AuthenticateClientSubjectChecker.class.getName());

    public void checkClientSubject(Subject subject) {
        if (subject == null) {
            logger.info("The subject is null");
        } else {
            Iterator i = subject.getPrincipals().iterator();
            if (i.hasNext()) {
                Principal p = (Principal)i.next();
                logger.info("The subject is "+p.getName());
            } else {
                logger.info("The principal set is empty.");
            }
        }
    }
}
