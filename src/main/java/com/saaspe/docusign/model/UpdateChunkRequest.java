package com.saaspe.docusign.model;

import lombok.Data;

@Data
public class UpdateChunkRequest {

	private String chunkedUploadId;
	private String chunkedUploadSeq;
	private String data;
}
