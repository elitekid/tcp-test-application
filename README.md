# TCP Test Application

ISO8583 메시지 기반 TCP 통신 테스트 도구입니다.

## 🚀 시작하기

### 필수 요구사항
- Java 17
- Docker

### 로컬 실행
```bash
./gradlew clean bootRun
```

## 🐳 Docker 사용법

### 1. 도커 이미지 생성
```bash
./gradlew clean bootJar
docker build -t tcp-test-app .
```

### 2. 컨테이너 실행
```bash
docker run -d \
  --name tcp-test-app \
  -p 8080:8080 \
  -p 8583:8583 \
  tcp-test-app
```

## 📡 API 사용법

### 서버 관리
```bash
# 서버 상태 확인
curl http://localhost:8080/api/tcp-test/server/status

# 서버 시작/중지
curl -X POST http://localhost:8080/api/tcp-test/server/start
curl -X POST http://localhost:8080/api/tcp-test/server/stop
```

### 메시지 테스트

#### 결제 요청
```bash
curl -X POST http://localhost:8080/api/tcp-test/client/payment \
  -H "Content-Type: application/json" \
  -d '{
    "pan": "4111111111111111",
    "amount": 10000,
    "terminalId": "TEST001"
  }'
```

#### 잔액 조회
```bash
curl -X POST http://localhost:8080/api/tcp-test/client/balance \
  -H "Content-Type: application/json" \
  -d '{
    "pan": "4111111111111111",
    "terminalId": "TEST001"
  }'
```

#### 취소 요청
```bash
curl -X POST http://localhost:8080/api/tcp-test/client/reversal \
  -H "Content-Type: application/json" \
  -d '{
    "pan": "4111111111111111",
    "amount": 10000,
    "originalStan": "000001",
    "originalRrn": "123456789012"
  }'
```

#### 네트워크 테스트
```bash
curl -X POST http://localhost:8080/api/tcp-test/client/network-test
```

## ⚙️ 설정

### 환경변수
- `TCP_TEST_SERVER_PORT`: TCP 서버 포트 (기본: 8583)
- `TCP_TEST_CLIENT_TARGET_HOST`: 클라이언트 대상 호스트 (기본: localhost)
- `TCP_TEST_CLIENT_TARGET_PORT`: 클라이언트 대상 포트 (기본: 8583)

## 📊 모니터링

```bash
# Health Check
curl http://localhost:8080/actuator/health
```

실행 후:
- **HTTP 서버**: http://localhost:8080
- **TCP 서버**: localhost:8583