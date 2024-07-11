package com.saaspe.docusign.model;

import lombok.Data;

@Data
public class Notification {

	private Expiration expirations;

	private Reminders reminders;

	private String useAccountDefaults;

}
