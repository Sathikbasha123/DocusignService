package com.saaspe.docusign.model;

import lombok.Data;

@Data
public class SignerRequestt {
	private String name;
	private String email;
	private String recipientId;
	private String recipientType;
	private String recipientRole;
	private Boolean canSignOffline;

}
