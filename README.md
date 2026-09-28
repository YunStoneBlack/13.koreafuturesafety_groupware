# 한국미래안전 그룹웨어

사내 그룹웨어(공지사항/전자결재/일정공유/직원정보 + 향후 보고서 자동화 모듈).
`https://www.kfsc21c.com/`(별도 소스 없는 fat jar)와는 완전히 분리된 별도 Spring Boot
프로젝트이며, `groupware.kfsc21c.com` 서브도메인으로 배포된다.

## 현재 단계: 1단계 (DB 기반 로그인)

- `auth` 패키지: `User`(username/passwordHash/displayName/role/enabled), BCrypt,
  `CustomUserDetailsService`
- 최초 기동 시 계정이 하나도 없으면 `admin` 계정을 자동 생성(`config/DataSeeder`).
  비밀번호는 `GROUPWARE_ADMIN_INITIAL_PASSWORD` 환경변수로 지정하거나, 지정 안 하면
  무작위 생성 후 기동 로그에 한 번만 출력됨(재기동 시 계정이 이미 있으면 아무 것도 안 함)
- `/health` — 헬스체크
- `/` — 로그인 후 보이는 임시 홈 화면 (모듈 자리 표시만, 실제 기능은 다음 단계부터)
- H2 파일 DB (`${GROUPWARE_DATA_DIR:./data}/groupware`)

## 빌드 (서버에서, 로컬 자바 툴체인 없음이 전제)

```bash
mvn clean package -DskipTests
java -jar target/groupware.jar
```

## 배포 환경변수

- `GROUPWARE_DATA_DIR`: H2 DB 파일 저장 위치. 운영 서버에서는
  `/home/ec2-user/groupware/data`로 지정(systemd 유닛의 `Environment=`).

## 다음 단계

1. `auth` 패키지: DB 기반 User/Role, BCrypt, 관리자 계정 시딩 → 위 인메모리 계정 제거
2. `staff` (직원정보/주소록)
3. `board` (공지사항)
4. `approval` (전자결재)
5. `calendar` (일정공유, FullCalendar)
6. `report` — **연결 완료(2026-09-28)**: 메뉴 "보고서 자동화" → `/report/`. 실제 기능은 별도 프로젝트
   (12-1 보고서 자동화 웹판)가 보고서 PC(윈도우 + 한글)에서 돌고, nginx가 `/report/`를 SSH 역방향 통로로 그 PC에 넘긴다.
   그룹웨어는 `ReportController`의 `/internal/report-auth`(nginx `auth_request`용 로그인 확인 — 외부 직접 접근은 nginx가 404)와
   `/report-shell/sidebar`(보고서 화면이 끼워 넣는 사이드바 조각)만 제공. 자세한 구조·nginx 설정은 12-1 저장소 `server/README_DEPLOY.md` 6번.
