PATCH INFO

This patch fixes the JSONArray to XML transformation.  This patch is made against https://github.com/douglascrockford/JSON-java/commit/1b5a59f4285e798da1a77a9282351732c37994af
git commit # 1b5a59f4285e798da1a77a9282351732c37994af

using "diff -rupN douglascrockford-JSON-java-3e3951f l7p1 > json-java-1.0-l7p1.patch"

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

As of patch 2;
When transforming XML to JSON.  The library will attempt to convert numeric values into its corresponding type class and the algorithm to do this is rather buggy;
03 is converted to an Integer of 3, which is fine.
003 is converted to a String of 003, which is not fine as it should drop the leading 0.
the existing code only check the first character or the first two character if the first is a minus sign.

based on the client's requirement, they like to preserve the leading 0, as such, we will be removing the numeric value conversion bit.

IMPORTANT NOTE:  The ServerJsonTransformationAssertion currently (as of Icefish) uses the -l7p2 jar but it currently bypasses the org.json.XML
class in favor of its own private modified version CustomizedJsonXml which fixes a few more bugs (as noted in its header) and makes the
number output behavior configurable.