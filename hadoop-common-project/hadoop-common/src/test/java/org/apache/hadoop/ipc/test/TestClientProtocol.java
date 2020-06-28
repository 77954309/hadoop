package org.apache.hadoop.ipc.test;

import org.apache.hadoop.ipc.VersionedProtocol;

import java.io.IOException;

/**
 * @Classname in
 * @Description TODO
 * @Date 2020/6/28 20:41
 * @Created by limeng
 */
public interface TestClientProtocol extends VersionedProtocol {
    //版本号，默认情况下，不同版本号Client 和Server之间不能相互通信
    public static final long versionID = 1L;
    String echo(String value) throws IOException;
    int add(int v1,int v2) throws IOException;
}
