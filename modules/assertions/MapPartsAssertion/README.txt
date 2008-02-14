This is the MapPartsAssertion.  It copies Content-IDs from a multipart message into a variable for the use
of custom assertions, since there is currently no other way for a custom assertion to access this information.

To install it, copy it into the /ssg/modules/assertions directory on the Gateway, wait five seconds, and then
reconnect with the Manager.  A new assertion "Map Part IDs" should show up in the "Service Availability" folder
of the assertion palette.

This MapPartsAssertion has no configuration options, and at runtime will always returns success.  It sets the
context variable request.parts.contentIds to an array of String each containing the content ID of one of the
message parts.  The array is never empty but may contain null elements wherever the corresponding part did not
declare a content ID.

If the request is not multipart/related the array will contain a single null element.

For a multipart/related request, there will be one element per message part, in the order the parts come in.
This corresponds to the array size and order of the Custom Assertion context variable "messageParts" and serves
to fill in the missing bit of information about their content IDs.  Element zero of the array is the content ID
of the SOAP part.
