/*
 * Copyright (c) 2017, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.wso2.carbon.analytics.idp.client.external.impl;

import feign.Client;
import feign.Feign;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import feign.RequestTemplate;
import feign.Response;
import feign.auth.BasicAuthRequestInterceptor;
import feign.codec.EncodeException;
import feign.codec.Encoder;
import feign.gson.GsonDecoder;
import org.wso2.carbon.analytics.idp.client.core.exception.IdPClientException;
import org.wso2.carbon.analytics.idp.client.core.utils.IdPClientConstants;
import org.wso2.carbon.analytics.idp.client.external.util.SPSSLSocketFactory;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * OAuth2 service stubs.
 */
public class OAuth2ServiceStubs {
    private String tokenEndpoint;
    private String revokeEndpoint;
    private String introspectEndpoint;
    private String kmCertAlias;
    private String username;
    private String password;

    /**
     * Constructor.
     *
     * @param tokenEndpoint      Token endpoint URL
     * @param revokeEndpoint     Revoke endpoint URL
     * @param introspectEndpoint Token introspection endpoint
     * @param kmCertAlias        Key manager certificate alias
     * @param username           Username of Key Manager
     * @param password           Password of Key Manager
     */
    public OAuth2ServiceStubs(String tokenEndpoint, String revokeEndpoint, String introspectEndpoint,
                              String kmCertAlias, String username, String password) {
        this.tokenEndpoint = tokenEndpoint;
        this.revokeEndpoint = revokeEndpoint;
        this.introspectEndpoint = introspectEndpoint;
        this.kmCertAlias = kmCertAlias;
        this.username = username;
        this.password = password;
    }

    /**
     * Create and return OAuth2 token service stubs.
     *
     * @return OAuth2 token service stub
     * @throws IdPClientException if error occurs while crating OAuth2 token service stub
     */
    public OAuth2ServiceStubs.TokenServiceStub getTokenServiceStub() throws IdPClientException {
        return Feign.builder()
                .encoder(new FormEncoder())
                .decoder(new GsonDecoder())
                .client(new Client.Default(SPSSLSocketFactory.getSSLSocketFactory(kmCertAlias),
                        (hostname, sslSession) -> true))
                .target(OAuth2ServiceStubs.TokenServiceStub.class, tokenEndpoint);
    }

    /**
     * Create and return OAuth2 revoke service stubs.
     *
     * @return OAuth2 revoke service stub
     * @throws IdPClientException if error occurs while crating OAuth2 revoke service stub
     */
    public OAuth2ServiceStubs.RevokeServiceStub getRevokeServiceStub() throws IdPClientException {
        return Feign.builder()
                .encoder(new FormEncoder())
                .client(new Client.Default(SPSSLSocketFactory.getSSLSocketFactory(kmCertAlias),
                        (hostname, sslSession) -> true))
                .target(OAuth2ServiceStubs.RevokeServiceStub.class, revokeEndpoint);
    }

    /**
     * Create and return OAuth2 Introspection service stubs.
     *
     * @return OAuth2 introspection service stub
     * @throws IdPClientException if error occurs while crating OAuth2 introspection service stub
     */
    public OAuth2ServiceStubs.IntrospectionServiceStub getIntrospectionServiceStub() throws IdPClientException {
        return Feign.builder()
                .requestInterceptor(new BasicAuthRequestInterceptor(username, password))
                .encoder(new FormEncoder())
                .decoder(new GsonDecoder())
                .client(new Client.Default(SPSSLSocketFactory.getSSLSocketFactory(kmCertAlias),
                        (hostname, sslSession) -> true))
                .target(OAuth2ServiceStubs.IntrospectionServiceStub.class, introspectEndpoint);
    }

    /**
     * This interface is for /token API stub.
     */
    @Headers("Content-Type: application/x-www-form-urlencoded")
    public interface TokenServiceStub {

        @Headers("Authorization: Basic {auth_token}")
        @RequestLine("POST /")
        Response generateAccessToken(@Param("auth_token") String authToken,
                                     @Param("grant_type") String grantType,
                                     @Param("code") String code,
                                     @Param("redirect_uri") String redirectUri,
                                     @Param("refresh_token") String refreshToken,
                                     @Param("username") String username,
                                     @Param("password") String password,
                                     @Param("scope") String scopes);

        /**
         * Get a access token by Password grant type.
         *
         * @param username     Username of the user
         * @param password     Password of the user
         * @param scopes       Required scopes (space separated) for the access token
         * @param clientId     Consumer Key of the application
         * @param clientSecret Consumer Secret of the application
         * @return Feign Response Object
         */
        default Response generatePasswordGrantAccessToken(String username, String password, String scopes,
                                                          String clientId, String clientSecret) {
            String credentials = clientId + ':' + clientSecret;
            String authToken = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            return generateAccessToken(authToken, IdPClientConstants.PASSWORD_GRANT_TYPE, null, null, null,
                    username, password, scopes);
        }

        /**
         * Get a access token by Authorization Code grant type.
         *
         * @param code         Authorization Code
         * @param redirectUri  Callback URL
         * @param scopes       Required scopes (space separated) for the access token
         * @param clientId     Consumer Key of the application
         * @param clientSecret Consumer Secret of the application
         * @return Feign Response Object
         */
        default Response generateAuthCodeGrantAccessToken(String code, String redirectUri, String scopes,
                                                          String clientId, String clientSecret) {
            String credentials = clientId + ':' + clientSecret;
            String authToken = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            return generateAccessToken(authToken, IdPClientConstants.AUTHORIZATION_CODE_GRANT_TYPE, code, redirectUri,
                    null, null, null, scopes);
        }

        /**
         * Get a access token by Refresh grant type.
         *
         * @param refreshToken Refresh Token
         * @param scopes       Required scopes (space separated) for the access token
         * @param clientId     Consumer Key of the application
         * @param clientSecret Consumer Secret of the application
         * @return Feign Response Object
         */
        default Response generateRefreshGrantAccessToken(String refreshToken, String scopes,
                                                         String clientId, String clientSecret) {
            String credentials = clientId + ':' + clientSecret;
            String authToken = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            return generateAccessToken(authToken, IdPClientConstants.REFRESH_GRANT_TYPE, null,
                    null, refreshToken, null, null, scopes);
        }
    }

    /**
     * This interface is for /revoke API stub.
     */
    @Headers("Content-Type: application/x-www-form-urlencoded")
    public interface RevokeServiceStub {

        @RequestLine("POST /")
        @Headers("Authorization: Basic {auth_token}")
        Response revokeToken(@Param("auth_token") String authToken, @Param("token") String token);

        /**
         * Revoke access token.
         *
         * @param token        Access token that is required to be revoked
         * @param clientId     Consumer Key of the application
         * @param clientSecret Consumer Secret of the application
         * @return Feign Response Object
         */
        default Response revokeAccessToken(String token, String clientId, String clientSecret) {
            String credentials = clientId + ':' + clientSecret;
            String authToken = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            return revokeToken(authToken, token);
        }
    }

    /**
     * This interface is for /introspect API stub.
     */
    @Headers("Content-Type: application/x-www-form-urlencoded")
    public interface IntrospectionServiceStub {
        @RequestLine("POST /")
        Response introspectToken(@Param("token") String token);
    }

    private static class FormEncoder implements Encoder {
        @Override
        public void encode(Object o, Type type, RequestTemplate requestTemplate) throws EncodeException {
            Map<String, Object> params = (Map<String, Object>) o;
            String paramString = params.entrySet().stream()
                    .map(this::urlEncodeKeyValuePair)
                    .collect(Collectors.joining("&"));
            requestTemplate.body(paramString);
        }

        private String urlEncodeKeyValuePair(Map.Entry<String, Object> entry) {
            try {
                return URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8.toString()) + '='
                        + URLEncoder.encode(String.valueOf(entry.getValue()), StandardCharsets.UTF_8.toString());
            } catch (UnsupportedEncodingException ex) {
                throw new EncodeException("Error occurred while URL encoding message", ex);
            }
        }
    }
}
