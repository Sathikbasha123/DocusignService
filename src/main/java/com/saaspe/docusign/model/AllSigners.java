package com.saaspe.docusign.model;

import lombok.Data;

@Data
public class AllSigners {

    private String name;
    private String displayName;
    private String email;
    private String recipientType;
    private String routingOrder;

}
