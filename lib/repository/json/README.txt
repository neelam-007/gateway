PATCH INFO

This patch fixes the JSONArray to XML transformation.  This patch is made against https://github.com/douglascrockford/JSON-java/commit/90e62b0e1bd8be118f40a9bd0e660f44e224ed47
git commit # 90e62b0e1bd8be118f40a9bd0e660f44e224ed47

using "diff -crB douglascrockford-JSON-java-3e3951f l7p1 > json-java-1.0-l7p1.patch"

Currently;
[0, 1, 2] is emitted as <array>0</array><array>1</array><array>2</array>
if we provide a custom tag name, it will become <customRootTag>0</customRootTag><customRootTag>1</customRootTag><customRootTag>2</customRootTag>
which is inconsistent to JSONObject (below) and will never produce a well-formed xml.

<customRootTag>
    <foo>bar</foo>
</customRootTag>

The patch will make it so that [0, 1, 2] will be serialized as;
<array>
    <value>0</value>
    <value>1</value>
    <value>2</value>
</array>
if a root tag name is not provided.
If a root tag name is provided, the result below is produced.
<myTagName>
    <value>0</value>
    <value>1</value>
    <value>2</value>
</myTagName>