package com.saaspe.docusign.model;

import java.util.List;

import lombok.Data;

@Data
public class DocusignUser {
	private List<Users> users;
	private UserPaging paging;
}
