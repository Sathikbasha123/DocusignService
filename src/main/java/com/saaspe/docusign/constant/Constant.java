package com.saaspe.docusign.constant;

public class Constant {

	private Constant() {
		super();
	}

	public static final String ERROR_IN_LIST_ENVELOPE_RECIPIENTS = "[TRACE_ID: {}] Error in listEnvelopeRecipients: {}";
	public static final String ERROR_IN_GET_AUDIT = "[TRACE_ID: {}] Error in getAudit: {}";
	public static final String AUTHORIZATION = "Authorization";
	public static final String ACCEPT = "Accept";
	public static final String APPLICATION_JSON = "application/json";
	public static final String CONTENT_TYPE = "Content-Type";
	public static final String BEARER = "Bearer ";
	public static final String USER_ACCOUNT_ID = "/users?account_id=";
	public static final String USER_NAME = "user_name";
	public static final String FIRST_NAME = "first_name";
	public static final String LAST_NAME = "last_name";
	public static final String MEMBERSHIP_STATUS = "membership_status";
	public static final String EMAIL = "email";
	public static final String MEMBERSHIP_CREATED_ON = "membership_created_on";
	public static final String MEMBERSHIP_ID = "membership_id";
	public static final String USERS = "users";

	public static final String SIGNATURE = "Signature";
	public static final String IMPERSONATION = "impersonation";
	public static final String CONSENT_REQUIRED = "consent_required";
	public static final String REDIRECT_URI = "&redirect_uri=";
	public static final String CONSENT_REQUIRED_PROVIDE_CONSENT = "Consent required, please provide consent in browser window and then run this app again.";
	public static final String DELETE = "delete";
	public static final String ISSUE_IN_GETTING_CONSENT = "Issue in getting consent";
	public static final String JSON_CONTENT_TYPE = "application/json";
	public static final String HEADER_CONTENT_TYPE = "Content-Type";
	public static final String COMMIT_CHUNK = "commit";
	
	public static final String FALSE = "false";
	public static final String X_DOCUSIGN_EDIT = "X-DocuSign-Edit";
	public static final String CERTIFICATE = "certificate";
	public static final String CREATE_FILE = "createFile";
	public static final String UPDATE_FILE = "updateFile";
	public static final String LOCK_HEADER = "{\"LockToken\":\"%s\", \"LockDurationInSeconds\":\"%s\"}";

	public static final String LOA_DOCUMENT_REPOSITORY = "LOA";
	public static final String LOA_CONTRACT_DOCUMENT_REPOSITORY = "LOA_CONTRACT";
	public static final String LOO_CONTRACT_DOCUMENT_REPOSITORY = "LOO";
	public static final String CMU_DOCUMENT_REPOSITORY = "CMU";
	public static final String TA_DOCUMENT_REPOSITORY = "TA";
	
	public static String vendorMessage="Dear Sir/Madam,\n\nTender/RFP/Quotation No : {contractNumber}\n\nFirst and foremost, we wish to congratulate you on this award for the above Contract."
			+ "\nAttached herein a {moduleName} ({contractName}) for the above Works/Services/Supply for your reference and further action.\n\n"
			+ "Hence, please sign the {moduleName} ({contractName}) via our digital signing platform and subsequently proceed with LHDN stamping. The LHDN "
			+ "Certificate is to be uploaded in the same platform  within fourteen (14) days from the date of this {abbreviation}. You may"
			+ " refer to the guidelines as attached for the procedure of the said requirements.\n\n"
			+ "Please read and understand all the requirements and information provided in the {abbreviation} and to comply with all the"
			+ " Conditions Precedent stated. You are further required to extend the cover notes with policies of the relevant insurances"
			+ "(softcopy-via email) and Performance Bond (hardcopy to our office) within twenty one (21) days from the date of this {abbreviation} for our record and further action.\n\n"
			+ "Best regards and thank you\n\n"
			+ "The Information transmitted is intended only for the person or entity to which it is addressed and may contain confidential "
			+ "and/or privileged material. Any review, retransmission, dissemination or other use of, or taking of any action in reliance upon,"
			+ "this information by persons or entities other than the intended recipient is prohibited. If there is any doubt, please refer to the "
			+ "Chief Procurement Officer, MAHB / Senior Managers of Procurement & Contract Division, MAHB for direction before action.\n\n"
			+ "Please rate our service level based on your personal experience by completing our short survey. Kindly click on the following link:\n\n"
			+ "External/ Vendor   - <a href=\""+"https://forms.gle/x6o8T8RznjuDXLo7A"+"\">https://forms.gle/x6o8T8RznjuDXLo7A</a>\n\n"
			+ "MAHB Staff/ Subs - <a href=\""+"https://forms.gle/x6o8T8RznjuDXLo7A"+"\">https://forms.gle/yT69XJRwaFLYGxq59</a>\n\n"
			+ "Hosting Joyful Connections\n"
			+ "<a href=\""+"{videoURL1}"+"\"><div style=\"text-align: center;\"><button style=\"background-color:#29256e; padding:10px; color:white; border:none; border-collapse:collapse!important; border-radius:3px; box-sizing:border-box; min-width:250px;\">Click for {moduleName2} Signing Demo (EN)</button></div></a>\n"
			+ "<a href=\""+"{videoURL2}"+"\"><div style=\"text-align: center;\"><button style=\"background-color:#29256e;padding: 10px; color:white; border:none; border-collapse:collapse!important; border-radius:3px; box-sizing:border-box; min-width:250px;\">Click for {moduleName2} Signing Demo (BM)</button></div></a>";
	
	
	public static String cvefMessage="Dear Sir/Madam,\r\n"
			+ "<div>Kindly seek your kind approval for the CVEF Form, in order for us to obtain the necessary endorsement for the {moduleName} ({contractName}) from the Legal department.</div>\n"
			+ "Thank you.\n";
//			+ "<a href=\""+"{videoURL1}"+"\"><div style=\"text-align: center;\"><button style=\"background-color:#29256e; padding:10px; color:white; border:none; border-collapse:collapse!important; border-radius:3px; box-sizing:border-box; min-width:250px;\">Click for {moduleName} Signing Demo (EN)</button></div></a>\n"
//			+ "<a href=\""+"{videoURL2}"+"\"><div style=\"text-align: center;\"><button style=\"background-color:#29256e;padding: 10px; color:white; border:none; border-collapse:collapse!important; border-radius:3px; box-sizing:border-box; min-width:250px;\">Click for {moduleName} Signing Demo (BM)</button></div></a>";
	
	public static String stamperMessage="Dear Legal team,\r\n"
			+ "<div>Appreciate your kind endorsement for the attached {moduleName} ({contractName}).</div>\n"
			+ "Thank You.\n";
//			+ "<a href=\""+"{videoURL1}"+"\"><div style=\"text-align: center;\"><button style=\"background-color:#29256e; padding:10px; color:white; border:none; border-collapse:collapse!important; border-radius:3px; box-sizing:border-box; min-width:250px;\">Click for {moduleName} Signing Demo (EN)</button></div></a>\n"
//			+ "<a href=\""+"{videoURL2}"+"\"><div style=\"text-align: center;\"><button style=\"background-color:#29256e;padding: 10px; color:white; border:none; border-collapse:collapse!important; border-radius:3px; box-sizing:border-box; min-width:250px;\">Click for {moduleName} Signing Demo (BM)</button></div></a>";
	
	public static String tenantMessage="Dear Sir/Madam,\r\n"
			+ "<div>Please refer to the attached {moduleName} ({contractName}) for your review and action.\n"
			+ "Kindly signify your acceptance by signing both documents and provide the LHDN Stamp Duty Certificate within 14 days from the date of this letter as per the Stamp Act 1949.</div>\n"
			+ "Thank You.\n"
			+ "<a href=\""+"{videoURL1}"+"\"><div style=\"text-align: center;\"><button style=\"background-color:#29256e; padding:10px; color:white; border:none; border-collapse:collapse!important; border-radius:3px; box-sizing:border-box; min-width:250px;\">Click for {moduleName} Signing Demo (EN)</button></div></a>\n"
			+ "<a href=\""+"{videoURL2}"+"\"><div style=\"text-align: center;\"><button style=\"background-color:#29256e;padding: 10px; color:white; border:none; border-collapse:collapse!important; border-radius:3px; box-sizing:border-box; min-width:250px;\">Click for {moduleName} Signing Demo (BM)</button></div></a>";
	
	public static String lhdnMessage="Dear Sir/ Madam,\r\n"
			+ "<div>Appreciate your submission of the LHDN Stamp Duty Certificate  for the specified contract.</div>\n"
			+ "Thank You.\n"
			+ "<a href=\"https://drive.google.com/file/d/1r9i-aQNFaW2u5bWHjsk50PfguGg7MBIN/view?usp=drive_link\"><div style=\"text-align: center;\"><button style=\"background-color:#29256e; padding:10px; color:white; border:none; border-collapse:collapse!important; border-radius:3px; box-sizing:border-box; min-width:250px;\">Click for {moduleName} Signing Demo (EN)</button></div></a>\n"
			+ "<a href=\"https://drive.google.com/file/d/1e8l5qdNRgys0zJAL_xVDLY7liEEW5d2G/view?usp=drive_link\"><div style=\"text-align: center;\"><button style=\"background-color:#29256e;padding: 10px; color:white; border:none; border-collapse:collapse!important; border-radius:3px; box-sizing:border-box; min-width:250px;\">Click for {moduleName} Signing Demo (BM)</button></div></a>";			;
	
	public static String contractCvef="Hello, \r\n"
			+ "You received this email as the designated person to sign on behalf of the respective parties. Please proceed with your signature wherever necessary.\n\n"
			+ "Thank You.";
	
	public static String contractStamper="Dear Legal Admin,\r\n"
			+ "The CVEF circular has been finalized. As such, please proceed with affixing the Legal stamp on each page of the Conditions of Contract (CoC).\n\n"
			+ "Thank You.";
}
