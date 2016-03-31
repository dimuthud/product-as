package org.wso2.appserver.distributed.tests;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.wso2.appserver.distributed.common.utils.DockerController;
import org.wso2.carbon.automation.engine.context.AutomationContext;
import org.wso2.carbon.automation.engine.context.ContextXpathConstants;
import org.wso2.carbon.automation.test.utils.common.TestConfigurationProvider;

import javax.xml.xpath.XPathExpressionException;
import java.io.File;

/**
 * Created by dimuthu on 3/23/16.
 */
public class NewSampleTestCase {

    private AutomationContext automationContext;
    private DockerController dockerController;
    private String resourceLocation;
    private String mysqlContainerIP;
    //private String dirPath = "/home/dimuthu/Desktop/Products/C5Automation/SampleProject/mavn2/src/main/resources/temp/dockerfiles";


    @BeforeClass(alwaysRun = true)
    public void init() throws XPathExpressionException {

        automationContext = new AutomationContext("AS", "appServerInstance0001", ContextXpathConstants.SUPER_TENANT,
                ContextXpathConstants.SUPER_ADMIN);
        dockerController = new DockerController();
        resourceLocation = TestConfigurationProvider.getResourceLocation("AS");

    }

    @Test(groups = "wso2.as", description = "H2DB Password changing script run test")
    public void testScriptRunChangeUserPasswordH2DB() throws Exception {

        // git clone - dockerfile repo
        dockerController.gitRepoClone("https://github.com/wso2/dockerfiles.git", resourceLocation
                + File.separator + "temp");

        // git clone - puppetmodule repo
        dockerController.gitRepoClone("https://github.com/wso2/puppet-modules.git", resourceLocation
                + File.separator + "temp");

        // Building mysql-container
        dockerController.buildDockerFile("http://127.0.0.1:2375", resourceLocation + File.separator
                + "mysqldockerfile", "mysql:5.7.11");

        // Running mysql docker image
        dockerController.runDockerImage(resourceLocation + "/json/mysqlcreatecontainer.json",
                resourceLocation + "/json/mysqlstartcontainer.json");


        //build wso2base image
        //TODO

        //run wso2as image
        dockerController.runDockerImage(resourceLocation + File.separator + "json"
                + File.separator + "wso2ascreatecontainer.json", resourceLocation + "/json/wso2asstartcontainer.json");

    }

}
