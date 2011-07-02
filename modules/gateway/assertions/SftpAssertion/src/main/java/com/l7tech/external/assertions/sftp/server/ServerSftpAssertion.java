package com.l7tech.external.assertions.sftp.server;

import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.sftp.SftpAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.RoutingStatus;
import com.l7tech.policy.variable.NoSuchVariableException;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.util.ExceptionUtils;
import org.springframework.beans.factory.BeanFactory;

import javax.net.SocketFactory;
import java.io.IOException;
import java.util.Map;

/**
 * TODO Server side implementation of the SftpAssertion.
 *
 * @see com.l7tech.external.assertions.sftp.SftpAssertion
 */
public class ServerSftpAssertion extends AbstractServerAssertion<SftpAssertion> {
    private final StashManagerFactory stashManagerFactory;
    private final String[] referencedVariables;
    SocketFactory socketFactory = SocketFactory.getDefault();

    public ServerSftpAssertion(SftpAssertion assertion, BeanFactory beanFactory)
            throws PolicyAssertionException, IOException
    {
        super(assertion);
        this.stashManagerFactory = beanFactory == null ? new ByteArrayStashManagerFactory() : beanFactory.getBean("stashManagerFactory", StashManagerFactory.class);
        this.referencedVariables = assertion.getVariablesUsed();
    }

    @Override
    public AssertionStatus checkRequest(PolicyEnforcementContext context) throws IOException, PolicyAssertionException {

        try {
            Message request = assertion.getRequestTarget() == null ? null : context.getTargetMessage(assertion.getRequestTarget(), true);
            Message response = assertion.getResponseTarget() == null ? null : context.getOrCreateTargetMessage(assertion.getResponseTarget(), false);

            Map<String,?> vars = context.getVariableMap(referencedVariables, getAudit());

            context.setRoutingStatus(RoutingStatus.ATTEMPTED);
            transmitOverSftp(context, request, response, vars);
            context.setRoutingStatus(RoutingStatus.ROUTED);

            return AssertionStatus.NONE;

        } catch (NoSuchVariableException e) {
            logAndAudit( AssertionMessages.NO_SUCH_VARIABLE, e.getVariable() );
            return AssertionStatus.SERVER_ERROR;
        } catch (IOException e) {
            //noinspection ThrowableResultOfMethodCallIgnored
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{ "SFTP route failed: " + ExceptionUtils.getMessage( e ) }, ExceptionUtils.getDebugException( e ) );
            return AssertionStatus.FAILED;
        } catch (NoSuchPartException e) {
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{ "SFTP route failed: " + ExceptionUtils.getMessage( e ) }, e );
            return AssertionStatus.FAILED;
        } catch (VariableNameSyntaxException e) {
            //noinspection ThrowableResultOfMethodCallIgnored
            logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, new String[]{ "SFTP route failed: " + ExceptionUtils.getMessage( e ) }, ExceptionUtils.getDebugException( e ) );
            return AssertionStatus.SERVER_ERROR;
        }
    }

    private void transmitOverSftp(PolicyEnforcementContext context, Message request, Message response, Map<String, ?> vars) throws IOException, NoSuchPartException {
        // TODO
    }
}
