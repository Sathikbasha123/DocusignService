package com.saaspe.docusign.model;

import lombok.Data;

@Data
public class DocumentResponse {

	private String documentId;
	
	private String documentIdGuid;
	
	private String name;
	
	private String documentBase64;
}
