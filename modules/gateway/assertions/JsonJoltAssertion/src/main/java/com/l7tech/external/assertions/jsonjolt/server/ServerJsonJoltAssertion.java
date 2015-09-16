package com.l7tech.external.assertions.jsonjolt.server;

import com.bazaarvoice.jolt.Chainr;
import com.l7tech.common.io.NonCloseableOutputStream;
import com.l7tech.common.mime.NoSuchPartException;
import com.l7tech.external.assertions.jsonjolt.JsonJoltAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.server.message.AuthenticationContext;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.policy.assertion.AbstractMessageTargetableServerAssertion;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.util.PoolByteArrayOutputStream;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.module.SimpleModule;
import org.codehaus.jackson.type.TypeReference;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Server side implementation of the JsonJoltAssertion.
 *
 * @see com.l7tech.external.assertions.jsonjolt.JsonJoltAssertion
 */
public class ServerJsonJoltAssertion extends AbstractMessageTargetableServerAssertion<JsonJoltAssertion> {
    private final String[] variablesUsed;

    private final ObjectMapper objectMapper;

    public ServerJsonJoltAssertion( final JsonJoltAssertion assertion ) throws PolicyAssertionException {
        super( assertion );

        this.variablesUsed = assertion.getVariablesUsed();

        objectMapper = new ObjectMapper();
        // Preserve order when deserializing JSON maps
        SimpleModule stockModule = new SimpleModule( "stockJoltMapping", new Version( 1, 0, 0, null ) )
                .addAbstractTypeMapping( Map.class, LinkedHashMap.class );
        objectMapper.registerModule( stockModule );
        objectMapper.configure( JsonParser.Feature.ALLOW_COMMENTS, true );
    }

    @Override
    protected AssertionStatus doCheckRequest( PolicyEnforcementContext context, Message message, String messageDescription, AuthenticationContext authContext ) throws IOException, PolicyAssertionException {

        try {
            Map<String, Object> variableMap = context.getVariableMap( variablesUsed, getAudit() );
            String specString = ExpandVariables.process( assertion.getSchemaExpression(), variableMap, getAudit() );
            Object spec = objectMapper.readValue( specString, new TypeReference<List<Object>>() {} );
            Chainr chainr = Chainr.fromSpec( spec );

            Object input = objectMapper.readValue( message.getMimeKnob().getFirstPart().getInputStream( false ), Object.class );
            Object output = chainr.transform( input );

            try ( PoolByteArrayOutputStream outStream = new PoolByteArrayOutputStream() ) {
                objectMapper.writeValue( new NonCloseableOutputStream( outStream ), output );
                message.getMimeKnob().getFirstPart().setBodyBytes( outStream.toByteArray() );
            }

            return AssertionStatus.NONE;

        } catch ( NoSuchPartException e ) {
            logAndAudit( AssertionMessages.NO_SUCH_PART, messageDescription, e.getWhatWasMissing() );
            return AssertionStatus.SERVER_ERROR;
        }
   }
}
