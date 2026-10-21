# Github_Actions

A small Java 17 Maven project that shows how GitHub Actions compiles code and runs JUnit 5 tests. The sample class is `navaneeth.Calculator`. The workflow file is `.github/workflows/maven-junit.yml`. On the Actions tab the workflow is named **Maven JUnit tests**.

The longer walkthrough of the same workflow is in [EXPLANATION.md](EXPLANATION.md).

## What is in the repository

| Path | Role |
| --- | --- |
| `src/main/java/navaneeth/Calculator.java` | `add` and `subtract` for two integers |
| `src/test/java/navaneeth/CalculatorTest.java` | Two JUnit 5 tests: `addReturnsSum` and `subtractReturnsDifference` |
| `pom.xml` | Maven coordinates `navaneeth:Github_Actions:1.0-SNAPSHOT`, Java 17, JUnit Jupiter 5.14.4, Surefire 3.6.0 |
| `.github/workflows/maven-junit.yml` | The workflow that checks out the repo, installs Temurin 17, runs the tests, and uploads the reports |

## Run the tests on your machine

From the project directory:

```shell
mvn -B clean test
```

`-B` is batch mode, so Maven does not stop for a prompt. `clean` deletes `target/`. `test` compiles `src/main/java` and `src/test/java`, then Surefire runs `CalculatorTest`. A passing run prints:

```text
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
```

Reports are written to `target/surefire-reports/`. That folder is gitignored because `target/` is build output.

## What GitHub Actions does

GitHub Actions runs the workflow on a computer GitHub provides. The workflow starts when GitHub receives an event for this repository. Saving the file on your laptop does nothing until that file is in a commit GitHub has.

This workflow listens for:

- **`push`** on any branch, and on tags. A local `git commit` does not start it. `git push` does.
- **`pull_request`** when a pull request is opened, updated (`synchronize`), or reopened. Checkout uses GitHub's test merge, so the tests run on the code as it would look after the merge.

The job id is `maven-junit-tests`. It runs on `ubuntu-latest`. `permissions: contents: read` lets checkout read the repository. The job has four steps:

1. **Check out repository** — `actions/checkout@v7` copies the repository onto the runner.
2. **Set up Temurin JDK 17** — `actions/setup-java@v6` installs Eclipse Temurin 17, matching `maven.compiler.release` in `pom.xml`, and caches Maven downloads.
3. **Run unit tests** — `mvn -B clean test`. The step passes when Maven exits 0. A failed assertion, an unexpected exception, or a compile error makes Maven exit non-zero, and the step fails.
4. **Upload Surefire reports** — `actions/upload-artifact@v7` saves `target/surefire-reports/` on the run as `surefire-reports` for 14 days. `if: always()` uploads the reports after a failed test as well. The runner's disk is deleted when the job ends.

## When the check is green or red

The job name **Maven JUnit tests** is also the check name on a commit and on a pull request.

The job is green when checkout, JDK setup, and `mvn -B clean test` all succeed. For this project a passing Maven log includes `BUILD SUCCESS` and `Tests run: 2, Failures: 0, Errors: 0, Skipped: 0`.

The job is red when any step that ran fails. The usual cause is **Run unit tests**: an assertion failure (`Failures` above 0), an unexpected exception (`Errors` above 0), or a compile or download problem. A later step that succeeds, including the report upload, does not turn a failed job green.

On a pull request, that red check blocks the merge when the repository requires the **Maven JUnit tests** check. That requirement is a branch setting. The workflow file does not create it.

## Read the result on GitHub

Open the **Actions** tab, select the **Maven JUnit tests** run, and open the job with the same name.

- Expand **Run unit tests** for the Maven log and the Surefire summary.
- Expand **Upload Surefire reports** to see whether the files were saved.
- On the run summary, **Artifacts** lists `surefire-reports` for download.

**Re-run failed jobs** or **Re-run all jobs** starts a new attempt on a new computer.
