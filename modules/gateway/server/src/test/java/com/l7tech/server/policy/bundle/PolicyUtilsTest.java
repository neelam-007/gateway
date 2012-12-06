package com.l7tech.server.policy.bundle;

import com.l7tech.common.io.XmlUtil;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class PolicyUtilsTest {

    @Test
    public void testFindContextVariables() throws Exception {
        final Document serviceEnumDoc = XmlUtil.parse(CANNED_L7_POLICY_WITH_VARIABLES);
        final List<Element> contextVariables = PolicyUtils.findContextVariables(serviceEnumDoc.getDocumentElement());
        assertEquals("Incorrect number of context variables found", 4, contextVariables.size());
    }

    @Test
    public void testFindProtectedServiceUrls() throws Exception {
        final Document servicePolicyDoc = XmlUtil.parse(CANNED_L7_POLICY_ROUTING_URLS_JDBC);
        final List<Element> protectedUrls = PolicyUtils.findProtectedUrls(servicePolicyDoc.getDocumentElement());
        assertEquals("Incorrect number of protected urls found", 3, protectedUrls.size());
    }

    @Test
    public void testFindJdbcReferences() throws Exception {
        final Document servicePolicyDoc = XmlUtil.parse(CANNED_L7_POLICY_ROUTING_URLS_JDBC);
        final List<Element> protectedUrls = PolicyUtils.findJdbcReferences(servicePolicyDoc.getDocumentElement());
        assertEquals("Incorrect number of jdbc query elements found", 2, protectedUrls.size());
    }

    // - PRIVATE

    public static final String CANNED_L7_POLICY_WITH_VARIABLES = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:SetVariable>\n" +
            "            <L7p:Base64Expression stringValue=\"T0F1dGggMS4wIEF1dGhvcml6YXRpb24gYW5kIFJlc291cmNlIFNlcnZlcg==\"/>\n" +
            "            <L7p:VariableToSet stringValue=\"this.app.name\"/>\n" +
            "        </L7p:SetVariable>\n" +
            "        <L7p:SetVariable>\n" +
            "            <L7p:Base64Expression stringValue=\"PHN0eWxlIHR5cGU9InRleHQvY3NzIj4NCmJvZHkuZHJhZ3tjdXJzb3I6bW92ZTt9dGguYWN0aXZlIGltZ3tkaXNwbGF5OmlubGluZTt9dHIuZXZlbix0ci5vZGR7YmFja2dyb3VuZC1jb2xvcjojZWVlO2JvcmRlci1ib3R0b206MXB4IHNvbGlkICNjY2M7cGFkZGluZzowLjFlbSAwLjZlbTt9DQp0ci5kcmFne2JhY2tncm91bmQtY29sb3I6I2ZmZmZmMDt9dHIuZHJhZy1wcmV2aW91c3tiYWNrZ3JvdW5kLWNvbG9yOiNmZmQ7fQ0KdGQuYWN0aXZle2JhY2tncm91bmQtY29sb3I6I2RkZDt9dGQuY2hlY2tib3gsdGguY2hlY2tib3h7dGV4dC1hbGlnbjpjZW50ZXI7fQ0KdGJvZHl7Ym9yZGVyLXRvcDoxcHggc29saWQgI2NjYzt9dGJvZHkgdGh7Ym9yZGVyLWJvdHRvbToxcHggc29saWQgI2NjYzt9DQp0aGVhZCB0aHt0ZXh0LWFsaWduOmxlZnQ7DQogIHBhZGRpbmctcmlnaHQ6MWVtOw0KICBib3JkZXItYm90dG9tOjNweCBzb2xpZCAjY2NjO30NCi5icmVhZGNydW1ie3BhZGRpbmctYm90dG9tOi41ZW19ZGl2LmluZGVudGF0aW9ue3dpZHRoOjIwcHg7aGVpZ2h0OjEuN2VtO21hcmdpbjotMC40ZW0gMC4yZW0gLTAuNGVtIC0wLjRlbTsNCiAgcGFkZGluZzowLjQyZW0gMCAwLjQyZW0gMC42ZW07DQogIGZsb2F0OmxlZnQ7fQ0KdWwucHJpbWFyeXtib3JkZXItY29sbGFwc2U6Y29sbGFwc2U7cGFkZGluZzowIDAgMCAxZW07DQogIHdoaXRlLXNwYWNlOm5vd3JhcDtsaXN0LXN0eWxlOm5vbmU7bWFyZ2luOjVweDtoZWlnaHQ6YXV0bztsaW5lLWhlaWdodDpub3JtYWw7Ym9yZGVyLWJvdHRvbToxcHggc29saWQgI2JiYjt9DQp1bC5wcmltYXJ5IGxpe2Rpc3BsYXk6aW5saW5lO311bC5wcmltYXJ5IGxpIGF7YmFja2dyb3VuZC1jb2xvcjojZGRkO2JvcmRlci1jb2xvcjojYmJiO2JvcmRlci13aWR0aDoxcHg7Ym9yZGVyLXN0eWxlOnNvbGlkIHNvbGlkIG5vbmUgc29saWQ7aGVpZ2h0OmF1dG87bWFyZ2luLXJpZ2h0OjAuNWVtOw0KICBwYWRkaW5nOjAgMWVtO3RleHQtZGVjb3JhdGlvbjpub25lO311bC5wcmltYXJ5IGxpLmFjdGl2ZSBhe2JhY2tncm91bmQtY29sb3I6I2ZmZjtib3JkZXI6MXB4IHNvbGlkICNiYmI7Ym9yZGVyLWJvdHRvbTojZmZmIDFweCBzb2xpZDt9DQp1bC5wcmltYXJ5IGxpIGE6aG92ZXJ7YmFja2dyb3VuZC1jb2xvcjojZWVlO2JvcmRlci1jb2xvcjojY2NjO2JvcmRlci1ib3R0b20tY29sb3I6I2VlZTt9dWwuc2Vjb25kYXJ5e2JvcmRlci1ib3R0b206MXB4IHNvbGlkICNiYmI7cGFkZGluZzowLjVlbSAxZW07bWFyZ2luOjVweDt9DQoNCip7bWFyZ2luOjA7fQ0KaHRtbCxib2R5e2JhY2tncm91bmQ6I2ZmZjtmb250OjEycHgvMS4yZW0gaGVsdmV0aWNhLGFyaWFsLHNhbnMtc2VyaWY7Y29sb3I6IzRiNGI0YjtoZWlnaHQ6MTAwJTtsaW5lLWhlaWdodDoxLjNlbTt9DQpocntoZWlnaHQ6MXB4O2JhY2tncm91bmQtY29sb3I6I2RkZDsgbm8tcmVwZWF0IGNlbnRlciBib3R0b207cGFkZGluZzo4cHggYXV0bztib3JkZXI6MCBub25lO30NCmlucHV0e2NvbG9yOiM0YjRiNGI7fQ0KaDEsaDIsaDMsaDQsaDUsaDZ7cGFkZGluZzowO2ZvbnQtd2VpZ2h0Om5vcm1hbDtmb250LWZhbWlseTpoZWx2ZXRpY2EsYXJpYWwsc2Fucy1zZXJpZjt9DQpoMXtmb250LXNpemU6MjUwJTtjb2xvcjp3aGl0ZTt6LWluZGV4OjU7fQ0KaDJ7Zm9udC1zaXplOjE3NSU7fQ0KaDN7Zm9udC1zaXplOjE1MCU7fQ0KaDR7Zm9udC1zaXplOjEyMCU7fQ0KaDV7Zm9udC1zaXplOjExMCU7fQ0KaDZ7Zm9udC1zaXplOjEwNSU7fQ0KYTphY3RpdmUsYS5hY3RpdmV7Y29sb3I6IzRiNGI0Yjt0ZXh0LWRlY29yYXRpb246bm9uZTt9DQphOmxpbmssYTp2aXNpdGVke2NvbG9yOiM0YjRiNGI7dGV4dC1kZWNvcmF0aW9uOnVuZGVybGluZTtvdXRsaW5lOm5vbmU7fQ0KYTpob3Zlcntjb2xvcjojZGIzYjJjO3RleHQtZGVjb3JhdGlvbjp1bmRlcmxpbmU7fQ0KcHttYXJnaW4tYm90dG9tOi41ZW07bGluZS1oZWlnaHQ6MS4zZW07bGlzdC1zdHlsZTpub25lO3RleHQtYWxpZ246bGVmdDtsaW5lLWhlaWdodDoxLjVlbTtjb2xvcjojNGI0YjRiO30NCmJsb2NrcXVvdGUgKnttYXJnaW46NXB4IDAgMCAxMHB4O311bHtsaW5lLWhlaWdodDoxLjVlbTsNCnBhZGRpbmc6LjJlbSAxZW0gMWVtIDFlbTt9b2x7bGluZS1oZWlnaHQ6MS41ZW07cGFkZGluZzouMmVtIDFlbSAxZW0gMWVtO310ci5ldmVuLHRyLm9kZHtiYWNrZ3JvdW5kOm5vbmU7fWltZyxhIGltZ3tib3JkZXI6bm9uZTt9DQpuby1yZXBlYXQgbGVmdCAzNSU7bWFyZ2luLWxlZnQ6LjVlbTtwYWRkaW5nLWxlZnQ6MWVtO30NCiN3cmFwcGVye21pbi1oZWlnaHQ6MTAwJTtoZWlnaHQ6YXV0byAhaW1wb3J0YW50O2hlaWdodDoxMDAlO21hcmdpbjowIGF1dG8gLTVlbTt9I2NvbnRhaW5lcnttYXJnaW46LTIuNWVtIGF1dG8gMCAyLjVlbTt9DQouZmllbGQtY29udGVudCAudmlldy1kb20taWQtNSAudmlldy1jb250ZW50IC52aWV3cy1yb3d7DQogICAgbWFyZ2luOjBweDtwYWRkaW5nOjBweDt9DQoNCiNzaWRlYmFyLWxlZnR7cG9zaXRpb246cmVsYXRpdmU7ZmxvYXQ6bGVmdDtjbGVhcjpsZWZ0O3BhZGRpbmc6MmVtIDAgNmVtIDA7ei1pbmRleDo1MDA7d2lkdGg6MTMuNWVtO30NCiNzaWRlYmFyLWxlZnQgLmNvbnRlbnR7bWFyZ2luLXRvcDouOGVtO30NCiNzaWRlYmFyLWxlZnQgYXtkaXNwbGF5OmJsb2NrO2xpbmUtaGVpZ2h0OjEuMmVtO3BhZGRpbmc6M3B4IDAgMXB4IDNweDt0ZXh0LWRlY29yYXRpb246bm9uZTtmb250LXNpemU6MTA1JTttYXJnaW4tbGVmdDozcHg7bWFyZ2luLXJpZ2h0OjNweDt9DQojc2lkZWJhci1sZWZ0IHVse3dpZHRoOjEzLjVlbTtkaXNwbGF5OmJsb2NrO2xpc3Qtc3R5bGU6bm9uZTt9DQojc2lkZWJhci1sZWZ0IHVsIHVse25vLXJlcGVhdCBjZW50ZXIgdG9wO3BhZGRpbmctbGVmdDouMmVtO30NCiNzaWRlYmFyLWxlZnQgYTpob3ZlciwNCiNzaWRlYmFyLWxlZnQgYS5hY3RpdmV7YmFja2dyb3VuZC1jb2xvcjojZDgzYjJjO2NvbG9yOndoaXRlO30NCiNzaWRlYmFyLWxlZnQgLm5vbGluay1saXtwYWRkaW5nLXRvcDoxLjNlbTt9DQojc2lkZWJhci1sZWZ0IC5maXJzdC5ub2xpbmstbGl7cGFkZGluZy10b3A6MDt9DQojc2lkZWJhci1sZWZ0IC5ub2xpbmt7Y29sb3I6IzIzMjAyMTtmb250LXNpemU6MTIwJTtmb250LXdlaWdodDo2MDA7fQ0KI3NpZGViYXItbGVmdCAubWVudS1uYW1lLW1lbnUtcmVzb3VyY2VzIC5ub2xpbmt7Zm9udC13ZWlnaHQ6MTAwO2ZvbnQtc2l6ZToxMDUlO30NCiNzaWRlYmFyLWxlZnQgbGkubGVhZntib3JkZXI6bm9uZTt0ZXh0LWluZGVudDpub25lOyBuby1yZXBlYXQgY2VudGVyIGJvdHRvbTtkaXNwbGF5OmJsb2NrO3BhZGRpbmc6MXB4IDAgMnB4O30NCiNzaWRlYmFyLWxlZnQgdWwgdWwgbGkuZmlyc3R7cGFkZGluZy10b3A6MnB4O30NCg0KLnZpZXctY29udGVudCBoNHtmb250LXNpemU6MTAwJTt9I25hdmlnYXRpb257cG9zaXRpb246cmVsYXRpdmU7Ym9yZGVyOjBweCBzb2xpZCByZWQ7bWFyZ2luOjBweDtwYWRkaW5nOjBweDt9DQoNCi5icmVhZGNydW1ie3RleHQtYWxpZ246bGVmdDtwYWRkaW5nOjA7cG9zaXRpb246YWJzb2x1dGU7dG9wOjQuN2VtO2xlZnQ6MTUuOGVtO3otaW5kZXg6NTA7fS5icmVhZGNydW1iIGEuYWN0aXZlLC5icmVhZGNydW1iIGE6YWN0aXZle2NvbG9yOiMyMzIwMjE7dGV4dC1kZWNvcmF0aW9uOm5vbmU7fS5icmVhZGNydW1iIGE6aG92ZXJ7Y29sb3I6I2RiM2IyYzt9bGkubGVhZntkaXNwbGF5OmlubGluZTtwYWRkaW5nOjAgMC41ZW0gMCAwLjNlbTt2ZXJ0aWNhbC1hbGlnbjptaWRkbGU7Ym9yZGVyLXJpZ2h0OjFweCBzb2xpZCAjNzI3MjcyO30NCg0KI2hlYWRlcntoZWlnaHQ6Ni41ZW07bWF4LXdpZHRoOjEwMCU7cG9zaXRpb246cmVsYXRpdmU7fSNoZWFkZXIgI2xvZ28tZmxvYXRlcntmbG9hdDpsZWZ0O3Bvc2l0aW9uOnJlbGF0aXZlO3RvcDoxMCU7d2lkdGg6MTU5cHg7aGVpZ2h0OjUzcHg7bWFyZ2luLWxlZnQ6Mi41ZW07fWRpdiNyZXN7ZGlzcGxheTpub25lO3Zpc2liaWxpdHk6aGlkZGVuO30jbWVudWJrZ3twb3NpdGlvbjpyZWxhdGl2ZTt3aWR0aDoxMDAlO2hlaWdodDozMXB4O2JhY2tncm91bmQtY29sb3I6IzY2Njt0b3A6Ni42ZW07ei1pbmRleDowO30NCg0KZGl2Lmxpbmtze2Rpc3BsYXk6bm9uZTt9DQoNCiNjb250ZW50e21hcmdpbjo0LjNlbSBhdXRvIDFlbSAyLjVlbTt9DQouc3VibWl0dGVke2Rpc3BsYXk6bm9uZTt9LmNvbnRhaW5lcnt6LWluZGV4OjU7aGVpZ2h0OjEwMCU7cGFkZGluZy10b3A6LjJlbTt9DQoudGl0bGUtYm94IGgxe2NvbG9yOiNkYjNiMmM7bGluZS1oZWlnaHQ6MWVtO30NCg0KI25hdmlnYXRpb24gZGl2Lm1lbnUtbmFtZS1wcmltYXJ5LWxpbmtzIHVse2JvcmRlcjowO2xpc3Qtc3R5bGU6bm9uZTtoZWlnaHQ6MzFweDttYXJnaW46MDtwYWRkaW5nOjA7d2lkdGg6MTAwJTt9DQojbmF2aWdhdGlvbiBkaXYubWVudS1uYW1lLXByaW1hcnktbGlua3MgdWwgbGl7YmFja2dyb3VuZDp0cmFuc3BhcmVudDtib3JkZXI6MDtmbG9hdDpsZWZ0O21hcmdpbjowO3BhZGRpbmc6MDtsaXN0LXN0eWxlOm5vbmU7cG9zaXRpb246cmVsYXRpdmU7fQ0KI25hdmlnYXRpb24gZGl2Lm1lbnUtbmFtZS1wcmltYXJ5LWxpbmtzIHVsIGxpIGF7ZGlzcGxheTpibG9jaztoZWlnaHQ6MzFweDt0ZXh0LWluZGVudDotNTAwMHB4O30NCiNuYXZpZ2F0aW9uIGRpdi5tZW51LW5hbWUtcHJpbWFyeS1saW5rcyBsaSBhe2JhY2tncm91bmQtY29sb3I6I2NjYzt9DQojbmF2aWdhdGlvbiBkaXYubWVudS1uYW1lLXByaW1hcnktbGlua3MgbGkgYTpob3ZlcntiYWNrZ3JvdW5kLWNvbG9yOiNjY2M7fQ0KI25hdmlnYXRpb24gZGl2Lm1lbnUtbmFtZS1wcmltYXJ5LWxpbmtzIGxpLm1lbnUtbWxpZC0yMzkzID4gYTpob3Zlcnt9DQoNCiNib3R0b21iYXJ7cG9zaXRpb246cmVsYXRpdmU7cGFkZGluZzoyZW0gMCA2ZW0gMDt6LWluZGV4OjUwMDt3aWR0aDoxMy41ZW07Zm9udC1zaXplOjEzMCU7fQ0KI2JvdHRvbWJhciAuY29udGVudHttYXJnaW4tdG9wOi44ZW07fQ0KI2JvdHRvbWJhciBhe2xpbmUtaGVpZ2h0OjEuMmVtO3BhZGRpbmc6M3B4IDNweCAzcHggM3B4O3RleHQtZGVjb3JhdGlvbjpub25lO30NCiNib3R0b21iYXIgYTpob3ZlciwNCiNib3R0b21iYXIgYS5hY3RpdmV7Y29sb3I6I2Q4M2IyYzt9DQojYm90dG9tYmFyIGJ7Zm9udC1zaXplOjEzMyU7fQ0KDQo8L3N0eWxlPg==\"/>\n" +
            "            <L7p:VariableToSet stringValue=\"website_style\"/>\n" +
            "        </L7p:SetVariable>\n" +
            "        <L7p:SetVariable>\n" +
            "            <L7p:Base64Expression stringValue=\"PCFET0NUWVBFIGh0bWwgUFVCTElDICItLy9XM0MvL0RURCBYSFRNTCAxLjAgU3RyaWN0Ly9FTiINCiJodHRwOi8vd3d3LnczLm9yZy9UUi94aHRtbDEvRFREL3hodG1sMS1zdHJpY3QuZHRkIj4NCjxodG1sIGNsYXNzPSJqcyIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzE5OTkveGh0bWwiIHhtbDpsYW5nPSJlbiIgZGlyPSJsdHIiIGxhbmc9ImVuIj4NCjxoZWFkPg0KICAgIDxtZXRhIGh0dHAtZXF1aXY9IkNvbnRlbnQtVHlwZSIgY29udGVudD0idGV4dC9odG1sOyBjaGFyc2V0PXV0Zi04Ij4NCiAgICA8dGl0bGU+JHt0aGlzLmFwcC5uYW1lfTwvdGl0bGU+DQo8L2hlYWQ+DQoke3dlYnNpdGVfc3R5bGV9DQo8Ym9keSBjbGFzcz0ic2lkZWJhci1sZWZ0IGNyb24tY2hlY2stcHJvY2Vzc2VkIj4NCjxkaXYgaWQ9IndyYXBwZXIiPg0KPGRpdiBpZD0ibWVudWJrZyI+PC9kaXY+DQo8ZGl2IGlkPSJjb250YWluZXIiPg0KPGRpdiBpZD0iaGVhZGVyIj4NCiAgICA8ZGl2IGlkPSJsb2dvLWZsb2F0ZXIiPg0KCQk8YSB0aXRsZT0iIiBocmVmPSIke3JlcXVlc3QudXJsLnBhdGh9Ij48aW1nIGlkPSJsb2dvIiAgaGVpZ2h0PSI1MyIgd2lkdGg9IjE1OSIgYWx0PSJMYXllcjciIHNyYz0iZGF0YTppbWFnZS9wbmc7YmFzZTY0LGlWQk9SdzBLR2dvQUFBQU5TVWhFVWdBQUFKOEFBQUExQ0FNQUFBQm9Rc2xhQUFBQUdYUkZXSFJUYjJaMGQyRnlaUUJCWkc5aVpTQkpiV0ZuWlZKbFlXUjVjY2xsUEFBQUFNQlFURlJGRkJBTWpJNk83cU9sYTI5dzV1Zm45TWpKNW1GZ3lNaklUMUpTNFR3dDFkZll0TGU0K2ZuNTgvVDBwNmluWFdObStlVGwvdno3K3YzK3E3cTgrdno1K3Uvdy9mbjd3TVBFNm9lSWdJU0cwZExTK2Z2OC9mei85dmo2Ny9Eem82R2ZWVnhmOS9qM3E2NncrZmYwdWJ1OW5xS2tSRUU5Mjl6ZHk4M092OEMrOWZYMTdlN3U2dXZyNHVQazVFeElkM3Q3cnEyclhsdFhob1NCOTlqYWxaaVozK0RnTHl3b1pXUmhzck95OGZIeG5aNmQrUGo3Ky96Ny9QejgvZjM5Ly8vL0FTRERZd0FBREZwSlJFRlVlTnJVV1d0N3Fqb1dOcUxGZWdGaEtCWjMyb3ExRmF1VTNZcHVXNU9zLy8rdlpxMGtJTFoyNW5ubXc1eHpjczYyR0lKNTg2NzdvZ1YvNzlINlIrQlRVa2twaFZENkd3ZWhGTWhBcWI4SlBnVUtFVWtKc0o0YzVvdWNBRXNoYVFJdjFWK1BUeGwwZDVPRXNZUWxneGZnZ1FqMXJQcHJFYllxOFFLSWFabDhac3RsMzJQTTVVRFFDSjhlZnprK2dKMlhNTmRNTGxuaW9aQVJtelFVL25VQXJYd2h6SkprRUFPWElsQ1NpMFhDc2tmU1FRV2FRdmovQU5TN3lPLzJ5L2w2L3JuUlZxSUN4QU44VTVZZFE2Mm05MzlDK0RQdjZvZHJZd3ZmOFVrK0dpRW9kREN4aUJYQ1JGUzdUdDVmZHNqWGtKQTF5Sys3Zk45ZFdVeXF3bWVWdDlwVk5VZmwyaG9UeWpKUjM5YjRBbjR6ZmdRZWNhRUNycmdJT01MS0Z5eEp5c2thMFpQelVlZG4vV0kwcCswMUxERGJHTnRYcCtYcXpOcVVPVTBOVDlvSitSV2ZpdHV2dCtNSXRTOEt3b0NneU0ycUtEeDN4Wnl5bjJvejF0NVFuVW5OL0RaODIxNXZKSnZFV0FkbXI0U3NWMVdjNmNmbDJZTk4rVzZ2cnErdjJ6Y2hpQUFDQkRQS21EUFFkemJNV1d4Q2E4b25RS0EzcktmTS9xZlAwMTF6Tkh0RG1tblpQTVdGSVdtYzQzdEZmTmRYM1VjRmtlQXlMLzJuRDJKTkJnQnVrVXp2eUpJMWg2cnk1cktHSXB1QURBejdXWCtUTmI3R0FVUURkUFZEMVZKUUpuaForUnA4MTllM04xeHk5VkVrNVAwNDJvdFFBbDZlbkRrb0RSQXNDRU9MYXNySlRndWhUNitqbzdUWDFhelFmNFRHWlNjYTRQUTU2a2R3djIvODRYZ2RjeDUwbGdvRWo1UUkwRjRvWjFnK1FZeGlGeUFxSGFIZmpYQS9YQjFvV0lUOWx4aUs0UkNQUkVpRytJbi82VU5pcU1UdmRGZlBSMUpHMFRFMGl5SzlEbjlIeFhFVTR3aGlFZUFmeGIvcW54NVhZWWg3eGZnTENwOElTSnNEY3g2QkY0cHpmVmJBMDIwbXJqdEZueVJCUnFBem5rN211bTZXNHhsZThNSmRJejFDYmFhdTJ4OUJ4NjFHSDJkaDZtYjZ5M1N0UWg0ckNBS1EzZWJZM3NBRitlSVk4NUE4RFI1SUJzaElFQWp0VzVSd2wwWlFXa1lJOGRCcXRaNUJLNmJVMGdjSFovYXROL1FQblI1ZUZTK2tSQVZPRmkvd29PL1J2ejhZbCtEWmZtM3RlMmlISVc3QjFYVnp2SGJEaS95aEZWK2hHb1lTTXhqRUZSTmF5RXRuRUhvKzJ5bEpISklhQ2ZodDhCRzE1TkFGYk41b3cxYXZBeXFsbTYwbEt1OEhJVDBBZkxhcThmWUlCcDhkZTQraUtqNy9CUjljMGovRTE2VzdiVkE4TkJvTkw1N2ordzY4N0o1OGI0U1NESVcyWTgyZkVsYW5VVTFYWnRQbkZhcHBoNjQvWHdBOFpNbjVBTU5mK2M3ZTM3MVVDSTN2YzVNUGVuUWdDREVxeFB3THZxYitqYS9POGFFZWJrbW1LQW51RnI1UCtBUjZ4YVNZWUxRaDhVckxYeWhqUEFVRlFYaEhibWkvZHpUOWlPN3UwUXZRaEljK3ZTUWdqeDg0Um1TYWhPODNia0R3bjljUTQxdzRyZ2FCdVkwYjhnMzU3VmQ4dU9JUmpaYm56UEVOdmdDTmVuTnd5ZzBRcTVhL29ISUpTajJpZUowUzkzdkRmV0ZEQ0E1OGluL2VkaEJxL25xQkRuYlM2dDhEOHV6aXhmT0c0L05oSFN2YlJNOFlnbVorZFZMQUd0LzE3WmhEdi9CbkJoODZBUHo1ZE1reXJyUmJOUHdKVFNaNkRNaHdLNVlqWDg5b3dVSHM2ZHVPNFNrdy9Kbjl1Y1huSWI0SjRmc0FOR0VWb1pESm14RzgxMjNZOUMraENydXYzL0JkdDJIMGFkQVJmeEtmUnArNVNWeUk1Y2wrY1RkQjV3ZTJiKzEvRTZEOWdxeDZRK2I1WU9oRFFNVGZjN2JDZ1c2SFcvbUNodjJHZ3RHdW1ZZGhEQVRrdFJ0RXZKRWZTQjZxN2UwM2ZGZmp6RG5oRTlhcFQzZEEvcmppTDR6STV3aUFQN2hUcmpjc0tPT1J6THFRMytTQXRQM3VuMmxNSTJudy9mbjhMRWo5TWx3ZUNSUFpqQ1cwWTJrVEppdGZqQUxoWS9mcUM3N3I3cjhxZUFaZmhQQW1aWkxCaVQvaVRwQjFrTVcrcmFhRXFrY2lWNXBBUTU4dy9Ka3hpV1hsWDNERjNza3BCZFJueEdnZjNock5EN2hzK0JkU2QxU3Vtelp5MnozRDE2dncvUUU4STBELzA1bjFQQXh6dGYvajJqdEw3VWlJSUdKa2lSSU40WDFQQUI2R1FFNUs4L2RlRk1YN0xsREd2enkvMFdjUG5hTkdSd2xEWlJ0WWFEVGxpeXJMTWQ1Q3VHMjNiN1lYOFNVb1Njam54V3ptOTd3WGltOE4vVk5HdkxYSEpjOFJDbTNDaUpYSU1mWUw2VXVheGp3eS9MSGMxZkx0RThIRzU0KzE4aW5LUTRNbVBzenQwZFBGK2lxK3VvVFB4ZmpxTW0zTVBlOE8vVjNGWDJneXVnOHl4SUl4UmtFRXlaYWhnais0T3h1UlB6SDhXZnNsYVZuNzBDSG5YYXVyVHFpTThtbnllTlN3RDFRQml2d2l4clNGZDcvam15VnBBSzQxRm8xUFdQNVVxRDBnTFBhMEVZNFZUYWU2aEhGSXZLbkJaL2pEQ2dLTFF5bXMvd1ByZGloQ2toQm81NnNiRENka0VHZjhZZFdHa2lKRjRnRzBYOC94emZ3eWg3aFQyWExQTzBKdHY1WVNuUVo0cEFOOUNyZ1paZUVWUHZMaFRmOEgxbjdKLytrZnljSDRlRTFmbHhKeXJrQis2VjloMnNTeCtFQlhPQjUzYnh2NFpuNHlXT2FqeDNrbDZwbEhuR2orOWg2TmgxeW16OXBSVU5xRlNQY01pQTV5enArMDF1cmZzMTd0ZFd6OGZiTHhBLzJsRHVOd280MGprczJtU3V0VTZtRG1URG5WemRWcmU5dStzdmhtdnZQVVh6NFZnNGlkOE4wUkVPOWtFQWZsSWxYRm1xcGxVWWxNMXZnSWEzbGF2UmlxbXIvOGp6a05LVW5RTmRxbi9TQ2MxUittcHFDR1VCRHIvS1c3dlgzRnMweDhoMlg1d08vTkhGWGpRL3NkcGNUZm5nYjVrQldVZVBFQU9rMlk0UHh6U2ljb2FKS3VVT0c4bGxtTS8wK0dFdE9HdmNhWE1wenlYMHluZ3FUV0JhbWFOWHFyV1ZoVGJoZFYrY3YydHN0SGswV2VKYVNFRFh5enczcmFRVmY0enFyUmg5LzRpVjZQV09zOHZMT3lReHdjR0h2UGhwVHVZM1JPNnRVN3hQS0F0NmFVUnl3WnJrWUZKSXNqbVcyMXg2L0x0N1ArcVJKWW5NZU4vQVhMOUE3cnpVejhxUERObkt6ajV1ZGRFdk1sVWpyUHRFN0VGbUFxTWdBYlExaDY4RGpxdEZxYngrc1d6aHRtRFh3cUNNaUV1NmZ3aXlRVnZTcStXWHl6MlFCMkIrTEtWbUttRU1NTHJuUkNxNGVlVjFVUlo1SWNrNGNOVlpYUm1wdkNya1QrWGcyK1FGN0doM2VVaWtVamZ3bEdaZVdnVC9KOXV0ZG5yd0RVdjYrTE9BTkdhbmRXTFJDbnNyRytQQU5KZzRKZXQ5M3Uzb1JWTCtYY1BrQjdIUlh3NklmOFJScDhSY1loRXVlbmw2cXVjeFZ0VkpkUnBxa3RxNlU2c2JYek5xTFY0QU9qY3FpMVhQM1V2MGZwWWhGNk9UOXdPT0Z6RGltRlZqazhVcDBicFZUQTRpWjNSNUpvZEF4K0hkRkRpU2lTUjhRcG96VDlGUVJCTk1SU04rRGg4VTVFRkFXR3VCekw2V0g4SzZhMVE0VEwwNVR1eWFvcDhnTSt2QkZFUCtDREo5OHZONkh1RSswV2g4T3FBNXY1WVhHUHoweFlPVDFDdXNxZ1AwbGgyQi9rbzhNYWZVSEdtSnRpZGIvS0tZUGVlR3lPdFFFZkxkQzhNZHhQSnFnb3kvbkJmWUcwWHpJWHF0YkRqL3pwTkNZZS81Qy9qQmI1a0pPU1NSaE5ITFlaUWQ5M2Q2bU1YU2YzaWh4R3lSTjRDVzQybUQzZCt6bUl6TzlQL0NuQW9NREFBdXRQdGlreGN6M2kyckpjQXpCcW9oeWNhVDZFRGZ1OTlEZXFxbVIreEVmbExiK1l2emdUVUVPdEtoVExZVmVVdUdYZnozTEVWeWFqSFRMelViQk9pZWxLT3VnbGZjVEg1OG5IeGhta2lNOUZCOVF2NW1yaGRPQll6SWZySGFZYkxFRW5PaSttRzB4ajNwUHB2UWhNRStxODdkbjYwbHNOd2t2NWk1OGNkWjlBNkVSWGFud0srck9rM0tpWUpSK2sxS09rR0NRTStUdjRIbk55R1RJMnN2Z3lpQWdmVEJEMjBUOTBQcjBQTVBnR2ZuTEF4RzdKL09RbE1EbSsrdkg5Rm5WYjBJdUYzZXV2K1F1N1Y5YWJtVVROOERkMThpT1h2eGc3NWhnQzdvc3lmU0w1em90K3o4bTVtaWZIa2JNWUlqNFVNdlFSeU1LWDZxN3crSXAxb09KdmczWGF4MlM5OUplYUFWQS82eCttVmhKQkJ2RzJtYi80c3dJMW1Tby8reFlNUThUU1liaDAybU5QVXhDVEdmTFdnWHVuaEtkaUJFUFBPUmIrTHBEOTJaejVmZFF4UHlrbmNGODZpK1FUa3dYUFdiQUNvU1dvc3pDZmxkNE9Pc25ud1YvYlYxZy82eCtsRFFGd3JLWnZ1bFgrNHZkUUhoRmd1b2poSmJEOVF6endsRlIrc01KQUY5MjUyU1RIVU8vMlZUOGJ3bkE1Z2VYaVE4bzBjMWZMUndXNzFXcUI4WVk2V0dqdFlqM0pwdTRJSUtPUDNXRGlidkNSbGRzWDVvM1FmM2gvYVhxak9zaUhZNnhEWDhlVDNxeGMzZ0hJWm9lVGFFeUgxSEJMMHhTTkJzUkl0NmlGYWVoSmdTRXNwVGNWS2swcHRBNXhXVXFDR3cxMTJpVkcxQWRVcVY2THR5aDFlVWxCeVV0dnFsb1hYaEZ3VXNSZzI5N0c2eW1aMTZtQnJPb0dzbW5PNkI2TnN2MXpxQnZRMHJRc2RVTmNMOU9tcFV4SHh4N1VObHUxMWxENm9NeUMvL2IrVjNjZmpSMURhTDIyUE85aU54dmV0c2RyTzdLNnF5eDBGMWlhQnJNUTByYVZxLzYxZmxiVTRidHVzWXZMYi9rdXZKOXVOT09wQ1JKRWxjOVU5ZnNNZVhwNWFMU21sbjdWZjY5YjMrWTFudTFhYXlhMW00TnE3YWxsRDVjUS9qUGVuLzk5eDc4RkdBQ0Q5Z1pIaXo2ZUZnQUFBQUJKUlU1RXJrSmdnZz09Ij48L2E+DQoJPC9kaXY+CQ0KPC9kaXY+DQo8ZGl2IGlkPSJjb250ZW50Ij4NCgk8aWZyYW1lIHN0eWxlPSJkaXNwbGF5Om5vbmUiIHNyYz0iIiBpZD0ibXlpZnJhbWUiIGZyYW1lYm9yZGVyPSIwIiBzY3JvbGxpbmc9ImF1dG8iPg0KCTwvaWZyYW1lPg0KCQk8ZGl2IGlkPSJjb250ZW50X2xhbmRpbmdfcGFnZSI+ICAgICAgICAgICAgICANCgkJPGRpdiBjbGFzcz0idmlldyB2aWV3LXByb2R1Y3RzMyB2aWV3LWlkLXByb2R1Y3RzMyB2aWV3LWRpc3BsYXktaWQtcGFnZV8xIHZpZXctZG9tLWlkLTEiPgkJDQoJCTxkaXYgY2xhc3M9ImNvbnRhaW5lciI+DQoJCTxkaXYgY2xhc3M9InRpdGxlLWJveCI+DQoJCTxoMT4ke3RoaXMuYXBwLm5hbWV9PC9oMT4NCgkJPGJyPg0KCQk8aDM+T0F1dGggdmVyc2lvbjogMS4wPC9oMz4NCgkJPGJyPg0KCQkNCgkJPHA+PC9wPg0KCQkJDQoJCTwvZGl2Pg0KDQoJCSA8ZGl2IGNsYXNzPSJ2aWV3IHZpZXctcHJvZHVjdHMzIHZpZXctaWQtcHJvZHVjdHMzIHZpZXctZGlzcGxheS1pZC1wYWdlXzEgdmlldy1kb20taWQtMSI+CQ0KPGRpdiBjbGFzcz0iY29udGFpbmVyIj4=\"/>\n" +
            "            <L7p:VariableToSet stringValue=\"website_top\"/>\n" +
            "        </L7p:SetVariable>\n" +
            "        <L7p:SetVariable>\n" +
            "            <L7p:Base64Expression stringValue=\"PC9kaXY+DQo8L2Rpdj48L2Rpdj4NCjwvYm9keT4NCjwvaHRtbD4=\"/>\n" +
            "            <L7p:VariableToSet stringValue=\"website_bottom\"/>\n" +
            "        </L7p:SetVariable>\n" +
            "        <L7p:HardcodedResponse>\n" +
            "            <L7p:AssertionComment assertionComment=\"included\">\n" +
            "                <L7p:Properties mapValue=\"included\">\n" +
            "                    <L7p:entry>\n" +
            "                        <L7p:key stringValue=\"LEFT.COMMENT\"/>\n" +
            "                        <L7p:value stringValue=\"200\"/>\n" +
            "                    </L7p:entry>\n" +
            "                </L7p:Properties>\n" +
            "            </L7p:AssertionComment>\n" +
            "            <L7p:Base64ResponseBody stringValue=\"JHt3ZWJzaXRlX3RvcH0KCQkgCjwhLS1vYXV0aF9jb250ZW50X3BsYWNlaG9sZGVyLS0+Cgoke3dlYnNpdGVfYm90dG9tfQ==\"/>\n" +
            "            <L7p:ResponseContentType stringValue=\"text/html; charset=UTF-8\"/>\n" +
            "        </L7p:HardcodedResponse>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>\n";

    private final static String CANNED_L7_POLICY_ROUTING_URLS_JDBC = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:CommentAssertion>\n" +
            "            <L7p:Comment stringValue=\"Implements the token endpoint for OAuth 1.0\"/>\n" +
            "        </L7p:CommentAssertion>\n" +
            "        <L7p:CommentAssertion>\n" +
            "            <L7p:Comment stringValue=\"This endpoint issues an access token\"/>\n" +
            "        </L7p:CommentAssertion>\n" +
            "        <L7p:SslAssertion/>\n" +
            "        <L7p:Include>\n" +
            "            <L7p:PolicyGuid stringValue=\"f0eb1f7b-392b-40a4-9f4f-46d00ffad3d3\"/>\n" +
            "        </L7p:Include>\n" +
            "        <L7p:Include>\n" +
            "            <L7p:PolicyGuid stringValue=\"60cec430-0767-429c-8eff-62891c2eb343\"/>\n" +
            "        </L7p:Include>\n" +
            "        <L7p:Include>\n" +
            "            <L7p:PolicyGuid stringValue=\"35aa8869-8319-44e0-a6b6-2ed2ce59001e\"/>\n" +
            "        </L7p:Include>\n" +

            "        <wsp:All wsp:Usage=\"Required\">\n" +
            "            <L7p:SetVariable>\n" +
            "                <L7p:Base64Expression stringValue=\"JHtwYXJhbXN9\"/>\n" +
            "                <L7p:ContentType stringValue=\"application/x-www-form-urlencoded\"/>\n" +
            "                <L7p:DataType variableDataType=\"message\"/>\n" +
            "                <L7p:VariableToSet stringValue=\"params\"/>\n" +
            "            </L7p:SetVariable>\n" +
            "                    <L7p:JdbcQuery>\n" +
            "                        <L7p:AssertionFailureEnabled booleanValue=\"false\"/>\n" +
            "                        <L7p:ConnectionName stringValue=\"OAuth\"/>\n" +
            "                        <L7p:SqlQuery stringValueReference=\"inline\"><![CDATA[select ock.client_key, ock.secret,\n" +
            "                    ock.scope, ock.callback, ock.environment, ock.expiration, ock.status, ock.client_ident, ock.created,\n" +
            "                    ock.created_by, ock.client_name\n" +
            "                    from oauth_client_key ock\n" +
            "                    where ock.client_key = ${request.http.parameter.client_key}]]></L7p:SqlQuery>\n" +
            "                    </L7p:JdbcQuery>\n" +
            "            <L7p:HttpRoutingAssertion>\n" +
            "                <L7p:CurrentSecurityHeaderHandling intValue=\"3\"/>\n" +
            "                <L7p:FailOnErrorStatus booleanValue=\"false\"/>\n" +
            "                <L7p:ProtectedServiceUrl stringValue=\"${host_oauth_ovp_server}/oauth/validation/validate/v1/signature\"/>\n" +
            "                <L7p:ProxyPassword stringValueNull=\"null\"/>\n" +
            "                <L7p:ProxyUsername stringValueNull=\"null\"/>\n" +
            "                <L7p:RequestHeaderRules httpPassthroughRuleSet=\"included\">\n" +
            "                    <L7p:Rules httpPassthroughRules=\"included\"/>\n" +
            "                </L7p:RequestHeaderRules>\n" +
            "                <L7p:RequestMsgSrc stringValue=\"params\"/>\n" +
            "                <L7p:RequestParamRules httpPassthroughRuleSet=\"included\">\n" +
            "                    <L7p:Rules httpPassthroughRules=\"included\"/>\n" +
            "                </L7p:RequestParamRules>\n" +
            "                <L7p:ResponseHeaderRules httpPassthroughRuleSet=\"included\">\n" +
            "                    <L7p:Rules httpPassthroughRules=\"included\"/>\n" +
            "                </L7p:ResponseHeaderRules>\n" +
            "                <L7p:ResponseMsgDest stringValue=\"validationResult\"/>\n" +
            "                <L7p:SamlAssertionVersion intValue=\"2\"/>\n" +
            "            </L7p:HttpRoutingAssertion>\n" +
            "            <L7p:ResponseXpathAssertion>\n" +
            "                <L7p:VariablePrefix stringValue=\"\"/>\n" +
            "                <L7p:XmlMsgSrc stringValue=\"validationResult\"/>\n" +
            "                <L7p:XpathExpression xpathExpressionValue=\"included\">\n" +
            "                    <L7p:Expression stringValue=\"/validation/result[text()='valid']\"/>\n" +
            "                    <L7p:Namespaces mapValue=\"included\">\n" +
            "                        <L7p:entry>\n" +
            "                            <L7p:key stringValue=\"s\"/>\n" +
            "                            <L7p:value stringValue=\"http://schemas.xmlsoap.org/soap/envelope/\"/>\n" +
            "                        </L7p:entry>\n" +
            "                    </L7p:Namespaces>\n" +
            "                </L7p:XpathExpression>\n" +
            "            </L7p:ResponseXpathAssertion>\n" +
            "            <L7p:assertionComment>\n" +
            "                <L7p:Properties mapValue=\"included\">\n" +
            "                    <L7p:entry>\n" +
            "                        <L7p:key stringValue=\"RIGHT.COMMENT\"/>\n" +
            "                        <L7p:value stringValue=\"Validate the token including the signature\"/>\n" +
            "                    </L7p:entry>\n" +
            "                </L7p:Properties>\n" +
            "            </L7p:assertionComment>\n" +
            "        </wsp:All>\n" +
            "        <L7p:SetVariable>\n" +
            "            <L7p:Base64Expression stringValue=\"YXQ=\"/>\n" +
            "            <L7p:VariableToSet stringValue=\"token_type\"/>\n" +
            "        </L7p:SetVariable>\n" +
            "        <L7p:Include>\n" +
            "            <L7p:PolicyGuid stringValue=\"585c3a3d-8fd7-4337-8d91-46846bfc4194\"/>\n" +
            "        </L7p:Include>\n" +
            "        <L7p:SetVariable>\n" +
            "            <L7p:Base64Expression stringValue=\"dG9rZW49JHtnZW5Ub2tlbn0mc2VjcmV0PSR7Z2VuU2VjcmV0fSZleHBpcmF0aW9uPSR7Z2VuRXhwaXJhdGlvbn0mY2xpZW50X2tleT0ke29hdXRoX2NvbnN1bWVyX2tleX0mdGVtcF90b2tlbj0ke29hdXRoX3Rva2VufSZzdGF0dXM9RU5BQkxFRA==\"/>\n" +
            "            <L7p:ContentType stringValue=\"application/x-www-form-urlencoded\"/>\n" +
            "            <L7p:DataType variableDataType=\"message\"/>\n" +
            "            <L7p:VariableToSet stringValue=\"toStore\"/>\n" +
            "        </L7p:SetVariable>\n" +
            "                    <L7p:JdbcQuery>\n" +
            "                        <L7p:AssertionFailureEnabled booleanValue=\"false\"/>\n" +
            "                        <L7p:ConnectionName stringValue=\"OAuth\"/>\n" +
            "                        <L7p:SqlQuery stringValueReference=\"inline\"><![CDATA[select ock.client_key, ock.secret,\n" +
            "                    ock.scope, ock.callback, ock.environment, ock.expiration, ock.status, ock.client_ident, ock.created,\n" +
            "                    ock.created_by, ock.client_name\n" +
            "                    from oauth_client_key ock\n" +
            "                    where ock.client_key = ${request.http.parameter.client_key}]]></L7p:SqlQuery>\n" +
            "                    </L7p:JdbcQuery>\n" +
            "        <L7p:HttpRoutingAssertion>\n" +
            "            <L7p:HttpMethod httpMethod=\"POST\"/>\n" +
            "            <L7p:ProtectedServiceUrl stringValue=\"${host_oauth_tokenstore_server}/oauth/tokenstore/store\"/>\n" +
            "            <L7p:ProxyPassword stringValueNull=\"null\"/>\n" +
            "            <L7p:ProxyUsername stringValueNull=\"null\"/>\n" +
            "            <L7p:RequestHeaderRules httpPassthroughRuleSet=\"included\">\n" +
            "                <L7p:Rules httpPassthroughRules=\"included\"/>\n" +
            "            </L7p:RequestHeaderRules>\n" +
            "            <L7p:RequestMsgSrc stringValue=\"toStore\"/>\n" +
            "            <L7p:RequestParamRules httpPassthroughRuleSet=\"included\">\n" +
            "                <L7p:Rules httpPassthroughRules=\"included\"/>\n" +
            "            </L7p:RequestParamRules>\n" +
            "            <L7p:ResponseHeaderRules httpPassthroughRuleSet=\"included\">\n" +
            "                <L7p:Rules httpPassthroughRules=\"included\"/>\n" +
            "            </L7p:ResponseHeaderRules>\n" +
            "            <L7p:ResponseMsgDest stringValue=\"toStoreResult\"/>\n" +
            "            <L7p:SamlAssertionVersion intValue=\"2\"/>\n" +
            "        </L7p:HttpRoutingAssertion>\n" +
            "        <L7p:ComparisonAssertion>\n" +
            "            <L7p:Expression1 stringValue=\"${toStoreResult.mainpart}\"/>\n" +
            "            <L7p:Expression2 stringValue=\"persisted\"/>\n" +
            "            <L7p:Predicates predicates=\"included\">\n" +
            "                <L7p:item binary=\"included\">\n" +
            "                    <L7p:RightValue stringValue=\"persisted\"/>\n" +
            "                </L7p:item>\n" +
            "            </L7p:Predicates>\n" +
            "        </L7p:ComparisonAssertion>\n" +
            "        <wsp:OneOrMore wsp:Usage=\"Required\">\n" +
            "            <wsp:All wsp:Usage=\"Required\">\n" +
            "                <L7p:SetVariable>\n" +
            "                    <L7p:Base64Expression stringValue=\"dGVtcF90b2tlbj0ke29hdXRoX3Rva2VufQ==\"/>\n" +
            "                    <L7p:ContentType stringValue=\"application/x-www-form-urlencoded\"/>\n" +
            "                    <L7p:DataType variableDataType=\"message\"/>\n" +
            "                    <L7p:VariableToSet stringValue=\"toStore\"/>\n" +
            "                </L7p:SetVariable>\n" +
            "                <L7p:HttpRoutingAssertion>\n" +
            "                    <L7p:HttpMethod httpMethod=\"POST\"/>\n" +
            "                    <L7p:ProtectedServiceUrl stringValue=\"${host_oauth_tokenstore_server}/oauth/tokenstore/delete\"/>\n" +
            "                    <L7p:ProxyPassword stringValueNull=\"null\"/>\n" +
            "                    <L7p:ProxyUsername stringValueNull=\"null\"/>\n" +
            "                    <L7p:RequestHeaderRules httpPassthroughRuleSet=\"included\">\n" +
            "                        <L7p:Rules httpPassthroughRules=\"included\"/>\n" +
            "                    </L7p:RequestHeaderRules>\n" +
            "                    <L7p:RequestMsgSrc stringValue=\"toStore\"/>\n" +
            "                    <L7p:RequestParamRules httpPassthroughRuleSet=\"included\">\n" +
            "                        <L7p:Rules httpPassthroughRules=\"included\"/>\n" +
            "                    </L7p:RequestParamRules>\n" +
            "                    <L7p:ResponseHeaderRules httpPassthroughRuleSet=\"included\">\n" +
            "                        <L7p:Rules httpPassthroughRules=\"included\"/>\n" +
            "                    </L7p:ResponseHeaderRules>\n" +
            "                    <L7p:ResponseMsgDest stringValue=\"toStoreResult\"/>\n" +
            "                    <L7p:SamlAssertionVersion intValue=\"2\"/>\n" +
            "                </L7p:HttpRoutingAssertion>\n" +
            "            </wsp:All>\n" +
            "            <L7p:AuditDetailAssertion>\n" +
            "                <L7p:Detail stringValue=\"The temporary token '${oauth_token}' could not be deleted.\"/>\n" +
            "                <L7p:Level stringValue=\"WARNING\"/>\n" +
            "            </L7p:AuditDetailAssertion>\n" +
            "            <L7p:assertionComment>\n" +
            "                <L7p:Properties mapValue=\"included\">\n" +
            "                    <L7p:entry>\n" +
            "                        <L7p:key stringValue=\"RIGHT.COMMENT\"/>\n" +
            "                        <L7p:value stringValue=\"Delete the used authorized_request_token\"/>\n" +
            "                    </L7p:entry>\n" +
            "                </L7p:Properties>\n" +
            "            </L7p:assertionComment>\n" +
            "        </wsp:OneOrMore>\n" +
            "        <L7p:HardcodedResponse>\n" +
            "            <L7p:AssertionComment assertionComment=\"included\">\n" +
            "                <L7p:Properties mapValue=\"included\">\n" +
            "                    <L7p:entry>\n" +
            "                        <L7p:key stringValue=\"LEFT.COMMENT\"/>\n" +
            "                        <L7p:value stringValue=\"200\"/>\n" +
            "                    </L7p:entry>\n" +
            "                </L7p:Properties>\n" +
            "            </L7p:AssertionComment>\n" +
            "            <L7p:Base64ResponseBody stringValue=\"b2F1dGhfdG9rZW49JHtnZW5Ub2tlbn0mb2F1dGhfdG9rZW5fc2VjcmV0PSR7Z2VuU2VjcmV0fQ==\"/>\n" +
            "            <L7p:EarlyResponse booleanValue=\"true\"/>\n" +
            "            <L7p:ResponseContentType stringValue=\"application/x-www-form-urlencoded\"/>\n" +
            "        </L7p:HardcodedResponse>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>\n";
}

