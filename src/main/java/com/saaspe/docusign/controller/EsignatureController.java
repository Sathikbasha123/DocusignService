package com.saaspe.docusign.controller;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.docusign.esign.client.ApiException;
import com.docusign.esign.model.ChunkedUploadRequest;
import com.docusign.esign.model.Envelope;
import com.docusign.esign.model.EnvelopeDocumentsResult;
import com.docusign.esign.model.EnvelopesInformation;
import com.docusign.esign.model.RecipientViewRequest;
import com.docusign.esign.model.Recipients;
import com.saaspe.docusign.aspect.ControllerLogging;
import com.saaspe.docusign.constant.Constant;
import com.saaspe.docusign.model.AuditEvent;
import com.saaspe.docusign.model.DeleteEnvelopeRequest;
import com.saaspe.docusign.model.EsignaturePojo;
import com.saaspe.docusign.model.TemplateGetByIdResponse;
import com.saaspe.docusign.model.UpdateChunkRequest;
import com.saaspe.docusign.service.EsignatureService;
import com.saaspe.docusign.service.EsignatureService.CreateTemplate;
import com.saaspe.docusign.service.EsignatureService.GetUserTokenResponse;

@RestController
@CrossOrigin
@ControllerLogging
public class EsignatureController {

	@Autowired
	private EsignatureService service;

	private static final Logger log = LoggerFactory.getLogger(EsignatureController.class);

	@PostMapping(value = "/createEnvelopeMultiple")
	public ResponseEntity<?> createEnvelopeMultiple(@RequestBody EsignaturePojo signatureModel,
			@RequestParam(required = false) String templateId, @RequestParam String userId) throws ApiException {
		String traceId = UUID.randomUUID().toString();
		try {
			log.info("[TRACE_ID: {}] Success In createEnvelopeMultiple {}", traceId, signatureModel);
			return service.createEnvelopeMultiple(signatureModel, templateId, userId);
		} catch (Exception e) {
			log.error("[TRACE_ID: {}] Error in createEnvelopeMultiple: {}", traceId, e.getMessage(), e);
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@PostMapping(value = "/newCreateEnvelopeMultiple")
	public ResponseEntity<?> newCreateEnvelopeMultiple(@RequestBody EsignaturePojo signatureModel,
			@RequestParam(required = false) String templateId, @RequestParam String userId) throws ApiException {
		String traceId = UUID.randomUUID().toString();
		try {
			log.info("[TRACE_ID: {}] Success In newCreateEnvelopeMultiple {}", traceId, signatureModel);
			return service.newCreateEnvelopeMultiple(signatureModel, templateId, userId);
		} catch (Exception e) {
			log.error("[TRACE_ID: {}] Error in newCreateEnvelopeMultiple: {}", traceId, e.getMessage(), e);
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@PostMapping(value = "/updateEnvelopeMultiple")
	public ResponseEntity<?> updateEnvelopeDocument(@RequestBody EsignaturePojo signatureModel,
			@RequestParam String envelopeId, @RequestParam String userId) throws ApiException {
		String traceId = UUID.randomUUID().toString();
		try {
			log.info("[TRACE_ID: {}] Success In updateEnvelopeDocument {}", traceId, signatureModel);
			return service.updateEnvelopeDocument(signatureModel, envelopeId, userId);
		} catch (Exception e) {
			log.error("[TRACE_ID: {}] Error in updateEnvelopeDocument: {}", traceId, e.getMessage(), e);
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@PostMapping(value = "/newLoaContractDocument")
	public ResponseEntity<?> createNewContractDocument(@RequestBody EsignaturePojo signatureModel,
			@RequestParam String envelopeId, @RequestParam String senderUserId) throws ApiException {
		String traceId = UUID.randomUUID().toString();
		try {
			log.info("[TRACE_ID: {}] Success In createNewContractDocument {}", traceId, signatureModel);
			return service.createNewContractDocument(signatureModel, envelopeId, senderUserId);
		} catch (Exception e) {
			log.error("[TRACE_ID: {}] Error in createNewContractDocument: {}", traceId, e.getMessage(), e);
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@PostMapping(value = "/newContractDocument")
	public ResponseEntity<?> newContractDocument(@RequestBody EsignaturePojo signatureModel,
			@RequestParam String envelopeId, @RequestParam String senderUserId) throws ApiException {
		String traceId = UUID.randomUUID().toString();
		try {
			log.info("[TRACE_ID: {}] Success In newContractDocument : {}", traceId, signatureModel);
			return service.newContractDocument(signatureModel, envelopeId, senderUserId);
		} catch (Exception e) {
			log.error("[TRACE_ID: {}] Error in newContractDocument : {}", traceId, e.getMessage(), e);
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@PostMapping(value = "/updateEnvelopeStatus")
	public ResponseEntity<?> updateEnvelopeStatus(@RequestParam String envelopeId, @RequestParam String userId,
			@RequestParam String status) throws ApiException {
		String traceId = UUID.randomUUID().toString();
		try {
			log.info("[TRACE_ID: {}] Success In updateEnvelopeDocumentStatus {}", traceId, userId);
			return service.updateEnvelopeDocumentStatus(envelopeId, userId, status);
		} catch (Exception e) {
			log.error("[TRACE_ID: {}] Error in updateEnvelopeDocumentStatus : {}", traceId, e.getMessage(), e);
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@GetMapping(value = "/getEnvelopeDocuments/{envelopeId}/{documentId}")
	public String getEnvelopeDocument(@PathVariable String envelopeId, @PathVariable String documentId)
			throws ApiException {
		String traceId = UUID.randomUUID().toString();
		try {
			log.info("[TRACE_ID: {}] Success In getEnvelopeDocument {}, {}", traceId, envelopeId, documentId);
			return service.getEnvelopeDocument(envelopeId, documentId);
		} catch (Exception e) {
			log.error("[TRACE_ID: {}] Error in getEnvelopeDocument: {}", traceId, e.getMessage(), e);
			return e.getLocalizedMessage();
		}
	}

	@GetMapping(value = "/getIndividualEnvelopeStatus/{envelopeId}")
	public Envelope getIndividualEnvelopeStatus(@PathVariable String envelopeId) throws java.io.IOException {
		String traceId = UUID.randomUUID().toString();
		try {
			log.info("[TRACE_ID: {}] Success In getIndividualEnvelopeStatus {}", traceId, envelopeId);
			return service.getIndividualEnvelopeStatus(envelopeId);
		} catch (Exception e) {
			log.error("[TRACE_ID: {}] Error in getIndividualEnvelopeStatus: {}", traceId, e.getMessage(), e);
			return null;
		}
	}

	@GetMapping(value = "/getListOfEnvelopes")
	public EnvelopesInformation getListOfEnvelopes(@RequestParam String fromDate, @RequestParam String toDate,
			@RequestParam String startPosition, @RequestParam String count) throws ApiException {

		String traceId = UUID.randomUUID().toString();
		try {
			log.info("[TRACE_ID: {}] Success In getListOfEnvelopes {},{},{},{}", traceId, fromDate, toDate,
					startPosition, count);
			return service.getListOfEnvelopes(fromDate, toDate, startPosition, count);
		} catch (Exception e) {
			log.error("[TRACE_ID: {}] Error in getListOfEnvelopes: {}", traceId, e.getMessage(), e);
			return null;
		}
	}

	@GetMapping(value = "/getListOfEnvelopes/envelopeids")
	public EnvelopesInformation getListOfEnvelopes(@RequestParam String envelopeId) {
		String traceId = UUID.randomUUID().toString();
		try {
			log.info("[TRACE_ID: {}] Success In getListOfEnvelopes {}", traceId, envelopeId);
			return service.getListOfEnvelopes(envelopeId);
		} catch (Exception e) {
			log.error("[TRACE_ID: {}] Error in getListOfEnvelopes: {}", traceId, e.getMessage(), e);
			return null;
		}
	}

	@GetMapping(value = "/getEnvelopeDocumentDetails/{envelopeId}")
	public EnvelopeDocumentsResult getEnvelopeDocumentDetails(@PathVariable String envelopeId) throws ApiException {
		String traceId = UUID.randomUUID().toString();
		try {
			log.info("[TRACE_ID: {}] Success In getEnvelopeDocumentDetails {}", traceId, envelopeId);
			return service.getEnvelopeDocumentDetails(envelopeId);
		} catch (Exception e) {
			log.error("[TRACE_ID: {}] Error in getEnvelopeDocumentDetails: {}", traceId, e.getMessage(), e);
			return null;
		}
	}

	@GetMapping(value = "/listEnvelopeRecipients/{envelopeId}")
	public Recipients listEnvelopeRecipients(@PathVariable String envelopeId) throws ApiException {
		String traceId = UUID.randomUUID().toString();
		try {
			log.info("[TRACE_ID: {}] Success In listEnvelopeRecipients {}", traceId, envelopeId);
			return service.listEnvelopeRecipients(envelopeId);
		} catch (Exception e) {
			log.error(Constant.ERROR_IN_LIST_ENVELOPE_RECIPIENTS, traceId, e.getMessage(), e);
			return null;
		}
	}

	@GetMapping(value = "/templates")
	public Object listTemplates(@RequestParam(defaultValue = "10") String count,
			@RequestParam(defaultValue = "0") String start, @RequestParam(defaultValue = "asc") String order,
			@RequestParam(defaultValue = "modified") String orderBy,
			@RequestParam(name = "searchText", required = false) String searchText, @RequestParam String folderId) {
		String traceId = UUID.randomUUID().toString();
		try {
			log.info("[TRACE_ID: {}] Success In listTemplates {},{},{},{},{}", traceId, count, start, order, orderBy,
					searchText);
			return service.listTemplates(count, start, order, orderBy, searchText, folderId);
		} catch (Exception e) {
			log.error(Constant.ERROR_IN_LIST_ENVELOPE_RECIPIENTS, traceId, e.getMessage(), e);
			return null;
		}
	}

	@GetMapping(value = "/template/{templateId}")
	public TemplateGetByIdResponse listTemplateById(@PathVariable String templateId) {
		String traceId = UUID.randomUUID().toString();
		try {
			log.info("[TRACE_ID: {}] Success In listTemplates {}", traceId, templateId);
			return service.listTemplateById(templateId);
		} catch (Exception e) {
			log.error(Constant.ERROR_IN_LIST_ENVELOPE_RECIPIENTS, traceId, e.getMessage(), e);
			return null;
		}
	}

	@GetMapping(value = "/audit/{envelopeId}")
	public AuditEvent getAudit(@PathVariable String envelopeId) throws ApiException {
		String traceId = UUID.randomUUID().toString();
		try {
			log.info("[TRACE_ID: {}] Success In getAudit {}", traceId, envelopeId);
			return service.getAudit(envelopeId);
		} catch (Exception e) {
			log.error(Constant.ERROR_IN_GET_AUDIT, traceId, e.getMessage(), e);
			return null;
		}

	}

	@PostMapping(value = "/create/template")
	public ResponseEntity<Object> createTemplate(@RequestBody CreateTemplate createTemplate, @RequestParam String email,
			@RequestParam String folderId, @RequestParam String userId) throws ApiException {
		String traceId = UUID.randomUUID().toString();
		try {
			return ResponseEntity.ok(service.createTemplate(createTemplate, email, folderId, userId));
		} catch (ApiException e) {
			log.error("[TRACE_ID: {}] Error in getAudit: {}", traceId, e.getMessage(), e);
			return ResponseEntity.badRequest().body(e.getResponseBody());
		} catch (Exception e) {
			log.error(Constant.ERROR_IN_GET_AUDIT, traceId, e.getMessage(), e);
			return ResponseEntity.badRequest().body(e.getMessage());
		}

	}

	@PutMapping(value = "/update/template/{templateId}")
	public ResponseEntity<Object> updateTemplate(@PathVariable String templateId,
			@RequestBody CreateTemplate updatetemplaterequest) throws ApiException {
		String traceId = UUID.randomUUID().toString();
		try {
			return ResponseEntity.ok(service.updateTemplate(templateId, updatetemplaterequest));
		} catch (ApiException e) {
			log.error("[TRACE_ID: {}] Error in updateTemplate: {}", traceId, e.getMessage(), e);
			return ResponseEntity.badRequest().body(e.getResponseBody());
		} catch (Exception e) {
			log.error(Constant.ERROR_IN_GET_AUDIT, traceId, e.getMessage(), e);
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@GetMapping(value = "/envelope/{envelopeId}")
	public ResponseEntity<?> getEnvelopeById(@PathVariable String envelopeId) throws ApiException {
		String traceId = UUID.randomUUID().toString();
		try {
			log.info("[TRACE_ID: {}] Success In getEnvelopeById {}", traceId, envelopeId);
			return ResponseEntity.ok(service.getEnvelopeById(envelopeId));
		} catch (Exception e) {
			log.error("[TRACE_ID: {}] Error in getEnvelopeById: {}", traceId, e.getMessage(), e);
			return ResponseEntity.badRequest().body(e.getLocalizedMessage());
		}
	}

	@GetMapping(value = "/getTemplateDocuments/{templateId}/{documentId}")
	public ResponseEntity<byte[]> getTemplateDocument(@PathVariable String templateId,
			@PathVariable String documentId) {
		String traceId = UUID.randomUUID().toString();
		try {
			log.info("[TRACE_ID: {}] Success In getTemplateDocument {}, {}", traceId, templateId, documentId);
			return ResponseEntity.ok(service.getTemplateDocument(templateId, documentId));
		} catch (Exception e) {
			log.error("[TRACE_ID: {}] Error in getTemplateDocument: {}", traceId, e.getMessage(), e);
			return null;
		}
	}

	@GetMapping(value = "/getEnvelopeComment/{envelopeId}")
	public ResponseEntity<byte[]> getEnvelopeComment(@PathVariable String envelopeId, @RequestParam String email,
			@RequestParam String userId) {
		String traceId = UUID.randomUUID().toString();
		try {
			log.info("[TRACE_ID: {}] Success In getEnvelopeComment {}, {},{}", traceId, envelopeId, email, userId);
			return ResponseEntity.ok(service.getEnvelopeComment(envelopeId, email, userId));
		} catch (Exception e) {
			log.error("[TRACE_ID: {}] Error in getEnvelopeComment: {}", traceId, e.getMessage(), e);
			return null;
		}
	}

	@GetMapping(value = "/getConsent")
	public Map<String, String> getConsent(@RequestParam String email, @RequestParam(required = false) String userId)
			throws IllegalArgumentException, IOException, com.docusign.admin.client.ApiException {
		Map<String, String> response = new HashMap<>();
		GetUserTokenResponse tokenResponse = service.getTest(email, userId);
		response.put("error", tokenResponse.getErrorMessage());
		response.put("consentUrl", tokenResponse.getAuthEnpoint());
		response.put("activated", tokenResponse.getActivated());
		return response;
	}

	@PostMapping("/getConsoleViewUrl")
	public ResponseEntity<?> getConsoleViewURL(@RequestParam(value = "envelopeId") String envelopeId,
			@RequestParam(value = "returnUrl") String returnUrl,
			@RequestParam(value = "action",required = false) String action)  {
		 try {
		        String consoleViewUrl = service.getConsoleURL(envelopeId, returnUrl,action);
		        return ResponseEntity.ok(consoleViewUrl);
		    } catch (ApiException e) {
		        e.printStackTrace();
		        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
		                .body("Failed to retrieve console view URL: " + e.getMessage());
		    }
	}

	@PutMapping("/createLock")
	public ResponseEntity<?> createLock(@RequestParam String envelopeId, @RequestParam String duration)
			throws ApiException {
		String traceId = UUID.randomUUID().toString();
		try {
			log.info("[TRACE_ID: {}] Success In createLock {}", traceId, envelopeId);
			return service.createLock(envelopeId, duration);
		} catch (Exception e) {
			log.error("[TRACE_ID: {}] Error in createLock: {}", traceId, e.getMessage(), e);
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}

	@PutMapping("/deleteLock")
	public String deleteLock(@RequestParam String envelopeId) {
		String traceId = UUID.randomUUID().toString();
		try {
			log.info("[TRACE_ID: {}] Success In deleteLock {}", traceId, envelopeId);
			return service.deleteLock(envelopeId);
		} catch (Exception e) {
			log.error("[TRACE_ID: {}] Error in deleteLock: {}", traceId, e.getMessage(), e);
			return e.getMessage();
		}
	}

//	@PostMapping("/createChunk")
//	public String createChunk(@RequestBody ChunkedUploadRequest request) {
//		String traceId = UUID.randomUUID().toString();
//		try {
//			log.info("[TRACE_ID: {}] Success In createChunk {}", traceId, request.getChunkedUploadId());
//			return service.createChunkie(request);
//		} catch (ApiException e) {
//			log.error("[TRACE_ID: {}] Error in createChunk: {}", traceId, e.getMessage(), e);
//			e.printStackTrace();
//			return e.getMessage();
//		}
//
//	}

//	@PutMapping("/commitChunk")
//	public String commitChunk(@RequestParam(defaultValue = "chunkUploadId") String chunkUploadId) {
//		String traceId = UUID.randomUUID().toString();
//		try {
//			log.info("[TRACE_ID: {}] Success In commitChunk {}", traceId, chunkUploadId);
//			return service.commitChunk(chunkUploadId);
//		} catch (ApiException e) {
//			log.error("[TRACE_ID: {}] Error in commitChunk: {}", traceId, e.getMessage(), e);
//			e.printStackTrace();
//			return e.getMessage();
//		}
//
//	}

//	@PutMapping("/updateChunk")
//	public String updateChunk(@RequestBody UpdateChunkRequest updateRequest) {
//		String traceId = UUID.randomUUID().toString();
//		try {
//			log.info("[TRACE_ID: {}] Success In updateChuk {}", traceId, updateRequest.getChunkedUploadId());
//			return service.updateChunk(updateRequest);
//		} catch (ApiException e) {
//			log.error("[TRACE_ID: {}] Error in updateChunk: {}", traceId, e.getMessage(), e);
//			e.printStackTrace();
//			return e.getMessage();
//		}
//
//	}

	@GetMapping(value = "/envelope/notification/{envelopeId}")
	public ResponseEntity<?> getEnvelopeNotificationById(@PathVariable String envelopeId) throws ApiException {
		String traceId = UUID.randomUUID().toString();
		try {
			log.info("[TRACE_ID: {}] Success In getEnvelopeNotificationById {}", traceId, envelopeId);
			return ResponseEntity.ok(service.getEnvelopeNotification(envelopeId));
		} catch (Exception e) {
			log.error("[TRACE_ID: {}] Error in getEnvelopeNotificationById: {}", traceId, e.getMessage(), e);
			return ResponseEntity.badRequest().body(e.getLocalizedMessage());
		}
	}

	@GetMapping(value = "/upload/document/envelope")
	public String uploadTemplateDocumentToEnvelope(@RequestParam String envelopeId, @RequestParam String templateId,
			@RequestParam(value = "templateDocument") String request, @RequestParam String envelopeDocumentId,
			@RequestParam String documentType, @RequestParam String flowType) {
		String traceId = UUID.randomUUID().toString();
		try {
			log.info("[TRACE_ID: {}] Success In uploadTemplateDocumentToEnvelope {}, {},{},{}", traceId, envelopeId,
					templateId, request, envelopeDocumentId);
			return service.uploadTemplateDocumentToEnvelope(envelopeId, templateId, request, envelopeDocumentId,
					documentType, flowType);
		} catch (Exception e) {
			log.error("[TRACE_ID: {}] Error in uploadTemplateDocumentToEnvelope: {}", traceId, e.getMessage(), e);
			return e.getLocalizedMessage();
		}
	}

	@DeleteMapping(value = "/delete/envelope-document")
	public String deleteEnvelopeDocument(@RequestBody DeleteEnvelopeRequest request) throws ApiException {
		String traceId = UUID.randomUUID().toString();
		try {
			log.info("[TRACE_ID: {}] Success In deleteEnvelopeDocument {}", traceId, request);
			return service.deleteEnvelopeDocument(request);
		} catch (Exception e) {
			log.error("[TRACE_ID: {}] Error in deleteEnvelopeDocument: {}", traceId, e.getMessage(), e);
			return e.getLocalizedMessage();
		}
	}

	@PostMapping("/updateDocument")
	public Object updateDocument(@RequestPart(value = "document-file", required = false) MultipartFile file,
			@RequestParam String envelopeId, @RequestParam String documentId) {
		String traceId = UUID.randomUUID().toString();
		try {
			log.info("[TRACE_ID: {}] Success In updateDocument {}", traceId, envelopeId);
			return service.updateDocument(file, envelopeId, documentId);
		} catch (Exception e) {
			log.info("[TRACE_ID: {}] Success In updateDocument {}", traceId, envelopeId);
			e.printStackTrace();
			return e;
		}

	}
	
	@PostMapping(value = "/createEnvelope")
	public ResponseEntity<?> createEnvelope(@RequestBody EsignaturePojo signatureModel, @RequestParam String templateId,
			@RequestParam String userId) throws ApiException {
		String traceId = UUID.randomUUID().toString();
		try {
			log.info("[TRACE_ID: {}] Success In createEnvelopes {}", traceId, signatureModel);
			return service.createEnvelope(signatureModel, templateId, userId);
		} catch (Exception e) {
			log.error("[TRACE_ID: {}] Error in createEnvelopes: {}", traceId, e.getMessage(), e);
			return ResponseEntity.badRequest().body(e.getMessage());
		}
	}
	
	@GetMapping(value = "/download-documents")
	public ResponseEntity<byte[]> downloadDocuments(@RequestParam String envelopeId) throws ApiException {
		String traceId = UUID.randomUUID().toString();
		try {
			log.info("[TRACE_ID: {}] Success In downloadDocuments {}", traceId, envelopeId);
			return service.downloadDocuments(envelopeId);
		} catch (Exception e) {
			log.error("[TRACE_ID: {}] Error in downloadDocuments: {}", traceId, e.getMessage(), e);
			return null;
		}
	}
	
	@GetMapping("/getCompletedViewUrl")
	public String getCompletedViewURL(@RequestParam(value = "envelopeId",required = true) @Valid String envelopeId
			,@RequestParam(value = "email", required = true) @Valid String email,@RequestParam(value = "userId", required = true) @Valid String userId
			,@RequestParam(value = "clientUserId", required = true) @Valid String clientUserId
			,@RequestParam(value = "returnUrl", required = true) String returnUrl) throws IllegalArgumentException, IOException  {
		 try {
			    log.info("Came Here");
		        String consoleViewUrl = service.getCompleteViewURL(envelopeId,email,userId,clientUserId,returnUrl);
		        return consoleViewUrl;
		    } catch (ApiException e) {
		        e.printStackTrace();
		        return null;
		    }
	}
}
