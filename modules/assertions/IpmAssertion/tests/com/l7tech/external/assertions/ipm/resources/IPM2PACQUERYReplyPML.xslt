<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
<xsl:template match="/">
<xsl:variable name="d" select="//DATA_BUFF"/>
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/">
<soapenv:Body>
<PREMIER-ACCESS-QUERY-REPLY xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns:xsd="http://www.w3.org/2001/XMLSchema">
      <PACQUERY-ORDER-NBR><xsl:value-of select="substring($d, 1, 9)"/></PACQUERY-ORDER-NBR>
      <PACQUERY-TIE-NUM><xsl:value-of select="substring($d, 10, 3)"/></PACQUERY-TIE-NUM>
      <PACQUERY-CONTRACT-STATUS><xsl:value-of select="substring($d, 13, 5)"/></PACQUERY-CONTRACT-STATUS>
      <PACQUERY-SYSTEM-TYPE><xsl:value-of select="substring($d, 18, 5)"/></PACQUERY-SYSTEM-TYPE>
      <PACQUERY-ORDER-SKU-INFO-NBR><xsl:value-of select="substring($d, 23, 3)"/></PACQUERY-ORDER-SKU-INFO-NBR>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 26, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 29, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 42, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 82, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 89, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 90, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 93, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 106, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 146, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 153, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 154, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 157, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 170, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 210, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 217, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 218, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 221, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 234, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 274, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 281, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 282, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 285, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 298, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 338, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 345, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 346, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 349, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 362, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 402, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 409, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 410, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 413, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 426, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 466, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 473, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 474, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 477, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 490, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 530, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 537, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 538, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 541, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 554, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 594, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 601, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 602, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 605, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 618, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 658, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 665, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 666, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 669, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 682, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 722, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 729, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 730, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 733, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 746, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 786, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 793, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 794, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 797, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 810, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 850, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 857, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 858, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 861, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 874, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 914, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 921, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 922, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 925, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 938, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 978, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 985, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 986, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 989, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 1002, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 1042, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 1049, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 1050, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 1053, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 1066, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 1106, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 1113, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 1114, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 1117, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 1130, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 1170, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 1177, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 1178, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 1181, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 1194, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 1234, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 1241, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 1242, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 1245, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 1258, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 1298, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 1305, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 1306, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 1309, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 1322, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 1362, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 1369, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 1370, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 1373, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 1386, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 1426, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 1433, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 1434, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 1437, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 1450, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 1490, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 1497, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 1498, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 1501, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 1514, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 1554, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 1561, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 1562, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 1565, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 1578, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 1618, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 1625, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 1626, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 1629, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 1642, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 1682, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 1689, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 1690, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 1693, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 1706, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 1746, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 1753, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 1754, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 1757, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 1770, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 1810, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 1817, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 1818, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 1821, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 1834, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 1874, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 1881, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 1882, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 1885, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 1898, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 1938, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 1945, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 1946, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 1949, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 1962, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 2002, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 2009, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 2010, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 2013, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 2026, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 2066, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 2073, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 2074, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 2077, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 2090, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 2130, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 2137, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 2138, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 2141, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 2154, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 2194, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 2201, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 2202, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 2205, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 2218, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 2258, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 2265, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 2266, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 2269, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 2282, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 2322, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 2329, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 2330, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 2333, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 2346, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 2386, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 2393, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 2394, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 2397, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 2410, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 2450, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 2457, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 2458, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 2461, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 2474, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 2514, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 2521, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 2522, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 2525, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 2538, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 2578, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 2585, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 2586, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 2589, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 2602, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 2642, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 2649, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 2650, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 2653, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 2666, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 2706, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 2713, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 2714, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 2717, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 2730, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 2770, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 2777, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 2778, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 2781, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 2794, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 2834, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 2841, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 2842, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 2845, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 2858, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 2898, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 2905, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 2906, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 2909, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 2922, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 2962, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 2969, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 2970, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 2973, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 2986, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 3026, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 3033, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 3034, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 3037, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 3050, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 3090, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 3097, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 3098, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 3101, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 3114, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 3154, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 3161, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 3162, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 3165, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 3178, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 3218, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 3225, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 3226, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 3229, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 3242, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 3282, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 3289, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 3290, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 3293, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 3306, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 3346, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 3353, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 3354, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 3357, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 3370, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 3410, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 3417, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 3418, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 3421, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 3434, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 3474, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 3481, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 3482, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 3485, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 3498, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 3538, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 3545, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 3546, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 3549, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 3562, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 3602, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 3609, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 3610, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 3613, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 3626, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 3666, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 3673, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 3674, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 3677, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 3690, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 3730, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 3737, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 3738, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 3741, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 3754, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 3794, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 3801, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 3802, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 3805, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 3818, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 3858, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 3865, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 3866, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 3869, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 3882, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 3922, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 3929, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 3930, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 3933, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 3946, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 3986, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 3993, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 3994, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 3997, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 4010, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 4050, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 4057, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 4058, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 4061, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 4074, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 4114, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 4121, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 4122, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 4125, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 4138, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 4178, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 4185, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 4186, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 4189, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 4202, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 4242, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 4249, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 4250, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 4253, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 4266, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 4306, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 4313, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 4314, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 4317, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 4330, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 4370, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 4377, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 4378, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 4381, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 4394, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 4434, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 4441, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 4442, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 4445, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 4458, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 4498, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 4505, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 4506, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 4509, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 4522, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 4562, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 4569, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 4570, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 4573, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 4586, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 4626, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 4633, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 4634, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 4637, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 4650, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 4690, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 4697, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 4698, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 4701, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 4714, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 4754, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 4761, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 4762, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 4765, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 4778, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 4818, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 4825, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 4826, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 4829, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 4842, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 4882, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 4889, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 4890, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 4893, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 4906, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 4946, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 4953, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 4954, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 4957, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 4970, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 5010, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 5017, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 5018, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 5021, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 5034, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 5074, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 5081, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 5082, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 5085, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 5098, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 5138, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 5145, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 5146, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 5149, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 5162, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 5202, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 5209, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 5210, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 5213, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 5226, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 5266, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 5273, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 5274, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 5277, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 5290, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 5330, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 5337, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 5338, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 5341, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 5354, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 5394, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 5401, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 5402, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 5405, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 5418, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 5458, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 5465, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 5466, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 5469, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 5482, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 5522, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 5529, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 5530, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 5533, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 5546, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 5586, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 5593, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 5594, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 5597, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 5610, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 5650, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 5657, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 5658, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 5661, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 5674, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 5714, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 5721, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 5722, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 5725, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 5738, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 5778, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 5785, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 5786, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 5789, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 5802, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 5842, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 5849, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 5850, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 5853, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 5866, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 5906, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 5913, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 5914, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 5917, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 5930, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 5970, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 5977, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 5978, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 5981, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 5994, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 6034, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 6041, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 6042, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 6045, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 6058, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 6098, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 6105, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 6106, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 6109, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 6122, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 6162, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 6169, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 6170, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 6173, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 6186, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 6226, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 6233, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 6234, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 6237, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 6250, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 6290, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 6297, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 6298, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 6301, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 6314, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 6354, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 6361, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 6362, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 6365, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 6378, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 6418, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 6425, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 6426, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 6429, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 6442, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 6482, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 6489, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 6490, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 6493, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 6506, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 6546, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 6553, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 6554, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 6557, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 6570, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 6610, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 6617, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 6618, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 6621, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 6634, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 6674, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 6681, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 6682, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 6685, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 6698, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 6738, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 6745, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 6746, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 6749, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 6762, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 6802, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 6809, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 6810, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 6813, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 6826, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 6866, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 6873, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 6874, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 6877, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 6890, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 6930, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 6937, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 6938, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 6941, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 6954, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 6994, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 7001, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 7002, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 7005, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 7018, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 7058, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 7065, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 7066, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 7069, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 7082, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 7122, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 7129, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 7130, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 7133, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 7146, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 7186, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 7193, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 7194, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 7197, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 7210, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 7250, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 7257, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 7258, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 7261, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 7274, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 7314, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 7321, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 7322, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 7325, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 7338, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 7378, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 7385, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 7386, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 7389, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 7402, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 7442, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 7449, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 7450, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 7453, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 7466, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 7506, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 7513, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 7514, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 7517, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 7530, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 7570, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 7577, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 7578, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 7581, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 7594, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 7634, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 7641, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 7642, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 7645, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 7658, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 7698, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 7705, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 7706, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 7709, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 7722, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 7762, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 7769, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 7770, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 7773, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 7786, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 7826, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 7833, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 7834, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 7837, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 7850, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 7890, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 7897, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 7898, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 7901, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 7914, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 7954, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 7961, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 7962, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 7965, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 7978, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 8018, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 8025, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 8026, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 8029, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 8042, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 8082, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 8089, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 8090, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 8093, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 8106, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 8146, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 8153, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 8154, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 8157, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 8170, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 8210, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 8217, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 8218, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 8221, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 8234, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 8274, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 8281, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 8282, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 8285, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 8298, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 8338, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 8345, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 8346, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 8349, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 8362, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 8402, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 8409, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 8410, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 8413, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 8426, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 8466, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 8473, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 8474, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 8477, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 8490, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 8530, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 8537, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 8538, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 8541, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 8554, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 8594, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 8601, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 8602, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 8605, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 8618, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 8658, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 8665, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 8666, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 8669, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 8682, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 8722, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 8729, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 8730, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 8733, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 8746, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 8786, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 8793, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 8794, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 8797, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 8810, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 8850, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 8857, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 8858, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 8861, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 8874, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 8914, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 8921, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 8922, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 8925, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 8938, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 8978, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 8985, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 8986, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 8989, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 9002, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 9042, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 9049, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 9050, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 9053, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 9066, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 9106, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 9113, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 9114, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 9117, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 9130, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 9170, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 9177, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 9178, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 9181, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 9194, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 9234, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 9241, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 9242, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 9245, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 9258, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 9298, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 9305, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 9306, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 9309, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 9322, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 9362, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 9369, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 9370, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 9373, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 9386, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 9426, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 9433, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 9434, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 9437, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 9450, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 9490, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 9497, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 9498, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 9501, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 9514, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 9554, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 9561, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 9562, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 9565, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 9578, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 9618, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 9625, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 9626, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 9629, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 9642, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 9682, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 9689, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 9690, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 9693, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 9706, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 9746, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 9753, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 9754, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 9757, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 9770, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 9810, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 9817, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 9818, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 9821, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 9834, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 9874, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 9881, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 9882, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 9885, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 9898, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 9938, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 9945, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 9946, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 9949, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 9962, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 10002, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 10009, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 10010, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 10013, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 10026, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 10066, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 10073, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 10074, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 10077, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 10090, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 10130, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 10137, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 10138, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 10141, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 10154, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 10194, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 10201, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 10202, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 10205, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 10218, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 10258, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 10265, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 10266, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 10269, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 10282, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 10322, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 10329, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 10330, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 10333, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 10346, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 10386, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 10393, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 10394, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 10397, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 10410, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 10450, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 10457, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 10458, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 10461, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 10474, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 10514, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 10521, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 10522, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 10525, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 10538, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 10578, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 10585, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 10586, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 10589, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 10602, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 10642, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 10649, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 10650, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 10653, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 10666, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 10706, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 10713, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 10714, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 10717, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 10730, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 10770, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 10777, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 10778, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 10781, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 10794, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 10834, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 10841, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 10842, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 10845, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 10858, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 10898, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 10905, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 10906, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 10909, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 10922, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 10962, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 10969, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 10970, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 10973, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 10986, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 11026, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 11033, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 11034, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 11037, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 11050, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 11090, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 11097, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 11098, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 11101, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 11114, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 11154, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 11161, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 11162, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 11165, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 11178, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 11218, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 11225, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 11226, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 11229, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 11242, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 11282, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 11289, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 11290, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 11293, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 11306, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 11346, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 11353, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 11354, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 11357, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 11370, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 11410, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 11417, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 11418, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 11421, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 11434, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 11474, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 11481, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 11482, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 11485, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 11498, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 11538, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 11545, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 11546, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 11549, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 11562, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 11602, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 11609, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 11610, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 11613, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 11626, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 11666, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 11673, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 11674, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 11677, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 11690, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 11730, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 11737, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 11738, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 11741, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 11754, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 11794, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 11801, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 11802, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 11805, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 11818, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 11858, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 11865, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 11866, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 11869, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 11882, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 11922, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 11929, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 11930, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 11933, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 11946, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 11986, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 11993, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 11994, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 11997, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 12010, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 12050, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 12057, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 12058, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 12061, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 12074, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 12114, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 12121, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 12122, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 12125, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 12138, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 12178, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 12185, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 12186, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 12189, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 12202, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 12242, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 12249, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 12250, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 12253, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 12266, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 12306, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 12313, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 12314, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 12317, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 12330, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 12370, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 12377, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 12378, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 12381, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 12394, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 12434, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 12441, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 12442, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 12445, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 12458, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 12498, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 12505, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 12506, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 12509, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 12522, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 12562, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 12569, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 12570, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 12573, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 12586, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 12626, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 12633, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 12634, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 12637, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 12650, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 12690, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 12697, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 12698, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 12701, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 12714, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 12754, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 12761, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-ORDER-SKU-INFO>
          <PACQUERY-DETAIL-SEQ-NBR><xsl:value-of select="substring($d, 12762, 3)"/></PACQUERY-DETAIL-SEQ-NBR>
          <PACQUERY-SKU-NBR><xsl:value-of select="substring($d, 12765, 13)"/></PACQUERY-SKU-NBR>
          <PACQUERY-SKU-DESC><xsl:value-of select="substring($d, 12778, 40)"/></PACQUERY-SKU-DESC>
          <PACQUERY-SKU-QTY><xsl:value-of select="substring($d, 12818, 7)"/></PACQUERY-SKU-QTY>
          <PACQUERY-SKU-QTY-SIGN><xsl:value-of select="substring($d, 12825, 1)"/></PACQUERY-SKU-QTY-SIGN>
      </PACQUERY-ORDER-SKU-INFO>
      <PACQUERY-MORE-ORDER-SKU-INFO><xsl:value-of select="substring($d, 12826, 1)"/></PACQUERY-MORE-ORDER-SKU-INFO>
      <PACQUERY-LAST-DETAIL-SEQ-NO><xsl:value-of select="substring($d, 12827, 3)"/></PACQUERY-LAST-DETAIL-SEQ-NO>
      <PACQUERY-PARTS-INFO-NBR><xsl:value-of select="substring($d, 12830, 3)"/></PACQUERY-PARTS-INFO-NBR>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 12833, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 12837, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 12842, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 12872, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 12881, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 12885, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 12890, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 12920, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 12929, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 12933, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 12938, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 12968, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 12977, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 12981, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 12986, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 13016, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 13025, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 13029, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 13034, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 13064, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 13073, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 13077, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 13082, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 13112, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 13121, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 13125, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 13130, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 13160, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 13169, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 13173, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 13178, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 13208, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 13217, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 13221, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 13226, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 13256, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 13265, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 13269, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 13274, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 13304, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 13313, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 13317, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 13322, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 13352, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 13361, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 13365, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 13370, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 13400, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 13409, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 13413, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 13418, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 13448, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 13457, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 13461, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 13466, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 13496, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 13505, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 13509, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 13514, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 13544, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 13553, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 13557, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 13562, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 13592, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 13601, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 13605, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 13610, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 13640, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 13649, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 13653, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 13658, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 13688, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 13697, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 13701, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 13706, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 13736, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 13745, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 13749, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 13754, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 13784, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 13793, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 13797, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 13802, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 13832, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 13841, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 13845, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 13850, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 13880, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 13889, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 13893, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 13898, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 13928, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 13937, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 13941, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 13946, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 13976, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 13985, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 13989, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 13994, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 14024, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 14033, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 14037, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 14042, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 14072, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 14081, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 14085, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 14090, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 14120, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 14129, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 14133, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 14138, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 14168, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 14177, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 14181, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 14186, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 14216, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 14225, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 14229, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 14234, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 14264, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 14273, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 14277, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 14282, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 14312, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 14321, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 14325, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 14330, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 14360, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 14369, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 14373, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 14378, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 14408, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 14417, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 14421, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 14426, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 14456, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 14465, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 14469, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 14474, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 14504, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 14513, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 14517, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 14522, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 14552, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 14561, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 14565, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 14570, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 14600, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 14609, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 14613, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 14618, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 14648, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 14657, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 14661, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 14666, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 14696, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 14705, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 14709, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 14714, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 14744, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 14753, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 14757, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 14762, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 14792, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 14801, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 14805, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 14810, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 14840, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 14849, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 14853, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 14858, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 14888, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 14897, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 14901, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 14906, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 14936, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 14945, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 14949, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 14954, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 14984, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 14993, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 14997, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 15002, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 15032, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 15041, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 15045, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 15050, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 15080, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 15089, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 15093, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 15098, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 15128, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 15137, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 15141, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 15146, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 15176, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 15185, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 15189, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 15194, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 15224, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 15233, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 15237, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 15242, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 15272, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 15281, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 15285, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 15290, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 15320, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 15329, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 15333, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 15338, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 15368, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 15377, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 15381, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 15386, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 15416, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 15425, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 15429, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 15434, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 15464, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 15473, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 15477, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 15482, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 15512, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 15521, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 15525, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 15530, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 15560, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 15569, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 15573, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 15578, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 15608, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 15617, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 15621, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 15626, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 15656, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 15665, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 15669, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 15674, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 15704, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 15713, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 15717, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 15722, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 15752, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 15761, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 15765, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 15770, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 15800, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 15809, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 15813, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 15818, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 15848, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 15857, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 15861, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 15866, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 15896, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 15905, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 15909, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 15914, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 15944, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 15953, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 15957, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 15962, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 15992, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 16001, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 16005, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 16010, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 16040, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 16049, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 16053, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 16058, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 16088, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 16097, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 16101, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 16106, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 16136, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 16145, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 16149, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 16154, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 16184, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 16193, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 16197, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 16202, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 16232, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 16241, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 16245, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 16250, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 16280, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 16289, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 16293, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 16298, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 16328, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 16337, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 16341, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 16346, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 16376, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 16385, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 16389, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 16394, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 16424, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 16433, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 16437, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 16442, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 16472, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 16481, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 16485, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 16490, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 16520, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 16529, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 16533, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 16538, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 16568, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 16577, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 16581, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 16586, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 16616, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 16625, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 16629, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 16634, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 16664, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 16673, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 16677, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 16682, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 16712, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 16721, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 16725, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 16730, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 16760, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 16769, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 16773, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 16778, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 16808, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 16817, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 16821, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 16826, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 16856, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 16865, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 16869, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 16874, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 16904, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 16913, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 16917, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 16922, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 16952, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 16961, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 16965, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 16970, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 17000, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 17009, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 17013, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 17018, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 17048, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 17057, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 17061, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 17066, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 17096, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 17105, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 17109, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 17114, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 17144, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 17153, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 17157, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 17162, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 17192, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 17201, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 17205, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 17210, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 17240, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 17249, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 17253, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 17258, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 17288, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 17297, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 17301, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 17306, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 17336, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 17345, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 17349, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 17354, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 17384, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 17393, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 17397, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 17402, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 17432, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 17441, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 17445, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 17450, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 17480, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 17489, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 17493, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 17498, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 17528, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 17537, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 17541, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 17546, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 17576, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 17585, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 17589, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 17594, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 17624, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 17633, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 17637, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 17642, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 17672, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 17681, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 17685, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 17690, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 17720, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 17729, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 17733, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 17738, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 17768, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 17777, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 17781, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 17786, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 17816, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 17825, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 17829, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 17834, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 17864, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 17873, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 17877, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 17882, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 17912, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 17921, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 17925, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 17930, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 17960, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 17969, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 17973, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 17978, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 18008, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 18017, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 18021, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 18026, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 18056, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 18065, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 18069, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 18074, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 18104, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 18113, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 18117, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 18122, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 18152, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 18161, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 18165, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 18170, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 18200, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 18209, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 18213, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 18218, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 18248, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 18257, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 18261, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 18266, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 18296, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 18305, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 18309, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 18314, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 18344, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 18353, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 18357, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 18362, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 18392, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 18401, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 18405, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 18410, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 18440, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 18449, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 18453, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 18458, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 18488, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 18497, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 18501, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 18506, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 18536, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 18545, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 18549, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 18554, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 18584, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 18593, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 18597, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 18602, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 18632, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 18641, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 18645, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 18650, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 18680, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 18689, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 18693, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 18698, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 18728, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 18737, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 18741, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 18746, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 18776, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 18785, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 18789, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 18794, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 18824, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 18833, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 18837, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 18842, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 18872, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 18881, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 18885, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 18890, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 18920, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 18929, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 18933, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 18938, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 18968, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 18977, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 18981, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 18986, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 19016, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 19025, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 19029, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 19034, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 19064, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 19073, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 19077, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 19082, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 19112, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 19121, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 19125, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 19130, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 19160, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 19169, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 19173, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 19178, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 19208, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 19217, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 19221, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 19226, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 19256, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 19265, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 19269, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 19274, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 19304, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 19313, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 19317, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 19322, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 19352, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 19361, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 19365, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 19370, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 19400, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 19409, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 19413, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 19418, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 19448, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 19457, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 19461, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 19466, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 19496, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 19505, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 19509, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 19514, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 19544, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 19553, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 19557, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 19562, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 19592, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 19601, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 19605, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 19610, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 19640, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 19649, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 19653, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 19658, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 19688, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 19697, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 19701, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 19706, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 19736, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 19745, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 19749, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 19754, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 19784, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 19793, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 19797, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 19802, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 19832, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 19841, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 19845, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 19850, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 19880, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 19889, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 19893, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 19898, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 19928, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 19937, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 19941, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 19946, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 19976, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 19985, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 19989, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 19994, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 20024, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 20033, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 20037, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 20042, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 20072, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 20081, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 20085, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 20090, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 20120, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 20129, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 20133, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 20138, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 20168, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 20177, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 20181, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 20186, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 20216, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 20225, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 20229, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 20234, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 20264, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 20273, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 20277, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 20282, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 20312, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 20321, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 20325, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 20330, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 20360, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 20369, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 20373, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 20378, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 20408, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 20417, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 20421, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 20426, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 20456, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 20465, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 20469, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 20474, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 20504, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 20513, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 20517, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 20522, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 20552, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 20561, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 20565, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 20570, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 20600, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 20609, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 20613, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 20618, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 20648, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 20657, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 20661, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 20666, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 20696, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 20705, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 20709, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 20714, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 20744, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 20753, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 20757, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 20762, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 20792, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 20801, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 20805, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 20810, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 20840, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 20849, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 20853, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 20858, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 20888, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 20897, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 20901, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 20906, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 20936, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 20945, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 20949, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 20954, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 20984, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 20993, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 20997, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 21002, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 21032, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 21041, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 21045, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 21050, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 21080, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 21089, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 21093, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 21098, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 21128, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 21137, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 21141, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 21146, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 21176, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 21185, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 21189, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 21194, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 21224, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 21233, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 21237, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 21242, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 21272, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 21281, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 21285, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 21290, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 21320, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 21329, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 21333, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 21338, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 21368, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 21377, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 21381, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 21386, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 21416, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 21425, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 21429, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 21434, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 21464, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 21473, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 21477, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 21482, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 21512, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 21521, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 21525, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 21530, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 21560, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 21569, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 21573, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 21578, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 21608, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 21617, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 21621, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 21626, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 21656, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 21665, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 21669, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 21674, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 21704, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 21713, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 21717, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 21722, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 21752, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 21761, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 21765, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 21770, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 21800, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 21809, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 21813, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 21818, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 21848, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 21857, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 21861, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 21866, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 21896, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 21905, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 21909, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 21914, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 21944, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 21953, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 21957, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 21962, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 21992, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 22001, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 22005, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 22010, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 22040, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 22049, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 22053, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 22058, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 22088, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 22097, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 22101, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 22106, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 22136, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 22145, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 22149, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 22154, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 22184, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 22193, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 22197, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 22202, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 22232, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 22241, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 22245, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 22250, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 22280, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 22289, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 22293, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 22298, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 22328, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 22337, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 22341, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 22346, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 22376, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 22385, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 22389, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 22394, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 22424, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 22433, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 22437, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 22442, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 22472, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 22481, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 22485, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 22490, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 22520, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 22529, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 22533, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 22538, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 22568, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 22577, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 22581, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 22586, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 22616, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 22625, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 22629, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 22634, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 22664, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 22673, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 22677, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 22682, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 22712, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 22721, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 22725, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 22730, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 22760, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 22769, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 22773, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 22778, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 22808, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 22817, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 22821, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 22826, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 22856, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 22865, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 22869, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 22874, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 22904, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 22913, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 22917, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 22922, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 22952, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 22961, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 22965, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 22970, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 23000, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 23009, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 23013, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 23018, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 23048, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 23057, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 23061, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 23066, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 23096, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 23105, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 23109, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 23114, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 23144, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 23153, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 23157, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 23162, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 23192, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 23201, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 23205, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 23210, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 23240, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 23249, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 23253, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 23258, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 23288, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 23297, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 23301, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 23306, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 23336, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 23345, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 23349, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 23354, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 23384, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 23393, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 23397, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 23402, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 23432, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 23441, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 23445, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 23450, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 23480, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 23489, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 23493, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 23498, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 23528, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 23537, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 23541, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 23546, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 23576, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 23585, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 23589, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 23594, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 23624, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 23633, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 23637, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 23642, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 23672, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 23681, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 23685, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 23690, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 23720, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 23729, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 23733, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 23738, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 23768, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 23777, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 23781, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 23786, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 23816, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 23825, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 23829, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 23834, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 23864, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 23873, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 23877, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 23882, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 23912, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 23921, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 23925, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 23930, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 23960, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 23969, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 23973, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 23978, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 24008, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 24017, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 24021, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 24026, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 24056, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 24065, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 24069, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 24074, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 24104, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 24113, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 24117, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 24122, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 24152, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 24161, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 24165, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 24170, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 24200, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 24209, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 24213, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 24218, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 24248, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 24257, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 24261, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 24266, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 24296, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 24305, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 24309, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 24314, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 24344, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 24353, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 24357, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 24362, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 24392, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 24401, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 24405, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 24410, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 24440, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 24449, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 24453, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 24458, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 24488, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 24497, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 24501, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 24506, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 24536, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 24545, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 24549, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 24554, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 24584, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 24593, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 24597, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 24602, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 24632, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 24641, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 24645, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 24650, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 24680, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 24689, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 24693, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 24698, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 24728, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 24737, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 24741, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 24746, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 24776, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 24785, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 24789, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 24794, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 24824, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 24833, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 24837, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 24842, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 24872, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 24881, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 24885, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 24890, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 24920, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 24929, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 24933, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 24938, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 24968, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 24977, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 24981, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 24986, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 25016, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 25025, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 25029, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 25034, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 25064, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 25073, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 25077, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 25082, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 25112, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 25121, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 25125, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 25130, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 25160, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 25169, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 25173, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 25178, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 25208, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 25217, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 25221, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 25226, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 25256, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 25265, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 25269, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 25274, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 25304, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 25313, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 25317, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 25322, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 25352, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 25361, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 25365, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 25370, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 25400, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 25409, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 25413, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 25418, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 25448, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 25457, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 25461, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 25466, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 25496, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 25505, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 25509, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 25514, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 25544, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 25553, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 25557, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 25562, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 25592, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 25601, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 25605, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 25610, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 25640, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 25649, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 25653, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 25658, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 25688, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 25697, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 25701, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 25706, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 25736, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 25745, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 25749, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 25754, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 25784, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 25793, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 25797, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 25802, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 25832, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 25841, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 25845, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 25850, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 25880, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 25889, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 25893, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 25898, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 25928, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 25937, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 25941, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 25946, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 25976, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 25985, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 25989, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 25994, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 26024, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 26033, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 26037, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 26042, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 26072, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 26081, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 26085, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 26090, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 26120, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 26129, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 26133, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 26138, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 26168, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 26177, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 26181, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 26186, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 26216, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 26225, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 26229, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 26234, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 26264, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 26273, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 26277, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 26282, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 26312, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 26321, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 26325, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 26330, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 26360, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 26369, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 26373, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 26378, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 26408, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 26417, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 26421, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 26426, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 26456, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 26465, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 26469, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 26474, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 26504, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 26513, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 26517, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 26522, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 26552, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 26561, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 26565, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 26570, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 26600, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 26609, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 26613, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 26618, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 26648, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 26657, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 26661, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 26666, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 26696, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 26705, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 26709, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 26714, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 26744, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 26753, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 26757, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 26762, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 26792, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 26801, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 26805, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 26810, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 26840, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 26849, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 26853, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 26858, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 26888, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 26897, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 26901, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 26906, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 26936, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 26945, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 26949, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 26954, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 26984, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 26993, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 26997, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 27002, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 27032, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 27041, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 27045, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 27050, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 27080, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 27089, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 27093, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 27098, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 27128, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 27137, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 27141, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 27146, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 27176, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-PARTS-INFO>
         <PACQUERY-SEQ-NBR><xsl:value-of select="substring($d, 27185, 4)"/></PACQUERY-SEQ-NBR>
         <PACQUERY-PART-NBR><xsl:value-of select="substring($d, 27189, 5)"/></PACQUERY-PART-NBR>
         <PACQUERY-PART-DESC><xsl:value-of select="substring($d, 27194, 30)"/></PACQUERY-PART-DESC>
         <PACQUERY-PART-QTY><xsl:value-of select="substring($d, 27224, 9)"/></PACQUERY-PART-QTY>
      </PACQUERY-PARTS-INFO>
      <PACQUERY-GOLD-ACCOUNT-FLAG><xsl:value-of select="substring($d, 27233, 1)"/></PACQUERY-GOLD-ACCOUNT-FLAG>
      <PACQUERY-CONTRACTS-INFO-NBR><xsl:value-of select="substring($d, 27234, 2)"/></PACQUERY-CONTRACTS-INFO-NBR>
      <PACQUERY-CONTRACTS-INFO>
         <PACQUERY-CNTR-TYPE><xsl:value-of select="substring($d, 27236, 1)"/></PACQUERY-CNTR-TYPE>
         <PACQUERY-SERVICE-LVL><xsl:value-of select="substring($d, 27237, 2)"/></PACQUERY-SERVICE-LVL>
         <PACQUERY-COVERAGE-CODE><xsl:value-of select="substring($d, 27239, 5)"/></PACQUERY-COVERAGE-CODE>
         <PACQUERY-BEGIN-DATE><xsl:value-of select="substring($d, 27244, 8)"/></PACQUERY-BEGIN-DATE>
         <PACQUERY-END-DATE><xsl:value-of select="substring($d, 27252, 8)"/></PACQUERY-END-DATE>
      </PACQUERY-CONTRACTS-INFO>
      <PACQUERY-CONTRACTS-INFO>
         <PACQUERY-CNTR-TYPE><xsl:value-of select="substring($d, 27260, 1)"/></PACQUERY-CNTR-TYPE>
         <PACQUERY-SERVICE-LVL><xsl:value-of select="substring($d, 27261, 2)"/></PACQUERY-SERVICE-LVL>
         <PACQUERY-COVERAGE-CODE><xsl:value-of select="substring($d, 27263, 5)"/></PACQUERY-COVERAGE-CODE>
         <PACQUERY-BEGIN-DATE><xsl:value-of select="substring($d, 27268, 8)"/></PACQUERY-BEGIN-DATE>
         <PACQUERY-END-DATE><xsl:value-of select="substring($d, 27276, 8)"/></PACQUERY-END-DATE>
      </PACQUERY-CONTRACTS-INFO>
      <PACQUERY-CONTRACTS-INFO>
         <PACQUERY-CNTR-TYPE><xsl:value-of select="substring($d, 27284, 1)"/></PACQUERY-CNTR-TYPE>
         <PACQUERY-SERVICE-LVL><xsl:value-of select="substring($d, 27285, 2)"/></PACQUERY-SERVICE-LVL>
         <PACQUERY-COVERAGE-CODE><xsl:value-of select="substring($d, 27287, 5)"/></PACQUERY-COVERAGE-CODE>
         <PACQUERY-BEGIN-DATE><xsl:value-of select="substring($d, 27292, 8)"/></PACQUERY-BEGIN-DATE>
         <PACQUERY-END-DATE><xsl:value-of select="substring($d, 27300, 8)"/></PACQUERY-END-DATE>
      </PACQUERY-CONTRACTS-INFO>
      <PACQUERY-CONTRACTS-INFO>
         <PACQUERY-CNTR-TYPE><xsl:value-of select="substring($d, 27308, 1)"/></PACQUERY-CNTR-TYPE>
         <PACQUERY-SERVICE-LVL><xsl:value-of select="substring($d, 27309, 2)"/></PACQUERY-SERVICE-LVL>
         <PACQUERY-COVERAGE-CODE><xsl:value-of select="substring($d, 27311, 5)"/></PACQUERY-COVERAGE-CODE>
         <PACQUERY-BEGIN-DATE><xsl:value-of select="substring($d, 27316, 8)"/></PACQUERY-BEGIN-DATE>
         <PACQUERY-END-DATE><xsl:value-of select="substring($d, 27324, 8)"/></PACQUERY-END-DATE>
      </PACQUERY-CONTRACTS-INFO>
      <PACQUERY-CONTRACTS-INFO>
         <PACQUERY-CNTR-TYPE><xsl:value-of select="substring($d, 27332, 1)"/></PACQUERY-CNTR-TYPE>
         <PACQUERY-SERVICE-LVL><xsl:value-of select="substring($d, 27333, 2)"/></PACQUERY-SERVICE-LVL>
         <PACQUERY-COVERAGE-CODE><xsl:value-of select="substring($d, 27335, 5)"/></PACQUERY-COVERAGE-CODE>
         <PACQUERY-BEGIN-DATE><xsl:value-of select="substring($d, 27340, 8)"/></PACQUERY-BEGIN-DATE>
         <PACQUERY-END-DATE><xsl:value-of select="substring($d, 27348, 8)"/></PACQUERY-END-DATE>
      </PACQUERY-CONTRACTS-INFO>
      <PACQUERY-CONTRACTS-INFO>
         <PACQUERY-CNTR-TYPE><xsl:value-of select="substring($d, 27356, 1)"/></PACQUERY-CNTR-TYPE>
         <PACQUERY-SERVICE-LVL><xsl:value-of select="substring($d, 27357, 2)"/></PACQUERY-SERVICE-LVL>
         <PACQUERY-COVERAGE-CODE><xsl:value-of select="substring($d, 27359, 5)"/></PACQUERY-COVERAGE-CODE>
         <PACQUERY-BEGIN-DATE><xsl:value-of select="substring($d, 27364, 8)"/></PACQUERY-BEGIN-DATE>
         <PACQUERY-END-DATE><xsl:value-of select="substring($d, 27372, 8)"/></PACQUERY-END-DATE>
      </PACQUERY-CONTRACTS-INFO>
      <PACQUERY-CONTRACTS-INFO>
         <PACQUERY-CNTR-TYPE><xsl:value-of select="substring($d, 27380, 1)"/></PACQUERY-CNTR-TYPE>
         <PACQUERY-SERVICE-LVL><xsl:value-of select="substring($d, 27381, 2)"/></PACQUERY-SERVICE-LVL>
         <PACQUERY-COVERAGE-CODE><xsl:value-of select="substring($d, 27383, 5)"/></PACQUERY-COVERAGE-CODE>
         <PACQUERY-BEGIN-DATE><xsl:value-of select="substring($d, 27388, 8)"/></PACQUERY-BEGIN-DATE>
         <PACQUERY-END-DATE><xsl:value-of select="substring($d, 27396, 8)"/></PACQUERY-END-DATE>
      </PACQUERY-CONTRACTS-INFO>
      <PACQUERY-CONTRACTS-INFO>
         <PACQUERY-CNTR-TYPE><xsl:value-of select="substring($d, 27404, 1)"/></PACQUERY-CNTR-TYPE>
         <PACQUERY-SERVICE-LVL><xsl:value-of select="substring($d, 27405, 2)"/></PACQUERY-SERVICE-LVL>
         <PACQUERY-COVERAGE-CODE><xsl:value-of select="substring($d, 27407, 5)"/></PACQUERY-COVERAGE-CODE>
         <PACQUERY-BEGIN-DATE><xsl:value-of select="substring($d, 27412, 8)"/></PACQUERY-BEGIN-DATE>
         <PACQUERY-END-DATE><xsl:value-of select="substring($d, 27420, 8)"/></PACQUERY-END-DATE>
      </PACQUERY-CONTRACTS-INFO>
      <PACQUERY-CONTRACTS-INFO>
         <PACQUERY-CNTR-TYPE><xsl:value-of select="substring($d, 27428, 1)"/></PACQUERY-CNTR-TYPE>
         <PACQUERY-SERVICE-LVL><xsl:value-of select="substring($d, 27429, 2)"/></PACQUERY-SERVICE-LVL>
         <PACQUERY-COVERAGE-CODE><xsl:value-of select="substring($d, 27431, 5)"/></PACQUERY-COVERAGE-CODE>
         <PACQUERY-BEGIN-DATE><xsl:value-of select="substring($d, 27436, 8)"/></PACQUERY-BEGIN-DATE>
         <PACQUERY-END-DATE><xsl:value-of select="substring($d, 27444, 8)"/></PACQUERY-END-DATE>
      </PACQUERY-CONTRACTS-INFO>
      <PACQUERY-CONTRACTS-INFO>
         <PACQUERY-CNTR-TYPE><xsl:value-of select="substring($d, 27452, 1)"/></PACQUERY-CNTR-TYPE>
         <PACQUERY-SERVICE-LVL><xsl:value-of select="substring($d, 27453, 2)"/></PACQUERY-SERVICE-LVL>
         <PACQUERY-COVERAGE-CODE><xsl:value-of select="substring($d, 27455, 5)"/></PACQUERY-COVERAGE-CODE>
         <PACQUERY-BEGIN-DATE><xsl:value-of select="substring($d, 27460, 8)"/></PACQUERY-BEGIN-DATE>
         <PACQUERY-END-DATE><xsl:value-of select="substring($d, 27468, 8)"/></PACQUERY-END-DATE>
      </PACQUERY-CONTRACTS-INFO>
      <PACQUERY-TAG-ADDRESS>
         <PACQUERY-CONTACT-1><xsl:value-of select="substring($d, 27476, 30)"/></PACQUERY-CONTACT-1>
         <PACQUERY-CONTACT-2><xsl:value-of select="substring($d, 27506, 30)"/></PACQUERY-CONTACT-2>
         <PACQUERY-ADDRESS-1><xsl:value-of select="substring($d, 27536, 30)"/></PACQUERY-ADDRESS-1>
         <PACQUERY-ADDRESS-2><xsl:value-of select="substring($d, 27566, 30)"/></PACQUERY-ADDRESS-2>
         <PACQUERY-CITY><xsl:value-of select="substring($d, 27596, 30)"/></PACQUERY-CITY>
         <PACQUERY-COUNTY><xsl:value-of select="substring($d, 27626, 30)"/></PACQUERY-COUNTY>
         <PACQUERY-POST-CODE><xsl:value-of select="substring($d, 27656, 9)"/></PACQUERY-POST-CODE>
         <PACQUERY-PHONE-1><xsl:value-of select="substring($d, 27665, 26)"/></PACQUERY-PHONE-1>
         <PACQUERY-PHONE-2><xsl:value-of select="substring($d, 27691, 26)"/></PACQUERY-PHONE-2>
         <PACQUERY-ALT-COMPANY><xsl:value-of select="substring($d, 27717, 30)"/></PACQUERY-ALT-COMPANY>
      </PACQUERY-TAG-ADDRESS>      <PACQUERY-PREMIER-SVC-TYPE><xsl:value-of select="substring($d, 27747, 1)"/></PACQUERY-PREMIER-SVC-TYPE>
      <FILE-STATUS><xsl:value-of select="substring($d, 27748, 2)"/></FILE-STATUS>
      <FEL-GUARDIAN-ERR><xsl:value-of select="substring($d, 27750, 3)"/></FEL-GUARDIAN-ERR>
      <FILE-NAME><xsl:value-of select="substring($d, 27753, 8)"/></FILE-NAME>
      <ERROR-CODE><xsl:value-of select="substring($d, 27761, 4)"/></ERROR-CODE>
      <ERROR-TEXT><xsl:value-of select="substring($d, 27765, 78)"/></ERROR-TEXT>
</PREMIER-ACCESS-QUERY-REPLY></soapenv:Body>
</soapenv:Envelope>
</xsl:template>
</xsl:stylesheet>
