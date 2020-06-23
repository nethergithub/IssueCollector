package com.testing.jiramate.core;

import com.google.gson.Gson;
import com.testing.jiramate.entities.Departments;
import com.testing.jiramate.entities.JiraIssue;
import com.testing.jiramate.mapper.JiraIssueMapper;
import com.testing.jiramate.utils.JiraUtil;
import com.testing.jiramate.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jettison.json.JSONException;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Slf4j
@Component
public class JiraMateExecutor {
    private Gson gson = new Gson();

    @Autowired
    private JiraMate jiraMate;

    @Autowired
    private JiraIssue jiraIssue;

    @Autowired
    private Departments departments;

    @Autowired
    private JiraIssueMapper jiraIssueMapper;

    @Autowired
    private TimeUtils timeUtils;

    /**
     * 清空缺陷数据库，为全量同步数据做准备
     */
    private void clearAllIssueFromDB() {
        jiraIssueMapper.clearAllIssueForSyncData();
    }

    /**
     * 根据传入的偏差参数，对大时段进行切割处理。
     * 目的为了避免出现大时段缺陷数单次查询超过 1000 条
     *
     * @param startDate
     * @param offset
     * @return
     * @throws ParseException
     */
    private ArrayList<String[]> dispatchDate(String startDate, int offset) throws ParseException {
        String datePattern = "yyyy-MM-dd";
        ArrayList<String[]> dateRange = new ArrayList<>();
        log.info("Start Date:" + startDate);
        // 开始日期至今天数
        int sumdiff = timeUtils.dateBetweenIncludeToday(new SimpleDateFormat(datePattern).parse(startDate), new DateTime().toDate());
        startDate = timeUtils.dateAdd(new SimpleDateFormat(datePattern).parse(startDate), -1);
        String nextDate = timeUtils.dateAdd(new SimpleDateFormat(datePattern).parse(startDate), offset);
        log.info("Next Date:" + nextDate);
        dateRange.add(new String[]{startDate, nextDate});
        startDate = timeUtils.dateAdd(new SimpleDateFormat(datePattern).parse(nextDate), -1);
        while (sumdiff - 1 > offset) {
            log.info("^^^^^^^^^^^^^^^^^^^^^^^");
            log.info("Start Date:" + startDate);
            nextDate = timeUtils.dateAdd(new SimpleDateFormat(datePattern).parse(startDate), offset);
            log.info("Next Date:" + nextDate);
            dateRange.add(new String[]{startDate, nextDate});
            startDate = timeUtils.dateAdd(new SimpleDateFormat(datePattern).parse(nextDate), -1);
            sumdiff = timeUtils.dateBetweenIncludeToday(new SimpleDateFormat(datePattern).parse(nextDate), new DateTime().toDate());
        }
        startDate = timeUtils.dateAdd(new SimpleDateFormat(datePattern).parse(nextDate), -1);
        nextDate = timeUtils.dateAdd(new SimpleDateFormat(datePattern).parse(startDate), sumdiff + 1);
        dateRange.add(new String[]{startDate, nextDate});
        log.info(new Gson().toJson(dateRange));
        return dateRange;
    }

    /**
     * 实时采集流程
     *
     * @param runParams
     * @throws ParseException
     * @throws JSONException
     */
    public void issuesRealTimeCollector(String... runParams) throws ParseException, JSONException {
        int collectNewIssueTotal = 0;
        LinkedHashMap<String, ArrayList<?>> fetchedIssuesByRange = new LinkedHashMap<>();
        int fetchInterval = 0 - Integer.parseInt(runParams[1]);
        jiraMate.setRangeDate(new DateTime(), fetchInterval);
        log.info("采集规则：每 " + runParams[2] + " 小时，采集 " + Arrays.toString(jiraMate.getDateRange()) + " 区间内的所有缺陷数据");
        for (String dept : departments.getDepartment()) {
            log.info("----------------< " + dept + " [START] >----------------");
            jiraMate.setEmployees(dept);
            log.info("参与统计成员工号：" + Arrays.toString(jiraMate.getEmployees()));
            if (jiraMate.getEmployees().length == 0) {
                log.warn(dept + " 没有成员！");
                continue;
            }
            jiraMate.setJql(jiraMate.getDateRange(), jiraMate.getEmployees());
            log.debug("查询语句：" + jiraMate.getJiraJql());
            fetchedIssuesByRange.put(dept, jiraMate.queryJiraIssue(new JiraUtil(), jiraMate.getJiraJql()));
            if (runParams[0].equals("-realtime")) {
                // 写库操作
                collectNewIssueTotal += writeIssueDataToDB(fetchedIssuesByRange, runParams[2]);
            } else if (runParams[0].equals("-test")) {
                log.info(gson.toJson(fetchedIssuesByRange));
                collectNewIssueTotal += fetchedIssuesByRange.size();
            }
            fetchedIssuesByRange.clear();
            log.info("----------------< " + dept + " [END] >----------------");
        }
        log.info("最近 " + runParams[2] + " 小时所有部门新增：" + collectNewIssueTotal + " 条缺陷。");
    }

    /**
     * 采集本年度至今所有缺陷
     *
     * @param StartDate
     * @param offSetNum
     * @throws ParseException
     */
    public void getAllIssueForThisYear(String StartDate, int offSetNum) throws ParseException, JSONException {
        // 清库操作
        log.warn("清除缺陷库中所有数据 ！！！");
        clearAllIssueFromDB();
        log.warn("清除缺陷库 OK! 准备全量同步.... ！！！");
        int collectNewIssueTotal = 0;
        LinkedHashMap<String, ArrayList<?>> fetchedIssuesByRange = new LinkedHashMap<>();
        int sumDays = timeUtils.dateBetweenIncludeToday(
                new SimpleDateFormat("yyyy-MM-dd").parse(StartDate),
                new DateTime().toDate());
        if (sumDays > offSetNum) {
            log.info("该时段属于大时段，需要切割...");
            ArrayList<String[]> dateRange = this.dispatchDate(StartDate, offSetNum);
            for (String dept : departments.getDepartment()) {
                log.info("----------------< " + dept + " [START] >----------------");
                jiraMate.setEmployees(dept);
                log.info(dept + "部门成员：" + Arrays.toString(jiraMate.getEmployees()));
                if (jiraMate.getEmployees().length == 0) {
                    log.warn(dept + " 没有成员！");
                    continue;
                }
                for (String[] dr : dateRange) {
                    jiraMate.setJqlForAll(dr, jiraMate.getEmployees());
                    log.info("查询语句：" + jiraMate.getJiraJqlForAll());
                    fetchedIssuesByRange.put(dept, jiraMate.queryJiraIssue(new JiraUtil(), jiraMate.getJiraJqlForAll()));
                    // 写库操作
                    collectNewIssueTotal += writeIssueDataToDB(fetchedIssuesByRange, "0");
                }
                fetchedIssuesByRange.clear();
                log.info("----------------< " + dept + " [END] >----------------");
            }
            log.info("总共采集：" + collectNewIssueTotal + " 条缺陷。");
        } else {
            log.info("本年已过天数小于设置的偏差值。直接采集...");
            System.out.println("本年已过天数小于设置的偏差值。请以实时采集方式运行...");
            System.exit(2);
        }
    }

    /**
     * 处理采集到的数据待入库
     *
     * @param issueData
     * @return
     */
    private int writeIssueDataToDB(LinkedHashMap<String, ArrayList<?>> issueData, String interval) {
        int dataCnt = 0;
        for (Map.Entry<String, ArrayList<?>> entry : issueData.entrySet()) {
            jiraIssue.setDepartment(entry.getKey());
            // 获取当前库中所有缺陷单号
            ArrayList<String> currentAllIssueId = jiraIssueMapper.getAllIssueId();
            ArrayList<LinkedHashMap<String, String>> issueValue = (ArrayList<LinkedHashMap<String, String>>) entry.getValue();
            for (LinkedHashMap<String, String> entryin : issueValue) {
                String issueId = entryin.get("JIRA单号");
                String issueRp = entryin.get("报告人");
                String issueSt = entryin.get("缺陷状态");
                String issueLe = entryin.get("缺陷等级");
                String issueTt = entryin.get("测试验证时效(工作时间)");
                String issueDt = entryin.get("开发修复时效(工作时间)");
                if (currentAllIssueId.contains(issueId)) {
                    log.debug("已经存在的单号：" + issueId + " ,准备更新此缺陷单。");
                    jiraIssueMapper.deleteExistsIssue(issueId);
                } else {
                    log.info("缺陷简介：[" + issueId + "] [" + issueRp + "] [" + issueSt + "] [" + issueLe + "] [" + issueTt + "](测试验证时效)" + "] [" + issueDt + "](开发修复时效)");
                    dataCnt++;
                }
                jiraIssue.setIssueprojid(entryin.get("项目编号"));
                jiraIssue.setIssueprojname(entryin.get("项目名称"));
                jiraIssue.setIssuesysname(entryin.get("项目系统名称"));
                jiraIssue.setIssueid(entryin.get("JIRA单号"));
                jiraIssue.setIssuetitle(entryin.get("标题"));
                jiraIssue.setUsername(entryin.get("报告人"));
                jiraIssue.setIssuestatus(entryin.get("缺陷状态"));
                jiraIssue.setIssuelevel(entryin.get("缺陷等级"));
                jiraIssue.setDevcircle(entryin.get("开发修复时效"));
                jiraIssue.setDevcircle_infact(entryin.get("开发修复时效(工作时间)"));
                jiraIssue.setTestingcircle(entryin.get("测试验证时效"));
                jiraIssue.setTestingcircle_infact(entryin.get("测试验证时效(工作时间)"));
                jiraIssue.setIssuecreatetime(entryin.get("创建时间"));
                jiraIssue.setIssueupdatetime(entryin.get("更新时间"));
                jiraIssue.setResponsibleman(entryin.get("责任人"));
                jiraIssue.setReopentimes(entryin.get("重开次数"));
                // 写入数据库
                jiraIssueMapper.writeToDB(jiraIssue);
            }
            if (!interval.equals("0")) {
                log.info("最近 " + interval + " 小时部门新增缺陷：" + dataCnt + " 个。");
            }
        }
        return dataCnt;
    }
}
