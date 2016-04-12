/*
*Copyright (c) 2005-2010, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
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
*//*

package org.wso2.appserver.distributed.admin.clients;

import junit.framework.Assert;
import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.om.impl.builder.StAXOMBuilder;
import org.apache.axis2.AxisFault;
import org.apache.axis2.description.WSDL2Constants;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.config.xml.XMLConfigConstants;
import org.apache.synapse.core.axis2.ProxyService;
import org.wso2.carbon.proxyadmin.stub.ProxyServiceAdminProxyAdminException;
import org.wso2.carbon.proxyadmin.stub.ProxyServiceAdminStub;
import org.wso2.carbon.proxyadmin.stub.types.carbon.Entry;
import org.wso2.carbon.proxyadmin.stub.types.carbon.ProxyData;
import org.wso2.carbon.proxyadmin.stub.types.carbon.ProxyServicePolicyInfo;
import org.wso2.esb.integration.common.clients.client.utils.AuthenticateStub;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Locale;
import java.util.ResourceBundle;

*/
/**
 * This class exposing internal command methods to invoke ProxyAdmin admin service
 *//*


public class ProxyServiceAdminClient {
    private static final Log log = LogFactory.getLog(ProxyServiceAdminClient.class);

    private ProxyServiceAdminStub proxyServiceAdminStub;
    private final String serviceName = "ProxyServiceAdmin";

    */
/**
     * Authenticate stub
     *
     * @param backEndUrl server backend URL
     * @throws org.apache.axis2.AxisFault
     *//*

    public ProxyServiceAdminClient(String backEndUrl, String sessionCookie) throws AxisFault {

        String endPoint = backEndUrl + serviceName;
        proxyServiceAdminStub = new ProxyServiceAdminStub(endPoint);
        AuthenticateStub.authenticateStub(sessionCookie, proxyServiceAdminStub);
    }

    public ProxyServiceAdminClient(String backEndUrl, String userName, String password)
            throws AxisFault {

        String endPoint = backEndUrl + serviceName;
        proxyServiceAdminStub = new ProxyServiceAdminStub(endPoint);
        AuthenticateStub.authenticateStub(userName, password, proxyServiceAdminStub);
    }
*/
/*

    */
/**
     * @param data
     * @throws ProxyServiceAdminProxyAdminException
     *
     * @throws java.io.IOException
     * @throws javax.xml.stream.XMLStreamException
     *//*

    public void addProxyService(OMElement data)
            throws Exception {

        ProxyData proxyData = getProxyData(data.toString());
        try {
            proxyServiceAdminStub.addProxy(proxyData);
        } catch (Exception e) {
            throw new Exception("Error while adding proxy ", e);
        }
        log.info("Proxy Added");
    }

    */
/**
     * Delete proxy service
     *
     * @param proxyName Name of the proxy service to be deleted
     * @throws ProxyServiceAdminProxyAdminException
     *                         proxy admin exception
     * @throws java.rmi.RemoteException remote exception
     *//*

    public void deleteProxy(String proxyName)
            throws ProxyServiceAdminProxyAdminException, RemoteException {
        AuthenticateStub auth = new AuthenticateStub();

        proxyServiceAdminStub.deleteProxyService(proxyName);
        log.info("Proxy Deleted");
    }

}
*/
