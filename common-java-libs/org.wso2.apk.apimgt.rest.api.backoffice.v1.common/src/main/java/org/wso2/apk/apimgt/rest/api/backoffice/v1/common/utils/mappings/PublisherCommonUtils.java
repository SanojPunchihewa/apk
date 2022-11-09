/*
 * Copyright (c) 2022, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.apk.apimgt.rest.api.backoffice.v1.common.utils.mappings;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.language.FieldDefinition;
import graphql.language.ObjectTypeDefinition;
import graphql.language.TypeDefinition;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.wso2.apk.apimgt.api.*;
import org.wso2.apk.apimgt.api.doc.model.APIResource;
import org.wso2.apk.apimgt.api.model.*;
import org.wso2.apk.apimgt.impl.APIConstants;
import org.wso2.apk.apimgt.impl.definitions.AsyncApiParser;
import org.wso2.apk.apimgt.impl.definitions.OASParserUtil;
import org.wso2.apk.apimgt.impl.importexport.APIImportExportException;
import org.wso2.apk.apimgt.impl.importexport.ExportFormat;
import org.wso2.apk.apimgt.impl.importexport.ImportExportConstants;
import org.wso2.apk.apimgt.impl.importexport.utils.CommonUtil;
import org.wso2.apk.apimgt.impl.utils.APIUtil;
import org.wso2.apk.apimgt.impl.utils.APIVersionStringComparator;
import org.wso2.apk.apimgt.rest.api.backoffice.v1.common.utils.crypto.CryptoTool;
import org.wso2.apk.apimgt.rest.api.backoffice.v1.common.utils.crypto.CryptoToolException;
import org.wso2.apk.apimgt.rest.api.backoffice.v1.dto.*;
import org.wso2.apk.apimgt.rest.api.common.RestApiCommonUtil;
import org.wso2.apk.apimgt.rest.api.common.RestApiConstants;
import org.wso2.apk.apimgt.rest.api.common.annotations.Scope;

import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * This is a publisher rest api utility class.
 */
public class PublisherCommonUtils {

    private static final Log log = LogFactory.getLog(PublisherCommonUtils.class);

    /**
     * Update an API.
     *
     * @param originalAPI    Existing API
     * @param apiDtoToUpdate New API DTO to update
     * @param apiProvider    API Provider
     * @param tokenScopes    Scopes of the token
     * @throws ParseException         If an error occurs while parsing the endpoint configuration
     * @throws APIManagementException If an error occurs while updating the API
     * @throws FaultGatewaysException If an error occurs while updating manage of an existing API
     */
    public static API updateApi(API originalAPI, APIDTO apiDtoToUpdate, APIProvider apiProvider, String[] tokenScopes)
            throws APIManagementException, FaultGatewaysException {

        APIIdentifier apiIdentifier = originalAPI.getId();
        // Validate if the USER_REST_API_SCOPES is not set in WebAppAuthenticator when scopes are validated
        if (tokenScopes == null) {
            throw new APIManagementException("Error occurred while updating the  API " + originalAPI.getUUID()
                    + " as the token information hasn't been correctly set internally",
                    ExceptionCodes.TOKEN_SCOPES_NOT_SET);
        }
        boolean isGraphql = originalAPI.getType() != null && APIConstants.APITransportType.GRAPHQL.toString()
                .equals(originalAPI.getType());
        boolean isAsyncAPI = originalAPI.getType() != null
                && (APIConstants.APITransportType.WS.toString().equals(originalAPI.getType())
                || APIConstants.APITransportType.WEBSUB.toString().equals(originalAPI.getType())
                || APIConstants.APITransportType.SSE.toString().equals(originalAPI.getType())
                || APIConstants.APITransportType.ASYNC.toString().equals(originalAPI.getType()));

        Scope[] apiDtoClassAnnotatedScopes = APIDTO.class.getAnnotationsByType(Scope.class);
        boolean hasClassLevelScope = checkClassScopeAnnotation(apiDtoClassAnnotatedScopes, tokenScopes);

        JSONParser parser = new JSONParser();
        String oldEndpointConfigString = originalAPI.getEndpointConfig();
        JSONObject oldEndpointConfig = null;
        if (StringUtils.isNotBlank(oldEndpointConfigString)) {
            try {
                oldEndpointConfig = (JSONObject) parser.parse(oldEndpointConfigString);
            } catch (ParseException e) {
                throw new APIManagementException("Error while parsing endpoint config",
                        ExceptionCodes.JSON_PARSE_ERROR);
            }
        }
        String oldProductionApiSecret = null;
        String oldSandboxApiSecret = null;

        if (oldEndpointConfig != null) {
            if ((oldEndpointConfig.containsKey(APIConstants.ENDPOINT_SECURITY))) {
                JSONObject oldEndpointSecurity = (JSONObject) oldEndpointConfig.get(APIConstants.ENDPOINT_SECURITY);
                if (oldEndpointSecurity.containsKey(APIConstants.OAuthConstants.ENDPOINT_SECURITY_PRODUCTION)) {
                    JSONObject oldEndpointSecurityProduction = (JSONObject) oldEndpointSecurity
                            .get(APIConstants.OAuthConstants.ENDPOINT_SECURITY_PRODUCTION);

                    if (oldEndpointSecurityProduction.get(APIConstants.OAuthConstants.OAUTH_CLIENT_ID) != null
                            && oldEndpointSecurityProduction.get(APIConstants.OAuthConstants.OAUTH_CLIENT_SECRET)
                            != null) {
                        oldProductionApiSecret = oldEndpointSecurityProduction
                                .get(APIConstants.OAuthConstants.OAUTH_CLIENT_SECRET).toString();
                    }
                }
                if (oldEndpointSecurity.containsKey(APIConstants.OAuthConstants.ENDPOINT_SECURITY_SANDBOX)) {
                    JSONObject oldEndpointSecuritySandbox = (JSONObject) oldEndpointSecurity
                            .get(APIConstants.OAuthConstants.ENDPOINT_SECURITY_SANDBOX);

                    if (oldEndpointSecuritySandbox.get(APIConstants.OAuthConstants.OAUTH_CLIENT_ID) != null
                            && oldEndpointSecuritySandbox.get(APIConstants.OAuthConstants.OAUTH_CLIENT_SECRET)
                            != null) {
                        oldSandboxApiSecret = oldEndpointSecuritySandbox
                                .get(APIConstants.OAuthConstants.OAUTH_CLIENT_SECRET).toString();
                    }
                }
            }
        }

//        Map endpointConfig = (Map) apiDtoToUpdate.getEndpointConfig();
//        CryptoTool cryptoTool = CryptoToolUtil.getDefaultCryptoTool();
//
//        // OAuth 2.0 backend protection: API Key and API Secret encryption
//        encryptEndpointSecurityOAuthCredentials(endpointConfig, cryptoTool, oldProductionApiSecret, oldSandboxApiSecret,
//                apiDtoToUpdate);
//
//        // AWS Lambda: secret key encryption while updating the API
//        if (apiDtoToUpdate.getEndpointConfig() != null) {
//            if (endpointConfig.containsKey(APIConstants.AMZN_SECRET_KEY)) {
//                String secretKey = (String) endpointConfig.get(APIConstants.AMZN_SECRET_KEY);
//                if (!StringUtils.isEmpty(secretKey)) {
//                    if (!APIConstants.AWS_SECRET_KEY.equals(secretKey)) {
//                        try {
//                            String encryptedSecretKey = cryptoTool.encryptAndBase64Encode(secretKey.getBytes());
//                            endpointConfig.put(APIConstants.AMZN_SECRET_KEY, encryptedSecretKey);
//                            apiDtoToUpdate.setEndpointConfig(endpointConfig);
//                        } catch (CryptoToolException e) {
//                            throw new APIManagementException(ExceptionCodes.from(ExceptionCodes.ENDPOINT_CRYPTO_ERROR,
//                                    "Error while encrypting AWS secret key"));
//                        }
//
//                    } else {
//                        try {
//                            JSONParser jsonParser = new JSONParser();
//                            JSONObject originalEndpointConfig = (JSONObject) jsonParser
//                                    .parse(originalAPI.getEndpointConfig());
//                            String encryptedSecretKey = (String) originalEndpointConfig
//                                    .get(APIConstants.AMZN_SECRET_KEY);
//                            endpointConfig.put(APIConstants.AMZN_SECRET_KEY, encryptedSecretKey);
//                            apiDtoToUpdate.setEndpointConfig(endpointConfig);
//                        } catch (ParseException e) {
//                            throw new APIManagementException("Error while parsing endpoint config",
//                                    ExceptionCodes.JSON_PARSE_ERROR);
//                        }
//                    }
//                }
//            }
//        }

        if (!hasClassLevelScope) {
            // Validate per-field scopes
            apiDtoToUpdate = getFieldOverriddenAPIDTO(apiDtoToUpdate, originalAPI, tokenScopes);
        }
        //Overriding some properties:
        //API Name change not allowed if OnPrem
        if (APIUtil.isOnPremResolver()) {
            apiDtoToUpdate.setName(apiIdentifier.getApiName());
        }
        apiDtoToUpdate.setVersion(apiIdentifier.getVersion());
        //apiDtoToUpdate.setProvider(apiIdentifier.getProviderName());
        apiDtoToUpdate.setContext(originalAPI.getContextTemplate());
        //apiDtoToUpdate.setLifeCycleStatus(originalAPI.getStatus());
        apiDtoToUpdate.setType(APIDTO.TypeEnum.fromValue(originalAPI.getType()));

        List<APIResource> removedProductResources = getRemovedProductResources(apiDtoToUpdate, originalAPI);

        if (!removedProductResources.isEmpty()) {
            throw new APIManagementException(
                    "Cannot remove following resource paths " + removedProductResources.toString()
                            + " because they are used by one or more API Products", ExceptionCodes
                    .from(ExceptionCodes.API_PRODUCT_USED_RESOURCES, originalAPI.getId().getApiName(),
                            originalAPI.getId().getVersion()));
        }

//        // Validate API Security
//        List<String> apiSecurity = apiDtoToUpdate.getSecurityScheme();
//        //validation for tiers
//        List<String> tiersFromDTO = apiDtoToUpdate.getPolicies();
//        String originalStatus = originalAPI.getStatus();
//        if (apiSecurity.contains(APIConstants.DEFAULT_API_SECURITY_OAUTH2) || apiSecurity
//                .contains(APIConstants.API_SECURITY_API_KEY)) {
//            if ((tiersFromDTO == null || tiersFromDTO.isEmpty() && !(APIConstants.CREATED.equals(originalStatus)
//                    || APIConstants.PROTOTYPED.equals(originalStatus)))
//                    && !apiDtoToUpdate.getAdvertiseInfo().isAdvertised()) {
//                throw new APIManagementException(
//                        "A tier should be defined if the API is not in CREATED or PROTOTYPED state",
//                        ExceptionCodes.TIER_CANNOT_BE_NULL);
//            }
//        }

//        if (tiersFromDTO != null && !tiersFromDTO.isEmpty()) {
//            //check whether the added API's tiers are all valid
//            Set<Tier> definedTiers = apiProvider.getTiers();
//            List<String> invalidTiers = getInvalidTierNames(definedTiers, tiersFromDTO);
//            if (invalidTiers.size() > 0) {
//                throw new APIManagementException(
//                        "Specified tier(s) " + Arrays.toString(invalidTiers.toArray()) + " are invalid",
//                        ExceptionCodes.TIER_NAME_INVALID);
//            }
//        }
//        if (apiDtoToUpdate.getAccessControlRoles() != null) {
//            String errorMessage = validateUserRoles(apiDtoToUpdate.getAccessControlRoles());
//            if (!errorMessage.isEmpty()) {
//                throw new APIManagementException(errorMessage, ExceptionCodes.INVALID_USER_ROLES);
//            }
//        }
//        if (apiDtoToUpdate.getVisibleRoles() != null) {
//            String errorMessage = validateRoles(apiDtoToUpdate.getVisibleRoles());
//            if (!errorMessage.isEmpty()) {
//                throw new APIManagementException(errorMessage, ExceptionCodes.INVALID_USER_ROLES);
//            }
//        }
//        if (apiDtoToUpdate.getAdditionalProperties() != null) {
//            String errorMessage = validateAdditionalProperties(apiDtoToUpdate.getAdditionalProperties());
//            if (!errorMessage.isEmpty()) {
//                throw new APIManagementException(errorMessage, ExceptionCodes
//                        .from(ExceptionCodes.INVALID_ADDITIONAL_PROPERTIES, apiDtoToUpdate.getName(),
//                                apiDtoToUpdate.getVersion()));
//            }
//        }
        // Validate if resources are empty
        if (apiDtoToUpdate.getOperations() == null || apiDtoToUpdate.getOperations().isEmpty()) {
            throw new APIManagementException(ExceptionCodes.NO_RESOURCES_FOUND);
        }
        API apiToUpdate = APIMappingUtil.fromDTOtoAPI(apiDtoToUpdate, apiIdentifier.getProviderName());
        if (APIConstants.PUBLIC_STORE_VISIBILITY.equals(apiToUpdate.getVisibility())) {
            apiToUpdate.setVisibleRoles(StringUtils.EMPTY);
        }
        apiToUpdate.setUUID(originalAPI.getUUID());
        apiToUpdate.setOrganization(originalAPI.getOrganization());
        //validateScopes(apiToUpdate);
        apiToUpdate.setThumbnailUrl(originalAPI.getThumbnailUrl());

        //preserve monetization status in the update flow
        //apiProvider.configureMonetizationInAPIArtifact(originalAPI); ////////////TODO /////////REG call

        if (!isAsyncAPI) {
            String oldDefinition = apiProvider
                    .getOpenAPIDefinition(apiToUpdate.getUuid(), originalAPI.getOrganization());
            APIDefinition apiDefinition = OASParserUtil.getOASParser(oldDefinition);
            SwaggerData swaggerData = new SwaggerData(apiToUpdate);
            String newDefinition = apiDefinition.generateAPIDefinition(swaggerData, oldDefinition);
            apiProvider.saveSwaggerDefinition(apiToUpdate, newDefinition, originalAPI.getOrganization());
            if (!isGraphql) {
                Set<URITemplate> uriTemplates = apiDefinition.getURITemplates(newDefinition);

                //set operation policies from the original API Payload
                Set<URITemplate> uriTemplatesFromPayload = apiToUpdate.getUriTemplates();
                Map<String, List<OperationPolicy>> operationPoliciesPerURITemplate = new HashMap<>();
                for (URITemplate uriTemplate : uriTemplatesFromPayload) {
                    if (!uriTemplate.getOperationPolicies().isEmpty()) {
                        String key = uriTemplate.getHTTPVerb() + ":" + uriTemplate.getUriTemplate();
                        operationPoliciesPerURITemplate.put(key, uriTemplate.getOperationPolicies());
                    }
                }

                for (URITemplate uriTemplate : uriTemplates) {
                    String key = uriTemplate.getHTTPVerb() + ":" + uriTemplate.getUriTemplate();
                    if (operationPoliciesPerURITemplate.containsKey(key)) {
                        uriTemplate.setOperationPolicies(operationPoliciesPerURITemplate.get(key));
                    }
                }

                apiToUpdate.setUriTemplates(uriTemplates);
            }
        } else {
            String oldDefinition = apiProvider
                    .getAsyncAPIDefinition(apiToUpdate.getUuid(), originalAPI.getOrganization());
            AsyncApiParser asyncApiParser = new AsyncApiParser();
            String updateAsyncAPIDefinition = asyncApiParser.updateAsyncAPIDefinition(oldDefinition, apiToUpdate);
            apiProvider.saveAsyncApiDefinition(originalAPI, updateAsyncAPIDefinition);
        }
        //apiToUpdate.setWsdlUrl(apiDtoToUpdate.getWsdlUrl());
        //apiToUpdate.setGatewayType(apiDtoToUpdate.getGatewayType());

        //validate API categories
        List<APICategory> apiCategories = apiToUpdate.getApiCategories();
        List<APICategory> apiCategoriesList = new ArrayList<>();
        for (APICategory category : apiCategories) {
            category.setOrganization(originalAPI.getOrganization());
            apiCategoriesList.add(category);
        }
        apiToUpdate.setApiCategories(apiCategoriesList);
        if (apiCategoriesList.size() > 0) {
            if (!APIUtil.validateAPICategories(apiCategoriesList, originalAPI.getOrganization())) {
                throw new APIManagementException("Invalid API Category name(s) defined",
                        ExceptionCodes.from(ExceptionCodes.API_CATEGORY_INVALID));
            }
        }

        apiToUpdate.setOrganization(originalAPI.getOrganization());
        apiProvider.updateAPI(apiToUpdate, originalAPI);

        return apiProvider.getAPIbyUUID(originalAPI.getUuid(), originalAPI.getOrganization());
        // TODO use returend api
    }

    /**
     * This method will encrypt the OAuth 2.0 API Key and API Secret
     *
     * @param endpointConfig         endpoint configuration of API
     * @param cryptoTool             cryptography util
     * @param oldProductionApiSecret existing production API secret
     * @param oldSandboxApiSecret    existing sandbox API secret
     * @param apidto                 API DTO
     * @throws APIManagementException if an error occurs due to a problem in the endpointConfig payload
     */
    public static void encryptEndpointSecurityOAuthCredentials(Map endpointConfig, CryptoTool cryptoTool,
            String oldProductionApiSecret, String oldSandboxApiSecret, APIDTO apidto)
            throws APIManagementException {
        // OAuth 2.0 backend protection: API Key and API Secret encryption
        String customParametersString;
        if (endpointConfig != null) {
            if ((endpointConfig.get(APIConstants.ENDPOINT_SECURITY) != null)) {
                Map endpointSecurity = (Map) endpointConfig.get(APIConstants.ENDPOINT_SECURITY);
                if (endpointSecurity.get(APIConstants.OAuthConstants.ENDPOINT_SECURITY_PRODUCTION) != null) {
                    Map endpointSecurityProduction = (Map) endpointSecurity
                            .get(APIConstants.OAuthConstants.ENDPOINT_SECURITY_PRODUCTION);
                    String productionEndpointType = (String) endpointSecurityProduction
                            .get(APIConstants.OAuthConstants.ENDPOINT_SECURITY_TYPE);

                    // Change default value of customParameters JSONObject to String
                    if (!(endpointSecurityProduction
                            .get(APIConstants.OAuthConstants.OAUTH_CUSTOM_PARAMETERS) instanceof String)) {
                        LinkedHashMap<String, String> customParametersHashMap = (LinkedHashMap<String, String>)
                                endpointSecurityProduction.get(APIConstants.OAuthConstants.OAUTH_CUSTOM_PARAMETERS);
                        customParametersString = JSONObject.toJSONString(customParametersHashMap);
                    } else if (endpointSecurityProduction.get(APIConstants.OAuthConstants.OAUTH_CUSTOM_PARAMETERS)
                            != null) {
                        customParametersString = (String) endpointSecurityProduction
                                .get(APIConstants.OAuthConstants.OAUTH_CUSTOM_PARAMETERS);
                    } else {
                        customParametersString = "{}";
                    }

                    endpointSecurityProduction
                            .put(APIConstants.OAuthConstants.OAUTH_CUSTOM_PARAMETERS, customParametersString);

                    if (APIConstants.OAuthConstants.OAUTH.equals(productionEndpointType)) {
                        if (endpointSecurityProduction.get(APIConstants.OAuthConstants.OAUTH_CLIENT_SECRET) != null
                                && StringUtils.isNotBlank(
                                endpointSecurityProduction.get(APIConstants.OAuthConstants.OAUTH_CLIENT_SECRET)
                                        .toString())) {
                            String apiSecret = endpointSecurityProduction
                                    .get(APIConstants.OAuthConstants.OAUTH_CLIENT_SECRET).toString();
                            try {
                                String encryptedApiSecret = cryptoTool.encryptAndBase64Encode(apiSecret.getBytes());
                                endpointSecurityProduction
                                        .put(APIConstants.OAuthConstants.OAUTH_CLIENT_SECRET, encryptedApiSecret);
                            } catch (CryptoToolException e) {
                                throw new APIManagementException(ExceptionCodes
                                        .from(ExceptionCodes.ENDPOINT_CRYPTO_ERROR,
                                                "Error while encoding OAuth client secret"));
                            }
                        } else if (StringUtils.isNotBlank(oldProductionApiSecret)) {
                            endpointSecurityProduction
                                    .put(APIConstants.OAuthConstants.OAUTH_CLIENT_SECRET, oldProductionApiSecret);
                        } else {
                            String errorMessage = "Client secret is not provided for production endpoint security";
                            throw new APIManagementException(
                                    ExceptionCodes.from(ExceptionCodes.INVALID_ENDPOINT_CREDENTIALS, errorMessage));
                        }
                    }
                    endpointSecurity
                            .put(APIConstants.OAuthConstants.ENDPOINT_SECURITY_PRODUCTION, endpointSecurityProduction);
                    endpointConfig.put(APIConstants.ENDPOINT_SECURITY, endpointSecurity);
                    //apidto.setEndpointConfig(endpointConfig);
                }
                if (endpointSecurity.get(APIConstants.OAuthConstants.ENDPOINT_SECURITY_SANDBOX) != null) {
                    Map endpointSecuritySandbox = (Map) endpointSecurity
                            .get(APIConstants.OAuthConstants.ENDPOINT_SECURITY_SANDBOX);
                    String sandboxEndpointType = (String) endpointSecuritySandbox
                            .get(APIConstants.OAuthConstants.ENDPOINT_SECURITY_TYPE);

                    // Change default value of customParameters JSONObject to String
                    if (!(endpointSecuritySandbox
                            .get(APIConstants.OAuthConstants.OAUTH_CUSTOM_PARAMETERS) instanceof String)) {
                        Map<String, String> customParametersHashMap = (Map<String, String>) endpointSecuritySandbox
                                .get(APIConstants.OAuthConstants.OAUTH_CUSTOM_PARAMETERS);
                        customParametersString = JSONObject.toJSONString(customParametersHashMap);
                    } else if (endpointSecuritySandbox.get(APIConstants.OAuthConstants.OAUTH_CUSTOM_PARAMETERS)
                            != null) {
                        customParametersString = (String) endpointSecuritySandbox
                                .get(APIConstants.OAuthConstants.OAUTH_CUSTOM_PARAMETERS);
                    } else {
                        customParametersString = "{}";
                    }
                    endpointSecuritySandbox
                            .put(APIConstants.OAuthConstants.OAUTH_CUSTOM_PARAMETERS, customParametersString);

                    if (APIConstants.OAuthConstants.OAUTH.equals(sandboxEndpointType)) {
                        if (endpointSecuritySandbox.get(APIConstants.OAuthConstants.OAUTH_CLIENT_SECRET) != null
                                && StringUtils.isNotBlank(
                                endpointSecuritySandbox.get(APIConstants.OAuthConstants.OAUTH_CLIENT_SECRET)
                                        .toString())) {
                            String apiSecret = endpointSecuritySandbox
                                    .get(APIConstants.OAuthConstants.OAUTH_CLIENT_SECRET).toString();
                            try {
                                String encryptedApiSecret = cryptoTool.encryptAndBase64Encode(apiSecret.getBytes());
                                endpointSecuritySandbox
                                        .put(APIConstants.OAuthConstants.OAUTH_CLIENT_SECRET, encryptedApiSecret);
                            } catch (CryptoToolException e) {
                                throw new APIManagementException(ExceptionCodes
                                        .from(ExceptionCodes.ENDPOINT_CRYPTO_ERROR,
                                                "Error while encoding OAuth client secret"));
                            }
                        } else if (StringUtils.isNotBlank(oldSandboxApiSecret)) {
                            endpointSecuritySandbox
                                    .put(APIConstants.OAuthConstants.OAUTH_CLIENT_SECRET, oldSandboxApiSecret);
                        } else {
                            String errorMessage = "Client secret is not provided for sandbox endpoint security";
                            throw new APIManagementException(
                                    ExceptionCodes.from(ExceptionCodes.INVALID_ENDPOINT_CREDENTIALS, errorMessage));
                        }
                    }
                    endpointSecurity
                            .put(APIConstants.OAuthConstants.ENDPOINT_SECURITY_SANDBOX, endpointSecuritySandbox);
                    endpointConfig.put(APIConstants.ENDPOINT_SECURITY, endpointSecurity);
                    //apidto.setEndpointConfig(endpointConfig);
                }
            }
        }
    }

    /**
     * Check whether the token has APIDTO class level Scope annotation.
     *
     * @return true if the token has APIDTO class level Scope annotation
     */
    private static boolean checkClassScopeAnnotation(Scope[] apiDtoClassAnnotatedScopes, String[] tokenScopes) {

        for (Scope classAnnotation : apiDtoClassAnnotatedScopes) {
            for (String tokenScope : tokenScopes) {
                if (classAnnotation.name().equals(tokenScope)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Override the API DTO field values with the user passed new values considering the field-wise scopes defined as
     * allowed to update in REST API definition yaml.
     */
    private static JSONObject overrideDTOValues(JSONObject originalApiDtoJson, JSONObject newApiDtoJson, Field field,
                                                String[] tokenScopes, Scope[] fieldAnnotatedScopes)
            throws APIManagementException {

        for (String tokenScope : tokenScopes) {
            for (Scope scopeAnt : fieldAnnotatedScopes) {
                if (scopeAnt.name().equals(tokenScope)) {
                    // do the overriding
                    originalApiDtoJson.put(field.getName(), newApiDtoJson.get(field.getName()));
                    return originalApiDtoJson;
                }
            }
        }
        throw new APIManagementException("User is not authorized to update one or more API fields. None of the "
                + "required scopes found in user token to update the field. So the request will be failed.",
                ExceptionCodes.INVALID_SCOPE);
    }

    /**
     * Get the API DTO object in which the API field values are overridden with the user passed new values.
     *
     * @throws APIManagementException
     */
    private static APIDTO getFieldOverriddenAPIDTO(APIDTO apidto, API originalAPI, String[] tokenScopes)
            throws APIManagementException {

        APIDTO originalApiDTO;
        APIDTO updatedAPIDTO;

        try {
            originalApiDTO = APIMappingUtil.fromAPItoDTO(originalAPI);

            Field[] fields = APIDTO.class.getDeclaredFields();
            ObjectMapper mapper = new ObjectMapper();
            String newApiDtoJsonString = mapper.writeValueAsString(apidto);
            JSONParser parser = new JSONParser();
            JSONObject newApiDtoJson = (JSONObject) parser.parse(newApiDtoJsonString);

            String originalApiDtoJsonString = mapper.writeValueAsString(originalApiDTO);
            JSONObject originalApiDtoJson = (JSONObject) parser.parse(originalApiDtoJsonString);

            for (Field field : fields) {
                Scope[] fieldAnnotatedScopes = field.getAnnotationsByType(Scope.class);
                String originalElementValue = mapper.writeValueAsString(originalApiDtoJson.get(field.getName()));
                String newElementValue = mapper.writeValueAsString(newApiDtoJson.get(field.getName()));

                if (!StringUtils.equals(originalElementValue, newElementValue)) {
                    originalApiDtoJson = overrideDTOValues(originalApiDtoJson, newApiDtoJson, field, tokenScopes,
                            fieldAnnotatedScopes);
                }
            }

            updatedAPIDTO = mapper.readValue(originalApiDtoJson.toJSONString(), APIDTO.class);

        } catch (IOException | ParseException e) {
            String msg = "Error while processing API DTO json strings";
            throw new APIManagementException(msg, e, ExceptionCodes.JSON_PARSE_ERROR);
        }
        return updatedAPIDTO;
    }

    /**
     * Finds resources that have been removed in the updated API, that are currently reused by API Products.
     *
     * @param updatedDTO  Updated API
     * @param existingAPI Existing API
     * @return List of removed resources that are reused among API Products
     */
    private static List<APIResource> getRemovedProductResources(APIDTO updatedDTO, API existingAPI) {

        List<APIOperationsDTO> updatedOperations = updatedDTO.getOperations();
        Set<URITemplate> existingUriTemplates = existingAPI.getUriTemplates();
        List<APIResource> removedReusedResources = new ArrayList<>();

        for (URITemplate existingUriTemplate : existingUriTemplates) {

            // If existing URITemplate is used by any API Products
            if (!existingUriTemplate.retrieveUsedByProducts().isEmpty()) {
                String existingVerb = existingUriTemplate.getHTTPVerb();
                String existingPath = existingUriTemplate.getUriTemplate();
                boolean isReusedResourceRemoved = true;

                for (APIOperationsDTO updatedOperation : updatedOperations) {
                    String updatedVerb = updatedOperation.getVerb();
                    String updatedPath = updatedOperation.getTarget();

                    //Check if existing reused resource is among updated resources
                    if (existingVerb.equalsIgnoreCase(updatedVerb) && existingPath.equalsIgnoreCase(updatedPath)) {
                        isReusedResourceRemoved = false;
                        break;
                    }
                }

                // Existing reused resource is not among updated resources
                if (isReusedResourceRemoved) {
                    APIResource removedResource = new APIResource(existingVerb, existingPath);
                    removedReusedResources.add(removedResource);
                }
            }
        }

        return removedReusedResources;
    }

    /**
     * To validate the roles against user roles and tenant roles.
     *
     * @param inputRoles Input roles.
     * @return relevant error string or empty string.
     * @throws APIManagementException API Management Exception.
     */
    public static String validateUserRoles(List<String> inputRoles) throws APIManagementException {

        String userName = RestApiCommonUtil.getLoggedInUsername();
        boolean isMatched = false;
        String[] userRoleList = null;

        if (APIUtil.hasPermission(userName, APIConstants.Permissions.APIM_ADMIN)) {
            isMatched = true;
        } else {
            userRoleList = APIUtil.getListOfRoles(userName);
        }
        if (inputRoles != null && !inputRoles.isEmpty()) {
            if (!isMatched && userRoleList != null) {
                for (String inputRole : inputRoles) {
                    if (APIUtil.compareRoleList(userRoleList, inputRole)) {
                        isMatched = true;
                        break;
                    }
                }
                return isMatched ? "" : "This user does not have at least one role specified in API access control.";
            }

            String roleString = String.join(",", inputRoles);
            if (!APIUtil.isRoleNameExist(userName, roleString)) {
                return "Invalid user roles found in accessControlRole list";
            }
        }
        return "";
    }

    /**
     * To validate the roles against and tenant roles.
     *
     * @param inputRoles Input roles.
     * @return relevant error string or empty string.
     */
    public static String validateRoles(List<String> inputRoles) {

        String userName = RestApiCommonUtil.getLoggedInUsername();
        boolean isMatched = false;
        if (inputRoles != null && !inputRoles.isEmpty()) {
            String roleString = String.join(",", inputRoles);
            isMatched = APIUtil.isRoleNameExist(userName, roleString);
            if (!isMatched) {
                return "Invalid user roles found in visibleRoles list";
            }
        }
        return "";
    }

//    /**
//     * To validate the additional properties.
//     * Validation will be done for the keys of additional properties. Property keys should not contain spaces in it
//     * and property keys should not conflict with reserved key words.
//     *
//     * @param additionalProperties Map<String, String>  properties to validate
//     * @return error message if there is an validation error with additional properties.
//     */
//    public static String validateAdditionalProperties(List<APIInfoAdditionalPropertiesDTO> additionalProperties) {
//
//        if (additionalProperties != null) {
//            for (APIInfoAdditionalPropertiesDTO property : additionalProperties) {
//                String propertyKey = property.getName();
//                String propertyValue = property.getValue();
//                if (propertyKey.contains(" ")) {
//                    return "Property names should not contain space character. Property '" + propertyKey + "' "
//                            + "contains space in it.";
//                }
//                if (Arrays.asList(APIConstants.API_SEARCH_PREFIXES).contains(propertyKey.toLowerCase())) {
//                    return "Property '" + propertyKey + "' conflicts with the reserved keywords. Reserved keywords "
//                            + "are [" + Arrays.toString(APIConstants.API_SEARCH_PREFIXES) + "]";
//                }
//                // Maximum allowable characters of registry property name and value is 100 and 1000. Hence we are
//                // restricting them to be within 80 and 900.
//                if (propertyKey.length() > 80) {
//                    return "Property name can have maximum of 80 characters. Property '" + propertyKey + "' + contains "
//                            + propertyKey.length() + "characters";
//                }
//                if (propertyValue.length() > 900) {
//                    return "Property value can have maximum of 900 characters. Property '" + propertyKey + "' + "
//                            + "contains a value with " + propertyValue.length() + "characters";
//                }
//            }
//        }
//        return "";
//    }

//    /**
//     * validate user inout scopes.
//     *
//     * @param api api information
//     * @throws APIManagementException throw if validation failure
//     */
//    public static void validateScopes(API api) throws APIManagementException {
//
//        String username = RestApiCommonUtil.getLoggedInUsername();
//        int tenantId = APIUtil.getInternalOrganizationId(api.getOrganization());
//        String tenantDomain = APIUtil.getTenantDomainFromTenantId(tenantId);
//        APIProvider apiProvider = RestApiCommonUtil.getProvider(username);
//        Set<org.wso2.carbon.apimgt.api.model.Scope> sharedAPIScopes = new HashSet<>();
//
//        for (org.wso2.carbon.apimgt.api.model.Scope scope : api.getScopes()) {
//            String scopeName = scope.getKey();
//            if (!(APIUtil.isAllowedScope(scopeName))) {
//                // Check if each scope key is already assigned as a local scope to a different API which is also not a
//                // different version of the same API. If true, return error.
//                // If false, check if the scope key is already defined as a shared scope. If so, do not honor the
//                // other scope attributes (description, role bindings) in the request payload, replace them with
//                // already defined values for the existing shared scope.
//                if (apiProvider.isScopeKeyAssignedLocally(api.getId().getApiName(), scopeName, api.getOrganization())) {
//                    throw new APIManagementException(
//                            "Scope " + scopeName + " is already assigned locally by another API",
//                            ExceptionCodes.SCOPE_ALREADY_ASSIGNED);
//                } else if (apiProvider.isSharedScopeNameExists(scopeName, tenantId)) {
//                    sharedAPIScopes.add(scope);
//                    continue;
//                }
//            }
//
//            //set display name as empty if it is not provided
//            if (StringUtils.isBlank(scope.getName())) {
//                scope.setName(scopeName);
//            }
//
//            //set description as empty if it is not provided
//            if (StringUtils.isBlank(scope.getDescription())) {
//                scope.setDescription("");
//            }
//            if (scope.getRoles() != null) {
//                for (String aRole : scope.getRoles().split(",")) {
//                    boolean isValidRole = APIUtil.isRoleNameExist(username, aRole);
//                    if (!isValidRole) {
//                        throw new APIManagementException("Role '" + aRole + "' does not exist.",
//                                ExceptionCodes.ROLE_DOES_NOT_EXIST);
//                    }
//                }
//            }
//        }
//
//        apiProvider.validateSharedScopes(sharedAPIScopes, tenantDomain);
//    }

//    /**
//     * Add API with the generated swagger from the DTO.
//     *
//     * @param apiDto     API DTO of the API
//     * @param oasVersion Open API Definition version
//     * @param username   Username
//     * @param organization  Organization Identifier
//     * @return Created API object
//     * @throws APIManagementException Error while creating the API
//     */
//    public static API addAPIWithGeneratedSwaggerDefinition(APIDTO apiDto, String oasVersion, String username,
//                                                           String organization)
//            throws APIManagementException {
//        if (APIUtil.isOnPremResolver()) {
//            String name = apiDto.getName();
//            //replace all white spaces in the API Name
//            apiDto.setName(name.replaceAll("\\s+", ""));
//        }
//        if (APIDTO.TypeEnum.ASYNC.equals(apiDto.getType())) {
//            throw new APIManagementException("ASYNC API type does not support API creation from scratch",
//                    ExceptionCodes.API_CREATION_NOT_SUPPORTED_FOR_ASYNC_TYPE_APIS);
//        }
//        boolean isWSAPI = APIDTO.TypeEnum.WS.equals(apiDto.getType());
//        boolean isAsyncAPI =
//                isWSAPI || APIDTO.TypeEnum.WEBSUB.equals(apiDto.getType()) ||
//                        APIDTO.TypeEnum.SSE.equals(apiDto.getType()) || APIDTO.TypeEnum.ASYNC.equals(apiDto.getType());
//        username = StringUtils.isEmpty(username) ? RestApiCommonUtil.getLoggedInUsername() : username;
//        APIProvider apiProvider = RestApiCommonUtil.getProvider(username);
//
//        // validate web socket api endpoint configurations
//        if (isWSAPI && !PublisherCommonUtils.isValidWSAPI(apiDto)) {
//            throw new APIManagementException("Endpoint URLs should be valid web socket URLs",
//                    ExceptionCodes.INVALID_ENDPOINT_URL);
//        }
//
//        // validate sandbox and production endpoints
//        if (!PublisherCommonUtils.validateEndpoints(apiDto)) {
//            throw new APIManagementException("Invalid/Malformed endpoint URL(s) detected",
//                    ExceptionCodes.INVALID_ENDPOINT_URL);
//        }
//
//        Map endpointConfig = (Map) apiDto.getEndpointConfig();
//        CryptoTool cryptoTool = CryptoToolUtil.getDefaultCryptoTool();
//
//        // OAuth 2.0 backend protection: API Key and API Secret encryption
//        encryptEndpointSecurityOAuthCredentials(endpointConfig, cryptoTool, StringUtils.EMPTY, StringUtils.EMPTY,
//                apiDto);
//
//        // AWS Lambda: secret key encryption while creating the API
//        if (apiDto.getEndpointConfig() != null) {
//            if (endpointConfig.containsKey(APIConstants.AMZN_SECRET_KEY)) {
//                String secretKey = (String) endpointConfig.get(APIConstants.AMZN_SECRET_KEY);
//                if (!StringUtils.isEmpty(secretKey)) {
//                    try {
//                        String encryptedSecretKey = cryptoTool.encryptAndBase64Encode(secretKey.getBytes());
//                        endpointConfig.put(APIConstants.AMZN_SECRET_KEY, encryptedSecretKey);
//                        apiDto.setEndpointConfig(endpointConfig);
//                    } catch (CryptoToolException e) {
//                        throw new APIManagementException(ExceptionCodes.from(ExceptionCodes.ENDPOINT_CRYPTO_ERROR,
//                                "Error while encrypting AWS secret key"));
//                    }
//
//                }
//            }
//        }
//
//       /* if (isWSAPI) {
//            ArrayList<String> websocketTransports = new ArrayList<>();
//            websocketTransports.add(APIConstants.WS_PROTOCOL);
//            websocketTransports.add(APIConstants.WSS_PROTOCOL);
//            apiDto.setTransport(websocketTransports);
//        }*/
//        API apiToAdd = prepareToCreateAPIByDTO(apiDto, apiProvider, username, organization);
//        validateScopes(apiToAdd);
//        //validate API categories
//        List<APICategory> apiCategories = apiToAdd.getApiCategories();
//        List<APICategory> apiCategoriesList = new ArrayList<>();
//        for (APICategory category : apiCategories) {
//            category.setOrganization(organization);
//            apiCategoriesList.add(category);
//        }
//        apiToAdd.setApiCategories(apiCategoriesList);
//        if (apiCategoriesList.size() > 0) {
//            if (!APIUtil.validateAPICategories(apiCategoriesList, organization)) {
//                throw new APIManagementException("Invalid API Category name(s) defined",
//                        ExceptionCodes.from(ExceptionCodes.API_CATEGORY_INVALID));
//            }
//        }
//
//        if (!isAsyncAPI) {
//            APIDefinition oasParser;
//            if (RestApiConstants.OAS_VERSION_2.equalsIgnoreCase(oasVersion)) {
//                oasParser = new OAS2Parser();
//            } else {
//                oasParser = new OAS3Parser();
//            }
//            SwaggerData swaggerData = new SwaggerData(apiToAdd);
//            String apiDefinition = oasParser.generateAPIDefinition(swaggerData);
//            apiToAdd.setSwaggerDefinition(apiDefinition);
//        } else {
//            AsyncApiParser asyncApiParser = new AsyncApiParser();
//            String asyncApiDefinition = asyncApiParser.generateAsyncAPIDefinition(apiToAdd);
//            apiToAdd.setAsyncApiDefinition(asyncApiDefinition);
//        }
//
//        apiToAdd.setOrganization(organization);
//        if (isAsyncAPI) {
//            AsyncApiParser asyncApiParser = new AsyncApiParser();
//            String apiDefinition = asyncApiParser.generateAsyncAPIDefinition(apiToAdd);
//            apiToAdd.setAsyncApiDefinition(apiDefinition);
//        }
//
//        //adding the api
//        apiProvider.addAPI(apiToAdd);
//        return apiToAdd;
//    }

//    /**
//     * Validate endpoint configurations of {@link APIDTO} for web socket endpoints.
//     *
//     * @param api api model
//     * @return validity of the web socket api
//     */
//    public static boolean isValidWSAPI(APIDTO api) {
//
//        boolean isValid = false;
//
//        if (api.getEndpointConfig() != null) {
//            Map endpointConfig = (Map) api.getEndpointConfig();
//            String prodEndpointUrl = String
//                    .valueOf(((Map) endpointConfig.get("production_endpoints")).get("url"));
//            String sandboxEndpointUrl = String
//                    .valueOf(((Map) endpointConfig.get("sandbox_endpoints")).get("url"));
//            isValid = prodEndpointUrl.startsWith("ws://") || prodEndpointUrl.startsWith("wss://");
//
//            if (isValid) {
//                isValid = sandboxEndpointUrl.startsWith("ws://") || sandboxEndpointUrl.startsWith("wss://");
//            }
//        }
//
//        return isValid;
//    }

    /**
     * Validate sandbox and production endpoint URLs.
     *
     * @param apiDto API DTO of the API
     * @return validity of URLs found within the endpoint configurations of the DTO
     */
    public static boolean validateEndpoints(APIDTO apiDto) {

        ArrayList<String> endpoints = new ArrayList<>();
//        org.json.JSONObject endpointConfiguration = new org.json.JSONObject((Map) apiDto.getEndpointConfig());
//
//        if (!endpointConfiguration.isNull(APIConstants.API_ENDPOINT_CONFIG_PROTOCOL_TYPE) && StringUtils.equals(
//                endpointConfiguration.get(APIConstants.API_ENDPOINT_CONFIG_PROTOCOL_TYPE).toString(),
//                APIConstants.ENDPOINT_TYPE_DEFAULT)) {
//            // if the endpoint type is dynamic, then the validation should be skipped
//            return true;
//        }
//
//        // extract sandbox endpoint URL(s)
//        extractURLsFromEndpointConfig(endpointConfiguration, APIConstants.API_DATA_SANDBOX_ENDPOINTS, endpoints);
//
//        // extract production endpoint URL(s)
//        extractURLsFromEndpointConfig(endpointConfiguration, APIConstants.API_DATA_PRODUCTION_ENDPOINTS, endpoints);

        return APIUtil.validateEndpointURLs(endpoints);
    }

    /**
     * Extract sandbox or production endpoint URLs from endpoint config object.
     *
     * @param endpointConfigObj Endpoint config JSON object
     * @param endpointType      Indicating whether Sandbox or Production endpoints are to be extracted
     * @param endpoints         List of URLs. Extracted URL(s), if any, are added to this list.
     */
    private static void extractURLsFromEndpointConfig(org.json.JSONObject endpointConfigObj, String endpointType,
            ArrayList<String> endpoints) {
        if (!endpointConfigObj.isNull(endpointType)) {
            org.json.JSONObject endpointObj = endpointConfigObj.optJSONObject(endpointType);
            if (endpointObj != null) {
                endpoints.add(endpointConfigObj.getJSONObject(endpointType).getString(APIConstants.API_DATA_URL));
            } else {
                JSONArray endpointArray = endpointConfigObj.getJSONArray(endpointType);
                for (int i = 0; i < endpointArray.length(); i++) {
                    endpoints.add((String) endpointArray.getJSONObject(i).get(APIConstants.API_DATA_URL));
                }
            }
        }
    }

    public static String constructEndpointConfigForService(String serviceUrl, String protocol) {

        StringBuilder sb = new StringBuilder();
        String endpointType = APIDTO.TypeEnum.HTTP.value().toLowerCase();
        if (StringUtils.isNotEmpty(protocol) && (APIDTO.TypeEnum.SSE.equals(protocol.toUpperCase())
                || APIDTO.TypeEnum.WS.equals(protocol.toUpperCase()))) {
            endpointType = "ws";
        }
        if (StringUtils.isNotEmpty(serviceUrl)) {
            sb.append("{\"endpoint_type\": \"")
                    .append(endpointType)
                    .append("\",")
                    .append("\"production_endpoints\": {\"url\": \"")
                    .append(serviceUrl)
                    .append("\"}}");
        } // TODO Need to check on the endpoint security
        return sb.toString();
    }

    public static APIDTO.TypeEnum getAPIType(ServiceEntry.DefinitionType definitionType, String protocol)
            throws APIManagementException {
        if (ServiceEntry.DefinitionType.ASYNC_API.equals(definitionType)) {
            if (protocol.isEmpty()) {
                throw new APIManagementException("A protocol should be specified in the Async API definition",
                        ExceptionCodes.MISSING_PROTOCOL_IN_ASYNC_API_DEFINITION);
            } else if (!APIConstants.API_TYPE_WEBSUB.equals(protocol.toUpperCase()) &&
                    !APIConstants.API_TYPE_SSE.equals(protocol.toUpperCase()) &&
                    !APIConstants.API_TYPE_WS.equals(protocol.toUpperCase())) {
                throw new APIManagementException("Unsupported protocol specified in Async API Definition",
                        ExceptionCodes.UNSUPPORTED_PROTOCOL_SPECIFIED_IN_ASYNC_API_DEFINITION);
            }

        }
        switch (definitionType) {
            case WSDL1:
            case WSDL2:
                return APIDTO.TypeEnum.SOAP;
            case GRAPHQL_SDL:
                return APIDTO.TypeEnum.GRAPHQL;
            case ASYNC_API:
                return APIDTO.TypeEnum.fromValue(protocol.toUpperCase());
            default:
                return APIDTO.TypeEnum.HTTP;
        }
    }

//    /**
//     * Prepares the API Model object to be created using the DTO object.
//     *
//     * @param body        APIDTO of the API
//     * @param apiProvider API Provider
//     * @param username    Username
//     * @param organization  Organization Identifier
//     * @return API object to be created
//     * @throws APIManagementException Error while creating the API
//     */
//    public static API prepareToCreateAPIByDTO(APIDTO body, APIProvider apiProvider, String username,
//                                              String organization)
//            throws APIManagementException {
//
//        String context = body.getContext();
//        //Make sure context starts with "/". ex: /pizza
//        context = context.startsWith("/") ? context : ("/" + context);
//
//        if (body.getAccessControlRoles() != null) {
//            String errorMessage = PublisherCommonUtils.validateUserRoles(body.getAccessControlRoles());
//
//            if (!errorMessage.isEmpty()) {
//                throw new APIManagementException(errorMessage, ExceptionCodes.INVALID_USER_ROLES);
//            }
//        }
//        if (body.getAdditionalProperties() != null) {
//            String errorMessage = PublisherCommonUtils.validateAdditionalProperties(body.getAdditionalProperties());
//            if (!errorMessage.isEmpty()) {
//                throw new APIManagementException(errorMessage, ExceptionCodes
//                        .from(ExceptionCodes.INVALID_ADDITIONAL_PROPERTIES, body.getName(), body.getVersion()));
//            }
//        }
//        if (body.getContext() == null) {
//            throw new APIManagementException("Parameter: \"context\" cannot be null",
//                    ExceptionCodes.PARAMETER_NOT_PROVIDED);
//        } else if (body.getContext().endsWith("/")) {
//            throw new APIManagementException("Context cannot end with '/' character", ExceptionCodes.INVALID_CONTEXT);
//        }
//        if (apiProvider.isApiNameWithDifferentCaseExist(body.getName(), organization)) {
//            throw new APIManagementException(
//                    "Error occurred while adding API. API with name " + body.getName() + " already exists.",
//                    ExceptionCodes.from(ExceptionCodes.API_NAME_ALREADY_EXISTS, body.getName()));
//        }
//        if (body.getAuthorizationHeader() == null) {
//            body.setAuthorizationHeader(APIUtil.getOAuthConfigurationFromAPIMConfig(APIConstants.AUTHORIZATION_HEADER));
//        }
//        if (body.getAuthorizationHeader() == null) {
//            body.setAuthorizationHeader(APIConstants.AUTHORIZATION_HEADER_DEFAULT);
//        }
//
//        if (body.getVisibility() == APIDTO.VisibilityEnum.RESTRICTED && body.getVisibleRoles().isEmpty()) {
//            throw new APIManagementException(
//                    "Valid roles should be added under 'visibleRoles' to restrict " + "the visibility",
//                    ExceptionCodes.USER_ROLES_CANNOT_BE_NULL);
//        }
//        if (body.getVisibleRoles() != null) {
//            String errorMessage = PublisherCommonUtils.validateRoles(body.getVisibleRoles());
//            if (!errorMessage.isEmpty()) {
//                throw new APIManagementException(errorMessage, ExceptionCodes.INVALID_USER_ROLES);
//            }
//        }
//
//        //Get all existing versions of  api been adding
//        List<String> apiVersions = apiProvider.getApiVersionsMatchingApiNameAndOrganization(body.getName(),
//                username, organization);
//        if (!apiVersions.isEmpty()) {
//            //If any previous version exists
//            for (String version : apiVersions) {
//                if (version.equalsIgnoreCase(body.getVersion())) {
//                    //If version already exists
//                    if (apiProvider.isDuplicateContextTemplateMatchingOrganization(context, organization)) {
//                        throw new APIManagementException(
//                                "Error occurred while " + "adding the API. A duplicate API already exists for "
//                                        + context + " in the organization : " + organization,
//                                ExceptionCodes.API_ALREADY_EXISTS);
//                    } else {
//                        throw new APIManagementException(
//                                "Error occurred while adding API. API with name " + body.getName()
//                                        + " already exists with different context" + context  + " in the organization" +
//                                        " : " + organization,  ExceptionCodes.API_ALREADY_EXISTS);
//                    }
//                }
//            }
//        } else {
//            //If no any previous version exists
//            if (apiProvider.isDuplicateContextTemplateMatchingOrganization(context, organization)) {
//                throw new APIManagementException(
//                        "Error occurred while adding the API. A duplicate API context already exists for "
//                                + context + " in the organization" + " : " + organization, ExceptionCodes
//                        .from(ExceptionCodes.API_CONTEXT_ALREADY_EXISTS, context));
//            }
//        }
//
//        //Check if the user has admin permission before applying a different provider than the current user
//        String provider = body.getProvider();
//        if (!StringUtils.isBlank(provider) && !provider.equals(username)) {
//            if (!APIUtil.hasPermission(username, APIConstants.Permissions.APIM_ADMIN)) {
//                if (log.isDebugEnabled()) {
//                    log.debug("User " + username + " does not have admin permission ("
//                            + APIConstants.Permissions.APIM_ADMIN + ") hence provider (" + provider
//                            + ") overridden with current user (" + username + ")");
//                }
//                provider = username;
//            } else {
//                if (!APIUtil.isUserExist(provider)) {
//                    throw new APIManagementException("Specified provider " + provider + " not exist.",
//                            ExceptionCodes.PARAMETER_NOT_PROVIDED);
//                }
//            }
//        } else {
//            //Set username in case provider is null or empty
//            provider = username;
//        }
//
//        List<String> tiersFromDTO = body.getPolicies();
//
//        //check whether the added API's tiers are all valid
//        Set<Tier> definedTiers = apiProvider.getTiers();
//        List<String> invalidTiers = getInvalidTierNames(definedTiers, tiersFromDTO);
//        if (!invalidTiers.isEmpty()) {
//            throw new APIManagementException(
//                    "Specified tier(s) " + Arrays.toString(invalidTiers.toArray()) + " are invalid",
//                    ExceptionCodes.TIER_NAME_INVALID);
//        }
//        APIPolicy apiPolicy = apiProvider.getAPIPolicy(username, body.getApiThrottlingPolicy());
//        if (apiPolicy == null && body.getApiThrottlingPolicy() != null) {
//            throw new APIManagementException("Specified policy " + body.getApiThrottlingPolicy() + " is invalid",
//                    ExceptionCodes.UNSUPPORTED_THROTTLE_LIMIT_TYPE);
//        }
//
//        API apiToAdd = APIMappingUtil.fromDTOtoAPI(body, provider);
//        //Overriding some properties:
//        //only allow CREATED as the stating state for the new api if not status is PROTOTYPED
//        if (!APIConstants.PROTOTYPED.equals(apiToAdd.getStatus())) {
//            apiToAdd.setStatus(APIConstants.CREATED);
//        }
//
//        if (!apiToAdd.isAdvertiseOnly() || StringUtils.isBlank(apiToAdd.getApiOwner())) {
//            //we are setting the api owner as the logged in user until we support checking admin privileges and
//            //assigning the owner as a different user
//            apiToAdd.setApiOwner(provider);
//        }
//
//        // Set default gatewayVendor
//        if (body.getGatewayVendor() == null) {
//            apiToAdd.setGatewayVendor(APIConstants.WSO2_GATEWAY_ENVIRONMENT);
//        }
//        apiToAdd.setOrganization(organization);
//        //apiToAdd.setGatewayType(body.getGatewayType());
//        return apiToAdd;
//    }

    public static String updateAPIDefinition(String apiId, APIDefinitionValidationResponse response,
                ServiceEntry service, String organization) throws APIManagementException, FaultGatewaysException {

        if (ServiceEntry.DefinitionType.OAS2.equals(service.getDefinitionType()) ||
                ServiceEntry.DefinitionType.OAS3.equals(service.getDefinitionType())) {
            return updateSwagger(apiId, response, true, organization);
        } else if (ServiceEntry.DefinitionType.ASYNC_API.equals(service.getDefinitionType())) {
            return updateAsyncAPIDefinition(apiId, response, organization);
        }
        return null;
    }

    /**
     * update AsyncPI definition of the given api.
     *
     * @param apiId    API Id
     * @param response response of the AsyncAPI definition validation call
     * @param organization identifier of the organization
     * @return updated AsyncAPI definition
     * @throws APIManagementException when error occurred updating AsyncAPI definition
     * @throws FaultGatewaysException when error occurred publishing API to the gateway
     */
    public static String updateAsyncAPIDefinition(String apiId, APIDefinitionValidationResponse response,
            String organization) throws APIManagementException, FaultGatewaysException {

        APIProvider apiProvider = RestApiCommonUtil.getLoggedInUserProvider();
        //this will fall if user does not have access to the API or the API does not exist
        API oldapi = apiProvider.getAPIbyUUID(apiId, organization);
        API existingAPI = apiProvider.getAPIbyUUID(apiId, organization);
        existingAPI.setOrganization(organization);
        String apiDefinition = response.getJsonContent();

        AsyncApiParser asyncApiParser = new AsyncApiParser();
        // Set uri templates
        Set<URITemplate> uriTemplates = asyncApiParser.getURITemplates(apiDefinition, APIConstants.
                API_TYPE_WS.equals(existingAPI.getType()) || !APIConstants.WSO2_GATEWAY_ENVIRONMENT.equals
                (existingAPI.getGatewayVendor()));
        if (uriTemplates == null || uriTemplates.isEmpty()) {
            throw new APIManagementException(ExceptionCodes.NO_RESOURCES_FOUND);
        }
        existingAPI.setUriTemplates(uriTemplates);

        // Update ws uri mapping
        existingAPI.setWsUriMapping(asyncApiParser.buildWSUriMapping(apiDefinition));

        //updating APi with the new AsyncAPI definition
        existingAPI.setAsyncApiDefinition(apiDefinition);
        apiProvider.saveAsyncApiDefinition(existingAPI, apiDefinition);
        apiProvider.updateAPI(existingAPI, oldapi);
        //retrieves the updated AsyncAPI definition
        return apiProvider.getAsyncAPIDefinition(existingAPI.getId().getUUID(), organization);
    }

    /**
     * update swagger definition of the given api.
     *
     * @param apiId    API Id
     * @param response response of a swagger definition validation call
     * @param organization  Organization Identifier
     * @return updated swagger definition
     * @throws APIManagementException when error occurred updating swagger
     */
    public static String updateSwagger(String apiId, APIDefinitionValidationResponse response, boolean isServiceAPI,
                                       String organization)
            throws APIManagementException {

        APIProvider apiProvider = RestApiCommonUtil.getLoggedInUserProvider();
        //this will fail if user does not have access to the API or the API does not exist
        API existingAPI = apiProvider.getAPIbyUUID(apiId, organization);
        APIDefinition oasParser = response.getParser();
        String apiDefinition = response.getJsonContent();
        if (isServiceAPI) {
            apiDefinition = oasParser.copyVendorExtensions(existingAPI.getSwaggerDefinition(), apiDefinition);
        } else {
            apiDefinition = OASParserUtil.preProcess(apiDefinition);
        }
        if (APIConstants.API_TYPE_SOAPTOREST.equals(existingAPI.getType())) {
//            List<SOAPToRestSequence> sequenceList = SequenceGenerator.generateSequencesFromSwagger(apiDefinition);
//            existingAPI.setSoapToRestSequences(sequenceList);
        }
        Set<URITemplate> uriTemplates = null;
        uriTemplates = oasParser.getURITemplates(apiDefinition);

        if (uriTemplates == null || uriTemplates.isEmpty()) {
            throw new APIManagementException(ExceptionCodes.NO_RESOURCES_FOUND);
        }
//        Set<org.wso2.carbon.apimgt.api.model.Scope> scopes = oasParser.getScopes(apiDefinition);
//        //validating scope roles
//        for (org.wso2.carbon.apimgt.api.model.Scope scope : scopes) {
//            String roles = scope.getRoles();
//            if (roles != null) {
//                for (String aRole : roles.split(",")) {
//                    boolean isValidRole = APIUtil.isRoleNameExist(RestApiCommonUtil.getLoggedInUsername(), aRole);
//                    if (!isValidRole) {
//                        throw new APIManagementException("Role '" + aRole + "' Does not exist.",
//                                ExceptionCodes.ROLE_DOES_NOT_EXIST);
//                    }
//                }
//            }
//        }

        List<APIResource> removedProductResources = apiProvider.getRemovedProductResources(uriTemplates, existingAPI);

        if (!removedProductResources.isEmpty()) {
            throw new APIManagementException(
                    "Cannot remove following resource paths " + removedProductResources.toString()
                            + " because they are used by one or more API Products", ExceptionCodes
                    .from(ExceptionCodes.API_PRODUCT_USED_RESOURCES, existingAPI.getId().getApiName(),
                            existingAPI.getId().getVersion()));
        }

        //set existing operation policies to URI templates
        apiProvider.setOperationPoliciesToURITemplates(apiId, uriTemplates);

        existingAPI.setUriTemplates(uriTemplates);
        //existingAPI.setScopes(scopes);
        //PublisherCommonUtils.validateScopes(existingAPI);
        //Update API is called to update URITemplates and scopes of the API
        SwaggerData swaggerData = new SwaggerData(existingAPI);
        String updatedApiDefinition = oasParser.populateCustomManagementInfo(apiDefinition, swaggerData);
        apiProvider.saveSwaggerDefinition(existingAPI, updatedApiDefinition, organization);
        existingAPI.setSwaggerDefinition(updatedApiDefinition);
        API unModifiedAPI = apiProvider.getAPIbyUUID(apiId, organization);
        existingAPI.setStatus(unModifiedAPI.getStatus());
        try {
            apiProvider.updateAPI(existingAPI, unModifiedAPI);
        } catch (FaultGatewaysException e) {
            throw new APIManagementException("Error while updating the API: " + apiId, ExceptionCodes.INTERNAL_ERROR);
        }

        //retrieves the updated swagger definition
        String apiSwagger = apiProvider.getOpenAPIDefinition(apiId, organization); // TODO see why we need to get it
        // instead of passing same
        return oasParser.getOASDefinitionForPublisher(existingAPI, apiSwagger);
    }

//    /**
//     * Add GraphQL schema.
//     *
//     * @param originalAPI      API
//     * @param schemaDefinition GraphQL schema definition to add
//     * @param apiProvider      API Provider
//     * @return the arrayList of APIOperationsDTOextractGraphQLOperationList
//     */
//    public static API addGraphQLSchema(API originalAPI, String schemaDefinition, APIProvider apiProvider)
//            throws APIManagementException, FaultGatewaysException {
//        API oldApi = apiProvider.getAPIbyUUID(originalAPI.getUuid(), originalAPI.getOrganization());
//
//        List<APIOperationsDTO> operationListWithOldData = APIMappingUtil
//                .getOperationListWithOldData(originalAPI.getUriTemplates(),
//                        extractGraphQLOperationList(schemaDefinition));
//
//        Set<URITemplate> uriTemplates = APIMappingUtil.getURITemplates(originalAPI, operationListWithOldData);
//        originalAPI.setUriTemplates(uriTemplates);
//
//        apiProvider.saveGraphqlSchemaDefinition(originalAPI.getUuid(), schemaDefinition, originalAPI.getOrganization());
//        apiProvider.updateAPI(originalAPI, oldApi);
//
//        return originalAPI;
//    }

    /**
     * Extract GraphQL Operations from given schema.
     *
     * @param schema graphQL Schema
     * @return the arrayList of APIOperationsDTOextractGraphQLOperationList
     */
    public static List<APIOperationsDTO> extractGraphQLOperationList(String schema) {

        List<APIOperationsDTO> operationArray = new ArrayList<>();
        SchemaParser schemaParser = new SchemaParser();
        TypeDefinitionRegistry typeRegistry = schemaParser.parse(schema);
        Map<String, TypeDefinition> operationList = typeRegistry.types();
        for (Map.Entry<String, TypeDefinition> entry : operationList.entrySet()) {
            if (entry.getValue().getName().equals(APIConstants.GRAPHQL_QUERY) || entry.getValue().getName()
                    .equals(APIConstants.GRAPHQL_MUTATION) || entry.getValue().getName()
                    .equals(APIConstants.GRAPHQL_SUBSCRIPTION)) {
                for (FieldDefinition fieldDef : ((ObjectTypeDefinition) entry.getValue()).getFieldDefinitions()) {
                    APIOperationsDTO operation = new APIOperationsDTO();
                    operation.setVerb(entry.getKey());
                    operation.setTarget(fieldDef.getName());
                    operationArray.add(operation);
                }
            }
        }
        return operationArray;
    }

//    /**
//     * Validate GraphQL Schema.
//     *
//     * @param filename file name of the schema
//     * @param schema   GraphQL schema
//     */
//    public static GraphQLValidationResponseDTO validateGraphQLSchema(String filename, String schema)
//            throws APIManagementException {
//
//        String errorMessage;
//        GraphQLValidationResponseDTO validationResponse = new GraphQLValidationResponseDTO();
//        boolean isValid = false;
//        try {
//            if (filename.endsWith(".graphql") || filename.endsWith(".txt") || filename.endsWith(".sdl")) {
//                if (schema.isEmpty()) {
//                    throw new APIManagementException("GraphQL Schema cannot be empty or null to validate it",
//                            ExceptionCodes.GRAPHQL_SCHEMA_CANNOT_BE_NULL);
//                }
//                SchemaParser schemaParser = new SchemaParser();
//                TypeDefinitionRegistry typeRegistry = schemaParser.parse(schema);
//                GraphQLSchema graphQLSchema = UnExecutableSchemaGenerator.makeUnExecutableSchema(typeRegistry);
//                SchemaValidator schemaValidation = new SchemaValidator();
//                Set<SchemaValidationError> validationErrors = schemaValidation.validateSchema(graphQLSchema);
//
//                if (validationErrors.toArray().length > 0) {
//                    errorMessage = "InValid Schema";
//                    validationResponse.isValid(Boolean.FALSE);
//                    validationResponse.errorMessage(errorMessage);
//                } else {
//                    validationResponse.setIsValid(Boolean.TRUE);
//                    GraphQLValidationResponseGraphQLInfoDTO graphQLInfo = new GraphQLValidationResponseGraphQLInfoDTO();
//                    GraphQLSchemaDefinition graphql = new GraphQLSchemaDefinition();
//                    List<URITemplate> operationList = graphql.extractGraphQLOperationList(typeRegistry, null);
//                    List<APIOperationsDTO> operationArray = APIMappingUtil
//                            .fromURITemplateListToOprationList(operationList);
//                    graphQLInfo.setOperations(operationArray);
//                    GraphQLSchemaDTO schemaObj = new GraphQLSchemaDTO();
//                    schemaObj.setSchemaDefinition(schema);
//                    graphQLInfo.setGraphQLSchema(schemaObj);
//                    validationResponse.setGraphQLInfo(graphQLInfo);
//                }
//            } else {
//                throw new APIManagementException("Unsupported extension type of file: " + filename,
//                        ExceptionCodes.UNSUPPORTED_GRAPHQL_FILE_EXTENSION);
//            }
//            isValid = validationResponse.isIsValid();
//            errorMessage = validationResponse.getErrorMessage();
//        } catch (SchemaProblem e) {
//            errorMessage = e.getMessage();
//        }
//
//        if (!isValid) {
//            validationResponse.setIsValid(isValid);
//            validationResponse.setErrorMessage(errorMessage);
//        }
//        return validationResponse;
//    }

    /**
     * Update thumbnail of an API/API Product
     *
     * @param fileInputStream Input stream
     * @param fileContentType The content type of the image
     * @param apiProvider     API Provider
     * @param apiId           API/API Product UUID
     * @param tenantDomain    Tenant domain of the API
     * @throws APIManagementException If an error occurs while updating the thumbnail
     */
    public static void updateThumbnail(InputStream fileInputStream, String fileContentType, APIProvider apiProvider,
                                       String apiId, String tenantDomain) throws APIManagementException {
        ResourceFile apiImage = new ResourceFile(fileInputStream, fileContentType);
        apiProvider.setThumbnailToAPI(apiId, apiImage, tenantDomain);
    }

    /**
     * Add document DTO.
     *
     * @param documentDto Document DTO
     * @param apiId       API UUID
     * @return Added documentation
     * @param organization  Identifier of an Organization
     * @throws APIManagementException If an error occurs when retrieving API Identifier,
     *                                when checking whether the documentation exists and when adding the documentation
     */
    public static Documentation addDocumentationToAPI(DocumentDTO documentDto, String apiId, String organization)
            throws APIManagementException {

        APIProvider apiProvider = RestApiCommonUtil.getLoggedInUserProvider();
        Documentation documentation = DocumentationMappingUtil.fromDTOtoDocumentation(documentDto);
        String documentName = documentDto.getName();
        if (documentDto.getType() == null) {
            throw new APIManagementException("Documentation type cannot be empty",
                    ExceptionCodes.PARAMETER_NOT_PROVIDED);
        }
        if (documentDto.getType() == DocumentDTO.TypeEnum.OTHER && StringUtils
                .isBlank(documentDto.getOtherTypeName())) {
            //check otherTypeName for not null if doc type is OTHER
            throw new APIManagementException("otherTypeName cannot be empty if type is OTHER.",
                    ExceptionCodes.PARAMETER_NOT_PROVIDED);
        }
        String sourceUrl = documentDto.getSourceUrl();
        if (documentDto.getSourceType() == DocumentDTO.SourceTypeEnum.URL && (
                StringUtils.isBlank(sourceUrl) || !RestApiCommonUtil.isURL(sourceUrl))) {
            throw new APIManagementException("Invalid document sourceUrl Format",
                    ExceptionCodes.PARAMETER_NOT_PROVIDED);
        }

        if (apiProvider.isDocumentationExist(apiId, documentName, organization)) {
            throw new APIManagementException("Requested document '" + documentName + "' already exists",
                    ExceptionCodes.DOCUMENT_ALREADY_EXISTS);
        }
        documentation = apiProvider.addDocumentation(apiId, documentation, organization);

        return documentation;
    }

    /**
     * Add documentation content of inline and markdown documents.
     *
     * @param documentation Documentation
     * @param apiProvider   API Provider
     * @param apiId         API/API Product UUID
     * @param documentId    Document ID
     * @param organization  Identifier of the organization
     * @param inlineContent Inline content string
     * @throws APIManagementException If an error occurs while adding the documentation content
     */
    public static void addDocumentationContent(Documentation documentation, APIProvider apiProvider, String apiId,
                                               String documentId, String organization, String inlineContent)
            throws APIManagementException {
        DocumentationContent content = new DocumentationContent();
        content.setSourceType(DocumentationContent.ContentSourceType.valueOf(documentation.getSourceType().toString()));
        content.setTextContent(inlineContent);
        apiProvider.addDocumentationContent(apiId, documentId, organization, content);
    }

    /**
     * Add documentation content of files.
     *
     * @param inputStream  Input Stream
     * @param mediaType    Media type of the document
     * @param filename     File name
     * @param apiProvider  API Provider
     * @param apiId        API/API Product UUID
     * @param documentId   Document ID
     * @param organization organization of the API
     * @throws APIManagementException If an error occurs while adding the documentation file
     */
    public static void addDocumentationContentForFile(InputStream inputStream, String mediaType, String filename,
                                                      APIProvider apiProvider, String apiId,
                                                      String documentId, String organization)
            throws APIManagementException {
        DocumentationContent content = new DocumentationContent();
        ResourceFile resourceFile = new ResourceFile(inputStream, mediaType);
        resourceFile.setName(filename);
        content.setResourceFile(resourceFile);
        content.setSourceType(DocumentationContent.ContentSourceType.FILE);
        apiProvider.addDocumentationContent(apiId, documentId, organization, content);
    }

    /**
     * Checks whether the list of tiers are valid given the all valid tiers.
     *
     * @param allTiers     All defined tiers
     * @param currentTiers tiers to check if they are a subset of defined tiers
     * @return null if there are no invalid tiers or returns the set of invalid tiers if there are any
     */
    public static List<String> getInvalidTierNames(Set<Tier> allTiers, List<String> currentTiers) {

        List<String> invalidTiers = new ArrayList<>();
        for (String tierName : currentTiers) {
            boolean isTierValid = false;
            for (Tier definedTier : allTiers) {
                if (tierName.equals(definedTier.getName())) {
                    isTierValid = true;
                    break;
                }
            }
            if (!isTierValid) {
                invalidTiers.add(tierName);
            }
        }
        return invalidTiers;
    }


    public static boolean isStreamingAPI(APIDTO apidto) {

        return APIDTO.TypeEnum.WS.equals(apidto.getType()) || APIDTO.TypeEnum.SSE.equals(apidto.getType()) ||
                APIDTO.TypeEnum.WEBSUB.equals(apidto.getType()) || APIDTO.TypeEnum.ASYNC.equals(apidto.getType());
    }

//    public static boolean isThirdPartyAsyncAPI(APIDTO apidto) {
//        return APIDTO.TypeEnum.ASYNC.equals(apidto.getType()) && apidto.getAdvertiseInfo() != null &&
//                apidto.getAdvertiseInfo().isAdvertised();
//    }

    /**
     * Add WSDL file of an API.
     *
     * @param fileContentType Content type of the file
     * @param fileInputStream Input Stream
     * @param api             API to which the WSDL belongs to
     * @param apiProvider     API Provider
     * @param tenantDomain    Tenant domain of the API
     * @throws APIManagementException If an error occurs while adding the WSDL resource
     */
    public static void addWsdl(String fileContentType, InputStream fileInputStream, API api, APIProvider apiProvider,
                               String tenantDomain) throws APIManagementException {
        ResourceFile wsdlResource;
        if (APIConstants.APPLICATION_ZIP.equals(fileContentType) || APIConstants.APPLICATION_X_ZIP_COMPRESSED
                .equals(fileContentType)) {
            wsdlResource = new ResourceFile(fileInputStream, APIConstants.APPLICATION_ZIP);
        } else {
            wsdlResource = new ResourceFile(fileInputStream, fileContentType);
        }
        api.setWsdlResource(wsdlResource);
        apiProvider.addWSDLResource(api.getUuid(), wsdlResource, null, tenantDomain);
    }

//    /**
//     * Set the generated SOAP to REST sequences from the swagger file to the API and update it.
//     *
//     * @param swaggerContent Swagger content
//     * @param api            API to update
//     * @param apiProvider    API Provider
//     * @param organization  Organization Identifier
//     * @return Updated API Object
//     * @throws APIManagementException If an error occurs while generating the sequences or updating the API
//     * @throws FaultGatewaysException If an error occurs while updating the API
//     */
//    public static API updateAPIBySettingGenerateSequencesFromSwagger(String swaggerContent, API api,
//                                                                     APIProvider apiProvider, String organization)
//            throws APIManagementException, FaultGatewaysException {
//        List<SOAPToRestSequence> list = SequenceGenerator.generateSequencesFromSwagger(swaggerContent);
//        API updatedAPI = apiProvider.getAPIbyUUID(api.getUuid(), organization);
//        updatedAPI.setSoapToRestSequences(list);
//        return apiProvider.updateAPI(updatedAPI, api);
//    }

    /**
     * Change the lifecycle state of an API or API Product identified by UUID
     *
     * @param action       LC state change action
     * @param apiTypeWrapper API Type Wrapper (API or API Product)
     * @param lcChecklist  LC state change check list
     * @param organization Organization of logged-in user
     * @return APIStateChangeResponse
     * @throws APIManagementException Exception if there is an error when changing the LC state of API or API Product
     */
    public static APIStateChangeResponse changeApiOrApiProductLifecycle(String action, ApiTypeWrapper apiTypeWrapper,
                                                                        String lcChecklist, String organization)
            throws APIManagementException {

        String[] checkListItems = lcChecklist != null ? lcChecklist.split(APIConstants.DELEM_COMMA) : new String[0];
        APIProvider apiProvider = RestApiCommonUtil.getLoggedInUserProvider();

        Map<String, Object> apiLCData = apiProvider.getAPILifeCycleData(apiTypeWrapper.getUuid(), organization);

        String[] nextAllowedStates = (String[]) apiLCData.get(APIConstants.LC_NEXT_STATES);
        if (!ArrayUtils.contains(nextAllowedStates, action)) {
            throw new APIManagementException("Action '" + action + "' is not allowed. Allowed actions are "
                    + Arrays.toString(nextAllowedStates), ExceptionCodes.from(ExceptionCodes
                    .UNSUPPORTED_LIFECYCLE_ACTION, action));
        }

        //check and set lifecycle check list items including "Deprecate Old Versions" and "Require Re-Subscription".
        Map<String, Boolean> lcMap = new HashMap<>();
        for (String checkListItem : checkListItems) {
            String[] attributeValPair = checkListItem.split(APIConstants.DELEM_COLON);
            if (attributeValPair.length == 2) {
                String checkListItemName = attributeValPair[0].trim();
                boolean checkListItemValue = Boolean.parseBoolean(attributeValPair[1].trim());
                lcMap.put(checkListItemName, checkListItemValue);
            }
        }

        return apiProvider.changeLifeCycleStatus(organization, apiTypeWrapper, action, lcMap);
    }

    /**
     * Retrieve lifecycle history of API or API Product by Identifier
     *
     * @param uuid    Unique UUID of API or API Product
     * @return LifecycleHistoryDTO object
     * @throws APIManagementException exception if there is an error when retrieving the LC history
     */
    public static LifecycleHistoryDTO getLifecycleHistoryDTO(String uuid, APIProvider apiProvider)
            throws APIManagementException {

        List<LifeCycleEvent> lifeCycleEvents = apiProvider.getLifeCycleEvents(uuid);
        return APIMappingUtil.fromLifecycleHistoryModelToDTO(lifeCycleEvents);
    }

    /**
     * Get lifecycle state information of API or API Product
     *
     * @param identifier   Unique identifier of API or API Product
     * @param organization Organization of logged-in user
     * @return LifecycleStateDTO object
     * @throws APIManagementException if there is en error while retrieving the lifecycle state information
     */
    public static LifecycleStateDTO getLifecycleStateInformation(Identifier identifier, String organization)
            throws APIManagementException {

        APIProvider apiProvider = RestApiCommonUtil.getLoggedInUserProvider();
        Map<String, Object> apiLCData = apiProvider.getAPILifeCycleData(identifier.getUUID(), organization);
        if (apiLCData == null) {
            String type;
            if (identifier instanceof APIProductIdentifier) {
                type = APIConstants.API_PRODUCT;
            } else {
                type = APIConstants.API_IDENTIFIER_TYPE;
            }
            throw new APIManagementException("Error while getting lifecycle state for " + type + " with ID "
                    + identifier, ExceptionCodes.from(ExceptionCodes.LIFECYCLE_STATE_INFORMATION_NOT_FOUND, type,
                    identifier.getUUID()));
        } else {
            boolean apiOlderVersionExist = false;
            // check whether other versions of the current API exists
            APIVersionStringComparator comparator = new APIVersionStringComparator();
            Set<String> versions =
                    apiProvider.getAPIVersions(APIUtil.replaceEmailDomain(identifier.getProviderName()),
                            identifier.getName(), organization);

            for (String tempVersion : versions) {
                if (comparator.compare(tempVersion, identifier.getVersion()) < 0) {
                    apiOlderVersionExist = true;
                    break;
                }
            }
            return APIMappingUtil.fromLifecycleModelToDTO(apiLCData, apiOlderVersionExist);
        }
    }


    /**
     * Attaches a file to the specified document
     *
     * @param apiId         identifier of the API, the document belongs to
     * @param documentation Documentation object
     * @param inputStream   input Stream containing the file
     * @param fileName      File name
     * @param mediaType     Media type
     * @param organization  identifier of an organization
     * @throws APIManagementException if unable to add the file
     */
    public static void attachFileToDocument(String apiId, Documentation documentation, InputStream inputStream,
                                            String fileName, String mediaType, String organization)
            throws APIManagementException {

        APIProvider apiProvider = RestApiCommonUtil.getLoggedInUserProvider();
        String documentId = documentation.getId();
        String randomFolderName = RandomStringUtils.randomAlphanumeric(10);
        String tmpFolder = System.getProperty(RestApiConstants.JAVA_IO_TMPDIR) + File.separator
                + RestApiConstants.DOC_UPLOAD_TMPDIR + File.separator + randomFolderName;
        File docFile = new File(tmpFolder);

        boolean folderCreated = docFile.mkdirs();
        if (!folderCreated) {
            throw new APIManagementException("Failed to add content to the document " + documentId,
                    ExceptionCodes.INTERNAL_ERROR);
        }

        InputStream docInputStream = null;
        try {
            if (StringUtils.isBlank(fileName)) {
                fileName = RestApiConstants.DOC_NAME_DEFAULT + randomFolderName;
                log.warn(
                        "Couldn't find the name of the uploaded file for the document " + documentId + ". Using name '"
                                + fileName + "'");
            }
            //APIIdentifier apiIdentifier = APIMappingUtil
            //        .getAPIIdentifierFromUUID(apiId, tenantDomain);

            transferFile(inputStream, fileName, docFile.getAbsolutePath());
            docInputStream = new FileInputStream(docFile.getAbsolutePath() + File.separator + fileName);
            mediaType = mediaType == null ? RestApiConstants.APPLICATION_OCTET_STREAM : mediaType;
            PublisherCommonUtils
                    .addDocumentationContentForFile(docInputStream, mediaType, fileName, apiProvider, apiId,
                            documentId, organization);
            docFile.deleteOnExit();
        } catch (FileNotFoundException e) {
            throw new APIManagementException("Unable to read the file from path ", e, ExceptionCodes.INTERNAL_ERROR);
        } finally {
            IOUtils.closeQuietly(docInputStream);
        }
    }

    /**
     * This method uploads a given file to specified location
     *
     * @param uploadedInputStream input stream of the file
     * @param newFileName         name of the file to be created
     * @param storageLocation     destination of the new file
     * @throws APIManagementException if the file transfer fails
     */
    public static void transferFile(InputStream uploadedInputStream, String newFileName, String storageLocation)
            throws APIManagementException {
        FileOutputStream outFileStream = null;

        try {
            outFileStream = new FileOutputStream(new File(storageLocation, newFileName));
            int read;
            byte[] bytes = new byte[1024];
            while ((read = uploadedInputStream.read(bytes)) != -1) {
                outFileStream.write(bytes, 0, read);
            }
        } catch (IOException e) {
            String errorMessage = "Error in transferring files.";
            log.error(errorMessage, e);
            throw new APIManagementException(errorMessage, e, ExceptionCodes.INTERNAL_ERROR);
        } finally {
            IOUtils.closeQuietly(outFileStream);
        }
    }

    /**
     * This method validates monetization properties
     *
     * @param monetizationProperties map of monetization properties
     * @throws APIManagementException
     */
    public static void validateMonetizationProperties(Map<String, String> monetizationProperties)
            throws APIManagementException {

        String errorMessage;
        if (monetizationProperties != null) {
            for (Map.Entry<String, String> entry : monetizationProperties.entrySet()) {
                String monetizationPropertyKey = entry.getKey().trim();
                String propertyValue = entry.getValue();
                if (monetizationPropertyKey.contains(" ")) {
                    errorMessage = "Monetization property names should not contain space character. " +
                            "Monetization property '" + monetizationPropertyKey + "' "
                            + "contains space in it.";
                    throw new APIManagementException(errorMessage, ExceptionCodes.INVALID_PARAMETERS_PROVIDED);
                }
                // Maximum allowable characters of registry property name and value is 100 and 1000.
                // Hence we are restricting them to be within 80 and 900.
                if (monetizationPropertyKey.length() > 80) {
                    errorMessage = "Monetization property name can have maximum of 80 characters. " +
                            "Monetization property '" + monetizationPropertyKey + "' + contains "
                            + monetizationPropertyKey.length() + "characters";
                    throw new APIManagementException(errorMessage, ExceptionCodes.INVALID_PARAMETERS_PROVIDED);
                }
                if (propertyValue.length() > 900) {
                    errorMessage = "Monetization property value can have maximum of 900 characters. " +
                            "Property '" + monetizationPropertyKey + "' + "
                            + "contains a value with " + propertyValue.length() + "characters";
                    throw new APIManagementException(errorMessage, ExceptionCodes.INVALID_PARAMETERS_PROVIDED);
                }
            }
        }
    }

    /**
     * This method is used to read input stream of a file and return the string content.
     * @param fileInputStream File input stream
     * @return String
     * @throws APIManagementException*/
    public static String readInputStream(InputStream fileInputStream)
            throws APIManagementException {

        String content = null;
        if (fileInputStream != null) {
            try {
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                IOUtils.copy(fileInputStream, outputStream);
                byte[] sequenceBytes = outputStream.toByteArray();
                InputStream inSequenceStream = new ByteArrayInputStream(sequenceBytes);
                content = IOUtils.toString(inSequenceStream, StandardCharsets.UTF_8.name());
            } catch (IOException e) {
                throw new APIManagementException("Error occurred while reading inputs", e,
                        ExceptionCodes.INTERNAL_ERROR);
            }

        }
        return content;
    }

    public static File exportOperationPolicyData(OperationPolicyData policyData, String format)
            throws APIManagementException {

        File exportFolder = null;
        try {
            exportFolder = CommonUtil.createTempDirectoryFromName(policyData.getSpecification().getName()
                    + "_" + policyData.getSpecification().getVersion());
            String exportAPIBasePath = exportFolder.toString();
            String archivePath =
                    exportAPIBasePath.concat(File.separator + policyData.getSpecification().getName());
            CommonUtil.createDirectory(archivePath);
            String policyName = archivePath + File.separator + policyData.getSpecification().getName();
            if (policyData.getSpecification() != null) {
                if (format.equalsIgnoreCase(ExportFormat.YAML.name())) {
                    CommonUtil.writeDtoToFile(policyName, ExportFormat.YAML,
                            ImportExportConstants.TYPE_POLICY_SPECIFICATION,
                            policyData.getSpecification());
                } else if (format.equalsIgnoreCase(ExportFormat.JSON.name())) {
                    CommonUtil.writeDtoToFile(policyName, ExportFormat.JSON,
                            ImportExportConstants.TYPE_POLICY_SPECIFICATION,
                            policyData.getSpecification());
                }
            }
            if (policyData.getSynapsePolicyDefinition() != null) {
                CommonUtil.writeFile(policyName + APIConstants.SYNAPSE_POLICY_DEFINITION_EXTENSION,
                        policyData.getSynapsePolicyDefinition().getContent());
            }
            if (policyData.getCcPolicyDefinition() != null) {
                CommonUtil.writeFile(policyName + APIConstants.CC_POLICY_DEFINITION_EXTENSION,
                        policyData.getCcPolicyDefinition().getContent());
            }

            CommonUtil.archiveDirectory(exportAPIBasePath);
            FileUtils.deleteQuietly(new File(exportAPIBasePath));
            return new File(exportAPIBasePath + APIConstants.ZIP_FILE_EXTENSION);
        } catch (APIImportExportException | IOException e) {
            throw new APIManagementException("Error while exporting operation policy", e,
                    ExceptionCodes.INTERNAL_ERROR);
        }
    }
}