package com.xxl.job.admin.core.alarm.impl;

import com.xxl.job.admin.core.alarm.JobAlarm;
import com.xxl.job.admin.core.conf.XxlJobAdminConfig;
import com.xxl.job.admin.core.model.XxlJobGroup;
import com.xxl.job.admin.core.model.XxlJobInfo;
import com.xxl.job.admin.core.model.XxlJobLog;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.util.DateUtil;
import com.xxl.job.core.util.XxlJobRemotingUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * 飞书消息通知
 *
 * @author zhongjun
 * @date 2023/2/8
 **/
@Component
public class FeiShuJobAlarm implements JobAlarm {
    private static final Logger logger = LoggerFactory.getLogger(FeiShuJobAlarm.class);

    @Override
    public boolean doAlarm(XxlJobInfo info, XxlJobLog jobLog) {
        XxlJobGroup group = XxlJobAdminConfig.getAdminConfig().getXxlJobGroupDao().load(info.getJobGroup());
        if (group != null && group.getTitle().contains("!SN!")) {
            logger.info("当前Group报警被禁用，不发送消息");
            return false;
        }
        String feiShuWebHook = XxlJobAdminConfig.getAdminConfig().getFeiShuWebHook();
        if (feiShuWebHook == null || feiShuWebHook.isEmpty() || feiShuWebHook.trim().isEmpty()) {
            logger.info("飞书webhook为空，不发送飞书消息");
            return false;
        }
        String triggerMsg = jobLog.getTriggerMsg();
        if (triggerMsg != null) {
            triggerMsg = triggerMsg.replace("\"", "'");
        }
        String handleMsg = jobLog.getHandleMsg();
        if (handleMsg != null) {
            handleMsg = handleMsg.replace("\"", "'");
        }
        String body = String.format("{\"msg_type\":\"interactive\",\"card\":{\"config\":{\"wide_screen_mode\":true},\"header\":{\"template\":\"red\",\"title\":{\"tag\":\"plain_text\",\"content\":\"XXL-JOB任务调度中心监控报警\"}},\"elements\":[{\"fields\":[{\"is_short\":true,\"text\":{\"content\":\"**时间**：%s\",\"tag\":\"lark_md\"}},{\"is_short\":true,\"text\":{\"content\":\"**服务**：%s\",\"tag\":\"lark_md\"}}],\"tag\":\"div\"},{\"fields\":[{\"is_short\":true,\"text\":{\"content\":\"**任务ID**：%s\",\"tag\":\"lark_md\"}},{\"is_short\":true,\"text\":{\"content\":\"**任务描述**：%s\",\"tag\":\"lark_md\"}}],\"tag\":\"div\"},{\"fields\":[{\"is_short\":true,\"text\":{\"content\":\"**参数**：%s\",\"tag\":\"lark_md\"}},{\"is_short\":true,\"text\":{\"content\":\"**LogId**：%s\",\"tag\":\"lark_md\"}}],\"tag\":\"div\"},{\"fields\":[{\"is_short\":true,\"text\":{\"content\":\"**负责人**： %s\",\"tag\":\"lark_md\"}},{\"is_short\":true,\"text\":{\"content\":\"**执行器**：%s\",\"tag\":\"lark_md\"}}],\"tag\":\"div\"},{\"tag\":\"div\",\"text\":{\"content\":\"**调度信息**：\\n %s\",\"tag\":\"lark_md\"}},{\"tag\":\"div\",\"text\":{\"content\":\"**执行信息**：\\n %s\",\"tag\":\"lark_md\"}}]}}",
                DateUtil.formatDateTime(new Date()),
                XxlJobAdminConfig.getAdminConfig().getAppName(),
                info.getId(),
                info.getJobDesc() + "-" + info.getExecutorHandler(),
                jobLog.getExecutorParam(),
                jobLog.getId(),
                info.getAuthor(),
                group == null ? "" : group.getTitle(),
                triggerMsg,
                handleMsg);
        body = body.replace("\n", "\\n").replace("\t", "\\t").replace("<br>", "\\n");
        logger.info("Send FeiShu Msg Url:{} Request：{}", feiShuWebHook, body);
        ReturnT result = XxlJobRemotingUtil.postBody(feiShuWebHook, null, 3, body, String.class);
        logger.info("Send FeiShu Msg  Response：{}", result);
        return result.isSuccess();
    }

}
