This module contains the SplitAssertion and the JoinAssertion.  The SplitAssertion splits a single-valued
context variable into multiple values, and the JoinAssertion combines a multi-valued variable into a single value.

To install the module, copy it into the /ssg/modules/assertions directory on the Gateway, wait five seconds, and then
reconnect with the Manager.  Two new assertions "Strip Parts" and "Join Parts" should show up in the "Policy Logic"
folder of the assertion palette.

Both assertions have three configuration options, all strings: an input variable name, an output variable name,
and a delimiter.

For the SplitAssertion, the delimiter is expected to be formatted as a regular expression that can be passed to
Pattern.compile().  The assertion will find the value of the input variable, split it as with Pattern.split(),
and store the result in the output variable as a List<String>.

The SplitAssertion succeeds if it stores a result in the output variable, even if the resulting list is empty.
If the input variable does not exist, it fails with the assertion status FAILED (601) and audits a message to this
effect at the WARNING level.  If the delimiter is not a valid regular expression, the assertion will always fail
with the assertion status SERVER_ERROR (500) and audit a message at the WARNING level.

For the JoinAssertion, the delimiter is a free form String.  The assertion will find the values from the input variable,
concatenate them together with the delimiter between each adjacent pair, and store the resulting string in the
output variable.  The input variable must exist and its value must be assignable to either Collection or Object[].

The JoinAssertion succeeds if it stores a result in the output variable.  If the input variable does not exist,
or its value is not null and is not assignable to either Object[] or Collection, the assertion fails with the
assertion status FAILED and audits a WARNING.  If the input value is null the assertion emits an empty collection
as its result and returns success.
