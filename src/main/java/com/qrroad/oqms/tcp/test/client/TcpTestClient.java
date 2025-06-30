package com.qrroad.oqms.tcp.test.client;

import com.qrroad.oqms.tcp.test.config.TcpTestProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@RequiredArgsConstructor
public class TcpTestClient {

    private final TcpTestProperties properties;
    private final ISOPackager packager;

    private final AtomicInteger stanCounter = new AtomicInteger(1);

    public ISOMsg sendPaymentRequest(String pan, long amount, String terminalId) {
        return sendMessage(createPaymentRequest(pan, amount, terminalId));
    }

    public ISOMsg sendBalanceInquiry(String pan, String terminalId) {
        return sendMessage(createBalanceInquiry(pan, terminalId));
    }

    public ISOMsg sendReversalRequest(String pan, long amount, String originalStan, String originalRrn) {
        return sendMessage(createReversalRequest(pan, amount, originalStan, originalRrn));
    }

    public ISOMsg sendNetworkTest() {
        return sendMessage(createNetworkTest());
    }

    private ISOMsg sendMessage(ISOMsg requestMsg) {
        Socket socket = null;
        try {
            TcpTestProperties.Client clientConfig = properties.getClient();

            socket = new Socket();
            socket.connect(new java.net.InetSocketAddress(
                            clientConfig.getTargetHost(), clientConfig.getTargetPort()),
                    clientConfig.getConnectTimeoutMs());
            socket.setSoTimeout(clientConfig.getReadTimeoutMs());
            socket.setKeepAlive(clientConfig.isKeepAlive());

            try (InputStream inputStream = socket.getInputStream();
                 OutputStream outputStream = socket.getOutputStream()) {

                // 요청 전송
                sendMessage(outputStream, requestMsg);
                log.info("Sent request: MTI={}, STAN={}",
                        requestMsg.getMTI(), requestMsg.getString(11));

                // 응답 수신
                ISOMsg responseMsg = receiveMessage(inputStream);
                log.info("Received response: MTI={}, STAN={}, Response={}",
                        responseMsg.getMTI(), responseMsg.getString(11), responseMsg.getString(39));

                return responseMsg;
            }

        } catch (Exception e) {
            log.error("Error sending message", e);
            return null;
        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    log.warn("Error closing socket", e);
                }
            }
        }
    }

    private ISOMsg createPaymentRequest(String pan, long amount, String terminalId) {
        try {
            ISOMsg msg = new ISOMsg();
            msg.setPackager(packager);
            msg.setMTI("0200");

            String stan = String.format("%06d", stanCounter.getAndIncrement());
            String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
            String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMdd"));
            String transmissionTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmss"));

            msg.set(2, pan);                           // PAN
            msg.set(3, "000000");                      // Processing code (purchase)
            msg.set(4, String.format("%012d", amount)); // Transaction amount
            msg.set(7, transmissionTime);              // Transmission date/time
            msg.set(11, stan);                         // STAN
            msg.set(12, currentTime);                  // Local time
            msg.set(13, currentDate);                  // Local date
            msg.set(18, "5999");                       // Merchant type
            msg.set(22, "051");                        // POS entry mode
            msg.set(25, "00");                         // POS condition code
            msg.set(37, generateRrn());                // RRN
            msg.set(41, terminalId);                   // Terminal ID
            msg.set(42, "TEST_MERCHANT_001");          // Merchant ID
            msg.set(43, "TEST MERCHANT LOCATION");     // Merchant name/location
            msg.set(49, "410");                        // Currency code (KRW)

            return msg;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create payment request", e);
        }
    }

    private ISOMsg createBalanceInquiry(String pan, String terminalId) {
        try {
            ISOMsg msg = createPaymentRequest(pan, 0L, terminalId);
            msg.set(3, "380000"); // Processing code (balance inquiry)
            msg.unset(4);         // No amount for balance inquiry

            return msg;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create balance inquiry", e);
        }
    }

    private ISOMsg createReversalRequest(String pan, long amount, String originalStan, String originalRrn) {
        try {
            ISOMsg msg = new ISOMsg();
            msg.setPackager(packager);
            msg.setMTI("0400");

            String stan = String.format("%06d", stanCounter.getAndIncrement());
            String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
            String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMdd"));
            String transmissionTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmss"));

            msg.set(2, pan);                           // PAN
            msg.set(3, "000000");                      // Processing code
            msg.set(4, String.format("%012d", amount)); // Transaction amount
            msg.set(7, transmissionTime);              // Transmission date/time
            msg.set(11, stan);                         // STAN
            msg.set(12, currentTime);                  // Local time
            msg.set(13, currentDate);                  // Local date
            msg.set(37, generateRrn());                // RRN
            msg.set(41, "TEST001");                    // Terminal ID
            msg.set(42, "TEST_MERCHANT_001");          // Merchant ID
            msg.set(90, originalStan + originalRrn + currentDate + "000000"); // Original data

            return msg;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create reversal request", e);
        }
    }

    private ISOMsg createNetworkTest() {
        try {
            ISOMsg msg = new ISOMsg();
            msg.setPackager(packager);
            msg.setMTI("0800");

            String stan = String.format("%06d", stanCounter.getAndIncrement());
            String transmissionTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmss"));

            msg.set(7, transmissionTime);  // Transmission date/time
            msg.set(11, stan);             // STAN
            msg.set(70, "001");            // Network management info (echo test)

            return msg;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create network test", e);
        }
    }

    private void sendMessage(OutputStream outputStream, ISOMsg msg) throws Exception {
        byte[] msgBytes = msg.pack();
        byte[] lengthHeader = createLengthHeader(msgBytes.length);

        outputStream.write(lengthHeader);
        outputStream.write(msgBytes);
        outputStream.flush();
    }

    private ISOMsg receiveMessage(InputStream inputStream) throws Exception {
        // 길이 헤더 읽기
        int headerLength = properties.getClient().getMessageHeaderLength();
        byte[] lengthHeader = new byte[headerLength];
        readFully(inputStream, lengthHeader);

        int messageLength = parseMessageLength(lengthHeader);

        // 메시지 본문 읽기
        byte[] messageBytes = new byte[messageLength];
        readFully(inputStream, messageBytes);

        // ISO8583 메시지 언팩
        ISOMsg msg = new ISOMsg();
        msg.setPackager(packager);
        msg.unpack(messageBytes);

        return msg;
    }

    private byte[] createLengthHeader(int length) {
        int headerLength = properties.getClient().getMessageHeaderLength();
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

    private String generateRrn() {
        return String.format("%012d", System.currentTimeMillis() % 1000000000000L);
    }
}