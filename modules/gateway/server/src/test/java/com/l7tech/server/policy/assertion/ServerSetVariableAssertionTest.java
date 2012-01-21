package com.l7tech.server.policy.assertion;

import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.gateway.common.audit.Audit;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.PolicyAssertionException;
import com.l7tech.policy.assertion.SetVariableAssertion;
import com.l7tech.policy.variable.DataType;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.w3c.dom.Document;

import java.io.InputStream;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: ymoiseyenko
 * Date: 12/20/11
 * Time: 10:44 AM
 */
@RunWith(MockitoJUnitRunner.class)
public class ServerSetVariableAssertionTest {

    private static final String DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    private static final String VARIABLE_VALUE = "this is a message content";
    private static final String XML_MESSAGE_CONTENT = "<text>" + VARIABLE_VALUE + "</text>";
    public static final String VARIABLE_NAME = "var";

    ServerSetVariableAssertion fixture;
    @Mock
    PolicyEnforcementContext mockContext;

    Map<String, Object> varMap = new HashMap<String, Object>();

    @Before
    public void setUp() {
       when(mockContext.getVariableMap(Matchers.<String[]>any(), any(Audit.class))).thenReturn(varMap);
    }

    @Test
    public void shouldSetStringVariable() throws Exception {
        SetVariableAssertion assertion = new SetVariableAssertion(VARIABLE_NAME, VARIABLE_VALUE);
        fixture = new ServerSetVariableAssertion(assertion);
        assertEquals(AssertionStatus.NONE, fixture.checkRequest(mockContext));
        verify(mockContext, times(1)).setVariable(VARIABLE_NAME, VARIABLE_VALUE);
    }

    @Test
    public void shouldSetDateTimeVariable() throws Exception {
        Calendar cal = GregorianCalendar.getInstance(TimeZone.getTimeZone("PST"));
        cal.set(2011,11,20,11,31,43);
        cal.set(Calendar.MILLISECOND, 98);
        Date expected = cal.getTime();
        SetVariableAssertion assertion = new SetVariableAssertion();
        assertion.setVariableToSet(VARIABLE_NAME);
        assertion.setDataType(DataType.DATE_TIME);
        assertion.setFormat(DATE_TIME_FORMAT);
        assertion.setExpression("2011-12-20T11:31:43.098-08:00");
        fixture = new ServerSetVariableAssertion(assertion);
        assertEquals(AssertionStatus.NONE, fixture.checkRequest(mockContext));
        verify(mockContext, times(1)).setVariable(assertion.getVariableToSet(), expected);
    }

    @Test
    public void shouldSetDateTimeVariableIfExpressionIsDateTimeType() throws Exception {
        Calendar cal = GregorianCalendar.getInstance(TimeZone.getTimeZone("PST"));
        cal.set(2011,11,20,11,31,43);
        cal.set(Calendar.MILLISECOND, 98);
        Date expected = cal.getTime();
        SetVariableAssertion assertion = new SetVariableAssertion();
        assertion.setDataType(DataType.DATE_TIME);
        assertion.setVariableToSet(VARIABLE_NAME);
        assertion.setExpression("${date1}");
        varMap.put("date1", expected);
        fixture = new ServerSetVariableAssertion(assertion);
        assertEquals(AssertionStatus.NONE,fixture.checkRequest(mockContext));
        verify(mockContext).setVariable(VARIABLE_NAME, expected);
    }

    @Test
    public void shouldThrowExceptionWhenDateTimeValueIsIncorrect() throws Exception{
        SetVariableAssertion assertion = new SetVariableAssertion();
        assertion.setFormat("yyyy-MM-dd");
        assertion.setVariableToSet(VARIABLE_NAME);
        assertion.setDataType(DataType.DATE_TIME);
        assertion.setExpression("adsfhas;dlrkhyjsaewryh");
        fixture = new ServerSetVariableAssertion(assertion);
        assertEquals(AssertionStatus.FALSIFIED, fixture.checkRequest(mockContext));
        verify(mockContext, never()).setVariable(anyString(), any());
    }


    @Test
    public void shouldThrowExceptionWhenExpressionIsMultiValueString() throws Exception {
        SetVariableAssertion assertion = new SetVariableAssertion();
        List<Object> expectedVar = new ArrayList<Object>();
        Calendar cal = GregorianCalendar.getInstance(TimeZone.getTimeZone("PST"));
        cal.set(2011,11,20,11,31,43);
        cal.set(Calendar.MILLISECOND, 98);
        expectedVar.add( new Date(cal.getTimeInMillis()));
        cal.set(2011,11,25, 12, 0, 0);
        cal.set(Calendar.MILLISECOND,0);
        expectedVar.add(new Date(cal.getTimeInMillis()));
        assertion.setVariableToSet(VARIABLE_NAME);
        assertion.setDataType(DataType.DATE_TIME);
        assertion.setFormat(DATE_TIME_FORMAT);
        String[] expression = {"2011-12-20T11:31:43.098-08:00","2011-12-25T12:00:00.000-0800"};
        assertion.setExpression("${date1}");
        varMap.put("date1", expectedVar);

        fixture = new ServerSetVariableAssertion(assertion);
        assertEquals(AssertionStatus.FALSIFIED, fixture.checkRequest(mockContext));

        verify(mockContext, never()).setVariable(VARIABLE_NAME, Arrays.asList(expression));
    }

    @Test
    public void shouldSetMessageVariable() throws Exception {
        SetVariableAssertion assertion = new SetVariableAssertion();
        assertion.setDataType(DataType.MESSAGE);
        assertion.setVariableToSet(VARIABLE_NAME);
        assertion.setContentType("text/xml");
        assertion.setExpression(XML_MESSAGE_CONTENT);
        fixture = new ServerSetVariableAssertion(assertion);
        PolicyEnforcementContext fakeContext = makeFakeContext();

        assertEquals(AssertionStatus.NONE, fixture.checkRequest(fakeContext));
        ContentTypeHeader contentType = ContentTypeHeader.parseValue(assertion.getContentType());
        Message actualMessage = (Message)fakeContext.getVariable(VARIABLE_NAME);
        assertTrue(actualMessage.isXml());
        Document doc = actualMessage.getXmlKnob().getDocumentReadOnly();
        String actualContent = doc.getDocumentElement().getChildNodes().item(0).getTextContent();
        assertEquals(VARIABLE_VALUE, actualContent);

    }


     private static PolicyEnforcementContext makeFakeContext() {
        Message request = new Message();
        Message response = new Message();
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(request, response);
    }
}
