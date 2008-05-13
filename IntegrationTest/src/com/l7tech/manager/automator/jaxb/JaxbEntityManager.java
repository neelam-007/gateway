package com.l7tech.manager.automator.jaxb;

import com.l7tech.common.policy.PolicyAdmin;
import com.l7tech.common.policy.Policy;
import com.l7tech.common.policy.PolicyType;
import com.l7tech.common.util.XmlUtil;
import com.l7tech.common.security.TrustedCertAdmin;
import com.l7tech.common.security.TrustedCert;
import com.l7tech.common.transport.jms.JmsAdmin;
import com.l7tech.common.transport.jms.JmsConnection;
import com.l7tech.common.transport.jms.JmsEndpoint;
import com.l7tech.common.transport.TransportAdmin;
import com.l7tech.common.transport.SsgConnector;
import com.l7tech.common.transport.SsgConnectorProperty;
import com.l7tech.common.xml.schema.SchemaAdmin;
import com.l7tech.common.xml.schema.SchemaEntry;
import com.l7tech.admin.AdminContext;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.Include;
import com.l7tech.policy.assertion.JmsRoutingAssertion;
import com.l7tech.policy.assertion.identity.SpecificUser;
import com.l7tech.policy.assertion.identity.MemberOfGroup;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.wsp.WspWriter;
import com.l7tech.service.ServiceAdmin;
import com.l7tech.service.PublishedService;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.identity.internal.InternalGroup;
import com.l7tech.identity.*;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.identity.fed.FederatedIdentityProviderConfig;
import com.l7tech.identity.fed.FederatedGroup;
import com.l7tech.identity.fed.VirtualGroup;
import com.l7tech.identity.fed.FederatedUser;
import com.l7tech.manager.automator.Main;
import com.l7tech.objectmodel.*;
import com.l7tech.cluster.ClusterStatusAdmin;
import com.l7tech.cluster.ClusterProperty;

import javax.xml.bind.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.Result;
import java.io.*;
import java.util.*;
import java.lang.reflect.Method;
import java.lang.reflect.Field;

/**
 * Created by IntelliJ IDEA.
 * User: darmstrong
 * Date: Apr 4, 2008
 * Time: 12:33:05 PM
 * JaxbEntityManager provides functionality to download, upload and delete the following entities from / to an SSG:
 * IdentityProviders
 * Groups and Users including group memberhship for an Internal Identity Provider or a Federated Identity Provider
 * TrustedCerts
 * Policy Fragments
 * Published Services including their private policies.
 * JMS Connections and JMS Endpoints - not successfully tested yet - need JMS jars. Will copy and save to
 * a new SSG ok, but can't be sure I havn't missed something.
 * SchemaEntry's
 *
 * Download will save an xml file per Entity on disk with it's root folder specified in
 * manager_automater.properties jaxb.download.rootdirectory property.
 *
 * To download all of the above listed entites use downloadAllEntities()
 * To upload all of the above listed entites use uploadAllEntities()
 * To delete all of the above listed entites use deleteAllEntities()
 *
 * With any of the above methods, any exception causes execution to stop and no logic is provided to clean up
 * any actions taken, with the exception of TrustedCerts as they can be invalid, processing will continue.
 *
 * The SSG from which you download, upload or delete from is determined by the property ssg.host
 * in manager_automater.properties.
 *
 * A fresh SSG will require a license to be uploaded before any of the Manager api functionality becomes available.
 * Specify the dev license to be uploaded using the property jaxb.upload.licensefile in manager_automater.properties
 */
public class JaxbEntityManager {

    private AdminContext adminContext;
    private TrustedCertAdmin trustedCertAdmin;
    private PolicyAdmin policyAdmin;
    private ServiceAdmin serviceAdmin;
    private IdentityAdmin identityAdmin;
    private ClusterStatusAdmin clusterAdmin;
    private JAXBContext context;
    private Marshaller marshaller;
    private final Unmarshaller unmarshaller;
    private IdentityProviderConfig internalProv;
    private Map<String, IdentityHeader> allGroupMap;
    private String jaxbDir;
    private JmsAdmin jmsAdmin;
    private SchemaAdmin schemaAdmin;
    private Set<String> processedSchemas;
    private Map<Long, IdentityProviderConfig> oldIdToNewForIdentityProvider;
    
    public final static String SOAP_SCHEMA = "soapenv";
    private TransportAdmin transportAdmin;
    private Map<String, JmsEndpoint> endPointMap;
    private Map<String, User> fedUserNameToUserMap;
    private Map<IdentityProviderConfig, Map<String, IdentityHeader>> providerToGroupMap;

    private FilenameFilter xmlFilter = new FilenameFilter(){

                public boolean accept(File dir, String name){
                    if(name.endsWith("xml")){
                        return true;
                    }else{
                        return false;
                    }
                }
            };

    private FilenameFilter nonSVNDirFilter = new FilenameFilter(){
                public boolean accept(File dir, String name){
                    if(name.indexOf(".svn") == -1 ){
                        return true;
                    }else{
                        return false;
                    }
                }
            };

    private FilenameFilter xsdFilter = new FilenameFilter(){
                public boolean accept(File dir, String name){
                    if(name.endsWith("xsd")){
                        return true;
                    }else{
                        return false;
                    }
                }
            };

    public JaxbEntityManager(AdminContext adminContext){

        this.adminContext = adminContext;
        policyAdmin = adminContext.getPolicyAdmin();
        serviceAdmin = adminContext.getServiceAdmin();
        identityAdmin = adminContext.getIdentityAdmin();
        trustedCertAdmin = adminContext.getTrustedCertAdmin();
        jmsAdmin = adminContext.getJmsAdmin();
        schemaAdmin = adminContext.getSchemaAdmin();
        clusterAdmin = adminContext.getClusterStatusAdmin();
        transportAdmin = this.adminContext.getTransportAdmin();
        
        processedSchemas = new HashSet<String>();
        final File baseDir = new File(".");

        //This schema is not actually used
        class MySchemaOutputResolver extends SchemaOutputResolver {
            public Result createOutput( String namespaceUri, String suggestedFileName ) throws IOException {
                return new StreamResult(new File(baseDir,"JaxbEntityManagerSchema.xsd"));
            }
        }

        try{
            //All classes the marshaller / unmarshaller needs to be able to process must be listed here.
            //don't need to list all clases in a hierarchy, just the type of the object you want processed.
            context = JAXBContext.newInstance(Policy.class, JaxbPublishedService.class, IdentityProviderConfig.class, InternalUser.class, InternalGroup.class, JaxbPersistentUser.class, FederatedIdentityProviderConfig.class, LdapIdentityProviderConfig.class, TrustedCert.class, JaxbFederatedIdentityProviderConfig.class, VirtualGroup.class, FederatedGroup.class, FederatedUser.class, JmsConnection.class, JaxbJmsEndpoint.class, SchemaEntry.class, ClusterProperty.class, SsgConnector.class, SsgConnectorProperty.class, JaxbSsgConnectorProperty.class);
            //Here we generate a schema representing all the classes we've told jaxb about
            //This is not needed but is available if you want to validate xml before unmarshalling.
            context.generateSchema(new MySchemaOutputResolver());
            marshaller = context.createMarshaller();
            unmarshaller = context.createUnmarshaller();
            internalProv = this.getInternalIdentityProvider();

        }catch(Exception ex){
            throw new RuntimeException(ex);
        }

        Properties props = Main.getProperties();
        jaxbDir = props.getProperty("jaxb.download.rootdirectory");
        File dir = new File(jaxbDir);
        if(!dir.exists()){
            if(!dir.mkdir()){
                throw new RuntimeException("Cannot create root directory: " + jaxbDir);
            }
        }

        providerToGroupMap = new HashMap<IdentityProviderConfig, Map<String, IdentityHeader>>();

    }

    /*Helper function to get out files from a directory matching the supplied filter
    * 'files' is a Java File which can mean either a file or a directory*/
    private File [] getFilesFromDirectory(String directory, FilenameFilter filter) throws IOException{
        File dir = new File(directory);
        return this.getFilesFromDirectory(dir, filter);
    }

    /*Helper function to get all files matching the filter out of the supplied File*/
    private File [] getFilesFromDirectory(File dir, FilenameFilter filter) throws IOException{

        if(dir.exists() && dir.isDirectory()){
            File [] dirFiles = dir.listFiles(filter);
            return dirFiles;
        }
        throw new RuntimeException("Directory: " + dir.getName() +" not found");
    }

    /**
     * Download all entities were currently able to handle.
     * Entities are downloaded in order of their dependencies.
     *
     */
    public void downloadAllEntities() throws Exception{
        downloadIdentityProviders();//includes all groups and users
        downloadTrustedCerts();
        downloadAllClusterProperties();
        downloadSchemaEntries();
        downloadAllTransports();
        downloadJmsConnectionAndEndpoints();
        downloadAllPublishedServices();//this includes private policies
    }

    /**
     * Upload all entities were currently able to handle.
     * Entities are uploaded in order of their dependencies.
     *
     */
    public void uploadAllEntities() throws Exception{
        uploadLicense();
        oldIdToNewForIdentityProvider = new HashMap<Long, IdentityProviderConfig>();
        uploadGroupsInternalProvider();
        uploadUsersInternalProvider();
        uploadLdapIdentityProviders();
        uploadAllClusterProperties();
        uploadTrustedCerts();
        uploadFedIdentityProviders();
        uploadSchemasEntries();
        uploadAllTransports();
        uploadAllJmsConnections();
        uploadAllJmsEndpoints();
        uploadAllPublishedServices();//this includes private policies
    }

    /*Delete all entities we know about.
    * Entities are deleted in order of their dependencies.*/
    public void deleteAllEntities() throws Exception{
        deleteAllPublishedServices();
        deleteSchemaEntries();
        deleteAllPolicyForType(PolicyType.INCLUDE_FRAGMENT);
        deleteIdentityProviders();//includes groups and users
        deleteTrustedCerts();
        deleteAllJmsConnectionsAndEndpoints();
        deleteAllClusterProperties();        
    }

    /*
    * Delete all cluster properties from the SSG apart from the cluster.internodePort and license properties.*/
    public void deleteAllClusterProperties() throws Exception{
        System.out.println("Deleting all Cluster Properties");
        Collection<ClusterProperty> cluProperties = clusterAdmin.getAllProperties();
        for(ClusterProperty cP: cluProperties){
            if(cP.getName().equals("cluster.internodePort") || cP.getName().equals("license")){
                continue;
            }

            this.clusterAdmin.deleteProperty(cP);
        }
        System.out.println("Finished deleting all Cluster Properties");
    }

    public void downloadAllClusterProperties() throws Exception{
        System.out.println("Downloading Cluster Properties");
        Collection<ClusterProperty> cluProperties = clusterAdmin.getAllProperties();
        for(ClusterProperty cP: cluProperties){
            if(cP.getName().equals("cluster.internodePort") || cP.getName().equals("license")){
                continue;
            }
            this.doMarshall(cP, jaxbDir+"/ClusterProperties/", cP.getId() +".xml");            
        }
        System.out.println("Finished downloading Cluster Properties");
    }

    public void uploadAllClusterProperties() throws Exception{
        System.out.println("Uploading all Cluster Properties");
        File [] files = this.getFilesFromDirectory(jaxbDir+"/ClusterProperties", this.xmlFilter);
        for(File f: files){
            ClusterProperty clusterProperty = (ClusterProperty)this.unmarshaller.unmarshal(f);
            clusterProperty.setOid(ClusterProperty.DEFAULT_OID);
            this.clusterAdmin.saveProperty(clusterProperty);
        }
        System.out.println("Finished uploading all Cluster Properties");        
    }
    /* Download all trusted certs in this SSG*/
    public void downloadTrustedCerts() throws Exception{
        System.out.println("Downloading Trusted Certs");
        TrustedCertAdmin certAdmin = this.adminContext.getTrustedCertAdmin();
        List<TrustedCert> allCerts = certAdmin.findAllCerts();
        for(TrustedCert tC: allCerts){
           this.doMarshall(tC, jaxbDir+"/TrustedCerts/", tC.getId() +".xml");
        }
        System.out.println("Finished downloading Trusted Certs");
    }

    /*Delete all trusted certs in this SSG.*/
    public void deleteTrustedCerts() throws Exception{
        System.out.println("Deleting all Trusted Certs");
        List<TrustedCert> tCerts = this.trustedCertAdmin.findAllCerts();
        for(TrustedCert tC: tCerts){
            this.trustedCertAdmin.deleteCert(tC.getOid());
        }
        System.out.println("Finished deleting all Trusted Certs");
    }

    /* Upload all TrustedCerts found. Invalid certs exceptions are caught and processing will continue*/
    public void uploadTrustedCerts() throws Exception{
        System.out.println("Uploading all Trusted Certs");
        File [] files = this.getFilesFromDirectory(jaxbDir+"/TrustedCerts/", this.xmlFilter);        

        for( File f: files){
            TrustedCert tCert = (TrustedCert)this.unmarshaller.unmarshal(f);
            TrustedCertAdmin certAdmin = this.adminContext.getTrustedCertAdmin();
            try{
                certAdmin.saveCert(tCert);
            }catch(UpdateException uE){
                System.out.println("Cert: " + tCert.getName()+ " could not be saved: " + uE.getMessage());
            }
        }
        System.out.println("Finished uploading all Trusted Certs");
    }

    /*A fresh SSG requires a license before the manager api functionality becomes available to this app
    * The property jaxb.upload.licensefile must be set in manager_automater.properties and must point to a valid
    * dev license xml file.
    * */
    private void uploadLicense() throws Exception{
        System.out.println("Uploading license");
        ClusterStatusAdmin clusterAdmin = this.adminContext.getClusterStatusAdmin();
        File licenseFile = new File(Main.getProperties().getProperty("jaxb.upload.licensefile"));
        FileInputStream inputStream = new FileInputStream(licenseFile);
        String licenseXml = XmlUtil.nodeToString(XmlUtil.parse(inputStream));
        clusterAdmin.installNewLicense(licenseXml);
    }

    /*Test method to vary the various JaxbEntityManager calls*/
    public void doTests(String action) throws Exception{

        long startTime = System.currentTimeMillis();
        System.out.println("Starting test " + startTime);

        if(action.equalsIgnoreCase("Download")){
            //this.deleteAllEntities();
           this.downloadAllTransports();
            //this.downloadPublishedServices("655407");
            //this.downloadAllPublishedServices();
        }else if(action.equalsIgnoreCase("Upload")){
            //this.uploadAllClusterProperties();
            //this.uploadPublishedServices("23691272");
            //this.uploadAllTransports();
            //this.uploadAllJmsConnections();
            //this.uploadAllJmsEndpoints();
            //this.uploadPublishedServices("51609604");
            /*
            this.uploadLicense();
            for(int i = 0; i < 1000; i++){
                this.deleteSchemaEntries();
                this.uploadSchemasEntries();
                processedSchemas.clear();
            }
            */
            
            //this.deleteAllEntities();
            uploadLicense();
            //deleteAllNonStandardTransports();
            this.uploadAllTransports();
/*
            oldIdToNewForIdentityProvider = new HashMap<Long, IdentityProviderConfig>();
            uploadGroupsInternalProvider();
            uploadUsersInternalProvider();
            uploadLdapIdentityProviders();
            uploadAllClusterProperties();
            uploadTrustedCerts();
            uploadFedIdentityProviders();
            uploadSchemasEntries();
            uploadAllTransports();
            uploadAllJmsConnections();
            uploadAllJmsEndpoints();
            this.uploadPublishedServices("18677761");
*/
        }else if(action.equalsIgnoreCase("TestJaxb")){
            //1) Create a canned version of this version of TestJaxb
            /*
            TestJaxb testJaxb = new TestJaxb();
            testJaxb.setAddress("address");
            testJaxb.setName("name");
            testJaxb.setPhone("phone");
            this.doMarshall(testJaxb,jaxbDir+"/Testing/", "TestJaxb.xml");
            */
            //2) What happens when we add a new property to the class but not the canned xml??
            //add property country to JaxbTest - now unmarshall above marshalled object
            /*
            File f = new File(jaxbDir+"/Testing/TestJaxb.xml");
            TestJaxb testJaxb = (TestJaxb)this.unmarshaller.unmarshal(f);
            System.out.println(testJaxb.toString());   //new property country is just null
            */

            //3) What happens if you remove a property that exists in the canned xml?
            //remove property phone from JaxbTest
            /*
            File f = new File(jaxbDir+"/Testing/TestJaxb.xml");
            TestJaxb testJaxb = (TestJaxb)this.unmarshaller.unmarshal(f);
            System.out.println(testJaxb.toString());  //phone property is not set, no errors or warnings generated
            */

        }

        long duration = System.currentTimeMillis() - startTime;
        System.out.println("Duration: " + duration);  //5664050

    }

    /*
    * Download all connectors - this is important for connectors which
    * are not default like FTP..should prob call it download optional connectors
    * or all you to supply the connectors required...*/
    private void downloadAllTransports() throws Exception{
        System.out.println("Downloading all Transports");
        Collection<SsgConnector> conns = transportAdmin.findAllSsgConnectors();
        for(SsgConnector sG: conns){
            JaxbSsgConnectorProperty jaxbSsgConnProp = new JaxbSsgConnectorProperty();
            jaxbSsgConnProp.setSsgConnector(sG);
            Map<String, String> props = new HashMap<String, String>();
            List<String> propNames = sG.getPropertyNames();
            for(String key: propNames){
                String value = sG.getProperty(key);
                props.put(key, value);
            }
            jaxbSsgConnProp.setProperties(props);                
            this.doMarshall(jaxbSsgConnProp, jaxbDir+"/Transports", sG.getName()+".xml");
        }
        System.out.println("Finished downloading all Transports");
    }

    /*
    * Unmarshall and upload all Transports EXCEPT transports which are allready defined on the SSG
    * Unlike other uploads this method will first download all Transports to know what Transports
    * exist and then only upload those which are new
    * Since a port on THIS SSG we are uploading too can only have one enabled port at a time a connector
    * is determined to be already existing if its port and enabled status match.
    *
    * When marshalling an SsgConnector, it's Set<SsgConnectorProperty> is also marshalled. Each
    * SsgConnectorProperty in this set contains a reference to an SsgConnector, but this relationship is
    * not marshalled due to the infinite loop it causes. So when uploading we need to set the SsgConnectorProperty's
    * reference to the SsgConnector to which it belongs.
    * */
    public void uploadAllTransports() throws Exception{
        System.out.println("Uploading all NEW Transports");
        File [] files = this.getFilesFromDirectory(jaxbDir+"/Transports", this.xmlFilter);        

        //Download all existing Tansports on this SSG
        Collection<SsgConnector> conns = this.transportAdmin.findAllSsgConnectors();
        Set<String> connSet = new HashSet<String>();
        for(SsgConnector sG: conns){
            String uniqueConnName = getUniqueSsgConnectorName(sG);
            connSet.add(uniqueConnName);
        }
        
        for(File f: files){
            JaxbSsgConnectorProperty jaxbSsgConnectorProperty = (JaxbSsgConnectorProperty)this.unmarshaller.unmarshal(f);
            String uniqueConnName = getUniqueSsgConnectorName(jaxbSsgConnectorProperty.getSsgConnector());
            if(!connSet.contains(uniqueConnName)){
                //upload as this transport isn't defined on the SSG
                System.out.println("Saving SsgConnector: " + uniqueConnName);
                //Set<SsgConnectorProperty> connProps = jaxbSsgConnectorProperty.getProperties();
                //Working around protected get/setProperties in SsgConnectorProperty, need to take the
                //long route instead of changing the access modifier, need to create new properties
                //as no way to modify the SsgConnector's internal state. Don't want to change the protected
                //to public right now
                Map<String, String> props = jaxbSsgConnectorProperty.getProperties();
                SsgConnector ssgConnector = jaxbSsgConnectorProperty.getSsgConnector();
                for(String key: props.keySet()){
                    ssgConnector.putProperty(key, props.get(key));
                }
                //Update the oid of this jaxbSsgConnectorProperty before adding it's props
                ssgConnector.setOid(SsgConnector.DEFAULT_OID);

                this.transportAdmin.saveSsgConnector(ssgConnector);
            }else{
                System.out.println("Not saving SsgConnector: " + uniqueConnName);                
            }
        }
        System.out.println("Finished uploading all NEW Transports");        

    }

    public void deleteAllNonStandardTransports() throws Exception{
        System.out.println("Deleting all non standard Transports");
        Collection<SsgConnector> conns = this.transportAdmin.findAllSsgConnectors();
        for(SsgConnector sG: conns){
           int port = sG.getPort();
           if(port != 8080 && port != 8443 && port != 8443){
               this.transportAdmin.deleteSsgConnector(sG.getOid());
           }
        }
        System.out.println("Finished deleting all non standard Transports");        
    }

    private String getUniqueSsgConnectorName(SsgConnector sG){
        //return sG.getName()+"_"+sG.getPort()+"_"+sG.isEnabled();
        return sG.getPort()+"_"+sG.isEnabled();
    }
    /*
    * Download all schema entries. The id of the SchemaEntry is the only thing which makes it unique.
    * Name is not required. However if you include a global schema in a policy assertion it will reference
    * the schema based on name. Therefore we don't need to manage any links when downloading / uploading
    * SchemaEntry's.
    * */
    public void downloadSchemaEntries() throws Exception{
        System.out.println("Downloading all SchemaEntries");
        Collection<SchemaEntry> allSchemas = this.schemaAdmin.findAllSchemas();
        for(SchemaEntry sE: allSchemas){
            if(!sE.getName().equals(SOAP_SCHEMA)){
                this.doMarshall(sE, jaxbDir+"/SchemaEntries", sE.getName());
            }
        }
        System.out.println("Finished downloading all SchemaEntries");
    }

    /**
     * Unmarshall and upload all SchemaEntry's.
     * Some of these global schemas can reference each other. When uploading schemas the SchemaManager will resolve
     * all references. Our only concern when uploading SchemaEntry's is to ensure that any
     * global schemas a SchemaEntry references in it's schema xml exist already on the SSG so that it can be located
     * when the SSG is validating the SchemaEntry.
     *
     * The dependency search is recursive and will upload files in the order of their dependencies. A file representing
     * a SchemaEntry will not be uploaded more than once.
     *
     * Note: AutoTest currently does not reference any schema's listed in the table community_schemas.
     */
    public void uploadSchemasEntries() throws Exception{
        System.out.println("Uploading all Schema Entries");

        File [] files = this.getFilesFromDirectory(jaxbDir+"/SchemaEntries", this.xsdFilter);        

        for(File f: files){
            SchemaEntry schemaEntry = (SchemaEntry)this.unmarshaller.unmarshal(f);
            System.out.println("Working on schema: " + f.getName());
            //Before we load a schema we need to make sure we have loaded any file it's
            //dependent on first, that will exist as an entry in the  community_schema table.
            try{
                this.processSchemas(schemaEntry);
            }catch(Exception eX){
                System.out.println("Exception with schema: " + f.getName()+" " + eX.getMessage());
                System.out.println(schemaEntry.getSchema());                
                throw eX;
            }
        }
        System.out.println("Finished uploading all Schema Entries");
    }

    /*
    * Recursively search the supplied schemaEntry for included global schemas. Any found are loaded to the SSG*/
    private void processSchemas(SchemaEntry schemaEntry) throws Exception{

        //List<SchemaEntry> schemaIncludes = this.getAllIncludes(schemaEntry);
        List<SchemaEntry> schemaIncludes = getAllIncludesByTextSearch(schemaEntry);
        for(SchemaEntry se: schemaIncludes){
            if(!processedSchemas.contains(se.getName())){
                processSchemas(se);
            }
        }

        //If an included file was found it may have been loaded before we come to it on disk
        //in that case don't save it twice, the SSG will accept duplicate schemas.
        if(!processedSchemas.contains(schemaEntry.getName())){
            System.out.println("UPLOADING Schema: " + schemaEntry.getName());
            uploadSchema(schemaEntry);
            processedSchemas.add(schemaEntry.getName());
        }

    }

    /*
    * NOTE: Not using at the moment, using getAllIncludesByTextSearch as we only care about locally referenced
    * schemas and this method will fail there is a remote schema.
    * Retrieve a List<SchemaEntry> of all global schemas the supplied SchemaEntry includes.
    * This code will only work if all included schemas are other global schemas which we have locally on disk
    * after downloading all SchemaEntry's previously.
    * @param schemaEntry the SchemaEntry to search for includes
    * @return List<SchemaEntry> the List of SchemaEntry's included by schemaEntry
    * @throws Exception if any includes cannot be found..as well as all the other exceptions being ducked - io,jaxb..
    * */
    /*
    private List<SchemaEntry> getAllIncludes(SchemaEntry schemaEntry)  throws Exception{
        
        final List<SchemaEntry> schemaEntryList = new ArrayList<SchemaEntry>();

        SchemaFactory sf = SchemaFactory.newInstance(XmlUtil.W3C_XML_SCHEMA);
        LSResourceResolver lsrr = new LSResourceResolver() {
            public LSInput resolveResource(String type,
                                           String namespaceURI,
                                           String publicId,
                                           String systemId,
                                           String baseURI)
            {
                //Get the required SchemaEntry
                File refedFile = new File(jaxbDir+"/SchemaEntries/" + systemId);
                SchemaEntry sEntry = null;
                try{
                    sEntry = (SchemaEntry)unmarshaller.unmarshal(refedFile);
                }catch(JAXBException jE){
                    //very basic test to see if
                    throw new RuntimeException("Could not unmarshall refed SchemaEntry");
                }
                schemaEntryList.add(sEntry);                
                LSInputImpl lsi =  new LSInputImpl();
                lsi.setStringData(sEntry.getSchema());
                lsi.setSystemId(systemId);
                return lsi;
            }
        };
        sf.setResourceResolver(lsrr);

        String schema = schemaEntry.getSchema();
        ByteArrayInputStream bais = new ByteArrayInputStream(schema.getBytes("UTF-8"));
        Schema s = sf.newSchema(new StreamSource(bais, "NotUsedHere")); // populates imports as side-effect
        //Our ResourceRevolver in SchemaFactory has now been executed...if their were any includes
        
        return schemaEntryList;
    }
    */
    
    /*
    *
    * Searches the schema xml in the supplied SchemaEntry for includes which are local.
    * @param schemaEntry the SchemaEntry who's schema xml we need to search for schema includes
    * @return a list of SchemaEntry's referenced by schemaEntry
    * @throws Exception if the schemaLocation points to a file which has not previously been downloaded.
    * */
    private List<SchemaEntry> getAllIncludesByTextSearch(SchemaEntry schemaEntry) throws Exception{
        List<SchemaEntry> schemaEntryList = new ArrayList<SchemaEntry>();

        String s = schemaEntry.getSchema();
        int lastIndex = 0;
        while(true){
            int i = s.indexOf("schemaLocation=\"", lastIndex);
            if(i == -1){
                break;
            }
            int s1 = s.indexOf("\"", i);
            int s2 = s.indexOf("\"", s1+1);            
            String fileName = s.substring(s1+1, s2);
            if(fileName.indexOf("http") != -1){
                continue;
            }
            SchemaEntry sEntry = (SchemaEntry)unmarshaller.unmarshal(new File(jaxbDir+"/SchemaEntries/" +fileName));
            schemaEntryList.add(sEntry);
            lastIndex = s2;
        }

        return schemaEntryList;
    }

    private void uploadSchema(String fileName) throws Exception{
        File file = new File(jaxbDir+"/SchemaEntries/" + fileName);
        SchemaEntry schemaEntry = (SchemaEntry)this.unmarshaller.unmarshal(file);
        this.uploadSchema(schemaEntry);
    }

    private void uploadSchema(SchemaEntry schemaEntry) throws Exception{
        if(!schemaEntry.getName().equals(SOAP_SCHEMA)){
            schemaEntry.setOid(SchemaEntry.DEFAULT_OID);
            System.out.println("Saving Schema:");
            System.out.println(schemaEntry.getName());
            this.schemaAdmin.saveSchemaEntry(schemaEntry);
        }
    }

    /*
    * Delete all SchemaEntry's from the SSG.*/
    public void deleteSchemaEntries() throws Exception{
        System.out.println("Deleting all SchemaEntries");
        Collection<SchemaEntry> allSchemas = this.schemaAdmin.findAllSchemas();
        for(SchemaEntry sE: allSchemas){
            if(!sE.getName().equals(SOAP_SCHEMA)){
                this.schemaAdmin.deleteSchemaEntry(sE);
            }
        }
        System.out.println("Finished deleting all SchemaEntries");
    }
    /*
    * Unmarshall and upload all JmsConnections
    * See downloadJmsConnectionAndEndpoints*/
    public void uploadAllJmsConnections() throws Exception{
        System.out.println("Uploading all JmsConnections");
        File [] files = this.getFilesFromDirectory(jaxbDir+"/JmsConnections", this.xmlFilter);

        for(File f: files){
            JmsConnection jmsConn = (JmsConnection)this.unmarshaller.unmarshal(f);
            jmsConn.setOid(JmsConnection.DEFAULT_OID);//This is PersistentEntity's DEFAULT_OID
            this.jmsAdmin.saveConnection(jmsConn);
        }
        System.out.println("Finished uploading all JmsConnections");
    }

    /**
     * Download from the SSG all JmsConnections and return a Map of unique identifier
     * to JmsConnection. This info is needed when uploading JmsEndpoints
     * See getJmsConnectionUniqueIdentifiers for how the unique identifier for
     * a JmsConnection is created
     * @return Map of JmsConnection unique identifiers to JmsConnection objects.
     *
     */
    private Map getJmsConnectionUniqueIdentifiers() throws Exception{
        JmsConnection [] jConns = this.jmsAdmin.findAllConnections();
        Map<String, JmsConnection> returnMap = new HashMap<String, JmsConnection>();

        for(JmsConnection jC: jConns){
            String uniqueIdentifier = this.createUniqueJmsConnectionIdentifier(jC);
            returnMap.put(uniqueIdentifier, jC);
        }

        return returnMap;
    }

    public void uploadAllJmsEndpoints() throws Exception{
        System.out.println("Uploading all JmsEndpoints");
        File [] files = this.getFilesFromDirectory(jaxbDir+"/JmsEndpoints", this.xmlFilter);
        Map<String, JmsConnection> uniqIdToJmsConn = null;
        if(files.length > 0){
            uniqIdToJmsConn = this.getJmsConnectionUniqueIdentifiers();
        }

        System.out.println("Found : " + files.length+ " JmsEndpoints");
        for(File f: files){
            JaxbJmsEndpoint jaxbEndPoint = (JaxbJmsEndpoint) this.unmarshaller.unmarshal(f);
            String uniqueIdentifier = jaxbEndPoint.getJmsConnectionUniqueIdentifier();
            System.out.println("Uploading endpoint: " + uniqueIdentifier);
            if(!uniqIdToJmsConn.containsKey(uniqueIdentifier)){
                throw new RuntimeException("JmsConnection required for JmsEndpoint not found on SSG");
            }
            JmsConnection endPointConn = uniqIdToJmsConn.get(uniqueIdentifier);
            JmsEndpoint endPoint = jaxbEndPoint.getJmsEndPoint();
            endPoint.setOid(JmsConnection.DEFAULT_OID);//PersistentEntity.DEFAULT_OID
            endPoint.setConnectionOid(endPointConn.getOid());
            this.jmsAdmin.saveEndpoint(endPoint);
        }
        System.out.println("Finished uploading all JmsEndpoints");
    }

    /*Delete's all JMSConnections from the SSG.*/
    public void deleteAllJmsConnectionsAndEndpoints() throws Exception{
        System.out.println("Deleting all JmsConnections and JmsEndpoints");
        JmsAdmin.JmsTuple [] tuples = this.jmsAdmin.findAllTuples();
        for(JmsAdmin.JmsTuple tuple: tuples){
            this.jmsAdmin.deleteEndpoint(tuple.getEndpoint().getOid());
        }
        //Deleting JmsConnections separately as in theory we can have connections without endpoints
        JmsConnection [] jmsConns = this.jmsAdmin.findAllConnections();

        for(JmsConnection jC: jmsConns){
            this.jmsAdmin.deleteConnection(jC.getOid());
        }
        System.out.println("Finished deleting all JmsConnections and JmsEndpoints");
    }

    /*
    * Easiest way to manage JmsEndpoints and the JmsConnection they depend on is to
    * download them together via JmsAdmin.findAllTuples.
    * This way we can store information to enable the JmsConnection that the JmsEndpoint
    * depends on to be looked up when we recreate the JmsEndpoint on a fresh SSG.
    * This can in theory miss some JmsConnections as they are not dependent on their relationship
    * with any JmsEndpoints - so this is checked also*/
    public void downloadJmsConnectionAndEndpoints() throws Exception{
        System.out.println("Downloading all JmsConnections and JmsEndpoints");

        JmsAdmin.JmsTuple[] tuples = jmsAdmin.findAllTuples();
        for(JmsAdmin.JmsTuple tuple: tuples){
            JmsConnection jmsConn = tuple.getConnection();
            JmsEndpoint jmsEndPoint = tuple.getEndpoint();
            System.out.println("Conn: " + jmsConn.getName()+" Endpoint: " + jmsEndPoint.getName());
            this.doMarshall(jmsConn, jaxbDir+"/JmsConnections", jmsConn.getId()+".xml");

            JaxbJmsEndpoint jaxbJmsEndpoint = new JaxbJmsEndpoint();
            jaxbJmsEndpoint.setJmsEndPoint(jmsEndPoint);
            String uniqueIdentifier = this.createUniqueJmsConnectionIdentifier(jmsConn);
            jaxbJmsEndpoint.setJmsConnectionUniqueIdentifier(uniqueIdentifier);
            this.doMarshall(jaxbJmsEndpoint, jaxbDir+"/JmsEndpoints", jmsEndPoint.getId()+".xml");
        }

        //Did we miss any JmsConnections?
        JmsConnection [] conns = this.jmsAdmin.findAllConnections();
        if(conns.length != tuples.length){
            //We missed some JmsConnections
            for(JmsConnection jC: conns){
                System.out.println("Downloading unused JmsConnections");
                //can either just marshall and overwrite any existing file
                //or check if the file exists
                //or store a map of oid's create above and compare to jC..
                //going with file option
                File testFile = new File(jaxbDir+"/JmsEndPoints/" + jC.getId());
                if(!testFile.exists()){
                    this.doMarshall(jC, jaxbDir+"/JmsEndPoints", jC.getId()+".xml");
                }
            }
        }
        System.out.println("Finsihed downloading all JmsConnections and JmsEndpoints");
    }

    /*
    * Create a unique identifier for a JmsConnection from it's non null db properties.
    * These properties are name, jndi_url, version and factory_classname
    * With this information for a JmsEndpoint we can look up its JmsConnection to get
    * it's new oid in a fresh SSG*/
    private String createUniqueJmsConnectionIdentifier(JmsConnection jmsConn){
        StringBuffer buff = new StringBuffer();
        buff.append(jmsConn.getName()+"_");
        buff.append(jmsConn.getJndiUrl()+"_");
        buff.append(jmsConn.getInitialContextFactoryClassname()+"_");
        buff.append(jmsConn.getVersion());
        return buff.toString();
    }
    /*
    * Download all groups belonging to the supplied identity provider.
    * For Federated groups we have no way of looking up the identity provider when moving to a fresh SSG.
    * If we upload the identity providers followed by the groups, we have no way of telling from the group
    * which proivder it came from. The means of handling this in JaxbEntityManager currently is to download
    * groups after downloading the identity provider to which it belongs. subFolderName should point to a folder
    * within a specific identity provider folder.
    * Jaxb can't marshall interfaces so we marshall Group objects as a subtype of PersistentGroup
    * */
    public void downloadGroupsForIdentityProvider(IdentityProviderConfig config, String subFolderName) throws Exception{

        IdentityHeader [] iHeaders = this.identityAdmin.findAllGroups(config.getOid());
        for(IdentityHeader iH: iHeaders){
            Group g = this.identityAdmin.findGroupByID(config.getOid(), iH.getStrId());

            //fed providers can have two types of groups, show below
            if( g instanceof VirtualGroup){
                VirtualGroup vGroup = (VirtualGroup)g;
                this.doMarshall(vGroup, jaxbDir+"/"+subFolderName+"/VirtualGroup/", vGroup.getId() +".xml");
            }else if(g instanceof FederatedGroup){
                FederatedGroup fGroup = (FederatedGroup)g;
                this.doMarshall(fGroup, jaxbDir+"/"+subFolderName+"/FedGroup", fGroup.getId() +".xml");
            }else if(g instanceof InternalGroup){
                InternalGroup iGroup = (InternalGroup)g;
                //internal providers only have 1 type of group
                this.doMarshall(iGroup, jaxbDir+"/"+subFolderName+"/", iGroup.getId() +".xml");
            }
        }
    }

    /*
    * Unmarshall and upload all groups belonging to the Identity Provider.
    * Each Group is created with an empty set of IdentityHeaders i.e. the group has no memberhship.
    * This was due to decision to store group information with the individual uesrs.
    * */
    public void uploadGroups(IdentityProviderConfig config, String subFolder) throws Exception{

        System.out.println("Uploading all Groups for " +config.getName());
        File [] files = this.getFilesFromDirectory(jaxbDir+"/"+subFolder, this.xmlFilter);

        for( File f: files){
            PersistentGroup persistentGroup = (PersistentGroup)this.unmarshaller.unmarshal(f);
            //Upload this InternalGroup to the FRESH ssg - in this test project
            //this is the ssg identified in the manager_automater.properties file
            persistentGroup.setOid(PersistentEntity.DEFAULT_OID);
            Set<IdentityHeader> identityHeaders = new HashSet<IdentityHeader>();
            String newGroupId = this.identityAdmin.saveGroup(config.getOid() , persistentGroup, identityHeaders);
        }
        System.out.println("Finished uploading Groups for " + config.getName());
    }

    /*Delete all groups belonging to the supplied identity provider.
    * This method should only be called for Internal Identity providers or Federated Identity providers*/
    public void deleteGroups(IdentityProviderConfig config) throws Exception{

        System.out.println("Delete all Groups for " + config.getName());
        IdentityHeader [] identityHeaders = this.identityAdmin.findAllGroups(config.getOid());
        for(IdentityHeader iH: identityHeaders){
            this.identityAdmin.deleteGroup(config.getOid(), iH.getStrId());
        }
        System.out.println("Finished deleting all Groups for " + config.getName());
    }

     /* Download all users from the IdentityProviderConfig.
    * Jaxb cannot marshall Interfaces, also we need to store group membership along with the
    * user, so User objects are marshalled as JaxbPersistentUser objects which hold a reference to
    * a PersistentUser (as apposed to a User).
    * By storing group information with each user when we are uploading we can download all groups and cache
    * their IdentityHeaders as apposed to looking up each user in a group to get it's IdentityHeader if we
    * do it the other way around.
    * */
    public void downloadUsersForIdentityProvider(IdentityProviderConfig config, String subFolderName) throws Exception{
        System.out.println("Downloading all "+config.getName()+" Identity Users");
        IdentityHeader[] identityHeader = this.identityAdmin.findAllUsers(config.getOid());
        for(IdentityHeader iHeader: identityHeader){

            User user = this.identityAdmin.findUserByID(config.getOid(), iHeader.getStrId());
            Set<IdentityHeader> iHeaders = this.identityAdmin.getGroupHeaders(config.getOid(), user.getId());
            Set<String> groupNames = new HashSet();
            for(IdentityHeader iH: iHeaders){
                groupNames.add(iH.getName());
            }
            //JAXB can't process Interfaces - it needs a concrete class with a no arg constructor.
            JaxbPersistentUser jaxbUser = null;
            if(user instanceof InternalUser){
                InternalUser iUser = (InternalUser)user;
                jaxbUser = new JaxbPersistentUser(iUser, groupNames);
            }else if(user instanceof FederatedUser){
                FederatedUser iUser = (FederatedUser)user;
                jaxbUser = new JaxbPersistentUser(iUser, groupNames);
            }
            this.doMarshall(jaxbUser, jaxbDir+"/"+subFolderName+"/", jaxbUser.getPersistentUser().getId() +".xml");
        }
        System.out.println("Finished downloading all "+config.getName()+" Identity Users");
    }

    /*Delete all users belong to the supplied identity provider*/
    public void deleteUsers(IdentityProviderConfig config) throws Exception{

        System.out.println("Deleteing all "+config.getName()+" Identity Users");
        IdentityHeader[] identityHeader = this.identityAdmin.findAllUsers(config.getOid());
        for(IdentityHeader iHeader: identityHeader){

            if(!iHeader.getName().equals("admin")){
                this.identityAdmin.deleteUser(config.getOid(), iHeader.getStrId());
            }
        }
        System.out.println("Finished deleteing all "+config.getName()+" Identity Users");
    }

    /*When creating a User you can specify the Users group membership via a Set of IdentityHeaders.
    Use this method to get the set of IdentityHeaders required by a user by specifying what groups
    the user is currently a member of.
    This method caches the groups identity header's the first time it comes across an IdentityProviderConfig
    @param    groupNames A set of strings representing the groups we want the IdentityHeader for
    @return   Return a set of IdentityHeaders of ALL IdentityHeaders in
    the Internal Identity Provider, whose getName().equals a string in the set groupNames
    * */
    private Set<IdentityHeader> getUserGroupMembership(IdentityProviderConfig config, Set<String> groupNames) throws Exception{
        Set<IdentityHeader> returnSet = new HashSet<IdentityHeader>();
        if(groupNames == null){
            return returnSet;
        }

        //do we need to download all the Groups for the supplied IdentityProviderConfig?
        if(!providerToGroupMap.containsKey(config)){
            IdentityHeader [] identityHeaders = this.identityAdmin.findAllGroups(config.getOid());
            Map<String, IdentityHeader> groupMap = new HashMap();
            for(IdentityHeader iHeader: identityHeaders){
                groupMap.put(iHeader.getName(), iHeader);
            }
            providerToGroupMap.put(config, groupMap);
        }

        Map<String, IdentityHeader> groupMap = providerToGroupMap.get(config);
        for(String groupName: groupNames){
            IdentityHeader iHeader = groupMap.get(groupName);
            returnSet.add(iHeader);
        }

        return returnSet;
    }

    /*
    * Unmarshall and upload all Internal Identity Provider users.
    * User's are unmarshalled as JaxbPersistentUser's which stores a reference to a PersistentUser
    * and a Set<String> representing the User's group membership.
    * This function will first download all groups and store their IdentityHeaders (so on this SSG the
    * group info must have been uploaded first). When uploading a user to the fresh SSG it includes
    * it's group membership via these IdentityHeaders.
    * */
    public void uploadUsers(IdentityProviderConfig config, String subFolder) throws Exception{

        System.out.println("Uploading all "+config.getName()+" Identity Users");
        File [] files = this.getFilesFromDirectory(jaxbDir+"/"+subFolder+"/", this.xmlFilter);

        for( File f: files){
            Object o = this.unmarshaller.unmarshal(f);
            if(o instanceof JaxbPersistentUser){
                JaxbPersistentUser jaxbUser = (JaxbPersistentUser)o;
                PersistentUser pUser = jaxbUser.getPersistentUser();

                pUser.setOid(PersistentEntity.DEFAULT_OID);
                Set<String> groupNames = jaxbUser.getGroupNamesSet();

                Set<IdentityHeader> setHeaders = getUserGroupMembership(config, groupNames);
                //A corner case is the admin user, we can't create as already there but transfer any group memberhsip
                if(config.getTypeVal() == 1 && pUser.getName().equals("admin")){
                    User user = this.identityAdmin.findUserByLogin(config.getOid(),"admin");
                    this.identityAdmin.saveUser(config.getOid(), user, setHeaders);
                }else{
                    this.identityAdmin.saveUser(config.getOid(), pUser, setHeaders);
                }
            }
        }
        System.out.println("Finished uploading all "+config.getName()+" Identity Users");
    }

    private IdentityProviderConfig getInternalIdentityProvider() throws Exception{

        EntityHeader[] allProviderHeaders = this.identityAdmin.findAllIdentityProviderConfig();
        for(EntityHeader h: allProviderHeaders){
            IdentityProviderConfig providerCfg = this.identityAdmin.findIdentityProviderConfigByID(h.getOid());

            if(providerCfg.getTypeVal() == 1){
                return providerCfg;
            }
        }
        throw new Exception("Cannot find internal identity provider");
    }

    /*
     * Download all Identity Provider information. For the Internal Identity Provider this only means its
     * users and groups, as a fresh SSG will have the internal provider by default.
     * For Ldap this function will download just the identity provider information.
     * For Federated it will download the identiy provider as well as it's Groups and users.
     * */
    public void downloadIdentityProviders() throws Exception{
        System.out.println("Downloading all Identity Providers");
        EntityHeader[] allProviderHeaders = this.identityAdmin.findAllIdentityProviderConfig();
        for(EntityHeader h: allProviderHeaders){
            IdentityProviderConfig providerCfg = this.identityAdmin.findIdentityProviderConfigByID(h.getOid());

            if(providerCfg.getTypeVal() == 1){
                IdentityProviderConfig config = (IdentityProviderConfig) providerCfg;
                downloadGroupsForIdentityProvider(config, "IdentityProviders/Internal/InternalGroups");
                downloadUsersForIdentityProvider(config,"IdentityProviders/Internal/InternalUsers/");
            }else if(providerCfg.getTypeVal() == 2){
                LdapIdentityProviderConfig config = (LdapIdentityProviderConfig) providerCfg;
                //LDAP only require connection information - no need to separate out into folder hierarchy.
                this.doMarshall(config, jaxbDir+"/IdentityProviders/LDAP/", providerCfg.getName() + "_"+providerCfg.getTypeVal() +".xml");
            }else if(providerCfg.getTypeVal() == 3){
                FederatedIdentityProviderConfig config = (FederatedIdentityProviderConfig) providerCfg;
                JaxbFederatedIdentityProviderConfig jaxbFedProv = new JaxbFederatedIdentityProviderConfig(config);
                //does it require a cert?
                long [] oids = config.getTrustedCertOids();
                List<String> certDns = new ArrayList<String>();
                if(oids != null){
                    for(long l: oids){
                        TrustedCert cert = this.trustedCertAdmin.findCertByPrimaryKey(l);
                        certDns.add(cert.getSubjectDn());//dn is unique for every cert in a SSG
                    }
                    jaxbFedProv.setTrustedCertDns(certDns);
                }
                downloadGroupsForIdentityProvider(config, "IdentityProviders/FED/"+providerCfg.getName()+"/");
                downloadUsersForIdentityProvider(config,"IdentityProviders/FED/"+providerCfg.getName()+"/Users/");
                this.doMarshall(jaxbFedProv, jaxbDir+"/IdentityProviders/FED/"+providerCfg.getName()+"/", providerCfg.getName() + "_"+providerCfg.getTypeVal() +".xml");
            }

        }
        System.out.println("Finished downloading all Identity Providers");
    }

    /*
    * Delete all identity providers and associated groups and users.
    * For the internal provider this will just be Group and user info.
    * For Ldap's this will just be the identity provider itself.*/
    public void deleteIdentityProviders() throws Exception{
        System.out.println("Deleting all Identity Providers");
        EntityHeader[] allProviderHeaders = this.identityAdmin.findAllIdentityProviderConfig();
        for(EntityHeader h: allProviderHeaders){
            IdentityProviderConfig providerCfg = this.identityAdmin.findIdentityProviderConfigByID(h.getOid());
            //don't delete ldap groups or users
            if(providerCfg.getTypeVal() != 2){
                this.deleteGroups(providerCfg);
                this.deleteUsers(providerCfg);
            }

            if(providerCfg.getTypeVal() == 1){
                continue;
            }
            this.identityAdmin.deleteIdentityProviderConfig(providerCfg.getOid());
        }
        System.out.println("Finished deleting all Identity Providers");
    }



    /*Unmarshall and upload all LDAP Identity Providers*/
    public void uploadLdapIdentityProviders() throws Exception{
        System.out.println("Uploading all Identity Providers");

        File [] files = this.getFilesFromDirectory(jaxbDir+"/IdentityProviders/LDAP", this.xmlFilter);

        for( File f: files){
            LdapIdentityProviderConfig providerCfg = (LdapIdentityProviderConfig)this.unmarshaller.unmarshal(f);
            long oldId = providerCfg.getOid();
            providerCfg.setOid(IdentityProviderConfig.DEFAULT_OID);
            long id = this.identityAdmin.saveIdentityProviderConfig(providerCfg);
            providerCfg.setOid(id);
            oldIdToNewForIdentityProvider.put(oldId, providerCfg);
            System.out.println("Added: " + oldId+" - " + id);
        }

    }

    /*Upload all internal provider groups*/
    public void uploadGroupsInternalProvider() throws Exception{
        this.uploadGroups(this.internalProv,"/IdentityProviders/Internal/InternalGroups");
    }

    /*Upload all internal provider users*/
    public void uploadUsersInternalProvider() throws Exception{
        this.uploadUsers(this.internalProv,"/IdentityProviders/Internal/InternalUsers");
    }

    /*
    * Fed requires groups and users unlike Ldap. Also unlike the internal provider a group or user can belong
    * to any fed provider. All identity from group to provider is via the providers oid, as a result the method
    * taken by JaxbEntityManager is to store fed group / user info as a subdirectory and to upload it directly
    * after uploading the fed provider, when we will have it's new provider id. We could also cache this info
    * so that we can upload groups separately, however the uploading of identity provider will always have to
    * be done first. When downloading Groups we could store information to uniquely identify the Identity Provider
    * to which it belongs.
    * */
    public void uploadFedIdentityProviders() throws Exception{
        System.out.println("Uploading all Federated Identity Providers");

        File [] files = getFilesFromDirectory(jaxbDir+"/IdentityProviders/FED", this.nonSVNDirFilter);
        for( File f: files){
            if(f.isDirectory()){
                File [] subDirFiles = this.getFilesFromDirectory(f, this.xmlFilter);
                if(subDirFiles.length > 1){
                    throw new RuntimeException("More than one Identity Provider found in "+ f.getName());
                }
                File f1 = subDirFiles[0];
                JaxbFederatedIdentityProviderConfig providerCfg = (JaxbFederatedIdentityProviderConfig)this.unmarshaller.unmarshal(f1);
                FederatedIdentityProviderConfig config = providerCfg.getFedProvider();
                long oldId = config.getOid();
                config.setOid(IdentityProviderConfig.DEFAULT_OID);
                //Update it's trustedcert oids from the fresh ssg
                List<String> certDns = providerCfg.getTrustedCertDns();
                if(certDns != null){
                    long [] newOids = new long[certDns.size()];
                    int i = 0;
                    for(String s: certDns){
                        TrustedCert tC = this.trustedCertAdmin.findCertBySubjectDn(s);
                        if(tC != null){
                            newOids[i] = tC.getOid();
                        }
                        i++;
                    }
                    config.setTrustedCertOids(newOids);
                    Method recreateSerializedPropsMethod = FederatedIdentityProviderConfig.class.getMethod("recreateSerializedProps");
                    if(recreateSerializedPropsMethod == null) {
                        Field propsXmlField = FederatedIdentityProviderConfig.class.getField("propsXml");
                        propsXmlField.setAccessible(true);
                        propsXmlField.set(config, null);
                        config.getSerializedProps();
                    } else {
                        recreateSerializedPropsMethod.invoke(config);
                    }

                }else{
                    config.setTrustedCertOids(null);
                }


                long id = this.identityAdmin.saveIdentityProviderConfig(config);
                config.setOid(id);//update so we can use in uploadGroups
                oldIdToNewForIdentityProvider.put(oldId, config);
                System.out.println("Added: " + oldId+" - " + id);
                //Test for Fed Groups
                String fileName = jaxbDir+"/IdentityProviders/FED/"+f.getName()+"/FedGroup";
                File testFile = new File(fileName);
                if(testFile.exists()){
                    uploadGroups(config, "/IdentityProviders/FED/"+f.getName()+"/FedGroup");
                }
                //Test for Virtual Groups
                fileName = jaxbDir+"/IdentityProviders/FED/"+f.getName()+"/VirtualGroup";
                testFile = new File(fileName);
                if(testFile.exists()){
                    uploadGroups(config, "/IdentityProviders/FED/"+f.getName()+"/VirtualGroup");
                }
                uploadUsers(config,"/IdentityProviders/FED/"+f.getName()+"/Users");
            }
        }
        System.out.println("Finished uploading all Federated Identity Providers");
    }

    /*
    * Download and marshall all Policy objects of the specified type and to the specified folder.
    * This function was going to be used to download Policy objects of type PRIVATE_SERVICE also however
    * this is handled via the download/upload for PublishedServices.
    * Currently this function is only used for policy fragments.
    * */
    public void downloadAllPolicyForType(PolicyType type, String folderName) throws Exception{

        System.out.println("Downloading all policies of type: " + type);
        Collection<PolicyHeader> policyFragments = this.policyAdmin.findPolicyHeadersByType(type);
        for(PolicyHeader pH: policyFragments){
            //entity-manager//Policy policy = this.policyAdmin.findPolicyByUniqueName(pH.getName());
            //entity-manager//this.doMarshall(policy, jaxbDir+"/"+folderName+"/",  type+"_"+policy.getId() +".xml" );
        }
        System.out.println("Finished downloading all policies of type: " + type);
    }

    /*Delete all policy of the specified type*/
    public void deleteAllPolicyForType(PolicyType type) throws Exception{
        System.out.println("Deleting all policies of type: " + type);
        Collection<PolicyHeader> policyFragments = this.policyAdmin.findPolicyHeadersByType(type);
        for(PolicyHeader pH: policyFragments){
            this.policyAdmin.deletePolicy(pH.getOid());
        }
        System.out.println("Finished deleting all policies of type: " + type);
    }

    /*Marshall the supplied object to the file specified by the directoryPath and fileName*/
    private void doMarshall(Object obj, String directoryPath, String fileName) throws Exception{

        File dir = new File(directoryPath);
        if(!dir.exists()){
            if(!dir.mkdirs()){
                throw new RuntimeException("Cannot create directory: " + directoryPath);
            }
        }
        OutputStream os = new FileOutputStream(dir.getAbsolutePath()+"/" + fileName );
        marshaller.marshal(obj,os);
    }

    //for testing
    public void downloadPublishedServices(String serviceId) throws Exception{
        PublishedService pService = serviceAdmin.findServiceByID(serviceId);
        JaxbPublishedService jService = new JaxbPublishedService(pService);
        System.out.println(pService.getName());
        this.doMarshall(jService, jaxbDir+"/PublishedService/", pService.getOid() +".xml");
    }

    /**
     * Download and marshall all Published Services.
     * A PublishedService contains a property called httpMethods which returns an unmodifiable Collection.
     * Jaxb is unable to work with this collection. As a result PublishedServices are marshalled as
     * a JaxbPublishedService which contains a reference to a PublishedService.
     * See uploadAllPublishedServices
     */
    public void downloadAllPublishedServices() throws Exception{

        System.out.println("Downloading all Published Services");
        ServiceHeader [] serviceHeaders = this.serviceAdmin.findAllPublishedServices();
        int count = 0;
        for(ServiceHeader sH: serviceHeaders){
            PublishedService pService = serviceAdmin.findServiceByID(sH.getStrId());
            JaxbPublishedService jService = new JaxbPublishedService(pService);
            this.doMarshall(jService, jaxbDir+"/PublishedService/", pService.getOid() +".xml");
            System.out.print(".");
            count++;
            if(count % 100 == 0){
                System.out.println();
            }

        }
        System.out.println();
        System.out.println("Finished downloading all Published Services");
    }

    //Just for testing
    public void uploadPublishedServices(String serviceId) throws Exception{

        File f = new File(jaxbDir+"/PublishedService/"+serviceId+".xml");
        JaxbPublishedService jService = (JaxbPublishedService)this.unmarshaller.unmarshal(f);
        PublishedService pService = jService.getPService();
        pService.setHttpMethods(jService.getHttpMethods());
        Policy policy = pService.getPolicy();
        //In case any Include assertions exist - update their oid in the policy
        this.updatePolicyOids(policy);
        policy.setOid(Policy.DEFAULT_OID);
        //update the Published Service before save
        pService.setOid(PersistentEntity.DEFAULT_OID);
        long newServiceOid = this.serviceAdmin.savePublishedService(pService);

    }

    /*
    * Unmarshall and upload all PublishedService's.
    * PublishedService's are marshalled as JaxbPublishedService which contains a reference to a PublishedService.
    * When the JaxbPublishedService is unmarshalled we can get a Set<String> from it representing the Service's
    * available HTTP methods. This is then set on the PublishedService. This is done as getHttpMethods() from
    * PublishedService returns an unmodifiable Collection which Jaxb can't work with.
    * */
    public void uploadAllPublishedServices() throws Exception{

        System.out.println("Uploading all Published Services");
        File [] files = this.getFilesFromDirectory(jaxbDir+"/PublishedService/", this.xmlFilter);

        int count = 0;
        for( File f: files){
            JaxbPublishedService jService = (JaxbPublishedService)this.unmarshaller.unmarshal(f);
            PublishedService pService = jService.getPService();
            pService.setHttpMethods(jService.getHttpMethods());
            Policy policy = pService.getPolicy();
            //In case any Include assertions exist - update their oid in the policy
            try{
                this.updatePolicyOids(policy);
            }catch(RuntimeException rE){
                Set<Long> keys = this.oldIdToNewForIdentityProvider.keySet();
                System.out.println("Keys and values are:");
                for(Long l: keys){
                    System.out.println("key: " + l+" value: " + this.oldIdToNewForIdentityProvider.get(l));
                }
                System.out.println("Published Service: " + f.getName()+" threw: " + rE.getMessage());
                throw rE;
            }
            policy.setOid(Policy.DEFAULT_OID);
            //update the Published Service before save
            pService.setOid(PersistentEntity.DEFAULT_OID);
            long newServiceOid = this.serviceAdmin.savePublishedService(pService);
            count++;
            System.out.print(".");
            if(count % 100 == 0){
                System.out.println();
            }

        }
        System.out.println();
        System.out.println("Finished uploading all Published Services");
    }

    /*Delete all published services.*/
    private void deleteAllPublishedServices() throws Exception{
        System.out.println("Deleting all Published Services");
        ServiceHeader [] serviceHeaders = this.serviceAdmin.findAllPublishedServices();
        int count = 0;
        for(ServiceHeader sH: serviceHeaders){
            PublishedService pService = serviceAdmin.findServiceByID(sH.getStrId());
            this.serviceAdmin.deletePublishedService(sH.getStrId());
            System.out.print(".");
            count++;
            if(count % 100 == 0){
                System.out.println();
            }
        }
        System.out.println();
        System.out.println("Finished deleting all Published Services");
    }
    /**
     * Unmarshall and upload all policy fragments.
     * Fragments MUST be uploaded before the policies that use them. This is because when
     * a policy includes a policy fragment reference it will contain a boxed oid.
     * This oid referes to the oid on the ssg from where this policy fragment came. As a result
     * we need to update this boxed oid before uploading the private service policy.
     */
    public void uploadAllPolicyFragments() throws Exception{

        System.out.println("Uploading all Policy Fragments");
        File [] files = this.getFilesFromDirectory(jaxbDir+"/PolicyFragments/", this.xmlFilter);

        for( File f: files){
            Policy policy = (Policy)this.unmarshaller.unmarshal(f);
            policy.setOid(Policy.DEFAULT_OID);
            boolean isEnabled = !(policy.isDisabled());
            this.policyAdmin.savePolicy(policy, isEnabled);
        }
        System.out.println("Finished uploading all Policy Fragments");
    }

    /*Find any oids in the supplied policy and update the oid to be the id from
    from the new SSG.
    Currently this includes the Include and SpecificUser assertions.
    */
    private void updatePolicyOids(Policy policy) throws Exception{

        Assertion a = policy.getAssertion();
        if(a != null && a instanceof AllAssertion){
            AllAssertion aA = (AllAssertion)a;
            boolean policyUpdated = this.processAssertions(aA);
            //Only reparse the assertions into xml if required
            if(policyUpdated){
                String newPolicyXml = WspWriter.getPolicyXml(a);
                policy.setXml(newPolicyXml);
            }
        }

    }

    /*Look for include assertions so that we can update any references to invalid policy fragment oid's.
    * Recursive*/
    private boolean processAssertions(CompositeAssertion a) throws Exception{

        boolean policyUpdated = false;
        List children = a.getChildren();
        Iterator iter = children.iterator();
        while(iter.hasNext()){
            Assertion a1 = (Assertion)iter.next();
            //CompositeAssertion subclasses are AllAssertion, one or more, exactly one
            if(a1 instanceof CompositeAssertion){
                //keep any true found in processing the policy, ensure processAssertion is on the left of || to make
                //sure it's executed for a1, even after the first CompositeAssertion found caused a policy update.
                policyUpdated = this.processAssertions((CompositeAssertion)a1) || policyUpdated;
            }
            if(a1 instanceof Include){
                updateIncludeAssertion((Include)a1);
                policyUpdated = true;
            }
            if(a1 instanceof SpecificUser){
                updateSpecificUserAssertion((SpecificUser)a1);                
                policyUpdated = true;
            }
            if(a1 instanceof MemberOfGroup){
                updateGroupAssertion((MemberOfGroup)a1);
                policyUpdated = true;
            }
            if(a1 instanceof JmsRoutingAssertion){
                updateJmsRoutingAssertion((JmsRoutingAssertion)a1);
                policyUpdated = true;
            }
            
        }
        return policyUpdated;
    }

    /*
    * Update the oid reference to the JMS endpoint contained in the supplied jmsAssertion
    * The first time is called it will download and cache all JmsConnection, JmsEndpoint tuples.
    * @param jmsAssertion The JmsRoutingAssertion to update
    * Note: After this method has been ran for the supplied JmsRoutingAssertion it's internal state is
    * upto date however the Policy it belongs to xml has not yet been updated.
    */
    private void updateJmsRoutingAssertion(JmsRoutingAssertion jmsAssertion) throws Exception{

        //Need to look up this endpoint. This is not directly possible so we need to first
        //download all of them via findAllTuples.
        if(this.endPointMap == null){
            JmsAdmin.JmsTuple[] jmsTuples = this.jmsAdmin.findAllTuples();
            endPointMap = new HashMap<String, JmsEndpoint>();
            for(JmsAdmin.JmsTuple tuple: jmsTuples){
                JmsEndpoint endPoint = tuple.getEndpoint();
                endPointMap.put(endPoint.getName(), endPoint);
            }
        }

        String endPointName = jmsAssertion.getEndpointName();
        if(!this.endPointMap.containsKey(endPointName)){
            //throw new RuntimeException("JmsEndpoint: " + endPointName+ " not found in SSG");
            //dont throw the exception, as it will stop all other services from being uploaded.
            System.out.println("Could not find endPoint: " + endPointName);
            return;
        }

        JmsEndpoint endPoint = this.endPointMap.get(endPointName);
        jmsAssertion.setEndpointOid(endPoint.getOid());
    }
    
    /*
    * Updated the supplied MemberOfGroup's oid's used within it to reference the Group it
    * requires as well as the IdentityProvider.
    * @param group The MemberOfGroup to update
    * Note: After this method has been ran for the supplied MemberOfGroup it's internal state is
    * upto date however the Policy it belongs to xml has not yet been updated.*/
    private void updateGroupAssertion(MemberOfGroup group) throws Exception{
        long providerId = group.getIdentityProviderOid();
        long providerIdToUse;
        if(providerId != IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID){
           if(!this.oldIdToNewForIdentityProvider.containsKey(providerId)){
               throw new RuntimeException("Cannot find the new provider id for provider id: " + providerId);
           }
           IdentityProviderConfig config = this.oldIdToNewForIdentityProvider.get(providerId);
           providerIdToUse = config.getOid();
        }else{
            providerIdToUse = providerId;
        }
        String groupName = group.getGroupName();
        try{
            Group foundGroup = this.identityAdmin.findGroupByName(providerIdToUse, groupName);
            if(foundGroup == null){
                throw new RuntimeException("Group: "+ groupName +" not found");
            }
            String oid = foundGroup.getId();
            group.setGroupId(oid);
            group.setIdentityProviderOid(providerIdToUse);
        }catch(Exception ex){
            System.out.println("Exception finding group with name: " + groupName);
            System.out.println("Exception is: " + ex.getMessage());
        }
    }

    /*
    * Updated the supplied SpecificUser's oid's used within it to reference the user it
    * requires as well as the IdentityProvider.
    * @param group The SpecificUser to update
    * Note: After this method has been ran for the supplied SpecificUser it's internal state is
    * upto date however the Policy it belongs to xml has not yet been updated.*/
    private void updateSpecificUserAssertion(SpecificUser specificUser) throws Exception{

        long providerId = specificUser.getIdentityProviderOid();
        long providerIdToUse;
        IdentityProviderConfig config = null;
        if(providerId != IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_OID){
           if(!this.oldIdToNewForIdentityProvider.containsKey(providerId)){
               throw new RuntimeException("Cannot find the new provider id for provider id: " + providerId);
           }
           config = this.oldIdToNewForIdentityProvider.get(providerId);
           providerIdToUse = config.getOid();
        }else{
            providerIdToUse = providerId;
        }
        String userLogin = specificUser.getUserLogin();
        try{
            //Fed users in the AutoTest db's can have blank login's! This means we can't find them with current
            // Manager api....unless we download them all...so we know who they are.

            //this handles internal and ldap users and fed users with non blank login's...
            User user = null;
            try{
                user = this.identityAdmin.findUserByLogin(providerIdToUse, userLogin);
            }catch(Exception ex){ //FindException not working..coming back as spring roll back exception...
                System.out.println("User not found: " + userLogin);
                user = null;
            }
            //this handles when fed user doesn't have a non blank login
            if(user == null && config != null){
                if(config instanceof FederatedIdentityProviderConfig){
                    if(this.fedUserNameToUserMap == null){
                        doDownloadAllFedUsers(providerIdToUse);
                    }                        
                    //get fed user here
                    if(!this.fedUserNameToUserMap.containsKey(specificUser.getUserName())){
                        throw new RuntimeException("Fed user: " + specificUser.getUserName()+" could not be found");
                    }
                    user = this.fedUserNameToUserMap.get(specificUser.getUserName());
                }
            }
            //or maybe the user just doesn't exist..
            if(user == null){
                throw new RuntimeException("User: "+ userLogin+" not found");
            }
            String oid = user.getId();
            specificUser.setUserUid(oid);
            specificUser.setIdentityProviderOid(providerIdToUse);
        }catch(Exception ex){
            //This happens as some published services have policies with blank user logins
            //also the usernames associated with these blank user logins are not valid.
            System.out.println("Exception finding user with login: " + userLogin);
            System.out.println("This users name is: " + specificUser.getUserName());
            System.out.println("Exception is: " + ex.getMessage());
        }
    }

    /*
    * Download and store in instance variable fedUserNameToUserMap all Fed Users
    * Required when uploading PublishedServices and we need to resolve the new user oid of the
    * fed user which is referenced from with the services policy xml*/
    private void doDownloadAllFedUsers(long providerId) throws Exception{
        if(this.fedUserNameToUserMap == null){
            fedUserNameToUserMap = new HashMap<String, User>();
            IdentityHeader [] fedUsers = this.identityAdmin.findAllUsers(providerId);
            for(IdentityHeader iH: fedUsers){
                User aFedUser = this.identityAdmin.findUserByID(providerId, iH.getStrId());
                fedUserNameToUserMap.put(iH.getName(), aFedUser);
            }
        }
    }

    /*Look up the include assertion and get it's new oid. Do the look up based on name*/
    private void updateIncludeAssertion(Include include) throws Exception{

        String policyName = include.getPolicyName();
        Policy policy = this.policyAdmin.findPolicyByUniqueName(policyName);
        if(policy == null){
            throw new RuntimeException("Policy fragment: "+ policyName+" not found");
        }
        long oid = policy.getOid();
        include.setPolicyOid(oid);
    }

}
