package com.testing.jiramate.core;

import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.domain.Issue;
import com.google.gson.Gson;
import com.testing.jiramate.mapper.JiraIssueMapper;
import com.testing.jiramate.mapper.JiraMateMapper;
import com.testing.jiramate.utils.JiraUtil;
import com.testing.jiramate.utils.TimeUtils;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;

/**
 * @Author Waynexu
 */
@Slf4j
@Component
@Getter
public class JiraMate {

    @Autowired
    private JiraMateMapper jiraMateMapper;
    @Autowired
    private JiraIssueMapper jiraIssueMapper;
    @Autowired
    private TimeUtils timeUtils;

    private static Gson gson = new Gson();
    private final static String USERNAME = "git***min";
    private final static String PASSWORD = "git***min";

    private String jiraJql;
    private String jiraJqlForAll;
    private String[] dateRange;
    private String[] employees;
    private ArrayList<String> allIssueId;

    public ArrayList<String> getAllIssueId() {
        return jiraIssueMapper.getAllIssueId();
    }

    /**
     * 设置采集jira缺陷的时间范围
     *
     * @return
     */
    public String[] setRangeDate(DateTime curDate, int offSetDays) throws ParseException {
        String datePattern = "yyyy-MM-dd";
        String sTime = timeUtils.dateAdd(curDate.toDate(), offSetDays);
        String eTime = timeUtils.dateFormat(curDate.toDate(), datePattern);
        eTime = timeUtils.dateAdd(new SimpleDateFormat(datePattern).parse(eTime), 1);
        return this.dateRange = new String[]{sTime, eTime};
    }

    /**
     * 设置员工工号
     *
     * @return
     */
    public String[] setEmployees(String deptName) {
        return this.employees = jiraMateMapper.getEmployeesByDeptName(deptName);
    }

    /**
     * 生成 查询缺陷的 JQL 语句
     *
     * @param dateRange
     * @param employees
     * @return
     */
    public String setJql(String[] dateRange, String[] employees) {
        return this.jiraJql = "issuetype = \"缺陷\" AND updated >= " + dateRange[0] + " AND reporter in (" + Arrays.toString(employees).replaceAll("\\[", "").replaceAll("\\]", "") + ")";
    }

    /**
     * 生成 查询缺陷的 JQL 语句 (分时段)
     *
     * @param dateRange
     * @param employees
     * @return
     */
    public String setJqlForAll(String[] dateRange, String[] employees) {
        return this.jiraJqlForAll = "issuetype = \"缺陷\" AND updated >= " + dateRange[0] + " AND updated <= " + dateRange[1] + " AND reporter in (" + Arrays.toString(employees).replaceAll("\\[", "").replaceAll("\\]", "") + ")";
    }

    /**
     * 将各个部门的数据封装到 ArrayList 中
     *
     * @param jiraUtil
     * @param jiraJQL
     * @return
     */
    public ArrayList<LinkedHashMap<String, String>> queryJiraIssue(JiraUtil jiraUtil, String jiraJQL) throws JSONException {
        JiraRestClient jiraRestClient = jiraUtil.login_jira(USERNAME, PASSWORD);
        ArrayList<String> searchByJql = jiraUtil.search_jql(jiraRestClient, jiraJQL);
        ArrayList<Issue> issuesForJira = new ArrayList<>();
        for (String jiraId : searchByJql) {
            issuesForJira.add(jiraUtil.getIssue(jiraRestClient, jiraId));
        }
        if (this.getDateRange() != null) {
            log.info(Arrays.toString(this.getDateRange()) + " 区间内，采集到本部门缺陷总数: " + issuesForJira.size());
        } else {
            log.info("此时段，采集到本部门缺陷总数: " + issuesForJira.size());
        }
        ArrayList<LinkedHashMap<String, String>> issuesJiraRefactor = new ArrayList<>();
        for (Issue issue : issuesForJira) {
            LinkedHashMap<String, String> issueOneByOne = new LinkedHashMap<>();
            issueOneByOne.put("JIRA单号", issue.getKey());
            if (issue.getFieldByName("所属项目ID").getValue() != null) {
                issueOneByOne.put("项目编号", issue.getFieldByName("所属项目ID").getValue().toString());
            } else {
                issueOneByOne.put("项目编号", "未立项(ITP中不存在的项目)");
            }
            if (issue.getFieldByName("所属项目中文名称").getValue() != null) {
                issueOneByOne.put("项目名称", issue.getFieldByName("所属项目中文名称").getValue().toString());
            } else {
                issueOneByOne.put("项目名称", "未知项目名称");
            }
            issueOneByOne.put("项目系统名称", issue.getProject().getName());
            issueOneByOne.put("标题", issue.getSummary());
            issueOneByOne.put("报告人", issue.getReporter().getDisplayName().split(" ")[0]);
            issueOneByOne.put("缺陷状态", issue.getStatus().getName());
            issueOneByOne.put("缺陷等级", new JSONObject(issue.getFieldByName("缺陷等级").getValue().toString()).getString("value"));
            log.debug(issue.getKey());
            // 取 开发修复 时间
            if (issue.getFieldByName("缺陷修复周期").getValue() != null) {
                issueOneByOne.put("开发修复时效", issue.getFieldByName("缺陷修复周期").getValue().toString());
            } else {
                issueOneByOne.put("开发修复时效", "0.0");
            }
            // 取 测试验证 时间
            if (issue.getFieldByName("缺陷验证周期").getValue() != null) {
                issueOneByOne.put("测试验证时效", issue.getFieldByName("缺陷验证周期").getValue().toString());
            } else {
                issueOneByOne.put("测试验证时效", "0.0");
            }
            // 取 开发修复 时间(工作时间)
            if (issue.getFieldByName("修复周期(工作时间)").getValue() != null) {
                issueOneByOne.put("开发修复时效(工作时间)", issue.getFieldByName("修复周期(工作时间)").getValue().toString());
            } else {
                issueOneByOne.put("开发修复时效(工作时间)", "0.0");
            }
            // 取 测试验证 时间(工作时间)
            if (issue.getFieldByName("验证周期(工作时间)").getValue() != null) {
                issueOneByOne.put("测试验证时效(工作时间)", issue.getFieldByName("验证周期(工作时间)").getValue().toString());
            } else {
                issueOneByOne.put("测试验证时效(工作时间)", "0.0");
            }
            issueOneByOne.put("创建时间", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(issue.getCreationDate().toDate()));
            issueOneByOne.put("更新时间", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(issue.getUpdateDate().toDate()));
            if (issue.getAssignee() != null) {
                issueOneByOne.put("责任人", issue.getAssignee().getDisplayName().split(" ")[0]);
            } else {
                issueOneByOne.put("责任人", "未指定");
            }
            issueOneByOne.put("重开次数", issue.getFieldByName("重开次数").getValue().toString());

            issuesJiraRefactor.add(issueOneByOne);
        }
        return issuesJiraRefactor;
    }
}
