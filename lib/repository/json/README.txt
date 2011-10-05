PATCH INFO

JSONML.java
* when converting XML to JSONML format, if we pass it a non-xml content, it will go into an infinite loop
created using:  diff -rNU 3 --exclude=CSV /json/src/main/org/json/JSONML.java exported_source/JSONML.java > JSONML.patch

XML.java
* consistency when JSONObject or JSONArray is serialized with a custom tag name.
* created using:  diff -rNU 3 --exclude=CSV /json/src/main/org/json/XML.java exported_source/XML.java > XML.patch

Currently;
[0, 1, 2] is emitted as <array>0</array><array>1></array><array>2</array>
if we provide a custom tag name, it will become <customRootTag>0</customRootTag><customRootTag>1></customRootTag><customRootTag>2</customRootTag>
which is inconsistent to JSONObject (below)

<customRootTag>
    <foo>bar</foo>
</customRootTag>

The patch will make it so that [0, 1, 2] will be serialized as;
<customRootTag>
    <value>0</value>
    <value>1</value>
    <value>2</value>
</customRootTag>

