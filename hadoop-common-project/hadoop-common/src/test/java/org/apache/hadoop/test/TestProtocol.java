package org.apache.hadoop.test;

import org.apache.hadoop.ipc.ProtocolSignature;
import org.apache.hadoop.ipc.VersionedProtocol;

import java.io.IOException;

/**
 * @Classname TestProtocol
 * @Description TODO
 * @Date 2020/5/22 20:00
 * @Created by limeng
 */
public interface TestProtocol extends VersionedProtocol {
    public static final long versionID = 1L;

    String echo(String value) throws IOException;
}
