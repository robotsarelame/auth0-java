package com.auth0.net;

import com.auth0.exception.APIException;
import com.auth0.exception.Auth0Exception;
import com.auth0.exception.RateLimitException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.MapType;
import okhttp3.*;
import okhttp3.Request;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("WeakerAccess")
public class CustomRequest<T> extends BaseRequest<T> implements CustomizableRequest<T> {

    private static final String CONTENT_TYPE_APPLICATION_JSON = "application/json";

    private final String url;
    private final String method;
    private final ObjectMapper mapper;
    private final TypeReference<T> tType;
    private final Map<String, String> headers;
    private final Map<String, Object> parameters;
    private Object body;

    private static final int STATUS_CODE_TOO_MANY_REQUEST = 429;

    CustomRequest(OkHttpClient client, String url, String method, ObjectMapper mapper, TypeReference<T> tType) {
        super(client);
        this.url = url;
        this.method = method;
        this.mapper = mapper;
        this.tType = tType;
        this.headers = new HashMap<>();
        this.parameters = new HashMap<>();
    }

    public CustomRequest(OkHttpClient client, String url, String method, TypeReference<T> tType) {
        this(client, url, method, new ObjectMapper(), tType);
    }

    @Override
    protected Request createRequest() throws Auth0Exception {
        Request.Builder builder = new Request.Builder()
                .url(url)
                .method(method, createBody());
        for (Map.Entry<String, String> e : headers.entrySet()) {
            builder.addHeader(e.getKey(), e.getValue());
        }
        builder.addHeader("Content-Type", CONTENT_TYPE_APPLICATION_JSON);
        return builder.build();
    }

    @Override
    protected T parseResponse(Response response) throws Auth0Exception {
        if (!response.isSuccessful()) {
            throw createResponseException(response);
        }

        try (ResponseBody body = response.body()) {
            String payload = body.string();
            return mapper.readValue(payload, tType);
        } catch (IOException e) {
            throw new APIException("Failed to parse json body", response.code(), e);
        }
    }

    @Override
    public CustomRequest<T> addHeader(String name, String value) {
        headers.put(name, value);
        return this;
    }

    @Override
    public CustomRequest<T> addParameter(String name, Object value) {
        parameters.put(name, value);
        return this;
    }

    @Override
    public CustomRequest<T> setBody(Object value) {
        body = value;
        return this;
    }

    protected RequestBody createBody() throws Auth0Exception {
        if (body == null && parameters.isEmpty()) {
            return null;
        }
        try {
            byte[] jsonBody = mapper.writeValueAsBytes(body != null ? body : parameters);
            return RequestBody.create(MediaType.parse(CONTENT_TYPE_APPLICATION_JSON), jsonBody);
        } catch (JsonProcessingException e) {
            throw new Auth0Exception("Couldn't create the request body.", e);
        }
    }

    protected Auth0Exception createResponseException(Response response) {
        if (response.code() == STATUS_CODE_TOO_MANY_REQUEST) {
            return createRateLimitException(response);
        }

        String payload = null;
        try (ResponseBody body = response.body()) {
            payload = body.string();
            MapType mapType = mapper.getTypeFactory().constructMapType(HashMap.class, String.class, Object.class);
            Map<String, Object> values = mapper.readValue(payload, mapType);
            return new APIException(values, response.code());
        } catch (IOException e) {
            return new APIException(payload, response.code(), e);
        }
    }

    private RateLimitException createRateLimitException(Response response) {
        // -1 as default value if the header could not be found.
        long limit = Long.parseLong(response.header("X-RateLimit-Limit", "-1"));
        long remaining = Long.parseLong(response.header("X-RateLimit-Remaining", "-1"));
        long reset = Long.parseLong(response.header("X-RateLimit-Reset", "-1"));
        return new RateLimitException(limit, remaining, reset);
    }
}
