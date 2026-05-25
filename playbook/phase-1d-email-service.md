# Phase 1D — Email Service

## Prerequisites

**Phase 0 (Scaffolding) must be complete and merged to `master`.**

The `spring-boot-migration/` directory must exist with the Maven project skeleton. The `spring-boot-starter-mail` dependency should already be in `pom.xml`.

## Repository

`codev-workshops/talend-sample-3` (branch: `master`)

## Context

The original Talend project has a joblet (`jblt_send_status_mail`) that sends SMTP email notifications after job completion. In the original Talend jobs, this component is **deactivated by default** (`ACTIVATE=false` at `j_usecase_customer_setup_0.1.item` line 359). The configuration is at `TALEND_AWS_DI_USECASE/joblets/jblt_send_status_mail_0.1.item`:

- **Lines 76–103**: tSendMail component — SMTP config with localhost:25, plain text, ISO-8859-15 encoding
- **Lines 106–109**: tWarn component — on send failure, log warn with message `"There was an error sending status mail."` and code 43

## Task

Implement the following classes in `spring-boot-migration/`.

### 1. `MailConfig.java` — `com.migration.etl.config`

```java
@ConfigurationProperties(prefix = "app.mail")
```

Properties:
- `smtpHost` — String, default: `"localhost"`
- `port` — int, default: `25`
- `to` — String (recipient email address)
- `from` — String (sender email address)
- `subject` — String, default: `"Talend Open Studio notification"`

These defaults come from the tSendMail config at `jblt_send_status_mail_0.1.item` lines 78–90.

Make sure to enable configuration properties scanning (`@EnableConfigurationProperties(MailConfig.class)`) in the config class or main application.

### 2. `StatusMailService.java` — `com.migration.etl.service`

- Annotate with `@Service`
- Inject `JavaMailSender` and `MailConfig`
- Feature-flagged via `app.email.enabled` property (use `@Value("${app.email.enabled:false}")`)
- Default: **disabled** (`false`)

Implement method:

```java
public void sendStatusMail(String body)
```

Behavior:
- If `app.email.enabled` is `false`: do nothing (no-op), return immediately
- If enabled: construct a `SimpleMailMessage` with to, from, subject from `MailConfig`, and the body parameter
- Send via `JavaMailSender.send()`
- **On failure**: catch the exception, log a WARN with message `"There was an error sending status mail."` and code 43 — do NOT throw

This matches the tWarn behavior at `jblt_send_status_mail_0.1.item` lines 107–109.

### 3. Unit Test — `StatusMailServiceTest.java`

In `src/test/java/com/migration/etl/service/`:

- **Test 1 — disabled (default)**: Set `app.email.enabled=false`. Call `sendStatusMail("test")`. Verify `JavaMailSender.send()` is **never called**.
- **Test 2 — enabled and successful**: Set `app.email.enabled=true`. Call `sendStatusMail("test body")`. Verify `JavaMailSender.send()` is called once with correct message properties.
- **Test 3 — enabled but failure**: Set `app.email.enabled=true`. Configure mock `JavaMailSender.send()` to throw `MailSendException`. Call `sendStatusMail("test")`. Verify **no exception propagates** and a WARN log is emitted.

## Reference Files

- `TALEND_AWS_DI_USECASE/joblets/jblt_send_status_mail_0.1.item`:
  - Lines 76–103: tSendMail SMTP configuration
  - Lines 106–109: tWarn error handling (message: "There was an error sending status mail.", code: 43)
- `TALEND_AWS_DI_USECASE/process/j_usecase_customer_setup_0.1.item`:
  - Line 359: `ACTIVATE=false` (email disabled by default)

## Acceptance Criteria

- All unit tests pass
- Email is **off by default** (`app.email.enabled=false`)
- When enabled, sends via `JavaMailSender` with correct config
- On send failure: logs WARN with message containing `"error sending status mail"` and code 43, does not throw
- `mvn test` passes

## Commit & PR

- Branch from `master`
- Commit message: `feat: implement StatusMailService with feature flag`
- Create a PR targeting `master`
