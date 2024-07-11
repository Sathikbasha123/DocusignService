package com.saaspe.docusign.model;

import lombok.Data;

@Data
public class Document {

	private String documentBase64;
	private String documentId;
	private String fileExtension;
	private String name;
	private String category;
}
