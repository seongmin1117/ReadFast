#!/bin/bash

# ReadFast 스크립트 실행 도우미
# 프로젝트 루트에서 scripts 폴더의 스크립트를 쉽게 실행

SCRIPT_DIR="scripts"

if [ ! -d "$SCRIPT_DIR" ]; then
    echo "❌ scripts 디렉토리를 찾을 수 없습니다."
    exit 1
fi

# 인자가 없으면 도움말 표시
if [ $# -eq 0 ]; then
    echo "🐍 ReadFast 테스트 데이터 생성 도구"
    echo ""
    echo "사용법:"
    echo "  $0 setup                    # Python 환경 설정"
    echo "  $0 generate [옵션]          # 테스트 데이터 생성"
    echo "  $0 large                    # 대용량 데이터 생성 (3개월)"
    echo "  $0 verify [날짜]            # 데이터 검증"
    echo ""
    echo "예시:"
    echo "  $0 generate --start-date 2025-01-01 --end-date 2025-01-03 --records-per-day 100"
    echo "  $0 verify 2025-01-01"
    echo ""
    echo "자세한 정보: $SCRIPT_DIR/README.md"
    exit 0
fi

COMMAND=$1
shift

case $COMMAND in
    "setup")
        echo "🔧 Python 환경 설정 중..."
        cd $SCRIPT_DIR && ./setup_python.sh
        ;;
    "generate")
        echo "📊 테스트 데이터 생성 중..."
        cd $SCRIPT_DIR && ./generate_test_data.py "$@"
        ;;
    "large")
        echo "🚀 대용량 테스트 데이터 생성 중..."
        cd $SCRIPT_DIR && ./generate_large_dataset.sh
        ;;
    "verify")
        if [ -z "$1" ]; then
            echo "❌ 검증할 날짜를 입력하세요. 예: $0 verify 2025-01-01"
            exit 1
        fi
        echo "🔍 데이터 검증 중..."
        cd $SCRIPT_DIR && ./generate_test_data.py --verify "$1"
        ;;
    *)
        echo "❌ 알 수 없는 명령: $COMMAND"
        echo "사용법을 보려면: $0"
        exit 1
        ;;
esac