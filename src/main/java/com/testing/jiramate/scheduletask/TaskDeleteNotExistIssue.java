package com.testing.jiramate.scheduletask;

import com.atlassian.jira.rest.client.domain.Issue;
import com.testing.jiramate.mapper.JiraIssueMapper;
import com.testing.jiramate.utils.JiraUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Slf4j
@Component
public class TaskDeleteNotExistIssue {

    @Autowired
    private JiraIssueMapper jiraIssueMapper;

    @Scheduled(cron = "0 10 2 ? * SAT")
    public void process() {
        log.info("开始同步数据库与Jira库 Issue 缺陷单...");
        int delcnt = 0;
        ArrayList<String> allIssueKeyFromDB = jiraIssueMapper.getAllIssueId();
        for (String issuekey : allIssueKeyFromDB) {
            try {
                Issue issue = new JiraUtil().getIssue(new JiraUtil().login_jira("gitpAdmin", "gitpAdmin"), issuekey);
                log.info(issuekey + " 单号同步存在于数据库和Jira库。");
            } catch (Exception e) {
                if (e.getMessage().equals("问题不存在")) {
                    log.error("在JIRA中：" + e.getMessage());
                    log.info("准备从数据库中同步删除 " + issuekey);
                    jiraIssueMapper.deleteExistsIssue(issuekey);
                    delcnt++;
                } else {
                    log.error(e.getMessage());
                }
            }
        }
        log.info("从数据库中删除Jira库中不存在的单子数量：" + delcnt);
        log.info("同步结束，等待下个同步周期...");
    }
}
