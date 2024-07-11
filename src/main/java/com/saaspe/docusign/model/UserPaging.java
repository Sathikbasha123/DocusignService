package com.saaspe.docusign.model;

import lombok.Data;

@Data
public class UserPaging {

	private int result_set_size;
	private int result_set_start_position;
	private int result_set_end_position;
	private int total_set_size;
	private String next;
}
