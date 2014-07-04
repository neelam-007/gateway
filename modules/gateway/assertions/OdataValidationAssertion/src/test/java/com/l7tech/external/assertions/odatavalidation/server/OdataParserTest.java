package com.l7tech.external.assertions.odatavalidation.server;

import org.apache.olingo.odata2.api.edm.Edm;
import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.apache.olingo.odata2.api.ep.EntityProviderException;
import org.apache.olingo.odata2.api.uri.UriNotMatchingException;
import org.apache.olingo.odata2.api.uri.UriSyntaxException;
import org.apache.olingo.odata2.api.uri.expression.CommonExpression;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
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
                "      <Association Name=\"Product_Supplier_Supplier_Products\">\n" +
                "        <End Role=\"Product_Supplier\" Type=\"ODataDemo.Product\" Multiplicity=\"*\" />\n" +
                "        <End Role=\"Supplier_Products\" Type=\"ODataDemo.Supplier\" Multiplicity=\"0..1\" />\n" +
                "      </Association>\n" +
                "      <EntityContainer Name=\"DemoService\" m:IsDefaultEntityContainer=\"true\">\n" +
                "        <EntitySet Name=\"Products\" EntityType=\"ODataDemo.Product\" />\n" +
                "        <EntitySet Name=\"Categories\" EntityType=\"ODataDemo.Category\" />\n" +
                "        <EntitySet Name=\"Suppliers\" EntityType=\"ODataDemo.Supplier\" />\n" +
                "        <AssociationSet Name=\"Products_Category_Categories\" \n" +
                "Association=\"ODataDemo.Product_Category_Category_Products\">\n" +
                "          <End Role=\"Product_Category\" EntitySet=\"Products\" />\n" +
                "          <End Role=\"Category_Products\" EntitySet=\"Categories\" />\n" +
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
            "       \"uri\": \"http://services.odata.org/V2/OData/OData.svc/Categories(0)\", " +
            "       \"type\": \"ODataDemo.Category\"\n" +
            "   }, \n" +
            "   \"ID\": 100, \n" +
            "   \"Name\": \"Food\"\n" +
            "}}";

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
     * In this test, the System Query Option '$top' has been misspelled and should not be recognized.
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
    public void testParsePayload_UseInvalidMethodForRequestPathType_ParsingFails() throws Exception {
        InputStream payloadInputStream = new ByteArrayInputStream(NEW_CATEGORY_ENTRY_PAYLOAD_JSON.getBytes());

        OdataRequestInfo requestInfo = parser.parseRequest("/Products", "");

        try {
            parser.parsePayload("PUT", requestInfo, payloadInputStream, "application/json");

            fail("Expected OdataParsingException");
        } catch (OdataParser.OdataParsingException e) {
            assertEquals("HTTP method PUT invalid for the requested resource.", e.getMessage());
        }
    }

    @Test
    public void testParsePayload_GivenValidAtomPayload_ParsingSucceeds() throws Exception {
        InputStream payloadInputStream = new ByteArrayInputStream(NEW_CATEGORY_ENTRY_PAYLOAD_ATOM.getBytes());

        OdataRequestInfo requestInfo = parser.parseRequest("/Categories", "");

        OdataPayloadInfo payloadInfo =
                parser.parsePayload("POST", requestInfo, payloadInputStream, "application/atom+xml");

        assertEquals(false, payloadInfo.containsOpenTypeEntity());
        // TODO jwilliams: validate payloadInfo
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
                parser.parsePayload("POST", requestInfo, payloadInputStream, "application/atom+xml");

        assertEquals(false, payloadInfo.containsOpenTypeEntity());
        // TODO jwilliams: validate payloadInfo
    }

    @Test
    public void testParsePayload_GivenPoorlyFormedAtomPayload_ParsingFails() throws Exception {
        InputStream payloadInputStream = new ByteArrayInputStream("blarg/>".getBytes());

        OdataRequestInfo requestInfo = parser.parseRequest("/Categories", "");

        try {
            parser.parsePayload("POST", requestInfo, payloadInputStream, "application/atom+xml");

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
                parser.parsePayload("POST", requestInfo, payloadInputStream, "application/json");

        assertEquals(false, payloadInfo.containsOpenTypeEntity());
        // TODO jwilliams: validate payloadInfo
    }

    @Test
    public void testParsePayload_GivenWrongJsonPayloadForUri_ParsingFails() throws Exception {
        InputStream payloadInputStream = new ByteArrayInputStream(NEW_CATEGORY_ENTRY_PAYLOAD_JSON.getBytes());

        OdataRequestInfo requestInfo = parser.parseRequest("/Products", "");

        try {
            parser.parsePayload("POST", requestInfo, payloadInputStream, "application/json");

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
            parser.parsePayload("POST", requestInfo, payloadInputStream, "application/json");

            fail("Expected OdataParsingException");
        } catch (OdataParser.OdataParsingException e) {
            assertEquals(EntityProviderException.class, e.getCause().getClass());
            assertEquals("An exception of type 'MalformedJsonException' occurred.", e.getMessage());
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
        for(String part : expectedParts) {
            assertTrue(actualParts.contains(part));
        }
    }


}
