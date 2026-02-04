# Debugging/Triage Exercise Proposal

## Repo map
- Entry point and cross-cutting config
  - src/main/java/com/dehold/contentmanager/ContentManagerApplication.java: Spring Boot entrypoint, enables caching and retry.
  - src/main/java/com/dehold/contentmanager/config/CacheConfig.java: Caffeine cache setup and TTL.
  - src/main/java/com/dehold/contentmanager/config/SecurityConfig.java: Basic auth, JDBC users, HTTP auth rules.
  - src/main/java/com/dehold/contentmanager/config/WebMvcConfig.java: Registers RateLimitInterceptor on /api/**.
- Content domains (core HTTP APIs)
  - content/blogpost: BlogPost CRUD, pagination, soft delete, history, moderation state machine, export endpoints.
    - service/BlogPostService.java, repository/BlogPostRepository.java, web/BlogPostController.java
    - export/*: ExportService, CSV/XML converters
  - content/customersupport: SupportRequest/SupportResponse CRUD, retry, caching, subscribers.
    - service/SupportRequestService.java, service/SupportResponseServiceImpl.java
  - content/generic: GenericContentModel with JSON fields, CRUD and validation integration.
  - content/productoffer: ProductOffer CRUD with pricing fields.
  - content/webhook: Webhook CRUD with URL validation and ownership checks.
  - content/faqpage: FAQ persistence (repository-centric, no main API controller).
- Validation subsystem (core data flow)
  - validation/pipeline: pipeline builder and factory, field extractors.
  - validation/step: validators (length, regex, numeric range, forbidden words, phone).
  - validation/service: ValidationServiceImpl, report generation/export.
  - validation/repository: validation_result, validation_pipeline, forbidden_words persistence.
  - validation/web: validation endpoints and pipeline CRUD endpoints.
- Rate limiting subsystem
  - ratelimiter/config: TokenBucket, RateLimitInterceptor.
  - ratelimiter/service/repository: per-endpoint configs with cached prefix matching.
- Persistence and schema
  - JdbcTemplate repositories across domains; schema in src/main/resources/schema.sql.
  - H2 for test, MySQL for prod, caching via Caffeine.

## Bug candidates
Ranking scale: 1 (low) to 5 (high) for Exercise value, Stealth, Scorability.

### B01
- Location: src/main/java/com/dehold/contentmanager/content/blogpost/service/BlogPostService.java, findPaginated(int page, int size, UUID userId), around L137-L149
- Core relevance: Blog post list endpoint is a primary API and common UI path.
- Bug type: Off-by-one pagination.
- Proposed change: Compute offset as (page + 1) * size instead of page * size.
- Trigger conditions: Any page request when there are multiple pages of data.
- Expected symptom: Page 0 starts at what should be page 1; users miss first items.
- Why it is hard: Only visible with enough data; looks like data ordering issue. Likely too easy unless pagination tests are removed.
- Static-analysis discoverability: Medium (offset logic is small but visible).
- Suggested detection: Integration test asserting page 0 includes earliest item.
- Ranking: Exercise value 4/5, Stealth 2/5, Scorability 5/5

### B02
- Location: src/main/java/com/dehold/contentmanager/content/blogpost/service/BlogPostService.java, findPaginated(... includeSoftDeleted), around L296-L312
- Core relevance: Soft delete plus pagination is core for blog post workflows.
- Bug type: Count mismatch / pagination metadata error.
- Proposed change: Call countBlogPosts(userId, false) regardless of includeSoftDeleted.
- Trigger conditions: Soft-deleted posts exist and includeSoftDeleted=true.
- Expected symptom: totalElements and totalPages undercount; last flag wrong.
- Why it is hard: Only shows with soft-deleted content and specific flag.
- Static-analysis discoverability: Low.
- Suggested detection: Integration test for includeSoftDeleted pagination counts.
- Ranking: Exercise value 4/5, Stealth 4/5, Scorability 4/5

### B03
- Location: src/main/java/com/dehold/contentmanager/content/blogpost/service/BlogPostService.java, getBlogPost(UUID, boolean), around L290-L294
- Core relevance: Single blog post fetch is a hot path; caching is critical.
- Bug type: Cache key collision / stale cache.
- Proposed change: Change @Cacheable key to "#id" (drop includeSoftDeleted).
- Trigger conditions: First request caches includeSoftDeleted=false, then includeSoftDeleted=true (or vice versa).
- Expected symptom: Soft-deleted posts stay hidden or appear when they should not.
- Why it is hard: Depends on cache state and call order; intermittent.
- Static-analysis discoverability: Low.
- Suggested detection: Cache-aware integration test toggling includeSoftDeleted.
- Ranking: Exercise value 5/5, Stealth 5/5, Scorability 4/5

### B04
- Location: src/main/java/com/dehold/contentmanager/content/blogpost/service/BlogPostService.java, findPaginated cache key, around L137-L138 or L296-L298
- Core relevance: Cached pagination is a core read path.
- Bug type: Cache collision across sizes.
- Proposed change: Remove size from the cache key.
- Trigger conditions: Same page requested with different size.
- Expected symptom: Wrong number of items returned; inconsistent paging.
- Why it is hard: Requires cache and multiple page sizes; appears as flaky data.
- Static-analysis discoverability: Low.
- Suggested detection: Integration test for cache key correctness.
- Ranking: Exercise value 4/5, Stealth 4/5, Scorability 4/5

### B05
- Location: src/main/java/com/dehold/contentmanager/content/blogpost/repository/BlogPostRepository.java, softDelete(UUID id), around L216-L220
- Core relevance: Soft delete affects all read paths and compliance workflows.
- Bug type: Data integrity / soft delete not enforced.
- Proposed change: Update only deleted_at, omit setting soft_deleted=true.
- Trigger conditions: Soft-delete endpoint called.
- Expected symptom: Soft-deleted posts still appear in normal listings.
- Why it is hard: Looks like filter bug elsewhere; easy to misdiagnose.
- Static-analysis discoverability: Low.
- Suggested detection: Integration test for soft delete filtering.
- Ranking: Exercise value 4/5, Stealth 4/5, Scorability 4/5

### B06
- Location: src/main/java/com/dehold/contentmanager/content/blogpost/repository/BlogPostHistoryRepository.java, getNextVersionNumber, around L50-L56
- Core relevance: Version history is a key feature and affects restore.
- Bug type: Off-by-one versioning.
- Proposed change: Return version (or version==null?0:version) instead of version+1.
- Trigger conditions: First and subsequent edits to a blog post.
- Expected symptom: Duplicate version numbers or version 0 entries; restore may pick wrong version.
- Why it is hard: Only visible after multiple updates. Likely too easy unless history tests are removed.
- Static-analysis discoverability: Medium.
- Suggested detection: Unit test for version increments.
- Ranking: Exercise value 4/5, Stealth 2/5, Scorability 5/5

### B07
- Location: src/main/java/com/dehold/contentmanager/content/blogpost/service/BlogPostService.java, updateBlogPostVersion, around L118-L125
- Core relevance: History capture is central to audit/restore behavior.
- Bug type: History capture ordering error.
- Proposed change: Move saveHistory(...) after updating the blog post.
- Trigger conditions: Updating a blog post with history enabled.
- Expected symptom: History stores the new content, losing the previous version.
- Why it is hard: Appears as restore bug; only seen when comparing history. Likely too easy unless history tests are removed.
- Static-analysis discoverability: Medium.
- Suggested detection: Integration test update then restore.
- Ranking: Exercise value 5/5, Stealth 3/5, Scorability 5/5

### B08
- Location: src/main/java/com/dehold/contentmanager/content/blogpost/repository/BlogPostRepository.java, searchByTerm, around L207-L213
- Core relevance: Search endpoint is a core discovery feature.
- Bug type: Search logic regression.
- Proposed change: Build LIKE pattern as term.trim() + "%" (prefix-only).
- Trigger conditions: Search term appears mid-string.
- Expected symptom: Missing matches; search looks incomplete.
- Why it is hard: Depends on term position; easily overlooked. Likely too easy unless search tests are removed.
- Static-analysis discoverability: Medium.
- Suggested detection: Integration test with mid-string match.
- Ranking: Exercise value 3/5, Stealth 2/5, Scorability 5/5

### B09
- Location: src/main/java/com/dehold/contentmanager/content/blogpost/repository/BlogPostRepository.java, getPaginatedBlogPosts(... includeSoftDeleted), around L236-L249
- Core relevance: Pagination is a high-traffic read path.
- Bug type: Pagination instability / performance regression.
- Proposed change: Remove ORDER BY clause to "optimize".
- Trigger conditions: Concurrent inserts/updates or DB reordering.
- Expected symptom: Duplicates or missing items across pages.
- Why it is hard: Nondeterministic; only appears under load or churn.
- Static-analysis discoverability: Low.
- Suggested detection: Integration test requesting multiple pages and verifying no overlap.
- Ranking: Exercise value 5/5, Stealth 5/5, Scorability 4/5

### B10
- Location: src/main/java/com/dehold/contentmanager/content/blogpost/service/BlogPostService.java, restoreVersion, around L260-L269
- Core relevance: Restore is a key workflow for moderation/history.
- Bug type: Wrong version selection.
- Proposed change: Change filter to version >= requested and take first.
- Trigger conditions: Restoring when newer versions exist.
- Expected symptom: Restores a newer version than requested.
- Why it is hard: Restore appears to work; only exact version comparisons reveal issue.
- Static-analysis discoverability: Low-medium.
- Suggested detection: History test with multiple versions.
- Ranking: Exercise value 4/5, Stealth 4/5, Scorability 4/5

### B11
- Location: src/main/java/com/dehold/contentmanager/content/blogpost/export/ExportXmlConverter.java, toXmlBytes, around L31-L75
- Core relevance: Export endpoints are high value for data portability.
- Bug type: Timezone/format drift.
- Proposed change: Use ISO_LOCAL_DATE_TIME with system default zone instead of ISO_INSTANT.
- Trigger conditions: XML export consumed by UTC-expecting clients.
- Expected symptom: Timestamps shift or missing "Z"; cross-system comparisons fail.
- Why it is hard: Only visible when comparing across timezones; not obvious in logs.
- Static-analysis discoverability: Low.
- Suggested detection: Export test asserting exact timestamp format. Likely too easy unless XML export tests are removed.
- Ranking: Exercise value 4/5, Stealth 4/5, Scorability 4/5

### B12
- Location: src/main/java/com/dehold/contentmanager/content/blogpost/export/ExportCsvConverter.java, quote method, around L83-L86
- Core relevance: CSV export is a main user-facing feature.
- Bug type: Data integrity / CSV escaping.
- Proposed change: Remove the quote escaping (do not double internal quotes).
- Trigger conditions: Content contains quotes or commas.
- Expected symptom: Malformed CSV; columns shift or parse errors.
- Why it is hard: Only hits specific content; casual testing may miss.
- Static-analysis discoverability: Medium.
- Suggested detection: Unit test for CSV with quoted text.
- Ranking: Exercise value 4/5, Stealth 3/5, Scorability 4/5

### B13
- Location: src/main/java/com/dehold/contentmanager/content/blogpost/export/ExportService.java, exportForUserByType, around L40-L58
- Core relevance: Export filtering is a core API contract.
- Bug type: API contract drift.
- Proposed change: Ignore contentType and always return exportAllForUser.
- Trigger conditions: Export with contentType=blogpost/supportrequest/supportresponse.
- Expected symptom: Extra content types returned; payload larger than requested.
- Why it is hard: Output still looks valid; only consumers checking counts notice. Likely too easy unless filter tests are removed.
- Static-analysis discoverability: Medium.
- Suggested detection: Integration test for contentType filter.
- Ranking: Exercise value 4/5, Stealth 2/5, Scorability 5/5

### B14
- Location: src/main/java/com/dehold/contentmanager/content/customersupport/service/SupportRequestService.java, updateCustomerRequest, around L86-L89
- Core relevance: SupportRequest CRUD is a primary API.
- Bug type: Cache invalidation gap.
- Proposed change: Remove cache eviction for supportRequestById.
- Trigger conditions: Update a request then fetch by id.
- Expected symptom: Stale data returned until cache expiry.
- Why it is hard: Requires cache enabled and specific sequence; appears intermittent.
- Static-analysis discoverability: Low.
- Suggested detection: Cache-aware integration test after update.
- Ranking: Exercise value 4/5, Stealth 4/5, Scorability 4/5

### B15
- Location: src/main/java/com/dehold/contentmanager/content/customersupport/service/SupportRequestService.java, addSubscriber, around L100-L110
- Core relevance: Subscriber management affects notifications and workflows.
- Bug type: Lost update.
- Proposed change: Remove repository.update(req) after modifying subscribers.
- Trigger conditions: Subscribe endpoint called.
- Expected symptom: API returns OK but subscribers not persisted.
- Why it is hard: In-memory object looks updated; only DB reload shows issue.
- Static-analysis discoverability: Low.
- Suggested detection: Integration test that reloads request and verifies subscribers.
- Ranking: Exercise value 3/5, Stealth 4/5, Scorability 4/5

### B16
- Location: src/main/java/com/dehold/contentmanager/content/customersupport/repository/SupportRequestRepository.java, update, around L67-L75
- Core relevance: SupportRequest persistence is core to customer support.
- Bug type: Data integrity (timestamps).
- Proposed change: Set created_at to Instant.now() on update (or use updatedAt for createdAt).
- Trigger conditions: Any update call.
- Expected symptom: created_at changes; audit data corrupted.
- Why it is hard: Often not asserted; looks like expected update behavior.
- Static-analysis discoverability: Low.
- Suggested detection: Unit test preserving created_at on update.
- Ranking: Exercise value 3/5, Stealth 4/5, Scorability 4/5

### B17
- Location: src/main/java/com/dehold/contentmanager/content/customersupport/repository/SupportResponseRepository.java, getSupportResponsesByUserId, around L66-L70
- Core relevance: Support response visibility is core to users and validation.
- Bug type: Authorization/data leakage.
- Proposed change: Join/filter on the wrong column (cr.user_id or sr.user_id instead of cr.customer_id).
- Trigger conditions: Fetching responses by user id.
- Expected symptom: User sees other users' responses or misses their own.
- Why it is hard: Requires multi-user dataset; can look like data issues.
- Static-analysis discoverability: Medium.
- Suggested detection: Integration test with multiple users and customers.
- Ranking: Exercise value 5/5, Stealth 4/5, Scorability 4/5

### B18
- Location: src/main/java/com/dehold/contentmanager/validation/pipeline/ValidationPipelineFactory.java, getFieldExtractor for GenericContentModel, around L61-L69
- Core relevance: Validation pipeline is core for content quality.
- Bug type: Field mapping error.
- Proposed change: Use capitalize(fieldName) when accessing genericContent.getFieldNameToValue().
- Trigger conditions: Generic content with lower-case field names.
- Expected symptom: Validator reads null and passes invalid content.
- Why it is hard: Only affects generic content; validation still "works" but misses errors.
- Static-analysis discoverability: Low.
- Suggested detection: Integration test for generic content validation.
- Ranking: Exercise value 5/5, Stealth 5/5, Scorability 4/5

### B19
- Location: src/main/java/com/dehold/contentmanager/validation/pipeline/ValidationPipelineFactory.java, reflection path, around L73-L86
- Core relevance: Pipeline creation is central to validation.
- Bug type: Silent validation skip.
- Proposed change: On NoSuchMethodException, return extractor that always returns null instead of throwing.
- Trigger conditions: Misconfigured field name in pipeline.
- Expected symptom: Validation passes silently; misconfiguration undetected.
- Why it is hard: No errors, only missing validations.
- Static-analysis discoverability: Low.
- Suggested detection: Unit test expecting error on unknown field.
- Ranking: Exercise value 4/5, Stealth 5/5, Scorability 3/5

### B20
- Location: src/main/java/com/dehold/contentmanager/validation/step/ValidationStepFactory.java, LENGTH_VALIDATION path, around L34-L37
- Core relevance: Length validation is a common pipeline step.
- Bug type: Parameter swap.
- Proposed change: Parse minLength from "maxLength" and maxLength from "minLength".
- Trigger conditions: Any length validation.
- Expected symptom: Unexpected failures/passes at boundaries.
- Why it is hard: Looks like bad configuration; only boundary tests reveal. Likely too easy unless unit tests are removed.
- Static-analysis discoverability: Medium.
- Suggested detection: Unit tests for min/max boundaries.
- Ranking: Exercise value 3/5, Stealth 2/5, Scorability 5/5

### B21
- Location: src/main/java/com/dehold/contentmanager/validation/step/RegexValidator.java, constructor, around L26-L30
- Core relevance: Regex validation is used for policy enforcement.
- Bug type: Input validation gap.
- Proposed change: Remove CASE_INSENSITIVE flag from Pattern.compile.
- Trigger conditions: Regex relies on case-insensitive matching.
- Expected symptom: Forbidden patterns slip through in different casing.
- Why it is hard: Depends on data casing; subtle in manual testing.
- Static-analysis discoverability: Low.
- Suggested detection: Unit test for case-insensitive match.
- Ranking: Exercise value 3/5, Stealth 4/5, Scorability 4/5

### B22
- Location: src/main/java/com/dehold/contentmanager/validation/step/NumericRangeValidator.java, checkMinBoundary/checkMaxBoundary, around L66-L90
- Core relevance: Numeric validation affects pricing and numeric fields.
- Bug type: Boundary condition error.
- Proposed change: Invert inclusive logic (use < where <= or vice versa).
- Trigger conditions: Value exactly equals min or max.
- Expected symptom: Boundary values incorrectly rejected or accepted.
- Why it is hard: Only boundary values; easy to miss without specific tests. Likely too easy unless numeric range tests are removed.
- Static-analysis discoverability: Medium.
- Suggested detection: Boundary-focused unit tests.
- Ranking: Exercise value 4/5, Stealth 2/5, Scorability 5/5

### B23
- Location: src/main/java/com/dehold/contentmanager/validation/step/ForbiddenWordValidator.java, fetchForbiddenWords, around L64-L69
- Core relevance: Forbidden words are central to moderation.
- Bug type: Lookup key mismatch.
- Proposed change: Use content.getClass().getName() instead of getSimpleName().toLowerCase().
- Trigger conditions: Any forbidden words validation.
- Expected symptom: Custom forbidden words never applied; only defaults.
- Why it is hard: Validation still runs but with reduced coverage.
- Static-analysis discoverability: Low.
- Suggested detection: Unit test with custom forbidden words.
- Ranking: Exercise value 4/5, Stealth 4/5, Scorability 4/5

### B24
- Location: src/main/java/com/dehold/contentmanager/validation/service/ValidationServiceImpl.java, runSupportRequestValidation, around L237-L254
- Core relevance: Bulk validation is a key user workflow.
- Bug type: Data duplication / run tracking.
- Proposed change: Generate a new runId per ValidationResult rather than once per run.
- Trigger conditions: Support request bulk validation.
- Expected symptom: validation_result table grows rapidly; duplicates instead of upserts.
- Why it is hard: Functional response still correct; only persistence/reporting shows drift.
- Static-analysis discoverability: Low.
- Suggested detection: Integration test verifying upsert behavior.
- Ranking: Exercise value 5/5, Stealth 5/5, Scorability 4/5

### B25
- Location: src/main/java/com/dehold/contentmanager/validation/repository/ValidationResultRepository.java, upsert, around L82-L99
- Core relevance: Validation results persistence is core to reporting.
- Bug type: Data integrity collision.
- Proposed change: Remove content_type from MERGE ON condition.
- Trigger conditions: Same UUID used across content types.
- Expected symptom: Results overwrite each other across types.
- Why it is hard: Requires ID collision across types; subtle to trace.
- Static-analysis discoverability: Low.
- Suggested detection: Integration test with identical UUIDs across types.
- Ranking: Exercise value 5/5, Stealth 5/5, Scorability 4/5

### B26
- Location: src/main/java/com/dehold/contentmanager/validation/repository/ValidationPipelineRepository.java, insertValidationStep, around L83-L94
- Core relevance: Pipeline config is core to validation behavior.
- Bug type: Configuration inversion.
- Proposed change: Persist is_enabled as !step.isEnabled().
- Trigger conditions: Disabled step configured.
- Expected symptom: Disabled steps run; enabled steps may be skipped based on UI state.
- Why it is hard: Only appears when users toggle steps off; otherwise looks normal.
- Static-analysis discoverability: Low.
- Suggested detection: Unit test for disabled steps.
- Ranking: Exercise value 4/5, Stealth 4/5, Scorability 4/5

### B27
- Location: src/main/java/com/dehold/contentmanager/validation/service/ValidationServiceImpl.java, generateValidationReport, around L131-L148
- Core relevance: Reports are a key user-facing summary.
- Bug type: Reporting inconsistency.
- Proposed change: Do not normalize contentType to lower-case for detailed maps.
- Trigger conditions: Mixed case contentType values in stored results.
- Expected symptom: Duplicate keys ("BlogPost" vs "blogpost") and wrong totals per type.
- Why it is hard: Report still returns data; only careful aggregation reveals drift.
- Static-analysis discoverability: Low.
- Suggested detection: Unit test with mixed case content types.
- Ranking: Exercise value 3/5, Stealth 4/5, Scorability 3/5

### B28
- Location: src/main/java/com/dehold/contentmanager/validation/service/ForbiddenWordsService.java, getDefaultForbiddenWords, around L63-L70
- Core relevance: Forbidden words are used across validation.
- Bug type: Resource leak / IO.
- Proposed change: Replace try-with-resources with manual InputStream creation without close.
- Trigger conditions: Repeated calls to forbidden words endpoints.
- Expected symptom: File descriptor leak; long-running service degrades.
- Why it is hard: Not visible in functional tests; shows under uptime/load.
- Static-analysis discoverability: Medium (some linters detect).
- Suggested detection: Long-running integration test or resource monitoring.
- Ranking: Exercise value 3/5, Stealth 4/5, Scorability 3/5

### B29
- Location: src/main/java/com/dehold/contentmanager/content/generic/repository/GenericModelRepository.java, serializeFields, around L106-L117
- Core relevance: Generic content persistence is a core extensibility feature.
- Bug type: Serialization contract drift.
- Proposed change: Store valueType as lower-case (valueType.name().toLowerCase()).
- Trigger conditions: Read back generic content.
- Expected symptom: ValueType.valueOf fails; reads throw or fields drop.
- Why it is hard: Writes succeed; errors occur later on reads.
- Static-analysis discoverability: Low.
- Suggested detection: Repository round-trip test.
- Ranking: Exercise value 4/5, Stealth 4/5, Scorability 4/5

### B30
- Location: src/main/java/com/dehold/contentmanager/content/generic/repository/GenericModelRepository.java, coerce, around L155-L162
- Core relevance: Generic content supports numeric validation and reporting.
- Bug type: Numeric precision loss.
- Proposed change: For DECIMAL, coerce to Integer (intValue) instead of Double.
- Trigger conditions: Decimal fields with fractional values.
- Expected symptom: Values truncated; validation/reporting inconsistent.
- Why it is hard: Looks like data entry issue; only decimals affected.
- Static-analysis discoverability: Low.
- Suggested detection: Unit test for decimal round-trip.
- Ranking: Exercise value 4/5, Stealth 4/5, Scorability 4/5

### B31
- Location: src/main/java/com/dehold/contentmanager/content/generic/service/GenericContentServiceImpl.java, update, around L43-L50
- Core relevance: Generic content update is core API behavior.
- Bug type: Silent upsert.
- Proposed change: Remove existence check before repository.save.
- Trigger conditions: Update called with missing id.
- Expected symptom: Update creates new content instead of 404.
- Why it is hard: Only visible when client sends bad id; no error surfaced.
- Static-analysis discoverability: Low.
- Suggested detection: Integration test expecting 404 on update for missing id.
- Ranking: Exercise value 3/5, Stealth 4/5, Scorability 4/5

### B32
- Location: src/main/java/com/dehold/contentmanager/content/productoffer/repository/ProductOfferRepository.java, create, around L55-L74
- Core relevance: ProductOffer pricing is central to marketplace data.
- Bug type: Data integrity (field swap).
- Proposed change: Swap original_price and offer_price in INSERT parameters.
- Trigger conditions: Create a product offer.
- Expected symptom: Pricing inverted; discount math wrong.
- Why it is hard: Values still plausible; only detected by comparing inputs to outputs.
- Static-analysis discoverability: Medium.
- Suggested detection: Repository integration test for all fields.
- Ranking: Exercise value 4/5, Stealth 3/5, Scorability 4/5

### B33
- Location: src/main/java/com/dehold/contentmanager/content/productoffer/service/ProductOfferServiceImpl.java, updateProductOffer, around L78-L85
- Core relevance: ProductOffer updates are common and user-visible.
- Bug type: Field mapping error.
- Proposed change: Set offerPrice from offer.getOriginalPrice (swap on update).
- Trigger conditions: Update product offer pricing.
- Expected symptom: Updated price fields incorrect.
- Why it is hard: Update appears successful; only careful value checks expose it.
- Static-analysis discoverability: Medium.
- Suggested detection: Unit test for update field mapping.
- Ranking: Exercise value 3/5, Stealth 3/5, Scorability 4/5

### B34
- Location: src/main/java/com/dehold/contentmanager/user/service/UserServiceImpl.java, updateUser, around L86-L88
- Core relevance: User updates and auth data are core for security.
- Bug type: Security/identity drift.
- Proposed change: Remove updateAuthorityUsername call.
- Trigger conditions: User changes username.
- Expected symptom: Auth table retains old username; login fails or roles missing.
- Why it is hard: Only affects username changes; likely too easy unless user update tests are removed.
- Static-analysis discoverability: Medium.
- Suggested detection: Integration test that changes username and authenticates.
- Ranking: Exercise value 4/5, Stealth 2/5, Scorability 5/5

### B35
- Location: src/main/java/com/dehold/contentmanager/user/service/UserServiceImpl.java, deleteUser, around L95-L101
- Core relevance: User deletion is a core admin/security path.
- Bug type: Cleanup gap / security hygiene.
- Proposed change: Remove deleteSecurityAuthorities or use wrong username.
- Trigger conditions: Delete user.
- Expected symptom: Orphaned authorities; potential security confusion if username reused.
- Why it is hard: Only visible in DB or when username reused.
- Static-analysis discoverability: Low.
- Suggested detection: Integration test asserting authorities cleanup.
- Ranking: Exercise value 3/5, Stealth 4/5, Scorability 4/5

### B36
- Location: src/main/java/com/dehold/contentmanager/user/web/UserController.java, webhook ownership checks, around L179-L212
- Core relevance: Webhook management is user-facing and security-sensitive.
- Bug type: Authorization bypass.
- Proposed change: Compare webhook.getId() to userId (or remove ownership check).
- Trigger conditions: User accesses another user's webhook id.
- Expected symptom: Cross-user read/update/delete allowed.
- Why it is hard: Requires crafted IDs; not in happy-path tests.
- Static-analysis discoverability: Medium.
- Suggested detection: Security-focused integration tests.
- Ranking: Exercise value 5/5, Stealth 4/5, Scorability 4/5

### B37
- Location: src/main/java/com/dehold/contentmanager/config/SecurityConfig.java, filterChain, around L59-L64
- Core relevance: Security rules protect core mutation endpoints.
- Bug type: Security misconfiguration.
- Proposed change: Permit PUT/DELETE to /api/users/* (or broaden to /api/**) instead of requiring auth.
- Trigger conditions: Unauthenticated user issues update/delete.
- Expected symptom: Unauthorized mutations succeed.
- Why it is hard: Small matcher change; not noticed without security tests.
- Static-analysis discoverability: Medium.
- Suggested detection: Security tests verifying auth requirements.
- Ranking: Exercise value 5/5, Stealth 3/5, Scorability 4/5

### B38
- Location: src/main/java/com/dehold/contentmanager/ratelimiter/config/TokenBucket.java, refill, around L21-L31
- Core relevance: Rate limiting protects all APIs.
- Bug type: Time unit error.
- Proposed change: Compute slots using elapsed / 1000 instead of elapsed / refillIntervalMillis.
- Trigger conditions: Any sustained traffic.
- Expected symptom: Tokens refill too fast or too slow; rate limiting ineffective.
- Why it is hard: Looks like configuration issue; only visible under load.
- Static-analysis discoverability: Low.
- Suggested detection: Time-based unit test or load test.
- Ranking: Exercise value 5/5, Stealth 5/5, Scorability 4/5

### B39
- Location: src/main/java/com/dehold/contentmanager/ratelimiter/service/RateLimitConfigService.java, reloadCache/findBestMatchForPath, around L24-L53
- Core relevance: Per-endpoint rate limits are critical for reliability.
- Bug type: Config precedence error.
- Proposed change: Sort cachedConfigs ascending by path length (shortest prefix wins).
- Trigger conditions: Overlapping path patterns (e.g., /api and /api/users).
- Expected symptom: Generic limits override specific ones.
- Why it is hard: Only shows with overlapping configs; appears as misconfigured limits.
- Static-analysis discoverability: Low.
- Suggested detection: Unit test for longest-prefix match.
- Ranking: Exercise value 4/5, Stealth 4/5, Scorability 4/5

### B40
- Location: src/main/java/com/dehold/contentmanager/ratelimiter/config/RateLimitInterceptor.java, preHandle, around L52-L60
- Core relevance: Rate limiting is enforced on every API call.
- Bug type: Key collision.
- Proposed change: Build composedKey without cfg.getPathPattern() (use only userKey).
- Trigger conditions: Multiple per-endpoint rate limit configs for same user.
- Expected symptom: Endpoints share one bucket, causing unexpected 429s.
- Why it is hard: Only appears with multiple configs; looks like "too strict" limits.
- Static-analysis discoverability: Low.
- Suggested detection: Integration test hitting two endpoints with different configs.
- Ranking: Exercise value 4/5, Stealth 4/5, Scorability 4/5

## Top 10 recommended set
- B03: Cache key collision between soft-deleted and normal reads; core read path and subtle cache state dependency.
- B07: History capture ordering bug that quietly corrupts restore semantics.
- B09: Unstable pagination via missing ORDER BY; nondeterministic and hard to catch.
- B13: Export filtering ignored; breaks API contract while still returning valid-looking data.
- B18: Generic content validation silently skips fields due to extractor mismatch.
- B22: Numeric boundary inversion; teaches careful boundary handling in validators.
- B24: RunId per result breaks de-duplication; subtle data growth and reporting drift.
- B25: Upsert merge missing content_type; cross-content collisions are hard to reason about.
- B36: Webhook ownership check bypass; clear security lesson with precise rubric.
- B38: Token refill unit bug; time-based reliability issue, not obvious in static review.
