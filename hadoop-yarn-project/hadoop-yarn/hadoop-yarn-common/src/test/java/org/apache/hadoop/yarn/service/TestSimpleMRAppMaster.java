package org.apache.hadoop.yarn.service;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.service.CompositeService;
import org.apache.hadoop.service.Service;
import org.apache.hadoop.yarn.event.AsyncDispatcher;
import org.apache.hadoop.yarn.event.Dispatcher;
import org.apache.hadoop.yarn.event.EventHandler;

/**
 * @Classname TestSimpleMRAppMaster
 * @Description TODO
 * @Date 2020/5/30 17:49
 * @Created by limeng
 */

public class TestSimpleMRAppMaster extends CompositeService {
    private Dispatcher dispatcher;//中央异步调度器
    private String jobID;
    private int taskNumber;//该作业包含的任务数目
    private String[] taskIDs;//该作业内部包含的所有任务



    public TestSimpleMRAppMaster(String name, String jobID, int taskNumber) {
        super(name);
        this.jobID = jobID;
        this.taskNumber = taskNumber;
        this.taskIDs = new String[taskNumber];
        for (int i = 0; i < taskNumber; i++) {
            taskIDs[i] = new String(jobID+"_task_"+i);
        }
    }


    @Override
    public void serviceInit(final Configuration conf) throws Exception {
        dispatcher = new AsyncDispatcher();
        //注册Job和Task事件调度器
        dispatcher.register(TestJobEventType.class,new JobEventDispatcher());
        dispatcher.register(TestTaskEventType.class,new TaskEventDispatcher());
        addService((Service)dispatcher);
        super.serviceInit(conf);
    }

    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    private class JobEventDispatcher implements EventHandler<TestJobEvent> {

        @Override
        public void handle(TestJobEvent event) {
            if(event.getType() == TestJobEventType.JOB_KILL){
                System.out.println("Receive JOB_KILL event, killing all the tasks");
                for (int i = 0; i < taskNumber; i++) {
                    dispatcher.getEventHandler().handle(new TestTaskEvent(taskIDs[i], TestTaskEventType.T_KILL));
                }
            }else if(event.getType() == TestJobEventType.JOB_INIT){
                System.out.println("Receive JOB_INIT event, scheduling tasks");
                for (int i = 0; i < taskNumber; i++) {
                    dispatcher.getEventHandler().handle(new TestTaskEvent(taskIDs[i], TestTaskEventType.T_SCHEDULE));
                }
            }
        }
    }

    private class TaskEventDispatcher implements EventHandler<TestTaskEvent> {
        @Override
        public void handle(TestTaskEvent event) {
            if (event.getType() == TestTaskEventType.T_KILL) {
                System.out.println("Receive T_KILL event of task" + event.getTaskID());
            } else if (event.getType() == TestTaskEventType.T_SCHEDULE) {
                System.out.println("Receive T_SCHEDULE of task" + event.getTaskID());
            }
        }
    }
}
