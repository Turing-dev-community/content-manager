# Debugging/Triage Exercise Proposal (B41–B80)

## Repo map (core files only)
- Entrypoint / cross-cutting config
  - ContentManagerApplication.java: enables caching/retry globally.
  - CacheConfig.java: Caffeine cache manager, TTL/size settings.
  - SecurityConfig.java: auth rules, CSRF, JDBC user/authority queries.
  - WebMvcConfig.java: rate limiter interceptor coverage.
- Error/exception surface
  - GlobalExceptionHandler.java: maps exceptions to HTTP responses.
  - CustomErrorResponse.java: error payload shape.
  - EntityNotFoundException.java, InvalidPayloadException.java: domain error types.
- Validation domain (core pipeline/data flow)
  - ContentTypeRegistry.java: content-type to class mapping.
  - Models: ForbiddenWords, ValidationResult, ValidationPipelineModel, ValidationStepModel, ValidationError, ValidationStepType.
  - Repositories: ForbiddenWordsRepository, ValidationPipelineRepository, ValidationResultRepository.
  - Services: ForbiddenWordsService, ValidationPipelineService, ValidationReportExportService,
    ValidationReportExporterUtility, ValidationServiceImpl (central orchestration).
  - Validators: ForbiddenWordValidator, LengthValidator, NumericRangeValidator,
    PhoneNumberForbiddenValidator, RegexValidator, ValidationStepFactory.
  - Web: ValidationController, ValidationPipelineController, ForbiddenWordsController.

## Bug candidates (B41–B80)
Each item lists: Location, Core relevance, Bug type, Proposed change, Trigger, Symptom, Why hard,
Static-analysis discoverability, Suggested detection, and Rankings.

### B41
- Location: src/main/java/com/dehold/contentmanager/ContentManagerApplication.java, class annotations (approx L8–L10)
- Core relevance: App-wide retry is critical for transient DB failures.
- Bug type: Reliability regression (retry disabled).
- Proposed change: Remove `@EnableRetry`.
- Trigger conditions: Transient DB errors in retryable services.
- Expected symptom: Increased 5xx responses instead of successful retries.
- Why it’s hard: Looks like infra flakiness; no direct stack traces indicate missing retry.
- Static-analysis discoverability: Medium.
- Suggested detection: Integration test with transient DB error simulation.
- Ranking: Exercise value 4/5, Stealth 3/5, Scorability 4/5

### B42
- Location: CacheConfig.java, caffeineCacheBuilder (approx L25–L30)
- Core relevance: Caching affects most read endpoints.
- Bug type: Performance regression (TTL too short).
- Proposed change: Change `expireAfterWrite(10, TimeUnit.MINUTES)` to `TimeUnit.SECONDS`.
- Trigger conditions: Normal traffic with repeated reads.
- Expected symptom: Cache thrash, higher DB load, slower responses.
- Why it’s hard: Looks like load-related slowness; no errors.
- Static-analysis discoverability: Medium.
- Suggested detection: Load test with cache hit-rate assertions.
- Ranking: Exercise value 4/5, Stealth 4/5, Scorability 4/5

### B43
- Location: CacheConfig.java, cacheManager() (approx L18–L22)
- Core relevance: Cache correctness on entity creation.
- Bug type: Negative caching / stale cache.
- Proposed change: `cacheManager.setAllowNullValues(true)`.
- Trigger conditions: Request missing entity before it exists, then create it.
- Expected symptom: Reads keep returning null/404 until TTL expires.
- Why it’s hard: Depends on call order; intermittent.
- Static-analysis discoverability: Low.
- Suggested detection: Integration test: miss then create then read.
- Ranking: Exercise value 5/5, Stealth 5/5, Scorability 4/5

### B44
- Location: CacheConfig.java, caffeineCacheBuilder (approx L25–L30)
- Core relevance: Cache stability for hot paths.
- Bug type: Unpredictable eviction.
- Proposed change: Add `weakKeys()` (or `weakValues()`).
- Trigger conditions: GC pressure or low-memory conditions.
- Expected symptom: Random cache misses and latency spikes.
- Why it’s hard: Nondeterministic, environment dependent.
- Static-analysis discoverability: Low.
- Suggested detection: Load test + GC pressure; monitor cache stats.
- Ranking: Exercise value 4/5, Stealth 5/5, Scorability 3/5

### B45
- Location: SecurityConfig.java, filterChain CSRF config (approx L55–L58)
- Core relevance: Security posture across all APIs.
- Bug type: Security misconfiguration.
- Proposed change: Change `.ignoringRequestMatchers("/api/users/**")` to `"/api/**"`.
- Trigger conditions: CSRF on any non-user API endpoint.
- Expected symptom: CSRF protection disabled broadly.
- Why it’s hard: Security risk not visible in functional tests.
- Static-analysis discoverability: Medium.
- Suggested detection: Security tests / static config review.
- Ranking: Exercise value 5/5, Stealth 4/5, Scorability 4/5

### B46
- Location: SecurityConfig.java, authorizeHttpRequests order (approx L59–L64)
- Core relevance: Admin endpoints are critical.
- Bug type: Authorization rule order bug.
- Proposed change: Move `.anyRequest().permitAll()` before `/api/admin/**` matcher.
- Trigger conditions: Requests to admin endpoints.
- Expected symptom: Admin endpoints accessible without ADMIN role.
- Why it’s hard: Only shows with security testing; normal tests may pass.
- Static-analysis discoverability: Medium.
- Suggested detection: Security integration tests for admin access.
- Ranking: Exercise value 5/5, Stealth 3/5, Scorability 5/5

### B47
- Location: SecurityConfig.java, jdbcUserDetailsManager usersByUsernameQuery (approx L35–L40)
- Core relevance: Authentication for all protected routes.
- Bug type: Authentication regression / contract drift.
- Proposed change: Select `email` as username column (or use WHERE email = ?).
- Trigger conditions: Users log in with username (not email).
- Expected symptom: Valid credentials rejected; inconsistent auth behavior.
- Why it’s hard: Looks like bad credentials; only affects some users.
- Static-analysis discoverability: Low.
- Suggested detection: Auth test using username vs email.
- Ranking: Exercise value 4/5, Stealth 4/5, Scorability 4/5

### B48
- Location: WebMvcConfig.java, addInterceptors (approx L21–L25)
- Core relevance: Rate limiting protects all API traffic.
- Bug type: Coverage gap.
- Proposed change: Change path pattern to `"/api/*"`.
- Trigger conditions: Nested endpoints like `/api/users/{id}/...`.
- Expected symptom: Rate limits bypassed on most endpoints.
- Why it’s hard: Only visible under abuse/load.
- Static-analysis discoverability: Medium.
- Suggested detection: Integration test hitting nested endpoints.
- Ranking: Exercise value 4/5, Stealth 4/5, Scorability 4/5

### B49
- Location: GlobalExceptionHandler.java, handle InvalidPayloadException (approx L29–L40)
- Core relevance: Error responses in core API flows.
- Bug type: Incorrect HTTP status.
- Proposed change: Return `HttpStatus.NOT_FOUND` instead of `BAD_REQUEST`.
- Trigger conditions: Invalid payload scenarios.
- Expected symptom: Clients treat validation errors as missing resources.
- Why it’s hard: Still returns JSON error; easy to miss in manual testing.
- Static-analysis discoverability: Medium.
- Suggested detection: Controller tests expecting 400 for payload errors.
- Ranking: Exercise value 3/5, Stealth 3/5, Scorability 5/5

### B50
- Location: GlobalExceptionHandler.java, all handlers (approx L19–L24)
- Core relevance: Error payload privacy and stability.
- Bug type: Information leak.
- Proposed change: Use `request.getDescription(true)` instead of `false`.
- Trigger conditions: Any error response.
- Expected symptom: Session ID or internal details leaked in `path`.
- Why it’s hard: Only visible if inspecting payloads; no functional failures.
- Static-analysis discoverability: Low.
- Suggested detection: Security tests or API contract checks.
- Ranking: Exercise value 4/5, Stealth 5/5, Scorability 4/5

### B51
- Location: EntityNotFoundException.java, of(...) (approx L8–L9)
- Core relevance: Error messages used in API contracts/tests.
- Bug type: Incorrect error message format.
- Proposed change: Swap `entityType` and `id` in the formatted message.
- Trigger conditions: Any 404 thrown.
- Expected symptom: Confusing error text; tests may fail.
- Why it’s hard: Seems minor; only surfaced if message asserted.
- Static-analysis discoverability: Medium.
- Suggested detection: Error-message contract tests.
- Ranking: Exercise value 2/5, Stealth 3/5, Scorability 5/5

### B52
- Location: CustomErrorResponse.java, constructor (approx L12–L16)
- Core relevance: Error payload correctness for all APIs.
- Bug type: Field misassignment.
- Proposed change: Accidentally assign `path` to `error` and `error` to `path`.
- Trigger conditions: Any error response.
- Expected symptom: Clients see URL in error field and message in path.
- Why it’s hard: Still structured JSON; requires careful inspection.
- Static-analysis discoverability: Low.
- Suggested detection: API contract tests for error payload fields.
- Ranking: Exercise value 3/5, Stealth 4/5, Scorability 4/5

### B53
- Location: ContentTypeRegistry.java, CONTENT_TYPES map (approx L12–L16)
- Core relevance: Validation pipeline uses class mapping.
- Bug type: Wrong mapping.
- Proposed change: Map `"supportrequest"` to `SupportResponse.class`.
- Trigger conditions: Validating support requests.
- Expected symptom: Field extraction uses wrong class; validators skip.
- Why it’s hard: Validation appears to succeed but misses errors.
- Static-analysis discoverability: Low.
- Suggested detection: Validation tests for support requests.
- Ranking: Exercise value 4/5, Stealth 5/5, Scorability 4/5

### B54
- Location: ContentTypeRegistry.java, getContentClass (approx L18–L20)
- Core relevance: Content type lookups are frequent.
- Bug type: Case-sensitivity regression.
- Proposed change: Remove `.toLowerCase()` normalization.
- Trigger conditions: Content types passed in mixed case.
- Expected symptom: Unexpected fallback to GenericContentModel.
- Why it’s hard: Only breaks when case differs from stored strings.
- Static-analysis discoverability: Low.
- Suggested detection: Unit tests with mixed-case contentType.
- Ranking: Exercise value 3/5, Stealth 4/5, Scorability 4/5

### B55
- Location: ForbiddenWords.java, constructor (approx L17–L26)
- Core relevance: Forbidden words are used across validation.
- Bug type: Data integrity / null timestamps.
- Proposed change: Remove initialization of createdAt/updatedAt.
- Trigger conditions: Creating forbidden words.
- Expected symptom: Persistence errors or null timestamps in DB.
- Why it’s hard: Might show only in downstream reporting.
- Static-analysis discoverability: Low.
- Suggested detection: Repository tests asserting timestamps.
- Ranking: Exercise value 3/5, Stealth 4/5, Scorability 4/5

### B56
- Location: ValidationError.java (record), approx L3
- Core relevance: Error code/message drive reporting.
- Bug type: Field swap.
- Proposed change: Swap record field order to `(message, code)`.
- Trigger conditions: Any validation error.
- Expected symptom: Error codes show human text and vice versa.
- Why it’s hard: Still non-null; requires semantic inspection.
- Static-analysis discoverability: Medium.
- Suggested detection: Unit tests asserting error codes.
- Ranking: Exercise value 3/5, Stealth 3/5, Scorability 5/5

### B57
- Location: ValidationResult.java, valid/invalid factory methods (approx L41–L47)
- Core relevance: Validation results persisted and reported.
- Bug type: Data integrity.
- Proposed change: Pass `userId` where `contentId` should be (swap args).
- Trigger conditions: Any validation run.
- Expected symptom: Results linked to wrong content IDs.
- Why it’s hard: IDs are valid UUIDs; errors look plausible.
- Static-analysis discoverability: Low.
- Suggested detection: Integration tests matching contentId to content.
- Ranking: Exercise value 5/5, Stealth 5/5, Scorability 4/5

### B58
- Location: ValidationResult.java, fromPersistence (approx L50–L53)
- Core relevance: Reporting uses persisted validity.
- Bug type: Logic error.
- Proposed change: Ignore `isValid` argument and set true always.
- Trigger conditions: Reading stored results.
- Expected symptom: Reports show all validations as valid.
- Why it’s hard: Only visible in reporting, not validation response.
- Static-analysis discoverability: Low.
- Suggested detection: Repository round-trip tests.
- Ranking: Exercise value 4/5, Stealth 4/5, Scorability 4/5

### B59
- Location: ValidationPipelineModel.java, setSteps (approx L57–L62)
- Core relevance: Pipeline definition integrity.
- Bug type: Null handling bug.
- Proposed change: If steps list is empty, set it to null.
- Trigger conditions: Pipelines with empty steps.
- Expected symptom: NPE in pipeline creation or no validators run.
- Why it’s hard: Only appears for empty pipelines.
- Static-analysis discoverability: Low.
- Suggested detection: Unit test with empty pipeline.
- Ranking: Exercise value 3/5, Stealth 4/5, Scorability 4/5

### B60
- Location: ValidationStepModel.java, isEnabled() (approx L24–L25)
- Core relevance: Step enable/disable is core behavior.
- Bug type: Inversion.
- Proposed change: `return !isEnabled;`
- Trigger conditions: Disabled step configured.
- Expected symptom: Disabled steps run (or enabled steps skip).
- Why it’s hard: Only seen when toggling steps.
- Static-analysis discoverability: Medium.
- Suggested detection: Tests asserting disabled steps are skipped.
- Ranking: Exercise value 4/5, Stealth 3/5, Scorability 5/5

### B61
- Location: ValidationStepType.java (approx L3–L7)
- Core relevance: Enum values persist in DB and APIs.
- Bug type: Contract drift.
- Proposed change: Rename `REGEX_VALIDATION` to `REGEX_VALIDATOR`.
- Trigger conditions: Loading existing pipelines from DB.
- Expected symptom: `IllegalArgumentException` on enum value.
- Why it’s hard: Only hits data created before change; seems like data issue.
- Static-analysis discoverability: Medium.
- Suggested detection: Migration/compatibility tests.
- Ranking: Exercise value 3/5, Stealth 3/5, Scorability 4/5

### B62
- Location: ForbiddenWordsRepository.java, insert/update (approx L36–L56)
- Core relevance: Forbidden words matching depends on storage format.
- Bug type: Serialization mismatch.
- Proposed change: Use `String.join(";", words)` instead of comma.
- Trigger conditions: Reading stored words.
- Expected symptom: Words parsed as a single entry; validation misses.
- Why it’s hard: Appears as missing forbidden words only.
- Static-analysis discoverability: Low.
- Suggested detection: Repository round-trip test for words list.
- Ranking: Exercise value 4/5, Stealth 4/5, Scorability 4/5

### B63
- Location: ForbiddenWordsRepository.java, findByUserIdAndContentType (approx L79–L82)
- Core relevance: Per-user policy isolation.
- Bug type: Data leakage.
- Proposed change: Remove `user_id = ?` predicate.
- Trigger conditions: Multiple users with different forbidden words.
- Expected symptom: Users inherit others’ forbidden words.
- Why it’s hard: Requires multi-user dataset; looks like configuration issue.
- Static-analysis discoverability: Medium.
- Suggested detection: Integration test with two users.
- Ranking: Exercise value 5/5, Stealth 4/5, Scorability 4/5

### B64
- Location: ValidationPipelineRepository.java, insertValidationStep (approx L83–L93)
- Core relevance: Steps must link to pipeline.
- Bug type: Foreign key mismatch.
- Proposed change: Use `step.getPipelineId()` instead of `pipelineId` param.
- Trigger conditions: Creating pipeline with steps.
- Expected symptom: Steps not attached; pipeline runs empty.
- Why it’s hard: Pipeline exists but silently does nothing.
- Static-analysis discoverability: Low.
- Suggested detection: Pipeline creation test verifying steps returned.
- Ranking: Exercise value 5/5, Stealth 5/5, Scorability 4/5

### B65
- Location: ValidationPipelineRepository.java, update(...) (approx L66–L80)
- Core relevance: Pipeline updates are common.
- Bug type: Duplicate data accumulation.
- Proposed change: Remove `DELETE FROM validation_step WHERE pipeline_id = ?`.
- Trigger conditions: Update pipeline multiple times.
- Expected symptom: Steps duplicated; validations run multiple times.
- Why it’s hard: Only appears after multiple updates.
- Static-analysis discoverability: Low.
- Suggested detection: Integration test updating pipeline twice.
- Ranking: Exercise value 4/5, Stealth 5/5, Scorability 4/5

### B66
- Location: ValidationPipelineRepository.java, loadStepsForPipeline (approx L162–L166)
- Core relevance: Step order affects validation semantics.
- Bug type: Ordering regression.
- Proposed change: Order by id DESC (reverse).
- Trigger conditions: Pipelines where order matters.
- Expected symptom: Different validation order; inconsistent errors.
- Why it’s hard: Semantics change subtly; tests often ignore order.
- Static-analysis discoverability: Low.
- Suggested detection: Unit test for step order with dependent validators.
- Ranking: Exercise value 3/5, Stealth 4/5, Scorability 3/5

### B67
- Location: ValidationResultRepository.java, create(...) (approx L27–L39)
- Core relevance: run_id groups results for dedup/reporting.
- Bug type: Data integrity / grouping.
- Proposed change: Always generate a new run_id instead of using result.runId.
- Trigger conditions: Bulk validation with intended grouping.
- Expected symptom: Reports show duplicates; upserts don’t dedupe.
- Why it’s hard: Functional responses still correct; DB grows quietly.
- Static-analysis discoverability: Low.
- Suggested detection: Integration test for run_id grouping.
- Ranking: Exercise value 5/5, Stealth 5/5, Scorability 4/5

### B68
- Location: ValidationResultRepository.java, deserializeErrors (approx L71–L79)
- Core relevance: Reporting depends on persisted errors.
- Bug type: Error swallowing.
- Proposed change: Catch exceptions and return empty list.
- Trigger conditions: Errors JSON malformed or schema changes.
- Expected symptom: Invalid results appear valid in reports.
- Why it’s hard: Only occurs when data format drifts.
- Static-analysis discoverability: Low.
- Suggested detection: Negative tests for invalid errors JSON.
- Ranking: Exercise value 4/5, Stealth 4/5, Scorability 3/5

### B69
- Location: ForbiddenWordsService.java, addDefaultForbiddenWords (approx L50–L60)
- Core relevance: Default forbidden words provide baseline safety.
- Bug type: Missing defaults.
- Proposed change: Return custom list immediately when empty (skip defaults).
- Trigger conditions: User has no custom forbidden words.
- Expected symptom: Validation allows previously forbidden content.
- Why it’s hard: Requires knowledge of defaults; looks like config issue.
- Static-analysis discoverability: Low.
- Suggested detection: Unit test that defaults are always included.
- Ranking: Exercise value 4/5, Stealth 4/5, Scorability 4/5

### B70
- Location: ValidationPipelineService.java, update(...) (approx L38–L41)
- Core relevance: Updates should not create new pipelines.
- Bug type: API contract drift.
- Proposed change: If id is null, call repository.save without validation (creates new).
- Trigger conditions: Update request missing id or mismatch.
- Expected symptom: Duplicate pipelines instead of update.
- Why it’s hard: Response looks valid; DB silently grows.
- Static-analysis discoverability: Medium.
- Suggested detection: Integration test with update on missing id.
- Ranking: Exercise value 4/5, Stealth 4/5, Scorability 4/5

### B71
- Location: ValidationReportExportService.java, exportReport (approx L26–L31)
- Core relevance: Report export is user-facing.
- Bug type: Input handling regression.
- Proposed change: Remove `format.toLowerCase()` (case-sensitive).
- Trigger conditions: Clients send `format=JSON`.
- Expected symptom: “Unsupported format” for valid inputs.
- Why it’s hard: Only case variation; easy to miss.
- Static-analysis discoverability: Medium.
- Suggested detection: Integration test with uppercase format.
- Ranking: Exercise value 3/5, Stealth 3/5, Scorability 4/5

### B72
- Location: ValidationReportExporterUtility.java, exportToJson (approx L22–L28)
- Core relevance: JSON export used in reporting APIs.
- Bug type: Null handling regression.
- Proposed change: Return bytes for literal `"null"` instead of empty array.
- Trigger conditions: Null report (e.g., userId missing).
- Expected symptom: Clients parse `"null"` as valid JSON but semantically wrong.
- Why it’s hard: Looks like success; no errors thrown.
- Static-analysis discoverability: Low.
- Suggested detection: Unit test for null report handling.
- Ranking: Exercise value 3/5, Stealth 4/5, Scorability 3/5

### B73
- Location: ValidationServiceImpl.java, runBlogPostValidation (approx L171–L174)
- Core relevance: Bulk validation for blog posts is a core path.
- Bug type: Content type mismatch.
- Proposed change: Use `"BlogPost"` instead of `"blogpost"` when building pipelines.
- Trigger conditions: Any blog post validation pipeline configured.
- Expected symptom: No pipelines found; validations silently skipped.
- Why it’s hard: Results list may be empty but no error thrown.
- Static-analysis discoverability: Low.
- Suggested detection: Integration test with configured pipeline.
- Ranking: Exercise value 5/5, Stealth 5/5, Scorability 4/5

### B74
- Location: ValidationServiceImpl.java, generateValidationReport (approx L127–L140)
- Core relevance: Reports drive user insight.
- Bug type: Counting error.
- Proposed change: Increment `totalErrorCount` even when errors list is empty.
- Trigger conditions: Valid results included in report.
- Expected symptom: Inflated error counts.
- Why it’s hard: Requires cross-checking raw results.
- Static-analysis discoverability: Low.
- Suggested detection: Unit test with mix of valid/invalid results.
- Ranking: Exercise value 3/5, Stealth 4/5, Scorability 4/5

### B75
- Location: ValidationServiceImpl.java, runSupportRequestValidation (approx L245–L253)
- Core relevance: Bulk validation uses upsert grouping.
- Bug type: Data duplication.
- Proposed change: Generate a new runId per pipeline loop instead of once per run.
- Trigger conditions: Multiple pipelines for support requests.
- Expected symptom: Multiple rows per content per run; reports duplicated.
- Why it’s hard: Only visible with multiple pipelines.
- Static-analysis discoverability: Low.
- Suggested detection: Integration test with 2 pipelines.
- Ranking: Exercise value 5/5, Stealth 5/5, Scorability 4/5

### B76
- Location: ValidationStepFactory.java, REGEX_VALIDATION branch (approx L42–L47)
- Core relevance: Regex validator used in pipelines.
- Bug type: Incorrect default.
- Proposed change: If pattern blank, default to `".*"` instead of throwing.
- Trigger conditions: Misconfigured regex step.
- Expected symptom: All values fail validation.
- Why it’s hard: Looks like strict policy; misconfiguration masked.
- Static-analysis discoverability: Low.
- Suggested detection: Unit test that blank pattern rejects config.
- Ranking: Exercise value 4/5, Stealth 4/5, Scorability 4/5

### B77
- Location: LengthValidator.java, validate(...) (approx L29–L39)
- Core relevance: Length validation is common.
- Bug type: Off-by-one boundary.
- Proposed change: Use `<= minLength` and `>= maxLength`.
- Trigger conditions: Values exactly at boundaries.
- Expected symptom: Boundary values rejected.
- Why it’s hard: Only hits exact boundary lengths.
- Static-analysis discoverability: Medium.
- Suggested detection: Boundary-focused unit tests.
- Ranking: Exercise value 3/5, Stealth 3/5, Scorability 5/5

### B78
- Location: NumericRangeValidator.java, validate(...) (approx L41–L54)
- Core relevance: Numeric validation for pricing/ranges.
- Bug type: Error handling gap.
- Proposed change: In NumberFormatException, return valid instead of invalid.
- Trigger conditions: Non-numeric input values.
- Expected symptom: Invalid numeric fields pass validation.
- Why it’s hard: Only visible with non-numeric data.
- Static-analysis discoverability: Low.
- Suggested detection: Unit test with non-numeric input.
- Ranking: Exercise value 4/5, Stealth 4/5, Scorability 4/5

### B79
- Location: PhoneNumberForbiddenValidator.java, PHONE_PATTERN (approx L15)
- Core relevance: Phone number detection is policy-critical.
- Bug type: Pattern regression (false negatives).
- Proposed change: Narrow regex to `\\d{7}` (too strict).
- Trigger conditions: Phone numbers with spaces, plus signs, or separators.
- Expected symptom: Phone numbers slip through.
- Why it’s hard: Only certain formats affected.
- Static-analysis discoverability: Low.
- Suggested detection: Parametrized tests with varied phone formats.
- Ranking: Exercise value 3/5, Stealth 4/5, Scorability 4/5

### B80
- Location: RegexValidator.java, validate(...) (approx L41–L46)
- Core relevance: Regex validation used across pipelines.
- Bug type: Matching semantics change.
- Proposed change: Use `matcher.matches()` instead of `matcher.find()`.
- Trigger conditions: Regex intended to match substrings.
- Expected symptom: Forbidden patterns no longer detected.
- Why it’s hard: Only for non-anchored patterns; subtle.
- Static-analysis discoverability: Medium.
- Suggested detection: Unit test with substring match pattern.
- Ranking: Exercise value 4/5, Stealth 3/5, Scorability 5/5

## Top 10 recommended set
- B43: Negative caching via null values; high-impact and subtle.
- B44: Weak keys cause nondeterministic cache misses; great debugging exercise.
- B46: Rule order exposes admin endpoints; security-critical and scorable.
- B48: Rate limiter path coverage bug; reliability impact.
- B53: Wrong content-type mapping breaks validation silently.
- B57: Swapped IDs in ValidationResult; deep data-integrity issue.
- B64: Pipeline step association bug; validators silently skipped.
- B67: run_id handling breaks deduplication; DB growth and report drift.
- B73: Case mismatch yields no pipelines; silent failure in core workflow.
- B75: Per-pipeline runId duplication; subtle reporting bugs with multiple pipelines.
