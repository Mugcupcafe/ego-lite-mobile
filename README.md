# ego-lite-mobile (v0.1)

HONOR MagicPad 3 Pro (`YLP-W00`, Android 16 / `arm64-v8a`) 태블릿용 AI 협업 브라우저 프로젝트입니다.

## 핵심 기능 (v0.1)

- **다중 Space WebView 아키텍처:** `Personal` 공간과 다중 `AI Space`(기본 최대 5개)가 탭·주소창은 완전히 격리하고, 로그인 세션(`CookieManager`)은 공유합니다.
- **알약 캡슐 Space 전환 UI:** 좌상단 알약 캡슐(`●`/`○` 반전 + AI 작동 중 에메랄드 테두리 회전 + 터치 시 하단 슬라이드 공간 전환 바), 최상단 탭 전용 바, 조건부 `MCP 끊김` 배지, 선택적 북마크 바.
- **한국 사이트 호환 및 3대 편의 기능:** `intent://`·간편인증 앱 스킴 호출, `window.open` 인증 팝업 지원, `wv` 제거 크롬 User-Agent + 데스크톱 모드, 광고 차단, 알고리즘 다크모드, 원클릭 한국어 번역.
- **코드 모드 3-툴 Streamable HTTP MCP (`/mcp`):**
  1. `browser_exec` — AI Space V8 샌드박스 내 다중 동작 JS 배치 실행
  2. `browser_observe` — `snapshot`(@N ref) / `markdown`(Firecrawl 스타일 본문 정제) / `screenshot`
  3. `research_last30days` — Reddit · Hacker News · Polymarket · Web (+ 로그인 WebView X 옵션) 최근 30일 경량 수집
- **이중 인증 & 내장 HTTPS 터널:** Gemini 웹/앱용 표준 OAuth 2.1(PKCE, 외부 HTTPS 동적 메타데이터) + Muse용 Bearer 토큰 + Cloudflare / ngrok 터널(`jniLibs/arm64-v8a/lib*.so` W^X 안전 실행) + Foreground Service.

## APK 빌드 방법 (2가지 중 선택)

### 방법 1: Codemagic으로 빌드 (`codemagic.yaml`)
1. 이 폴더를 GitHub 저장소에 푸시합니다.
2. [Codemagic](https://codemagic.io/)에서 해당 저장소를 연결하고 `codemagic.yaml` 워크플로우(`android-ego-lite-mobile`)를 실행합니다.
3. 빌드가 끝나면 Artifacts에서 `app-debug.apk`를 내려받아 태블릿에 설치합니다.

### 방법 2: GitHub Actions로 바로 빌드 (`.github/workflows/build-apk.yml`)
1. 이 폴더를 GitHub 저장소에 푸시하면 자동으로 빌드가 시작됩니다(또는 Actions 탭에서 수동 실행).
2. 완료 후 `ego-lite-mobile-v0.1-debug-apk` 아티팩트를 내려받아 설치합니다.

## 태블릿 초기 설정 (HONOR MagicPad 3 Pro)

1. APK 설치 후 앱을 열고 우측 `⋮` 메뉴(또는 `MCP 끊김` 배지)를 눌러 **MCP · 터널 설정 드로어**를 엽니다.
2. 하단의 **배터리 최적화 제외 설정** 버튼을 눌러 MagicOS 절전 제한을 해제합니다.
3. **MCP 서버 & HTTPS 터널** 스위치를 켜면 공인 `https://.../mcp` 주소가 표시됩니다.
   - **Gemini 웹/앱:** 표시된 HTTPS MCP 주소를 등록하면 OAuth 2.1 디스커버리로 자동 연결됩니다.
   - **Muse:** 표시된 HTTPS MCP 주소와 드로어의 **Bearer 토큰**을 복사해 입력합니다.
