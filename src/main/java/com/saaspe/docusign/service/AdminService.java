package com.saaspe.docusign.service;
import java.util.List;
import java.util.Map;
public interface AdminService {
	Map<String, String> getOrganizations();

	Map<String, List<Map<String, String>>> getUsers(String userEmail, String firstName, String lastName);

	String createDocusignUser(String userEmail, String firstName, String lastName);

	String getDocusignUser();
}
