# MiniGit

A small version control system written from scratch in plain Java, following Git's own design: content-addressed objects, a staging area, commits that form a history, and branches.

MiniGit stores its data in a `.minigit` directory. Blobs, trees and commits use Git's exact object format, so their hashes match the ones Git computes for the same content, and `git --git-dir=.minigit log` can read a MiniGit history.

## Build

Requires JDK 11 or newer. There are no dependencies.

```bash
javac -d out $(find src/main/java -name '*.java')
```

```bash
java -cp out minigit.Main <command> [<args>]
```

## Commands

| Command | What it does |
| --- | --- |
| `init` | Create an empty repository in the current directory |
| `add <path>...` | Stage files or directories (deleted tracked files are staged as deletions) |
| `commit -m <message>` | Record the staged snapshot as a new commit |
| `status` | Show staged, unstaged and untracked changes |
| `log [--oneline]` | Show the commit history of the current branch |
| `branch [-d] [<name>] [<start>]` | List, create or delete branches |
| `checkout [-b] <branch \| commit>` | Switch branches, detach HEAD at a commit, or create and switch |
| `hash-object [-w] <file>` | Print a file's blob hash, optionally storing it |
| `cat-file (-t \| -s \| -p) <object>` | Print an object's type, size or content |

Commits use the name and email from `MINIGIT_AUTHOR_NAME` / `MINIGIT_AUTHOR_EMAIL`, or from the `[user]` section of `~/.gitconfig`.

## Example

```text
$ minigit init
Initialized empty MiniGit repository in /projects/demo/.minigit
$ echo "hello" > hello.txt
$ minigit add hello.txt
$ minigit commit -m "Add hello"
[main (root-commit) 3f1c2a9] Add hello
$ minigit checkout -b feature
Switched to a new branch 'feature'
$ minigit log --oneline
3f1c2a9 Add hello
```

## Ignoring files

List patterns in `.minigitignore`, one per line:

```text
# a name matches files or directories with that name anywhere
out
# a path with a slash matches from the repository root
docs/drafts
# globs match against each file or directory name
*.class
```

## How it works

```text
.minigit/
├── HEAD               "ref: refs/heads/main", or a commit hash when detached
├── index              staged files, one "<blob hash> <path>" line each
├── refs/heads/<name>  the commit each branch points at
└── objects/ab/cdef…   zlib-compressed objects named by their SHA-1 hash
```

- **Blob**: the bytes of one file.
- **Tree**: one directory, a sorted list of `mode name hash` entries that point to blobs and subtrees.
- **Commit**: a root tree, the parent commit(s), author, committer and message.

`add` writes blobs and records them in the index. `commit` turns the index into trees, writes a commit pointing at the root tree and the previous commit, and moves the current branch to it. `checkout` reads a commit's tree back out to the working directory and the index.

## Project layout

```text
src/main/java/minigit/
├── Main.java          command dispatch
├── cli/               one class per command
├── objects/           Blob, Tree, Commit, Signature and the ObjectStore
├── repository/        Repository (HEAD and refs), Index, WorkingTree, Status
├── hashing/           SHA-1 and hex helpers
├── filesystem/        file I/O helpers
└── exceptions/        MiniGitException for user-facing errors
```
