This version of Xalan 2.7.2 has been patched (XSLTElementProcessor.patch) to permit foreign attributes in
secure processing mode as long as the attribute is not a global attribute in one of the following namespaces:

   http://xml.apache.org/xalan
   http://xml.apache.org/xslt
   http://icl.com/saxon
   http://www.w3.org/1999/XSL/Transform

and the element containing the attribute is an element literal result and is not in one of the above namespaces.

This is required because otherwise it would not be possible to (for example) have a stylesheet that emits
an XHTML output result (because Xalan would have rejected any attributes on any XHTML elements).
