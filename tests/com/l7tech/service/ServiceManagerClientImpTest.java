package com.l7tech.service;

import com.l7tech.adminws.ClientCredentialManager;
import com.l7tech.adminws.service.Client;
import com.l7tech.util.Locator;
import java.net.PasswordAuthentication;

/**
 * Created by IntelliJ IDEA.
 * User: flascell
 * Date: Jul 3, 2003
 * Time: 10:22:59 AM
 * To change this template use Options | File Templates.
 */
public class ServiceManagerClientImpTest {

    public static void testUpdateService(ServiceManagerClientImp manager) throws Exception {
        PublishedService svc = testCreateService(manager);
        System.out.println("change something in service");
        svc.setName("changed###");
        System.out.println("update service id=" + svc.getOid());
        manager.update(svc);
        System.out.println("done id=" + svc.getOid());
    }

    public static void testUpdateServiceOfPreviouslySaved(ServiceManagerClientImp manager) throws Exception {
        System.out.println("retrieving existing service");
        PublishedService svc = manager.findByPrimaryKey(5111809);
        System.out.println("change something in service");
        svc.setName("Jimmy Hendrix");
        System.out.println("update service id=" + svc.getOid());
        manager.update(svc);
        System.out.println("done id=" + svc.getOid());
    }

    public static PublishedService testCreateService(ServiceManagerClientImp manager) throws Exception {
        System.out.println("creating service");
        PublishedService originalService = new PublishedService();
        String differentStringEverytime = Long.toString(System.currentTimeMillis());
        originalService.setName("test" + differentStringEverytime);
        originalService.setPolicyXml("<test" + differentStringEverytime + "/>");
        originalService.setSoapAction("test:" + differentStringEverytime);
        originalService.setUrn(":test:" + differentStringEverytime);
        originalService.setWsdlUrl("http://test" + differentStringEverytime + "?wsdl");
        originalService.setWsdlXml("<test" + differentStringEverytime + "/>");
        System.out.println("saving service");
        long res = manager.save(originalService);
        System.out.println("saved with id=" + res);
        return originalService;
    }

    public static void main(String[] args) throws Exception {
        ClientCredentialManager credsManager = (ClientCredentialManager)Locator.getDefault().lookup(ClientCredentialManager.class);
        PasswordAuthentication creds = new PasswordAuthentication("ssgadmin", "ssgadminpasswd".toCharArray());
        credsManager.login(creds);

        ServiceManagerClientImp manager = new ServiceManagerClientImp();

        testUpdateServiceOfPreviouslySaved(manager);

        System.exit(0);
    }
}
