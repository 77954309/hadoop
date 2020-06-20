package org.apache.hadoop.yarn.service;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.conf.YarnConfiguration;

/**
 * @Classname TestSimpleMRAppMaster2
 * @Description TODO
 * @Date 2020/5/30 18:13
 * @Created by limeng
 */
public class TestSimpleMRAppMaster2 {
    public static void main(String[] args) throws Exception {
        String jobID = "job_20131215_12";
        TestSimpleMRAppMaster appMaster  = new TestSimpleMRAppMaster("Simple MRAppMaster", jobID, 5);
        YarnConfiguration conf = new YarnConfiguration(new Configuration());

        appMaster.serviceInit(conf);
//        appMaster.serviceStart();
//        appMaster.getDispatcher().getEventHandler().handle(new TestJobEvent(jobID, TestJobEventType.JOB_KILL));
//        appMaster.getDispatcher().getEventHandler().handle(new TestJobEvent(jobID, TestJobEventType.JOB_INIT));

    }
}
