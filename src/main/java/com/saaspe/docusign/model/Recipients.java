package com.saaspe.docusign.model;

import lombok.Data;

import java.util.List;

@Data
public class Recipients {

	private List<Signer> signers;
	private List<Carboncopy> cc;
}
