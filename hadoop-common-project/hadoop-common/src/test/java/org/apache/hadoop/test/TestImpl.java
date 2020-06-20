package org.apache.hadoop.test;

import org.apache.hadoop.ipc.ProtocolSignature;

import java.io.IOException;

/**
 * @Classname TestImpl
 * @Description TODO
 * @Date 2020/5/22 20:01
 * @Created by limeng
 */
public class TestImpl implements TestProtocol {

    @Override
    public String echo(String value) throws IOException {
        return value;
    }

    @Override
    public long getProtocolVersion(String protocol, long clientVersion) throws IOException {
        return TestProtocol.versionID;
    }

    @Override
    public ProtocolSignature getProtocolSignature(String protocol, long clientVersion, int clientMethodsHash) throws IOException {
        return new ProtocolSignature(TestProtocol.versionID,null);
    }
}
