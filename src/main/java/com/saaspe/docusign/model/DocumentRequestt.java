package com.saaspe.docusign.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.Data;

@Data
@JsonInclude(content = Include.NON_NULL)
public class DocumentRequestt {
	private String name;
	private String documentId;
	private String documentBase64;
}
