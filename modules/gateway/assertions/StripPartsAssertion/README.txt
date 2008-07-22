This is the StripPartsAssertion.  It converts a multipart/related message into a single-part message by throwing
away the multipart envelope and all parts except for the first one.

To install it, copy it into the /ssg/modules/assertions directory on the Gateway, wait five seconds, and then
reconnect with the Manager.  A new assertion "Strip Parts" should show up in the "Service Availability" folder
of the assertion palette.

The assertion has a single configuration option, actOnRequest, which is a boolean that controls whether
to strip the request or the reply.  If actOnRequest is true, the assertion operates on the request; otherwise,
it operates on the reply.

The assertion will always succeed unless there is an IO error while reading the request or reply, or while spooling,
unspooling, or deleting cache files in the case of exceptionally large XML parts and attachments.  If there is
an IO error the assertion will fail with the assertion status FAILED (601).