/*
*Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
*
*WSO2 Inc. licenses this file to you under the Apache License,
*Version 2.0 (the "License"); you may not use this file except
*in compliance with the License.
*You may obtain a copy of the License at
*
*http://www.apache.org/licenses/LICENSE-2.0
*
*Unless required by applicable law or agreed to in writing,
*software distributed under the License is distributed on an
*"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
*KIND, either express or implied.  See the License for the
*specific language governing permissions and limitations
*under the License.
*/

package org.wso2.appserver.distributed.tests;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.wink.client.ClientResponse;
import org.json.JSONObject;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.appserver.distributed.admin.clients.AARServiceUploaderClient;
import org.wso2.appserver.distributed.admin.clients.ServiceDeploymentUtil;
import org.wso2.appserver.distributed.common.utils.DockerController;
import org.wso2.carbon.authenticator.stub.AuthenticationAdminStub;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.automation.test.utils.dbutils.MySqlDatabaseManager;
import org.wso2.carbon.integration.common.admin.client.AuthenticatorClient;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

import static org.testng.Assert.assertNotNull;

/**
 * Before Running This Code Please be Ensure The Following
 * <p/>
 * 1. Run & check whether you have already running docker process
 * ps -ef|grep docker
 * 2. If so stop it
 * sudo docker stop / service docker stop
 * 3. Start docker daemon on port 2375
 * sudo docker daemon -H tcp://127.0.0.1:2375
 * 4. If you want to verify docker images
 * export DOCKER_HOST="tcp://127.0.0.1:2375"
 * 5. To retrieve docker run product url ( eg: 31cc6d932893 is the containerID (type docker ps to find so)
 * docker inspect --format '{{ .NetworkSettings.IPAddress }}' 31cc6d932893
 * <p/>
 * info :
 * https://docs.docker.com/engine/reference/api/remote_api_client_libraries/
 * <p/>
 * for error
 * Caused by: org.eclipse.jgit.errors.TransportException: : cannot open git-upload-pack
 * <p/>
 * solution
 * sudo git config --global http.sslVerify false
 * <p/>
 * <p/>
 * <p/>
 * To run with the profile
 * <p/>
 * <p/>
 * Following are the step sequence
 * 1. Building mysql-container
 * 2. Running mysql docker container
 * 3. Performing following git cloning ops.
 * 3.1 - DockerFile
 * 3.2 - Puppet module
 * 4. Copy product pack
 * 5. Copy mysql connector jar
 * 6. Copy yaml files (defult.yaml)
 * 7. Perform DB Ops.
 * 7.1 - Connecting to DB
 * 7.2 - Creating Databases
 * 7.3 - Execute create tables DB script
 * 8. Build Wso2 base
 * 9. Building wso2as container
 * 10.Running wso2as container ( access mgt console : https://172.17.0.3:9443/carbon/admin/login.jsp)
 * correct way : https://localhost:32004/carbon/admin/login.jsp
 */


@Test(groups = "wso2.com.dimuthu.tests")
public class SampleTestCase {

    private DockerController dockerController;
    private String resourceLocation;
    private String mysqlContainerIP;
    private String asContainerIP;
    private String asContainerPort;
    protected String sessionCookie;
    protected String backendURL;

    private static final Log log = LogFactory.getLog(SampleTestCase.class);

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {

   /*     AutomationContext automationContext = new AutomationContext("AS", "appServerInstance0001", ContextXpathConstants.SUPER_TENANT,
                ContextXpathConstants.SUPER_ADMIN);*/

        dockerController = new DockerController();
       /* HttpsResponse response =
                HttpsURLConnectionClient.getRequest("https://google.com",
                        null
                );*/

        resourceLocation = FrameworkPathUtil.getSystemResourceLocation();

        // git clone - dockerfile repo
        dockerController.gitRepoClone("https://github.com/wso2/dockerfiles.git", resourceLocation
                + File.separator + "temp" + File.separator + "dockerfiles");

        // git clone - puppetmodule repo
        dockerController.gitRepoClone("https://github.com/wso2/puppet-modules.git", resourceLocation
                + File.separator + "temp" + File.separator + "puppet-module");

    }

    @Test(groups = "wso2.as.esb", description = "Sample AS & ESB Test Scenario")
    public void testAsMysqlStartContainers() throws Exception {

        // Building mysql-container
        dockerController.buildDockerFile("http://127.0.0.1:2375", resourceLocation + File.separator
                + "mysql" + File.separator + "5.7" + File.separator + "Dockerfile", "mysql:5.7.111");

        // Running mysql docker image
        JSONObject jsonObjectMySql = dockerController.runDockerImage(resourceLocation + File.separator + "json" + File.separator + "mysqlcreatecontainer.json",
                resourceLocation + File.separator + "json" + File.separator + "mysqlstartcontainer.json");


        ClientResponse responseInspectContainerMySql = dockerController.inspectContainer(jsonObjectMySql);
        Object jsonInspectObjMySql = new JSONObject(responseInspectContainerMySql).get("NetworkSettings");
        mysqlContainerIP = ((JSONObject) jsonInspectObjMySql).get("IPAddress").toString();

        assertNotNull(mysqlContainerIP, "ID should not be null");


        // copy product pack
        DockerController.copyFiles(resourceLocation + "productpack" + File.separator + "wso2as-5.3.0.zip",
                resourceLocation + File.separator + "temp" + File.separator + "puppet-module" + File.separator + "modules" + File.separator +
                        "wso2as" + File.separator + "files" + File.separator + "wso2as-5.3.0.zip"
        );

        // copy mysql jar
        DockerController.copyFiles(resourceLocation + "connectors" + "/mysql-connector-java-5.1.26-bin.jar",
                resourceLocation + File.separator + "temp" + File.separator +
                        "puppet-module/modules/wso2as/files/configs/repository/components/lib" +
                        "/mysql-connector-java-5.1.26-bin.jar"
        );

        // copy yaml files
        DockerController.copyFiles(resourceLocation + File.separator + "yaml" + File.separator + "as" + File.separator + "default.yaml",
                resourceLocation + File.separator + "temp" + File.separator + "puppet-module/hieradata/dev/wso2/wso2as/5.3.0/default.yaml");

        performDBOperations();

        //build wso2base image
        //TODO

        //run wso2as image
        JSONObject jsonObjectAS = dockerController.runDockerImage(resourceLocation + File.separator + "json" + File.separator + "wso2ascreatecontainer.json",
                resourceLocation + File.separator + "json" + File.separator + "wso2asstartcontainer.json");

        ClientResponse responseInspectContainerAS = dockerController.inspectContainer(jsonObjectAS);
        Object jsonInspectObjAS = new JSONObject(responseInspectContainerAS).get("NetworkSettings");
        asContainerIP = ((JSONObject) jsonInspectObjAS).get("IPAddress").toString();
        asContainerPort = ((JSONObject) jsonInspectObjAS).get("Ports").toString();
    }


    @Test(groups = "wso2.as.esb", description = "Upload aar service and verify deployment", dependsOnMethods = "testStartESBContainer")
    public void testAarServiceUpload() throws Exception {

        getBackendUrl();

     /*   SchemeRegistry registry = new SchemeRegistry();
        HostnameVerifier hostnameVerifier = org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
        SSLSocketFactory socketFactory = SSLSocketFactory.getSocketFactory();
        socketFactory.setHostnameVerifier((X509HostnameVerifier) hostnameVerifier);
        registry.register(new Scheme("https", socketFactory, 443));
        SingleClientConnManager mgr = new SingleClientConnManager(client.getParams(), registry);*/

        //System.setProperty("javax.net.ssl.trustStore", "/home/dimuthu/Desktop/Products/AS/product-as/modules/distributed/tests.distributed/src/test/resources/keystores/products/client-truststore.jks");
        System.setProperty("javax.net.ssl.trustStore", "/home/dimuthu/Desktop/Products/AS/product-as/modules/distributed/tests.distributed/src/test/resources/productpack/wso2as-5.3.0/repository/resources/security/client-truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword", "wso2carbon");


 /*       SSLSocket socket = (SSLSocket) SSLSocketFactory.getSocketFactory().createSocket("localhost","32004",false);
        socket.setEnabledProtocols(new String[]{"SSLv3", "TLSv1"});*/


       /* AxisServiceClient axisServiceClient = new AxisServiceClient();
        String endpoint = "https://127.0.0.1:32004/services/AuthenticationAdmin.AuthenticationAdminHttpsSoap11Endpoint/";
        OMElement result = axisServiceClient.sendReceive(createPayload(), endpoint, "login");*/

/*
        HttpsResponse response =
                HttpsURLConnectionClient.getRequest("https://localhost:32004/carbon/admin/login.jsp",
                        null
                );*/

        AuthenticationAdminStub authenticationAdminStub = new AuthenticationAdminStub("https://localhost:32004/services/AuthenticationAdmin");
        authenticationAdminStub.login("admin", "admin", "localhost");

        //https://localhost:9543/services/AuthenticationAdmin

        AuthenticatorClient authenticatorClient = new AuthenticatorClient(backendURL);

        sessionCookie = authenticatorClient.login("admin", "admin", asContainerIP);

        AARServiceUploaderClient aarServiceUploaderClient
                = new AARServiceUploaderClient(backendURL, sessionCookie);
        aarServiceUploaderClient.uploadAARFile("Axis2Service.aar",
                FrameworkPathUtil.getSystemResourceLocation() + "artifacts" +
                        File.separator + "AS" + File.separator + "aarservices" + File.separator +
                        "HelloWorld.aar", ""
        );
        ServiceDeploymentUtil.isServiceDeployed(backendURL,
                sessionCookie, "HelloService");
        log.info("HelloWorld.aar service uploaded successfully");
    }


    @Test(groups = "wso2.as.esb", description = "Start ESB Container", dependsOnMethods = "testAsMysqlStartContainers")
    public void testStartESBContainer() throws Exception {

        // copy product pack
        DockerController.copyFiles(resourceLocation + "productpack" + File.separator + "wso2esb-4.9.0.zip",
                resourceLocation + File.separator + "temp" + File.separator + "puppet-module" + File.separator
                        + "modules" + File.separator + "wso2esb" + File.separator + "files" + File.separator +
                        "wso2esb-4.9.0.zip"
        );

        // copy yaml files
        DockerController.copyFiles(resourceLocation + File.separator + "yaml" + File.separator + "esb"
                + File.separator + "default.yaml", resourceLocation + File.separator + "temp" + File.separator +
                "puppet-module/hieradata/dev/wso2/wso2esb/4.9.0/default.yaml");

        //run wso2esb image
        JSONObject jsonObjectESB = dockerController.runDockerImage(resourceLocation + File.separator + "json"
                        + File.separator + "wso2esbcreatecontainer.json",
                resourceLocation + File.separator + "json" + File.separator + "wso2esbstartcontainer.json"
        );

        ClientResponse responseInspectContainerESB = dockerController.inspectContainer(jsonObjectESB);
        Object jsonInspectObjESB = new JSONObject(responseInspectContainerESB).
                get("NetworkSettings");
        String esbContainerIP = ((JSONObject) jsonInspectObjESB).get("IPAddress").toString();
        String esbContainerPort = ((JSONObject) jsonInspectObjESB).get("Ports").toString();
    }


    private void getBackendUrl() {
        backendURL = "https://" + asContainerIP + ":" + asContainerPort + "/services/";
    }

    /*public String login(String userName, String password, String host)
            throws LoginAuthenticationExceptionException, RemoteException {
        Boolean loginStatus;
        ServiceContext serviceContext;
        String sessionCookie;
        loginStatus = authenticationAdminStub.login(userName, password, host);
        if (!loginStatus) {
            throw new LoginAuthenticationExceptionException("Login Unsuccessful. Return false as a login status by Server");
        }
        log.info("Login Successful");
        serviceContext = authenticationAdminStub._getServiceClient().getLastOperationContext().getServiceContext();
        sessionCookie = (String) serviceContext.getProperty(HTTPConstants.COOKIE_STRING);
        if (log.isDebugEnabled()) {
            log.debug("SessionCookie :" + sessionCookie);
        }
        return sessionCookie;
    }*/

    /*protected void init(TestUserMode testUserMode) throws Exception {
        asServer = new AutomationContext("AS", testUserMode);
        loginLogoutClient = new LoginLogoutClient(asServer);
          sessionCookie = loginLogoutClient.login();
        backendURL = asServer.getContextUrls().getBackEndUrl();
        webAppURL = asServer.getContextUrls().getWebAppURL();
        userInfo = asServer.getContextTenant().getContextUser();
    }*/


    private void performDBOperations() throws SQLException, ClassNotFoundException, IOException, InterruptedException {

        Thread.sleep(40000);

        MySqlDatabaseManager dbs = new MySqlDatabaseManager("jdbc:mysql://" + mysqlContainerIP + ":3306/?zeroDateTimeBehavior=convertToNull",
                "root", "ROOT");

        // create DBs
        dbs.executeDBScript(resourceLocation + "/scripts/createdatabases.sql");

        //create Tables - wso2_config_DB
        dbs.execute("USE WSO2_CONFIG_DB");
        dbs.executeDBScript(resourceLocation + "/scripts/mysql.sql");

        //create Tables - wso2_gov_DB
        dbs.execute("USE WSO2_GOV_DB");
        dbs.executeDBScript(resourceLocation + "/scripts/mysql.sql");

        //create Tables - wso2_user_DB
        dbs.execute("USE WSO2_USER_DB");
        dbs.executeDBScript(resourceLocation + "/scripts/mysql.sql");

    }


  /*  private OMElement createPayload() throws Exception {
        String request = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:aut=\"http://authentication.services.core.carbon.wso2.org\">\n" +
                "   <soapenv:Header/>\n" +
                "   <soapenv:Body>\n" +
                "      <aut:login>\n" +
                "         <!--Optional:-->\n" +
                "         <aut:username>admin</aut:username>\n" +
                "         <!--Optional:-->\n" +
                "         <aut:password>admin</aut:password>\n" +
                "         <!--Optional:-->\n" +
                "         <aut:remoteAddress></aut:remoteAddress>\n" +
                "      </aut:login>\n" +
                "   </soapenv:Body>\n" +
                "</soapenv:Envelope>\n";
        return new StAXOMBuilder(new ByteArrayInputStream(request.getBytes())).getDocumentElement();
    }*/

    @AfterClass(alwaysRun = true)
    public void tearDown() throws IOException {
        DockerController.removeFiles(resourceLocation + File.separator + "temp");
    }

}
