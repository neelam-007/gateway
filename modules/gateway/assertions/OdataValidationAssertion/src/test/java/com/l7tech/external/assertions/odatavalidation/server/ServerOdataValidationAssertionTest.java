package com.l7tech.external.assertions.odatavalidation.server;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.StashManager;
import com.l7tech.external.assertions.odatavalidation.OdataValidationAssertion;
import com.l7tech.gateway.common.audit.AssertionMessages;
import com.l7tech.gateway.common.audit.TestAudit;
import com.l7tech.message.HttpServletRequestKnob;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.TestStashManagerFactory;
import com.l7tech.server.boot.GatewayPermissiveLoggingSecurityManager;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import com.l7tech.util.CollectionUtils;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.inject.Inject;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumSet;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * Test the OdataValidationAssertion.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "/com/l7tech/server/resources/testApplicationContext.xml")
public class ServerOdataValidationAssertionTest {
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

    private static final String METADATA_DOCUMENT_ODATA_V3 =
            "<?xml version=\"1.0\" encoding=\"utf-8\"?><edmx:Edmx Version=\"1.0\" \n" +
            "xmlns:edmx=\"http://schemas.microsoft.com/ado/2007/06/edmx\"><edmx:DataServices \n" +
            "m:DataServiceVersion=\"3.0\" m:MaxDataServiceVersion=\"3.0\" \n" +
            "xmlns:m=\"http://schemas.microsoft.com/ado/2007/08/dataservices/metadata\"><Schema \n" +
            "Namespace=\"ODataDemo\" xmlns=\"http://schemas.microsoft.com/ado/2009/11/edm\"><EntityType \n" +
            "Name=\"Product\"><Key><PropertyRef Name=\"ID\" /></Key><Property Name=\"ID\" \n" +
            "Type=\"Edm.Int32\" Nullable=\"false\" /><Property Name=\"Name\" Type=\"Edm.String\" \n" +
            "m:FC_TargetPath=\"SyndicationTitle\" m:FC_ContentKind=\"text\" m:FC_KeepInContent=\"false\" \n" +
            "/><Property Name=\"Description\" Type=\"Edm.String\" m:FC_TargetPath=\"SyndicationSummary\" \n" +
            "m:FC_ContentKind=\"text\" m:FC_KeepInContent=\"false\" /><Property Name=\"ReleaseDate\" \n" +
            "Type=\"Edm.DateTime\" Nullable=\"false\" /><Property Name=\"DiscontinuedDate\" \n" +
            "Type=\"Edm.DateTime\" /><Property Name=\"Rating\" Type=\"Edm.Int16\" Nullable=\"false\" \n" +
            "/><Property Name=\"Price\" Type=\"Edm.Double\" Nullable=\"false\" /><NavigationProperty \n" +
            "Name=\"Categories\" Relationship=\"ODataDemo.Product_Categories_Category_Products\" \n" +
            "ToRole=\"Category_Products\" FromRole=\"Product_Categories\" /><NavigationProperty \n" +
            "Name=\"Supplier\" Relationship=\"ODataDemo.Product_Supplier_Supplier_Products\" \n" +
            "ToRole=\"Supplier_Products\" FromRole=\"Product_Supplier\" /><NavigationProperty \n" +
            "Name=\"ProductDetail\" \n" +
            "Relationship=\"ODataDemo.Product_ProductDetail_ProductDetail_Product\" \n" +
            "ToRole=\"ProductDetail_Product\" FromRole=\"Product_ProductDetail\" \n" +
            "/></EntityType><EntityType Name=\"FeaturedProduct\" \n" +
            "BaseType=\"ODataDemo.Product\"><NavigationProperty Name=\"Advertisement\" \n" +
            "Relationship=\"ODataDemo.FeaturedProduct_Advertisement_Advertisement_FeaturedProduct\" \n" +
            "ToRole=\"Advertisement_FeaturedProduct\" FromRole=\"FeaturedProduct_Advertisement\" \n" +
            "/></EntityType><EntityType Name=\"ProductDetail\"><Key><PropertyRef Name=\"ProductID\" \n" +
            "/></Key><Property Name=\"ProductID\" Type=\"Edm.Int32\" Nullable=\"false\" /><Property \n" +
            "Name=\"Details\" Type=\"Edm.String\" /><NavigationProperty Name=\"Product\" \n" +
            "Relationship=\"ODataDemo.Product_ProductDetail_ProductDetail_Product\" \n" +
            "ToRole=\"Product_ProductDetail\" FromRole=\"ProductDetail_Product\" \n" +
            "/></EntityType><EntityType Name=\"Category\" OpenType=\"true\"><Key><PropertyRef Name=\"ID\" \n" +
            "/></Key><Property Name=\"ID\" Type=\"Edm.Int32\" Nullable=\"false\" /><Property Name=\"Name\" \n" +
            "Type=\"Edm.String\" m:FC_TargetPath=\"SyndicationTitle\" m:FC_ContentKind=\"text\" \n" +
            "m:FC_KeepInContent=\"true\" /><NavigationProperty Name=\"Products\" \n" +
            "Relationship=\"ODataDemo.Product_Categories_Category_Products\" \n" +
            "ToRole=\"Product_Categories\" FromRole=\"Category_Products\" /></EntityType><EntityType \n" +
            "Name=\"Supplier\"><Key><PropertyRef Name=\"ID\" /></Key><Property Name=\"ID\" \n" +
            "Type=\"Edm.Int32\" Nullable=\"false\" /><Property Name=\"Name\" Type=\"Edm.String\" \n" +
            "m:FC_TargetPath=\"SyndicationTitle\" m:FC_ContentKind=\"text\" m:FC_KeepInContent=\"true\" \n" +
            "/><Property Name=\"Address\" Type=\"ODataDemo.Address\" /><Property Name=\"Location\" \n" +
            "Type=\"Edm.GeographyPoint\" SRID=\"Variable\" /><Property Name=\"Concurrency\" \n" +
            "Type=\"Edm.Int32\" ConcurrencyMode=\"Fixed\" Nullable=\"false\" /><NavigationProperty \n" +
            "Name=\"Products\" Relationship=\"ODataDemo.Product_Supplier_Supplier_Products\" \n" +
            "ToRole=\"Product_Supplier\" FromRole=\"Supplier_Products\" /></EntityType><ComplexType \n" +
            "Name=\"Address\"><Property Name=\"Street\" Type=\"Edm.String\" /><Property Name=\"City\" \n" +
            "Type=\"Edm.String\" /><Property Name=\"State\" Type=\"Edm.String\" /><Property \n" +
            "Name=\"ZipCode\" Type=\"Edm.String\" /><Property Name=\"Country\" Type=\"Edm.String\" \n" +
            "/></ComplexType><EntityType Name=\"Person\"><Key><PropertyRef Name=\"ID\" \n" +
            "/></Key><Property Name=\"ID\" Type=\"Edm.Int32\" Nullable=\"false\" /><Property Name=\"Name\" \n" +
            "Type=\"Edm.String\" /><NavigationProperty Name=\"PersonDetail\" \n" +
            "Relationship=\"ODataDemo.Person_PersonDetail_PersonDetail_Person\" \n" +
            "ToRole=\"PersonDetail_Person\" FromRole=\"Person_PersonDetail\" /></EntityType><EntityType \n" +
            "Name=\"Customer\" BaseType=\"ODataDemo.Person\"><Property Name=\"TotalExpense\" \n" +
            "Type=\"Edm.Decimal\" Nullable=\"false\" /></EntityType><EntityType Name=\"Employee\" \n" +
            "BaseType=\"ODataDemo.Person\"><Property Name=\"EmployeeID\" Type=\"Edm.Int64\" \n" +
            "Nullable=\"false\" /><Property Name=\"HireDate\" Type=\"Edm.DateTime\" Nullable=\"false\" \n" +
            "/><Property Name=\"Salary\" Type=\"Edm.Single\" Nullable=\"false\" \n" +
            "/></EntityType><EntityType Name=\"PersonDetail\"><Key><PropertyRef Name=\"PersonID\" \n" +
            "/></Key><Property Name=\"PersonID\" Type=\"Edm.Int32\" Nullable=\"false\" /><Property \n" +
            "Name=\"Age\" Type=\"Edm.Byte\" Nullable=\"false\" /><Property Name=\"Gender\" \n" +
            "Type=\"Edm.Boolean\" Nullable=\"false\" /><Property Name=\"Phone\" Type=\"Edm.String\" \n" +
            "/><Property Name=\"Address\" Type=\"ODataDemo.Address\" /><Property Name=\"Photo\" \n" +
            "Type=\"Edm.Stream\" Nullable=\"false\" /><NavigationProperty Name=\"Person\" \n" +
            "Relationship=\"ODataDemo.Person_PersonDetail_PersonDetail_Person\" \n" +
            "ToRole=\"Person_PersonDetail\" FromRole=\"PersonDetail_Person\" /></EntityType><EntityType \n" +
            "Name=\"Advertisement\" m:HasStream=\"true\"><Key><PropertyRef Name=\"ID\" /></Key><Property \n" +
            "Name=\"ID\" Type=\"Edm.Guid\" Nullable=\"false\" /><Property Name=\"Name\" Type=\"Edm.String\" \n" +
            "/><Property Name=\"AirDate\" Type=\"Edm.DateTime\" Nullable=\"false\" /><NavigationProperty \n" +
            "Name=\"FeaturedProduct\" \n" +
            "Relationship=\"ODataDemo.FeaturedProduct_Advertisement_Advertisement_FeaturedProduct\" \n" +
            "ToRole=\"FeaturedProduct_Advertisement\" FromRole=\"Advertisement_FeaturedProduct\" \n" +
            "/></EntityType><Association Name=\"Product_Categories_Category_Products\"><End \n" +
            "Type=\"ODataDemo.Category\" Role=\"Category_Products\" Multiplicity=\"*\" /><End \n" +
            "Type=\"ODataDemo.Product\" Role=\"Product_Categories\" Multiplicity=\"*\" \n" +
            "/></Association><Association Name=\"Product_Supplier_Supplier_Products\"><End \n" +
            "Type=\"ODataDemo.Supplier\" Role=\"Supplier_Products\" Multiplicity=\"0..1\" /><End \n" +
            "Type=\"ODataDemo.Product\" Role=\"Product_Supplier\" Multiplicity=\"*\" \n" +
            "/></Association><Association Name=\"Product_ProductDetail_ProductDetail_Product\"><End \n" +
            "Type=\"ODataDemo.ProductDetail\" Role=\"ProductDetail_Product\" Multiplicity=\"0..1\" /><End \n" +
            "Type=\"ODataDemo.Product\" Role=\"Product_ProductDetail\" Multiplicity=\"0..1\" \n" +
            "/></Association><Association \n" +
            "Name=\"FeaturedProduct_Advertisement_Advertisement_FeaturedProduct\"><End \n" +
            "Type=\"ODataDemo.Advertisement\" Role=\"Advertisement_FeaturedProduct\" \n" +
            "Multiplicity=\"0..1\" /><End Type=\"ODataDemo.FeaturedProduct\" \n" +
            "Role=\"FeaturedProduct_Advertisement\" Multiplicity=\"0..1\" /></Association><Association \n" +
            "Name=\"Person_PersonDetail_PersonDetail_Person\"><End Type=\"ODataDemo.PersonDetail\" \n" +
            "Role=\"PersonDetail_Person\" Multiplicity=\"0..1\" /><End Type=\"ODataDemo.Person\" \n" +
            "Role=\"Person_PersonDetail\" Multiplicity=\"0..1\" /></Association><EntityContainer \n" +
            "Name=\"DemoService\" m:IsDefaultEntityContainer=\"true\"><EntitySet Name=\"Products\" \n" +
            "EntityType=\"ODataDemo.Product\" /><EntitySet Name=\"ProductDetails\" \n" +
            "EntityType=\"ODataDemo.ProductDetail\" /><EntitySet Name=\"Categories\" \n" +
            "EntityType=\"ODataDemo.Category\" /><EntitySet Name=\"Suppliers\" \n" +
            "EntityType=\"ODataDemo.Supplier\" /><EntitySet Name=\"Persons\" \n" +
            "EntityType=\"ODataDemo.Person\" /><EntitySet Name=\"PersonDetails\" \n" +
            "EntityType=\"ODataDemo.PersonDetail\" /><EntitySet Name=\"Advertisements\" \n" +
            "EntityType=\"ODataDemo.Advertisement\" /><FunctionImport Name=\"GetProductsByRating\" \n" +
            "ReturnType=\"Collection(ODataDemo.Product)\" EntitySet=\"Products\" \n" +
            "m:HttpMethod=\"GET\"><Parameter Name=\"rating\" Type=\"Edm.Int16\" Nullable=\"false\" \n" +
            "/></FunctionImport><AssociationSet Name=\"Products_Advertisement_Advertisements\" \n" +
            "Association=\"ODataDemo.FeaturedProduct_Advertisement_Advertisement_FeaturedProduct\"><E\n" +
            "nd Role=\"FeaturedProduct_Advertisement\" EntitySet=\"Products\" /><End \n" +
            "Role=\"Advertisement_FeaturedProduct\" EntitySet=\"Advertisements\" \n" +
            "/></AssociationSet><AssociationSet Name=\"Products_Categories_Categories\" \n" +
            "Association=\"ODataDemo.Product_Categories_Category_Products\"><End \n" +
            "Role=\"Product_Categories\" EntitySet=\"Products\" /><End Role=\"Category_Products\" \n" +
            "EntitySet=\"Categories\" /></AssociationSet><AssociationSet \n" +
            "Name=\"Products_Supplier_Suppliers\" \n" +
            "Association=\"ODataDemo.Product_Supplier_Supplier_Products\"><End \n" +
            "Role=\"Product_Supplier\" EntitySet=\"Products\" /><End Role=\"Supplier_Products\" \n" +
            "EntitySet=\"Suppliers\" /></AssociationSet><AssociationSet \n" +
            "Name=\"Products_ProductDetail_ProductDetails\" \n" +
            "Association=\"ODataDemo.Product_ProductDetail_ProductDetail_Product\"><End \n" +
            "Role=\"Product_ProductDetail\" EntitySet=\"Products\" /><End Role=\"ProductDetail_Product\" \n" +
            "EntitySet=\"ProductDetails\" /></AssociationSet><AssociationSet \n" +
            "Name=\"Persons_PersonDetail_PersonDetails\" \n" +
            "Association=\"ODataDemo.Person_PersonDetail_PersonDetail_Person\"><End \n" +
            "Role=\"Person_PersonDetail\" EntitySet=\"Persons\" /><End Role=\"PersonDetail_Person\" \n" +
            "EntitySet=\"PersonDetails\" /></AssociationSet></EntityContainer><Annotations \n" +
            "Target=\"ODataDemo.DemoService\"><ValueAnnotation \n" +
            "Term=\"Org.OData.Display.V1.Description\" String=\"This is a sample OData service with \n" +
            "vocabularies\" /></Annotations><Annotations Target=\"ODataDemo.Product\"><ValueAnnotation \n" +
            "Term=\"Org.OData.Display.V1.Description\" String=\"All Products available in the online \n" +
            "store\" /></Annotations><Annotations Target=\"ODataDemo.Product/Name\"><ValueAnnotation \n" +
            "Term=\"Org.OData.Display.V1.DisplayName\" String=\"Product Name\" \n" +
            "/></Annotations><Annotations Target=\"ODataDemo.DemoService/Suppliers\"><ValueAnnotation \n" +
            "Term=\"Org.OData.Publication.V1.PublisherName\" String=\"Microsoft Corp.\" \n" +
            "/><ValueAnnotation Term=\"Org.OData.Publication.V1.PublisherId\" String=\"MSFT\" \n" +
            "/><ValueAnnotation Term=\"Org.OData.Publication.V1.Keywords\" String=\"Inventory, \n" +
            "Supplier, Advertisers, Sales, Finance\" /><ValueAnnotation \n" +
            "Term=\"Org.OData.Publication.V1.AttributionUrl\" String=\"http://www.odata.org/\" \n" +
            "/><ValueAnnotation Term=\"Org.OData.Publication.V1.AttributionDescription\" String=\"All \n" +
            "rights reserved\" /><ValueAnnotation Term=\"Org.OData.Publication.V1.DocumentationUrl \" \n" +
            "String=\"http://www.odata.org/\" /><ValueAnnotation \n" +
            "Term=\"Org.OData.Publication.V1.TermsOfUseUrl\" String=\"All rights reserved\" \n" +
            "/><ValueAnnotation Term=\"Org.OData.Publication.V1.PrivacyPolicyUrl\" \n" +
            "String=\"http://www.odata.org/\" /><ValueAnnotation \n" +
            "Term=\"Org.OData.Publication.V1.LastModified\" String=\"4/2/2013\" /><ValueAnnotation \n" +
            "Term=\"Org.OData.Publication.V1.ImageUrl \" String=\"http://www.odata.org/\" \n" +
            "/></Annotations></Schema></edmx:DataServices></edmx:Edmx>\n";

    private static final String NEW_CATEGORY_ENTRY_PAYLOAD_JSON =
            "{\"d\": {\n" +
                    "   \"__metadata\": {\n" +
                    "       \"type\": \"ODataDemo.Category\"\n" +
                    "   }, \n" +
                    "   \"ID\": 100, \n" +
                    "   \"Name\": \"Food\"\n" +
                    "}}";

    private final ContentTypeHeader ODATA_JSON = ContentTypeHeader.create("application/json");

    @Inject
    private ApplicationContext applicationContext;

    @Inject
    private StashManagerFactory stashManagerFactory;

    private StashManager stashManager;
    private TestAudit testAudit;
    private SecurityManager originalSecurityManager;

    @Before
    public void setUp() {
        testAudit = new TestAudit();
        stashManager = stashManagerFactory.createStashManager();

        originalSecurityManager = System.getSecurityManager();
        System.setSecurityManager(new GatewayPermissiveLoggingSecurityManager());
    }

    @After
    public void tearDown() throws Exception {
        System.setSecurityManager(originalSecurityManager);
    }

    /**
     * A valid Service Metadata Document, no constraints violated, assertion should pass.
     */
    @Test
    public void doCheckRequest_GivenValidServiceMetadataDocument_AssertionPasses() throws Exception {
        OdataValidationAssertion assertion = new OdataValidationAssertion();
        assertion.setOdataMetadataSource("${fooVar}");
        assertion.setResourceUrl("${urlResource}");
        assertion.setValidatePayload(false);

        PolicyEnforcementContext pec = createPolicyEnforcementContext(TargetMessageType.REQUEST,
                createHttpRequestMessage("http://services.odata.org/OData/OData.svc/Categories(1)/Products",
                        "GET", ODATA_JSON,
                        new ByteArrayInputStream(new byte[0]))
        );

        pec.setVariable("fooVar", METADATA_DOCUMENT_ODATA_V2);
        pec.setVariable("urlResource", "/Categories(1)/Products");

        ServerOdataValidationAssertion serverAssertion = createServer(assertion);

        AssertionStatus status = serverAssertion.doCheckRequest(pec, pec.getRequest(),
                assertion.getTargetName(), pec.getAuthenticationContext(pec.getRequest()));

        assertEquals(AssertionStatus.NONE, status);
        assertTrue(pec.getVariable("odata.query.pathsegments") instanceof String[]);
        assertArrayEquals(new String[]{"Categories(1)", "Products"}, (String[])pec.getVariable("odata.query.pathsegments"));

        // expect the entry name length violation to be audited
        checkAuditPresence(false, false, false, false, false, false);
    }

    /**
     * Attempt to create entity, POST operations forbidden, assertion should fail.
     */
    @Test
    public void doCheckRequest_RequestUsesForbiddenOperation_AssertionFalsified() throws Exception {
        OdataValidationAssertion assertion = new OdataValidationAssertion();

        assertion.setOdataMetadataSource("${fooVar}");
        assertion.setResourceUrl("${urlResource}");
        assertion.setActions(EnumSet.noneOf(OdataValidationAssertion.ProtectionActions.class));
        assertion.setCreateOperation(false);

        PolicyEnforcementContext pec = createPolicyEnforcementContext(TargetMessageType.REQUEST,
                createHttpRequestMessage("http://services.odata.org/OData/OData.svc/Categories",
                        "MERGE", ODATA_JSON,
                        new ByteArrayInputStream(NEW_CATEGORY_ENTRY_PAYLOAD_JSON.getBytes()))
        );

        pec.setVariable("fooVar", METADATA_DOCUMENT_ODATA_V2);
        pec.setVariable("urlResource", "/Categories");

        ServerOdataValidationAssertion serverAssertion = createServer(assertion);

        AssertionStatus status = serverAssertion.doCheckRequest(pec, pec.getRequest(),
                assertion.getTargetName(), pec.getAuthenticationContext(pec.getRequest()));

        assertEquals(AssertionStatus.FALSIFIED, status);

        // expect the forbidden operation attempt to be audited
        checkAuditPresence(false, false, false, false, true, false);
    }

    /**
     * Request the Service Metadata Document when disallowed, assertion should fail.
     */
    @Test
    public void doCheckRequest_Request$MetadataNotAllowed_AssertionFalsified() throws Exception {
        OdataValidationAssertion assertion = new OdataValidationAssertion();

        assertion.setOdataMetadataSource("${fooVar}");
        assertion.setResourceUrl("${urlResource}");
        assertion.setActions(EnumSet.of(OdataValidationAssertion.ProtectionActions.ALLOW_RAW_VALUE));

        PolicyEnforcementContext pec = createPolicyEnforcementContext(TargetMessageType.REQUEST,
                createHttpRequestMessage("http://services.odata.org/OData/OData.svc/$metadata",
                        "GET", ODATA_JSON,
                        new ByteArrayInputStream(new byte[0]))
        );

        pec.setVariable("fooVar", METADATA_DOCUMENT_ODATA_V2);
        pec.setVariable("urlResource", "/$metadata");

        ServerOdataValidationAssertion serverAssertion = createServer(assertion);

        AssertionStatus status = serverAssertion.doCheckRequest(pec, pec.getRequest(),
                assertion.getTargetName(), pec.getAuthenticationContext(pec.getRequest()));

        assertEquals(AssertionStatus.FALSIFIED, status);

        // expect the entry name length violation to be audited
        checkAuditPresence(false, false, true, false, false, false);
    }

    /**
     * Request a raw value when disallowed, assertion should fail.
     */
    @Test
    public void doCheckRequest_Request$valueNotAllowed_AssertionFalsified() throws Exception {
        OdataValidationAssertion assertion = new OdataValidationAssertion();

        assertion.setOdataMetadataSource("${fooVar}");
        assertion.setResourceUrl("${urlResource}");
        assertion.setActions(EnumSet.of(OdataValidationAssertion.ProtectionActions.ALLOW_METADATA));

        PolicyEnforcementContext pec = createPolicyEnforcementContext(TargetMessageType.REQUEST,
                createHttpRequestMessage("http://services.odata.org/OData/OData.svc/Products(1)/Name/$value",
                        "GET", ODATA_JSON,
                        new ByteArrayInputStream(new byte[0]))
        );

        pec.setVariable("fooVar", METADATA_DOCUMENT_ODATA_V2);
        pec.setVariable("urlResource", "/Products(1)/Name/$value");

        ServerOdataValidationAssertion serverAssertion = createServer(assertion);

        AssertionStatus status = serverAssertion.doCheckRequest(pec, pec.getRequest(),
                assertion.getTargetName(), pec.getAuthenticationContext(pec.getRequest()));

        assertEquals(AssertionStatus.FALSIFIED, status);

        // expect the entry name length violation to be audited
        checkAuditPresence(false, false, false, true, false, false);

        // ensure the request path is included in the audit
        assertTrue(testAudit.isAuditPresentContaining("Request for raw value attempted: /Products(1)/Name/$value"));
    }

    @Test
    public void doCheckRequest_withQuery_AssertionPasses() throws Exception {
        OdataValidationAssertion assertion = new OdataValidationAssertion();
        assertion.setOdataMetadataSource("${fooVar}");
        assertion.setResourceUrl("${urlResource}");
        assertion.setVariablePrefix("o");
        assertion.setValidatePayload(false);

        PolicyEnforcementContext pec = createPolicyEnforcementContext(TargetMessageType.REQUEST,
                createHttpRequestMessage("http://services.odata.org/OData/OData.svc/Categories%281%29/Products?$top=3&$filter=length%28Name%29%20le%205",
                        "GET", ODATA_JSON,
                        new ByteArrayInputStream(new byte[0]))
        );

        pec.setVariable("fooVar", METADATA_DOCUMENT_ODATA_V2);
        pec.setVariable("urlResource", "/Categories(1)/Products?$top=3&$filter=length(Name) le 5&$skip=2&$select=*&$orderby=Rating,Category/Name desc&$expand=Category&custom=option");

        ServerOdataValidationAssertion serverAssertion = createServer(assertion);

        assertEquals(AssertionStatus.NONE, serverAssertion.doCheckRequest(pec, pec.getRequest(),assertion.getTargetName(), pec.getAuthenticationContext(pec.getRequest())));

        assertTrue(pec.getVariable("o.query.filter") instanceof String[]);
        assertEquals("3", pec.getVariable("o.query.top"));
        assertEquals("2", pec.getVariable("o.query.skip"));
        assertTrue(pec.getVariable("o.query.orderby") instanceof String[]);
        assertEquals("*", pec.getVariable("o.query.select"));
        assertTrue(pec.getVariable("o.query.customoptions") instanceof String[]);
        assertTrue(pec.getVariable("o.query.pathsegments") instanceof String[]);
        assertArrayEquals(new String[]{"Categories(1)", "Products"}, (String[])pec.getVariable("o.query.pathsegments"));

        // expect the entry name length violation to be audited
        checkAuditPresence(false, false, false, false, false, false);
    }

    @Test
    public void shouldValidateAddNewEntry_AssertionPasses() throws Exception {
        OdataValidationAssertion assertion = new OdataValidationAssertion();
        assertion.setOdataMetadataSource("${fooVar}");
        assertion.setResourceUrl("${urlResource}");
        assertion.setVariablePrefix("o");
        assertion.setValidatePayload(true);

        PolicyEnforcementContext pec = createPolicyEnforcementContext(TargetMessageType.REQUEST,
                createHttpRequestMessage("http://services.odata.org/OData/OData.svc/Categories",
                        "POST", ODATA_JSON,
                        new ByteArrayInputStream(NEW_CATEGORY_ENTRY_PAYLOAD_JSON.getBytes()))
        );

        pec.setVariable("fooVar", METADATA_DOCUMENT_ODATA_V2);
        pec.setVariable("urlResource", "/Categories");

        ServerOdataValidationAssertion serverAssertion = createServer(assertion);

        assertEquals(AssertionStatus.NONE, serverAssertion.doCheckRequest(pec, pec.getRequest(),assertion.getTargetName(), pec.getAuthenticationContext(pec.getRequest())));
        assertArrayEquals(new String[]{"Categories"}, (String[])pec.getVariable("o.query.pathsegments"));

    }

    /**
     * Given a poorly formed or unsupported Service Metadata Document, the assertion should
     * audit the fact and return FAILED.
     */
    @Test
    public void doCheckRequest_GivenBadServiceMetadataDocument_AssertionFails() throws Exception {
        OdataValidationAssertion assertion = new OdataValidationAssertion();
        assertion.setOdataMetadataSource("{fooVar}");
        assertion.setResourceUrl("/Category(1)/Products?$top=2");

        PolicyEnforcementContext pec = createPolicyEnforcementContext(TargetMessageType.REQUEST,
                createHttpRequestMessage("http://services.odata.org/OData/OData.svc/Categories(1)/Products",
                        "GET", ODATA_JSON,
                        new ByteArrayInputStream(new byte[0]))
        );

        pec.setVariable(assertion.getOdataMetadataSource(), METADATA_DOCUMENT_ODATA_V3);

        ServerOdataValidationAssertion serverAssertion = createServer(assertion);

        AssertionStatus status = serverAssertion.doCheckRequest(pec, pec.getRequest(),
                assertion.getTargetName(), pec.getAuthenticationContext(pec.getRequest()));

        assertEquals(AssertionStatus.FAILED, status);

        // expect the entry name length violation to be audited
        checkAuditPresence(true, false, false, false, false, false);
    }

    @Test
    public void doCheckRequest_sourceMessageHasNoPayloadWhenPayloadValidationRequested() throws Exception {
        OdataValidationAssertion assertion = new OdataValidationAssertion();
        assertion.setOdataMetadataSource("${fooVar}");
        assertion.setResourceUrl("${urlResource}");
        assertion.setVariablePrefix("o");
        assertion.setMergeOperation(true);
        assertion.setValidatePayload(true);

        PolicyEnforcementContext pec = createPolicyEnforcementContext(TargetMessageType.REQUEST,
                createHttpRequestMessage("http://services.odata.org/OData/OData.svc/Categories(1)/Products",
                        "MERGE", ODATA_JSON,
                        new ByteArrayInputStream(new byte[0]))
        );

        pec.setVariable("fooVar", METADATA_DOCUMENT_ODATA_V2);
        pec.setVariable("urlResource", "/Categories");
        ServerOdataValidationAssertion serverAssertion = createServer(assertion);

        Assert.assertEquals(AssertionStatus.FALSIFIED, serverAssertion.doCheckRequest(pec, pec.getRequest(),
                assertion.getTargetName(), pec.getAuthenticationContext(pec.getRequest())));

        checkAuditPresence(false, false, false, false, false, true);

    }

    @Test
    public void doCheckRequest_sourceMessageIsNotHttp() throws Exception {
        OdataValidationAssertion assertion = new OdataValidationAssertion();
        Message request = new Message();
        request.initialize(TestStashManagerFactory.getInstance().createStashManager(), ContentTypeHeader.APPLICATION_JSON, new ByteArrayInputStream(new byte[0]));
        PolicyEnforcementContext pec = createPolicyEnforcementContext(TargetMessageType.REQUEST, request);
        ServerOdataValidationAssertion serverAssertion = createServer(assertion);

        Assert.assertEquals(AssertionStatus.FALSIFIED, serverAssertion.doCheckRequest(pec, pec.getRequest(),
                assertion.getTargetName(), pec.getAuthenticationContext(pec.getRequest())));
        checkAuditPresence(false, true, false, false, false, false);
    }

    /**
     * Checks presence or absence of audits to confirm the expected audits are present/not present.
     */
    private void checkAuditPresence(boolean invalidServiceMetadataDocument, boolean invalidRequestUri,
                                    boolean metadataRequestBlocked, boolean rawValueRequestBlocked, 
                                    boolean forbiddenOperation, boolean invalidPayload) {
        assertEquals(AssertionMessages.ODATA_VALIDATION_INVALID_SMD.getMessage(),
                invalidServiceMetadataDocument,
                testAudit.isAuditPresent(AssertionMessages.ODATA_VALIDATION_INVALID_SMD));

        assertEquals(AssertionMessages.ODATA_VALIDATION_INVALID_URI.getMessage(),
                invalidRequestUri,
                testAudit.isAuditPresent(AssertionMessages.ODATA_VALIDATION_INVALID_URI));

        assertEquals(AssertionMessages.ODATA_VALIDATION_REQUEST_MADE_FOR_SMD.getMessage(),
                metadataRequestBlocked,
                testAudit.isAuditPresent(AssertionMessages.ODATA_VALIDATION_REQUEST_MADE_FOR_SMD));

        assertEquals(AssertionMessages.ODATA_VALIDATION_REQUEST_MADE_FOR_RAW_VALUE.getMessage(),
                rawValueRequestBlocked,
                testAudit.isAuditPresent(AssertionMessages.ODATA_VALIDATION_REQUEST_MADE_FOR_RAW_VALUE));

        assertEquals(AssertionMessages.ODATA_VALIDATION_FORBIDDEN_OPERATION_ATTEMPTED.getMessage(),
                forbiddenOperation,
                testAudit.isAuditPresent(AssertionMessages.ODATA_VALIDATION_FORBIDDEN_OPERATION_ATTEMPTED));

        assertEquals(AssertionMessages.ODATA_VALIDATION_TARGET_INVALID_PAYLOAD.getMessage(),
                invalidPayload,
                testAudit.isAuditPresent(AssertionMessages.ODATA_VALIDATION_TARGET_INVALID_PAYLOAD));

    }

    private ServerOdataValidationAssertion createServer(OdataValidationAssertion assertion) {
        ServerOdataValidationAssertion serverAssertion = new ServerOdataValidationAssertion(assertion);

        ApplicationContexts.inject(serverAssertion, CollectionUtils.<String, Object>mapBuilder()
                .put("auditFactory", testAudit.factory())
                .unmodifiableMap()
        );

        return serverAssertion;
    }

    private PolicyEnforcementContext createPolicyEnforcementContext(TargetMessageType targetType, Message target) {
        Message request;
        Message response;

        if (TargetMessageType.REQUEST == targetType) {
            request = target;
            response = new Message();
        } else if (TargetMessageType.RESPONSE == targetType) {
            request = new Message();
            response = target;
        } else {
            request = new Message();
            response = new Message();
        }

        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }

    private Message createHttpRequestMessage(String requestUri, String method, ContentTypeHeader contentTypeHeader,
                                             InputStream body) throws IOException {
        MockHttpServletRequest hRequest = new MockHttpServletRequest();
        hRequest.setMethod(method);

        hRequest.setRequestURI(requestUri);

        Message request = new Message(stashManager, contentTypeHeader, body);
        request.attachHttpRequestKnob(new HttpServletRequestKnob(hRequest));

        return request;
    }
}
