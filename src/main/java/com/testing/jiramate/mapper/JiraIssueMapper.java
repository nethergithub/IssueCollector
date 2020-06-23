package com.testing.jiramate.mapper;

import com.testing.jiramate.entities.JiraIssue;
import org.apache.ibatis.annotations.*;

import java.util.ArrayList;

@Mapper
public interface JiraIssueMapper {

    @Insert({"INSERT INTO jiraissues.t_issues (department,username,issueprojid,issueprojname,issuesysname,issueid,issuetitle,issuestatus,issuelevel,testingcircle,testingcircle_infact,devcircle,devcircle_infact,issuecreatetime,issueupdatetime,responsibleman,reopentimes) VALUES (#{department},#{username},#{issueprojid},#{issueprojname},#{issuesysname},#{issueid},#{issuetitle},#{issuestatus},#{issuelevel},#{testingcircle},#{testingcircle_infact},#{devcircle},#{devcircle_infact},#{issuecreatetime},#{issueupdatetime},#{responsibleman},#{reopentimes})"})
    int writeToDB(JiraIssue jiraIssue);

    @Delete({"DELETE FROM jiraissues.t_issues WHERE issueid = #{issueid}"})
    void deleteExistsIssue(@Param("issueid") String issueid);

    @Select({"SELECT issueid FROM jiraissues.t_issues"})
    ArrayList<String> getAllIssueId();

    @Delete({"TRUNCATE jiraissues.t_issues"})
    void clearAllIssueForSyncData();

}
