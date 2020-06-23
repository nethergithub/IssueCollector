package com.testing.jiramate.entities;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.Serializable;

@Data
@Component
public class JiraIssue implements Serializable {
    private Integer id;
    private String department;
    private String username;
    private String issueprojid;
    private String issueprojname;
    private String issuesysname;
    private String issueid;
    private String issuetitle;
    private String issuestatus;
    private String issuelevel;
    private String testingcircle;
    private String testingcircle_infact;
    private String devcircle;
    private String devcircle_infact;
    private String issuecreatetime;
    private String issueupdatetime;
    private String responsibleman;
    private String reopentimes;

    //Getter & Setter ...
}
