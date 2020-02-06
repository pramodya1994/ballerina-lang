/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.ballerinalang.module;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import org.ballerinalang.jvm.util.exceptions.BallerinaConnectorException;
import org.ballerinalang.module.util.ModuleCliUtil;
import org.ballerinalang.net.http.HttpConnectionManager;
import org.ballerinalang.net.http.HttpConstants;
import org.ballerinalang.net.http.HttpErrorType;
import org.ballerinalang.net.http.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.transport.http.netty.contract.Constants;
import org.wso2.transport.http.netty.contract.HttpClientConnector;
import org.wso2.transport.http.netty.contract.HttpClientConnectorListener;
import org.wso2.transport.http.netty.contract.HttpResponseFuture;
import org.wso2.transport.http.netty.contract.config.ProxyServerConfiguration;
import org.wso2.transport.http.netty.contract.config.SenderConfiguration;
import org.wso2.transport.http.netty.contractimpl.sender.channel.pool.PoolConfiguration;
import org.wso2.transport.http.netty.message.HttpCarbonMessage;
import org.wso2.transport.http.netty.message.HttpConnectorUtil;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.ballerinalang.module.util.HttpUtils.getURLProperties;

public class Push {

    private static Logger log = LoggerFactory.getLogger(Push.class);
    private static volatile Semaphore executionWaitSem = new Semaphore(0);
    private static int timeOut = 120;

    public static void pushModule(String urlWithModulePath, String proxyHost, int proxyPort, String proxyUsername,
            String proxyPassword, String accessToken, String organization, String pathToBalo, String outputLog) {


//        urlWithModulePath = "https://localhost:9095/hello/sayHello";


        HttpClientConnector httpClientConnector = initializeHttpClientConnector(urlWithModulePath, proxyHost, proxyPort,
                proxyUsername, proxyPassword);
        HttpMethod httpReqMethod = new HttpMethod("POST");
        HttpCarbonMessage cMessage = new HttpCarbonMessage(
                new DefaultHttpRequest(HttpVersion.HTTP_1_1, httpReqMethod, urlWithModulePath));

        cMessage.setHeader("Authorization", "Bearer " + accessToken);
        cMessage.setHeader("Push-Organization", organization);
        cMessage.setHeader("Content-Type", "application/octet-stream");

        Map tokenUrlProperties = getURLProperties(urlWithModulePath);
        // Set protocol type http or https
        cMessage.setProperty(Constants.PROTOCOL, tokenUrlProperties.get(Constants.PROTOCOL));
        // Set uri
        cMessage.setProperty(Constants.TO, tokenUrlProperties.get(Constants.TO));
        // set Host
        cMessage.setProperty(Constants.HTTP_HOST, tokenUrlProperties.get(Constants.HTTP_HOST));
        //set port
        cMessage.setProperty(Constants.HTTP_PORT,
                Integer.valueOf((String) tokenUrlProperties.get(Constants.HTTP_PORT)));
        // Set method
        cMessage.setHttpMethod(String.valueOf(httpReqMethod));
        //Set request URL
        cMessage.setRequestUrl((String) tokenUrlProperties.get("REQUEST_URL"));

        setHttpContent(cMessage, pathToBalo);
//        cMessage.completeMessage();

        HttpResponseFuture responseFuture = httpClientConnector.send(cMessage);

        responseFuture.setHttpConnectorListener(new HttpClientConnectorListener() {
            @Override public void onMessage(HttpCarbonMessage httpMessage) {
                try {
                    HttpContent httpContent = httpMessage.getHttpContent();
                    ByteBuf content = httpContent.content();
                    String str = content.toString();
                    executionWaitSem.release();
                } catch (Exception e) {
                    throw ModuleCliUtil.createModuleCliException(e.getMessage());
                }
            }

            @Override public void onError(Throwable throwable) {
                System.out.println(throwable);
                executionWaitSem.release();
            }
        });

        sync();

        if (responseFuture.getStatus().isSuccess()) {
            log.info(outputLog);
        } else {
            throw ModuleCliUtil.createModuleCliException(responseFuture.getStatus().getCause().getMessage());
        }
    }

    public static void sync() {
        try {
            executionWaitSem.tryAcquire(timeOut, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            //ignore
        }
    }

    static HttpClientConnector initializeHttpClientConnector(String urlWithModulePath, String proxyHost,
            int proxyPort, String proxyUsername, String proxyPassword) {
        HttpConnectionManager connectionManager = HttpConnectionManager.getInstance();
        Map<String, Object> properties = HttpConnectorUtil
                .getTransportProperties(connectionManager.getTransportConfig());

        URL url;
        try {
            url = new URL(urlWithModulePath);
        } catch (MalformedURLException e) {
            throw ModuleCliUtil.createModuleCliException("malformed URL: " + urlWithModulePath);
        }

        SenderConfiguration senderConfiguration = new SenderConfiguration();
        senderConfiguration.setScheme(url.getProtocol());
        senderConfiguration.setTLSStoreType(HttpConstants.PKCS_STORE_TYPE);
        senderConfiguration.setTrustStoreFile(
                "/Users/pramodya/Documents/ei/ballerina-platform/ballerina-lang/cli/ballerina-module-cli/src/main/resources/ballerinaTruststore.p12");
        senderConfiguration.setTrustStorePass("ballerina");
        if (connectionManager.isHTTPTraceLoggerEnabled()) {
            senderConfiguration.setHttpTraceLogEnabled(true);
        }

        if (!proxyHost.equals("") && proxyPort > 0) {
            try {
                populateProxyConfigurations(senderConfiguration, proxyHost, proxyPort, proxyUsername, proxyPassword);
            } catch (RuntimeException e) {
                throw HttpUtil.createHttpError(e.getMessage(), HttpErrorType.GENERIC_CLIENT_ERROR);
            }
        } else if (!proxyHost.equals("") || proxyPort > 0) {
            throw ModuleCliUtil.createModuleCliException("both host and port should be provided to enable proxy");
        }

        return HttpUtil.createHttpWsConnectionFactory()
                .createHttpClientConnector(properties, senderConfiguration);
    }

    private static void populateProxyConfigurations(SenderConfiguration senderConfiguration, String host, int port,
            String username, String password) {
        ProxyServerConfiguration proxyServerConfiguration;

        try {
            proxyServerConfiguration = new ProxyServerConfiguration(host, port);
        } catch (UnknownHostException e) {
            throw new BallerinaConnectorException("Failed to resolve host" + host, e);
        }
        if (!username.isEmpty()) {
            proxyServerConfiguration.setProxyUsername(username);
        }
        if (!password.isEmpty()) {
            proxyServerConfiguration.setProxyPassword(password);
        }
        senderConfiguration.setProxyServerConfiguration(proxyServerConfiguration);
    }

    private static void setHttpContent(HttpCarbonMessage cMsg, String baloPath) {



//        baloPath = "/Users/pramodya/Downloads/test/data.json";


        final int BUFFER_SIZE = 1024 * 4;
        byte[] buffer = new byte[BUFFER_SIZE];
        Path pp = FileSystems.getDefault().getPath(baloPath);

        try (FileInputStream fis = new FileInputStream(pp.toFile())) {
            int read = 0;
            while ((read = fis.read(buffer)) > 0) {
                if (!(read < BUFFER_SIZE)) {
                    DefaultHttpContent defaultHttpContent = new DefaultHttpContent(Unpooled.wrappedBuffer(buffer));
                    cMsg.addHttpContent(defaultHttpContent);
                } else {
                    DefaultLastHttpContent defaultLastHttpContent = new DefaultLastHttpContent(Unpooled.wrappedBuffer(buffer));
                    cMsg.addHttpContent(defaultLastHttpContent);
                }
            }
        } catch (FileNotFoundException e) {
            throw ModuleCliUtil.createModuleCliException("cannot read the balo: " + baloPath);
        } catch (IOException e) {
            throw ModuleCliUtil.createModuleCliException("cannot read the balo content");
        }
    }
}
