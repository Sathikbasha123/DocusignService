package com.saaspe.docusign.model;

import java.util.List;

import lombok.Data;

@Data
public class EsignaturePojo {
	private List<Document> documents;
	private String emailSubject;
	private String emailMessage;
	private String allowComments;
	private Boolean enforceSignerVisibility;
	private String recipientLock;
	private String messageLock;
	private Reminders reminders;
	private Expiration expirations;
	private Recipients recipients;
	private String status;
	private String useAccountDefaults;
	private Boolean signerCanSignOnMobile;
	private String userEmail;
	private List<AllSigners> allSigners;
	private String moduleName;
	private String contractName;
	private String contractNumber;
}
