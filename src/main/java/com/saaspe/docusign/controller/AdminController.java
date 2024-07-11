package com.saaspe.docusign.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.saaspe.docusign.aspect.ControllerLogging;
import com.saaspe.docusign.service.AdminService;

@RestController
@ControllerLogging
@CrossOrigin
public class AdminController {

	@Autowired
	AdminService adminService;

	@Value("${clientId}")
	public String clientId;

	@GetMapping("/organization")
	public Map<String, String> getOrganizations() {
		return adminService.getOrganizations();
	}

	@GetMapping("/users")
	public Map<String, List<Map<String, String>>> getUsers(@RequestParam String userEmail,
			@RequestParam String firstName, @RequestParam String lastName) {
		return adminService.getUsers(userEmail, firstName, lastName);
	}

	@GetMapping("/create/user")
	public String createUsers(@RequestParam String userEmail, @RequestParam String firstName,
			@RequestParam String lastName) {
		return adminService.createDocusignUser(firstName, lastName, userEmail);
	}

	@GetMapping("/get/user")
	public String getUsers() {
		return adminService.getDocusignUser();
	}

	@GetMapping("/get/clientId")
	public String getClientID() {
		return clientId;
	}

}
