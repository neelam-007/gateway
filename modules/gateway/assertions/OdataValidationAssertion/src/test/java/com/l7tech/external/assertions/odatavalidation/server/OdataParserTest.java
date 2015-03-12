package com.l7tech.external.assertions.odatavalidation.server;

import com.l7tech.test.BugId;
import org.apache.olingo.odata2.api.edm.Edm;
import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.apache.olingo.odata2.api.ep.EntityProviderException;
import org.apache.olingo.odata2.api.uri.UriNotMatchingException;
import org.apache.olingo.odata2.api.uri.UriSyntaxException;
import org.apache.olingo.odata2.api.uri.expression.CommonExpression;
import org.junit.Test;

import java.io.*;
import java.util.*;

import static org.junit.Assert.*;

/**
 * @author Jamie Williams - jamie.williams2@ca.com
 */
public class OdataParserTest {
    private static final String METADATA_DOCUMENT_ODATA_V2 =
            "<?xml version=\"1.0\" encoding=\"utf-8\" standalone=\"yes\"?>\n" +
                "<edmx:Edmx Version=\"1.0\" xmlns:edmx=\"http://schemas.microsoft.com/ado/2007/06/edmx\">\n" +
                "  <edmx:DataServices \n" +
                "xmlns:m=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\" \n" +
                "m:DataServiceVersion=\"2.0\">\n" +
                "    <Schema Namespace=\"ODataDemo\" \n" +
                "xmlns:d=\"http://schemas.microsoft.com/ado/2007/08/dataservices\" \n" +
                "xmlns:m=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\" \n" +
                "xmlns=\"http://schemas.microsoft.com/ado/2007/05/edm\">\n" +
                "      <EntityType Name=\"Product\">\n" +
                "        <Key>\n" +
                "          <PropertyRef Name=\"ID\" />\n" +
                "        </Key>\n" +
                "        <Property Name=\"ID\" Type=\"Edm.Int32\" Nullable=\"false\" />\n" +
                "        <Property Name=\"Name\" Type=\"Edm.String\" Nullable=\"true\" \n" +
                "m:FC_TargetPath=\"SyndicationTitle\" m:FC_ContentKind=\"text\" m:FC_KeepInContent=\"false\" />\n" +
                "        <Property Name=\"Description\" Type=\"Edm.String\" Nullable=\"true\" \n" +
                "m:FC_TargetPath=\"SyndicationSummary\" m:FC_ContentKind=\"text\" m:FC_KeepInContent=\"false\" />\n" +
                "        <Property Name=\"ReleaseDate\" Type=\"Edm.DateTime\" Nullable=\"false\" />\n" +
                "        <Property Name=\"DiscontinuedDate\" Type=\"Edm.DateTime\" Nullable=\"true\" />\n" +
                "        <Property Name=\"Rating\" Type=\"Edm.Int32\" Nullable=\"false\" />\n" +
                "        <Property Name=\"Price\" Type=\"Edm.Decimal\" Nullable=\"false\" />\n" +
                "        <NavigationProperty Name=\"Category\" \n" +
                "Relationship=\"ODataDemo.Product_Category_Category_Products\" FromRole=\"Product_Category\" \n" +
                "ToRole=\"Category_Products\" />\n" +
                "        <NavigationProperty Name=\"ProductImage\" \n" +
                "Relationship=\"ODataDemo.Product_ProductImage_ProductImage_Products\" FromRole=\"Product_ProductImage\" \n" +
                "ToRole=\"ProductImage_Products\" />\n" +
                "        <NavigationProperty Name=\"Supplier\" \n" +
                "Relationship=\"ODataDemo.Product_Supplier_Supplier_Products\" FromRole=\"Product_Supplier\" \n" +
                "ToRole=\"Supplier_Products\" />\n" +
                "      </EntityType>\n" +
                "      <EntityType Name=\"Category\">\n" +
                "        <Key>\n" +
                "          <PropertyRef Name=\"ID\" />\n" +
                "        </Key>\n" +
                "        <Property Name=\"ID\" Type=\"Edm.Int32\" Nullable=\"false\" />\n" +
                "        <Property Name=\"Name\" Type=\"Edm.String\" Nullable=\"true\" \n" +
                "m:FC_TargetPath=\"SyndicationTitle\" m:FC_ContentKind=\"text\" m:FC_KeepInContent=\"true\" />\n" +
                "        <NavigationProperty Name=\"Products\" \n" +
                "Relationship=\"ODataDemo.Product_Category_Category_Products\" FromRole=\"Category_Products\" \n" +
                "ToRole=\"Product_Category\" />\n" +
                "      </EntityType>\n" +
                "      <EntityType Name=\"Supplier\">\n" +
                "        <Key>\n" +
                "          <PropertyRef Name=\"ID\" />\n" +
                "        </Key>\n" +
                "        <Property Name=\"ID\" Type=\"Edm.Int32\" Nullable=\"false\" />\n" +
                "        <Property Name=\"Name\" Type=\"Edm.String\" Nullable=\"true\" \n" +
                "m:FC_TargetPath=\"SyndicationTitle\" m:FC_ContentKind=\"text\" m:FC_KeepInContent=\"true\" />\n" +
                "        <Property Name=\"Address\" Type=\"ODataDemo.Address\" Nullable=\"false\" />\n" +
                "        <Property Name=\"Concurrency\" Type=\"Edm.Int32\" Nullable=\"false\" \n" +
                "ConcurrencyMode=\"Fixed\" />\n" +
                "        <NavigationProperty Name=\"Products\" \n" +
                "Relationship=\"ODataDemo.Product_Supplier_Supplier_Products\" FromRole=\"Supplier_Products\" \n" +
                "ToRole=\"Product_Supplier\" />\n" +
                "      </EntityType>\n" +
                "      <EntityType Name=\"ProductImage\" m:HasStream=\"true\">\n" +
                "        <Key>\n" +
                "           <PropertyRef Name=\"PhotoId\" />\n" +
                "        </Key>\n" +
                "        <Property Name=\"PhotoId\" Type=\"Edm.Int32\" Nullable=\"false\" />\n" +
                "      </EntityType>\n" +
                "      <ComplexType Name=\"Address\">\n" +
                "        <Property Name=\"Street\" Type=\"Edm.String\" Nullable=\"true\" />\n" +
                "        <Property Name=\"City\" Type=\"Edm.String\" Nullable=\"true\" />\n" +
                "        <Property Name=\"State\" Type=\"Edm.String\" Nullable=\"true\" />\n" +
                "        <Property Name=\"ZipCode\" Type=\"Edm.String\" Nullable=\"true\" />\n" +
                "        <Property Name=\"Country\" Type=\"Edm.String\" Nullable=\"true\" />\n" +
                "      </ComplexType>\n" +
                "      <Association Name=\"Product_Category_Category_Products\">\n" +
                "        <End Role=\"Product_Category\" Type=\"ODataDemo.Product\" Multiplicity=\"*\" />\n" +
                "        <End Role=\"Category_Products\" Type=\"ODataDemo.Category\" Multiplicity=\"0..1\" />\n" +
                "      </Association>\n" +
                "      <Association Name=\"Product_ProductImage_ProductImage_Products\">\n" +
                "        <End Role=\"Product_ProductImage\" Type=\"ODataDemo.Product\" Multiplicity=\"*\" />\n" +
                "        <End Role=\"ProductImage_Products\" Type=\"ODataDemo.ProductImage\" Multiplicity=\"0..1\" />\n" +
                "      </Association>\n" +
                "      <Association Name=\"Product_Supplier_Supplier_Products\">\n" +
                "        <End Role=\"Product_Supplier\" Type=\"ODataDemo.Product\" Multiplicity=\"*\" />\n" +
                "        <End Role=\"Supplier_Products\" Type=\"ODataDemo.Supplier\" Multiplicity=\"0..1\" />\n" +
                "      </Association>\n" +
                "      <EntityContainer Name=\"DemoService\" m:IsDefaultEntityContainer=\"true\">\n" +
                "        <EntitySet Name=\"Products\" EntityType=\"ODataDemo.Product\" />\n" +
                "        <EntitySet Name=\"Categories\" EntityType=\"ODataDemo.Category\" />\n" +
                "        <EntitySet Name=\"Suppliers\" EntityType=\"ODataDemo.Supplier\" />\n" +
                "        <EntitySet Name=\"ProductImages\" EntityType=\"ODataDemo.ProductImage\" />\n" +
                "        <AssociationSet Name=\"Products_Category_Categories\" \n" +
                "Association=\"ODataDemo.Product_Category_Category_Products\">\n" +
                "          <End Role=\"Product_Category\" EntitySet=\"Products\" />\n" +
                "          <End Role=\"Category_Products\" EntitySet=\"Categories\" />\n" +
                "        </AssociationSet>\n" +
                "        <AssociationSet Name=\"Product_ProductImage_ProductImage_Products\" \n" +
                "Association=\"ODataDemo.Product_ProductImage_ProductImage_Products\">\n" +
                "          <End Role=\"Product_ProductImage\" EntitySet=\"Products\" />\n" +
                "          <End Role=\"ProductImage_Products\" EntitySet=\"ProductImages\" />\n" +
                "        </AssociationSet>\n" +
                "        <AssociationSet Name=\"Products_Supplier_Suppliers\" \n" +
                "Association=\"ODataDemo.Product_Supplier_Supplier_Products\">\n" +
                "          <End Role=\"Product_Supplier\" EntitySet=\"Products\" />\n" +
                "          <End Role=\"Supplier_Products\" EntitySet=\"Suppliers\" />\n" +
                "        </AssociationSet>\n" +
                "        <FunctionImport Name=\"GetProductsByRating\" EntitySet=\"Products\" \n" +
                "ReturnType=\"Collection(ODataDemo.Product)\" m:HttpMethod=\"GET\">\n" +
                "          <Parameter Name=\"rating\" Type=\"Edm.Int32\" Mode=\"In\" />\n" +
                "        </FunctionImport>\n" +
                "      </EntityContainer>\n" +
                "    </Schema>\n" +
                "  </edmx:DataServices>\n" +
                "</edmx:Edmx>\n";

    private static final String NEW_CATEGORY_ENTRY_PAYLOAD_ATOM =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?> \n"+
            "<entry xmlns:d=\"http://schemas.microsoft.com/ado/2007/08/dataservices\" "+
            "    xmlns:m=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\" "+
            "    xmlns=\"http://www.w3.org/2005/Atom\">\n"+
            "  <title type=\"text\"></title> \n"+
            "  <updated>2010-02-27T21:36:47Z</updated>\n"+
            "  <author> \n"+
            "    <name /> \n"+
            "  </author> \n"+
            "  <category term=\"DataServiceProviderDemo.Category\"\n"+
            "      scheme=\"http://schemas.microsoft.com/ado/2007/08/dataservices/scheme\" /> \n"+
            "  <content type=\"application/xml\"> \n"+
            "    <m:properties> \n"+
            "      <d:ID>10</d:ID> \n"+
            "      <d:Name>Clothing</d:Name>\n"+
            "    </m:properties> \n"+
            "  </content> \n"+
            "</entry>\n";

    private static final String NEW_CATEGORY_ENTRY_PAYLOAD_JSON =
            "{\"d\": {\n" +
            "   \"__metadata\": {\n" +
            "       \"type\": \"ODataDemo.Category\"\n" +
            "   }, \n" +
            "   \"ID\": 100, \n" +
            "   \"Name\": \"Food\"\n" +
            "}}";

    private static final String UPDATE_CATEGORY_ENTRY_PAYLOAD_JSON =
            "{\"d\": {\n" +
            "   \"__metadata\": {\n" +
            "       \"type\": \"ODataDemo.Category\"\n" +
            "   }, \n" +
            "   \"Name\": \"Cuisine\"\n" +
            "}}";

    private static final String UPDATE_PRODUCT_ENTRY_SIMPLE_PROPERTY_PAYLOAD_JSON =
            "{\"d\": {\n" +
            "   \"Description\": \"Updated description\"\n" +
            "}}";

    private static final String UPDATE_SUPPLIER_ENTRY_COMPLEX_PROPERTY_PAYLOAD_JSON =
            "{\"d\": {\n" +
            "   \"Address\": {\n" +
            "       \"Street\": \"123 Street Rd\"\n" +
            "   }\n" +
            "}}";

    private static final String UPDATE_ENTRY_SINGLE_ENTITY_LINK_PAYLOAD_JSON =
            "{\"d\": {\n" +
            "   \"uri\" : \"Categories(0)\"\n" +
            "}}";

    private static final String UPDATE_ENTRY_MULTIPLE_ENTITIES_LINK_PAYLOAD_JSON =
            "{\"d\": [\n" +
            "   {\"uri\" : \"Products(0)\"},\n" +
            "   {\"uri\" : \"Products(1)\"},\n" +
            "   {\"uri\" : \"Products(2)\"}\n" +
            "]}";

    private static final String GET = "GET";
    private static final String POST = "POST";
    private static final String PUT = "PUT";
    private static final String PATCH = "PATCH";
    private static final String MERGE = "MERGE";
    private static final String DELETE = "DELETE";
    private static final String TRACE = "TRACE"; // this is used as an example of an unsupported method

    private static final List<String> PAYLOAD_METHODS = Arrays.asList(POST, PUT, PATCH, MERGE, TRACE);

    private OdataParser parser;

    public OdataParserTest() throws EntityProviderException {
        Edm entityDataModel = EntityProvider.readMetadata(new ByteArrayInputStream(METADATA_DOCUMENT_ODATA_V2.getBytes()), false);
        parser = new OdataParser(entityDataModel);
    }

    @Test
    public void testParseRequest_GivenValidParametersForMetadataDocument_ParsingSucceeds() throws Exception {
        OdataRequestInfo requestInfo = parser.parseRequest("/$metadata", "");

        assertTrue(requestInfo.isMetadataRequest());

        assertFalse(requestInfo.isValueRequest());
        assertFalse(requestInfo.isServiceDocumentRequest());
        assertFalse(requestInfo.isCount());

        assertEquals(null, requestInfo.getExpandExpressionString());
        assertEquals(0, requestInfo.getExpandNavigationProperties().size());
        assertEquals(null, requestInfo.getFilterExpression());
        assertEquals(null, requestInfo.getFilterExpressionString());
        assertEquals(null, requestInfo.getFormat());
        assertEquals("none", requestInfo.getInlineCount());
        assertEquals(null, requestInfo.getOrderByExpression());
        assertEquals(null, requestInfo.getOrderByExpressionString());
        assertEquals(null, requestInfo.getSelectExpressionString());
        assertEquals(0, requestInfo.getSelectItemList().size());
        assertEquals(null, requestInfo.getSkip());
        assertEquals(null, requestInfo.getTop());
        assertEquals(0, requestInfo.getCustomQueryOptions().size());
    }

    @Test
    public void testParseRequest_GivenValidParametersForServiceDocument_ParsingSucceeds() throws Exception {
        OdataRequestInfo requestInfo = parser.parseRequest("/", "");

        assertTrue(requestInfo.isServiceDocumentRequest());

        assertFalse(requestInfo.isMetadataRequest());
        assertFalse(requestInfo.isValueRequest());

        assertEquals(null, requestInfo.getExpandExpressionString());
        assertEquals(0, requestInfo.getExpandNavigationProperties().size());
        assertEquals(null, requestInfo.getFilterExpression());
        assertEquals(null, requestInfo.getFilterExpressionString());
        assertEquals(null, requestInfo.getFormat());
        assertEquals("none", requestInfo.getInlineCount());
        assertEquals(null, requestInfo.getOrderByExpression());
        assertEquals(null, requestInfo.getOrderByExpressionString());
        assertEquals(null, requestInfo.getSelectExpressionString());
        assertEquals(0, requestInfo.getSelectItemList().size());
        assertEquals(null, requestInfo.getSkip());
        assertEquals(null, requestInfo.getTop());
        assertEquals(0, requestInfo.getCustomQueryOptions().size());
    }

    @Test
    public void testParseRequest_GivenMultipleValidQueryOptions_ParsingSucceeds() throws Exception {
        OdataRequestInfo requestInfo = parser.parseRequest("/Categories",
                "$inlinecount=allpages&$orderby=Name asc&$filter=length(Name) ge 5&" +
                        "$skip=2&$expand=Products&$format=atom&$select=Name,Products&custom1=val");

        assertFalse(requestInfo.isMetadataRequest());
        assertFalse(requestInfo.isValueRequest());
        assertFalse(requestInfo.isServiceDocumentRequest());
        assertFalse(requestInfo.isCount());

        assertNotNull(requestInfo.getExpandNavigationProperties());
        assertEquals("Products", requestInfo.getExpandExpressionString());
        assertNotNull(requestInfo.getFilterExpression());
        assertEquals("length(Name) ge 5", requestInfo.getFilterExpressionString());
        assertEquals("atom", requestInfo.getFormat());
        assertEquals("allpages", requestInfo.getInlineCount());
        assertNotNull(requestInfo.getOrderByExpression());
        assertEquals("Name asc", requestInfo.getOrderByExpressionString());
        assertEquals("Name,Products", requestInfo.getSelectExpressionString());
        assertNotNull(requestInfo.getSelectItemList());
        assertEquals(new Integer(2), requestInfo.getSkip());
        assertEquals(null, requestInfo.getTop());

        Map<String, String> customOptions = requestInfo.getCustomQueryOptions();

        assertEquals(1, customOptions.size());

        for (String optionName : customOptions.keySet()) {
            assertEquals("custom1", optionName);
            assertEquals("val", customOptions.get(optionName));
        }
    }

    @Test
    public void testParseRequest_GivenValidParametersForEntitySet_ParsingSucceeds() throws Exception {
        OdataRequestInfo requestInfo = parser.parseRequest("/Categories(2)/Products/$count", "");

        assertFalse(requestInfo.isMetadataRequest());
        assertFalse(requestInfo.isValueRequest());
        assertFalse(requestInfo.isServiceDocumentRequest());

        assertTrue(requestInfo.isCount());

        assertEquals(null, requestInfo.getExpandExpressionString());
        assertEquals(0, requestInfo.getExpandNavigationProperties().size());
        assertEquals(null, requestInfo.getFilterExpression());
        assertEquals(null, requestInfo.getFilterExpressionString());
        assertEquals(null, requestInfo.getFormat());
        assertEquals("none", requestInfo.getInlineCount());
        assertEquals(null, requestInfo.getOrderByExpression());
        assertEquals(null, requestInfo.getOrderByExpressionString());
        assertEquals(null, requestInfo.getSelectExpressionString());
        assertEquals(0, requestInfo.getSelectItemList().size());
        assertEquals(null, requestInfo.getSkip());
        assertEquals(null, requestInfo.getTop());
        assertEquals(0, requestInfo.getCustomQueryOptions().size());
    }

    @Test
    public void testParseRequest_GivenValidParametersForSingleEntity_ParsingSucceeds() throws Exception {
        OdataRequestInfo requestInfo = parser.parseRequest("/Categories(2)", "$expand=Products");

        assertFalse(requestInfo.isMetadataRequest());
        assertFalse(requestInfo.isValueRequest());
        assertFalse(requestInfo.isServiceDocumentRequest());
        assertFalse(requestInfo.isCount());

        assertNotNull(requestInfo.getExpandNavigationProperties());
        assertEquals("Products", requestInfo.getExpandExpressionString());
        assertEquals(null, requestInfo.getFilterExpression());
        assertEquals(null, requestInfo.getFilterExpressionString());
        assertEquals(null, requestInfo.getFormat());
        assertEquals("none", requestInfo.getInlineCount());
        assertEquals(null, requestInfo.getOrderByExpression());
        assertEquals(null, requestInfo.getOrderByExpressionString());
        assertEquals(null, requestInfo.getSelectExpressionString());
        assertEquals(0, requestInfo.getSelectItemList().size());
        assertEquals(null, requestInfo.getSkip());
        assertEquals(null, requestInfo.getTop());
        assertEquals(0, requestInfo.getCustomQueryOptions().size());
    }

    @Test
    public void testParseRequest_GivenValidParametersForSingleEntityProperty_ParsingSucceeds() throws Exception {
        OdataRequestInfo requestInfo = parser.parseRequest("/Categories(2)/Name", "$format=json");

        assertFalse(requestInfo.isMetadataRequest());
        assertFalse(requestInfo.isValueRequest());
        assertFalse(requestInfo.isServiceDocumentRequest());
        assertFalse(requestInfo.isCount());

        assertEquals(null, requestInfo.getExpandExpressionString());
        assertEquals(0, requestInfo.getExpandNavigationProperties().size());
        assertEquals(null, requestInfo.getFilterExpression());
        assertEquals(null, requestInfo.getFilterExpressionString());
        assertEquals("json", requestInfo.getFormat());
        assertEquals("none", requestInfo.getInlineCount());
        assertEquals(null, requestInfo.getOrderByExpression());
        assertEquals(null, requestInfo.getOrderByExpressionString());
        assertEquals(null, requestInfo.getSelectExpressionString());
        assertEquals(0, requestInfo.getSelectItemList().size());
        assertEquals(null, requestInfo.getSkip());
        assertEquals(null, requestInfo.getTop());
        assertEquals(0, requestInfo.getCustomQueryOptions().size());
    }

    @Test(expected = OdataParser.OdataParsingException.class)
    public void testParserRequest_parseDuplicateQueryOptions() throws Exception {
        parser.parseRequest("/Categories(2)/Name", "$format=json&$format=atom");
    }

    @Test
    public void testParseRequest_GivenValidParametersForSingleEntityPropertyRawValue_ParsingSucceeds() throws Exception {
        OdataRequestInfo requestInfo = parser.parseRequest("/Categories(2)/Name/$value", "");

        assertTrue(requestInfo.isValueRequest());

        assertFalse(requestInfo.isMetadataRequest());
        assertFalse(requestInfo.isServiceDocumentRequest());
        assertFalse(requestInfo.isCount());

        assertEquals(null, requestInfo.getExpandExpressionString());
        assertEquals(0, requestInfo.getExpandNavigationProperties().size());
        assertEquals(null, requestInfo.getFilterExpression());
        assertEquals(null, requestInfo.getFilterExpressionString());
        assertEquals(null, requestInfo.getFormat());
        assertEquals("none", requestInfo.getInlineCount());
        assertEquals(null, requestInfo.getOrderByExpression());
        assertEquals(null, requestInfo.getOrderByExpressionString());
        assertEquals(null, requestInfo.getSelectExpressionString());
        assertEquals(0, requestInfo.getSelectItemList().size());
        assertEquals(null, requestInfo.getSkip());
        assertEquals(null, requestInfo.getTop());
        assertEquals(0, requestInfo.getCustomQueryOptions().size());
    }

    @Test
    public void testParseRequest_MatrixParametersInResourcePath_ParsingFails() {
        try {
            parser.parseRequest("/Categori;es", "$top=5");

            fail("Expected OdataParsingException");
        } catch (OdataParser.OdataParsingException e) {
            assertEquals("Could not parse matrix parameters in resource path.", e.getMessage());
        }
    }

    /**
     * Requests for the Service Metadata Document may only use the $format system option; all others will
     * fail to be parsed.
     */
    @Test
    public void testParseRequest_GivenOutOfPlaceSystemQueryOption_ParsingFails() {
        try {
            parser.parseRequest("/$metadata", "$skip=5");

            fail("Expected OdataParsingException");
        } catch (OdataParser.OdataParsingException e) {
            assertEquals(UriSyntaxException.class, e.getCause().getClass());
            assertEquals("System query option '$skip' is not compatible with the return type.", e.getMessage());
        }
    }

    @Test
    public void testParseRequest_GivenInvalidEntitySetResourcePath_ParsingFails() {
        try {
            parser.parseRequest("/NonExistentEntitySet", "$top=5");

            fail("Expected OdataParsingException");
        } catch (OdataParser.OdataParsingException e) {
            assertEquals(UriNotMatchingException.class, e.getCause().getClass());
            assertEquals("Could not find an entity set or function import for 'NonExistentEntitySet'.", e.getMessage());
        }
    }

    /**
     * In this test, the System Query Option '$top' has been misspelled and should not be recognized.
     */
    @Test
    public void testParseRequest_GivenUnrecognizedSystemQueryOptions_ParsingFails() {
        try {
            parser.parseRequest("/Products(1)", "$toop=5");

            fail("Expected OdataParsingException");
        } catch (OdataParser.OdataParsingException e) {
            assertEquals(UriSyntaxException.class, e.getCause().getClass());
            assertEquals("Invalid system query option: '$toop'. ", e.getMessage());
        }
    }

    /**
     * In this test, the value of the System Query Option '$filter' includes a quoted ampersand. Parsing should
     * succeed.
     *
     * NOTE: As of Apache Olingo v2.0.3 the standard query parameter extraction method has been fixed and now handles
     * ampersands correctly, as it splits the query string before decoding it (issue OLINGO-547).
     * Our alternative implementation - OdataParser.extractQueryParameters() - which performed the split in the correct
     * order, has been removed. We are now using the library method RestUtil.extractAllQueryParameters().
     */
    @Test
    public void testParseRequest_GivenPoorlyFormedFilterExpression_ParsingSucceeds() throws Exception {
        OdataRequestInfo requestInfo = parser.parseRequest("/Products", "$filter=Name eq 'R%26D'");

        assertFalse(requestInfo.isMetadataRequest());
        assertFalse(requestInfo.isValueRequest());
        assertFalse(requestInfo.isServiceDocumentRequest());
        assertFalse(requestInfo.isCount());

        assertNotNull(requestInfo.getExpandNavigationProperties());
        assertEquals(null, requestInfo.getExpandExpressionString());
        assertNotNull(requestInfo.getFilterExpression());
        assertEquals("Name eq 'R&D'", requestInfo.getFilterExpressionString());
        assertEquals(null, requestInfo.getFormat());
        assertEquals("none", requestInfo.getInlineCount());
        assertEquals(null, requestInfo.getOrderByExpression());
        assertEquals(null, requestInfo.getOrderByExpressionString());
        assertEquals(null, requestInfo.getSelectExpressionString());
        assertEquals(0, requestInfo.getSelectItemList().size());
        assertEquals(null, requestInfo.getSkip());
        assertEquals(null, requestInfo.getTop());
        assertEquals(0, requestInfo.getCustomQueryOptions().size());
    }

    /**
     * In this test, the System Query Option '$orderby' refers to a non-existent property and should be rejected.
     */
    @Test
    public void testParseRequest_GivenPoorlyFormedOrderByExpression_ParsingFails() {
        try {
            parser.parseRequest("/Products", "$orderby=Nadme asc");

            fail("Expected OdataParsingException");
        } catch (OdataParser.OdataParsingException e) {
            assertEquals(UriSyntaxException.class, e.getCause().getClass());
            assertEquals("Invalid order by expression: 'Nadme asc'. ", e.getMessage());
        }
    }

    @Test
    public void testParsePayload_GivenClosedPayloadInputStream_ParsingFails() throws Exception {
        InputStream payloadInputStream = new BufferedInputStream(new ByteArrayInputStream(NEW_CATEGORY_ENTRY_PAYLOAD_JSON.getBytes()));
        payloadInputStream.close();

        OdataRequestInfo requestInfo = parser.parseRequest("/Categories", "");

        try {
            parser.parsePayload(GET, requestInfo, payloadInputStream, "application/json");

            fail("Expected OdataParsingException");
        } catch (OdataParser.OdataParsingException e) {
            assertEquals(IOException.class, e.getCause().getClass());
            assertEquals("Payload could not be read: Stream closed", e.getMessage());
        }
    }

    @Test
    public void testParsePayload_UseInvalidMethodForRequestPathType_ParsingFails() throws Exception {
        InputStream payloadInputStream = new ByteArrayInputStream(NEW_CATEGORY_ENTRY_PAYLOAD_JSON.getBytes());

        OdataRequestInfo requestInfo = parser.parseRequest("/Products", "");

        try {
            parser.parsePayload(PUT, requestInfo, payloadInputStream, "application/json");

            fail("Expected OdataParsingException");
        } catch (OdataParser.OdataParsingException e) {
            assertEquals("HTTP method 'PUT' invalid for the requested resource.", e.getMessage());
        }
    }

    @Test
    public void testParsePayload_GivenValidAtomPayload_ParsingSucceeds() throws Exception {
        InputStream payloadInputStream = new ByteArrayInputStream(NEW_CATEGORY_ENTRY_PAYLOAD_ATOM.getBytes());

        OdataRequestInfo requestInfo = parser.parseRequest("/Categories", "");

        OdataPayloadInfo payloadInfo =
                parser.parsePayload(POST, requestInfo, payloadInputStream, "application/atom+xml");

        validatePayloadInfo(payloadInfo, true, false, false, 0, 0); // entity
    }

    /**
     * The parsing of the Atom payload doesn't include determining if the payload is correct for the
     * request path, so, unlike a JSON payload, it will succeed.
     */
    @Test
    public void testParsePayload_GivenWrongAtomPayloadForUri_ParsingSucceeds() throws Exception {
        InputStream payloadInputStream = new ByteArrayInputStream(NEW_CATEGORY_ENTRY_PAYLOAD_ATOM.getBytes());

        OdataRequestInfo requestInfo = parser.parseRequest("/Products", "");

        OdataPayloadInfo payloadInfo =
                parser.parsePayload(POST, requestInfo, payloadInputStream, "application/atom+xml");

        validatePayloadInfo(payloadInfo, true, false, false, 0, 0); // entity
    }

    @Test
    public void testParsePayload_GivenPoorlyFormedAtomPayload_ParsingFails() throws Exception {
        InputStream payloadInputStream = new ByteArrayInputStream("blarg/>".getBytes());

        OdataRequestInfo requestInfo = parser.parseRequest("/Categories", "");

        try {
            parser.parsePayload(POST, requestInfo, payloadInputStream, "application/atom+xml");

            fail("Expected OdataParsingException");
        } catch (OdataParser.OdataParsingException e) {
            assertEquals(EntityProviderException.class, e.getCause().getClass());
            assertEquals("An exception of type 'WstxUnexpectedCharException' occurred.", e.getMessage());
        }
    }

    @Test
    public void testParsePayload_GivenValidJsonPayload_ParsingSucceeds() throws Exception {
        InputStream payloadInputStream = new ByteArrayInputStream(NEW_CATEGORY_ENTRY_PAYLOAD_JSON.getBytes());

        OdataRequestInfo requestInfo = parser.parseRequest("/Categories", "");

        OdataPayloadInfo payloadInfo =
                parser.parsePayload(POST, requestInfo, payloadInputStream, "application/json");

        validatePayloadInfo(payloadInfo, true, false, false, 0, 0); // entity
    }

    @Test
    public void testParsePayload_GivenWrongJsonPayloadForUri_ParsingFails() throws Exception {
        InputStream payloadInputStream = new ByteArrayInputStream(NEW_CATEGORY_ENTRY_PAYLOAD_JSON.getBytes());

        OdataRequestInfo requestInfo = parser.parseRequest("/Products", "");

        try {
            parser.parsePayload(POST, requestInfo, payloadInputStream, "application/json");

            fail("Expected OdataParsingException");
        } catch (OdataParser.OdataParsingException e) {
            assertEquals(EntityProviderException.class, e.getCause().getClass());
            assertEquals("Supplied entity type 'ODataDemo.Product' does not match the content entity type 'ODataDemo.Category'.", e.getMessage());
        }
    }

    @Test
    public void testParsePayload_GivenPoorlyFormedJsonPayload_ParsingFails() throws Exception {
        InputStream payloadInputStream = new ByteArrayInputStream(":blarg".getBytes());

        OdataRequestInfo requestInfo = parser.parseRequest("/Products", "");

        try {
            parser.parsePayload(POST, requestInfo, payloadInputStream, "application/json");

            fail("Expected OdataParsingException");
        } catch (OdataParser.OdataParsingException e) {
            assertEquals(EntityProviderException.class, e.getCause().getClass());
            assertEquals("An exception of type 'MalformedJsonException' occurred.", e.getMessage());
        }
    }
    
    @Test
    public void testParsePayload_GivenEmptyPayloadWithHttpMethodGET_ParsingSucceeds() throws Exception {
        InputStream payloadInputStream = new ByteArrayInputStream(new byte[0]); // empty payload expected

        OdataRequestInfo requestInfo = parser.parseRequest("/Categories(1)/Products", "$filter=length%28Name%29%20gt%204&amp;$format=json");

        OdataPayloadInfo payloadInfo = parser.parsePayload(GET, requestInfo, payloadInputStream, "application/json");

        assertNull(payloadInfo);
    }

    @Test
    public void testParsePayload_GivenNonEmptyPayloadWithHttpMethodGET_ParsingFails() throws Exception {
        InputStream payloadInputStream = new ByteArrayInputStream("FOO".getBytes()); // non-empty payload will fail

        OdataRequestInfo requestInfo = parser.parseRequest("/Products(1)/Supplier", "");

        try {
            parser.parsePayload(GET, requestInfo, payloadInputStream, "application/json");

            fail("Expected OdataParsingException");
        } catch (OdataParser.OdataParsingException e) {
            assertEquals("Payload not supported for HTTP method 'GET'.", e.getMessage());
        }
    }

    @Test
    public void testParsePayload_GivenEmptyPayloadWithHttpMethodDELETE_ParsingSucceeds() throws Exception {
        InputStream payloadInputStream = new ByteArrayInputStream(new byte[0]); // empty payload expected

        OdataRequestInfo requestInfo = parser.parseRequest("/Products(1)/Name/$value", "");

        OdataPayloadInfo payloadInfo = parser.parsePayload(DELETE, requestInfo, payloadInputStream, "application/json");

        assertNull(payloadInfo);
    }

    @BugId("SSG-8940")
    @Test
    public void testParsePayload_GivenHttpMethodDELETEForMetadata_ParsingFails() throws Exception {
        InputStream payloadInputStream = new ByteArrayInputStream(new byte[0]); // empty payload expected

        OdataRequestInfo requestInfo = parser.parseRequest("/$metadata", "");

        try {
            parser.parsePayload(DELETE, requestInfo, payloadInputStream, "application/json");

            fail("Expected OdataParsingException");
        } catch (OdataParser.OdataParsingException e) {
            assertEquals("HTTP method 'DELETE' invalid for the requested resource.", e.getMessage());
        }
    }

    @BugId("SSG-8997")
    @Test
    public void testParsePayload_GivenEmptyPayloadWithHttpMethodDELETEForResourceSet_ParsingFails() throws Exception {
        InputStream payloadInputStream = new ByteArrayInputStream(new byte[0]); // empty payload expected

        OdataRequestInfo requestInfo = parser.parseRequest("/Products", "");

        try {
            parser.parsePayload(DELETE, requestInfo, payloadInputStream, "application/json");

            fail("Expected OdataParsingException");
        } catch (OdataParser.OdataParsingException e) {
            assertEquals("HTTP method 'DELETE' invalid for the requested resource.", e.getMessage());
        }
    }

    @Test
    public void testParsePayload_GivenNonEmptyPayloadWithHttpMethodDELETE_ParsingFails() throws Exception {
        InputStream payloadInputStream = new ByteArrayInputStream("FOO".getBytes()); // non-empty payload will fail

        OdataRequestInfo requestInfo = parser.parseRequest("/Products(1)/Supplier", "");

        try {
            parser.parsePayload(DELETE, requestInfo, payloadInputStream, "application/json");

            fail("Expected OdataParsingException");
        } catch (OdataParser.OdataParsingException e) {
            assertEquals("Payload not supported for HTTP method 'DELETE'.", e.getMessage());
        }
    }

    /**
     * Metadata request URI should not
     */
    @Test
    public void testParsePayload_MetadataDocumentRequest_ParsingSucceeds() throws Exception {
        OdataRequestInfo requestInfo = parser.parseRequest("/$metadata", "");

        OdataPayloadInfo payloadInfo =
                checkInvalidMethodsRejected("FOO", requestInfo, POST, PUT, PATCH, MERGE, TRACE);

        assertNull(payloadInfo);
    }

    /**
     * Create media resource requests should only accept POST
     */
    @Test
    public void testParsePayload_CreateNewMediaResourceEntryWithHttpMethodPost_ParsingSucceeds() throws Exception {
        InputStream payloadInputStream = new ByteArrayInputStream("JUNK BYTES".getBytes());

        OdataRequestInfo requestInfo = parser.parseRequest("/ProductImages", "");

        OdataPayloadInfo payloadInfo = parser.parsePayload(POST, requestInfo, payloadInputStream, "image/png");

        validatePayloadInfo(payloadInfo, false, false, true, 0, 0); // new media resource entry
    }

    /**
     * Create entity requests should only accept POST
     */
    @Test
    public void testParsePayload_CreateEntity_ParsingFailsOnInvalidHttpMethods() throws Exception {
        OdataRequestInfo requestInfo = parser.parseRequest("/Categories", "");

        OdataPayloadInfo payloadInfo =
                checkInvalidMethodsRejected(NEW_CATEGORY_ENTRY_PAYLOAD_JSON, requestInfo, PUT, PATCH, MERGE, TRACE);

        validatePayloadInfo(payloadInfo, true, false, false, 0, 0); // entity
    }

    /**
     * Update entity requests should only accept PUT, PATCH, and MERGE
     */
    @Test
    public void testParsePayload_UpdateEntity_ParsingFailsOnInvalidHttpMethods() throws Exception {
        OdataRequestInfo requestInfo = parser.parseRequest("/Categories(1)", "");

        OdataPayloadInfo payloadInfo =
                checkInvalidMethodsRejected(UPDATE_CATEGORY_ENTRY_PAYLOAD_JSON, requestInfo, POST, TRACE);

        validatePayloadInfo(payloadInfo, true, false, false, 0, 0); // entity
    }

    /**
     * Update complex property requests should only accept PUT, PATCH, and MERGE
     */
    @Test
    public void testParsePayload_UpdateComplexProperty_ParsingFailsOnInvalidHttpMethods() throws Exception {
        OdataRequestInfo requestInfo = parser.parseRequest("/Suppliers(1)/Address", "");

        OdataPayloadInfo payloadInfo = checkInvalidMethodsRejected(UPDATE_SUPPLIER_ENTRY_COMPLEX_PROPERTY_PAYLOAD_JSON,
                requestInfo, POST, TRACE);

        validatePayloadInfo(payloadInfo, false, false, false, 0, 1); // one property
    }

    /**
     * Update simple property requests should only accept PUT, PATCH, and MERGE
     */
    @Test
    public void testParsePayload_UpdateSimpleProperty_ParsingFailsOnInvalidHttpMethods() throws Exception {
        OdataRequestInfo requestInfo = parser.parseRequest("/Products(1)/Description", "");

        OdataPayloadInfo payloadInfo = checkInvalidMethodsRejected(UPDATE_PRODUCT_ENTRY_SIMPLE_PROPERTY_PAYLOAD_JSON,
                requestInfo, POST, TRACE);

        validatePayloadInfo(payloadInfo, false, false, false, 0, 1); // one property
    }

    /**
     * Update simple property value should only accept PUT, PATCH, and MERGE
     */
    @Test
    public void testParsePayload_UpdateSimplePropertyValue_ParsingFails() throws Exception {
        OdataRequestInfo requestInfo = parser.parseRequest("/Products(1)/Name/$value", "");

        OdataPayloadInfo payloadInfo =
                checkInvalidMethodsRejected("DifferentValue", requestInfo, POST, TRACE);

        validatePayloadInfo(payloadInfo, false, true, false, 0, 0); // value
    }

    /**
     * Update link to single entity (1 or 0..1 multiplicity) requests should only accept
     */
    @Test
    public void testParsePayload_UpdateLinkToSingleEntity_ParsingFailsOnInvalidHttpMethods() throws Exception {
        OdataRequestInfo requestInfo = parser.parseRequest("/Products(1)/$links/Category", "");

        OdataPayloadInfo payloadInfo = checkInvalidMethodsRejected(UPDATE_ENTRY_SINGLE_ENTITY_LINK_PAYLOAD_JSON,
                requestInfo, POST, TRACE);

        validatePayloadInfo(payloadInfo, false, false, false, 1, 0); // one link
    }

    /**
     * Update link to multiple entities ('*' multiplicity) requests should only accept
     */
    @Test
    public void testParsePayload_UpdateLinkToMultipleEntities_ParsingFailsOnInvalidHttpMethods() throws Exception {
        OdataRequestInfo requestInfo = parser.parseRequest("/Categories(1)/$links/Products", "");

        OdataPayloadInfo payloadInfo = checkInvalidMethodsRejected(UPDATE_ENTRY_MULTIPLE_ENTITIES_LINK_PAYLOAD_JSON,
                requestInfo, PUT, PATCH, MERGE, TRACE);

        validatePayloadInfo(payloadInfo, false, false, false, 3, 0); // three links
    }

    /**
     * Update media resource
     */
    @Test
    public void testParsePayload_UpdateMediaLinkEntry_ParsingFails() throws Exception {
        OdataRequestInfo requestInfo = parser.parseRequest("/ProductImages(1)/$value", "");

        OdataPayloadInfo payloadInfo =
                checkInvalidMethodsRejected("FOR JUNK BYTES", requestInfo, POST, PATCH, MERGE, TRACE);

        validatePayloadInfo(payloadInfo, false, false, true, 0, 0); // is media
    }

    /**
     * As of Icefish, batch requests are entirely unsupported.
     */
    @Test
    public void testParsePayload_BatchRequest_ParsingFails() throws Exception {
        InputStream payloadInputStream = new ByteArrayInputStream(new byte[0]); // empty payload is fine - it should not be touched anyway

        OdataRequestInfo requestInfo = parser.parseRequest("/$batch", "");

        try {
            parser.parsePayload(POST, requestInfo, payloadInputStream, "application/json");

            fail("Expected OdataParsingException");
        } catch (OdataParser.OdataParsingException e) {
            assertEquals("Parsing of Batch Requests not supported.", e.getMessage());
        }
    }

    @Test
    public void testParseRequest_filterExpression() throws Exception {
        String[] expectedParts = {"length", "Name", "eq", "gt", "le", "and", "Price", "10", "30"};
        OdataRequestInfo requestInfo = parser.parseRequest("/Products", "$filter=length(Name) eq 10 and Price gt 10 and Price le 30");
        assertCommonExpression(expectedParts, requestInfo.getFilterExpression());
    }

    @Test
    public void testParseRequest_filterWithUnaryExpression() throws Exception {
        String[] expectedParts = {"not", "Address", "City", "eq", "'Redmond'"};
        OdataRequestInfo requestInfo = parser.parseRequest("/Suppliers", "$filter=not (Address/City eq 'Redmond')");
        assertCommonExpression(expectedParts, requestInfo.getFilterExpression());
    }

    @Test
    public void testParseRequest_orderExpression() throws Exception {
        String[] expectedParts = {"Rating", "Category", "Name", "desc", "asc"};
        OdataRequestInfo requestInfo = parser.parseRequest("/Products", "$orderby=Rating,Category/Name desc");
        CommonExpression expression = requestInfo.getOrderByExpression();
        assertCommonExpression(expectedParts, expression);
    }

    private void assertCommonExpression(String[] expectedParts, CommonExpression expression) throws OdataValidationException {
        Set<String> actualParts = OdataParserUtil.getExpressionParts(expression);
        assertEquals(expectedParts.length, actualParts.size());

        for (String part : expectedParts) {
            assertTrue(actualParts.contains(part));
        }
    }

    private OdataPayloadInfo checkInvalidMethodsRejected(String jsonPayload,
                                                         OdataRequestInfo requestInfo, String... invalidMethods) {
        List<String> invalidMethodList = Arrays.asList(invalidMethods);

        OdataPayloadInfo payloadInfo = null;

        for (String method : PAYLOAD_METHODS) {
            try {
                InputStream payloadInputStream = new ByteArrayInputStream(jsonPayload.getBytes());
                payloadInfo = parser.parsePayload(method, requestInfo, payloadInputStream, "application/json");

                if (invalidMethodList.contains(method)) {
                    fail("Expected failure for method '" + method + "'.");
                }
            } catch (OdataParser.OdataParsingException e) {
                if (invalidMethodList.contains(method)) {
                    assertEquals("HTTP method '" + method + "' invalid for the requested resource.", e.getMessage());
                } else {
                    fail("Method '" + method + "' should not have caused an exception: " + e.getMessage());
                }
            }
        }

        // at least one HTTP method will parse successfully, and the OdataPayloadInfo will be identical for each
        return payloadInfo;
    }

    private void validatePayloadInfo(OdataPayloadInfo payloadInfo, boolean expectEntity, boolean expectValue,
                                     boolean expectMedia, int numExpectedLinks, int numExpectedProperties) {
        assertNotNull(payloadInfo);

        if (expectEntity) {
            assertNotNull(payloadInfo.getOdataEntry());
        } else {
            assertNull(payloadInfo.getOdataEntry());
        }

        if (expectValue) {
            assertNotNull(payloadInfo.getValue());
        } else {
            assertNull(payloadInfo.getValue());
        }

        assertEquals(expectMedia, payloadInfo.isMedia());

        assertEquals(numExpectedLinks, payloadInfo.getLinks().size());
        assertEquals(numExpectedProperties, payloadInfo.properties().size());
    }
}