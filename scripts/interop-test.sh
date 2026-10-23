#!/usr/bin/env bash
# End-to-end check against real Git: build a history with MiniGit, then confirm
# that Git reads it and computes the same tree hash for the same files.
set -euo pipefail

JAR="$(cd "$(dirname "$0")/.." && pwd)/target/minigit.jar"
minigit() { java -jar "$JAR" "$@"; }

export MINIGIT_AUTHOR_NAME="CI" MINIGIT_AUTHOR_EMAIL="ci@example.com"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
cd "$work"

minigit init
echo "hello" > hello.txt
mkdir -p src/deep
echo "class A {}" > src/A.java
echo "nested" > src/deep/n.txt
minigit add .
minigit commit -m "First commit"

minigit checkout -b feature
echo "second" >> hello.txt
minigit add hello.txt
minigit commit -m "Second commit"
minigit checkout main
grep -qx "hello" hello.txt || { echo "checkout did not restore main's hello.txt"; exit 1; }
minigit checkout feature

# Git must read the history MiniGit wrote.
count="$(git --git-dir=.minigit rev-list --count HEAD)"
[ "$count" = "2" ] || { echo "expected 2 commits, git saw $count"; exit 1; }
git --git-dir=.minigit log --oneline

# Git must compute the same tree for the same working files.
minigit_tree="$(git --git-dir=.minigit rev-parse 'HEAD^{tree}')"
export GIT_WORK_TREE="$work"
git_tree="$(GIT_DIR=.realgit git init -q && GIT_DIR=.realgit git add hello.txt src && GIT_DIR=.realgit git write-tree)"
unset GIT_WORK_TREE
[ "$minigit_tree" = "$git_tree" ] || { echo "tree mismatch: minigit $minigit_tree, git $git_tree"; exit 1; }

# Every object MiniGit wrote must be valid to Git.
for object in $(find .minigit/objects -type f | sed -E 's#.*/objects/(..)/#\1#'); do
    git --git-dir=.minigit cat-file -e "$object"
done

echo "interop OK: tree $minigit_tree"
