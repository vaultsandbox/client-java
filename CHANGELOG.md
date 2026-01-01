# Changelog

All notable changes to this project will be documented in this file.

The format is inspired by [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [0.5.2] - 2025-12-31

### Changed

- Standardized email authentication result structs to match wire format and other SDKs

### Added

- End-to-end integration tests for email authentication results using the test email API

## [0.5.1] - 2025-12-29

### Added

- Full email authentication result fields: `ip`, `info` for SPF; `selector`, `info` for DKIM; `policy`, `aligned`, `info` for DMARC; `hostname`, `info` for Reverse DNS
- `getStatus()` accessor methods on all auth result classes
- `isAligned()` convenience method on `DmarcResult`
- `isValid()` convenience method on `ReverseDnsResult`

## [0.5.0] - 2025-12-18

### Initial release

- Quantum-safe email testing SDK with ML-KEM-768 encryption
- Automatic keypair generation and management
- Support for both polling and real-time (SSE) email delivery
- Full email content access including attachments and headers
- Built-in SPF/DKIM/DMARC authentication validation
- Thread-safe design for concurrent test execution
- Inbox import/export functionality for test reproducibility
- Comprehensive error handling with automatic retries
- Java 21+ with virtual threads support
