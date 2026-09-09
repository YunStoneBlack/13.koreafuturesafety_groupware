# 한국미래안전 그룹웨어

사내 그룹웨어(공지사항/전자결재/일정공유/직원정보 + 향후 보고서 자동화 모듈).
`https://www.kfsc21c.com/`(별도 소스 없는 fat jar)와는 완전히 분리된 별도 Spring Boot
프로젝트이며, `groupware.kfsc21c.com` 서브도메인으로 배포된다.

## 현재 단계: 0단계 (파이프라인 검증용 골격)

- Spring Security 인메모리 임시 계정(`admin` / `changeme123`, role `ADMIN`) — DB 기반
  계정(auth 패키지)이 만들어지면 교체 예정
- `/health` — 헬스체크
- `/` — 로그인 후 보이는 임시 홈 화면
- H2 파일 DB (`${GROUPWARE_DATA_DIR:./data}/groupware`) 연결만 확인, 아직 엔티티 없음

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
6. `report` — "준비중" 페이지만 우선, 실제 기능은 별도 프로젝트(보고서 자동화 프로그램)
   완성 후 이식
