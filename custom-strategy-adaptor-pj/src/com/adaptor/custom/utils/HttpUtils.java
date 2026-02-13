package com.adaptor.custom.utils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.adaptor.custom.utils.constants.CheckOptions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import okhttp3.Dispatcher;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Slf4j
public class HttpUtils {
	private static final OkHttpClient client;
	private static final ObjectMapper mapper = new ObjectMapper();
	public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
	public static final MediaType FORM = MediaType.parse("application/x-www-form-urlencoded");
	public static final MediaType TEXT = MediaType.parse("text/plain; charset=utf-8");
	public static final MediaType HTML = MediaType.parse("text/html; charset=utf-8");
	
	static {
		Dispatcher dispatcher = new Dispatcher();
		dispatcher.setMaxRequests(200);
		dispatcher.setMaxRequestsPerHost(100);
		
		client = new OkHttpClient.Builder()
								.connectTimeout(60, TimeUnit.SECONDS)
								.readTimeout(60, TimeUnit.SECONDS)
								.writeTimeout(60, TimeUnit.SECONDS)
								.dispatcher(dispatcher)
								.retryOnConnectionFailure(false)
								.build();
	}
	
	public static String toJson(Object data) throws JsonProcessingException {
		if (data == null) return "";
		return mapper.writeValueAsString(data);
	}
	
	public static Map<String, Object> getForMap(String url) throws IOException {
		Request request = new Request.Builder().url(url).get().build();
		return execute(request, new TypeReference<Map<String, Object>>() {	});
	}
	
	public static List<Map<String, Object>> getForList(String url) throws IOException {
		Request request = new Request.Builder().url(url).get().build();
		return execute(request, new TypeReference<List<Map<String, Object>>>() {	});
	}
	
	public static Map<String, Object> postForMap(String url, Object params) throws IOException {
		return postForMap(url, params, null);
	}
	
	public static Map<String, Object> postForMap(String url, Object params, MediaType contentType) throws IOException {
		RequestBody body = createRequestBody(params, contentType);
		Request request = new Request.Builder().url(url).post(body).build();
		return execute(request, new TypeReference<Map<String, Object>>() {	});
	}
	
	public static List<Map<String, Object>> postForList(String url, Object params) throws IOException {
		return postForList(url, params, null);
	}
	
	public static List<Map<String, Object>> postForList(String url, Object params, MediaType contentType) throws IOException {
		RequestBody body = createRequestBody(params, contentType);
		Request request = new Request.Builder().url(url).post(body).build();
		return execute(request, new TypeReference<List<Map<String, Object>>>() {	});
	}
	
	private static RequestBody createRequestBody(Object params, MediaType mediaType) throws JsonProcessingException {
		if (mediaType == null) {
			mediaType = JSON;
		}
		
		if ("x-www-form-urlencoded".equalsIgnoreCase(mediaType.subtype())) {
			FormBody.Builder formBuilder = new FormBody.Builder();
			
			if (params == null) return formBuilder.build();
			
			Map<String, Object> mapParams;
			if (params instanceof Map) {
				mapParams = (Map<String, Object>) params;
			} else {
				mapParams = mapper.convertValue(params, new TypeReference<Map<String, Object>>() {	});
			}
			
			for (Map.Entry<String, Object> entry : mapParams.entrySet()) {
				String value = (entry.getValue() == null) ? "" : String.valueOf(entry.getValue());
				formBuilder.add(entry.getKey(), value);
			}
			return formBuilder.build();
		}
		
		String content = (params instanceof String) ? (String) params : toJson(params);
		return RequestBody.create(mediaType, content);
	}
	
	private static <T> T execute(Request request, TypeReference<T> typeRef) throws IOException {
		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				throw new IOException("Unexpected HTTP Code: " + response.code());
			}
			if (response.body() == null) return null;
			
			String responseBody = response.body().string();
			if (StringUtils.checkAll(responseBody, CheckOptions.IS_EMPTY)) return null;
			return mapper.readValue(responseBody, typeRef);
		}
	}
}
