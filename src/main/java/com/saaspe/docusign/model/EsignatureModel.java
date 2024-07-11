package com.saaspe.docusign.model;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EsignatureModel {

    private String signerEmail;
    private String signerName;
    private String ccEmail;
    private String ccName;
    private String documentBase64;
    private String documentId;
    private String documentName;
    private String documentExtension;





}
