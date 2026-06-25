# Git Best Practices & Workflow Guide for TransactSphere

This guide outlines the standard Git workflow, branching strategy, daily practices, and troubleshooting tips for our group project. Following these guidelines will prevent code conflicts, ensure history remains clean, and make collaboration seamless.

---

## 1. Feature Branch Workflow (Recommended)

In a group project, **never commit directly to the `main` branch**. Always work in a feature or bugfix branch, test your changes, and then merge them into `main`.

### Step-by-Step Branching:

1. **Start from the latest `main` branch:**
   Always make sure you have the latest updates before creating a new branch.
   ```bash
   git checkout main
   git pull origin main
   ```

2. **Create and switch to your feature branch:**
   Use a clear naming convention: `feature/short-description` or `bugfix/short-description`.
   ```bash
   git checkout -b feature/add-git-guide
   ```
   *(Alternatively, you can use: `git switch -c feature/add-git-guide`)*

3. **Make changes and commit them:**
   Stage your changes and commit with a clear, descriptive message:
   ```bash
   git add git-for-project.md
   git commit -m "docs: add git best practices guide for the team"
   ```

4. **Push your branch to GitHub:**
   The first time you push a new branch, set the upstream branch:
   ```bash
   git push -u origin feature/add-git-guide
   ```
   For subsequent pushes on the same branch, simply run:
   ```bash
   git push
   ```

---

## 2. Daily Best Practices (Keep in Sync)

To minimize the risk of massive merge conflicts, adopt this daily routine:

* **Pull changes daily:** Before starting any coding session, pull the latest changes from `main` to ensure you aren't writing code on top of outdated code.
* **Keep your feature branch updated with `main`:** If your teammates have merged changes to `main` while you are still working on your branch, merge `main` into your feature branch to stay in sync:
  ```bash
  # While checked out on your feature branch:
  git fetch origin
  git merge origin/main
  ```
  *(Resolve any conflicts locally, test your code, and commit/push the merge).*
* **Commit often, but keep them logical:** Write small commits rather than one massive commit at the end of the week. This makes tracking down bugs and reverting mistakes much easier.

---

## 3. How to Bring Changes to the `main` Branch

There are two primary ways to merge your branch into `main`:

### Option A: Via GitHub Pull Request (PR) — *Highly Recommended*
1. Push your branch to GitHub.
2. Go to the repository on GitHub and click **Compare & pull request**.
3. Have a teammate review your code, verify it compiles, and merge the PR.
4. Once merged, delete your branch both on GitHub and locally:
   ```bash
   git checkout main
   git pull origin main
   git branch -d feature/add-git-guide
   ```

### Option B: Local Merging (If direct merging is allowed)
If your team does not enforce Pull Requests and you need to merge locally:
1. Switch to `main` and make sure it is updated:
   ```bash
   git checkout main
   git pull origin main
   ```
2. Merge your feature branch:
   ```bash
   git merge feature/add-git-guide
   ```
3. Push the merged changes to the remote repository:
   ```bash
   git push origin main
   ```
4. Delete your feature branch:
   ```bash
   git branch -d feature/add-git-guide
   ```

---

## 4. How to Undo / Revert Mistakes

Mistakes happen. Here is how to fix them depending on where the mistake is:

### Scenario A: You made changes in files but haven't committed them yet
If you want to discard your local uncommitted changes:
* **To discard changes in a specific file:**
  ```bash
  git restore <filename>
  ```
* **To discard all local changes in the current directory:**
  ```bash
  git restore .
  ```

### Scenario B: You staged a file (`git add`) but don't want it in the commit
If you want to unstage a file:
```bash
git restore --staged <filename>
```

### Scenario C: You want to edit the message of your last commit (Unpushed)
If you made a typo in your last commit message and haven't pushed it yet:
```bash
git commit --amend -m "Corrected commit message"
```

### Scenario D: You want to undo commits locally (Unpushed)
* **Option 1: Soft Reset (Keep your changes but undo the commit)**
  This removes the commit, but keeps all your modified files staged, so you can edit and recommit them:
  ```bash
  git reset --soft HEAD~1
  ```
* **Option 2: Hard Reset (Completely delete the commit and all changes)**
  > [!WARNING]
  > This will completely delete the last commit and all work done in it. It cannot be easily undone.
  ```bash
  git reset --hard HEAD~1
  ```

### Scenario E: You already pushed the bad commit to GitHub
**Never** use `git reset --hard` on commits that have already been pushed to a shared branch (like `main`), as it will rewrite history and break the repository for your teammates. Instead, use **`git revert`** to create a new commit that applies the exact opposite changes:
1. Find the commit hash using `git log --oneline`.
2. Revert the commit:
   ```bash
   git revert <commit-hash>
   ```
3. Push the revert commit:
   ```bash
   git push
   ```

---

## 5. Resolving Merge Conflicts

When two people modify the same line of the same file, Git won't know which version to keep. This is a merge conflict.

### How to resolve it:
1. When running `git merge` or `git pull`, Git will warn you about conflicts and mark files as `UU` (Unmerged).
2. Open the conflicting files. Look for conflict markers:
   ```text
   <<<<<<< HEAD
   Your changes on this branch
   =======
   Changes made by your teammate on main
   >>>>>>> origin/main
   ```
3. Edit the file to keep the correct code, remove the conflict markers (`<<<<<<<`, `=======`, `>>>>>>>`).
4. Save the file.
5. Add the resolved files to staging:
   ```bash
   git add <filename>
   ```
6. Complete the merge commit:
   ```bash
   git commit -m "merge: resolve merge conflicts with main"
   ```
