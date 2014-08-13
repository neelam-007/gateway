This version of Xalan 2.7.2 has been patched (XSLTElementProcessor.patch) to:

* Permit namespace declaration attributes in secure processing mode
* Permit foreign attributes in secure processing mode if not from a list of special namespaces
* Permit secure processing foreign attribute restrictions to be disabled completely with a system property


Permit namespace declaration attributes in secure processing mode
================================================================= 

Namespace declarations bypass foreign attribute restrictions.

Foreign attributes will be allowed if they are global attributes in one of the following namespace URIs:
   http://www.w3.org/XML/1998/namespace
   http://www.w3.org/2000/xmlns/

A preexisting hack (commented as "for Crimson.  -sb") sets the first of these namespaces if the attribute
qname is "xmlns" or starts with "xmlns:".

This is required because otherwise stylesheets input via DOMSource will cause namespace declaration attributes
to hit the attribute processor and fail the foreign attributes check.  (A work-around is to use StreamSource
instead.)



Permit foreign attributes in secure processing mode if not from a list of special namespaces
============================================================================================

Element literal results will bypass the foreign attribute check if they avoid possibly-problematic namespaces.

Foreign attributes will be permitted in secure processing mode as long as the attribute is not a global
attribute in one of the following namespace URIs:

   http://xml.apache.org/xalan
   http://xml.apache.org/xslt
   http://icl.com/saxon
   http://www.w3.org/1999/XSL/Transform

and the element containing the attribute is an element literal result and is not in one of the above namespaces.

This is required because otherwise it would not be possible to (for example) have a stylesheet that emits
an XHTML output result (because Xalan would have rejected any attributes on any XHTML elements).


Permit secure processing foreign attribute restrictions to be disabled completely with a system property
========================================================================================================

The system property "com.l7tech.org.apache.xalan.processor.allowAttributesInSecureMode" can be set to
"true" to disable the foreign attribute restrictions in secure mode that were added in Xalan 2.7.2.

This will hopefully never be required, but is available in case it is required.

Enabling this may permit insecure use of the content-handler and entities attributes and should be avoided except
as a last resort on systems that do not need to execute stylesheets from untrusted sources.
