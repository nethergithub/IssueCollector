package com.testing.jiramate.utils;

import com.atlassian.jira.rest.client.JiraRestClient;
import com.atlassian.jira.rest.client.NullProgressMonitor;
import com.atlassian.jira.rest.client.domain.BasicIssue;
import com.atlassian.jira.rest.client.domain.Issue;
import com.atlassian.jira.rest.client.domain.SearchResult;
import com.atlassian.jira.rest.client.internal.jersey.JerseyJiraRestClientFactory;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;

@Slf4j
public class JiraUtil {

    private final static String jiraURL = "http://newjira.cnsuning.com";
    private final static JerseyJiraRestClientFactory factory = new JerseyJiraRestClientFactory();
    private final static NullProgressMonitor pm = new NullProgressMonitor();

    /**
     * 登录JIRA并返回指定的JiraRestClient对象
     *
     * @param username
     * @param password
     * @return
     */
    public JiraRestClient login_jira(String username, String password) {
        try {
            final URI jiraServerUri = new URI(jiraURL);
            return factory.createWithBasicHttpAuthentication(jiraServerUri, username, password);
        } catch (Exception e) {
            e.getStackTrace();
            log.error(e.getMessage());
        }
        return null;
    }

    /**
     * 通过jql语句进行查询并返回一个包含issue的key的数组
     *
     * @param jql
     * @return
     */
    public ArrayList<String> search_jql(JiraRestClient restClient, String jql) {
        SearchResult searchResult = restClient.getSearchClient().searchJql(jql, 5000, 0, pm);
        Iterator<BasicIssue> basicIssues = searchResult.getIssues().iterator();
        ArrayList<String> issueKeys = new ArrayList<>();
        while (basicIssues.hasNext()) {
            String issueKey = basicIssues.next().getKey();
            issueKeys.add(issueKey);
        }
        return issueKeys;
    }

    /**
     * Get issue through issueKey
     *
     * @param restClient
     * @param issueKey
     * @return
     */
    public Issue getIssue(JiraRestClient restClient, String issueKey) {
        return restClient.getIssueClient().getIssue(issueKey, pm);
    }
}
