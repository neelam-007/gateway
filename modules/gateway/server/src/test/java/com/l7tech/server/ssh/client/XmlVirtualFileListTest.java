package com.l7tech.server.ssh.client;

import org.junit.Assert;
import org.junit.Test;

import javax.xml.bind.JAXB;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;

/**
 * This was created: 1/30/13 as 10:22 AM
 *
 * @author Victor Kazakov
 */
public class XmlVirtualFileListTest {

    @Test
    public void unmarshallXMLFileList(){
        XmlVirtualFileList rtn = JAXB.unmarshal(new StringReader("<files>\n" +
                "\t<file name=\"file1.txt\" size=\"123\" lastModified=\"123456\" file=\"true\" />\n" +
                "\t<file name=\"file2.txt\" size=\"567\" lastModified=\"678980\" file=\"true\" />\n" +
                "</files> "), XmlVirtualFileList.class);

        Assert.assertNotNull(rtn.getFileList());
        Assert.assertEquals("file1.txt", rtn.getFileList().get(0).getName());
        Assert.assertEquals("file2.txt", rtn.getFileList().get(1).getName());
        Assert.assertEquals(123, rtn.getFileList().get(0).getSize().intValue());
        Assert.assertEquals(678980L, rtn.getFileList().get(1).getLastModified().longValue());
        Assert.assertEquals(true, rtn.getFileList().get(0).isFile());
    }

    @Test
    public void marshallXMLFileList(){

        XmlSshFile[] xmlSshFile = new XmlSshFile[]{
                new XmlSshFile("file1.txt", true, 123L, 123456),
                new XmlSshFile("file2.txt", true, 222L, 678980),
                new XmlSshFile("myDirectory", false)
        };
        XmlVirtualFileList xmlVirtualFileList = new XmlVirtualFileList(Arrays.asList(xmlSshFile));

        StringWriter writer = new StringWriter();
        JAXB.marshal(xmlVirtualFileList, writer);
        String xml = writer.toString();

        Assert.assertNotNull(xml);

        XmlVirtualFileList rtn = JAXB.unmarshal(new StringReader(xml), XmlVirtualFileList.class);
        Assert.assertNotNull(rtn.getFileList());
        Assert.assertEquals("file1.txt", rtn.getFileList().get(0).getName());
        Assert.assertEquals("file2.txt", rtn.getFileList().get(1).getName());
        Assert.assertEquals(123, rtn.getFileList().get(0).getSize().intValue());
        Assert.assertEquals(678980L, rtn.getFileList().get(1).getLastModified().longValue());
        Assert.assertEquals(true, rtn.getFileList().get(0).isFile());
        Assert.assertEquals("myDirectory", rtn.getFileList().get(2).getName());
        Assert.assertEquals(false, rtn.getFileList().get(2).isFile());
    }
}
