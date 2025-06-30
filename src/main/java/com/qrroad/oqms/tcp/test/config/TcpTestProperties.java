package com.qrroad.oqms.tcp.test.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "tcp.test")
public class TcpTestProperties {
    private Server server = new Server();
    private Client client = new Client();

    @Data
    public static class Server {
        private int port = 8583;
        private String host = "0.0.0.0";
        private int backlog = 100;
        private boolean autoStart = true;
        private int messageHeaderLength = 2;
        private String encoding = "UTF-8";
    }

    @Data
    public static class Client {
        private String targetHost = "localhost";
        private int targetPort = 8583;
        private int connectTimeoutMs = 30000;
        private int readTimeoutMs = 60000;
        private boolean keepAlive = true;
        private int messageHeaderLength = 2;
        private String encoding = "UTF-8";
    }
}
