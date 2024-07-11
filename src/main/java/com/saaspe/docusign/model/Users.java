package com.saaspe.docusign.model;

import java.util.Date;

import lombok.Data;

@Data
public class Users {

	private String id;
	private String user_name;
	private String first_name;
	private String last_name;
	private String membership_status;
	private String email;
	private Date membership_created_on;
	private String membership_id;
	private String user_status;
	private Date created_on;
	private Date closed_on;
	private Date membership_closed_on;

}
