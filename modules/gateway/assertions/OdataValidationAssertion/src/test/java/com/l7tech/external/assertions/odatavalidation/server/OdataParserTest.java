package com.l7tech.external.assertions.odatavalidation.server;

import org.apache.olingo.odata2.api.edm.Edm;
import org.apache.olingo.odata2.api.ep.EntityProvider;
import org.apache.olingo.odata2.api.ep.EntityProviderException;
import org.apache.olingo.odata2.api.uri.UriNotMatchingException;
import org.apache.olingo.odata2.api.uri.UriSyntaxException;
import org.junit.Test;

import java.io.ByteArrayInputStream;

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

    private OdataParser parser;

    public OdataParserTest() throws EntityProviderException {
        Edm entityDataModel = EntityProvider.readMetadata(new ByteArrayInputStream(METADATA_DOCUMENT_ODATA_V2.getBytes()), false);
        parser = new OdataParser(entityDataModel);
    }

    @Test
    public void testParseRequest_GivenValidParametersForMetadataDocument_ParsingSucceeds() throws Exception {
        OdataParser.OdataRequestInfo requestInfo = parser.parseRequest("/$metadata", "");

        assertTrue(requestInfo.isMetadataRequest());

        assertFalse(requestInfo.isValueRequest());
        assertFalse(requestInfo.isServiceDocumentRequest());
    }

    @Test
    public void testParseRequest_GivenValidParametersForServiceDocument_ParsingSucceeds() throws Exception {
        OdataParser.OdataRequestInfo requestInfo = parser.parseRequest("/", "");

        assertTrue(requestInfo.isServiceDocumentRequest());

        assertFalse(requestInfo.isMetadataRequest());
        assertFalse(requestInfo.isValueRequest());
    }

    @Test
    public void testParseRequest_GivenValidParametersForEntitySet_ParsingSucceeds() throws Exception {
        OdataParser.OdataRequestInfo requestInfo = parser.parseRequest("/Categories(2)/Products", "$orderby=Name asc");

        assertFalse(requestInfo.isMetadataRequest());
        assertFalse(requestInfo.isValueRequest());
        assertFalse(requestInfo.isServiceDocumentRequest());
    }

    @Test
    public void testParseRequest_GivenValidParametersForSingleEntity_ParsingSucceeds() throws Exception {
        OdataParser.OdataRequestInfo requestInfo = parser.parseRequest("/Categories(2)", "");

        assertFalse(requestInfo.isMetadataRequest());
        assertFalse(requestInfo.isValueRequest());
        assertFalse(requestInfo.isServiceDocumentRequest());
    }

    @Test
    public void testParseRequest_GivenValidParametersForSingleEntityProperty_ParsingSucceeds() throws Exception {
        OdataParser.OdataRequestInfo requestInfo = parser.parseRequest("/Categories(2)/Name", "");

        assertFalse(requestInfo.isMetadataRequest());
        assertFalse(requestInfo.isValueRequest());
        assertFalse(requestInfo.isServiceDocumentRequest());
    }

    @Test
    public void testParseRequest_GivenValidParametersForSingleEntityPropertyRawValue_ParsingSucceeds() throws Exception {
        OdataParser.OdataRequestInfo requestInfo = parser.parseRequest("/Categories(2)/Name/$value", "");

        assertTrue(requestInfo.isValueRequest());

        assertFalse(requestInfo.isMetadataRequest());
        assertFalse(requestInfo.isServiceDocumentRequest());
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
}
