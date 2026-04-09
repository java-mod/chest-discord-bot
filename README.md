# chest-discord-bot

마인크래프트 Fabric 클라이언트 모드(`chest-fabric-mod`)와 연동해 상자 로그와 섬 은행 입출금 로그를 Discord로 보내는 Spring Boot 기반 봇/백엔드입니다.

## 요구 사항

- Java `21`
- Discord 봇 토큰
- Gradle 실행 환경

## 주요 기능

- Discord 슬래시 명령 `/창고`
  - `설정`: 섬 생성 및 서버 연결 준비
  - `섬이름`: 섬 표시 이름 변경
  - `채널연결`: 창고 로그 / 섬 은행 기록 채널 연결
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

프로젝트 루트에 `local.properties` 파일만 두면, 실행 시 Spring Boot가 이를 읽어 **그 안의 `discord.token`으로 Discord 봇을 로그인**시키고 같은 프로세스에서 API 서버도 함께 띄웁니다.

예시:

```properties
discord.token=your_discord_bot_token
app.admin-key=your_admin_key
```

로컬 H2 기본값으로 충분하면 위 2개만 있어도 **"local.properties 넣고 실행 = 그 토큰으로 봇+서버 구동"** 흐름이 바로 성립합니다.

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

빌드된 JAR 실행:

```powershell
.\gradlew.bat build
java -jar .\build\libs\chest-discord-bot-1.0.0.jar
```

## CI / 배포 메모

- `main` 브랜치 push 시 GitHub Actions가 빌드와 태그/릴리즈 생성을 시도합니다.
- 다만 운영 기준으로는 **GitHub Release에 JAR 자산이 항상 첨부된다고 가정하지 않는 편이 안전합니다.**
- 실제 배포용 파일은 로컬/CI의 `build/libs/*.jar` 산출물을 직접 확인하는 것을 권장합니다.

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
2. `local.properties`에 `discord.token` 등을 채우고 서버를 실행합니다.
3. Discord에서 `/창고 설정`으로 섬을 만들고 `/창고 채널연결`로 로그 채널을 연결합니다.
4. 게임 클라이언트에서 `/창고봇 서버 <서버주소>` 후 `/창고봇 연결`을 입력합니다.
5. 창고 등록이 필요하면 `/창고 관리자코드`를 발급받아 인게임 `/창고봇 관리자 <코드>` 흐름에 사용합니다.

## 모드 연동 API

기본 prefix: `/api/v1/client`

### 연결

```http
POST /api/v1/client/connect
```

### 관리자 연결

```http
POST /api/v1/client/admin/connect
Content-Type: application/json

{
  "adminCode": "ABCDEFGH"
}
```

### 관리자 상자 설정 확정

```http
POST /api/v1/client/admin/finalize
Content-Type: application/json

{
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
  "configVersion": 1,
  "playerName": "Steve",
  "playerUuid": "uuid",
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
- 현재 모드 연결은 참여 코드가 아니라 **서버 주소 기반**입니다.
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

## 라이선스

이 프로젝트는 **GPL-3.0-only** 라이선스를 따릅니다. 자세한 내용은 `LICENSE` 파일을 확인하세요.
