# ego-lite-mobile (v0.2)

HONOR MagicPad 3 Pro (`YLP-W00`, Android 16 / `arm64-v8a`) 태블릿용 AI 협업 브라우저 프로젝트입니다.

## v0.2 변경 사항
- **전용 런처 아이콘 적용:** `mipmap-mdpi` ~ `xxxhdpi` 및 Android Adaptive Icon(`ic_launcher`, `ic_launcher_round`) 탑재
- **Codemagic 무료 빌드 전용 설정:** `instance_type: mac_mini_m2`, `java: 17`, `max_build_duration: 30`, `./gradlew assembleDebug` 고정
- **버전 업그레이드:** `versionCode = 2`, `versionName = "0.2.0"`
- **GitHub Actions 워크플로 제거:** `codemagic.yaml` 단일 빌드 파이프라인으로 정리

## Codemagic 무료 빌드 방법 (`codemagic.yaml`)
1. 이 폴더(`ego-lite-mobile/`)를 GitHub 저장소에 올립니다.
2. Codemagic에서 해당 저장소를 연결하고 `android-ego-lite-mobile` 워크플로우(`mac_mini_m2`)를 실행합니다.
3. 빌드가 끝나면 Artifacts에서 `app-debug.apk`를 내려받아 태블릿에 설치합니다.
