package com.saaspe.docusign.model;

import java.util.List;

import lombok.Data;

@Data
public class UpdateTemplateRequest {
	private String name;
	private String description;
	private String emailSubject;
	private String emailBlurb;
	private List<DocumentRequestt> documents;
	private List<SignerRequestt> signerrequest;
	private Reminders reminders;
	private Boolean allowComments;
	private Boolean enforceSignerVisibility;
	private Expiration expiration;
	private Boolean signerCanSignOnMobile;

}
