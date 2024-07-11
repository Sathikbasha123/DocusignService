package com.saaspe.docusign.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import com.docusign.esign.client.ApiClient;

@Configuration
public class DocusignApiclient {

	@Value("${oAuthBasePath}")
	public String oAuthBasePath;

	@Value("${restBasePath}")
	public String restBasePath;

	@Bean
	ApiClient apiClient() {
		ApiClient apiClient = new ApiClient(restBasePath);
		apiClient.setOAuthBasePath(oAuthBasePath);
		return apiClient;
	}
	
	@Bean
	public RestTemplate getRestTemplate() {
		return new RestTemplate();
	}


}
