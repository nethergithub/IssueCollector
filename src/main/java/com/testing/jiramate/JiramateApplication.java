package com.testing.jiramate;

import com.testing.jiramate.core.JiraMateExecutor;
import com.testing.jiramate.utils.TimeUtils;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jettison.json.JSONException;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Scanner;

@SpringBootApplication
@EntityScan("com.testing.jiramate.entities")
@Slf4j
@EnableScheduling
public class JiramateApplication implements CommandLineRunner {

    @Autowired
    private JiraMateExecutor jiraMateExecutor;

    @Autowired
    private TimeUtils timeUtils;

    public static void main(String[] args) {
        SpringApplication springApplication = new SpringApplication(JiramateApplication.class);
        springApplication.setBannerMode(Banner.Mode.OFF);
        springApplication.run(args);
    }

    @Override
    public void run(String... args) throws ParseException, InterruptedException, JSONException {
        String showTimeFormat = "yyyy-MM-dd HH:mm:ss";
        Calendar calendar = Calendar.getInstance();
        while (true) {
            if (args.length == 3 && "-realtime".equals(args[0])) {
                log.info("准备根据规则实时采集 ...");
                log.info("Start Fetching Jira Issues ...");
                long fetchingInterval = Long.parseLong(args[2]) * 3600 * 1000;
                String[] runParams = { args[0], args[1], args[2] };
                jiraMateExecutor.issuesRealTimeCollector(runParams);
                log.info("Ending Mission!");
                log.info("本次采集完毕，下次运行时间为：" + new SimpleDateFormat(showTimeFormat)
                        .format(timeUtils.dateAddHours(new DateTime().toDate(), Integer.parseInt(args[2]))));
                log.info("采集机器人休息中 ... ...");
                Thread.sleep(fetchingInterval);
            } else if (args.length == 4 && "-realtime".equals(args[0]) && "-once".equals(args[3])) {
                log.info("准备根据规则实时采集 ...");
                log.info("Start Fetching Jira Issues ...");
                String[] runParams = { args[0], args[1], args[2] };
                jiraMateExecutor.issuesRealTimeCollector(runParams);
                log.info("Ending Mission!");
                System.exit(0);
            } else if (args.length == 3 && "-test".equals(args[0])) {
                log.info("准备根据规则实时采集 (测试，不入库) ...");
                log.info("Start Fetching Jira Issues ...");
                long fetchingInterval = Long.parseLong(args[2]) * 3600 * 1000;
                String[] runParams = { args[0], args[1], args[2] };
                jiraMateExecutor.issuesRealTimeCollector(runParams);
                log.info("Ending Mission!");
                log.info("本次采集完毕，下次运行时间为：" + new SimpleDateFormat(showTimeFormat)
                        .format(timeUtils.dateAddHours(new DateTime().toDate(), Integer.parseInt(args[2]))));
                log.info("采集机器人休息中 ... ...");
                Thread.sleep(fetchingInterval);
            } else if (args.length == 2 && "-all".equals(args[0]) && Integer.parseInt(args[1]) > 0) {
                log.info("准备全量更新 ...");
                System.out.print("全量同步将清空之前的缺陷库所有数据，你确定这样操作吗？(Yes/No)");
                Scanner scanner = new Scanner(System.in);
                String inputString = scanner.nextLine();
                scanner.close();
                if (inputString == null ) {
                    inputString = "No";
                }
                if (!inputString.toUpperCase().equals("Y") && !inputString.toUpperCase().equals("YES")) {
                    log.info("取消全量更新操作！");
                } else {
                    log.info("Start Fetching *ALL* Jira Issues ...");
                    jiraMateExecutor.getAllIssueForThisYear(calendar.get(Calendar.YEAR) + "-01-01",
                            Integer.parseInt(args[1]));
                    log.info("Ending Mission!");
                }
                System.exit(0);
            } else {
                log.error("参数错误！");
                System.out.println("运行示例:\n" + "\n"
                        + "\t1. java -jar 程序名称 \"[-realtime|-test]\" \"6\" \"3\" \"[-once]\" 含义：以【实时】或【测试】采集方式执行程序。规则为：每3小时采集一次6天以来的缺陷数据(-once 代表只执行一次规则内采集)。\n"
                        + "\n" + "\t2. java -jar 程序名称 \"-all\" \"15\"    \t\t含义：以全量方式一次性采集当年至今的所有缺陷数据。(15代表时间切割粒度)");
                System.exit(1);
            }
        }
    }
}
