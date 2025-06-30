package com.qrroad.oqms.tcp.test.handler;

import lombok.extern.slf4j.Slf4j;
import org.jpos.iso.ISOMsg;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
public class MessageHandler {

    private static final String SUCCESS_CODE = "00";
    private static final String INVALID_MESSAGE = "30";
    private static final String SYSTEM_ERROR = "96";

    public ISOMsg processMessage(ISOMsg requestMsg) {
        try {
            String mti = requestMsg.getMTI();
            log.info("Processing message with MTI: {}", mti);

            return switch (mti) {
                case "0200" -> processPaymentRequest(requestMsg);
                case "0400" -> processReversalRequest(requestMsg);
                case "0800" -> processNetworkMessage(requestMsg);
                default -> createErrorResponse(requestMsg, INVALID_MESSAGE, "Invalid message type");
            };

        } catch (Exception e) {
            log.error("Error processing message", e);
            return createErrorResponse(requestMsg, SYSTEM_ERROR, "System error");
        }
    }

    private ISOMsg processPaymentRequest(ISOMsg requestMsg) {
        try {
            // 결제 요청 처리 시뮬레이션
            ISOMsg responseMsg = createBaseResponse(requestMsg, "0210");

            // 기본 필드 복사
            copyFieldIfPresent(requestMsg, responseMsg, 2);  // PAN
            copyFieldIfPresent(requestMsg, responseMsg, 3);  // Processing Code
            copyFieldIfPresent(requestMsg, responseMsg, 4);  // Transaction Amount
            copyFieldIfPresent(requestMsg, responseMsg, 11); // STAN
            copyFieldIfPresent(requestMsg, responseMsg, 37); // RRN
            copyFieldIfPresent(requestMsg, responseMsg, 41); // Terminal ID
            copyFieldIfPresent(requestMsg, responseMsg, 42); // Merchant ID

            // 승인 처리
            String amount = requestMsg.getString(4);
            String processingCode = requestMsg.getString(3);

            if (amount != null && Long.parseLong(amount) > 100000000L) { // 1억원 초과
                responseMsg.set(39, "61"); // Amount limit exceeded
            } else if ("380000".equals(processingCode)) { // 잔액조회
                responseMsg.set(39, SUCCESS_CODE);
                responseMsg.set(54, "000000010000000"); // Available balance
            } else {
                responseMsg.set(39, SUCCESS_CODE); // Approved
                responseMsg.set(38, generateAuthCode()); // Auth code
            }

            log.info("Payment processed: Amount={}, Response={}",
                    amount, responseMsg.getString(39));

            return responseMsg;

        } catch (Exception e) {
            log.error("Error processing payment request", e);
            return createErrorResponse(requestMsg, SYSTEM_ERROR, "Payment processing error");
        }
    }

    private ISOMsg processReversalRequest(ISOMsg requestMsg) {
        try {
            // 취소 요청 처리 시뮬레이션
            ISOMsg responseMsg = createBaseResponse(requestMsg, "0410");

            // 기본 필드 복사
            copyFieldIfPresent(requestMsg, responseMsg, 2);  // PAN
            copyFieldIfPresent(requestMsg, responseMsg, 3);  // Processing Code
            copyFieldIfPresent(requestMsg, responseMsg, 4);  // Transaction Amount
            copyFieldIfPresent(requestMsg, responseMsg, 11); // STAN
            copyFieldIfPresent(requestMsg, responseMsg, 37); // RRN
            copyFieldIfPresent(requestMsg, responseMsg, 41); // Terminal ID
            copyFieldIfPresent(requestMsg, responseMsg, 42); // Merchant ID

            // 원거래 정보
            copyFieldIfPresent(requestMsg, responseMsg, 90); // Original data

            responseMsg.set(39, SUCCESS_CODE); // Reversal approved

            log.info("Reversal processed successfully");
            return responseMsg;

        } catch (Exception e) {
            log.error("Error processing reversal request", e);
            return createErrorResponse(requestMsg, SYSTEM_ERROR, "Reversal processing error");
        }
    }

    private ISOMsg processNetworkMessage(ISOMsg requestMsg) {
        try {
            // 네트워크 관리 메시지 처리
            ISOMsg responseMsg = createBaseResponse(requestMsg, "0810");

            copyFieldIfPresent(requestMsg, responseMsg, 11); // STAN
            copyFieldIfPresent(requestMsg, responseMsg, 70); // Network management info

            responseMsg.set(39, SUCCESS_CODE);

            log.info("Network message processed successfully");
            return responseMsg;

        } catch (Exception e) {
            log.error("Error processing network message", e);
            return createErrorResponse(requestMsg, SYSTEM_ERROR, "Network message error");
        }
    }

    private ISOMsg createBaseResponse(ISOMsg requestMsg, String responseMti) {
        try {
            ISOMsg responseMsg = new ISOMsg();
            responseMsg.setPackager(requestMsg.getPackager());
            responseMsg.setMTI(responseMti);

            // 기본 시간 정보 설정
            String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HHmmss"));
            String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMdd"));
            String transmissionTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MMddHHmmss"));

            responseMsg.set(7, transmissionTime);  // Transmission date/time
            responseMsg.set(12, currentTime);      // Local time
            responseMsg.set(13, currentDate);      // Local date

            return responseMsg;

        } catch (Exception e) {
            throw new RuntimeException("Failed to create base response", e);
        }
    }

    private ISOMsg createErrorResponse(ISOMsg requestMsg, String responseCode, String message) {
        try {
            String responseMti = switch (requestMsg.getMTI()) {
                case "0200" -> "0210";
                case "0400" -> "0410";
                case "0800" -> "0810";
                default -> "0210";
            };

            ISOMsg responseMsg = createBaseResponse(requestMsg, responseMti);

            copyFieldIfPresent(requestMsg, responseMsg, 11); // STAN
            copyFieldIfPresent(requestMsg, responseMsg, 37); // RRN

            responseMsg.set(39, responseCode);

            log.warn("Error response created: Code={}, Message={}", responseCode, message);
            return responseMsg;

        } catch (Exception e) {
            log.error("Failed to create error response", e);
            return null;
        }
    }

    private void copyFieldIfPresent(ISOMsg source, ISOMsg target, int fieldNumber) {
        try {
            if (source.hasField(fieldNumber)) {
                target.set(fieldNumber, source.getString(fieldNumber));
            }
        } catch (Exception e) {
            log.warn("Failed to copy field {}", fieldNumber, e);
        }
    }

    private String generateAuthCode() {
        return String.format("%06d", (int) (Math.random() * 1000000));
    }
}