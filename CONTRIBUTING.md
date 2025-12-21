# Contributing to VaultSandbox Client - Java

First off, thank you for considering contributing! This project and its community appreciate your time and effort.

Please take a moment to review this document in order to make the contribution process easy and effective for everyone involved.

## Code of Conduct

This project and everyone participating in it is governed by the [Code of Conduct](./CODE_OF_CONDUCT.md). By participating, you are expected to uphold this code. Please report unacceptable behavior to hello@vaultsandbox.com.

## How You Can Contribute

There are many ways to contribute, from writing tutorials or blog posts, improving the documentation, submitting bug reports and feature requests or writing code which can be incorporated into the main project.

### Reporting Bugs

If you find a bug, please ensure the bug was not already reported by searching on GitHub under [Issues](https://github.com/vaultsandbox/client-java/issues). If you're unable to find an open issue addressing the problem, [open a new one](https://github.com/vaultsandbox/client-java/issues/new). Be sure to include a **title and clear description**, as much relevant information as possible, and a **code sample** or an **executable test case** demonstrating the expected behavior that is not occurring.

### Suggesting Enhancements

If you have an idea for an enhancement, please open an issue with a clear title and description. Describe the enhancement, its potential benefits, and any implementation ideas you might have.

### Pull Requests

We love pull requests. Here's a quick guide:

1.  Fork the repository.
2.  Create a new branch for your feature or bug fix: `git checkout -b feat/my-awesome-feature` or `git checkout -b fix/that-annoying-bug`.
3.  Make your changes, adhering to the coding style.
4.  Add or update tests for your changes.
5.  Ensure all tests pass (`./gradlew test`).
6.  Ensure your code is formatted (`./gradlew spotlessApply`).
7.  Commit your changes with a descriptive commit message.
8.  Push your branch to your fork.
9.  Open a pull request to the `main` branch of the upstream repository.

## Development Setup

This project requires **Java 21** or later.

1.  Clone the repository
2.  Build the project: `./gradlew build`
3.  Configuration: If you are running integration tests that require environment variables, set `VAULTSANDBOX_API_KEY` in your environment.

## Running Tests

- **Run unit tests**:
  ```bash
  ./gradlew test
  ```
- **Run integration tests** (requires gateway):
  ```bash
  ./gradlew integrationTest
  ```
- **Run all tests with coverage**:
  ```bash
  ./gradlew jacocoFullReport
  ```

## Coding Style

- **Formatting**: We use [Spotless](https://github.com/diffplug/spotless) with Google Java Format. Run `./gradlew spotlessApply` before committing.
- **Static Analysis**: We use Checkstyle and SpotBugs. Run `./gradlew check` to verify.
- **Comments**: For new features or complex logic, please add Javadoc comments to explain the _why_ behind your code.

## Project Structure

```
src/
├── main/java/com/vaultsandbox/client/
│   ├── VaultSandboxClient.java    # Main client entry point
│   ├── Inbox.java                 # Inbox operations
│   ├── Email.java                 # Email model
│   ├── ClientConfig.java          # Configuration builder
│   ├── crypto/                    # Cryptographic operations
│   ├── http/                      # HTTP client layer
│   ├── strategy/                  # Delivery strategies (SSE, polling)
│   └── exception/                 # Custom exceptions
└── test/java/                     # Unit tests
└── integrationTest/java/          # Integration tests
```

Thank you for your contribution!
