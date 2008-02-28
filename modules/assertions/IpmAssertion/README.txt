This is the IpmAssertion.  It expands an IPM data buffer into an XML document using a provided template.

To install it, copy it into the /ssg/modules/assertions directory on the Gateway, wait five seconds, and then
reconnect with the Manager.  A new assertion "IPM To XML" should show up in the "Service Availability" folder
of the assertion palette.

This assertion has three configuration options: a template, a source variable name, and a destination variable name.

The template controls how the IPM data buffer is to be converted into XML.  It is a literal IPM template in a format
similar to the following:

    <PREMIER-ACCESS-QUERY-REPLY>
        <PACQUERY-ORDER-NBR type="9(9)"/>
        <PACQUERY-TIE-NUM type="9(3)"/>
        <PACQUERY-CONTRACT-STATUS type="X(5)"/>
        <PACQUERY-SYSTEM-TYPE type="X(5)"/>
        <PACQUERY-ORDER-SKU-INFO-NBR type="9(3)"/>
        <PACQUERY-ORDER-SKU-INFO occurs="200">
            <PACQUERY-DETAIL-SEQ-NBR type="9(3)"/>
            <PACQUERY-SKU-NBR type="X(13)"/>
            <PACQUERY-SKU-DESC type="X(40)"/>
            <PACQUERY-SKU-QTY type="9(7)"/>
            <PACQUERY-SKU-QTY-SIGN type="X(1)"/>
        </PACQUERY-ORDER-SKU-INFO>
        <PACQUERY-MORE-ORDER-SKU-INFO type="X(1)"/>
    </PREMIER-ACCESS-QUERY-REPLY>

The source variable name is the name of a context variable which is expected to already contain the IPM data buffer,
perhaps extracted from a message using an XPath assertion.

The target variable name is the name of a context variable in which to save the resulting expanded XML.

At runtime this assertion returns success if it is able to expand the input into the output variable.  If the
template is invalid, it audits an error message and fails with the assertion status SERVER_ERROR (500).  If the
source variable does not exist, it audits an error message fails with the assertion status FAILED (501).  Finally,
it can also fail if the input data buffer is too short for the current template, or if the currently configured
output buffer size is not big enough to expand this request.
