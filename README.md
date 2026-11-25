# ego-lite-mobile (v0.4.2)

HONOR MagicPad 3 Pro (`YLP-W00`, Android 16 / `arm64-v8a`) 태블릿용 AI 협업 브라우저 프로젝트입니다.

## v0.4.2 변경 사항 (`versionCode = 12`, `versionName = "0.4.2"`)
- **실행 직후 홈으로 튕기는 치명적 크래시 수정:** `EgoWebViewFactory`의 `shouldInterceptRequest`(백그라운드 IO 스레드)에서 `view?.url`을 직접 호출하던 WebView 스레드 위반 오류를 `@Volatile`/`currentPageHost` 추적 방식으로 해결
- **크래시 로그 자동 기록(`CrashLogger`) 추가:** 예기치 않은 종료 시 스택 트레이스·기기·버전 정보를 앱 내부(`last_crash.txt`)와 외부 저장소(`Android/data/com.egolite.mobile.debug/files/crash_logs/`)에 자동 저장하고, 다음 실행 시 **로그 복사 팝업**으로 즉시 확인 가능
- **앱 업데이트 시 덮어쓰기 설치 지원:** `codemagic.yaml`에서 `~/.android/debug.keystore`를 캐시(및 선택적 `DEBUG_KEYSTORE_B64` 환경변수 복원)해 빌드마다 서명이 바뀌는 문제를 해결, `v0.4.2` 이후부터 삭제 없이 바로 업데이트 가능

## Codemagic 무료 빌드 방법 (`codemagic.yaml`)
1. 이 폴더(`ego-lite-mobile/`)를 GitHub 저장소에 올립니다.
2. Codemagic에서 해당 저장소를 연결하고 `android-ego-lite-mobile` 워크플로우(`mac_mini_m2`, Java 17, 30분)를 실행합니다.
3. 빌드가 끝나면 Artifacts에서 `app-debug.apk`를 내려받아 태블릿에 설치합니다.
