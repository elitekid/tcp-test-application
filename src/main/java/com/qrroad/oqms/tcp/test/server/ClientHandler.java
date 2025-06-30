package com.qrroad.oqms.tcp.test.server;

import com.qrroad.oqms.tcp.test.config.TcpTestProperties;
import com.qrroad.oqms.tcp.test.handler.MessageHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;

import java.io.*;
import java.net.Socket;

@Slf4j
@RequiredArgsConstructor
public class ClientHandler implements Runnable {

    private final Socket clientSocket;
    private final ISOPackager packager;
    private final MessageHandler messageHandler;
    private final TcpTestProperties properties;

    @Override
    public void run() {
        String clientInfo = clientSocket.getRemoteSocketAddress().toString();
        log.info("Client handler started for: {}", clientInfo);

        try (InputStream inputStream = clientSocket.getInputStream();
             OutputStream outputStream = clientSocket.getOutputStream()) {

            while (!clientSocket.isClosed()) {
                try {
                    // 메시지 수신
                    ISOMsg receivedMsg = receiveMessage(inputStream);
                    if (receivedMsg == null) {
                        break;
                    }

                    log.info("Received message from {}: MTI={}, STAN={}",
                            clientInfo, receivedMsg.getMTI(), receivedMsg.getString(11));

                    // 메시지 처리
                    ISOMsg responseMsg = messageHandler.processMessage(receivedMsg);

                    // 응답 전송
                    if (responseMsg != null) {
                        sendMessage(outputStream, responseMsg);
                        log.info("Sent response to {}: MTI={}, STAN={}",
                                clientInfo, responseMsg.getMTI(), responseMsg.getString(11));
                    }

                } catch (Exception e) {
                    log.error("Error processing message for client: {}", clientInfo, e);
                    break;
                }
            }

        } catch (IOException e) {
            log.error("Error handling client: {}", clientInfo, e);
        } finally {
            try {
                clientSocket.close();
                log.info("Client connection closed: {}", clientInfo);
            } catch (IOException e) {
                log.warn("Error closing client socket: {}", clientInfo, e);
            }
        }
    }

    private ISOMsg receiveMessage(InputStream inputStream) throws Exception {
        // 길이 헤더 읽기
        int headerLength = properties.getServer().getMessageHeaderLength();
        byte[] lengthHeader = new byte[headerLength];

        int read = inputStream.read(lengthHeader);
        if (read != headerLength) {
            return null; // 연결 종료
        }

        int messageLength = parseMessageLength(lengthHeader);
        if (messageLength <= 0 || messageLength > 8192) {
            throw new IOException("Invalid message length: " + messageLength);
        }

        // 메시지 본문 읽기
        byte[] messageBytes = new byte[messageLength];
        readFully(inputStream, messageBytes);

        // ISO8583 메시지 언팩
        ISOMsg msg = new ISOMsg();
        msg.setPackager(packager);
        msg.unpack(messageBytes);

        return msg;
    }

    private void sendMessage(OutputStream outputStream, ISOMsg msg) throws Exception {
        byte[] msgBytes = msg.pack();
        byte[] lengthHeader = createLengthHeader(msgBytes.length);

        outputStream.write(lengthHeader);
        outputStream.write(msgBytes);
        outputStream.flush();
    }

    private int parseMessageLength(byte[] header) {
        if (header.length == 2) {
            return ((header[0] & 0xFF) << 8) | (header[1] & 0xFF);
        } else {
            return ((header[0] & 0xFF) << 24) |
                    ((header[1] & 0xFF) << 16) |
                    ((header[2] & 0xFF) << 8) |
                    (header[3] & 0xFF);
        }
    }

    private byte[] createLengthHeader(int length) {
        int headerLength = properties.getServer().getMessageHeaderLength();
        if (headerLength == 2) {
            return new byte[]{(byte) (length >> 8), (byte) length};
        } else {
            return new byte[]{
                    (byte) (length >> 24),
                    (byte) (length >> 16),
                    (byte) (length >> 8),
                    (byte) length
            };
        }
    }

    private void readFully(InputStream inputStream, byte[] buffer) throws IOException {
        int totalRead = 0;
        while (totalRead < buffer.length) {
            int read = inputStream.read(buffer, totalRead, buffer.length - totalRead);
            if (read == -1) {
                throw new IOException("Unexpected end of stream");
            }
            totalRead += read;
        }
    }
}