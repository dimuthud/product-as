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

package org.wso2.appserver.distributed.common.utils;

import io.fabric8.docker.client.Config;
import io.fabric8.docker.client.ConfigBuilder;
import io.fabric8.docker.client.DefaultDockerClient;
import io.fabric8.docker.client.DockerClient;

import javax.ws.rs.core.MediaType;

import io.fabric8.docker.dsl.EventListener;
import org.apache.commons.io.FileUtils;
import org.apache.wink.client.ClientResponse;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.wso2.carbon.automation.test.utils.wink.client.GenericRestClient;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.testng.Assert.assertEquals;
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
 */

public class DockerController {

    private static JSONParser parser = new JSONParser();
    private static Map<String, String> queryParamMap = new HashMap<String, String>();
    private static Map<String, String> headerMap = new HashMap<String, String>();
    private GenericRestClient genericRestClient;

    public DockerController(){
        genericRestClient = new GenericRestClient();
    }

    /**
     * @param dockerURL - docker daemon url
     * @param dockerFileLocation - docker image file location
     * @param repositoryName - repo name
     */
    public void buildDockerFile(String dockerURL, String dockerFileLocation, String repositoryName) {

        // Building docker image
        Config config = new ConfigBuilder()
                .withDockerUrl(dockerURL)
                .build();

        DockerClient clientOne = new DefaultDockerClient(config);

        clientOne.image().build().withRepositoryName(repositoryName).usingListener(new EventListener() {
            @Override
            public void onSuccess(String message) {
                System.out.println(message);
            }

            @Override
            public void onError(String errMessage) {
                System.err.println("Failure:" + errMessage);
            }

            @Override
            public void onEvent(String eventMessage) {
                System.out.println(eventMessage);
            }
        }).fromFolder(dockerFileLocation);

    }

    /**
     * Running of docker image
     * @param createContainerFilePath - file location contains create container request information
     * @param startContainer - file location contains start container request information
     * @return Json object of container info
     * @throws IOException
     * @throws ParseException
     * @throws JSONException
     */
    public JSONObject runDockerImage(String createContainerFilePath, String startContainer) throws IOException,
            ParseException, JSONException {

        // create container
        Object createObject = parser.parse(new FileReader(createContainerFilePath));

        String type = "restservice";
        queryParamMap.put("type", type);

        ClientResponse responseCreateContainer =
                genericRestClient.geneticRestRequestPost("http://localhost:2375/containers/create",
                        MediaType.APPLICATION_JSON, MediaType.APPLICATION_JSON,
                        createObject.toString(), queryParamMap, headerMap, null);

        JSONObject jsonObj = new JSONObject(responseCreateContainer.getEntity(String.class));

        assertNotNull(jsonObj.get("Id"), "ID should not be null");

        // start container
        Object startObject = parser.parse(new FileReader(startContainer));

        ClientResponse responseStartContainer =
                genericRestClient.geneticRestRequestPost("http://localhost:2375/containers/" +
                                jsonObj.get("Id").toString() + "/start", MediaType.APPLICATION_JSON,
                        MediaType.APPLICATION_JSON, startObject.toString(), queryParamMap, headerMap, null
                );

        assertNotNull(responseStartContainer);

        return jsonObj;
    }

    /**
     * Returns docker container related information
     * @param jsonObj - json object contains params for starting container
     * @return container information
     * @throws JSONException
     */
    public ClientResponse inspectContainer(JSONObject jsonObj) throws JSONException {
        // inspect container
        ClientResponse responseInspectContainer =
                genericRestClient.geneticRestRequestGet("http://localhost:2375/containers/" + jsonObj.get("Id").toString() + "/json",
                        queryParamMap, headerMap, null);

        return responseInspectContainer;

    }

    /**
     * This methods clones specified git repo
     * @param gitURL - URL of the git repository
     * @param dirPath - Destination directory location
     * @throws IOException
     * @throws GitAPIException
     */
    public void gitRepoClone(String gitURL ,String dirPath) throws IOException, GitAPIException {
        CloneCommand cloneCommand = Git.cloneRepository();
        File dirLocation = new File(dirPath);

        if (dirLocation.exists()) {
            FileUtils.forceDelete(dirLocation);
        }

        cloneCommand.setURI(gitURL);
        cloneCommand.setDirectory(dirLocation);

        Git local = cloneCommand.call();

        assertEquals(local.getRepository().getDirectory(), new File(dirLocation, ".git"));
        assertEquals(local.getRepository().getWorkTree(), dirLocation);
    }

    /**
     * File operation handler utility
     * @param sourceLocation - Source directory location
     * @param destinationLocation - Destination directory location
     * @throws IOException
     */
    public static void copyFiles(String sourceLocation, String destinationLocation) throws IOException {
        FileUtils.copyFile(new File(sourceLocation), new File(destinationLocation));
    }

    /**
     * File remover handler utility
     * @param sourceLocation -  Source directory location
     * @throws IOException
     */
    public static void removeFiles(String sourceLocation) throws IOException {
        FileUtils.forceDelete(new File(sourceLocation));
    }
}