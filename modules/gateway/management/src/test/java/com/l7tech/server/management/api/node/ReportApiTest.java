package com.l7tech.server.management.api.node;

import org.junit.Test;
import org.junit.Assert;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.Unmarshaller;
import javax.activation.DataHandler;
import javax.mail.util.ByteArrayDataSource;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.io.StringWriter;
import java.io.StringReader;

/**
 * Tests for Management Report API
 */
public class ReportApiTest {

    @Test
    public void testReportSubmissionSerialization() throws Exception {
        ReportApi.ReportSubmission submission = new ReportApi.ReportSubmission();
        submission.setName("Test name");
        submission.setType(ReportApi.ReportType.PERFORMANCE_INTERVAL);

        Collection<ReportApi.ReportSubmission.ReportParam> params = new ArrayList<ReportApi.ReportSubmission.ReportParam>();
        params.add(new ReportApi.ReportSubmission.ReportParam("a", "string"));
        params.add(new ReportApi.ReportSubmission.ReportParam("b", Boolean.TRUE));
        params.add(new ReportApi.ReportSubmission.ReportParam("c", 1));
        params.add(new ReportApi.ReportSubmission.ReportParam("d", Collections.singleton("stringset")));
        params.add(new ReportApi.ReportSubmission.ReportParam("e", Collections.singletonList("stringlist")));
        params.add(new ReportApi.ReportSubmission.ReportParam("f", Arrays.asList("stringlist1", "stringlist2", "stringlist3")));
        params.add(new ReportApi.ReportSubmission.ReportParam("g", new LinkedHashSet<String>()));
        params.add(new ReportApi.ReportSubmission.ReportParam("h", new ArrayList<String>()));
        params.add(new ReportApi.ReportSubmission.ReportParam("i", Arrays.asList((String) null, (String) null)));
        params.add(new ReportApi.ReportSubmission.ReportParam("j", Arrays.asList("", "")));
        params.add(new ReportApi.ReportSubmission.ReportParam("k", Arrays.asList(" ", "  ", "   ")));
        submission.setParameters(params);

        roundTrip(submission);
    }

    @Test
    public void testReportStatusSerialization() throws Exception {
        ReportApi.ReportStatus status = new ReportApi.ReportStatus();
        status.setId("asdf");
        status.setStatus(ReportApi.ReportStatus.Status.COMPLETED);
        status.setTime(System.currentTimeMillis());

        roundTrip(status);
    }

    @Test
    public void testReportResultSerialization() throws Exception {
        ReportApi.ReportResult result = new ReportApi.ReportResult();
        result.setId(UUID.randomUUID().toString());
        result.setType(ReportApi.ReportOutputType.PDF);
        result.setData(new DataHandler(new ByteArrayDataSource(new byte[32], "application/octet-stream")));

        roundTrip(result);
    }

    @Test
    public void testGroupingKeySerialization() throws Exception {
        ReportApi.GroupingKey groupingKey = new ReportApi.GroupingKey();
        groupingKey.setType(ReportApi.GroupingKey.GroupingKeyType.CUSTOM);
        groupingKey.setName("ADSF");

        roundTrip(groupingKey);
    }

    private Object roundTrip(Object object) throws Exception {
        StringWriter out = new StringWriter();
        JAXBContext context = JAXBContext.newInstance(
                ReportApi.ReportSubmission.class,
                ReportApi.ReportType.class,
                ReportApi.ReportStatus.class,
                ReportApi.ReportResult.class,
                ReportApi.ReportOutputType.class,
                ReportApi.GroupingKey.class
        );
        Marshaller marshaller = context.createMarshaller();
        marshaller.setEventHandler(new ValidationEventHandler() {
            @Override
            public boolean handleEvent(final ValidationEvent event) {
                System.out.println(event);
                return false;
            }
        });
        marshaller.setListener(new Marshaller.Listener() {
            @Override
            public void beforeMarshal(Object source) {
                System.out.println("Marshalling: " + source);
            }
        });
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.marshal(object, out);
        System.out.println(out);

        Unmarshaller unmarshaller = context.createUnmarshaller();
        Object objectRedux = unmarshaller.unmarshal(new StringReader(out.toString()));

        Assert.assertTrue("Roundtrip JAXB equality failed.", object.equals(objectRedux));

        return objectRedux;
    }

    //Tests for FilterPair

    @Test
    public void testFilterPairEmpty() {
        ReportApi.FilterPair fp = new ReportApi.FilterPair();
        Assert.assertTrue("Filter pair should be empty", fp.isConstraintNotRequired());

        fp = new ReportApi.FilterPair("*");
        Assert.assertTrue("Filter pair with only '*' should be empty", fp.isConstraintNotRequired());
    }


    /**
     * Test the most basic operation of the FilterPair. It gives back what it was given
     */
    @Test
    public void testFilterPairNoWildcard() {

        String filterValue = "NothingSpecial";
        ReportApi.FilterPair fp = new ReportApi.FilterPair(filterValue);
        Assert.assertFalse("Filter pair should require equals and not like", fp.isQueryUsingWildCard());

        Assert.assertEquals("Filter value should equal: '" + filterValue + "' it was: '" + fp.getFilterValue() + "'",
                filterValue, fp.getFilterValue());

    }

    /**
     * Test that any '*' characters are replaced with the '%' character.
     * Also tests that when ever the wild card is used, FilterPair.isUseEquals() returns false, as like must
     * be used in the sql query
     */
    @Test
    public void testFilterPairWildCard() {
        String filterValue = "Normal*Wildcard";
        ReportApi.FilterPair fp = new ReportApi.FilterPair(filterValue);
        Assert.assertTrue("Filter pair should require like and not equals", fp.isQueryUsingWildCard());

        String actualValue = fp.getFilterValue();
        String expectedValue = "Normal%Wildcard";
        Assert.assertEquals("Filter value should equal: '" + expectedValue + "' it was: '" + actualValue + "'", expectedValue, actualValue);

    }

    /**
     * Tests that when a wild card is used, the character ''' is escaped and '_' and '%' are also escaped as otherwise
     * they will become part of the like matching rules
     */
    @Test
    public void testFilterPairWildCardAndEscapeValues() {

        //Test that '_' is escaped when a wild card is used
        String filterValue = "Wildcard*With_UnderScore";
        ReportApi.FilterPair fp = new ReportApi.FilterPair(filterValue);

        String actualValue = fp.getFilterValue();
        String expectedValue = "Wildcard%With\\_UnderScore";

        Assert.assertEquals("Filter value should equal: '" + expectedValue + "' it was: '" + actualValue + "'", expectedValue, actualValue);

        //Test that ''' is escaped when a wild card is used
        filterValue = "Wildcard*With\'SingleQuote";
        fp = new ReportApi.FilterPair(filterValue);

        //Test that '%' is escaped when a wild card is used
        filterValue = "Wildcard*With%Percentage";
        fp = new ReportApi.FilterPair(filterValue);

        expectedValue = "Wildcard%With\\%Percentage";
        actualValue = fp.getFilterValue();
        Assert.assertEquals("Filter value should equal: '" + expectedValue + "' it was: '" + actualValue + "'", expectedValue, actualValue);

        filterValue = "User_with_un*er_scores% [Internal Identity Provider]";
        fp = new ReportApi.FilterPair(filterValue);

        expectedValue = "User\\_with\\_un%er\\_scores\\% [Internal Identity Provider]";
        actualValue = fp.getFilterValue();
        Assert.assertEquals("Filter value should equal: '" + expectedValue + "' it was: '" + actualValue + "'", expectedValue, actualValue);
    }

    /**
     * Values should not be escaped when a wild card is not needed. The ''' character should always be escaped
     */
    @Test
    public void testFilterPairEscapeWithNoWildCard() {
        String filterValue = "NoWildcardWith'SingleQuote";
        ReportApi.FilterPair fp = new ReportApi.FilterPair(filterValue);

        filterValue = "NoWildcardWith_UnderScore";
        fp = new ReportApi.FilterPair(filterValue);

        String actualValue = fp.getFilterValue();
        String expectedValue = filterValue;

        Assert.assertEquals("Filter value should equal: '" + expectedValue + "' it was: '" + actualValue + "'", expectedValue, actualValue);

        //Test that '%' is escaped when a wild card is used
        filterValue = "NoWildcardWith%Percentage";
        fp = new ReportApi.FilterPair(filterValue);

        expectedValue = filterValue;
        actualValue = fp.getFilterValue();
        Assert.assertEquals("Filter value should equal: '" + expectedValue + "' it was: '" + actualValue + "'", expectedValue, actualValue);

    }

    /**
     * Interval ps reports are fed database values from the master report. These values are enscapsulated in a FilterPair
     * object and are used in the Utilities functions for generating sql values.
     * This test tests that these values are correctly escaped, as the database may contain characters which are not
     * valid in a sql query expression
     */
    @Test
    public void testFilterPairsLiteralDatabaseValues() {

        String filterValue = "'admin_user/%=like'1'*'";
        ReportApi.FilterPair fp = new ReportApi.FilterPair(filterValue, true);

        String expectedValue = "'admin_user/%=like'1'*'";
        String actualValue = fp.getFilterValue();

        Assert.assertEquals("Filter value should equal: '" + expectedValue + "' it was: '" + actualValue + "'", expectedValue, actualValue);
    }

}
