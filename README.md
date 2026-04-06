# chest-discord-bot

마인크래프트 Fabric 클라이언트 모드(`chest-fabric-mod`)와 연동해 상자 로그와 섬 은행 입출금 로그를 Discord로 보내는 Spring Boot 기반 봇/백엔드입니다.

## 요구 사항

- Java `21`
- Discord 봇 토큰
- Gradle 실행 환경

## 주요 기능

- Discord 슬래시 명령 `/창고`
  - `설정`: 섬 생성 및 참여 코드 발급
  - `섬이름`: 섬 표시 이름 변경
  - `채널연결`: 창고 로그 / 섬 은행 기록 채널 연결
  - `코드`: 현재 참여 코드 조회
  - `코드재발급`: 참여 코드 재발급
  - `관리자코드`: 인게임 상자 등록용 1회성 관리자 코드 발급
- Fabric 모드용 REST API 제공
- H2 파일 DB 저장
- Discord 임베드 로그 전송

## 기술 스택

- Spring Boot `3.4.3`
- Spring Web / Spring Data JPA
- JDA `5.2.1`
- H2 Database
- Gradle

## 설정

이 프로젝트는 `src/main/resources/application.properties`에서 아래 설정을 사용합니다.

```properties
spring.config.import=optional:file:./local.properties
server.port=${SERVER_PORT:5000}
spring.datasource.url=jdbc:h2:file:./data/chestbot;AUTO_SERVER=TRUE
spring.application.name=changojigi-discord-bot
```

### local.properties

루트에 `local.properties` 파일을 만들고 실제 값을 넣어야 합니다.

예시:

```properties
discord.token=your_discord_bot_token
app.admin-key=your_admin_key
```

### 환경변수

| 이름 | 설명 | 기본값 |
|---|---|---|
| `SERVER_PORT` | 서버 포트 | `5000` |

## 실행

Windows:

```powershell
$env:SERVER_PORT="5000"
.\gradlew.bat bootRun
```

macOS / Linux:

```bash
export SERVER_PORT="5000"
./gradlew bootRun
```

빌드:

```powershell
.\gradlew.bat build
```

## 데이터 저장 위치

- H2 DB: `./data/chestbot`
- 창고 로그 파일: `./data/chest-log-events.jsonl`
- 섬 은행 로그 파일: `./data/island-bank-log-events.jsonl`

## Discord 사용 흐름

1. Discord 서버에 봇을 초대합니다.
2. `/창고 설정`으로 섬을 만들고 참여 코드를 발급받습니다.
3. `/창고 채널연결`로 창고 로그 채널과 섬 은행 기록 채널을 연결합니다.
4. 게임 클라이언트에서 `/창고봇 연결 <참여코드>`를 입력합니다.
5. 필요하면 `/창고 관리자코드`를 발급받아 인게임 관리자 등록 흐름에 사용합니다.

## 모드 연동 API

기본 prefix: `/api/v1/client`

### 연결

```http
POST /api/v1/client/connect
Content-Type: application/json

{
  "joinCode": "ABC123"
}
```

### 관리자 연결

```http
POST /api/v1/client/admin/connect
Content-Type: application/json

{
  "joinCode": "ABC123",
  "adminCode": "ABCDEFGH"
}
```

### 관리자 상자 설정 확정

```http
POST /api/v1/client/admin/finalize
Content-Type: application/json

{
  "joinCode": "ABC123",
  "adminCode": "ABCDEFGH",
  "chests": [
    {
      "chestKey": "ore-storage",
      "displayName": "광물 창고",
      "x": 100,
      "y": 64,
      "z": 200,
      "worldHint": "world",
      "metadataJson": "{}"
    }
  ]
}
```

### 창고 로그 전송

```http
POST /api/v1/client/events/chest-log
Content-Type: application/json

{
  "joinCode": "ABC123",
  "configVersion": 1,
  "playerName": "Steve",
  "chestKey": "ore-storage",
  "taken": {
    "diamond": 3
  },
  "added": {
    "cobblestone": 64
  }
}
```

### 섬 은행 로그 전송

`transactionType`은 `DEPOSIT`, `WITHDRAW`, `입금`, `출금` 중 하나여야 하며, `amount`는 0보다 커야 합니다.

```http
POST /api/v1/client/events/island-bank-log
Content-Type: application/json

{
  "joinCode": "ABC123",
  "playerName": "Steve",
  "transactionType": "DEPOSIT",
  "amount": 5000,
  "balanceAfter": null,
  "note": "섬 공동 자금 입금"
}
```

## 관리자 API

기본 prefix: `/api/v1`

모든 관리자 API는 `X-Admin-Key` 헤더가 필요합니다.

- `POST /guilds/register`
- `GET /islands/{islandId}`
- `POST /islands/{islandId}/channels`
- `GET /islands/{islandId}/config`
- `POST /islands/{islandId}/config`

## 운영 시 주의점

- `local.properties`는 절대 커밋하지 마세요.
- Discord 채널이 연결되지 않으면 API 요청은 성공해도 Discord 전송은 생략될 수 있습니다.
- 최초 섬 생성과 참여 코드 발급은 Discord 명령 흐름을 기준으로 사용하는 것이 가장 안전합니다.
- 모드와 백엔드의 엔드포인트 계약이 다르면 연결은 성공해도 로그 전송이 실패할 수 있습니다.

## 프로젝트 구조

```text
src/main/java/com/example/chestbot
├─ controller/   # REST API
├─ discord/      # 슬래시 명령 리스너
├─ dto/          # 요청/응답 DTO
├─ persistence/  # 엔티티 / 리포지토리
└─ service/      # Discord, 섬 설정, 로그 처리
```
