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
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.appserver.distributed.admin.clients.AARServiceUploaderClient;
import org.wso2.appserver.distributed.admin.clients.ServiceDeploymentUtil;
import org.wso2.carbon.automation.engine.frameworkutils.FrameworkPathUtil;
import org.wso2.carbon.automation.extensions.distributed.extensions.BaseManager;
import org.wso2.carbon.integration.common.admin.client.AuthenticatorClient;

import java.io.File;
import java.io.IOException;

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

    private String mysqlContainerIP;
    private String asContainerIP;
    private String asContainerPort;
    private String esbContainerIP;
    private String backendURL;

    private static final Log log = LogFactory.getLog(SampleTestCase.class);

    @BeforeClass(alwaysRun = true)
    public void init() throws Exception {

    }

    @Test(groups = "wso2.as.esb", description = "Upload aar service and verify deployment")
    public void testAarServiceUpload() throws Exception {


        asContainerIP = System.getenv().get("asContainerIP");
        asContainerPort = System.getenv().get("asContainerPort");

        backendURL = "https://" + asContainerIP + ":" + asContainerPort + "/services/";

        AuthenticatorClient authenticatorClient = new AuthenticatorClient(backendURL);

        String sessionCookie = authenticatorClient.login("admin", "admin", asContainerIP);

        AARServiceUploaderClient aarServiceUploaderClient
                = new AARServiceUploaderClient(backendURL, sessionCookie);
        aarServiceUploaderClient.uploadAARFile("Axis2Service.aar",
                FrameworkPathUtil.getSystemResourceLocation() + "artifacts" +
                        File.separator + "common" + File.separator + "aarservices" + File.separator +
                        "HelloWorld.aar", ""
        );
        ServiceDeploymentUtil.isServiceDeployed(backendURL,
                sessionCookie, "HelloService");
        log.info("HelloWorld.aar service uploaded successfully");
    }

    @Test(groups = "wso2.as.esb", description = "Deploy proxy service and invoke backend",
            dependsOnMethods = "testAarServiceUpload")
    public void invokeService() throws Exception {

        AuthenticatorClient authenticatorClient = new AuthenticatorClient(backendURL);

        String sessionCookie = authenticatorClient.login("admin", "admin", esbContainerIP);
    }


/*
    @Test(groups = "wso2.as.esb", description = "Start ESB Container", dependsOnMethods = "testAsMysqlStartContainers")
    public void testStartESBContainer() throws Exception {

        // copy product pack
        BaseManager.copyFiles(resourceLocation + "productpack" + File.separator + "wso2esb-4.9.0.zip",
                resourceLocation + File.separator + "temp" + File.separator + "puppet-module" + File.separator
                        + "modules" + File.separator + "wso2esb" + File.separator + "files" + File.separator +
                        "wso2esb-4.9.0.zip"
        );

        // copy yaml files
        BaseManager.copyFiles(resourceLocation + File.separator + "yaml" + File.separator + "esb"
                + File.separator + "default.yaml", resourceLocation + File.separator + "temp" + File.separator +
                "puppet-module/hieradata/dev/wso2/wso2esb/4.9.0/default.yaml");

        //run wso2esb image
        JSONObject jsonObjectESB = BaseManager.runDockerImage(resourceLocation + File.separator + "json"
                        + File.separator + "wso2esbcreatecontainer.json",
                resourceLocation + File.separator + "json" + File.separator + "wso2esbstartcontainer.json"
        );

        ClientResponse responseInspectContainerESB = BaseManager.inspectContainer(jsonObjectESB);
        Object jsonInspectObjESB = new JSONObject(responseInspectContainerESB).
                get("NetworkSettings");
        esbContainerIP = ((JSONObject) jsonInspectObjESB).get("IPAddress").toString();
        esbContainerPort = ((JSONObject) jsonInspectObjESB).get("Ports").toString();
    }
*/




    /*private OMElement loadResource(String path) throws FileNotFoundException,
            XMLStreamException {
        OMElement documentElement = null;
        FileInputStream inputStream = null;
        XMLStreamReader parser = null;
        StAXOMBuilder builder = null;
        path = TestConfigurationProvider.getResourceLocation() + path;
        File file = new File(path);
        if (file.exists()) {
            try {
                inputStream = new FileInputStream(file);
                parser = XMLInputFactory.newInstance().createXMLStreamReader(inputStream);
                //create the builder
                builder = new StAXOMBuilder(parser);
                //get the root element (in this case the envelope)
                documentElement = builder.getDocumentElement().cloneOMElement();
            } finally {
                if (builder != null) {
                    builder.close();
                }
                if (parser != null) {
                    try {
                        parser.close();
                    } catch (XMLStreamException e) {
                        //ignore
                    }
                }
                if (inputStream != null) {
                    try {
                        inputStream.close();
                    } catch (IOException e) {
                        //ignore
                    }
                }

            }
        } else {
            throw new FileNotFoundException("File Not Exist at " + path);
        }
        return documentElement;
    }

    public void updateESBConfiguration(OMElement synapseConfig, String backendURL,
                                       String sessionCookie)
            throws Exception {

        ServiceAdminClient adminServiceService = new ServiceAdminClient(backendURL, sessionCookie);
        ProxyServiceAdminClient proxyAdmin = new ProxyServiceAdminClient(backendURL, sessionCookie);

        Iterator<OMElement> proxies = synapseConfig.getChildrenWithLocalName("proxy");

        while (proxies.hasNext()) {
            OMElement proxy = proxies.next();
            String proxyName = proxy.getAttributeValue(new QName("name"));
            if (adminServiceService.isServiceExists(proxyName)) {
                proxyAdmin.deleteProxy(proxyName);
                assertTrue(isProxyUnDeployed(backendURL, sessionCookie, proxyName), proxyName + " Undeployment failed");
            }
            proxyAdmin.addProxyService(proxy);
            assertTrue(isProxyDeployed(backendURL, sessionCookie, proxyName), proxyName + " deployment failed");
            log.info(proxyName + " Proxy Uploaded");
        }
    }*/



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
        BaseManager.removeFiles(FrameworkPathUtil.getSystemResourceLocation() + File.separator + "temp");
    }

}
