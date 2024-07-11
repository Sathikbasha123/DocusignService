package com.saaspe.docusign.model;

import lombok.Data;

@Data
public class Reminders {
	private String reminderDelay;
	private String reminderFrequency;
	private Boolean reminderEnabled;

}
