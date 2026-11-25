# ego-lite-mobile (v0.4.6)

HONOR MagicPad 3 Pro (`YLP-W00`, Android 16 / `arm64-v8a`) 태블릿용 AI 협업 브라우저 프로젝트입니다.

## v0.4.6 변경 사항 (`versionCode = 16`, `versionName = "0.4.6"`)
- **Drag-to-Open → Drag-to-Select 완벽 수정:** `⋮` 브라우저 메뉴와 좌상단 `Personal / AI Space` 알약 캡슐의 `pointerInput` 키를 `Unit` + `rememberUpdatedState`로 고정해 누른 직후 드래그·릴리즈 선택이 끊기지 않도록 해결
- **북마크·새 탭·Omnibox 실제 사이트 파비콘(Favicon) 적용:** `FaviconCache`(메모리+디스크 캐시 + Google S2 파비콘 + `WebChromeClient.onReceivedIcon`)로 실제 사이트 아이콘 표시
- **크롬형 문맥 인식 `⋮` 메뉴:** 새 탭(`ego://newtab`)에서는 공통 메뉴만 간결하게 표시하고, 실제 웹페이지(`http/https`)에서만 웹 전용 항목(공유·링크 복사·인쇄/PDF·페이지 찾기·번역·읽기 모드·데스크톱 사이트) 표시
- **주소창 바 좌측 홈 버튼(`🏠`) 추가:** 설정된 홈페이지(기본 `ego://newtab`)로 원탭 이동
- **Omnibox 오버레이 위치·길이 1:1 동적 정렬:** `onGloballyPositioned`로 실제 주소창의 `(x, y, width)`를 실시간 측정해 `MCP 끊김` 배지 유무와 상관없이 실제 주소창과 정확히 일치

## Codemagic 무료 빌드 방법 (`codemagic.yaml`)
1. 이 폴더(`ego-lite-mobile/`)를 GitHub 저장소에 올립니다.
2. Codemagic에서 해당 저장소를 연결하고 `android-ego-lite-mobile` 워크플로우(`mac_mini_m2`, Java 17, 30분)를 실행합니다.
3. 빌드가 끝나면 Artifacts에서 `app-debug.apk`를 내려받아 태블릿에 설치합니다.
