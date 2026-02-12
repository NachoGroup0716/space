package com.adaptor.custom.utils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

public class APIUtils {
	final private static RestTemplate restTemplate = new RestTemplate();
	
	public static List<Map<String, Object>> createPostForList(String url, Map<String, Object> param) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Map> request = new HttpEntity<Map>(param, headers);
		ResponseEntity<List> response = restTemplate.exchange(url, HttpMethod.POST, request, List.class);
		return response.getBody();
	}
}
