Overview
========
This is the IpmAssertion.  It expands an IPM data buffer into an XML document using a provided template.


Installation
============
To install it, copy it into the /ssg/modules/assertions directory on the Gateway, wait five seconds, and then
reconnect with the Manager.  A new assertion "IPM To XML" should show up in the "Service Availability" folder
of the assertion palette.


Configuration
=============
This assertion has three configuration options: some literal IPM template XML, a source variable name,
and a destination variable or message.

The template controls how the IPM data buffer is to be converted into XML.  It is a literal IPM template in a format
similar to the following:

  <soap:Envelope xmlns:soap="http://schemas.xmlsoap.org/soap/envelope/">
      <soap:Body>
          <PREMIER-ACCESS-QUERY-REPLY>
              <PACQUERY-ORDER-NBR pic="9(9)"/>
              <PACQUERY-TIE-NUM pic="9(3)"/>
              <PACQUERY-CONTRACT-STATUS pic="X(5)"/>
              <PACQUERY-SYSTEM-TYPE pic="X(5)"/>
              <PACQUERY-ORDER-SKU-INFO-NBR pic="9(3)"/>
              <PACQUERY-ORDER-SKU-INFO occurs="200">
                  <PACQUERY-DETAIL-SEQ-NBR pic="9(3)"/>
                  <PACQUERY-SKU-NBR pic="X(13)"/>
                  <PACQUERY-SKU-DESC pic="X(40)"/>
                  <PACQUERY-SKU-QTY pic="9(7)"/>
                  <PACQUERY-SKU-QTY-SIGN pic="X(1)"/>
              </PACQUERY-ORDER-SKU-INFO>
              <PACQUERY-MORE-ORDER-SKU-INFO pic="X(1)"/>
          </PREMIER-ACCESS-QUERY-REPLY>
      </soap:Body>
  </soap:Envelope>

The source variable name is the name of a context variable which is expected to already contain the IPM data buffer,
perhaps extracted from a message using an XPath assertion.

The target variable name is the name of a context variable in which to save the resulting expanded XML, or else
an indication that the XML should expanded directly into the request or response message.

Elements in the template that do not contain "pic" or "occurs" attributes will be copied to the result unchanged. 


Error reporting
===============
At runtime this assertion returns success if it is able to expand the input into the output variable.  If the
template is invalid, it audits an error message and fails with the assertion status SERVER_ERROR (500).  If the
source variable does not exist, it audits an error message fails with the assertion status FAILED (501).  Finally,
it can also fail if the input data buffer is too short for the current template, or if the currently configured
output buffer size is not big enough to expand this request.


Buffers and memory usage
========================
The IPM output buffer size is configured by the cluster property "ipm.outputBuffer".  It defaults to 131071 characters.
Changes to this value will take effect within 120 seconds.  The maximum number of output buffers is controlled by
the cluster property "ipm.maxBuffers".  This limits the number of concurrent requests that can perform the same
type of IPM expansion concurrently.  (Expansion directly into a message does not use the same buffers as expansion
into a variable.)  It is possible to further reduce memory usage when expanding directly into a message by setting
the cluster property "ipm.sharedByteBuffers" to "true".
