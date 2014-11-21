package com.l7tech.external.assertions.bufferdata.server;

import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.bufferdata.BufferDataAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.Charsets;
import com.l7tech.util.IOUtils;
import com.l7tech.util.Pair;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Map;

/**
 * Server side implementation of the BufferDataAssertion.
 *
 * @see com.l7tech.external.assertions.bufferdata.BufferDataAssertion
 */
public class ServerBufferDataAssertion extends AbstractServerAssertion<BufferDataAssertion> {
    private final String[] variablesUsed;

    public ServerBufferDataAssertion( final BufferDataAssertion assertion ) throws PolicyAssertionException {
        super(assertion);

        this.variablesUsed = assertion.getVariablesUsed();
    }

    public AssertionStatus checkRequest( final PolicyEnforcementContext context ) throws IOException, PolicyAssertionException {
        Map<String, Object> varMap = context.getVariableMap( variablesUsed, getAudit() );

        String bufferName = ExpandVariables.process( assertion.getBufferName(), varMap, getAudit() );
        Buffer buffer = MemoryBufferManager.getOrCreateBuffer( bufferName );

        Object rec = varMap.get( assertion.getNewDataVarName() );

        final byte[] bytes;
        if ( null == rec ) {
            bytes = new byte[0];
        } else if ( rec instanceof byte[] ) {
            bytes = (byte[]) rec;
        } else if ( rec instanceof CharSequence ) {
            CharSequence charSequence = (CharSequence) rec;
            bytes = charSequence.toString().getBytes( Charsets.UTF8 ); // TODO make charset configurable?
        } else if ( rec instanceof Message ) {
            Message message = (Message) rec;
            try {
                bytes = IOUtils.slurpStream( message.getMimeKnob().getEntireMessageBodyAsInputStream() );
            } catch ( NoSuchPartException e ) {
                getAudit().logAndAudit( AssertionMessages.NO_SUCH_PART, assertion.getNewDataVarName(), e.getWhatWasMissing() );
                return AssertionStatus.SERVER_ERROR;
            }
        } else {
            getAudit().logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Unsupported context variable type: " + rec.getClass() );
            return AssertionStatus.SERVER_ERROR;
        }

        // Sanity check
        if ( bytes.length > assertion.getMaxSizeBytes() ) {
            getAudit().logAndAudit( AssertionMessages.EXCEPTION_WARNING_WITH_MORE_INFO, "Data chunk size " + bytes.length + " exceeds entire buffer maximum size " + assertion.getMaxSizeBytes() );
            return AssertionStatus.SERVER_ERROR;
        }

        Pair<OrderedMemoryBuffer.BufferStatus, byte[]> result = buffer.appendAndMaybeExtract( bytes, assertion.getMaxSizeBytes(), assertion.getMaxAgeMillis() );

        Message extractedMessage = null;
        if ( result.right != null ) {
            extractedMessage = new Message( new ByteArrayStashManager(), ContentTypeHeader.OCTET_STREAM_DEFAULT, new ByteArrayInputStream( result.right ) );
        }

        context.setVariable( assertion.prefix( "extractedMessage" ), extractedMessage );
        context.setVariable( assertion.prefix( "newAge.millis" ), result.left.getAge() );
        context.setVariable( assertion.prefix( "newSize.bytes" ), result.left.getSize() );
        context.setVariable( assertion.prefix( "wasExtracted" ), result.left.isWasExtracted() );
        context.setVariable( assertion.prefix( "wasFull" ), result.left.isWasFull() );

        return AssertionStatus.NONE;
    }

}
