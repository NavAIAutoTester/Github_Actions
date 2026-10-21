# How this project runs JUnit tests on GitHub Actions

This guide is for someone who knows basic Java and is new to GitHub Actions. It follows one small Maven project, `navaneeth:Github_Actions:1.0-SNAPSHOT`. The project compiles as Java 17. Its only test class is `navaneeth.CalculatorTest`. Maven Surefire Plugin 3.6.0 runs that class.

Read the first six sections to understand the workflow that is in the repo today. Sections 7 and 8 explain the reports and the Maven commands. Sections 9 and 10 are extra options. The workflow file does not use those options yet.

1. [This project](#1-this-project)
2. [What GitHub Actions is](#2-what-github-actions-is)
3. [What GitHub Actions is used for](#3-what-github-actions-is-used-for)
4. [The workflow file, line by line](#4-the-workflow-file-line-by-line)
5. [How the JUnit tests run](#5-how-the-junit-tests-run)
6. [When the job passes or fails](#6-when-the-job-passes-or-fails)
7. [The test reports](#7-the-test-reports)
8. [Maven commands](#8-maven-commands)
9. [Run the tests on one branch](#9-run-the-tests-on-one-branch)
10. [More events, actions, steps, and jobs](#10-more-events-actions-steps-and-jobs)

## 1. This project

The workflow file is `.github/workflows/maven-junit.yml`.

Run the same tests on your own machine from the project directory:

```shell
mvn -B clean test
```

That command only runs on your machine. GitHub Actions starts when GitHub receives an event for this repository, such as a push or a pull request.

## 2. What GitHub Actions is

GitHub Actions runs tasks for a repository on computers that GitHub provides. The tasks are written in a file in the repository, so they stay with the code.

GitHub looks for workflow files in `.github/workflows` at the root of the repository. It reads the file from the commit on GitHub that the event points at. Saving the file on your laptop does nothing until that file is in a commit GitHub has.

These are the words used in the rest of this guide. They are listed in the order a run uses them.

| Word | Plain meaning in this project |
| --- | --- |
| **Event** | The signal that starts a run. This file listens for `push` and `pull_request`. |
| **Workflow** | One automated process. It is the YAML file `.github/workflows/maven-junit.yml`. On the Actions tab its name is "Maven JUnit tests". |
| **Job** | A list of tasks that run on one computer. This file has one job. Its id is `maven-junit-tests`. Its display name is "Maven JUnit tests". |
| **Runner** | The computer that runs the job. `ubuntu-latest` means a current Ubuntu machine hosted by GitHub. GitHub creates it for the job and deletes it when the job ends. |
| **Step** | One task inside the job. Steps run in order. A step can run a shell command (`run`) or call a ready-made action (`uses`). |
| **Action** | A ready-made step, with a version. This file uses `actions/checkout@v7`, `actions/setup-java@v6`, and `actions/upload-artifact@v7`. The `@v7` or `@v6` part is the major version. |
| **Artifact** | Files saved from the runner and attached to the run. This workflow saves `target/surefire-reports` so you can download the test reports after the computer is gone. |

## 3. What GitHub Actions is used for

For this project, Actions is continuous integration. On every push, and when a pull request is opened, updated, or reopened, GitHub compiles the code and runs JUnit:

```shell
mvn -B clean test
```

If that command exits with a code other than 0, the "Run unit tests" step fails. A failed step fails the job. A failed job fails the workflow run. On a pull request, the check named "Maven JUnit tests" is red.

That red check blocks the merge only when the repository requires the "Maven JUnit tests" check before merge. That requirement is a setting on the GitHub repository. The workflow file does not create it. With the setting turned on, the pull request stays blocked until a later run is green.

Teams also use Actions for other work. This workflow does not do these. Examples are in [section 10](#10-more-events-actions-steps-and-jobs):

- **Build** a jar with `mvn package`.
- **Lint** the code and fail on style problems.
- **Package** and publish a jar after the tests pass.
- **Deploy** the build to a server.
- **Run on a schedule**, such as every Monday.
- **Run by hand** from the Actions tab.

## 4. The workflow file, line by line

This is `.github/workflows/maven-junit.yml`, in the same order as the file.

### The name

```yaml
name: Maven JUnit tests
```

`name` is the title in the Actions tab. It is the workflow name. The job name below can be the same words. A clear name makes the run easy to find.

### What starts the run

```yaml
on:
  push:
  pull_request:
    types:
      - opened
      - synchronize
      - reopened
```

`on` lists the events that start a run.

`push` has no branch list, so a push of any branch starts the workflow. A tag push starts it too. GitHub uses the workflow file in the commit that was pushed, and checkout uses that commit. [Section 9](#9-run-the-tests-on-one-branch) shows how to limit this to one branch.

`pull_request` is limited to three activities:

- `opened` — someone opened a new pull request.
- `synchronize` — someone pushed new commits to the pull request branch. GitHub runs the tests again so the check matches the new code.
- `reopened` — a closed pull request was opened again.

Editing the title or adding a label does not start this workflow. For a pull request, checkout uses GitHub's test merge: the pull request branch merged into the target branch. The tests run on the code as it would look after the merge. GitHub uses the workflow file from that pull request, so a change to this YAML in the pull request is the file that runs.

`git commit` and `git pull` are not events.

`git commit` only saves a commit on your machine. GitHub does not see it. The matching event is `push`, which happens when you send those commits with `git push`.

`git pull` only updates your machine. When someone pushes more commits onto a branch that already has an open pull request, GitHub sends `pull_request` with type `synchronize`. That event runs the tests again.

This file does not use `pull_request_target`. That event runs the workflow from the target branch and is for special cases that need the base repository's credentials. Normal test runs belong on `pull_request`.

### The job, the computer, and permissions

```yaml
permissions:
  contents: read

jobs:
  maven-junit-tests:
    name: Maven JUnit tests
    runs-on: ubuntu-latest
```

`permissions` limits the workflow token. `contents: read` lets checkout read the repository. The job cannot push new commits.

`maven-junit-tests` is the job id. You will see it in the run URL. `name` is the label people see. It is also the check name a branch rule can require: "Maven JUnit tests".

`runs-on: ubuntu-latest` asks for a current Ubuntu computer from GitHub. The computer does not have this repo until the checkout step. The next step installs the JDK this project asks for.

### Step 1: get the code

```yaml
- name: Check out repository
  uses: actions/checkout@v7
```

`actions/checkout@v7` copies the repository onto the runner. Later steps, including `mvn`, run in that folder, so `pom.xml` and `target/` are in the expected places. Until this step finishes, the computer does not have `Calculator.java` or `CalculatorTest.java`.

### Step 2: install Java 17

```yaml
- name: Set up Temurin JDK 17
  uses: actions/setup-java@v6
  with:
    distribution: temurin
    java-version: "17"
    cache: maven
```

`actions/setup-java@v6` installs a JDK and sets `JAVA_HOME`.

- `distribution: temurin` selects Eclipse Temurin, the Adoptium build of OpenJDK.
- `java-version: "17"` matches `maven.compiler.release` in `pom.xml`.
- `cache: maven` saves downloaded Maven files between runs. The next run can reuse them. If the cache is empty, Maven downloads what it needs. The result is the same either way.

### Step 3: run the tests

```yaml
- name: Run unit tests
  run: mvn -B clean test
```

`run` starts a shell command. This step has no `if`, so it runs when the earlier steps succeeded.

`-B` is batch mode. Maven will not stop and ask a question. The log stays moving, and the run can be repeated.

`clean test` deletes `target/`, compiles the code, compiles the tests, and runs them. [Section 8](#8-maven-commands) explains why `clean` is in the command.

The step passes when Maven exits 0. It fails when Maven exits with any other code. A failed or erroring JUnit test makes Surefire, and then Maven, exit with a non-zero code. The Actions log for this step is the Maven log, including `Tests run`, `Failures`, `Errors`, and `Skipped`.

### Step 4: save the reports

```yaml
- name: Upload Surefire reports
  if: always()
  uses: actions/upload-artifact@v7
  with:
    name: surefire-reports
    path: target/surefire-reports/
    retention-days: 14
    if-no-files-found: warn
```

`actions/upload-artifact@v7` copies `target/surefire-reports/` off the runner and attaches it to the run under the name `surefire-reports`.

`if: always()` matters. The normal rule is "run this step only if the earlier steps passed." After a failed test, that rule would skip the upload, and the reports would be deleted with the computer. `always()` runs the upload after a failed test too, so you can download the reports.

`path` is relative to the repository root on the runner. Surefire writes that folder during the test step. `retention-days: 14` keeps the files for 14 days. After that, GitHub deletes them. The log of the run can remain. A repository setting can set a lower maximum. 14 days is inside the usual limit.

`if-no-files-found: warn` covers a run where Surefire never created the folder, for example because the code did not compile. The upload prints a warning. That warning alone does not fail the job. The Maven step has already failed the job.

A skipped upload would mean this step's `if` was false. Skipping a step does not, by itself, fail the job. In this file the upload runs after success and after failure, because `if` is `always()`. If someone changed it back to the normal "only after success" rule, a failed test would skip the upload. The reports would exist only in the log.

The full rules for a green check, a red X, a skip, and a cancellation are in the next sections.

## 5. How the JUnit tests run

This is one run, from the GitHub event to the check mark.

1. **Event.** You push, or a pull request is opened, updated (`synchronize`), or reopened. A local `git commit` stops before this step.
2. **Workflow.** GitHub loads `.github/workflows/maven-junit.yml` from that commit and queues the "Maven JUnit tests" job.
3. **Computer.** GitHub starts a fresh `ubuntu-latest` machine for `maven-junit-tests`.
4. **Checkout.** `actions/checkout@v7` puts the repository on disk.
5. **JDK.** `actions/setup-java@v6` installs Temurin 17 and restores the Maven cache when one exists.
6. **Maven.** `mvn -B clean test` deletes `target/`, compiles `src/main/java` (including `navaneeth.Calculator`) for Java 17, compiles `src/test/java`, and hands the tests to Surefire.
7. **Surefire.** Surefire 3.6.0 chooses the JUnit Platform provider. A local run of this project logs `Using auto detected provider org.apache.maven.surefire.junitplatform.JUnitPlatformProvider`.
8. **JUnit 5.** The `junit-jupiter` dependency brings the JUnit Jupiter engine. That engine finds the tests.
9. **Test class.** Surefire looks for `**/Test*.java`, `**/*Test.java`, `**/*Tests.java`, and `**/*TestCase.java`. `CalculatorTest.java` matches `**/*Test.java`. The engine runs each method marked `@Test`: `addReturnsSum` and `subtractReturnsDifference`. `@DisplayName` is the human-readable label in the source. The Surefire XML from a passing run records the method name, not the display name.
10. **Result.** Each `assertEquals` compares the expected value with the value `Calculator` returned. If they differ, JUnit throws `org.opentest4j.AssertionFailedError` and includes the message from the test, for example `2 + 3 should equal 5`. Surefire records a **failure**. An unexpected exception that is not an assertion is an **error**. Either one makes Surefire, then Maven, exit non-zero. The step fails.
11. **Job mark.** Exit code 0 makes the Maven step pass. Any other exit code fails the step, the job, and the workflow run. The upload still runs because of `if: always()`.

### How to read the log and run it again

After the workflow has run on GitHub, open the **Actions** tab, select the "Maven JUnit tests" run, and open the "Maven JUnit tests" job.

- Expand **Run unit tests** for the Maven log and the Surefire summary.
- Expand **Upload Surefire reports** to see if the files were saved.
- On the run summary, **Artifacts** lists `surefire-reports` for download.

To run it again, open the same run and choose **Re-run failed jobs** or **Re-run all jobs**. GitHub starts a new attempt on a new computer. It does not reconnect to the old one.

## 6. When the job passes or fails

GitHub marks the job after the steps finish. In this file the job is **Maven JUnit tests**. That same name is the check on a commit and on a pull request. There is one job, so the workflow run is green when the job passes and red when the job fails.

### Passed

The job gets a green check when every step that ran finishes successfully.

- **Check out repository** copies the code.
- **Set up Temurin JDK 17** installs JDK 17.
- **Run unit tests** exits 0. Maven prints `BUILD SUCCESS`. Surefire prints `Failures: 0` and `Errors: 0`. For this project that line is `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`.
- **Upload Surefire reports** stores the files, or prints a warning and still exits 0 when the folder is missing and `if-no-files-found` is `warn`.

A step that is skipped because its `if` is false does not spoil a pass. JUnit methods that the suite itself skips also leave the job green, as long as the failure count and the error count stay at 0.

### Failed

The job gets a red X when any step that ran fails, and that step does not set `continue-on-error: true`. One failed step is enough. A later step that succeeds does not turn the job green. The report upload can succeed after a test failure, and the job stays red.

The usual failure is **Run unit tests**:

- An assertion fails. `addReturnsSum` throws `AssertionFailedError` with a message such as `2 + 3 should equal 5`. Surefire sets `Failures` above 0. Maven exits non-zero. The step is red.
- A test throws some other exception. Surefire counts an **error**. Maven still exits non-zero, so the job fails in the same way.
- The code does not compile, or Maven cannot download a plugin or a dependency. The build stops before a green test summary. The step is red.

The other steps can fail the job too. Checkout fails if the commit cannot be fetched. Java setup fails if JDK 17 cannot be installed. The upload fails if GitHub cannot store the files. A missing reports folder only warns, because this file sets `if-no-files-found: warn`. If the Maven step already failed, the job is red anyway.

### Skipped

The job is grey and marked skipped when its steps never start.

- The job's `if` is false. An example is `if: github.ref == 'refs/heads/main'` while the run is on another branch.
- A later job lists `needs: test`, and the test job failed or was skipped. The later job is skipped.

A skipped job is not a failed job. If every job that actually ran succeeded, the workflow run stays green. On a pull request, GitHub treats that skipped check as passing for a required check, so the skip does not block the merge.

A workflow that never started is different. A `branches` or `paths` filter can exclude the event. GitHub then creates no check. A required check can stay pending and block the merge until a run reports it.

### Cancelled

The job is cancelled when it is stopped before the steps finish.

- Someone presses **Cancel workflow**.
- A newer run on the same branch replaces this one. That needs the `concurrency` setting in [section 10](#10-more-events-actions-steps-and-jobs).
- The job runs longer than `timeout-minutes`.

A cancelled job is not a pass. The workflow run is not green, and a required check is not satisfied.

### More than one job

Each job has its own mark. The workflow run fails if any job failed or was cancelled. The run passes when every job passed or was skipped.

In the two-job example in [section 10](#10-more-events-actions-steps-and-jobs), a failed test job fails the run, and the package job is skipped because it needs the test job. A matrix job fails if any JDK row fails. The default `fail-fast: true` cancels the other rows. `fail-fast: false` lets the other rows finish. The job is still failed if one row failed.

`continue-on-error` changes the mark. On a **step**, the command can fail and the job can still pass. Leave it off `mvn -B clean test`, or a broken `CalculatorTest` would look green. On a **job**, that job stays failed, later jobs still run, and the workflow run can still pass.

## 7. The test reports

Surefire writes `target/surefire-reports` while the tests run. After `mvn -B clean test` on this project, that folder contains:

- `TEST-navaneeth.CalculatorTest.xml` — one XML file for the test class
- `navaneeth.CalculatorTest.txt` — a short text summary of the same class

The text file from a passing run looks like this:

```text
-------------------------------------------------------------------------------
Test set: navaneeth.CalculatorTest
-------------------------------------------------------------------------------
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.089 s -- in navaneeth.CalculatorTest
```

The XML root is `testsuite`. These values are from the passing run of `CalculatorTest`. The `time` value will vary a little:

| Attribute | Meaning in this run |
| --- | --- |
| `name` | `navaneeth.CalculatorTest`, the full class name |
| `tests` | `2`, the number of `@Test` methods |
| `failures` | `0`, assertion failures |
| `errors` | `0`, unexpected exceptions |
| `skipped` | `0`, tests that were skipped |
| `time` | seconds spent in the class |
| `flakes` | `0` on this report |

Each method is a `testcase` with `name` (the method), `classname` (`navaneeth.CalculatorTest`), and `time`. The passing file has no nested result:

```xml
<testsuite name="navaneeth.CalculatorTest" tests="2" errors="0" skipped="0" failures="0" time="0.089">
  <testcase name="subtractReturnsDifference" classname="navaneeth.CalculatorTest" time="0.05"/>
  <testcase name="addReturnsSum" classname="navaneeth.CalculatorTest" time="0.004"/>
</testsuite>
```

The same file also has a `properties` list: details about the computer that ran the tests. Those values can include local paths, so leave the reports under `target/`. This repository's `.gitignore` ignores `target/`.

When an assertion fails, Surefire sets `failures` to at least 1 and adds a `failure` child. The `message` has the assertion text and the expected and actual values. The `type` is the exception class. The body is the stack trace. For a wrong `Calculator.add` result, the shape is:

```xml
<testcase name="addReturnsSum" classname="navaneeth.CalculatorTest" time="0.012">
  <failure message="2 + 3 should equal 5 ==&gt; expected: &lt;5&gt; but was: &lt;0&gt;" type="org.opentest4j.AssertionFailedError"><![CDATA[org.opentest4j.AssertionFailedError: 2 + 3 should equal 5 ==> expected: <5> but was: <0>
	at navaneeth.CalculatorTest.addReturnsSum(CalculatorTest.java:...)
]]></failure>
</testcase>
```

That snippet shows the shape of a failure. The passing run above has no `failure` element. If a test prints to standard output or standard error, the XML also gets `system-out` or `system-err`. These sample tests do not print, so those elements are absent.

The `version` attribute on `testsuite` is the report format version. It is not the Surefire plugin version. This project runs plugin `3.6.0`.

### Why the upload matters

The runner's disk, including `target/`, is deleted when the job ends. The upload keeps a copy with the run for 14 days. Download `surefire-reports` from the run page to open the same XML and text files, including after a failed test.

### Open the reports on your machine

From the project directory:

```shell
mvn -B clean test
```

Then open:

- `target/surefire-reports/navaneeth.CalculatorTest.txt`
- `target/surefire-reports/TEST-navaneeth.CalculatorTest.xml`

`clean` deletes the old `target/` first, so these files describe only that command.

### The log and the files

The log is what Maven prints in the terminal and in the Actions step:

```text
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 0.089 s -- in navaneeth.CalculatorTest
[INFO] Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
```

The first line is one test class. The second line is the whole module. Around them, the log also has the Maven plugin lines (`clean`, `compiler`, `surefire`) and, on a failure, the assertion stack trace.

The files on disk are the saved copy of the same result. The `.txt` file is the class summary without the rest of the Maven log. The `.xml` file has one `testcase` per method, which other tools can read. The log stays in the Actions page even if the upload is skipped. The XML and text files are still available after the computer is deleted only because the upload saved them.

## 8. Maven commands

In GitHub Actions, call Maven with `-B` (batch mode) and one clear command. Batch mode keeps the computer from waiting for a person to answer a question. One command makes the log show exactly what ran, so you can use the same line on your machine. This workflow's test step is:

```shell
mvn -B clean test
```

Each command below says what it does, when it belongs in CI, and what it leaves undone.

### `mvn -B test`

Runs the build through the `test` phase: prepare resources, compile, compile the tests, and run Surefire. With this `pom.xml`, that runs `CalculatorTest`.

Use it when the folder is already empty and you only need the unit tests. A new GitHub computer has no old `target/`, so the result matches a clean test run.

It does not delete an existing `target/`. It does not package, verify, install, or deploy, so it does not build the jar and does not publish anything.

### `mvn -B clean test`

This is the command in `.github/workflows/maven-junit.yml`.

`clean` deletes `target/`, including old class files and old reports. `test` then compiles and runs JUnit from scratch. The reports in `target/surefire-reports` belong only to this run.

`clean` matters because an old `target/` can hide the real result. An old class file can make a test pass after the source was fixed, or an old XML file can be uploaded next to a new one. GitHub-hosted computers start empty, but `clean` keeps the CI command the same as a careful local run. It also keeps the result correct on a self-hosted computer that reuses a folder.

It does not build the jar, run the `verify` phase, install into your local Maven repository, or deploy.

### `mvn -B -DskipTests package`

Builds through `package` and skips running the tests. Maven still compiles the main code and the test code, then writes `target/Github_Actions-1.0-SNAPSHOT.jar`.

Use it only when that job is not the test gate. A packaging job can do this after a separate test job has already passed. This workflow does not use it. Skipping tests in the only CI job would make a broken `Calculator` look green.

It does not run JUnit. It does not deploy the jar. `-DskipTests` skips running the tests and still compiles them. `-Dmaven.test.skip=true` skips compiling them and running them.

### `mvn -B verify`

Runs through `verify`: everything `test` does, then `package`, then the integration-test and verify phases.

Use it when the build must also fail on integration tests, or on a plugin bound to `verify`. The Maven Failsafe Plugin is the usual integration-test plugin. This project has no Failsafe tests and no extra verify plugin, so `verify` runs `CalculatorTest` and also builds the jar.

It does not install the jar into the local repository and does not deploy it.

### `mvn -B -Dtest=CalculatorTest test` and one method

`-Dtest` chooses which tests Surefire runs.

```shell
mvn -B -Dtest=CalculatorTest test
```

runs `navaneeth.CalculatorTest` and leaves out any other test class.

```shell
mvn -B -Dtest=CalculatorTest#addReturnsSum test
```

runs only `addReturnsSum`. Surefire accepts the simple class name when it is unique. `-Dtest=navaneeth.CalculatorTest` names the class in full.

Use these when you are looking at one class or one method. They are a weak default for the main pull-request check, because a green run says nothing about the tests you left out.

They do not run the rest of the suite. They still compile the main code. They do not package or deploy. A pattern that matches nothing fails the build, unless the build is configured to allow zero tests.

### `mvn -B surefire:test`

Calls the Surefire goal directly. It does not walk the normal build, so it does not compile first.

Use it only after an earlier step has already compiled the main classes and the test classes into `target/`. On a fresh computer, as the first Maven command, the class files are not there yet, so `CalculatorTest` cannot run reliably.

It does not run `clean`, `compile`, or `test-compile` for you. It does not package or deploy. The workflow uses `mvn -B clean test` so a new computer compiles and tests in one command.

## 9. Run the tests on one branch

The workflow in this repository has no `branches` list. A push of `main`, `feature/calculator`, or any other branch starts "Maven JUnit tests". Add `branches` when the tests should run only on the branches you name.

`push` and `pull_request` use that list for different branches:

- Under `push`, `branches` is the branch you pushed. `branches: [main]` starts the workflow when commits land on `main`. A push to `feature/calculator` does not start it.
- Under `pull_request`, `branches` is the **target** branch, the branch the pull request merges into. `branches: [main]` starts the workflow for a pull request into `main`. The source branch can have any name. A pull request into `develop` does not match.

This example runs the tests when someone pushes `main` or `feature/calculator`, and when a pull request targets `main`:

```yaml
on:
  push:
    branches:
      - main
      - feature/calculator
  pull_request:
    branches:
      - main
    types:
      - opened
      - synchronize
      - reopened
```

A plain name is one branch. A pattern covers many branches. Put quotes around a pattern that contains `*`:

```yaml
on:
  push:
    branches:
      - main
      - "release/**"
      - "feature/**"
```

`release/**` matches `release/1.0` and `release/1.0/hotfix`. It does not match a branch named only `release`.

`branches-ignore` is the other kind of list: every branch starts the workflow except the ones you name. Use `branches` or `branches-ignore` on an event, not both. Adding `branches` also stops tag pushes from starting the workflow. The current file runs on tags because it sets neither `branches` nor `tags`. To keep tag builds, add a `tags` list:

```yaml
on:
  push:
    branches:
      - main
    tags:
      - "v*"
```

`v*` matches tags such as `v1.0.0`.

GitHub reads the workflow file that is **on that branch**. `.github/workflows/maven-junit.yml` must already be committed on `feature/calculator` before a push of that branch can run this suite. A branch that does not contain the file gives GitHub nothing to start.

### Start a run by pushing the branch

From the project directory, with the workflow committed on the branch:

```shell
git checkout feature/calculator
git push origin feature/calculator
```

`git push origin feature/calculator` sends that branch to GitHub. That is the `push` event. If `feature/calculator` is in `on.push.branches`, GitHub queues "Maven JUnit tests" and checks out `feature/calculator`. Checking out the branch on your machine does not start Actions.

If that branch already has an open pull request into `main`, the same push also sends `pull_request` with type `synchronize`. With the example above, that second run happens because the pull request targets `main`.

### Start a run from the Actions tab

The current file has no `workflow_dispatch` line, so the tab does not offer a branch picker yet. Add the event:

```yaml
on:
  push:
    branches:
      - main
      - feature/calculator
  pull_request:
    branches:
      - main
    types:
      - opened
      - synchronize
      - reopened
  workflow_dispatch:
```

After that change is on the repository's default branch, open the **Actions** tab, select **Maven JUnit tests**, and choose **Run workflow**. **Use workflow from** is the branch GitHub will check out. Pick `feature/calculator` or `main`, then run it. That branch must contain `.github/workflows/maven-junit.yml`. GitHub runs the YAML from the branch you picked.

A manual run is the `workflow_dispatch` event. It does not need a new commit. In the log, `github.ref` is `refs/heads/feature/calculator` when that branch was selected, and `github.sha` is the latest commit on the branch.

### Let the workflow start, then skip the job

A `branches` list decides whether a run is created. A job `if` decides whether the job does any work. This job runs Maven only on `main`. The workflow can still start for other branches:

```yaml
jobs:
  maven-junit-tests:
    name: Maven JUnit tests
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v7
      - uses: actions/setup-java@v6
        with:
          distribution: temurin
          java-version: "17"
          cache: maven
      - run: mvn -B clean test
```

On `feature/calculator` the job is skipped. [Section 6](#6-when-the-job-passes-or-fails) explains that skip. For a pull request, `github.ref` is a merge ref such as `refs/pull/7/merge`, not `refs/heads/main`. Use `github.base_ref == 'main'` to follow the pull request's target branch. Use `github.head_ref == 'feature/calculator'` to follow the source branch.

## 10. More events, actions, steps, and jobs

`.github/workflows/maven-junit.yml` uses two events, one job, and three actions. You can start the same Maven build in other ways, split it across jobs, and call other actions. The YAML in this section is an example. It is not a second file in the repo.

### Other events

An event name goes under `on`. Many events also have activity types. If you list `types`, only those activities start the workflow. If you omit `types`, GitHub uses that event's usual set.

| Event | What starts a run | How it fits this project |
| --- | --- | --- |
| `push` | Commits or tags are pushed | Run `mvn -B clean test` on the new commit. The current file does this for every branch. |
| `pull_request` | Something happens on a pull request | Run the tests as a check. The current file uses `opened`, `synchronize`, and `reopened`. |
| `workflow_dispatch` | Someone clicks **Run workflow**, or calls the API | Run the tests without a new commit. You can pass an input, such as a test class name. [Section 9](#9-run-the-tests-on-one-branch) shows the branch picker. |
| `workflow_call` | Another workflow calls this file | Share one Maven test definition with several callers. |
| `schedule` | A clock time in UTC | Run the suite on a timer, such as every Monday morning. |
| `release` | A GitHub release is published | Build the jar when a release exists. This project has no deploy step. |
| `workflow_run` | Another workflow in this repository finishes | Start a follow-up workflow after "Maven JUnit tests" completes. |
| `merge_group` | A merge queue builds the grouped commits | Run the same tests on the commit the queue is about to merge. |
| `create` | A branch or tag is created | React when the name appears. New commits on that branch still arrive as `push`. |
| `delete` | A branch or tag is deleted | Clean up something tied to that name. |
| `issues`, `issue_comment` | An issue is opened or someone comments | Useful for issue bots. These events do not compile `Calculator`. |

More `pull_request` activities you will see:

- `opened`, `synchronize`, `reopened` — the three this file already uses.
- `ready_for_review` — a draft pull request was marked ready.
- `closed` — the pull request was merged or closed. A job can read `github.event.pull_request.merged` when the work should happen only after a merge.
- `labeled` and `unlabeled` — a label was added or removed.
- `edited`, `assigned`, `review_requested` — title, people, or review requests changed. The Java files may be unchanged.

`pull_request_target` is a separate event. It loads the workflow from the target branch and runs with that repository's token. This project uses `pull_request`, so the workflow under test is the one from the pull request.

A workflow can listen to several events. Filters can also skip a run when only documentation changed:

```yaml
on:
  push:
    branches: [main]
    paths:
      - "src/**"
      - "pom.xml"
      - ".github/workflows/maven-junit.yml"
    paths-ignore:
      - "**.md"
  pull_request:
    branches: [main]
    types: [opened, synchronize, reopened, ready_for_review]
  workflow_dispatch:
    inputs:
      test-class:
        description: Surefire test class to run
        required: false
        default: CalculatorTest
        type: string
  schedule:
    - cron: "0 6 * * 1"
```

`paths` means the run starts when one of those files changes. `paths-ignore` skips a push that only changes Markdown, including `EXPLANATION.md`. A change to both `pom.xml` and a Markdown file still matches `paths`, so the tests run.

The value typed into `test-class` is available in a step as `${{ inputs.test-class }}`. The default runs `CalculatorTest`.

`schedule` uses five fields in UTC: minute, hour, day of month, month, day of week. `0 6 * * 1` is 06:00 UTC every Monday. GitHub reads the scheduled workflow from the default branch. The run can start a bit later when GitHub is busy. The shortest gap GitHub accepts is 5 minutes.

Inside a run, a step can read:

- `github.event_name` — `push`, `pull_request`, `workflow_dispatch`, `schedule`, and so on
- `github.sha` — the commit being built
- `github.ref` — the branch, tag, or pull request ref
- `github.actor` — the account that started the run

### Other actions

A step calls an action with `uses`. The text after `@` is the version. This project uses `actions/checkout@v7`, `actions/setup-java@v6`, and `actions/upload-artifact@v7`.

An action can come from three places:

- **GitHub's `actions` organization**, such as `actions/checkout@v7`. These are the ones this repository uses.
- **A folder in this repository**, such as `./.github/actions/print-maven-version`. This repository does not define one.
- **Another repository**, such as `some-org/some-action@v1`. Pin a major version or a full commit id so a surprise update cannot change the build.

**`actions/checkout@v7`** copies the repo. The current workflow calls it with no extra settings, so it checks out the commit for the event. `fetch-depth: 1` copies only the latest commit, which is enough to compile and test. Use a larger number, or `0` for the full history, when a step needs tags or older commits.

**`actions/setup-java@v6`** installs a JDK and can cache Maven downloads. The current file asks for Temurin 17 and `cache: maven`. Other builds it accepts include Zulu, Microsoft Build of OpenJDK, and Amazon Corretto. The action's readme lists the full set.

`java-version` can be `${{ matrix.java }}` when the job runs on more than one JDK. This `pom.xml` sets `maven.compiler.release` to 17, so the JDK must be 17 or newer. JDK 11 cannot compile this project.

`cache: maven` stores `~/.m2` between runs. `cache: gradle` and `cache: sbt` are for other build tools. This project is Maven, so `maven` is the matching value.

**`actions/upload-artifact@v7`** saves files from one job onto the run. The current file uploads `target/surefire-reports` for 14 days, including after a test failure.

**`actions/download-artifact@v8`** copies those files into a later job. Jobs do not share a disk. A package job that needs the Surefire XML downloads it:

```yaml
- name: Download Surefire reports
  uses: actions/download-artifact@v8
  with:
    name: surefire-reports
    path: target/surefire-reports/
```

**`actions/cache@v6`** is the general cache action. Use it when you want to choose the folder and the key yourself. For this `pom.xml`, `setup-java` with `cache: maven` already caches Maven downloads.

**`actions/github-script@v9`** runs a short script with the GitHub API, for example to comment a test summary on a pull request. The test result still comes from `mvn -B clean test`. The script only posts it.

### Other step settings

Steps in one job run in order on one computer and share the working folder. A file created by `mvn` is still there for the upload step. Each step is either an action (`uses`) or a shell command (`run`).

| Field | What it does |
| --- | --- |
| `name` | The label in the log. The current file names every step. |
| `id` | Lets a later step read `steps.<id>.outputs`. |
| `uses` | Calls an action. |
| `run` | Runs a shell command. On `ubuntu-latest` the shell is bash. |
| `with` | Inputs for the action. |
| `env` | Environment variables for that step only. |
| `if` | Decides whether the step runs. The normal rule is `success()`. |
| `working-directory` | Folder for a `run` step. This repo's `pom.xml` is at the root, so the current steps leave this out. |
| `shell` | `bash`, `pwsh`, or `python`, when the command needs a specific shell. |
| `continue-on-error` | `true` lets the job stay green when that step fails. |
| `timeout-minutes` | Stops the step if it runs longer than this. |

`if` values you will use with this project:

- `success()` — every earlier step passed. This is the normal rule. The Maven test step uses it.
- `failure()` — an earlier step failed. Use this for a step that should run only to report a failure.
- `always()` — run even after a failure. The Surefire upload uses this.
- `cancelled()` — the run was cancelled.
- `github.event_name == 'workflow_dispatch'` — this step runs only for a manual run.
- `github.ref == 'refs/heads/main'` — this step runs only on `main`.

This example is one possible job. It is not the job in the current file:

```yaml
steps:
  - name: Check out repository
    uses: actions/checkout@v7

  - name: Set up Temurin JDK 17
    uses: actions/setup-java@v6
    with:
      distribution: temurin
      java-version: "17"
      cache: maven

  - name: Show the JDK
    run: java -version

  - name: Run the requested test class
    if: github.event_name == 'workflow_dispatch'
    run: mvn -B -Dtest=${{ inputs.test-class }} test

  - name: Run the full suite
    if: github.event_name != 'workflow_dispatch'
    run: mvn -B clean test

  - name: Upload Surefire reports
    if: always()
    timeout-minutes: 5
    uses: actions/upload-artifact@v7
    with:
      name: surefire-reports
      path: target/surefire-reports/
      if-no-files-found: warn
```

On a push or a pull request, the full suite runs and the single-class step is skipped. On a manual run, only the requested class runs. The upload still runs either way because its `if` is `always()`.

A `run` step can hold several shell lines. They share one shell, so a variable set on the first line is still there on the second. The step fails if any line exits non-zero:

```yaml
- name: Compile, then test
  run: |
    mvn -B -DskipTests compile
    mvn -B test
```

The current workflow keeps this as one command, `mvn -B clean test`, so the log shows one Maven invocation.

`continue-on-error: true` and `if: always()` do different jobs. `always()` decides whether a step starts. `continue-on-error` lets a step fail while the job continues and can still pass. Keep `continue-on-error` off the test command. It can fit an optional reporting step, where a reporting problem should not hide a green test run.

### Other job shapes

A job is one computer plus its steps. The job id is the map key (`maven-junit-tests`). The `name` is the check title ("Maven JUnit tests"). This file has one job, so the run and that check pass or fail together.

**Side by side.** Two jobs with no `needs` start at the same time, each on its own computer. One can test on Ubuntu while another tests on Windows. They do not see each other's `target/` folder.

**One after another.** `needs` waits for another job to pass. A package job with `needs: test` does not start when the tests fail. Each job still gets a new empty computer, so the package job checks out the code again. To reuse files, the first job uploads an artifact and the second job downloads it.

**The same job on several JDKs.** `strategy.matrix` repeats one job. `fail-fast: false` lets the other JDKs finish after one of them fails.

```yaml
jobs:
  test:
    name: Tests on JDK ${{ matrix.java }}
    runs-on: ubuntu-latest
    strategy:
      fail-fast: false
      matrix:
        java: ["17", "21"]
    steps:
      - uses: actions/checkout@v7
      - uses: actions/setup-java@v6
        with:
          distribution: temurin
          java-version: ${{ matrix.java }}
          cache: maven
      - run: mvn -B clean test
```

JDK 17 matches `maven.compiler.release`. JDK 21 can compile that same level. Each row is its own check, named with the JDK number. If any row fails, the job fails. See [section 6](#6-when-the-job-passes-or-fails).

**Tests, then the jar.** The first job runs the tests. The second builds the jar only after the tests pass. The commands are the ones in [section 8](#8-maven-commands).

```yaml
jobs:
  test:
    name: Maven JUnit tests
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v7
      - uses: actions/setup-java@v6
        with:
          distribution: temurin
          java-version: "17"
          cache: maven
      - run: mvn -B clean test
      - if: always()
        uses: actions/upload-artifact@v7
        with:
          name: surefire-reports
          path: target/surefire-reports/
          retention-days: 14
          if-no-files-found: warn

  package:
    name: Package the jar
    needs: test
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v7
      - uses: actions/setup-java@v6
        with:
          distribution: temurin
          java-version: "17"
          cache: maven
      - run: mvn -B -DskipTests package
      - uses: actions/upload-artifact@v7
        with:
          name: jar
          path: target/Github_Actions-1.0-SNAPSHOT.jar
```

`package` is skipped when `test` fails. The run is red because `test` failed. The jar name comes from `artifactId` and `version` in `pom.xml`.

Other job settings:

- `runs-on` — `ubuntu-latest` in the current file. `windows-latest` and `macos-latest` are the other GitHub-hosted images. A self-hosted runner is a computer you register. This project uses GitHub-hosted computers.
- `if` — skip a whole job. [Section 9](#9-run-the-tests-on-one-branch) uses this to run Maven only on `main`.
- `timeout-minutes` — stop the job if it runs too long. The job is then cancelled, which is not a pass.
- `concurrency` — group runs so a newer push cancels the older run on the same branch:

```yaml
concurrency:
  group: maven-junit-${{ github.ref }}
  cancel-in-progress: true
```

- `environment` — tie a job to a GitHub environment. That can ask a person to approve the job before it starts. That is a deploy control. This project's test job does not need one.
- `services` — start a helper container next to the job, such as a database. `CalculatorTest` has no database, so the current job does not set this.
- `outputs` — a job publishes a value that a later job reads as `needs.<job-id>.outputs.<name>`. The value comes from a step that wrote to `GITHUB_OUTPUT`.

### One example that uses the extra pieces

A push to `main`, a pull request, a manual click, or Monday 06:00 UTC can start this example. The test job runs on two JDKs. The package job waits for both and runs only on `main`:

```yaml
name: Maven JUnit tests

on:
  push:
    branches: [main]
  pull_request:
    types: [opened, synchronize, reopened]
  workflow_dispatch:
  schedule:
    - cron: "0 6 * * 1"

concurrency:
  group: maven-junit-${{ github.ref }}
  cancel-in-progress: true

jobs:
  test:
    name: Tests on JDK ${{ matrix.java }}
    runs-on: ubuntu-latest
    strategy:
      matrix:
        java: ["17", "21"]
    steps:
      - uses: actions/checkout@v7
      - uses: actions/setup-java@v6
        with:
          distribution: temurin
          java-version: ${{ matrix.java }}
          cache: maven
      - run: mvn -B clean test

  package:
    name: Package the jar
    needs: test
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v7
      - uses: actions/setup-java@v6
        with:
          distribution: temurin
          java-version: "17"
          cache: maven
      - run: mvn -B -DskipTests package
```

Read it with the words from [section 2](#2-what-github-actions-is):

- **Events** decide when the file starts.
- **Jobs** are `test` and `package`. The matrix turns `test` into two computers. `package` waits with `needs`.
- **Steps** are checkout, JDK setup, and one Maven command.
- **Actions** are `actions/checkout@v7` and `actions/setup-java@v6`.

The file GitHub runs today is the shorter one in [section 4](#4-the-workflow-file-line-by-line): `push` and `pull_request`, one Ubuntu job, Temurin 17, `mvn -B clean test`, and an upload of `target/surefire-reports`. Add the pieces from this section when you want a manual run, a schedule, a second JDK, or a jar that is built only after the tests pass.
