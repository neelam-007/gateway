package com.l7tech.server.policy.assertion;

import com.l7tech.gateway.common.audit.*;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.util.DateTimeConfigUtils;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.SetVariableAssertion;
import com.l7tech.policy.variable.DataType;
import com.l7tech.server.ApplicationContexts;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Pair;
import com.l7tech.util.TimeSource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@SuppressWarnings({"JavaDoc"})
@RunWith(MockitoJUnitRunner.class)
public class ServerSetVariableAssertionTest {

    @Before
    public void setUp() throws Exception {
        testAudit = new TestAudit();
        // tests can reassign as needed
        timeSource = new TimeSource();
    }

    @Test
    public void testSetStringVariable() throws Exception {
        SetVariableAssertion assertion = new SetVariableAssertion("foo", "bar");
        fixture = new ServerSetVariableAssertion(assertion);
        final AssertionStatus assertionStatus = fixture.checkRequest(mockContext);
        assertEquals(AssertionStatus.NONE, assertionStatus);
        verify(mockContext, times(1)).setVariable("foo", "bar");
    }

    @Test
    public void testSetDateVariable_ParseAuto() throws Exception {
        final String inputDate = "2012-05-07T23:12:24.567Z";
        SetVariableAssertion assertion = new SetVariableAssertion("foo", inputDate);
        assertion.setDataType(DataType.DATE_TIME);
        createServerAssertion(assertion);

        final List<Pair<String, Pattern>> pairs = new ArrayList<Pair<String, Pattern>>();
        final String format = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
        pairs.add(new Pair<String, Pattern>(format, Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}\\.\\d{3}(?:Z|(?:\\+|-)\\d{2}:\\d{2})$")));
        dateParser.setAutoDateFormats(pairs);

        final AssertionStatus assertionStatus = fixture.checkRequest(mockContext);
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertFalse(testAudit.iterator().hasNext());

        final Date expectedDate = new SimpleDateFormat(format).parse(inputDate);
        verify(mockContext, times(1)).setVariable("foo", expectedDate);
    }

    /**
     * The resolved value for the expression is an unknown format.
     */
    @Test
    public void testSetDateVariable_ParseAuto_UnknownFormat() throws Exception {
        final String inputDate = "2012-05-07T23:12:24.567Z";
        SetVariableAssertion assertion = new SetVariableAssertion("foo", inputDate);
        assertion.setDataType(DataType.DATE_TIME);
        createServerAssertion(assertion);

        final List<Pair<String, Pattern>> pairs = new ArrayList<Pair<String, Pattern>>();
        dateParser.setAutoDateFormats(pairs); // empty

        final AssertionStatus assertionStatus = fixture.checkRequest(mockContext);
        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);

        for (String s : testAudit) {
            System.out.println(s);
        }

        assertTrue(testAudit.isAuditPresent(AssertionMessages.SET_VARIABLE_UNRECOGNISED_DATE_FORMAT));
        assertTrue(testAudit.isAuditPresentContaining(
                MessageFormat.format(AssertionMessages.SET_VARIABLE_UNRECOGNISED_DATE_FORMAT.getMessage(), inputDate)));
    }

    /**
     * The specific format chosen cannot parse the evaluated expression.
     */
    @Test
    public void testSetDateVariable_ParseSpecific() throws Exception {
        final String inputDate = "2012-05-07T23:12:24.567Z";
        SetVariableAssertion assertion = new SetVariableAssertion("foo", inputDate);
        assertion.setDataType(DataType.DATE_TIME);
        final String dateFormat = "yyyy-MM-dd'T'hh:mm:ss.SSSX"; // should be HH
        assertion.setDateFormat(dateFormat);
        createServerAssertion(assertion);

        final AssertionStatus assertionStatus = fixture.checkRequest(mockContext);
        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);

        for (String s : testAudit) {
            System.out.println(s);
        }

        assertTrue(testAudit.isAuditPresent(AssertionMessages.SET_VARIABLE_UNABLE_TO_PARSE_DATE));
        assertTrue(testAudit.isAuditPresentContaining(
                MessageFormat.format(AssertionMessages.SET_VARIABLE_UNABLE_TO_PARSE_DATE.getMessage(), inputDate)));
    }

    @Test
    public void testSetDateVariable_FormatFromVariable() throws Exception {
        final String inputDate = "2012-05-07T23:12:24.567Z";
        SetVariableAssertion assertion = new SetVariableAssertion("foo", inputDate);
        assertion.setDataType(DataType.DATE_TIME);
        assertion.setDateFormat("${dateformat}");

        final HashMap<String, Object> varsUsed = new HashMap<String, Object>();
        final String formatValue = "yyyy-MM-dd'T'HH:mm:ss.SSSX";
        varsUsed.put("dateformat", formatValue);
        when(mockContext.getVariableMap(Matchers.<String[]>any(), Matchers.<Audit>any())).thenReturn(varsUsed);

        createServerAssertion(assertion);

        final AssertionStatus assertionStatus = fixture.checkRequest(mockContext);
        for (String s : testAudit) {
            System.out.println(s);
        }
        assertEquals(AssertionStatus.NONE, assertionStatus);

        final Date expectedDate = new SimpleDateFormat(formatValue).parse(inputDate);
        verify(mockContext, times(1)).setVariable("foo", expectedDate);
    }

    /**
     * The format used is a valid simple date format, however it does not match the intended input string.
     * HH is needed for 24 hours, not hh
     */
    @Test
    public void testSetDateVariable_FormatFromVariable_InvalidLogicalFormat() throws Exception {
        final String inputDate = "2012-05-07T23:12:24.567Z";
        SetVariableAssertion assertion = new SetVariableAssertion("foo", inputDate);
        assertion.setDataType(DataType.DATE_TIME);
        assertion.setDateFormat("${dateformat}");

        final HashMap<String, Object> varsUsed = new HashMap<String, Object>();
        final String formatValue = "yyyy-MM-dd'T'hh:mm:ss.SSSX"; //hh is invalid for a 24 hour string
        varsUsed.put("dateformat", formatValue);
        when(mockContext.getVariableMap(Matchers.<String[]>any(), Matchers.<Audit>any())).thenReturn(varsUsed);

        createServerAssertion(assertion);

        final AssertionStatus assertionStatus = fixture.checkRequest(mockContext);
        for (String s : testAudit) {
            System.out.println(s);
        }
        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);

        assertTrue(testAudit.isAuditPresent(AssertionMessages.SET_VARIABLE_UNABLE_TO_PARSE_DATE));
        assertTrue(testAudit.isAuditPresentContaining(
                MessageFormat.format(AssertionMessages.SET_VARIABLE_UNABLE_TO_PARSE_DATE.getMessage(), inputDate)));

    }

    @Test
    public void testSetDateVariable_FormatFromVariable_InvalidFormat() throws Exception {
        final String inputDate = "2012-05-07T23:12:24.567Z";
        SetVariableAssertion assertion = new SetVariableAssertion("foo", inputDate);
        assertion.setDataType(DataType.DATE_TIME);
        assertion.setDateFormat("${dateformat}");

        final HashMap<String, Object> varsUsed = new HashMap<String, Object>();
        final String formatValue = "yyyy-MM-ddThh:mm:ss.SSSX"; //hh is invalid for a 24 hour string
        varsUsed.put("dateformat", formatValue);
        when(mockContext.getVariableMap(Matchers.<String[]>any(), Matchers.<Audit>any())).thenReturn(varsUsed);

        createServerAssertion(assertion);

        final AssertionStatus assertionStatus = fixture.checkRequest(mockContext);
        for (String s : testAudit) {
            System.out.println(s);
        }
        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);

        assertTrue(testAudit.isAuditPresent(AssertionMessages.SET_VARIABLE_INVALID_DATE_PATTERN));
        assertTrue(testAudit.isAuditPresentContaining("'T'"));
    }

    @Test
    public void testDateTime_Auto() throws Exception {
        final long currentTime = System.currentTimeMillis();
        timeSource = new TimeSource(){
            @Override
            public long currentTimeMillis() {
                return currentTime;
            }
        };
        final SetVariableAssertion assertion = new SetVariableAssertion("mydate", "");
        assertion.setDataType(DataType.DATE_TIME);
        createServerAssertion(assertion);

        final AssertionStatus assertionStatus = fixture.checkRequest(mockContext);
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertFalse(testAudit.iterator().hasNext());

        verify(mockContext, times(1)).setVariable("mydate", new Date(currentTime));
    }

    @Test
    public void testDateTime_PositiveOffset() throws Exception {
        final long currentTime = System.currentTimeMillis();
        timeSource = new TimeSource(){
            @Override
            public long currentTimeMillis() {
                return currentTime;
            }
        };

        final SetVariableAssertion assertion = new SetVariableAssertion("mydate", "");
        assertion.setDateOffsetExpression("100"); // defaults to seconds
        assertion.setDataType(DataType.DATE_TIME);
        createServerAssertion(assertion);

        final AssertionStatus assertionStatus = fixture.checkRequest(mockContext);
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertFalse(testAudit.iterator().hasNext());

        final Calendar expectedCal = Calendar.getInstance();
        expectedCal.setTimeInMillis(currentTime);
        expectedCal.add(Calendar.SECOND, 100);
        verify(mockContext, times(1)).setVariable("mydate", expectedCal.getTime());
    }

    @Test
    public void testDateTime_NegativeOffset_FromVariable() throws Exception {
        final String dateExpression = "2012-05-07T23:12:24.567Z";

        final Date expectedDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX").parse(dateExpression);

        timeSource = new TimeSource(){
            @Override
            public long currentTimeMillis() {
                return expectedDate.getTime();
            }
        };

        final SetVariableAssertion assertion = new SetVariableAssertion("mydate", dateExpression);
        assertion.setDataType(DataType.DATE_TIME);
        assertion.setDateOffsetExpression("${offset}");
        assertion.setDateOffsetField(Calendar.MILLISECOND);
        assertion.setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");

        createServerAssertion(assertion);

        final HashMap<String, Object> varsUsed = new HashMap<String, Object>();
        varsUsed.put("offset", "-5000");
        when(mockContext.getVariableMap(Matchers.<String[]>any(), Matchers.<Audit>any())).thenReturn(varsUsed);
        final AssertionStatus assertionStatus = fixture.checkRequest(mockContext);
        assertEquals(AssertionStatus.NONE, assertionStatus);
        assertFalse(testAudit.iterator().hasNext());

        final Calendar expectedCal = Calendar.getInstance();
        expectedCal.setTime(expectedDate);
        expectedCal.add(Calendar.MILLISECOND, -5000);
        verify(mockContext, times(1)).setVariable("mydate", expectedCal.getTime());
    }

    @Test
    public void testDateTime_InvalidOffset() throws Exception {
        final long currentTime = System.currentTimeMillis();
        timeSource = new TimeSource(){
            @Override
            public long currentTimeMillis() {
                return currentTime;
            }
        };

        final SetVariableAssertion assertion = new SetVariableAssertion("mydate", "");
        final String dateOffsetExpression = "one hundred";
        assertion.setDateOffsetExpression(dateOffsetExpression);
        assertion.setDataType(DataType.DATE_TIME);
        createServerAssertion(assertion);

        final AssertionStatus assertionStatus = fixture.checkRequest(mockContext);
        assertEquals(AssertionStatus.FALSIFIED, assertionStatus);

        for (String s : testAudit) {
            System.out.println(s);
        }

        assertTrue(testAudit.isAuditPresent(AssertionMessages.SET_VARIABLE_INVALID_DATE_OFFSET));
        assertTrue(testAudit.isAuditPresentContaining(
                MessageFormat.format(AssertionMessages.SET_VARIABLE_INVALID_DATE_OFFSET.getMessage(), dateOffsetExpression)));
    }

    @Test
    public void testDateTime_NoOffset() throws Exception {
        final long currentTime = System.currentTimeMillis();
        timeSource = new TimeSource(){
            @Override
            public long currentTimeMillis() {
                return currentTime;
            }
        };

        final SetVariableAssertion assertion = new SetVariableAssertion("mydate", "");
        assertion.setDateOffsetExpression(null); // null by default
        assertion.setDataType(DataType.DATE_TIME);
        createServerAssertion(assertion);

        final AssertionStatus assertionStatus = fixture.checkRequest(mockContext);
        assertEquals(AssertionStatus.NONE, assertionStatus);

        for (String s : testAudit) {
            System.out.println(s);
        }
    }

    // - PRIVATE

    @Mock
    private PolicyEnforcementContext mockContext;
    private final DateTimeConfigUtils dateParser = new DateTimeConfigUtils();

    private ServerSetVariableAssertion fixture;
    private TestAudit testAudit;
    private TimeSource timeSource;

    private void createServerAssertion(SetVariableAssertion assertion) throws PolicyAssertionException {
        fixture = new ServerSetVariableAssertion(assertion);

        ApplicationContexts.inject(fixture, CollectionUtils.<String, Object>mapBuilder()
                .put("dateParser", dateParser)
                .put("auditFactory", testAudit.factory())
                .put("timeSource", timeSource)
                .unmodifiableMap()
        );
    }

}
