package com.saaspe.docusign.service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.docusign.admin.client.ApiClient;
import com.docusign.admin.client.auth.OAuth.OAuthToken;
import com.docusign.esign.api.EnvelopesApi;
import com.docusign.esign.client.ApiException;
import com.docusign.esign.model.Document;
import com.docusign.esign.model.EnvelopeDefinition;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saaspe.docusign.constant.Constant;

@Service
public class AdminServiceImpl implements AdminService {

	@Value("${clientId}")
	public String clientId;

	@Value("${userId}")
	public String adminuserId;

	@Value("${rsaKeyFile}")
	public String rsaKeyFile;
	
	@Value("${rsaKeyFilePath}")
	public String rsaKeyFilePath;

	@Value("${organizationId}")
	public String organizationId;

	@Value("${accountId}")
	public String accountId;

	@Value("${permissionProfileId}")
	public String permissionProfileId;

	@Value("${docusignHost}")
	public String docusignHost;

	@Value("${oAuthBasePath}")
	public String oAuthBasePath;

	@Value("${basePath}")
	public String basePath;

	@Value("${docusign-organization-baseurl}")
	public String docusignOrganizationUrl;

	@Value("${clmHost}")
	public String clmHost;

	@Autowired
	private RestTemplate restTemplate;

	public String getAuthTokenOfAdmin() {
		//ClassPathResource resource = new ClassPathResource("resources/" + rsaKeyFile);
		String accessToken = null;
		try {
			ArrayList<String> scopes = new ArrayList<>();
			scopes.add("signature");
			scopes.add("impersonation");
			scopes.add("user_write");
			scopes.add("user_read");
			scopes.add("organization_read");
			scopes.add("account_read");
			ApiClient apiClient = new ApiClient(basePath);
			apiClient.setOAuthBasePath(oAuthBasePath);
		//  byte[] privateKeyBytes = resource.getClassLoader().getResource(rsaKeyFile).openStream().readAllBytes();
		//	String directory = "C:/rsakeyfile";
		//	String file_path = directory + rsaKeyFile;
			Path filePath = Paths.get(rsaKeyFilePath, rsaKeyFile);
			System.out.println("File Path "+filePath);
            byte[] fileBytes = Files.readAllBytes(filePath);
            System.out.println("File length "+fileBytes.length);
			OAuthToken oAuthToken = apiClient.requestJWTUserToken(clientId, adminuserId, scopes, fileBytes, 3600);
			accessToken = oAuthToken.getAccessToken();
			System.out.println("Access Token - "+accessToken);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return accessToken;
	}

	@Override
	public Map<String, String> getOrganizations() {
		Map<String, String> organiztaionDetails = new HashMap<>();
		HttpClient client = HttpClient.newBuilder().build();
		HttpRequest request = HttpRequest.newBuilder().uri(URI.create(docusignOrganizationUrl))
				.header(Constant.ACCEPT, Constant.APPLICATION_JSON).header(Constant.ACCEPT, Constant.APPLICATION_JSON)
				.header(Constant.CONTENT_TYPE, Constant.APPLICATION_JSON)
				.header(Constant.AUTHORIZATION, Constant.BEARER + getAuthTokenOfAdmin()).GET().build();
		HttpResponse<String> response = null;
		try {
			response = client.send(request, BodyHandlers.ofString());
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}
		String responseBody = response.body();
		System.out.println(responseBody);
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = objectMapper.readTree(responseBody);
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		JsonNode organizationsNode = rootNode.get("organizations");
		if (organizationsNode.isArray() && organizationsNode.size() > 0) {
			JsonNode firstOrganizationNode = organizationsNode.get(0);
			JsonNode organizatoinId = firstOrganizationNode.get("id");
			JsonNode organizatoinName = firstOrganizationNode.get("name");
			JsonNode accountsId = firstOrganizationNode.get("default_account_id");
			organiztaionDetails.put("organizatoinId", organizatoinId.asText());
			organiztaionDetails.put("organizatoinName", organizatoinName.asText());
			organiztaionDetails.put("accountId", accountsId.asText());
		}
		System.out.println(organiztaionDetails);
		return organiztaionDetails;
	}

	@Override
	public Map<String, List<Map<String, String>>> getUsers(String userEmail, String firstName, String lastName) {
		Map<String, List<Map<String, String>>> usersDataList = new HashMap<>();
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(docusignOrganizationUrl + "/" + organizationId + Constant.USER_ACCOUNT_ID + accountId))
				.GET().header(Constant.ACCEPT, Constant.APPLICATION_JSON)
				.header(Constant.CONTENT_TYPE, Constant.APPLICATION_JSON)
				.header(Constant.AUTHORIZATION, Constant.BEARER + getAuthTokenOfAdmin()).build();
		HttpResponse<String> response = null;
		try {
			response = client.send(request, BodyHandlers.ofString());
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = objectMapper.readTree(response.body());
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		JsonNode usersNode = rootNode.get(Constant.USERS);
		if (usersNode.isArray() && usersNode.size() > 0) {
			List<Map<String, String>> list = new ArrayList<>();
			for (JsonNode userNode : usersNode) {
				Map<String, String> usersData = new HashMap<>();
				usersData.put("userId", userNode.get("id").asText());
				usersData.put(Constant.USER_NAME, userNode.get(Constant.USER_NAME).asText());
				usersData.put(Constant.FIRST_NAME, userNode.get(Constant.FIRST_NAME).asText());
				usersData.put(Constant.LAST_NAME, userNode.get(Constant.LAST_NAME).asText());
				usersData.put(Constant.MEMBERSHIP_STATUS, userNode.get(Constant.MEMBERSHIP_STATUS).asText());
				usersData.put(Constant.EMAIL, userNode.get(Constant.EMAIL).asText());
				usersData.put(Constant.MEMBERSHIP_CREATED_ON, userNode.get(Constant.MEMBERSHIP_CREATED_ON).asText());
				usersData.put(Constant.MEMBERSHIP_ID, userNode.get(Constant.MEMBERSHIP_ID).asText());
				list.add(usersData);
			}
			usersDataList.put(Constant.USERS, list);
		}
		List<Map<String, String>> userList = usersDataList.get(Constant.USERS);
		for (Map<String, String> user : userList) {
			String email = user.get(Constant.EMAIL);
			if (!email.trim().equalsIgnoreCase(userEmail)) {
				createUser(firstName, lastName, userEmail);
			}
		}
		return usersDataList;
	}

	public Map<String, List<Map<String, String>>> getUsersList() {
		Map<String, List<Map<String, String>>> usersDataList = new HashMap<>();
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(docusignOrganizationUrl + "/" + organizationId + Constant.USER_ACCOUNT_ID + accountId))
				.GET().header(Constant.ACCEPT, Constant.APPLICATION_JSON)
				.header(Constant.CONTENT_TYPE, Constant.APPLICATION_JSON)
				.header(Constant.AUTHORIZATION, Constant.BEARER + getAuthTokenOfAdmin()).build();
		HttpResponse<String> response = null;
		try {
			response = client.send(request, BodyHandlers.ofString());
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode rootNode = null;
		try {
			rootNode = objectMapper.readTree(response.body());
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		JsonNode usersNode = rootNode.get(Constant.USERS);
		if (usersNode.isArray() && usersNode.size() > 0) {
			List<Map<String, String>> list = new ArrayList<>();
			for (JsonNode userNode : usersNode) {
				Map<String, String> usersData = new HashMap<>();
				usersData.put("userId", userNode.get("id").asText());
				usersData.put(Constant.USER_NAME, userNode.get(Constant.USER_NAME).asText());
				usersData.put(Constant.FIRST_NAME, userNode.get(Constant.FIRST_NAME).asText());
				usersData.put(Constant.LAST_NAME, userNode.get(Constant.LAST_NAME).asText());
				usersData.put(Constant.MEMBERSHIP_STATUS, userNode.get(Constant.MEMBERSHIP_STATUS).asText());
				usersData.put(Constant.EMAIL, userNode.get(Constant.EMAIL).asText());
				usersData.put(Constant.MEMBERSHIP_CREATED_ON, userNode.get(Constant.MEMBERSHIP_CREATED_ON).asText());
				usersData.put(Constant.MEMBERSHIP_ID, userNode.get(Constant.MEMBERSHIP_ID).asText());
				list.add(usersData);
			}
			usersDataList.put(Constant.USERS, list);
		}
		return usersDataList;
	}

	private String createUser(String firstName, String lastName, String email) {
		JSONObject jsonObject = new JSONObject();
		Map<String, String> map = new HashMap<>();
		map.put("id", permissionProfileId);
		map.put("name", "DocuSign Sender");
		jsonObject.put(Constant.FIRST_NAME, firstName);
		if (lastName != null) {
			if (!lastName.equalsIgnoreCase("null")) {
				jsonObject.put(Constant.USER_NAME, firstName + " " + lastName);
				jsonObject.put(Constant.LAST_NAME, lastName);
			} else {
				jsonObject.put(Constant.USER_NAME, firstName);
			}
		} else {
			jsonObject.put(Constant.USER_NAME, firstName);
		}
		jsonObject.put(Constant.EMAIL, email);
		jsonObject.put("default_account_id", accountId);
		jsonObject.put("language_culture", "en");
		jsonObject.put("auto_activate_memberships", false);
		jsonObject.put("permission_profile", map);
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = null;
		try {
			request = HttpRequest.newBuilder()
					.uri(new URI(docusignOrganizationUrl + "/" + organizationId + "/accounts/" + accountId + "/users"))
					.POST(BodyPublishers.ofString(jsonObject.toString()))
					.header(Constant.ACCEPT, Constant.APPLICATION_JSON)
					.header(Constant.CONTENT_TYPE, Constant.APPLICATION_JSON)
					.header("Authorization", Constant.BEARER + getAuthTokenOfAdmin()).build();
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		HttpResponse<String> response = null;
		try {
			response = client.send(request, BodyHandlers.ofString());
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}
		return response.body();
	}

	@Override
	public String createDocusignUser(String firstName, String lastName, String userEmail) {
		return createUser(firstName, lastName, userEmail);
	}

	@Override
	public String getDocusignUser() {
		HttpClient client = HttpClient.newHttpClient();
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(docusignOrganizationUrl + "/" + organizationId + Constant.USER_ACCOUNT_ID + accountId))
				.GET().header(Constant.ACCEPT, Constant.APPLICATION_JSON)
				.header(Constant.CONTENT_TYPE, Constant.APPLICATION_JSON)
				.header("Authorization", Constant.BEARER + getAuthTokenOfAdmin()).build();
		HttpResponse<String> response = null;
		try {
			response = client.send(request, BodyHandlers.ofString());
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			Thread.currentThread().interrupt();
		}
		return response.body();

	}

	public void attachFilesAsyn(EnvelopesApi envelopeApi, Document a, String envelopeId) {
		ExecutorService executor = Executors.newSingleThreadExecutor();
		executor.submit(() -> {
			EnvelopeDefinition definition = new EnvelopeDefinition();
			definition.setDocuments(Collections.singletonList(a));
			try {
				envelopeApi.updateDocuments(accountId, envelopeId, definition);
			} catch (ApiException e) {
				e.printStackTrace();
			}
			definition = null;
			System.gc();
			Thread.currentThread().setName(envelopeId);
			UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(clmHost + "/docusign/file-upload");
			builder.queryParam("envelopeId", envelopeId);
			builder.queryParam("documentId", a.getDocumentId());
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<?> httpEntity = new HttpEntity<>(headers);
			try {
				restTemplate.exchange(builder.toUriString(), HttpMethod.PUT, httpEntity, Object.class);
			} catch (HttpClientErrorException.BadRequest ex) {
				ex.printStackTrace();
			}
			executor.shutdown();
		});
	}

}
