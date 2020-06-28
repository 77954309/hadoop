package org.apache.hadoop.ipc.test;

import org.apache.hadoop.ipc.ProtocolSignature;

import java.io.IOException;

/**
 * @Classname TestClientProtocolImpl
 * @Description TODO
 * @Date 2020/6/28 20:48
 * @Created by limeng
 */
public class TestClientProtocolImpl implements TestClientProtocol {
    @Override
    public String echo(String value) throws IOException {
        return value;
    }

    @Override
    public int add(int v1, int v2) throws IOException {
        return v1+v2;
    }

    @Override
    public long getProtocolVersion(String protocol, long clientVersion) throws IOException {
        return TestClientProtocol.versionID;
    }

    @Override
    public ProtocolSignature getProtocolSignature(String protocol, long clientVersion, int clientMethodsHash) throws IOException {
        return new ProtocolSignature(TestClientProtocol.versionID,null);
    }
}
