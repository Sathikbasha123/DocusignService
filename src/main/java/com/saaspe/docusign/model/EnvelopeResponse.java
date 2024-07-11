package com.saaspe.docusign.model;

import java.util.List;

import com.docusign.esign.model.Envelope;

import lombok.Data;

@Data
public class EnvelopeResponse {

	private Envelope envelope;

	private String envelopeId;

	private List<DocumentResponse> documents;

}
