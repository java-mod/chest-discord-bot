# chest-discord-bot SETUP

이 프로젝트는 **Spring Boot 서버 + Discord 봇이 한 프로세스에서 같이 도는 구조**입니다.

즉, **프로젝트 루트에 `local.properties`를 넣고 실행하면** 그 안의 `discord.token`으로 Discord 봇이 로그인하고:

- REST API 서버가 뜨고
- 같은 프로세스에서 Discord 봇이 로그인하고
- 기본 DB는 로컬 H2 파일(`./data/chestbot`)을 사용합니다.

## 1. 준비물

- Java 21
- Discord Bot Token
- `local.properties`

## 2. local.properties 준비

프로젝트 루트에 `local.properties.example`을 복사해서 `local.properties`를 만듭니다.

최소 예시:

```properties
discord.token=YOUR_DISCORD_BOT_TOKEN
app.admin-key=CHANGE_ME
```

기본값으로는 H2 파일 DB를 사용하므로, 로컬 테스트만 할 때는 MySQL 설정이 없어도 됩니다.

## 3. 실행 방법

### 개발용

```powershell
$env:SERVER_PORT="5000"
.\gradlew.bat bootRun
```

### 배포/실행용 JAR

```powershell
.\gradlew.bat build
java -jar .\build\libs\chest-discord-bot-1.0.0.jar
```

## 4. 최초 Discord 설정

서버가 뜬 뒤 Discord에서:

1. `/창고 설정`
2. `/창고 채널연결`
3. 필요 시 `/창고 관리자코드`

## 5. 모드 연결 흐름

Fabric 모드 쪽에서는 참여 코드가 아니라 아래 순서로 붙습니다.

```text
/창고봇 서버 <서버주소>
/창고봇 연결
```

창고 등록이 필요하면:

```text
/창고 관리자코드   (Discord)
/창고봇 관리자 <코드>   (Minecraft)
```

## 6. 데이터 위치

- H2 DB: `./data/chestbot`
- 창고 로그 JSONL: `./data/chest-log-events.jsonl`
- 섬 은행 로그 JSONL: `./data/island-bank-log-events.jsonl`

## 7. 주의

- `local.properties`는 커밋하지 않습니다.
- 운영 로그/백업(`runlogs/`, `data-backups/`)은 로컬 산출물입니다.
- 기본 포트는 `5000`, 필요하면 `SERVER_PORT`로 변경합니다.
- GitHub Actions는 태그/릴리즈 생성을 시도하지만, Release 자산에 JAR이 항상 첨부된다고 전제하지 마세요. 배포 파일은 `build/libs/*.jar` 기준으로 확인하는 편이 안전합니다.

## 8. 라이선스

이 프로젝트는 **GPL-3.0-only** 라이선스를 따릅니다. 자세한 내용은 `LICENSE` 파일을 확인하세요.
