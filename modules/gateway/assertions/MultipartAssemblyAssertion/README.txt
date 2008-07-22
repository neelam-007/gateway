This is the MultipartAssemblyAssertion.  It wraps a message in a new multipart/related envelope and adds
zero or more file attachments whose details are provided in context variables.

To install it, copy it into the /ssg/modules/assertions directory on the Gateway, wait five seconds, and then
reconnect with the Manager.  A new assertion "Multipart Assembly" should show up in the "Service Availability" folder
of the assertion palette.

The assertion has two configuration options: actOnRequest, a boolean; and variablePrefix, a String.

The actOnRequest option is a boolean that controls whether to operate on the request or the reply.  If actOnRequest
is true, the assertion operates on the request; otherwise, it operates on the reply.

The variablePrefix option is a String that is used as a prefix for the names of the context variables holding
the part bodies, content types, and content IDs.  The default value is "multipartAssembly", and hence the default
context variables are as follows:

  multipartAssembly.payloads      an array or Collection of part bodies, in one of the formats listed below
  multipartAssembly.contentTypes  an array or Collection of Strings containing content type values
  multipartAssembly.partIds       an array or Collection of Strings containing content ID values

All three variables must exist and the values must be non-null and assignable to either Object[] or Collection.
All three must be the same size.  If no attachments are to be added, all three must still be provided but
should be empty.

If a required variable is not found, or its value is null or not assignable to either Object[] or Collection,
a message will be logged and audited at the WARNING level and the assertion will fail with status FAILED (601).

Within the payloads array or Collection, each element must be one of the formats listed below.  It is suggested,
but not required, that all elements be in the same format.

  byte[]         A byte array.  This must contain all the bytes of the attachment.

  CharSequence   A String, StringBuffer, or StringBuilder.  This must contain all the characters of the attachment.
                 The multipart envelope will use the encoding specified by the corresponding Content-Type of this
                 attachment when converting the characters to bytes; if the Content-Type does not specify an encoding,
                 UTF-8 will be used.

  ByteBuffer     A ByteBuffer.  This must contain all the bytes of the attachment.

  InputStream    An InputStream.  When read, this must produce all the bytes of the attachment and then signal EOF.

If a payload body is null, or is not in one of the above formats, a message will be logged and audited at the WARNING
level and the assertion will fail with the assertion status FAILED.

The assertion will succeed if it is able to successfully transform the message into a multipart envelope.  If an IO
error occurs while reading a message, or while spooling or unspooling cache files in the case of an exceptionally
large XML part or attachment, a message will be logged and audited at the WARNING level and the assertion will fail
with the assertion status FAILED.

The first part of the new multipart/related message will be the entire previous message.  If the previous message
was already multipart/related, there will be a nested multipart message inside the first part of the output message.
To avoid this, you can precede the MultipartAssemblyAssertion with a StripPartsAssertion.

There is no way to specify a Content-ID for the first part of the new multipart envelope.  A random Content-ID
is always chosen for the new first part.
