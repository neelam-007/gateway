package com.l7tech.external.assertions.jsonjolt.server;

import com.bazaarvoice.jolt.Chainr;
import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.external.assertions.jsonjolt.JsonJoltAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.Charsets;
import com.l7tech.util.IOUtils;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.module.SimpleModule;
import org.codehaus.jackson.type.TypeReference;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;

/**
 * Test the JsonJoltAssertion.
 */
public class ServerJsonJoltAssertionTest {

    private static final Logger log = Logger.getLogger(ServerJsonJoltAssertionTest.class.getName());
    private static final Class clazz = ServerJsonJoltAssertionTest.class;

    @Test
    public void testUnderlyingLibrary() throws Exception {
        List spec = loadJsonList( "/com/l7tech/external/assertions/jsonjolt/sample_spec.json" );
        Chainr chainr = Chainr.fromSpec( spec );

        Object input = loadJsonObject( "/com/l7tech/external/assertions/jsonjolt/sample_input.json" );

        Object output = chainr.transform( input );
        System.out.println( jsonToString( output ) );
    }

    @Test
    public void testSimpleChainrTransformation() throws Exception {
        JsonJoltAssertion ass = new JsonJoltAssertion();
        ass.setTarget( TargetMessageType.REQUEST );
        ass.setSchemaExpression( new String( IOUtils.slurpStream(
                clazz.getResourceAsStream( "/com/l7tech/external/assertions/jsonjolt/sample_spec.json" ) ), Charsets.UTF8 ) );

        PolicyEnforcementContext pec = PolicyEnforcementContextFactory.createPolicyEnforcementContext( new Message(), new Message() );
        pec.getRequest().initialize( new ByteArrayStashManager(),
                ContentTypeHeader.APPLICATION_JSON,
                clazz.getResourceAsStream(  "/com/l7tech/external/assertions/jsonjolt/sample_input.json" ) );

        ServerJsonJoltAssertion sass = new ServerJsonJoltAssertion( ass );
        AssertionStatus result = sass.checkRequest( pec );
        assertEquals( AssertionStatus.NONE, result );

        String transformOutput = new String( IOUtils.slurpStream( pec.getRequest().getMimeKnob().getEntireMessageBodyAsInputStream( false ) ), Charsets.UTF8 );
        assertEquals( "{\"Rating\":3,\"SecondaryRatings\":{\"quality\":{\"Id\":\"quality\",\"Value\":3,\"Range\":5}},\"Range\":5}",
                transformOutput );
    }

    private String jsonToString( Object jsonObj ) throws IOException {
        return objectMapper().writeValueAsString( jsonObj );
    }

    public static List<Object> loadJsonList( String classPath ) throws IOException {
        InputStream inputStream = ServerJsonJoltAssertionTest.class.getResourceAsStream( classPath );
        return objectMapper().readValue( inputStream, new TypeReference<List<Object>>() {} );
    }

    public static Object loadJsonObject( String classPath ) throws IOException {
        InputStream inputStream = ServerJsonJoltAssertionTest.class.getResourceAsStream( classPath );
        return objectMapper().readValue( inputStream, Object.class );
    }

    public static ObjectMapper objectMapper() {

        ObjectMapper objectMapper = new ObjectMapper(  );

        // All Json maps should be deserialized into LinkedHashMaps.
        SimpleModule stockModule = new SimpleModule("stockJoltMapping", new Version(1, 0, 0, null ) )
                .addAbstractTypeMapping( Map.class, LinkedHashMap.class );

        objectMapper.registerModule(stockModule);

        // allow the mapper to parse JSON with comments in it
        objectMapper.configure( JsonParser.Feature.ALLOW_COMMENTS, true);

        return objectMapper;
    }
}
