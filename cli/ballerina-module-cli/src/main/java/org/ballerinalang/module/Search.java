package org.ballerinalang.module;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.base64.Base64;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.LastHttpContent;
import org.ballerinalang.module.util.ModuleCliUtil;
import org.wso2.transport.http.netty.contract.Constants;
import org.wso2.transport.http.netty.contract.HttpClientConnector;
import org.wso2.transport.http.netty.contract.HttpClientConnectorListener;
import org.wso2.transport.http.netty.contract.HttpResponseFuture;
import org.wso2.transport.http.netty.message.HttpCarbonMessage;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static org.ballerinalang.module.util.HttpUtils.getURLProperties;

public class Search {
    private static volatile Semaphore executionWaitSem = new Semaphore(0);
    private static int timeOut = 120;

    public static void searchModule(String urlWithModulePath, String query, String proxyHost, int proxyPost,
            String proxyUsername, String proxyPassword, String terminalWidth) {
        urlWithModulePath = urlWithModulePath + "?q=" + query;
        urlWithModulePath = "http://www.mocky.io/v2/5e3bac93300000942d214657";

        HttpClientConnector httpClientConnector = Push
                .initializeHttpClientConnector(urlWithModulePath, proxyHost, proxyPost, proxyUsername, proxyPassword);

        HttpMethod httpReqMethod = new HttpMethod("GET");
        HttpCarbonMessage cMessage = new HttpCarbonMessage(
                new DefaultHttpRequest(HttpVersion.HTTP_1_1, httpReqMethod, urlWithModulePath));

        cMessage.setHeader("Accept", "*/*");
        cMessage.setHeader("Cache-Control", "no-cache");
//        cMessage.setHeader("Host", "api.central.ballerina.io");
        cMessage.setHeader("Host", "mocky.io");
        cMessage.setHeader("Accept-Encoding", "gzip, deflate");
        cMessage.setHeader("Connection", "keep-alive");

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
        cMessage.completeMessage();


        HttpResponseFuture responseFuture = httpClientConnector.send(cMessage);

        try {
            responseFuture.setHttpConnectorListener(new HttpClientConnectorListener() {
                @Override public void onMessage(HttpCarbonMessage httpMessage) {
                    System.out.println(httpMessage.isEmpty());
                    System.out.println(httpMessage.getFullMessageLength());
                    String response = "";
                    System.out.println("onMessage started");
                    do {
                        try {
                            HttpContent httpContent = httpMessage.getHttpContent();
                            ByteBuf content = httpContent.content();
                            ByteBuf encodedByteBuf = Base64.encode(content);
                            response = response + encodedByteBuf.toString(StandardCharsets.UTF_8);
                            if (httpContent instanceof LastHttpContent) {
                                break;
                            }
                        } catch (Exception e) {
                            System.out.println(e.getMessage());
                            break;
                        }
                    } while (true);
                    System.out.println(response);
                    executionWaitSem.release();
                    System.out.println("onMessage over");
                }

                @Override public void onError(Throwable throwable) {
                    System.out.println(throwable);
                    executionWaitSem.release();
                }
            });
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        System.out.println("sync started");
        sync();
        System.out.println("sync over");
    }

    public static void sync() {
        try {
            executionWaitSem.tryAcquire(timeOut, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            System.out.println(e.getMessage());
            throw ModuleCliUtil.createModuleCliException(e.getMessage());
        }
    }
}
