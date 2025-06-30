package com.qrroad.oqms.tcp.test.controller;

import com.qrroad.oqms.tcp.test.client.TcpTestClient;
import com.qrroad.oqms.tcp.test.server.TcpTestServer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jpos.iso.ISOMsg;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/tcp-test")
@RequiredArgsConstructor
public class TcpTestController {

    private final TcpTestClient tcpTestClient;
    private final TcpTestServer tcpTestServer;

    @GetMapping("/server/status")
    public ResponseEntity<Map<String, Object>> getServerStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("running", tcpTestServer.isRunning());
        status.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(status);
    }

    @PostMapping("/server/start")
    public ResponseEntity<Map<String, String>> startServer() {
        Map<String, String> response = new HashMap<>();

        if (tcpTestServer.isRunning()) {
            response.put("status", "already_running");
            response.put("message", "TCP Test Server is already running");
        } else {
            tcpTestServer.startServer();
            response.put("status", "started");
            response.put("message", "TCP Test Server started successfully");
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/server/stop")
    public ResponseEntity<Map<String, String>> stopServer() {
        Map<String, String> response = new HashMap<>();
        tcpTestServer.stopServer();
        response.put("status", "stopped");
        response.put("message", "TCP Test Server stopped");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/client/payment")
    public ResponseEntity<Map<String, Object>> sendPaymentRequest(
            @RequestBody PaymentRequest request) {

        try {
            ISOMsg responseMsg = tcpTestClient.sendPaymentRequest(
                    request.getPan(), request.getAmount(), request.getTerminalId());

            return ResponseEntity.ok(createResponseMap(responseMsg));

        } catch (Exception e) {
            log.error("Error sending payment request", e);
            return ResponseEntity.internalServerError()
                    .body(createErrorMap("Failed to send payment request: " + e.getMessage()));
        }
    }

    @PostMapping("/client/balance")
    public ResponseEntity<Map<String, Object>> sendBalanceInquiry(
            @RequestBody BalanceRequest request) {

        try {
            ISOMsg responseMsg = tcpTestClient.sendBalanceInquiry(
                    request.getPan(), request.getTerminalId());

            return ResponseEntity.ok(createResponseMap(responseMsg));

        } catch (Exception e) {
            log.error("Error sending balance inquiry", e);
            return ResponseEntity.internalServerError()
                    .body(createErrorMap("Failed to send balance inquiry: " + e.getMessage()));
        }
    }

    @PostMapping("/client/reversal")
    public ResponseEntity<Map<String, Object>> sendReversalRequest(
            @RequestBody ReversalRequest request) {

        try {
            ISOMsg responseMsg = tcpTestClient.sendReversalRequest(
                    request.getPan(), request.getAmount(),
                    request.getOriginalStan(), request.getOriginalRrn());

            return ResponseEntity.ok(createResponseMap(responseMsg));

        } catch (Exception e) {
            log.error("Error sending reversal request", e);
            return ResponseEntity.internalServerError()
                    .body(createErrorMap("Failed to send reversal request: " + e.getMessage()));
        }
    }

    @PostMapping("/client/network-test")
    public ResponseEntity<Map<String, Object>> sendNetworkTest() {
        try {
            ISOMsg responseMsg = tcpTestClient.sendNetworkTest();

            return ResponseEntity.ok(createResponseMap(responseMsg));

        } catch (Exception e) {
            log.error("Error sending network test", e);
            return ResponseEntity.internalServerError()
                    .body(createErrorMap("Failed to send network test: " + e.getMessage()));
        }
    }

    private Map<String, Object> createResponseMap(ISOMsg msg) {
        Map<String, Object> response = new HashMap<>();

        if (msg == null) {
            response.put("success", false);
            response.put("message", "No response received");
            return response;
        }

        try {
            response.put("success", true);
            response.put("mti", msg.getMTI());
            response.put("responseCode", msg.getString(39));
            response.put("stan", msg.getString(11));
            response.put("rrn", msg.getString(37));
            response.put("authCode", msg.getString(38));
            response.put("transmissionDateTime", msg.getString(7));

            // 추가 필드들
            Map<String, String> fields = new HashMap<>();
            for (int i = 1; i <= 128; i++) {
                if (msg.hasField(i)) {
                    fields.put(String.valueOf(i), msg.getString(i));
                }
            }
            response.put("allFields", fields);

        } catch (Exception e) {
            log.warn("Error parsing response message", e);
            response.put("success", false);
            response.put("message", "Error parsing response: " + e.getMessage());
        }

        return response;
    }

    private Map<String, Object> createErrorMap(String message) {
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("message", message);
        error.put("timestamp", System.currentTimeMillis());
        return error;
    }

    // Request DTOs
    public static class PaymentRequest {
        private String pan;
        private long amount;
        private String terminalId;

        // Getters and setters
        public String getPan() { return pan; }
        public void setPan(String pan) { this.pan = pan; }
        public long getAmount() { return amount; }
        public void setAmount(long amount) { this.amount = amount; }
        public String getTerminalId() { return terminalId; }
        public void setTerminalId(String terminalId) { this.terminalId = terminalId; }
    }

    public static class BalanceRequest {
        private String pan;
        private String terminalId;

        // Getters and setters
        public String getPan() { return pan; }
        public void setPan(String pan) { this.pan = pan; }
        public String getTerminalId() { return terminalId; }
        public void setTerminalId(String terminalId) { this.terminalId = terminalId; }
    }

    public static class ReversalRequest {
        private String pan;
        private long amount;
        private String originalStan;
        private String originalRrn;

        // Getters and setters
        public String getPan() { return pan; }
        public void setPan(String pan) { this.pan = pan; }
        public long getAmount() { return amount; }
        public void setAmount(long amount) { this.amount = amount; }
        public String getOriginalStan() { return originalStan; }
        public void setOriginalStan(String originalStan) { this.originalStan = originalStan; }
        public String getOriginalRrn() { return originalRrn; }
        public void setOriginalRrn(String originalRrn) { this.originalRrn = originalRrn; }
    }
}