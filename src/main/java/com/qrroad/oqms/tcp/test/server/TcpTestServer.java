package com.qrroad.oqms.tcp.test.server;

import com.qrroad.oqms.tcp.test.config.TcpTestProperties;
import com.qrroad.oqms.tcp.test.handler.MessageHandler;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jpos.iso.ISOPackager;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class TcpTestServer {

    private final TcpTestProperties properties;
    private final ISOPackager packager;
    private final MessageHandler messageHandler;

    private ServerSocket serverSocket;
    private ExecutorService executorService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (properties.getServer().isAutoStart()) {
            startServer();
        }
    }

    @Async
    public void startServer() {
        if (running.get()) {
            log.warn("TCP Test Server is already running");
            return;
        }

        try {
            TcpTestProperties.Server serverConfig = properties.getServer();
            serverSocket = new ServerSocket(serverConfig.getPort(),
                    serverConfig.getBacklog());

            executorService = Executors.newCachedThreadPool(r -> {
                Thread t = new Thread(r, "tcp-test-server-worker");
                t.setDaemon(true);
                return t;
            });

            running.set(true);

            log.info("TCP Test Server started on {}:{}",
                    serverConfig.getHost(), serverConfig.getPort());

            while (running.get() && !serverSocket.isClosed()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    log.info("New client connected: {}",
                            clientSocket.getRemoteSocketAddress());

                    executorService.submit(new ClientHandler(
                            clientSocket, packager, messageHandler, properties));

                } catch (IOException e) {
                    if (running.get()) {
                        log.error("Error accepting client connection", e);
                    }
                }
            }

        } catch (IOException e) {
            log.error("Failed to start TCP Test Server", e);
        }
    }

    @PreDestroy
    public void stopServer() {
        log.info("Stopping TCP Test Server...");
        running.set(false);

        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException e) {
                log.warn("Error closing server socket", e);
            }
        }

        if (executorService != null) {
            executorService.shutdown();
        }

        log.info("TCP Test Server stopped");
    }

    public boolean isRunning() {
        return running.get();
    }
}
