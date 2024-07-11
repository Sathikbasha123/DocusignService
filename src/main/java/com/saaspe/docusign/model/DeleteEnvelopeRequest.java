package com.saaspe.docusign.model;

import java.util.List;

import lombok.Data;

@Data
public class DeleteEnvelopeRequest {

	private String envelopeId;
	private List<String> delete_id;
}
