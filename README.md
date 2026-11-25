# ego-lite-mobile (v0.4.1)

HONOR MagicPad 3 Pro (`YLP-W00`, Android 16 / `arm64-v8a`) 태블릿용 AI 협업 브라우저 프로젝트입니다.

## v0.4.1 변경 사항 (`versionCode = 11`, `versionName = "0.4.1"`)
- **설정·토큰·광고차단 규칙 영구 저장:** `BrowserPreferences`(야간모드·북마크 바·번역/검색 엔진·글자 크기·자동번역 사이트·Firecrawl API 키), `TunnelSettings`(Cloudflare/ngrok/Bearer 토큰), `AdBlockEngine`(전역/코스메틱·화이트리스트·커스텀 차단 도메인/CSS·누적 차단 수) 모두 `SharedPreferences`에 영구 저장
- **새 탭(`ego://newtab`) 바로가기 추가 초기값 수정:** 새 탭 화면에서 `+ 바로가기 추가` 시 제목·URL 입력란이 비어 있는 상태로 열리도록 수정
- **탭/공간 전환 시 번역 상태 초기화:** 다른 탭이나 Space로 전환 시 이전 탭의 번역 팝업·활성 아이콘 상태가 자동으로 초기화되며, `ego://newtab`에서는 번역을 차단
- **시크릿 탭 시각 구분 추가:** 상단 탭 바, 주소창, 모든 탭 보기 다이얼로그에 시크릿 아이콘(`VisibilityOff`) 표시
- **Firecrawl API 연동 완료:** 설정 드로어에 Firecrawl API 키를 입력하면 `browser_observe(mode="markdown")`가 Firecrawl v1 Scrape API로 본문을 추출하고, 실패·미입력 시 로컬 WebView Markdown 추출로 자동 폴백
- **Gradle Wrapper 4종 완비:** `gradlew`, `gradlew.bat`, `gradle-wrapper.jar`, `gradle-wrapper.properties` 모두 포함

## Codemagic 무료 빌드 방법 (`codemagic.yaml`)
1. 이 폴더(`ego-lite-mobile/`)를 GitHub 저장소에 올립니다.
2. Codemagic에서 해당 저장소를 연결하고 `android-ego-lite-mobile` 워크플로우(`mac_mini_m2`, Java 17, 30분)를 실행합니다.
3. 빌드가 끝나면 Artifacts에서 `app-debug.apk`를 내려받아 태블릿에 설치합니다.
