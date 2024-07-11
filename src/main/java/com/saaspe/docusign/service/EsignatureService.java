package com.saaspe.docusign.service;

import java.awt.Desktop;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import com.docusign.admin.api.UsersApi;
import com.docusign.admin.client.auth.OAuth.OAuthToken;
import com.docusign.admin.model.MembershipResponse;
import com.docusign.admin.model.UserDrilldownResponse;
import com.docusign.esign.api.EnvelopesApi;
import com.docusign.esign.api.EnvelopesApi.GetEnvelopeOptions;
import com.docusign.esign.api.TemplatesApi;
import com.docusign.esign.api.TemplatesApi.GetOptions;
import com.docusign.esign.client.ApiClient;
import com.docusign.esign.client.ApiException;
import com.docusign.esign.client.auth.OAuth;
import com.docusign.esign.client.auth.OAuth.Account;
import com.docusign.esign.client.auth.OAuth.UserInfo;
import com.docusign.esign.model.CarbonCopy;
import com.docusign.esign.model.ConsoleViewRequest;
import com.docusign.esign.model.Document;
import com.docusign.esign.model.Envelope;
import com.docusign.esign.model.EnvelopeAuditEvent;
import com.docusign.esign.model.EnvelopeAuditEventResponse;
import com.docusign.esign.model.EnvelopeDefinition;
import com.docusign.esign.model.EnvelopeDocument;
import com.docusign.esign.model.EnvelopeDocumentsResult;
import com.docusign.esign.model.EnvelopeSummary;
import com.docusign.esign.model.EnvelopeTemplate;
import com.docusign.esign.model.EnvelopesInformation;
import com.docusign.esign.model.Expirations;
import com.docusign.esign.model.LockInformation;
import com.docusign.esign.model.LockRequest;
import com.docusign.esign.model.NameValue;
import com.docusign.esign.model.Notification;
import com.docusign.esign.model.RecipientEmailNotification;
import com.docusign.esign.model.RecipientViewRequest;
import com.docusign.esign.model.Recipients;
import com.docusign.esign.model.Reminders;
import com.docusign.esign.model.SignHere;
import com.docusign.esign.model.Signer;
import com.docusign.esign.model.Tabs;
import com.docusign.esign.model.TemplateDocumentsResult;
import com.docusign.esign.model.TemplateRecipients;
import com.docusign.esign.model.TemplateSummary;
import com.docusign.esign.model.ViewUrl;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.azure.storage.StorageException;
import com.microsoft.azure.storage.blob.CloudBlobClient;
import com.microsoft.azure.storage.blob.CloudBlobContainer;
import com.microsoft.azure.storage.blob.CloudBlockBlob;
import com.saaspe.docusign.constant.Constant;
import com.saaspe.docusign.model.AllSigners;
import com.saaspe.docusign.model.AuditEvent;
import com.saaspe.docusign.model.DeleteEnvelopeRequest;
import com.saaspe.docusign.model.DocumentResponse;
import com.saaspe.docusign.model.EnvelopeResponse;
import com.saaspe.docusign.model.EsignaturePojo;
import com.saaspe.docusign.model.EventField;
import com.saaspe.docusign.model.EventFieldsList;
import com.saaspe.docusign.model.TemplateGetByIdResponse;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Service
public class EsignatureService {

	@Value("${clientId}")
	String clientId;

	@Value("${userId}")
	String userId;

	@Value("${rsaKeyFile}")
	String rsaKeyFile;

	@Value("${rsaKeyFilePath}")
	public String rsaKeyFilePath;

	@Value("${redirectUrl}")
	String redirectUrl;

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

	@Value("${restBasePath}")
	public String restBasePath;

	@Value("${docusign-oauth-url}")
	public String docusignOAuthPath;

	@Value("${docusign-organization-baseurl}")
	public String docusignOrganizationUrl;

	@Value("${azure.storage.container.name}")
	private String containerName;

	@Value("${blob.videoURL}")
	private String videoURL;

	@Value("${image.key}")
	private String imageKey;
	@Autowired
	private CloudBlobClient cloudBlobClient;

	private final ApiClient apiClient;

	private static final Logger log = LoggerFactory.getLogger(EsignatureService.class);

	public EsignatureService(ApiClient apiClient) {
		this.apiClient = apiClient;
	}

	@Autowired
	AdminServiceImpl adminService;

	@Data
	@AllArgsConstructor
	@NoArgsConstructor
	public class GetUserTokenResponse {
		private String accessToken;
		private String accountId;
		private ApiClient apiClient;
		private String errorMessage;
		private String authEnpoint;
		private String activated;
	}

	private String getAccessToken() throws IOException, IllegalArgumentException, ApiException {
		ArrayList<String> scopes = new ArrayList<>();
		scopes.add(Constant.SIGNATURE);
		scopes.add(Constant.IMPERSONATION);
		// byte[] privateKeyBytes =
		// getClass().getClassLoader().getResourceAsStream(rsaKeyFile).readAllBytes();
		Path filePath = Paths.get(rsaKeyFilePath, rsaKeyFile);
		byte[] fileBytes = Files.readAllBytes(filePath);
		OAuth.OAuthToken oAuthToken = apiClient.requestJWTUserToken(clientId, userId, scopes, fileBytes, 3600);
		return oAuthToken.getAccessToken();
	}

	private GetUserTokenResponse generateUserToken(String userEmail) {
		String accessToken = null;
		GetUserTokenResponse userData = new GetUserTokenResponse();

		Map<String, List<Map<String, String>>> usersList = adminService.getUsersList();
		List<Map<String, String>> userList = usersList.get("users");
		for (Map<String, String> user : userList) {
			String email = user.get("email");
			if (email.trim().equalsIgnoreCase(userEmail)) {
				try {
					ArrayList<String> scopes = new ArrayList<>();
					scopes.add(Constant.SIGNATURE);
					scopes.add(Constant.IMPERSONATION);
					byte[] privateKeyBytes = getClass().getClassLoader().getResourceAsStream(rsaKeyFile).readAllBytes();
					OAuth.OAuthToken oAuthToken = apiClient.requestJWTUserToken(clientId, user.get("userId"), scopes,
							privateKeyBytes, 3600);
					accessToken = oAuthToken.getAccessToken();
					apiClient.addDefaultHeader(Constant.AUTHORIZATION, Constant.BEARER + accessToken);
					UserInfo userInfo = apiClient.getUserInfo(accessToken);
					userData.setApiClient(apiClient);
					userData.setAccessToken(accessToken);
					for (Account account : userInfo.getAccounts()) {
						if (account.getOrganization().getOrganizationId() != null
								&& (account.getOrganization().getOrganizationId().equalsIgnoreCase(organizationId))) {
							userData.setAccountId(account.getAccountId());
						}
					}
				} catch (ApiException exp) {
					exp.printStackTrace();
					if (exp.getMessage().contains(Constant.CONSENT_REQUIRED)) {
						try {
							userData.setErrorMessage(Constant.CONSENT_REQUIRED);
							userData.setAuthEnpoint(
									docusignHost + docusignOAuthPath + clientId + Constant.REDIRECT_URI + redirectUrl);
							return userData;
						} catch (Exception e) {
							userData.setErrorMessage(e.getMessage());
							return userData;
						}
					}
				} catch (Exception e) {
					userData.setErrorMessage(e.getMessage());
					return userData;
				}
			}
		}
		return userData;
	}

	public ResponseEntity<?> createEnvelopeMultiple(EsignaturePojo signatureModel, String templateId, String userId)
			throws ApiException, IllegalArgumentException, IOException, com.docusign.admin.client.ApiException {
		GetUserTokenResponse token = generateUserTokenByUserId(signatureModel.getUserEmail(), userId);
		ApiClient apiclient = apiClient.addDefaultHeader(Constant.AUTHORIZATION,
				Constant.BEARER + token.getAccessToken());
		EnvelopeSummary results = null;
		EnvelopeDefinition envelope = new EnvelopeDefinition();
		Notification notification = new Notification();
		Reminders reminders = new Reminders();
		reminders.setReminderDelay(signatureModel.getReminders().getReminderDelay());
		reminders.setReminderFrequency(signatureModel.getReminders().getReminderFrequency());
		reminders.setReminderEnabled(String.valueOf(signatureModel.getReminders().getReminderEnabled()));
		notification.setReminders(reminders);
		notification.setUseAccountDefaults(Constant.FALSE);
		Expirations expirations = new Expirations();
		expirations.setExpireAfter(signatureModel.getExpirations().getExpireAfter());
		expirations.setExpireEnabled(String.valueOf(signatureModel.getExpirations().getExpireEnabled()));
		expirations.setExpireWarn(signatureModel.getExpirations().getExpireWarn());
		notification.setExpirations(expirations);
		TemplatesApi templatesApi = new TemplatesApi(apiclient);
		envelope.setNotification(notification);
		envelope.setEmailSubject(signatureModel.getEmailSubject());
		envelope.setEmailBlurb(signatureModel.getEmailMessage());
		envelope.setStatus(signatureModel.getStatus());
		List<Document> createDocuments = new ArrayList<>();
		TemplateDocumentsResult envelopeDocuments = null;
		if (templateId != null) {
			envelopeDocuments = templatesApi.listDocuments(accountId, templateId);
			envelopeDocuments.getTemplateDocuments()
					.removeIf(p -> p.getDocumentId().equalsIgnoreCase(Constant.CERTIFICATE));
		}
		for (com.saaspe.docusign.model.Document document : signatureModel.getDocuments()) {
			SignHere signHere = new SignHere();
			signHere.setPageNumber("1");
			signHere.setXPosition("191");
			signHere.setYPosition("148");
			Tabs tabs = new Tabs();
			tabs.setSignHereTabs(Arrays.asList(signHere));
			if (document.getCategory().equalsIgnoreCase(Constant.CREATE_FILE)) {
				Document createDocument = new Document();
				createDocument.setName(document.getName());
				createDocument.setDocumentBase64(document.getDocumentBase64());
				createDocument.setDocumentId(document.getDocumentId());
				createDocument.setTabs(tabs);
				createDocuments.add(createDocument);
			} else if (document.getCategory().equalsIgnoreCase(Constant.DELETE)) {
				envelopeDocuments.getTemplateDocuments()
						.removeIf(p -> p.getDocumentId().equalsIgnoreCase(document.getDocumentId()));
			} else if (document.getCategory().equalsIgnoreCase(Constant.UPDATE_FILE)) {
				Document updateDocument = new Document();
				updateDocument.setName(document.getName());
				updateDocument.setDocumentBase64(document.getDocumentBase64());
				updateDocument.setDocumentId(document.getDocumentId());
				updateDocument.setTabs(tabs);
				envelopeDocuments.getTemplateDocuments()
						.removeIf(p -> p.getDocumentId().equalsIgnoreCase(document.getDocumentId()));
				createDocuments.add(updateDocument);
			}
		}
		if (envelopeDocuments != null) {
			for (EnvelopeDocument documentInfo : envelopeDocuments.getTemplateDocuments()) {
				byte[] documentBytes = templatesApi.getDocument(accountId, templateId, documentInfo.getDocumentId());
				String base64Doc = Base64.getEncoder().encodeToString(documentBytes);
				Document document1 = new Document();
				document1.setName(documentInfo.getName());
				document1.setDocumentBase64(base64Doc);
				document1.setDocumentId(documentInfo.getDocumentId());
				createDocuments.add(document1);
			}
		}
		List<Document> documentList = new ArrayList<>();
		documentList.addAll(createDocuments);
		int i = 1;
		SignHere signHere = new SignHere();
		signHere.setPageNumber("1");
		signHere.setXPosition("191");
		signHere.setYPosition("148");
		Tabs tabs = new Tabs();
		tabs.setSignHereTabs(Arrays.asList(signHere));
		List<Signer> signerList = new ArrayList<>();
		for (com.saaspe.docusign.model.Signer signer : signatureModel.getRecipients().getSigners()) {
			i++;
			Signer finalSigner = new Signer();
			finalSigner.setEmail(signer.getEmail());
			finalSigner.setName(signer.getName());
			finalSigner.setRecipientId(String.valueOf(i));
			finalSigner.setRoutingOrder(signer.getRoutingOrder());
			signerList.add(finalSigner);
		}
		List<CarbonCopy> carbonCopyList = new ArrayList<>();
		for (com.saaspe.docusign.model.Carboncopy cc : signatureModel.getRecipients().getCc()) {
			i++;
			CarbonCopy ccFinal = new CarbonCopy();
			ccFinal.setEmail(cc.getEmail());
			ccFinal.setName(cc.getName());
			ccFinal.setRecipientId(String.valueOf(i));
			ccFinal.setRoutingOrder(cc.getRoutingOrder());
			carbonCopyList.add(ccFinal);
		}
		Recipients recipients = new Recipients();
		Signer signer = new Signer();
		signer.setTabs(tabs);
		recipients.setSigners(signerList);
		recipients.setCarbonCopies(carbonCopyList);
		envelope.setRecipients(recipients);
		envelope.setAllowComments(signatureModel.getAllowComments());
		envelope.recipientsLock(String.valueOf(signatureModel.getRecipientLock()));
		envelope.messageLock(String.valueOf(signatureModel.getMessageLock()));
		envelope.setEnforceSignerVisibility(String.valueOf(signatureModel.getEnforceSignerVisibility()));
		envelope.setSignerCanSignOnMobile(String.valueOf(signatureModel.getSignerCanSignOnMobile()));
		EnvelopesApi envelopesApi = new EnvelopesApi(apiclient);
		results = envelopesApi.createEnvelope(accountId, envelope);

		for (Document document : documentList) {
			adminService.attachFilesAsyn(envelopesApi, document, results.getEnvelopeId());
		}

		return ResponseEntity.ok().body(results);
	}

	public ResponseEntity<?> newCreateEnvelopeMultiple(EsignaturePojo signatureModel, String templateId, String userId)
			throws ApiException, IllegalArgumentException, IOException, com.docusign.admin.client.ApiException {
		GetUserTokenResponse token = generateUserTokenByUserId(signatureModel.getUserEmail(), userId);
		ApiClient apiclient = apiClient.addDefaultHeader(Constant.AUTHORIZATION,
				Constant.BEARER + token.getAccessToken());
		log.info("Initialized apiClient with UserToken :{}", token.getAccessToken());
		EnvelopeSummary results = null;
		EnvelopeDefinition envelope = new EnvelopeDefinition();
		Notification notification = new Notification();
		Reminders reminders = new Reminders();
		reminders.setReminderDelay(signatureModel.getReminders().getReminderDelay());
		reminders.setReminderFrequency(signatureModel.getReminders().getReminderFrequency());
		reminders.setReminderEnabled(String.valueOf(signatureModel.getReminders().getReminderEnabled()));
		notification.setReminders(reminders);
		notification.setUseAccountDefaults(Constant.FALSE);
		Expirations expirations = new Expirations();
		expirations.setExpireAfter(signatureModel.getExpirations().getExpireAfter());
		expirations.setExpireEnabled(String.valueOf(signatureModel.getExpirations().getExpireEnabled()));
		expirations.setExpireWarn(signatureModel.getExpirations().getExpireWarn());
		notification.setExpirations(expirations);
		envelope.setNotification(notification);
		envelope.setEmailSubject(signatureModel.getEmailSubject());
		envelope.setEmailBlurb(signatureModel.getEmailMessage());
		envelope.setStatus(signatureModel.getStatus());
		List<Document> createDocuments = new ArrayList<>();
		TemplateDocumentsResult envelopeDocuments = null;
		if (signatureModel.getDocuments() != null) {
			for (com.saaspe.docusign.model.Document document : signatureModel.getDocuments()) {
				SignHere signHere = new SignHere();
				signHere.setPageNumber("1");
				signHere.setXPosition("191");
				signHere.setYPosition("148");
				Tabs tabs = new Tabs();
				tabs.setSignHereTabs(Arrays.asList(signHere));
				if (document.getCategory().equalsIgnoreCase(Constant.CREATE_FILE)) {
					Document createDocument = new Document();
					createDocument.setName(document.getName());
					createDocument.setDocumentBase64(document.getDocumentBase64());
					createDocument.setDocumentId(document.getDocumentId());
					createDocument.setTabs(tabs);
					createDocuments.add(createDocument);
				} else if (document.getCategory().equalsIgnoreCase(Constant.DELETE)) {
					envelopeDocuments.getTemplateDocuments()
							.removeIf(p -> p.getDocumentId().equalsIgnoreCase(document.getDocumentId()));
				} else if (document.getCategory().equalsIgnoreCase(Constant.UPDATE_FILE)) {
					Document updateDocument = new Document();
					updateDocument.setName(document.getName());
					updateDocument.setDocumentBase64(document.getDocumentBase64());
					updateDocument.setDocumentId(document.getDocumentId());
					updateDocument.setTabs(tabs);
					envelopeDocuments.getTemplateDocuments()
							.removeIf(p -> p.getDocumentId().equalsIgnoreCase(document.getDocumentId()));
					createDocuments.add(updateDocument);
				}
			}
		}
		int i = 1;
		SignHere signHere = new SignHere();
		signHere.setPageNumber("1");
		signHere.setXPosition("191");
		signHere.setYPosition("148");
		Tabs tabs = new Tabs();
		tabs.setSignHereTabs(Arrays.asList(signHere));
		List<Signer> signerList = new ArrayList<>();
//		int min = 0;
		for (AllSigners signer : signatureModel.getAllSigners()) {
			i++;
			if (signer.getRecipientType().equalsIgnoreCase("LHDN_Stamper")
					|| signer.getRecipientType().equalsIgnoreCase("Vendor")) {
				Signer finalSigner = new Signer();
				finalSigner.setEmail(signer.getEmail());
				finalSigner.setName(signer.getDisplayName());
				finalSigner.setRecipientId(String.valueOf(i));
				finalSigner.setRoutingOrder(signer.getRoutingOrder());
				RecipientEmailNotification ren = new RecipientEmailNotification();
				ren.setEmailSubject(signatureModel.getEmailSubject());
				String vendorMessage = null;
//				if (signer.getRecipientType().equalsIgnoreCase("LHDN_Stamper")
//						&& (min == 0 || Integer.parseInt(signer.getRoutingOrder()) < min))
//					min = Integer.parseInt(signer.getRoutingOrder());
				switch (signatureModel.getModuleName() != null ? signatureModel.getModuleName() : StringUtils.EMPTY) {
				case Constant.LOA_DOCUMENT_REPOSITORY:
					vendorMessage = Constant.vendorMessage;
					vendorMessage = vendorMessage.replace("{moduleName}", "LOA")
							.replace("{abbreviation}", "Letter Of Award")
							.replace("{contractName}", signatureModel.getContractName())
							.replace("{contractNumber}", signatureModel.getContractNumber());

					if (signer.getRecipientType().equalsIgnoreCase("LHDN_Stamper")) {
						vendorMessage = vendorMessage.replace("{moduleName2}", "LHDN").replace("{videoURL1}",
								"https://drive.google.com/file/d/1r9i-aQNFaW2u5bWHjsk50PfguGg7MBIN/view?usp=drive_link")
								.replace("{videoURL2}",
										"https://drive.google.com/file/d/1e8l5qdNRgys0zJAL_xVDLY7liEEW5d2G/view?usp=drive_link");
					} else {
						vendorMessage = vendorMessage.replace("{moduleName2}", "LOA").replace("{videoURL1}",
								"https://drive.google.com/file/d/1pgv99GynRRAFg2hT07WTZZfdBb2V-Xd1/view?usp=drive_link")
								.replace("{videoURL2}",
										"https://drive.google.com/file/d/1zEdT4L6d39vw0jn7LA6b9X3EsjF4rC1u/view?usp=drive_link");
					}
					break;
				case Constant.TA_DOCUMENT_REPOSITORY:
					vendorMessage = Constant.lhdnMessage;
					vendorMessage = vendorMessage.replace("{moduleName}", "LHDN").replace("{videoURL1}",
							"https://drive.google.com/file/d/1r9i-aQNFaW2u5bWHjsk50PfguGg7MBIN/view?usp=drive_link")
							.replace("{videoURL2}",
									"https://drive.google.com/file/d/1e8l5qdNRgys0zJAL_xVDLY7liEEW5d2G/view?usp=drive_link");
				}
//				if (signer.getRecipientType().equalsIgnoreCase("LHDN_Stamper")
//						&& Integer.parseInt(signer.getRoutingOrder()) <= min)
//					vendorMessage = Constant.lhdnMessage;

				ren.setEmailBody(vendorMessage);
				finalSigner.setEmailNotification(ren);
				signerList.add(finalSigner);
			} else if (signer.getRecipientType().equalsIgnoreCase("CVEF_Signer")) {
				Signer finalSigner = new Signer();
				finalSigner.setEmail(signer.getEmail());
				finalSigner.setName(signer.getDisplayName());
				finalSigner.setRecipientId(String.valueOf(i));
				finalSigner.setRoutingOrder(signer.getRoutingOrder());

				RecipientEmailNotification ren = new RecipientEmailNotification();
				ren.setEmailSubject(signatureModel.getEmailSubject());
				String cvefMessage = null;

				switch (signatureModel.getModuleName() != null ? signatureModel.getModuleName() : StringUtils.EMPTY) {
				case Constant.TA_DOCUMENT_REPOSITORY:
					cvefMessage = Constant.cvefMessage;
					cvefMessage = cvefMessage.replace("{moduleName}", "TA")
							.replace("{contractName}", signatureModel.getContractName())
							.replace("{videoURL1}",
									"https://drive.google.com/file/d/1fKEsmWmESZvR1M__hD4AQgGY3KM16Nmi/view?usp=drive_link")
							.replace("{videoURL2}",
									"https://drive.google.com/file/d/1-vP6CglHEIlk-kAxgeYf9P0Ea11TB7DI/view?usp=drive_link");
					break;
				}
				ren.setEmailBody(cvefMessage);
				finalSigner.setEmailNotification(ren);
				signerList.add(finalSigner);
			} else if (signer.getRecipientType().equalsIgnoreCase("Stamper")) {
				Signer finalSigner = new Signer();
				finalSigner.setEmail(signer.getEmail());
				finalSigner.setName(signer.getDisplayName());
				finalSigner.setRecipientId(String.valueOf(i));
				finalSigner.setRoutingOrder(signer.getRoutingOrder());
				RecipientEmailNotification ren = new RecipientEmailNotification();
				ren.setEmailSubject(signatureModel.getEmailSubject());
				String stamperMessage = null;
				switch (signatureModel.getModuleName() != null ? signatureModel.getModuleName() : StringUtils.EMPTY) {
				case Constant.TA_DOCUMENT_REPOSITORY:
					stamperMessage = Constant.stamperMessage;
					stamperMessage = stamperMessage.replace("{moduleName}", "TA")
							.replace("{contractName}", signatureModel.getContractName())
							.replace("{videoURL1}",
									"https://drive.google.com/file/d/1fKEsmWmESZvR1M__hD4AQgGY3KM16Nmi/view?usp=drive_link")
							.replace("{videoURL2}",
									"https://drive.google.com/file/d/1-vP6CglHEIlk-kAxgeYf9P0Ea11TB7DI/view?usp=drive_link");
					break;
				}
				ren.setEmailBody(stamperMessage);
				finalSigner.setEmailNotification(ren);
				signerList.add(finalSigner);
			} else if (signer.getRecipientType().equalsIgnoreCase("Tenant")) {
				Signer finalSigner = new Signer();
				finalSigner.setEmail(signer.getEmail());
				finalSigner.setName(signer.getDisplayName());
				finalSigner.setRecipientId(String.valueOf(i));
				finalSigner.setRoutingOrder(signer.getRoutingOrder());
				RecipientEmailNotification ren = new RecipientEmailNotification();
				ren.setEmailSubject(signatureModel.getEmailSubject());
				String tenantMessage = null;
				switch (signatureModel.getModuleName() != null ? signatureModel.getModuleName() : StringUtils.EMPTY) {
				case Constant.LOO_CONTRACT_DOCUMENT_REPOSITORY:
					tenantMessage = Constant.tenantMessage;
					tenantMessage = tenantMessage.replace("{moduleName}", "LOO")
							.replace("{contractName}", signatureModel.getContractName())
							.replace("{videoURL1}",
									"https://drive.google.com/drive/folders/17yKsXG0bd4ROamo3woV3fQ4mfI8wi6ah")
							.replace("{videoURL2}",
									"https://drive.google.com/drive/folders/1nl1wEzmwionn6yomHKdMGZ0P8kgEekdr");
					break;
				case Constant.TA_DOCUMENT_REPOSITORY:
					tenantMessage = Constant.tenantMessage;
					tenantMessage = tenantMessage.replace("{moduleName}", "TA")
							.replace("{contractName}", signatureModel.getContractName())
							.replace("{videoURL1}",
									"https://drive.google.com/file/d/1fKEsmWmESZvR1M__hD4AQgGY3KM16Nmi/view?usp=drive_link")
							.replace("{videoURL2}",
									"https://drive.google.com/file/d/1-vP6CglHEIlk-kAxgeYf9P0Ea11TB7DI/view?usp=drive_link");
					break;
				}
				ren.setEmailBody(tenantMessage);
				finalSigner.setEmailNotification(ren);
				signerList.add(finalSigner);
			} else {
				Signer finalSigner = new Signer();
				finalSigner.setEmail(signer.getEmail());
				finalSigner.setName(signer.getDisplayName());
				finalSigner.setRecipientId(String.valueOf(i));
				finalSigner.setRoutingOrder(signer.getRoutingOrder());
				signerList.add(finalSigner);
			}
		}
		List<CarbonCopy> carbonCopyList = new ArrayList<>();
		for (com.saaspe.docusign.model.Carboncopy cc : signatureModel.getRecipients().getCc()) {
			i++;
			CarbonCopy ccFinal = new CarbonCopy();
			ccFinal.setEmail(cc.getEmail());
			ccFinal.setName(cc.getName());
			ccFinal.setRecipientId(String.valueOf(i));
			ccFinal.setRoutingOrder("1");
			carbonCopyList.add(ccFinal);
		}
		Recipients recipients = new Recipients();
		Signer signer = new Signer();
		signer.setTabs(tabs);
		recipients.setSigners(signerList);
		recipients.setCarbonCopies(carbonCopyList);
		envelope.setDocuments(createDocuments);
		envelope.setRecipients(recipients);
		envelope.setAllowComments(signatureModel.getAllowComments());
		envelope.recipientsLock(String.valueOf(signatureModel.getRecipientLock()));
		envelope.messageLock(String.valueOf(signatureModel.getMessageLock()));
		envelope.setEnforceSignerVisibility(String.valueOf(signatureModel.getEnforceSignerVisibility()));
		envelope.setSignerCanSignOnMobile(String.valueOf(signatureModel.getSignerCanSignOnMobile()));
		EnvelopesApi envelopesApi = new EnvelopesApi(apiclient);
		log.info("Calling createEnvelope API");
		results = envelopesApi.createEnvelope(accountId, envelope);
		log.info("Success in createEnvelope API");
		return ResponseEntity.ok().body(results);
	}

	public String getEnvelopeDocument(String envelopeId, String documentId) throws ApiException {

		byte[] results = null;
		String base64EncodedContent = null;
		try {
			OAuth.UserInfo userInfo = apiClient.getUserInfo(getAccessToken());
			String accountsId = userInfo.getAccounts().get(0).getAccountId();
			apiClient.addDefaultHeader(Constant.AUTHORIZATION, Constant.BEARER + getAccessToken());
			apiClient.addDefaultHeader("Content-Transfer-Encoding", "base64");
			EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
			EnvelopesApi.GetDocumentOptions options = envelopesApi.new GetDocumentOptions();
			options.setEncoding("base64");
			results = envelopesApi.getDocument(accountsId, envelopeId, documentId);
			base64EncodedContent = Base64.getEncoder().encodeToString(results);
		} catch (ApiException exp) {
			if (exp.getMessage().contains(Constant.CONSENT_REQUIRED)) {
				try {
					log.info(Constant.CONSENT_REQUIRED_PROVIDE_CONSENT);
					Desktop.getDesktop().browse(
							new URI(docusignHost + docusignOAuthPath + clientId + Constant.REDIRECT_URI + redirectUrl));
				} catch (Exception e) {
					log.info(Constant.ISSUE_IN_GETTING_CONSENT);
				}
			}
		} catch (Exception e) {
			log.info(Constant.ISSUE_IN_GETTING_CONSENT);

		}
		return base64EncodedContent;
	}

	public Envelope getIndividualEnvelopeStatus(String envelopeId) throws java.io.IOException {
		Envelope results = null;
		try {
			OAuth.UserInfo userInfo = apiClient.getUserInfo(getAccessToken());
			String accountsId = userInfo.getAccounts().get(0).getAccountId();
			apiClient.addDefaultHeader(Constant.AUTHORIZATION, Constant.BEARER + getAccessToken());
			EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
			GetEnvelopeOptions options = envelopesApi.new GetEnvelopeOptions();
			options.setInclude("custom_fields,documents,recipients,workflow,tabs");
			results = envelopesApi.getEnvelope(accountsId, envelopeId, options);
		} catch (ApiException exp) {
			if (exp.getMessage().contains(Constant.CONSENT_REQUIRED)) {
				try {
					log.info(Constant.CONSENT_REQUIRED_PROVIDE_CONSENT);
					Desktop.getDesktop().browse(
							new URI(docusignHost + docusignOAuthPath + clientId + Constant.REDIRECT_URI + redirectUrl));
				} catch (Exception e) {
					log.info(Constant.ISSUE_IN_GETTING_CONSENT);
				}
			}
		} catch (Exception e) {
			log.info(Constant.ISSUE_IN_GETTING_CONSENT);
		}
		return results;
	}

	public EnvelopesInformation getListOfEnvelopes(String fromDate, String toDate, String startPosition, String count)
			throws ApiException {
		EnvelopesInformation results = null;
		try {
			OAuth.UserInfo userInfo = apiClient.getUserInfo(getAccessToken());
			String accountsId = userInfo.getAccounts().get(0).getAccountId();
			apiClient.addDefaultHeader(Constant.AUTHORIZATION, Constant.BEARER + getAccessToken());
			EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
			EnvelopesApi.ListStatusChangesOptions options = envelopesApi.new ListStatusChangesOptions();
			options.setFromDate(fromDate);
			options.setToDate(toDate);
			options.setStartPosition(startPosition);
			options.setCount(count);
			results = envelopesApi.listStatusChanges(accountsId, options);
		} catch (ApiException exp) {
			if (exp.getMessage().contains(Constant.CONSENT_REQUIRED)) {
				try {
					log.info(Constant.CONSENT_REQUIRED_PROVIDE_CONSENT);
					Desktop.getDesktop().browse(
							new URI(docusignHost + docusignOAuthPath + clientId + Constant.REDIRECT_URI + redirectUrl));
				} catch (Exception e) {
					log.info(Constant.ISSUE_IN_GETTING_CONSENT);
				}
			}
		} catch (Exception e) {
			log.info(Constant.ISSUE_IN_GETTING_CONSENT);
		}
		return results;
	}

	public EnvelopeDocumentsResult getEnvelopeDocumentDetails(String envelopeId) throws ApiException {
		EnvelopeDocumentsResult results = null;
		try {
			OAuth.UserInfo userInfo = apiClient.getUserInfo(getAccessToken());
			String accountsId = userInfo.getAccounts().get(0).getAccountId();
			apiClient.addDefaultHeader(Constant.AUTHORIZATION, Constant.BEARER + getAccessToken());
			EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
			results = envelopesApi.listDocuments(accountsId, envelopeId);
		} catch (ApiException exp) {
			if (exp.getMessage().contains(Constant.CONSENT_REQUIRED)) {
				try {
					log.info(Constant.CONSENT_REQUIRED_PROVIDE_CONSENT);
					Desktop.getDesktop().browse(
							new URI(docusignHost + docusignOAuthPath + clientId + Constant.REDIRECT_URI + redirectUrl));
				} catch (Exception e) {
					log.info(Constant.ISSUE_IN_GETTING_CONSENT);
				}
			}
		} catch (Exception e) {
			log.info(Constant.ISSUE_IN_GETTING_CONSENT);
		}
		return results;
	}

	public Recipients listEnvelopeRecipients(String envelopeId) throws ApiException {
		Recipients results = null;
		try {
			OAuth.UserInfo userInfo = apiClient.getUserInfo(getAccessToken());
			String accountsId = userInfo.getAccounts().get(0).getAccountId();
			apiClient.addDefaultHeader(Constant.AUTHORIZATION, Constant.BEARER + getAccessToken());
			EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
			results = envelopesApi.listRecipients(accountsId, envelopeId);
		} catch (ApiException exp) {
			if (exp.getMessage().contains(Constant.CONSENT_REQUIRED)) {
				try {
					log.info(Constant.CONSENT_REQUIRED_PROVIDE_CONSENT);
					Desktop.getDesktop().browse(
							new URI(docusignHost + docusignOAuthPath + clientId + Constant.REDIRECT_URI + redirectUrl));
				} catch (Exception e) {
					log.info(Constant.ISSUE_IN_GETTING_CONSENT);
				}
			}
		} catch (Exception e) {
			log.info(Constant.ISSUE_IN_GETTING_CONSENT);
		}
		return results;
	}

	public Object listTemplates(String count, String start, String order, String orderBy, String searchText,
			String folderId) {
		Object results = null;
		try {
			OAuth.UserInfo userInfo = apiClient.getUserInfo(getAccessToken());
			String accountsId = userInfo.getAccounts().get(0).getAccountId();
			apiClient.addDefaultHeader(Constant.AUTHORIZATION, Constant.BEARER + getAccessToken());
			TemplatesApi templatesApi = new TemplatesApi(apiClient);
			com.docusign.esign.api.TemplatesApi.ListTemplatesOptions options = new TemplatesApi().new ListTemplatesOptions();
			options.setCount(count);
			options.setStartPosition(start);
			options.setOrder(order);
			options.setOrderBy(orderBy);
			if (searchText != null) {
				options.setSearchText(searchText);
			}
			options.setFolderIds(folderId);
			results = templatesApi.listTemplates(accountsId, options);
		} catch (ApiException exp) {
			exp.printStackTrace();
			log.info(Constant.CONSENT_REQUIRED);
			if (exp.getMessage().contains(Constant.CONSENT_REQUIRED)) {
				try {
					log.info(Constant.CONSENT_REQUIRED_PROVIDE_CONSENT);
					Desktop.getDesktop().browse(
							new URI(docusignHost + docusignOAuthPath + clientId + Constant.REDIRECT_URI + redirectUrl));
				} catch (Exception e) {
					log.info(Constant.ISSUE_IN_GETTING_CONSENT);
				}
			}
		} catch (Exception e) {
			log.info(Constant.ISSUE_IN_GETTING_CONSENT);
		}
		return results;
	}

	public TemplateGetByIdResponse listTemplateById(String templateId) {
		TemplateGetByIdResponse response = new TemplateGetByIdResponse();
		try {
			OAuth.UserInfo userInfo = apiClient.getUserInfo(getAccessToken());
			String accountsId = userInfo.getAccounts().get(0).getAccountId();
			apiClient.addDefaultHeader(Constant.AUTHORIZATION, Constant.BEARER + getAccessToken());
			TemplatesApi templatesApi = new TemplatesApi(apiClient);
			GetOptions templateOptions = templatesApi.new GetOptions();
			templateOptions.setInclude("documents");
			EnvelopeTemplate template = templatesApi.get(accountsId, templateId, templateOptions);
			response.setData(template);
		} catch (ApiException exp) {
			exp.printStackTrace();
			if (exp.getMessage().contains(Constant.CONSENT_REQUIRED)) {
				try {
					log.info(Constant.CONSENT_REQUIRED_PROVIDE_CONSENT);
					Desktop.getDesktop().browse(
							new URI(docusignHost + docusignOAuthPath + clientId + Constant.REDIRECT_URI + redirectUrl));
				} catch (Exception e) {
					log.info(Constant.ISSUE_IN_GETTING_CONSENT);
				}
			}
		} catch (Exception e) {
			log.info(Constant.ISSUE_IN_GETTING_CONSENT);
		}
		return response;
	}

	public AuditEvent getAudit(String envelopeId) throws ApiException, IOException {
		AuditEvent auditEvent = new AuditEvent();
		OAuth.UserInfo userInfo = apiClient.getUserInfo(getAccessToken());
		String accountsId = userInfo.getAccounts().get(0).getAccountId();
		apiClient.addDefaultHeader(Constant.AUTHORIZATION, Constant.BEARER + getAccessToken());
		EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
		EnvelopeAuditEventResponse auditEventResponse = envelopesApi.listAuditEvents(accountsId, envelopeId);
		List<EnvelopeAuditEvent> auditEvents = auditEventResponse.getAuditEvents();
		List<EventFieldsList> listOfEventFields = new ArrayList<>();
		for (EnvelopeAuditEvent event : auditEvents) {
			List<NameValue> nameValues = event.getEventFields();
			EventFieldsList eventFieldsList = new EventFieldsList();
			List<EventField> eventFieldList = new ArrayList<>();
			for (NameValue value : nameValues) {
				EventField eventField = new EventField();
				eventField.setErrorDetails(value.getErrorDetails());
				eventField.setName(value.getName());
				eventField.setOriginalValue(value.getOriginalValue());
				eventField.setValue(value.getValue());
				eventFieldList.add(eventField);
			}
			eventFieldsList.setEventFields(eventFieldList);
			listOfEventFields.add(eventFieldsList);
		}
		auditEvent.setAuditEvents(listOfEventFields);
		return auditEvent;
	}

	public TemplateSummary createTemplate(CreateTemplate createTemplate, String email, String folderId, String userId)
			throws IllegalArgumentException, IOException, ApiException, com.docusign.admin.client.ApiException {

		OAuth.UserInfo userInfo = apiClient.getUserInfo(getAccessToken());
		String accountsId = userInfo.getAccounts().get(0).getAccountId();
		TemplatesApi template = new TemplatesApi(apiClient);
		EnvelopeTemplate envelopeTemplate = new EnvelopeTemplate();
		envelopeTemplate.setName(createTemplate.getTemplateName());
		envelopeTemplate.setDescription(createTemplate.getTemplateDescritpion());
		envelopeTemplate.setEmailSubject(createTemplate.getEmailSubject());
		envelopeTemplate.setEmailBlurb(createTemplate.getEmailMessage());
		envelopeTemplate.setSignerCanSignOnMobile(String.valueOf(createTemplate.getSignerCanSignONMobile()));
		generateUserToken(email);
		apiClient.addDefaultHeader(Constant.AUTHORIZATION,
				Constant.BEARER + generateUserTokenByUserId(email, userId).getAccessToken());
		log.info("Initialized apiClient with userToken :{}", generateUserTokenByUserId(email, userId).getAccessToken());
		envelopeTemplate.setFolderId(folderId);
		// Setting signers
		List<Signer> signers = new ArrayList<>();
		int i = 1;
		for (SignerRequest signer : createTemplate.getSigners()) {
			Signer signer1 = new Signer();
			signer1.setEmail(signer.getEmail());
			signer1.setName(signer.getName());
			signer1.setRecipientId(String.valueOf(i));
			signer1.setRecipientType(signer.getRecipientType());
			if (createTemplate != null && Boolean.TRUE.equals(createTemplate.getSigningOrder())) {
				signer1.setRoutingOrder(signer.getRoutingOrder());
			}
			signer1.setRoleName(signer.getRecipientRole());
			signer1.setCanSignOffline(String.valueOf(signer.getCanSignOffline()));
			i++;
			signers.add(signer1);
		}
		// Add the recipients to the template
		Recipients recipients = new Recipients();
		recipients.setSigners(signers);
		envelopeTemplate.setRecipients(recipients);
		// TemplateModification
		envelopeTemplate.recipientsLock(String.valueOf(createTemplate.getRecipientLock()));
		envelopeTemplate.messageLock(String.valueOf(createTemplate.getMessageLock()));
		// create document object
		List<Document> createDocument = new ArrayList<>();
		// create Document
		log.info("Listing Documents to create");
		for (DocumentRequest old : createTemplate.getDocuments()) {
			if (old.getCategory().equalsIgnoreCase("create")) {
				Document document1 = new Document();
				document1.setName(old.getName());
				document1.setDocumentBase64(old.getDocumentBase64());
				document1.setDocumentId(old.getDocumentId());
				SignHere signHere = new SignHere();
				signHere.setPageNumber("1");
				signHere.setXPosition("191");
				signHere.setYPosition("148");
				Tabs tabs = new Tabs();
				tabs.setSignHereTabs(Arrays.asList(signHere));
				document1.setTabs(tabs);
				createDocument.add(document1);
			}
		}
		List<Document> singleDocument = new ArrayList<>();
		singleDocument.add(createDocument.get(0));
		envelopeTemplate.setDocuments(singleDocument);
		// Reminder set and Expiration set
		Notification notification = new Notification();
		// reminders
		Reminders reminders = new Reminders();
		reminders.setReminderDelay(createTemplate.getReminders().getReminderDelay());
		reminders.setReminderFrequency(createTemplate.getReminders().getReminderFrequency());
		reminders.setReminderEnabled(String.valueOf(createTemplate.getReminders().getReminderEnabled()));
		notification.setReminders(reminders);
		// notifications
		Expirations expirations = new Expirations();
		expirations.setExpireAfter(createTemplate.getExpirations().getExpiryAfter());
		expirations.setExpireEnabled(String.valueOf(createTemplate.getExpirations().getExpiryEnabled()));
		expirations.setExpireWarn(createTemplate.getExpirations().getExpiryWarn());
		notification.setExpirations(expirations);
		envelopeTemplate.setNotification(notification);
		// comments
		envelopeTemplate.setAllowComments(String.valueOf(createTemplate.getAllowComments()));
		// template modification false
		envelopeTemplate.setEnforceSignerVisibility(String.valueOf(createTemplate.getEnforceSignerVisibility()));
		TemplateSummary templateResults = template.createTemplate(accountsId, envelopeTemplate);
		templateResults.getTemplateId();
		Iterator<Document> documentIterator = createDocument.iterator();
		while (documentIterator.hasNext()) {
			EnvelopeDefinition definition = new EnvelopeDefinition();
			definition.setDocuments(Collections.singletonList(documentIterator.next()));
			template.updateDocuments(accountsId, templateResults.getTemplateId(), definition);
			documentIterator.remove();
		}
		return templateResults;
	}

	@Data
	public static class CreateTemplate {
		private String templateName;
		private String templateDescritpion;
		private String emailSubject;
		private String emailMessage;
		private Boolean signerCanSignONMobile;
		private List<SignerRequest> signers;
		private List<DocumentRequest> documents;
		private RemainderRequest reminders;
		private ExpirationRequest expirations;
		private Boolean allowComments;
		private Boolean enforceSignerVisibility;
		private Boolean recipientLock;
		private Boolean messageLock;
		private Boolean signingOrder;
	}

	@Data
	public static class SignerRequest {
		private String name;
		private String email;
		private String recipientType;
		private String recipientRole;
		private Boolean canSignOffline;
		private String routingOrder;

	}

	@Data
	public static class DocumentRequest {
		private String name;
		private String documentId;
		private String documentBase64;
		private String category;
	}

	@Data
	public static class RemainderRequest {
		private Boolean reminderEnabled;
		private String reminderDelay;
		private String reminderFrequency;
	}

	@Data
	public static class ExpirationRequest {
		private Boolean expiryEnabled;
		private String expiryAfter;
		private String expiryWarn;
	}

	public Object updateTemplate(String templateId, CreateTemplate createTemplate)
			throws IOException, IllegalArgumentException, ApiException {
		OAuth.UserInfo userInfo = apiClient.getUserInfo(getAccessToken());
		String accountsId = userInfo.getAccounts().get(0).getAccountId();
		apiClient.addDefaultHeader(Constant.AUTHORIZATION, Constant.BEARER + getAccessToken());
		TemplatesApi templatesApi = new TemplatesApi(apiClient);
		// Retrieve the template using the Templates API and the template ID
		EnvelopeTemplate oldTemplate = templatesApi.get(accountsId, templateId);
		if (oldTemplate == null) {
			return null;
		}
		oldTemplate.setName(createTemplate.getTemplateName());
		oldTemplate.setDescription(createTemplate.getTemplateDescritpion());
		oldTemplate.setEmailSubject(createTemplate.getEmailSubject());
		oldTemplate.setEmailBlurb(createTemplate.getEmailMessage());
		oldTemplate.setSignerCanSignOnMobile(String.valueOf(createTemplate.getSignerCanSignONMobile()));
		// delete existingsigners
		if (createTemplate.getSigners() != null) {
			List<Signer> existingSigners = null;
			if (oldTemplate.getRecipients() != null) {
				existingSigners = oldTemplate.getRecipients().getSigners();
			}
			if (existingSigners != null) {
				TemplateRecipients recipients = new TemplateRecipients();
				recipients.setSigners(existingSigners);
				templatesApi.deleteRecipients(accountsId, templateId, recipients);
			}
		}
		// Setting signers
		List<Signer> signers = new ArrayList<>();
		int i = 1;
		for (SignerRequest signer : createTemplate.getSigners()) {
			Signer signer1 = new Signer();
			signer1.setEmail(signer.getEmail());
			signer1.setName(signer.getName());
			signer1.setRecipientId(String.valueOf(i));
			signer1.setRecipientType(signer.getRecipientType());
			signer1.setRoutingOrder(signer.getRoutingOrder());
			signer1.setRoleName(signer.getRecipientRole());
			signer1.setCanSignOffline(String.valueOf(signer.getCanSignOffline()));
			i++;
			signers.add(signer1);
		}
		// Add the recipients to the template
		Recipients recipients = new Recipients();
		recipients.setSigners(signers);
		oldTemplate.setRecipients(recipients);
		// TemplateModification
		oldTemplate.recipientsLock(String.valueOf(createTemplate.getRecipientLock()));
		oldTemplate.messageLock(String.valueOf(createTemplate.getMessageLock()));
		// Reminder set and Expiration set
		Notification notification = new Notification();
		// reminders
		Reminders reminders = new Reminders();
		reminders.setReminderDelay(createTemplate.getReminders().getReminderDelay());
		reminders.setReminderFrequency(createTemplate.getReminders().getReminderFrequency());
		reminders.setReminderEnabled(String.valueOf(createTemplate.getReminders().getReminderEnabled()));
		notification.setReminders(reminders);
		// notifications
		Expirations expirations = new Expirations();
		expirations.setExpireAfter(createTemplate.getExpirations().getExpiryAfter());
		expirations.setExpireEnabled(String.valueOf(createTemplate.getExpirations().getExpiryEnabled()));
		expirations.setExpireWarn(createTemplate.getExpirations().getExpiryWarn());
		notification.setExpirations(expirations);
		oldTemplate.setNotification(notification);
		// comments
		oldTemplate.setAllowComments(String.valueOf(createTemplate.getAllowComments()));
		List<Document> updateDocument = new ArrayList<>();
		// AddingDocuments
		List<Document> deleteDocuments = new ArrayList<>();
		// Update Document
		for (DocumentRequest old : createTemplate.getDocuments()) {
			if (old.getCategory().equalsIgnoreCase("create")) {
				Document document1 = new Document();
				document1.setName(old.getName());
				document1.setDocumentBase64(old.getDocumentBase64());
				document1.setDocumentId(old.getDocumentId());
				SignHere signHere = new SignHere();
				signHere.setPageNumber("1");
				signHere.setXPosition("191");
				signHere.setYPosition("148");
				Tabs tabs = new Tabs();
				tabs.setSignHereTabs(Arrays.asList(signHere));
				document1.setTabs(tabs);
				updateDocument.add(document1);
			} else if (old.getCategory().equalsIgnoreCase("update")) {
				Document document2 = new Document();
				document2.setName(old.getName());
				document2.setDocumentBase64(old.getDocumentBase64());
				document2.setDocumentId(old.getDocumentId());
				updateDocument.add(document2);
			} else if (old.getCategory().equalsIgnoreCase(Constant.DELETE)) {
				log.info(old.getDocumentBase64());
				Document document3 = new Document();
				document3.setName(old.getName());
				document3.setDocumentBase64(old.getDocumentBase64());
				document3.setDocumentId(old.getDocumentId());
				deleteDocuments.add(document3);
			}
		}
		if (oldTemplate.getDocuments() != null) {
			for (Document old : oldTemplate.getDocuments()) {
				if (updateDocument.stream().filter(p -> p.getDocumentId().equalsIgnoreCase(old.getDocumentId()))
						.collect(Collectors.toList()).isEmpty()) {
					updateDocument.add(old);
				}
			}
		}
		try {
			if (!deleteDocuments.isEmpty()) {
				EnvelopeDefinition deleteDefinition = new EnvelopeDefinition();
				deleteDefinition.setDocuments(deleteDocuments);
				templatesApi.deleteDocuments(accountsId, templateId, deleteDefinition);
			}
			if (!updateDocument.isEmpty()) {
				EnvelopeDefinition updateDefinition = new EnvelopeDefinition();
				updateDefinition.setDocuments(updateDocument);
				templatesApi.updateDocuments(accountsId, templateId, updateDefinition);
			}
			return templatesApi.update(accountsId, templateId, oldTemplate);
		} catch (ApiException e) {
			log.info(e.getLocalizedMessage());
			return null;
		}
	}

	public EnvelopeResponse getEnvelopeById(String envelopeId)
			throws ApiException, IllegalArgumentException, IOException {
		EnvelopeResponse envelopeResponse = new EnvelopeResponse();
//		OAuth.UserInfo userInfo = apiClient.getUserInfo(getAccessToken());
//		String accountsId = userInfo.getAccounts().get(0).getAccountId();
		apiClient.addDefaultHeader(Constant.AUTHORIZATION, Constant.BEARER + getAuthTokenOfAdmin());
		EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
		GetEnvelopeOptions envelopeOptions = envelopesApi.new GetEnvelopeOptions();
		envelopeOptions.setInclude("custom_fields,documents,recipients,workflow,tabs");
		Envelope envelope = envelopesApi.getEnvelope(accountId, envelopeId, envelopeOptions);
		EnvelopeDocumentsResult envelopeDocument = envelopesApi.listDocuments(accountId, envelopeId);
		envelopeDocument.getEnvelopeDocuments();
		List<DocumentResponse> documentResponsesList = new ArrayList<>();
		envelopeResponse.setEnvelopeId(envelopeId);
		envelopeResponse.setEnvelope(envelope);
		envelopeResponse.setDocuments(documentResponsesList);
		return envelopeResponse;
	}

	public EnvelopesInformation getListOfEnvelopes(String envelopeId) {

		EnvelopesInformation results = null;
		try {
			OAuth.UserInfo userInfo = apiClient.getUserInfo(getAccessToken());
			String accountsId = userInfo.getAccounts().get(0).getAccountId();
			apiClient.addDefaultHeader(Constant.AUTHORIZATION, Constant.BEARER + getAccessToken());
			// List envelope Documents
			EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
			EnvelopesApi.ListStatusChangesOptions options = envelopesApi.new ListStatusChangesOptions();
			options.setEnvelopeIds(envelopeId);
			results = envelopesApi.listStatusChanges(accountsId, options);
		} catch (ApiException exp) {
			if (exp.getMessage().contains(Constant.CONSENT_REQUIRED)) {
				try {
					log.info(Constant.CONSENT_REQUIRED_PROVIDE_CONSENT);
					Desktop.getDesktop().browse(
							new URI(docusignHost + docusignOAuthPath + clientId + Constant.REDIRECT_URI + redirectUrl));
				} catch (Exception e) {
					log.info(Constant.ISSUE_IN_GETTING_CONSENT);
				}
			}
		} catch (Exception e) {
			log.info(Constant.ISSUE_IN_GETTING_CONSENT);
		}
		return results;
	}

	public byte[] getTemplateDocument(String templateId, String documentId) {
		byte[] results = null;
		try {
			OAuth.UserInfo userInfo = apiClient.getUserInfo(getAccessToken());
			String accountsId = userInfo.getAccounts().get(0).getAccountId();
			apiClient.addDefaultHeader(Constant.AUTHORIZATION, Constant.BEARER + getAccessToken());
			log.info("Initialized apiClient");
			TemplatesApi templatesApi = new TemplatesApi(apiClient);
			log.info("Getting template Documents");
			results = templatesApi.getDocument(accountsId, templateId, documentId);
			log.info("Success in getting template Documents");
		} catch (ApiException exp) {
			log.error("Inside catchBlock - templateDocument");
			if (exp.getMessage().contains(Constant.CONSENT_REQUIRED)) {
				try {
					log.info(Constant.CONSENT_REQUIRED_PROVIDE_CONSENT);
					Desktop.getDesktop().browse(
							new URI(docusignHost + docusignOAuthPath + clientId + Constant.REDIRECT_URI + redirectUrl));
				} catch (Exception e) {
					log.info(Constant.ISSUE_IN_GETTING_CONSENT);
				}
			}
		} catch (Exception e) {
			log.info(Constant.ISSUE_IN_GETTING_CONSENT);
		}
		return results;
	}

	public byte[] getEnvelopeDocuments(String envelopeId, String documentId) {
		byte[] results = null;
		try {
			OAuth.UserInfo userInfo = apiClient.getUserInfo(getAccessToken());
			String accountsId = userInfo.getAccounts().get(0).getAccountId();
			apiClient.addDefaultHeader(Constant.AUTHORIZATION, Constant.BEARER + getAccessToken());
			EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
			results = envelopesApi.getDocument(accountsId, envelopeId, documentId);
		} catch (ApiException exp) {
			if (exp.getMessage().contains(Constant.CONSENT_REQUIRED)) {
				try {
					log.info(Constant.CONSENT_REQUIRED_PROVIDE_CONSENT);
					Desktop.getDesktop().browse(
							new URI(docusignHost + docusignOAuthPath + clientId + Constant.REDIRECT_URI + redirectUrl));
				} catch (Exception e) {
					log.info(Constant.ISSUE_IN_GETTING_CONSENT);
				}
			}
		} catch (Exception e) {
			log.info(Constant.ISSUE_IN_GETTING_CONSENT);
		}
		return results;
	}

	public GetUserTokenResponse getTest(String email, String userId)
			throws IllegalArgumentException, IOException, com.docusign.admin.client.ApiException {
		return generateUserTokenByUserId(email, userId);
	}

	public String getAuthTokenOfAdmin() {
		// ClassPathResource resource = new ClassPathResource("resources/" +
		// rsaKeyFile);
		String accessToken = null;
		try {
			ArrayList<String> scopes = new ArrayList<>();
			scopes.add(Constant.SIGNATURE);
			scopes.add(Constant.IMPERSONATION);
			scopes.add("user_write");
			scopes.add("user_read");
			scopes.add("organization_read");
			scopes.add("account_read");
			ApiClient apiclient = new ApiClient(basePath);
			apiclient.setOAuthBasePath(oAuthBasePath);
			log.info("Getting JWT userToken :{}");
			// byte[] privateKeyBytes =
			// resource.getClassLoader().getResource(rsaKeyFile).openStream().readAllBytes();
			// String file_path = directory + rsaKeyFile;
			Path filePath = Paths.get(rsaKeyFilePath, rsaKeyFile);
			byte[] fileBytes = Files.readAllBytes(filePath);
			com.docusign.esign.client.auth.OAuth.OAuthToken oAuthToken = apiclient.requestJWTUserToken(clientId, userId,
					scopes, fileBytes, 3600);
			accessToken = oAuthToken.getAccessToken();
			log.info("Success in getting JWT userToken :{}", accessToken);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return accessToken;
	}

	private GetUserTokenResponse generateUserTokenByUserId(String email, String useruserId)
			throws IOException, IllegalArgumentException, com.docusign.admin.client.ApiException {
		GetUserTokenResponse userData = new GetUserTokenResponse();
		log.info("userId: {}", useruserId);
		com.docusign.admin.client.ApiClient apiClient2 = new com.docusign.admin.client.ApiClient(
				basePath + "/management");
		// ClassPathResource resource = new ClassPathResource("resources/" +
		// rsaKeyFile);
		ArrayList<String> scopes = new ArrayList<>();
		scopes.add("signature");
		scopes.add("impersonation");
		scopes.add("user_write");
		scopes.add("user_read");
		scopes.add("organization_read");
		scopes.add("account_read");
		apiClient2.setOAuthBasePath(oAuthBasePath);
		Path filePath = Paths.get(rsaKeyFilePath, rsaKeyFile);
		byte[] fileBytes = Files.readAllBytes(filePath);
		// byte[] privateKeyBytes =
		// resource.getClassLoader().getResource(rsaKeyFile).openStream().readAllBytes();
		apiClient2.addDefaultHeader(Constant.HEADER_CONTENT_TYPE, Constant.JSON_CONTENT_TYPE);
		apiClient2.addDefaultHeader("Accept", Constant.JSON_CONTENT_TYPE);
		OAuthToken oAuthToken = apiClient2.requestJWTUserToken(clientId, userId, scopes, fileBytes, 3600);
		apiClient2.addDefaultHeader(Constant.AUTHORIZATION, Constant.BEARER + oAuthToken.getAccessToken());
		apiClient2.addDefaultHeader(Constant.HEADER_CONTENT_TYPE, Constant.JSON_CONTENT_TYPE);
		apiClient2.addDefaultHeader("Accept", Constant.JSON_CONTENT_TYPE);
		UsersApi usersApi = new UsersApi(apiClient2);
		List<UserDrilldownResponse> name = usersApi
				.getUserDSProfile(UUID.fromString(organizationId), UUID.fromString(useruserId)).getUsers();
		log.info("getUserProfileObject: {}", name);
		if (name.isEmpty())
			userData.setActivated(Constant.FALSE);
		else
			for (MembershipResponse response : name.get(0).getMemberships()) {
				if (response.getAccountId().equals(UUID.fromString(accountId))
						&& (!response.getStatus().equalsIgnoreCase("active"))) {
					userData.setActivated(Constant.FALSE);
				}
			}
		try {
			ArrayList<String> scope = new ArrayList<>();
			scope.add(Constant.SIGNATURE);
			scope.add(Constant.IMPERSONATION);
			OAuth.OAuthToken oauth = apiClient.requestJWTUserToken(clientId, useruserId, scope, fileBytes, 3600);
			userData.setAccessToken(oauth.getAccessToken());
		} catch (ApiException exp) {
			exp.printStackTrace();
			if (exp.getMessage().contains(Constant.CONSENT_REQUIRED)) {
				try {
					userData.setErrorMessage(Constant.CONSENT_REQUIRED);
					userData.setAuthEnpoint(
							docusignHost + docusignOAuthPath + clientId + Constant.REDIRECT_URI + redirectUrl);
					return userData;
				} catch (Exception e) {
					userData.setErrorMessage(e.getMessage());
					return userData;
				}
			} else if (exp.getMessage().contains("invalid_grant")) {
				try {
					userData.setErrorMessage("invalid_grant");
					return userData;
				} catch (Exception e) {
					userData.setErrorMessage(e.getMessage());
					return userData;
				}
			} else {
				try {
					userData.setErrorMessage(exp.getMessage());
					return userData;
				} catch (Exception e) {
					userData.setErrorMessage(e.getMessage());
					return userData;
				}
			}
		} catch (Exception e) {
			userData.setErrorMessage(e.getMessage());
			return userData;
		}
		return userData;
	}

	private GetUserTokenResponse generateUserTokenByUserId(String userId) {
		GetUserTokenResponse userData = new GetUserTokenResponse();
		try {
			ArrayList<String> scopes = new ArrayList<>();
			scopes.add(Constant.SIGNATURE);
			scopes.add(Constant.IMPERSONATION);
			Path filePath = Paths.get(rsaKeyFilePath, rsaKeyFile);
			byte[] fileBytes = Files.readAllBytes(filePath);
			// byte[] privateKeyBytes =
			// getClass().getClassLoader().getResourceAsStream(rsaKeyFile).readAllBytes();
			OAuth.OAuthToken oauth = apiClient.requestJWTUserToken(clientId, userId, scopes, fileBytes, 3600);
			log.info("Getting JWT userToken");
			userData.setAccessToken(oauth.getAccessToken());
			UserInfo userInfo = apiClient.getUserInfo(oauth.getAccessToken());
			log.info("Getting UserInfo :{}", userInfo.getName());
			for (Account account : userInfo.getAccounts()) {
				if (account.getOrganization().getOrganizationId() != null
						&& (account.getOrganization().getOrganizationId().equalsIgnoreCase(organizationId))) {
					userData.setAccountId(account.getAccountId());
				}
			}
		} catch (ApiException exp) {
			log.info("Inside catchBlock - generateUserTokenbyId");
			exp.printStackTrace();
			if (exp.getMessage().contains(Constant.CONSENT_REQUIRED)) {
				try {
					userData.setErrorMessage(Constant.CONSENT_REQUIRED);
					userData.setAuthEnpoint(
							docusignHost + docusignOAuthPath + clientId + Constant.REDIRECT_URI + redirectUrl);
					return userData;
				} catch (Exception e) {
					userData.setErrorMessage(e.getMessage());
					return userData;
				}
			}
		} catch (Exception e) {
			userData.setErrorMessage(e.getMessage());
			return userData;
		}
		return userData;
	}

	public byte[] getEnvelopeComment(String envelopeId, String email, String userId) {
		byte[] results = null;
		try {
			GetUserTokenResponse token = null;
			if (userId != null) {
				token = generateUserTokenByUserId(userId);
			} else {
				token = generateUserToken(email);
			}
			OAuth.UserInfo userInfo = apiClient.getUserInfo(token.getAccessToken());
			String accountsId = userInfo.getAccounts().get(0).getAccountId();
			apiClient.addDefaultHeader(Constant.AUTHORIZATION, Constant.BEARER + token.getAccessToken());
			log.info("Initialized apiClient");
			EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
			results = envelopesApi.getCommentsTranscript(accountsId, envelopeId);
		} catch (ApiException exp) {
			log.info("Inside catchBlock - getEnvelopeComments");
			if (exp.getMessage().contains(Constant.CONSENT_REQUIRED)) {
				try {
					Desktop.getDesktop().browse(
							new URI(docusignHost + docusignOAuthPath + clientId + Constant.REDIRECT_URI + redirectUrl));
				} catch (Exception e) {
					log.info(Constant.ISSUE_IN_GETTING_CONSENT);
				}
			}
		} catch (Exception e) {
			log.info(Constant.ISSUE_IN_GETTING_CONSENT);
		}
		return results;
	}

	public ResponseEntity<?> updateEnvelopeDocument(EsignaturePojo signatureModel, String envelopeId, String userId)
			throws IllegalArgumentException, ApiException, IOException, com.docusign.admin.client.ApiException {
		apiClient.addDefaultHeader(Constant.AUTHORIZATION,
				Constant.BEARER + generateUserTokenByUserId(null, userId).getAccessToken());
		EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
		EnvelopeDocumentsResult results = null;
		EnvelopeDefinition envelope = new EnvelopeDefinition();
		try {
			EnvelopeDocumentsResult envelopeDocuments = envelopesApi.listDocuments(accountId, envelopeId);
			envelopeDocuments.getEnvelopeDocuments()
					.removeIf(p -> p.getDocumentId().equalsIgnoreCase(Constant.CERTIFICATE));
			ResponseEntity<?> lockResponse = createLock(envelopeId, "1800");
			List<Document> createDocuments = new ArrayList<>();
			List<Document> deleteDocumentList = new ArrayList<>();
			for (com.saaspe.docusign.model.Document document : signatureModel.getDocuments()) {
				SignHere signHere = new SignHere();
				signHere.setPageNumber("1");
				signHere.setXPosition("191");
				signHere.setYPosition("148");
				Tabs tabs = new Tabs();
				tabs.setSignHereTabs(Arrays.asList(signHere));
				if (document.getCategory().equalsIgnoreCase(Constant.CREATE_FILE)) {
					Document createDocument = new Document();
					createDocument.setName(document.getName());
					createDocument.setDocumentBase64(document.getDocumentBase64());
					createDocument.setDocumentId(document.getDocumentId());
					createDocument.setTabs(tabs);
					createDocuments.add(createDocument);
				} else if (document.getCategory().equalsIgnoreCase(Constant.DELETE)) {
					Document deleteDocument = new Document();
					deleteDocument.setName(document.getName());
					deleteDocument.setDocumentId(document.getDocumentId());
					deleteDocumentList.add(deleteDocument);
					envelopeDocuments.getEnvelopeDocuments()
							.removeIf(p -> p.getDocumentId().equalsIgnoreCase(document.getDocumentId()));
				} else if (document.getCategory().equalsIgnoreCase(Constant.UPDATE_FILE)) {
					envelopeDocuments.getEnvelopeDocuments()
							.removeIf(p -> p.getDocumentId().equalsIgnoreCase(document.getDocumentId()));
					Document updateDocument = new Document();
					updateDocument.setName(document.getName());
					updateDocument.setDocumentBase64(document.getDocumentBase64());
					updateDocument.setDocumentId(document.getDocumentId());
					updateDocument.setTabs(tabs);
					createDocuments.add(updateDocument);
				}
			}
			for (EnvelopeDocument documentInfo : envelopeDocuments.getEnvelopeDocuments()) {
				byte[] documentBytes = envelopesApi.getDocument(accountId, envelopeId, documentInfo.getDocumentId());
				String base64Doc = Base64.getEncoder().encodeToString(documentBytes);
				Document document1 = new Document();
				document1.setName(documentInfo.getName());
				document1.setDocumentBase64(base64Doc);
				document1.setDocumentId(documentInfo.getDocumentId());
				createDocuments.add(document1);
			}
			envelope.setDocuments(createDocuments);
			String lockHeader = String.format(Constant.LOCK_HEADER, lockResponse.getBody().toString(), "1800");
			apiClient.addDefaultHeader(Constant.X_DOCUSIGN_EDIT, lockHeader);
			results = envelopesApi.updateDocuments(accountId, envelopeId, envelope);
			EnvelopeDefinition deleteEnvDefinition = new EnvelopeDefinition();
			deleteEnvDefinition.setDocuments(deleteDocumentList);
			log.info("Calling envelopeDocument deleteMethod");
			envelopesApi.deleteDocuments(accountId, envelopeId, deleteEnvDefinition);
			return ResponseEntity.ok().body(results);
		} finally {
			deleteLock(envelopeId);
		}
	}

	public ResponseEntity<?> updateEnvelopeDocumentStatus(String envelopeId, String userId, String status)
			throws IllegalArgumentException, ApiException, IOException, com.docusign.admin.client.ApiException {
		generateUserTokenByUserId(userId);
		apiClient.getUserInfo(generateUserTokenByUserId(null, userId).getAccessToken());
		apiClient.addDefaultHeader(Constant.AUTHORIZATION,
				Constant.BEARER + generateUserTokenByUserId(null, userId).getAccessToken());
		log.info("Initialized apiClient with UserToken");
		EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
		Envelope env = new Envelope();
		if (status.equalsIgnoreCase("voided")) {
			env.setStatus(status);
			env.setVoidedReason("Document Status Update");
		} else
			env.setStatus(status);
		log.info("Calling envelope update method");
		envelopesApi.update(accountId, envelopeId, env);
		log.info("Success in envelope update method");
		return ResponseEntity.ok().body("Envelope Status Updated Successfully");
	}

	public String getConsoleURL(String envelopeId, String returnUrl, String action) throws ApiException {
		try {
			ConsoleViewRequest viewRequest = new ConsoleViewRequest();
			viewRequest.setReturnUrl(returnUrl);
			if (envelopeId != null) {
				viewRequest.setEnvelopeId(envelopeId);
			}
			apiClient.addDefaultHeader(Constant.AUTHORIZATION, Constant.BEARER + getAuthTokenOfAdmin());
			log.info("Initialized apiClient");
			EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
			ViewUrl viewUrl = envelopesApi.createConsoleView(accountId, viewRequest);
			log.info("Calling createLock");
			ResponseEntity<?> lockResponse = createLock(envelopeId, "1500");
			log.info("Success in createLock");
			if (action != null && !action.isBlank())
				return viewUrl.getUrl()
						+ "&send=1&sendButtonAction=redirect&backButtonAction=redirect&showEditDocuments=true&showEditRecipients=false&showTabPalette=false&showMatchingTemplatesPrompt=false&lockToken="
						+ lockResponse.getBody();
			else
				return viewUrl.getUrl()
						+ "&send=1&sendButtonAction=redirect&backButtonAction=redirect&showEditDocumentVisibility=true&showMatchingTemplatesPrompt=false&lockToken="
						+ lockResponse.getBody();
		} catch (ApiException exp) {
			log.info("Inside catchBlock consoleView API");
			exp.printStackTrace();
			throw exp;
		}
	}

	public ResponseEntity<?> createNewContractDocument(EsignaturePojo signatureModel, String envelopeId,
			String senderUserId)
			throws IllegalArgumentException, IOException, com.docusign.admin.client.ApiException, ApiException {
		GetUserTokenResponse tokenResponse = generateUserTokenByUserId(senderUserId);
		apiClient.addDefaultHeader(Constant.AUTHORIZATION,
				Constant.BEARER + generateUserTokenByUserId(null, senderUserId).getAccessToken());
		log.info("Generating userToken : {}" + tokenResponse.getAccessToken());
		EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
		EnvelopeSummary results = null;
		EnvelopeDefinition envelope = new EnvelopeDefinition();
		// notificationPart
		Notification notification = new Notification();
		Reminders reminders = new Reminders();
		reminders.setReminderDelay(signatureModel.getReminders().getReminderDelay());
		reminders.setReminderFrequency(signatureModel.getReminders().getReminderFrequency());
		reminders.setReminderEnabled(String.valueOf(signatureModel.getReminders().getReminderEnabled()));
		notification.setReminders(reminders);
		notification.setUseAccountDefaults(Constant.FALSE);
		Expirations expirations = new Expirations();
		expirations.setExpireAfter(signatureModel.getExpirations().getExpireAfter());
		expirations.setExpireEnabled(String.valueOf(signatureModel.getExpirations().getExpireEnabled()));
		expirations.setExpireWarn(signatureModel.getExpirations().getExpireWarn());
		notification.setExpirations(expirations);
		envelope.setNotification(notification);
		envelope.setEmailSubject(signatureModel.getEmailSubject());
		envelope.setEmailBlurb(signatureModel.getEmailMessage());
		// creating client
		EnvelopeDocumentsResult envelopeDocuments = envelopesApi.listDocuments(accountId, envelopeId);
		envelopeDocuments.getEnvelopeDocuments()
				.removeIf(p -> p.getDocumentId().equalsIgnoreCase(Constant.CERTIFICATE));
		// adding Documents
		List<Document> createDocuments = new ArrayList<>();
		for (com.saaspe.docusign.model.Document document : signatureModel.getDocuments()) {
			SignHere signHere = new SignHere();
			signHere.setPageNumber("1");
			signHere.setXPosition("191");
			signHere.setYPosition("148");
			Tabs tabs = new Tabs();
			tabs.setSignHereTabs(Arrays.asList(signHere));
			if (document.getCategory().equalsIgnoreCase(Constant.CREATE_FILE)) {
				Document createDocument = new Document();
				createDocument.setName(document.getName());
				createDocument.setDocumentBase64(document.getDocumentBase64());
				createDocument.setDocumentId(document.getDocumentId());
				createDocument.setTabs(tabs);
				createDocuments.add(createDocument);
			} else if (document.getCategory().equalsIgnoreCase(Constant.DELETE)) {
				envelopeDocuments.getEnvelopeDocuments()
						.removeIf(p -> p.getDocumentId().equalsIgnoreCase(document.getDocumentId()));
			} else if (document.getCategory().equalsIgnoreCase(Constant.UPDATE_FILE)) {
				Document updateDocument = new Document();
				updateDocument.setName(document.getName());
				updateDocument.setDocumentBase64(document.getDocumentBase64());
				updateDocument.setDocumentId(document.getDocumentId());
				updateDocument.setTabs(tabs);
				envelopeDocuments.getEnvelopeDocuments()
						.removeIf(p -> p.getDocumentId().equalsIgnoreCase(document.getDocumentId()));
				createDocuments.add(updateDocument);
			}
		}
		for (EnvelopeDocument documentInfo : envelopeDocuments.getEnvelopeDocuments()) {
			byte[] documentBytes = envelopesApi.getDocument(accountId, envelopeId, documentInfo.getDocumentId());
			String base64Doc = Base64.getEncoder().encodeToString(documentBytes);
			Document document1 = new Document();
			document1.setName(documentInfo.getName());
			document1.setDocumentBase64(base64Doc);
			document1.setDocumentId(documentInfo.getDocumentId());
			createDocuments.add(document1);
		}
		int i = 1;
		SignHere signHere = new SignHere();
		signHere.setPageNumber("1");
		signHere.setXPosition("191");
		signHere.setYPosition("148");
		Tabs tabs = new Tabs();
		tabs.setSignHereTabs(Arrays.asList(signHere));
		List<Signer> signerList = new ArrayList<>();
		for (AllSigners signer : signatureModel.getAllSigners()) {
			i++;
			if (signer.getRecipientType().equalsIgnoreCase("LHDN_Stamper")
					|| signer.getRecipientType().equalsIgnoreCase("Vendor")) {
				Signer finalSigner = new Signer();
				finalSigner.setEmail(signer.getEmail());
				finalSigner.setName(signer.getDisplayName());
				finalSigner.setRecipientId(String.valueOf(i));
				finalSigner.setRoutingOrder(signer.getRoutingOrder());
				RecipientEmailNotification ren = new RecipientEmailNotification();
				ren.setEmailSubject(signatureModel.getEmailSubject());
				String vendorMessage = Constant.vendorMessage;
				switch (signatureModel.getModuleName() != null ? signatureModel.getModuleName() : "") {
				case Constant.LOA_CONTRACT_DOCUMENT_REPOSITORY:
					vendorMessage = vendorMessage.replace("{moduleName}", "contract")
							.replace("{moduleName2}", "CONTRACT").replace("{abbreviation}", "contract")
							.replace("{contractName}", signatureModel.getContractName())
							.replace("{contractNumber}", signatureModel.getContractNumber())
							.replace("{videoURL1}",
									"https://drive.google.com/file/d/1MXHA8gIFt63XAjPKXITaslcUvExkcBWu/view?usp=drive_link")
							.replace("{videoURL2}",
									"https://drive.google.com/file/d/1-YnHokKW7yx7aBkE87YFvfLHUxh_oFnN/view?usp=drive_link");
					break;
				}
				ren.setEmailBody(vendorMessage);
				finalSigner.setEmailNotification(ren);
				signerList.add(finalSigner);
			} else {
				Signer finalSigner = new Signer();
				finalSigner.setEmail(signer.getEmail());
				finalSigner.setName(signer.getDisplayName());
				finalSigner.setRecipientId(String.valueOf(i));
				finalSigner.setRoutingOrder(signer.getRoutingOrder());
				signerList.add(finalSigner);
			}
		}
		List<CarbonCopy> carbonCopyList = new ArrayList<>();
		for (com.saaspe.docusign.model.Carboncopy cc : signatureModel.getRecipients().getCc()) {
			i++;
			CarbonCopy ccFinal = new CarbonCopy();
			ccFinal.setEmail(cc.getEmail());
			ccFinal.setName(cc.getName());
			ccFinal.setRecipientId(String.valueOf(i));
			ccFinal.setRoutingOrder(cc.getRoutingOrder());
			carbonCopyList.add(ccFinal);
		}
		Recipients recipients = new Recipients();
		Signer signer = new Signer();
		signer.setTabs(tabs);
		recipients.setSigners(signerList);
		recipients.setCarbonCopies(carbonCopyList);
		envelope.setRecipients(recipients);
		envelope.setDocuments(createDocuments);
		envelope.setAllowComments(signatureModel.getAllowComments());
		envelope.recipientsLock(String.valueOf(signatureModel.getRecipientLock()));
		envelope.messageLock(String.valueOf(signatureModel.getMessageLock()));
		envelope.setEnforceSignerVisibility(String.valueOf(signatureModel.getEnforceSignerVisibility()));
		envelope.setSignerCanSignOnMobile(String.valueOf(signatureModel.getSignerCanSignOnMobile()));
		log.info("Creating Envelope API");
		results = envelopesApi.createEnvelope(tokenResponse.getAccountId(), envelope);
		log.info("Success in creating envelope : {}" + results.getEnvelopeId());
		return ResponseEntity.ok().body(results);
	}

	public ResponseEntity<?> createLock(String envelopeId, String duration) throws ApiException {
		apiClient.addDefaultHeader(Constant.AUTHORIZATION, Constant.BEARER + getAuthTokenOfAdmin());
		EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
		try {
			log.info("Getting envelopeLock Info");
			LockInformation lockInfo = envelopesApi.getLock(accountId, envelopeId);
			if (lockInfo != null) {
				String lockHeader = String.format(Constant.LOCK_HEADER, lockInfo.getLockToken(), duration);
				apiClient.addDefaultHeader(Constant.X_DOCUSIGN_EDIT, lockHeader);
				log.info("Initialized apiClient with LockHeader");
				envelopesApi.deleteLock(accountId, envelopeId);
				LockRequest lockRequest = new LockRequest();
				lockRequest.setLockDurationInSeconds(duration);
				lockRequest.setLockType("edit");
				log.info("Creating envelope Lock");
				LockInformation lockResponse = envelopesApi.createLock(accountId, envelopeId, lockRequest);
				return ResponseEntity.ok().body(lockResponse.getLockToken());
			}
		} catch (ApiException exp) {
			log.info("Inside catchBlock - createLock API");
			exp.printStackTrace();
			if (exp.getCode() == 404) {
				LockRequest lockRequest = new LockRequest();
				lockRequest.setLockDurationInSeconds(duration);
				lockRequest.setLockType("edit");
				LockInformation lockResponse = envelopesApi.createLock(accountId, envelopeId, lockRequest);
				LockInformation lockInfo = envelopesApi.getLock(accountId, envelopeId);
				log.info("Lock Token : {}", lockInfo.getLockToken());
				return ResponseEntity.ok().body(lockResponse.getLockToken());
			}
		}
		return null;

	}

	public String deleteLock(String envelopeId) {
		apiClient.addDefaultHeader(Constant.AUTHORIZATION, Constant.BEARER + getAuthTokenOfAdmin());
		EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
		try {
			log.info("Getting Envlope Lock Information");
			LockInformation lockInfo = envelopesApi.getLock(accountId, envelopeId);
			if (lockInfo != null) {
				String lockHeader = String.format(Constant.LOCK_HEADER, lockInfo.getLockToken(), "1800");
				apiClient.addDefaultHeader(Constant.X_DOCUSIGN_EDIT, lockHeader);
				log.info("Initialized apiClient with lockHeader");
				envelopesApi.deleteLock(accountId, envelopeId);
				return "Lock Deleted Successfully";
			} else
				return "Envelope is not Locked";
		} catch (ApiException exp) {
			log.info("Inside catchBlock - deleteLock");
			exp.printStackTrace();
			return exp.getResponseBody();
		}
	}

	public Object getEnvelopeNotification(String envelopeId) throws ApiException {
		try {
			apiClient.addDefaultHeader(Constant.AUTHORIZATION, Constant.BEARER + getAuthTokenOfAdmin());
			EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
			log.info("Calling EnvelopeDocument Notification API");
			return envelopesApi.getNotificationSettings(accountId, envelopeId);
		} catch (ApiException exp) {
			log.error("Inside catchBlock - Notification API");
			exp.printStackTrace();
			return exp.getResponseBody();
		}
	}

	public String uploadTemplateDocumentToEnvelope(String envelopeId, String templateId, String request,
			String envelopeDocumentId, String documentType, String flowType)
			throws URISyntaxException, StorageException, IOException {
		request = URLDecoder.decode(request, "UTF-8");
		ObjectMapper mapper = new ObjectMapper();
		JsonNode rootNode = mapper.readTree(request);
		String documentId = rootNode.get("documentId").asText();
		String name = rootNode.get("name").asText();
		byte[] document = null;
		if (documentType.equalsIgnoreCase("template"))
			document = getTemplateDocument(templateId, documentId);
		else if (documentType.equalsIgnoreCase("envelope"))
			document = getEnvelopeDocuments(templateId, documentId);
		if (flowType.equalsIgnoreCase("external")) {
			try {
				String docusignApiUrl = restBasePath + "/v2.1/accounts/" + accountId + "/envelopes/" + envelopeId
						+ "/documents/" + envelopeDocumentId;
				log.info(docusignApiUrl);
				HttpHeaders headers = new HttpHeaders();
				headers.setContentType(MediaType.APPLICATION_PDF);
				headers.add("Content-Disposition", "filename=\"" + name + "\"");
				headers.set(HttpHeaders.AUTHORIZATION, Constant.BEARER + getAuthTokenOfAdmin());
				HttpEntity<byte[]> requestEntity = new HttpEntity<>(document, headers);
				RestTemplate restTemplate = new RestTemplate();
				ResponseEntity<Object> response = restTemplate.exchange(docusignApiUrl, HttpMethod.PUT, requestEntity,
						Object.class);
				uploadFilesIntoBlob(document, envelopeId, envelopeDocumentId);
				System.out.println("UPLOAD "+response.getBody().toString());
				return response.getBody().toString();
			} catch (HttpClientErrorException e) {
				e.printStackTrace();
				rootNode = mapper.readTree(e.getResponseBodyAsString());
				String errorMessage = rootNode.get("message").asText();
				return errorMessage;
			} catch (Exception e) {
				e.printStackTrace();
				return e.getMessage();
			}
		} else {
			try {
				uploadFilesIntoBlob(document, envelopeId, envelopeDocumentId);
				return "Template Documents Uploaded Successfully";
			} catch (Exception e) {
				e.printStackTrace();
				return e.getMessage();
			}
		}
	}

	private void uploadFilesIntoBlob(byte[] document, String envelopeId, String documentId)
			throws URISyntaxException, StorageException, IOException {
		String path = envelopeId;
		CloudBlobContainer container = cloudBlobClient.getContainerReference(containerName);
		log.info("Initialized container :{}", containerName);
		CloudBlockBlob blob = container.getBlockBlobReference(containerName + path + "/V1/" + documentId);
		log.info("Initialized blob :{}", blob.getName());
		blob.getProperties().setContentType("application/pdf");
		try (InputStream inputStream = new ByteArrayInputStream(document);) {
			blob.upload(inputStream, document.length);
			log.info("Success in blob upload method");
		} catch (Exception e) {
			log.error("Inside catchBlock blob upload");
			e.printStackTrace();
		}
	}

	public String deleteEnvelopeDocument(DeleteEnvelopeRequest request)
			throws JsonMappingException, JsonProcessingException {
		apiClient.addDefaultHeader(Constant.AUTHORIZATION, Constant.BEARER + getAuthTokenOfAdmin());
		EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
		log.info("Initialized apiClient");
		List<Document> documents = new ArrayList<>();
		if (!request.getDelete_id().isEmpty()) {
			request.getDelete_id().forEach(p -> {
				Document document = new Document();
				document.setDocumentId(p);
				documents.add(document);
			});
			EnvelopeDefinition definition = new EnvelopeDefinition();
			definition.setDocuments(documents);
			try {
				log.info("Calling deleteDocuments API");
				envelopesApi.deleteDocuments(accountId, request.getEnvelopeId(), definition);
				return "Envelope Document Deleted SuccessFully";
			} catch (ApiException e) {
				log.error("Inside catchBlock - deleteDocuments API");
				e.printStackTrace();
				return e.getMessage();
			}
		}
		log.info("deleteId List is Empty");
		return null;

	}

	public ResponseEntity<?> newContractDocument(EsignaturePojo signatureModel, String envelopeId, String senderUserId)
			throws IllegalArgumentException, IOException, com.docusign.admin.client.ApiException, ApiException {
		GetUserTokenResponse tokenResponse = generateUserTokenByUserId(senderUserId);
		log.info("Generating userToken : {}", tokenResponse.getAccessToken());
		apiClient.addDefaultHeader(Constant.AUTHORIZATION,
				Constant.BEARER + generateUserTokenByUserId(null, senderUserId).getAccessToken());
		EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
		log.info("Initialized apiClient");
		EnvelopeSummary results = null;
		EnvelopeDefinition envelope = new EnvelopeDefinition();
		Notification notification = new Notification();
		Reminders reminders = new Reminders();
		reminders.setReminderDelay(signatureModel.getReminders().getReminderDelay());
		reminders.setReminderFrequency(signatureModel.getReminders().getReminderFrequency());
		reminders.setReminderEnabled(String.valueOf(signatureModel.getReminders().getReminderEnabled()));
		notification.setReminders(reminders);
		notification.setUseAccountDefaults(Constant.FALSE);
		Expirations expirations = new Expirations();
		expirations.setExpireAfter(signatureModel.getExpirations().getExpireAfter());
		expirations.setExpireEnabled(String.valueOf(signatureModel.getExpirations().getExpireEnabled()));
		expirations.setExpireWarn(signatureModel.getExpirations().getExpireWarn());
		notification.setExpirations(expirations);
		envelope.setNotification(notification);
		envelope.setEmailSubject(signatureModel.getEmailSubject());
		envelope.setEmailBlurb(signatureModel.getEmailMessage());
		int i = 1;
		SignHere signHere = new SignHere();
		signHere.setPageNumber("1");
		signHere.setXPosition("191");
		signHere.setYPosition("148");
		Tabs tabs = new Tabs();
		tabs.setSignHereTabs(Arrays.asList(signHere));
		List<Signer> signerList = new ArrayList<>();
		log.info("Adding SignersList to Envelope");
//		int min = 0;
		for (AllSigners signer : signatureModel.getAllSigners()) {
			i++;
			if (signer.getRecipientType().equalsIgnoreCase("LHDN_Stamper")
					|| signer.getRecipientType().equalsIgnoreCase("Vendor")) {
				Signer finalSigner = new Signer();
				finalSigner.setEmail(signer.getEmail());
				finalSigner.setName(signer.getDisplayName());
				finalSigner.setRecipientId(String.valueOf(i));
				finalSigner.setRoutingOrder(signer.getRoutingOrder());
				RecipientEmailNotification ren = new RecipientEmailNotification();
				ren.setEmailSubject(signatureModel.getEmailSubject());
//				if (signer.getRecipientType().equalsIgnoreCase("LHDN_Stamper")
//						&& (min == 0 || Integer.parseInt(signer.getRoutingOrder()) < min))
//					min = Integer.parseInt(signer.getRoutingOrder());
				String vendorMessage = null;
				switch (signatureModel.getModuleName()) {
				case Constant.LOA_CONTRACT_DOCUMENT_REPOSITORY:
					vendorMessage = Constant.vendorMessage;
					vendorMessage = vendorMessage.replace("{moduleName}", "contract")
							.replace("{abbreviation}", "contract")
							.replace("{contractName}", signatureModel.getContractName())
							.replace("{contractNumber}", signatureModel.getContractNumber());
					if (signer.getRecipientType().equalsIgnoreCase("LHDN_Stamper")) {
						vendorMessage = vendorMessage.replace("{moduleName2}", "LHDN").replace("{videoURL1}",
								"https://drive.google.com/file/d/1r9i-aQNFaW2u5bWHjsk50PfguGg7MBIN/view?usp=drive_link")
								.replace("{videoURL2}",
										"https://drive.google.com/file/d/1e8l5qdNRgys0zJAL_xVDLY7liEEW5d2G/view?usp=drive_link");
					} else {
						vendorMessage = vendorMessage.replace("{moduleName2}", "CONTRACT").replace("{videoURL1}",
								"https://drive.google.com/file/d/1MXHA8gIFt63XAjPKXITaslcUvExkcBWu/view?usp=drive_link")
								.replace("{videoURL2}",
										"https://drive.google.com/file/d/1-YnHokKW7yx7aBkE87YFvfLHUxh_oFnN/view?usp=drive_link");
					}
					break;
				}
//				if (signer.getRecipientType().equalsIgnoreCase("LHDN_Stamper")
//						&& Integer.parseInt(signer.getRoutingOrder()) <= min)
//					vendorMessage = Constant.lhdnMessage;
				ren.setEmailBody(vendorMessage);
				finalSigner.setEmailNotification(ren);
				signerList.add(finalSigner);
			}

			else if (signer.getRecipientType().equalsIgnoreCase("CVEF_Signer")) {
				Signer finalSigner = new Signer();
				finalSigner.setEmail(signer.getEmail());
				finalSigner.setName(signer.getDisplayName());
				finalSigner.setRecipientId(String.valueOf(i));
				finalSigner.setRoutingOrder(signer.getRoutingOrder());
				RecipientEmailNotification ren = new RecipientEmailNotification();
				ren.setEmailSubject(signatureModel.getEmailSubject());
				String cvefMessage = null;
				switch (signatureModel.getModuleName() != null ? signatureModel.getModuleName() : "") {
				case Constant.LOA_CONTRACT_DOCUMENT_REPOSITORY:
					cvefMessage = Constant.contractCvef;
					break;
				}
				ren.setEmailBody(cvefMessage);
				finalSigner.setEmailNotification(ren);
				signerList.add(finalSigner);
			} else if (signer.getRecipientType().equalsIgnoreCase("Stamper")) {
				Signer finalSigner = new Signer();
				finalSigner.setEmail(signer.getEmail());
				finalSigner.setName(signer.getDisplayName());
				finalSigner.setRecipientId(String.valueOf(i));
				finalSigner.setRoutingOrder(signer.getRoutingOrder());
				RecipientEmailNotification ren = new RecipientEmailNotification();
				ren.setEmailSubject(signatureModel.getEmailSubject());
				String stamperMessage = null;
				switch (signatureModel.getModuleName() != null ? signatureModel.getModuleName() : "") {
				case Constant.LOA_CONTRACT_DOCUMENT_REPOSITORY:
					stamperMessage = Constant.contractStamper;
					break;
				}
				ren.setEmailBody(stamperMessage);
				finalSigner.setEmailNotification(ren);
				signerList.add(finalSigner);
			} else {
				Signer finalSigner = new Signer();
				finalSigner.setEmail(signer.getEmail());
				finalSigner.setName(signer.getDisplayName());
				finalSigner.setRecipientId(String.valueOf(i));
				finalSigner.setRoutingOrder(signer.getRoutingOrder());
				signerList.add(finalSigner);
			}
		}
		List<CarbonCopy> carbonCopyList = new ArrayList<>();
		for (com.saaspe.docusign.model.Carboncopy cc : signatureModel.getRecipients().getCc()) {
			i++;
			CarbonCopy ccFinal = new CarbonCopy();
			ccFinal.setEmail(cc.getEmail());
			ccFinal.setName(cc.getName());
			ccFinal.setRecipientId(String.valueOf(i));
			ccFinal.setRoutingOrder(cc.getRoutingOrder());
			carbonCopyList.add(ccFinal);
		}

		Recipients recipients = new Recipients();
		Signer signer = new Signer();
		signer.setTabs(tabs);
		recipients.setSigners(signerList);
		recipients.setCarbonCopies(carbonCopyList);
		envelope.setRecipients(recipients);
		envelope.setAllowComments(signatureModel.getAllowComments());
		envelope.recipientsLock(String.valueOf(signatureModel.getRecipientLock()));
		envelope.messageLock(String.valueOf(signatureModel.getMessageLock()));
		envelope.setEnforceSignerVisibility(String.valueOf(signatureModel.getEnforceSignerVisibility()));
		envelope.setSignerCanSignOnMobile(String.valueOf(signatureModel.getSignerCanSignOnMobile()));
		log.info("Creating envelope");
		results = envelopesApi.createEnvelope(tokenResponse.getAccountId(), envelope);
		log.info("Success in creating envelope :{}", results.getEnvelopeId());
		return ResponseEntity.ok().body(results);
	}

	public Object updateDocument(MultipartFile file, String envelopeId, String documentId) {

//		System.out.println(documentId);
		String docusignApiUrl = restBasePath + "/v2.1/accounts/" + accountId + "/envelopes/" + envelopeId
				+ "/documents/" + documentId;
		try {
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_PDF);
			headers.add("Content-Disposition", "filename=\""
					+ file.getOriginalFilename().substring(0, file.getOriginalFilename().lastIndexOf(".")) + "\"");
			headers.set(HttpHeaders.AUTHORIZATION, "Bearer " + getAuthTokenOfAdmin());
			byte[] fileBytes = file.getBytes();
			HttpEntity<byte[]> requestEntity = new HttpEntity<>(fileBytes, headers);
			RestTemplate restTemplates = new RestTemplate();
		    ResponseEntity<Object> response= restTemplates.exchange(docusignApiUrl, HttpMethod.PUT, requestEntity, Object.class);
		    System.out.println("Upload API :"+response.getBody());
		    return response.getBody();
		} catch (IOException e) {
			e.printStackTrace();
			return new ResponseEntity<>("Failed to process the file", HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	public ResponseEntity<?> createEnvelope(EsignaturePojo signatureModel, String templateId, String userId)
			throws ApiException, IllegalArgumentException, IOException, com.docusign.admin.client.ApiException {
		GetUserTokenResponse token = generateUserTokenByUserId(signatureModel.getUserEmail(), userId);
		ApiClient apiclient = apiClient.addDefaultHeader(Constant.AUTHORIZATION,
				Constant.BEARER + token.getAccessToken());
		EnvelopeSummary results = null;
		EnvelopeDefinition envelope = new EnvelopeDefinition();
		Notification notification = new Notification();
		Reminders reminders = new Reminders();
		reminders.setReminderDelay(signatureModel.getReminders().getReminderDelay());
		reminders.setReminderFrequency(signatureModel.getReminders().getReminderFrequency());
		reminders.setReminderEnabled(String.valueOf(signatureModel.getReminders().getReminderEnabled()));
		notification.setReminders(reminders);
		notification.setUseAccountDefaults(Constant.FALSE);
		Expirations expirations = new Expirations();
		expirations.setExpireAfter(signatureModel.getExpirations().getExpireAfter());
		expirations.setExpireEnabled(String.valueOf(signatureModel.getExpirations().getExpireEnabled()));
		expirations.setExpireWarn(signatureModel.getExpirations().getExpireWarn());

		notification.setExpirations(expirations);
		TemplatesApi templatesApi = new TemplatesApi(apiclient);
		envelope.setNotification(notification);
		envelope.setEmailSubject(signatureModel.getEmailSubject());
		envelope.setEmailBlurb(signatureModel.getEmailMessage());
		envelope.setStatus(signatureModel.getStatus());
		List<Document> createDocuments = new ArrayList<>();
		TemplateDocumentsResult envelopeDocuments = templatesApi.listDocuments(accountId, templateId);
		envelopeDocuments.getTemplateDocuments()
				.removeIf(p -> p.getDocumentId().equalsIgnoreCase(Constant.CERTIFICATE));
		// adding Documents
		for (com.saaspe.docusign.model.Document document : signatureModel.getDocuments()) {
			SignHere signHere = new SignHere();
			signHere.setPageNumber("1");
			signHere.setXPosition("191");
			signHere.setYPosition("148");
			Tabs tabs = new Tabs();
			tabs.setSignHereTabs(Arrays.asList(signHere));
			if (document.getCategory().equalsIgnoreCase(Constant.CREATE_FILE)) {
				Document createDocument = new Document();
				createDocument.setName(document.getName());
				createDocument.setDocumentBase64(document.getDocumentBase64());
				createDocument.setDocumentId(document.getDocumentId());
				createDocument.setTabs(tabs);
				createDocuments.add(createDocument);
			} else if (document.getCategory().equalsIgnoreCase(Constant.DELETE)) {
				envelopeDocuments.getTemplateDocuments()
						.removeIf(p -> p.getDocumentId().equalsIgnoreCase(document.getDocumentId()));
			} else if (document.getCategory().equalsIgnoreCase(Constant.UPDATE_FILE)) {
				Document updateDocument = new Document();
				updateDocument.setName(document.getName());
				updateDocument.setDocumentBase64(document.getDocumentBase64());
				updateDocument.setDocumentId(document.getDocumentId());
				updateDocument.setTabs(tabs);
				envelopeDocuments.getTemplateDocuments()
						.removeIf(p -> p.getDocumentId().equalsIgnoreCase(document.getDocumentId()));
				createDocuments.add(updateDocument);
			}
		}
		for (EnvelopeDocument documentInfo : envelopeDocuments.getTemplateDocuments()) {
			byte[] documentBytes = templatesApi.getDocument(accountId, templateId, documentInfo.getDocumentId());
			String base64Doc = Base64.getEncoder().encodeToString(documentBytes);
			Document document1 = new Document();
			document1.setName(documentInfo.getName());
			document1.setDocumentBase64(base64Doc);
			document1.setDocumentId(documentInfo.getDocumentId());
			createDocuments.add(document1);
		}
		List<Document> documentList = new ArrayList<>();
		documentList.addAll(createDocuments);
		int i = 1;
		SignHere signHere = new SignHere();
		signHere.setPageNumber("1");
		signHere.setXPosition("191");
		signHere.setYPosition("148");
		Tabs tabs = new Tabs();
		tabs.setSignHereTabs(Arrays.asList(signHere));
		List<Signer> signerList = new ArrayList<>();
		for (com.saaspe.docusign.model.Signer signer : signatureModel.getRecipients().getSigners()) {
			i++;
			Signer finalSigner = new Signer();
			finalSigner.setEmail(signer.getEmail());
			finalSigner.setName(signer.getName());
			finalSigner.setRecipientId(String.valueOf(i));
			finalSigner.setRoutingOrder(signer.getRoutingOrder());
			signerList.add(finalSigner);
		}
		List<CarbonCopy> carbonCopyList = new ArrayList<>();
		for (com.saaspe.docusign.model.Carboncopy cc : signatureModel.getRecipients().getCc()) {
			i++;
			CarbonCopy ccFinal = new CarbonCopy();
			ccFinal.setEmail(cc.getEmail());
			ccFinal.setName(cc.getName());
			ccFinal.setRecipientId(String.valueOf(i));
			ccFinal.setRoutingOrder(cc.getRoutingOrder());
			carbonCopyList.add(ccFinal);
		}
		Recipients recipients = new Recipients();
		Signer signer = new Signer();
		signer.setTabs(tabs);
		recipients.setSigners(signerList);
		recipients.setCarbonCopies(carbonCopyList);
		envelope.setRecipients(recipients);
		envelope.setDocuments(documentList);
		envelope.setAllowComments(signatureModel.getAllowComments());
		envelope.recipientsLock(String.valueOf(signatureModel.getRecipientLock()));
		envelope.messageLock(String.valueOf(signatureModel.getMessageLock()));
		envelope.setEnforceSignerVisibility(Constant.FALSE);
		envelope.setSignerCanSignOnMobile(String.valueOf(signatureModel.getSignerCanSignOnMobile()));
		EnvelopesApi envelopesApi = new EnvelopesApi(apiclient);
		// Commented update document in templates
//		if (!deleteDocuments.isEmpty()) {
//			EnvelopeDefinition deleteDefinition = new EnvelopeDefinition();
//			deleteDefinition.setDocuments(deleteDocuments);
//			templatesApi.deleteDocuments(tokenResponse.getAccountId(), templateId, deleteDefinition);
//		} else if (!createDocuments.isEmpty()) {
//			EnvelopeDefinition updateDefinition = new EnvelopeDefinition();
//			updateDefinition.setDocuments(createDocuments);
//			templatesApi.updateDocuments(tokenResponse.getAccountId(), templateId, updateDefinition);
//		}
		results = envelopesApi.createEnvelope(accountId, envelope);
		return ResponseEntity.ok().body(results);
	}

	public ResponseEntity<byte[]> downloadDocuments(String envelopeId) throws IllegalArgumentException, IOException {
		try {
			OAuth.UserInfo userInfo = apiClient.getUserInfo(getAccessToken());
			String accountsId = userInfo.getAccounts().get(0).getAccountId();
			apiClient.addDefaultHeader(Constant.AUTHORIZATION, Constant.BEARER + getAccessToken());
			EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
			byte[] results = envelopesApi.getDocument(accountsId, envelopeId, "archive");
			HttpHeaders headers = new HttpHeaders();
			headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + envelopeId + ".zip");
			headers.add(HttpHeaders.CONTENT_TYPE, "application/octet-stream");
			return ResponseEntity.ok().headers(headers).body(results);
		} catch (ApiException exp) {
			exp.printStackTrace();
			if (exp.getMessage().contains(Constant.CONSENT_REQUIRED)) {
				try {
					log.info(Constant.CONSENT_REQUIRED_PROVIDE_CONSENT);
					Desktop.getDesktop().browse(
							new URI(docusignHost + docusignOAuthPath + clientId + Constant.REDIRECT_URI + redirectUrl));
				} catch (Exception e) {
					log.info(Constant.ISSUE_IN_GETTING_CONSENT);
				}
			}
			return null;
		}
	}

	public String getCompleteViewURL(@Valid String envelopeId, @Valid String email, @Valid String userId,
			@Valid String clientUserId, @Valid String returnUrl) throws ApiException {
		try {
			RecipientViewRequest viewRequest = new RecipientViewRequest();
			viewRequest.setAuthenticationMethod("None");
			viewRequest.setReturnUrl(returnUrl);
			viewRequest.setEmail(email);
			viewRequest.setUserName("NN");
			viewRequest.setClientUserId(clientUserId);
			viewRequest.setUserId(userId);
			apiClient.addDefaultHeader(Constant.AUTHORIZATION, Constant.BEARER + getAuthTokenOfAdmin());
			EnvelopesApi envelopesApi = new EnvelopesApi(apiClient);
			ViewUrl viewUrl = envelopesApi.createRecipientView(accountId, envelopeId.trim(), viewRequest);
			log.info("Calling createLock");
			return viewUrl.getUrl();
		} catch (ApiException exp) {
			log.info("Inside catchBlock consoleView API");
			exp.printStackTrace();
			throw exp;
		}
	}
}
