// Copyright (c) 2018, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
//
// WSO2 Inc. licenses this file to you under the Apache License,
// Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

import ballerina/log;

# Provides secure HTTP remote functions for interacting with HTTP endpoints. This will make use of the authentication
# schemes configured in the HTTP client endpoint to secure the HTTP requests.
#
# + url - The URL of the remote HTTP endpoint
# + config - The configurations of the client endpoint associated with this `HttpActions` instance
# + httpClient - The underlying `HttpActions` instance, which will make the actual network calls
public type HttpSecureClient client object {
    //These properties are populated from the init call and sent to the client connector as these will be needed at a
    //later stage for retrying and in other few places.
    public string url = "";
    public ClientEndpointConfig config = {};
    public HttpClient httpClient;

    public function __init(string url, ClientEndpointConfig config) {
        self.url = url;
        self.config = config;
        var simpleClient = createClient(url, self.config);
        if (simpleClient is HttpClient) {
            self.httpClient = simpleClient;
        } else {
            panic <error> simpleClient;
        }
    }

    # This wraps the `post()` function of the underlying HTTP remote functions provider. Add relevant authentication
    # headers to the request and send the request to actual network call.
    #
    # + path - Resource path
    # + message - An HTTP outbound request message or any payload of type `string`, `xml`, `json`, `byte[]`,
    #             `io:ReadableByteChannel` or `mime:Entity[]`
    # + return - The inbound response message or the error if one occurred while attempting to fulfill the HTTP request
    public remote function post(string path, RequestMessage message) returns Response|ClientError {
        Request req = <Request>message;
        req = check prepareSecureRequest(req, self.config);
        Response res = check self.httpClient->post(path, req);
        var inspection = check doInspection(req, res, self.config);
        if (inspection is Request) {
            return self.httpClient->post(path, inspection);
        }
        return res;
    }

    # This wraps the `head()` function of the underlying HTTP remote functions provider. Add relevant authentication
    # headers to the request and send the request to actual network call.
    #
    # + path - Resource path
    # + message - An optional HTTP outbound request message or any payload of type `string`, `xml`, `json`, `byte[]`,
    #             `io:ReadableByteChannel` or `mime:Entity[]`
    # + return - The inbound response message or the error if one occurred while attempting to fulfill the HTTP request
    public remote function head(string path, public RequestMessage message = ()) returns Response|ClientError {
        Request req = <Request>message;
        req = check prepareSecureRequest(req, self.config);
        Response res = check self.httpClient->head(path, message = req);
        var inspection = check doInspection(req, res, self.config);
        if (inspection is Request) {
            return self.httpClient->head(path, message = inspection);
        }
        return res;
    }

    # This wraps the `put()` function of the underlying HTTP remote functions provider. Add relevant authentication
    # headers to the request and send the request to actual network call.
    #
    # + path - Resource path
    # + message - An HTTP outbound request message or any payload of type `string`, `xml`, `json`, `byte[]`,
    #             `io:ReadableByteChannel` or `mime:Entity[]`
    # + return - The inbound response message or an error occurred while attempting to fulfill the HTTP request
    public remote function put(string path, RequestMessage message) returns Response|ClientError {
        Request req = <Request>message;
        req = check prepareSecureRequest(req, self.config);
        Response res = check self.httpClient->put(path, req);
        var inspection = check doInspection(req, res, self.config);
        if (inspection is Request) {
            return self.httpClient->put(path, inspection);
        }
        return res;
    }

    # This wraps the `execute()` function of the underlying HTTP remote functions provider. Add relevant authentication
    # headers o the request and send the request to actual network call.
    #
    # + httpVerb - HTTP verb value
    # + path - Resource path
    # + message - An HTTP outbound request message or any payload of type `string`, `xml`, `json`, `byte[]`,
    #             `io:ReadableByteChannel` or `mime:Entity[]`
    # + return - The inbound response message or an error occurred while attempting to fulfill the HTTP request
    public remote function execute(string httpVerb, string path, RequestMessage message) returns Response|ClientError {
        Request req = <Request>message;
        req = check prepareSecureRequest(req, self.config);
        Response res = check self.httpClient->execute(httpVerb, path, req);
        var inspection = check doInspection(req, res, self.config);
        if (inspection is Request) {
            return self.httpClient->execute(httpVerb, path, inspection);
        }
        return res;
    }

    # This wraps the `patch()` function of the underlying HTTP remote functions provider. Add relevant authentication
    # headers to the request and send the request to actual network call.
    #
    # + path - Resource path
    # + message - An HTTP outbound request message or any payload of type `string`, `xml`, `json`, `byte[]`,
    #             `io:ReadableByteChannel` or `mime:Entity[]`
    # + return - The inbound response message or an error occurred while attempting to fulfill the HTTP request
    public remote function patch(string path, RequestMessage message) returns Response|ClientError {
        Request req = <Request>message;
        req = check prepareSecureRequest(req, self.config);
        Response res = check self.httpClient->patch(path, req);
        var inspection = check doInspection(req, res, self.config);
        if (inspection is Request) {
            return self.httpClient->patch(path, inspection);
        }
        return res;
    }

    # This wraps the `delete()` function of the underlying HTTP remote functions provider. Add relevant authentication
    # headers to the request and send the request to actual network call.
    #
    # + path - Resource path
    # + message - An HTTP outbound request message or any payload of type `string`, `xml`, `json`, `byte[]`,
    #             `io:ReadableByteChannel` or `mime:Entity[]`
    # + return - The inbound response message or the error if one occurred while attempting to fulfill the HTTP request
    public remote function delete(string path, public RequestMessage message = ()) returns Response|ClientError {
        Request req = <Request>message;
        req = check prepareSecureRequest(req, self.config);
        Response res = check self.httpClient->delete(path, req);
        var inspection = check doInspection(req, res, self.config);
        if (inspection is Request) {
            return self.httpClient->delete(path, inspection);
        }
        return res;
    }

    # This wraps the `get()` function of the underlying HTTP remote functions provider. Add relevant authentication
    # headers to the request and send the request to actual network call.
    #
    # + path - Request path
    # + message - An HTTP outbound request message or any payload of type `string`, `xml`, `json`, `byte[]`,
    #             `io:ReadableByteChannel` or `mime:Entity[]`
    # + return - The inbound response message or the error if one occurred while attempting to fulfill the HTTP request
    public remote function get(string path, public RequestMessage message = ()) returns Response|ClientError {
        Request req = <Request>message;
        req = check prepareSecureRequest(req, self.config);
        Response res = check self.httpClient->get(path, message = req);
        var inspection = check doInspection(req, res, self.config);
        if (inspection is Request) {
            return self.httpClient->get(path, message = inspection);
        }
        return res;
    }

    # This wraps the `options()` function of the underlying HTTP remote functions provider. Add relevant authentication
    # headers to the request and send the request to actual network call.
    #
    # + path - Request path
    # + message - An optional HTTP outbound request message or any payload of type `string`, `xml`, `json`, `byte[]`,
    #             `io:ReadableByteChannel` or `mime:Entity[]`
    # + return - The inbound response message or the error if one  occurred while attempting to fulfill the HTTP request
    public remote function options(string path, public RequestMessage message = ()) returns Response|ClientError {
        Request req = <Request>message;
        req = check prepareSecureRequest(req, self.config);
        Response res = check self.httpClient->options(path, message = req);
        var inspection = check doInspection(req, res, self.config);
        if (inspection is Request) {
            return self.httpClient->options(path, message = inspection);
        }
        return res;
    }

    # This wraps the `forward()` function of the underlying HTTP remote functions provider. Add relevant authentication
    # headers to the request and send the request to actual network call.
    #
    # + path - Request path
    # + request - An HTTP inbound request message
    # + return - The inbound response message or the error if one occurred while attempting to fulfill the HTTP request
    public remote function forward(string path, Request request) returns Response|ClientError {
        Request req = request;
        req = check prepareSecureRequest(request, self.config);
        Response res = check self.httpClient->forward(path, request);
        var inspection = check doInspection(req, res, self.config);
        if (inspection is Request) {
            return self.httpClient->forward(path, inspection);
        }
        return res;
    }

    # This wraps the `submit()` function of the underlying HTTP remote functions provider. Add relevant authentication
    # headers to the request and send the request to actual network call.
    #
    # + httpVerb - The HTTP verb value
    # + path - The resource path
    # + message - An HTTP outbound request message or any payload of type `string`, `xml`, `json`, `byte[]`,
    #             `io:ReadableByteChannel` or `mime:Entity[]`
    # + return - An `HttpFuture` that represents an asynchronous service invocation, or an error if the submission fails
    public remote function submit(string httpVerb, string path, RequestMessage message) returns HttpFuture|ClientError {
        Request req = <Request>message;
        req = check prepareSecureRequest(req, self.config);
        return self.httpClient->submit(httpVerb, path, req);
    }

    # This just pass the request to actual network call.
    #
    # + httpFuture - The `HttpFuture` relates to a previous asynchronous invocation
    # + return - An HTTP response message, or an error if the invocation fails
    public remote function getResponse(HttpFuture httpFuture) returns Response|ClientError {
        return self.httpClient->getResponse(httpFuture);
    }

    # This just pass the request to actual network call.
    #
    # + httpFuture - The `HttpFuture` relates to a previous asynchronous invocation
    # + return - A `boolean` that represents whether a `PushPromise` exists
    public remote function hasPromise(HttpFuture httpFuture) returns boolean {
        return self.httpClient->hasPromise(httpFuture);
    }

    # This just pass the request to actual network call.
    #
    # + httpFuture - The `HttpFuture` relates to a previous asynchronous invocation
    # + return - An HTTP Push Promise message, or an error if the invocation fails
    public remote function getNextPromise(HttpFuture httpFuture) returns PushPromise|ClientError {
        return self.httpClient->getNextPromise(httpFuture);
    }

    # This just pass the request to actual network call.
    #
    # + promise - The related `PushPromise`
    # + return - A promised HTTP `Response` message, or an error if the invocation fails
    public remote function getPromisedResponse(PushPromise promise) returns Response|ClientError {
        return self.httpClient->getPromisedResponse(promise);
    }

    # This just pass the request to actual network call.
    #
    # + promise - The Push Promise to be rejected
    public remote function rejectPromise(PushPromise promise) {
        return self.httpClient->rejectPromise(promise);
    }
};

# Creates an HTTP client capable of securing HTTP requests with authentication.
#
# + url - Base URL
# + config - Client endpoint configurations
# + return - Created secure HTTP client
public function createHttpSecureClient(string url, ClientEndpointConfig config) returns HttpClient|ClientError {
    HttpSecureClient httpSecureClient;
    if (config.auth is OutboundAuthConfig) {
        httpSecureClient = new(url, config);
        return httpSecureClient;
    } else {
        return createClient(url, config);
    }
}

# Prepare an HTTP request with the required headers for authentication based on the scheme.
#
# + req - An HTTP outbound request message
# + config - Client endpoint configurations
# + return - Prepared HTTP request or `http:ClientError` if an error occurred at auth handler invocation
function prepareSecureRequest(Request req, ClientEndpointConfig config) returns Request|ClientError {
    var auth = config.auth;
    if (auth is OutboundAuthConfig) {
        OutboundAuthHandler authHandler = auth.authHandler;
        return authHandler.prepare(req);
    }
    // Never throw this error since the auth config is already validated.
    return prepareAuthenticationError("Failed to prepare the HTTP request since OutboundAuthConfig is not configured.");
}

# Do inspection with the received HTTP response for the prepared HTTP request.
#
# + req - An HTTP outbound request message
# + res - An HTTP outbound response message
# + config - Client endpoint configurations
# + return - Prepared HTTP request or `()` if nothing to be done or `http:ClientError` if an error occurred at auth handler invocation
function doInspection(Request req, Response res, ClientEndpointConfig config) returns Request|ClientError? {
    var auth = config.auth;
    if (auth is OutboundAuthConfig) {
        OutboundAuthHandler authHandler = auth.authHandler;
        return authHandler.inspect(req, res);
    }
    log:printDebug(function () returns string {
        return "Retry is not required for the given request after the inspection.";
    });
    return ();
}
