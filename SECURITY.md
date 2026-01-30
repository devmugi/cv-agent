# Security Policy

## Supported Versions

| Version | Supported          |
| ------- | ------------------ |
| 1.x     | :white_check_mark: |

## Reporting a Vulnerability

If you discover a security vulnerability in CV Agent, please report it responsibly.

### How to Report

**Email:** devmugi@gmail.com

Please include:
- Description of the vulnerability
- Steps to reproduce
- Potential impact
- Any suggested fixes (optional)

### What to Expect

- **Acknowledgment:** Within 48 hours
- **Initial Assessment:** Within 7 days
- **Resolution Timeline:** Depends on severity, typically 30-90 days

### Scope

This policy covers:
- The CV Agent Android application
- The shared Kotlin Multiplatform modules
- Build and configuration files in this repository

### Out of Scope

- Third-party dependencies (report to their maintainers)
- Groq API security (report to Groq)
- Firebase services (report to Google)

## Security Best Practices for Users

1. **API Keys:** Never commit your `local.properties` file
2. **Fork Security:** If forking, ensure you don't expose your own API keys
3. **Dependencies:** Keep dependencies updated via `./gradlew dependencyUpdates`

## Acknowledgments

We appreciate responsible disclosure and will acknowledge security researchers who report valid vulnerabilities (with permission).
