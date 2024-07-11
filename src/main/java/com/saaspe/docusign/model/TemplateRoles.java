package com.saaspe.docusign.model;

import lombok.Data;

@Data
public class TemplateRoles {
	private String email;
	private String name;
	private String roleName;
	private String routingOrder;
	private Tabs tabs;

}
