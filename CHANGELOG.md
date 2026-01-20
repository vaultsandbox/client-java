# Changelog

All notable changes to this project will be documented in this file.

The format is inspired by [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [0.8.5] - 2026-01-19

### Added

- Spam analysis support (Rspamd integration)
- Inbox creation option to enable/disable spam analysis
- Server capability detection for spam analysis

## [0.8.0] - 2026-01-16

### Added

- Webhooks support for inbox

## [0.7.0] - 2026-01-13

### Added

- Optional encryption support with `encryptionPolicy` option
- Optional email authentication feature

### Changed

- Updated ReverseDNS structure
- License changed from MIT to Apache 2.0

## [0.6.1] - 2026-01-11

### Changed

- **Breaking:** Removed `AUTO` strategy; default is now `SSE`
- SSE reconnect interval reduced from 5s to 2s for faster recovery

### Improved

- SSE sync uses hash-based change detection to minimize redundant fetches

## [0.6.0] - 2026-01-04

### Changed

- `listEmails()` now returns fully hydrated emails with complete content, matching other SDKs
- **Breaking:** Export format updated per spec §9.4 - public key is no longer included (derived from secret key)
- **Breaking:** `ExportedInbox` field renamed from `secretKeyB64` to `secretKey` per spec §9.3
- **Breaking:** `getPublicKeyB64()`/`setPublicKeyB64()` removed; use `derivePublicKey()` instead
- **Breaking:** Signature verification now requires pinned server key parameter for key pinning per spec §8.1
- Base64URL decoder now strictly rejects `+`, `/`, and `=` characters per spec §2.2

### Added

- `listEmailsMetadataOnly()` method for efficient metadata-only retrieval without fetching email content
- `EmailMetadata` class for lightweight email representation
- `version` field in `ExportedInbox` (currently version 1) per spec §9.3
- `derivePublicKey()` method on `ExportedInbox` to derive public key from secret key per spec §10.2
- `PayloadValidator` class for full payload validation per spec §8.1
- Email address validation (must contain exactly one `@`) per spec §10.1
- Server signature public key size validation (1952 bytes for ML-DSA-65)

### Fixed

- Stricter import validation order per spec §10.1

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
