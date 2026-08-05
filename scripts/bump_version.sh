#!/bin/bash
# 版本号自动递增脚本
# 用法: ./bump_version.sh [major|minor|patch]

set -e

TYPE=${1:-patch}

if [ ! -f "version.properties" ]; then
    echo "version.properties not found!"
    exit 1
fi

# 读取当前版本
MAJOR=$(grep "versionMajor" version.properties | cut -d'=' -f2 | tr -d ' ')
MINOR=$(grep "versionMinor" version.properties | cut -d'=' -f2 | tr -d ' ')
PATCH=$(grep "versionPatch" version.properties | cut -d'=' -f2 | tr -d ' ')
CODE=$(grep "versionCode" version.properties | cut -d'=' -f2 | tr -d ' ')

# 递增
case $TYPE in
    major)
        MAJOR=$((MAJOR + 1))
        MINOR=0
        PATCH=0
        ;;
    minor)
        MINOR=$((MINOR + 1))
        PATCH=0
        ;;
    patch|*)
        PATCH=$((PATCH + 1))
        ;;
esac

CODE=$((CODE + 1))

# 写回
cat > version.properties << EOF
# 版本管理 - 修改此处后提交，GitHub Action 会自动构建新版本
versionMajor=$MAJOR
versionMinor=$MINOR
versionPatch=$PATCH
versionCode=$CODE
EOF

echo "✅ 版本已更新: $MAJOR.$MINOR.$PATCH (code=$CODE)"

# 提示创建 tag
echo ""
echo "下一步:"
echo "  git add version.properties"
echo "  git commit -m 'chore: bump version to $MAJOR.$MINOR.$PATCH'"
echo "  git tag v$MAJOR.$MINOR.$PATCH"
echo "  git push && git push --tags"
