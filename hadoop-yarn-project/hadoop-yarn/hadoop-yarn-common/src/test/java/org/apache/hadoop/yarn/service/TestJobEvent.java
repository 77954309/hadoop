package org.apache.hadoop.yarn.service;

import org.apache.hadoop.yarn.event.AbstractEvent;

/**
 * @Classname TestJobEvent
 * @Description TODO
 * @Date 2020/5/30 17:47
 * @Created by limeng
 */
public class TestJobEvent extends AbstractEvent<TestJobEventType> {
    private String jobID;

    public TestJobEvent (String jobID, TestJobEventType type) {
        super(type);
        this.jobID = jobID;
    }

    public TestJobEvent(TestJobEventType testJobEventType) {
        super(testJobEventType);
    }

    public TestJobEvent(TestJobEventType testJobEventType, long timestamp) {
        super(testJobEventType, timestamp);
    }
}
