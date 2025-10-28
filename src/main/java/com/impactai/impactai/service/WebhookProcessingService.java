package com.impactai.impactai.service;

import com.impactai.impactai.model.LineRange;
import com.impactai.impactai.model.PRChangeInfo;
import com.impactai.impactai.parser.ParsedDependencyNode;
import com.impactai.impactai.util.ChangeAnalyzer;
import com.impactai.impactai.util.GraphUtils;
import com.impactai.impactai.util.PatchParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static com.impactai.impactai.util.GraphUtils.extractChangedNodeIdsFromPR;

@Service
public class WebhookProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookProcessingService.class);

    @Autowired
    private GitHubPRFileFetcherService prFileFetcher;

    @Autowired
    private DependencyParserService dependencyParserService;

    @Autowired
    private GraphBuilderService graphBuilderService;

    @Autowired
    private RepoMetadataService repoMetadataService;

    @Autowired
    private RepoParserService repoParserService;

    @Autowired
    private ImpactAnalysisService impactAnalysisService;

    @Autowired
    private ImpactReportFormatter impactReportFormatter;

    @Autowired
    private GitHubCommentService gitHubCommentService;

    /**
     * Process ping event asynchronously - builds baseline
     */
    @Async("webhookExecutor")
    public void processPingAsync(String owner, String repoName, String repoFullName,
                                 String defaultBranch, String repoLocalPath) {
        try {
            logger.info("[ASYNC] Starting baseline setup for: {}", repoFullName);

            // Check if already parsed
            if (repoMetadataService.isRepoFullyParsed(repoFullName)) {
                logger.info("[ASYNC] Repo already has baseline, skipping.");
                return;
            }

            // Parse entire repo to build baseline graph
            logger.debug("[ASYNC] Parsing full repository from: {}", repoLocalPath);
            List<ParsedDependencyNode> allParsedNodes = repoParserService.parseFullRepo(repoLocalPath);

            // Build and store baseline graph
            graphBuilderService.build(allParsedNodes);

            // Mark as ready
            repoMetadataService.markRepoAsFullyParsed(repoFullName, "ping-" + System.currentTimeMillis());

            logger.info("[ASYNC] ✓ Baseline complete! Parsed {} nodes for {}", allParsedNodes.size(), repoFullName);

        } catch (Exception e) {
            logger.error("[ASYNC] Error during ping baseline setup for {}: {}", repoFullName, e.getMessage(), e);
        }
    }

    /**
     * Process PR webhook asynchronously - incremental parsing with line-level detection
     * and sophisticated risk calculation
     */
    @Async("webhookExecutor")
    public void processPRAsync(String owner, String repoName, String repoFullName,
                               String defaultBranch, int prNumber, String headSha,
                               String action, String repoLocalPath) {
        try {
            logger.info("[ASYNC] Starting PR processing for: {} PR#{}", repoFullName, prNumber);

            List<ParsedDependencyNode> allParsedNodes;

            // Check if baseline exists
            if (!repoMetadataService.isRepoFullyParsed(repoFullName)) {
                logger.info("[ASYNC] === NO BASELINE FOUND: PERFORMING FULL SCAN ===");

                // Parse entire repo to build baseline
                allParsedNodes = repoParserService.parseFullRepo(repoLocalPath);
                logger.info("[ASYNC] Full scan complete. Parsed {} nodes.", allParsedNodes.size());

                // Build baseline graph
                graphBuilderService.build(allParsedNodes);

                // Mark repo as scanned
                repoMetadataService.markRepoAsFullyParsed(repoFullName, headSha);

                logger.info("[ASYNC] ✓ Baseline initialized for {}", repoFullName);
                return;  // Skip impact analysis for baseline creation

            } else {
                logger.info("[ASYNC] === INCREMENTAL PARSING (PR DIFF ONLY) ===");

                // Fetch changed files for this PR (includes patch data)
                logger.debug("[ASYNC] Fetching changed files for PR#{}", prNumber);
                List<PRChangeInfo> changedFiles = prFileFetcher.fetchChangedFiles(owner, repoName, prNumber);

                if (changedFiles == null || changedFiles.isEmpty()) {
                    logger.warn("[ASYNC] No changed files found for PR#{}", prNumber);
                    return;
                }

                logger.info("[ASYNC] Found {} changed files", changedFiles.size());

                // ===== STEP 1: Parse patches and extract line ranges =====
                logger.debug("[ASYNC] Parsing patches to extract changed line ranges...");
                for (PRChangeInfo changeInfo : changedFiles) {
                    try {
                        String patch = changeInfo.getPatch();
                        if (patch != null && !patch.isEmpty()) {
                            // Extract line ranges from unified diff
                            List<LineRange> changedLines = PatchParser.extractChangedLineRanges(patch);
                            changeInfo.setChangedLines(changedLines);

                            logger.debug("[ASYNC] File {}: extracted {} line ranges",
                                    changeInfo.getFilePath(), changedLines.size());
                        } else {
                            logger.warn("[ASYNC] No patch data for file: {} (changeType: {})",
                                    changeInfo.getFilePath(), changeInfo.getChangeType());
                        }
                    } catch (Exception e) {
                        logger.error("[ASYNC] Error parsing patch for {}: {}",
                                changeInfo.getFilePath(), e.getMessage());
                    }
                }

                // ===== STEP 2: Parse changed files =====
                logger.debug("[ASYNC] Parsing changed files...");
                List<String> absolutePaths = new ArrayList<>();
                for (PRChangeInfo info : changedFiles) {
                    if (info.getFilePath().endsWith(".java")) {
                        String absolutePath = repoLocalPath + File.separator + info.getFilePath();
                        absolutePaths.add(absolutePath);
                        logger.debug("[ASYNC] Mapped: {} -> {}", info.getFilePath(), absolutePath);
                    }
                }

                allParsedNodes = dependencyParserService.parseChangedFiles(absolutePaths);
                logger.info("[ASYNC] Incremental parse complete. Parsed {} nodes.", allParsedNodes.size());

                // ===== STEP 3: Build/update in-memory dependency graph =====
                logger.debug("[ASYNC] Building/updating dependency graph...");
                graphBuilderService.build(allParsedNodes);

                // ===== STEP 4: Extract changed node IDs (with LINE-LEVEL PRECISION) =====
                logger.debug("[ASYNC] Extracting changed node IDs with line-level detection...");
                List<String> changedNodeIds = extractChangedNodeIdsFromPR(changedFiles, allParsedNodes);

                if (changedNodeIds.isEmpty()) {
                    logger.warn("[ASYNC] No changed nodes detected for PR#{}", prNumber);
                    return;
                }

                logger.info("[ASYNC] Identified {} changed nodes", changedNodeIds.size());
                for (String nodeId : changedNodeIds) {
                    logger.debug("[ASYNC]   - {}", nodeId);
                }

                // ===== STEP 5: Run impact analysis =====
                logger.debug("[ASYNC] Running impact analysis...");
                ImpactAnalysisService.ImpactReport impactReport = impactAnalysisService.analyzeImpact(
                        graphBuilderService.getGraph(), changedNodeIds);

                // ===== STEP 5A: Analyze patches for comment-only changes =====
                logger.debug("[ASYNC] Analyzing patches for comment-only changes...");
                boolean isCommentOnlyOverall = true;
                for (PRChangeInfo changeInfo : changedFiles) {
                    try {
                        String patch = changeInfo.getPatch();
                        if (patch != null && !ChangeAnalyzer.isCommentOnly(patch)) {
                            isCommentOnlyOverall = false;
                            logger.debug("[ASYNC] File has logic changes: {}", changeInfo.getFilePath());
                            break;
                        }
                    } catch (Exception e) {
                        logger.debug("[ASYNC] Error analyzing patch: {}", e.getMessage());
                    }
                }

                if (isCommentOnlyOverall) {
                    logger.info("[ASYNC] ✓ All changes are comment-only");
                    impactReport.setHasCommentOnlyChanges(true);
                }

                // ===== STEP 5B: Check for critical methods in changed nodes =====
                logger.debug("[ASYNC] Checking for critical methods in changed nodes...");
                boolean hasCriticalMethods = false;
                for (String changedNodeId : changedNodeIds) {
                    try {
                        List<String> annotations = impactReport.getNodeAnnotations()
                                .getOrDefault(changedNodeId, new ArrayList<>());
                        for (String annotation : annotations) {
                            if (isCriticalAnnotation(annotation)) {
                                hasCriticalMethods = true;
                                logger.info("[ASYNC] ✓ Critical annotation found in {}: {}",
                                        changedNodeId, annotation);
                                break;
                            }
                        }
                        if (hasCriticalMethods) break;
                    } catch (Exception e) {
                        logger.debug("[ASYNC] Error checking annotations: {}", e.getMessage());
                    }
                }

                if (hasCriticalMethods) {
                    impactReport.setHasCriticalMethodChanges(true);
                }

                // ===== STEP 5C: Calculate risk with enhanced logic =====
                logger.debug("[ASYNC] Calculating risk score...");
                String risk = impactAnalysisService.calculateRisk(impactReport);

                // ===== STEP 6: Format comment =====
                logger.debug("[ASYNC] Formatting impact report comment...");
                String comment = impactReportFormatter.formatComment(impactReport, risk);

                // ===== STEP 7: Print summary to console/logs =====
                logger.info("\n[ASYNC] ========= IMPACT ANALYSIS RESULT =========");
                logger.info("[ASYNC] PR #{} for {}", prNumber, repoFullName);
                logger.info("[ASYNC] Changed Nodes:");
                for (String changed : impactReport.getChangedNodes()) {
                    logger.info("[ASYNC]   [Changed] {}", changed);
                }
                logger.info("[ASYNC] Impacted Nodes (transitive):");
                int impactedCount = 0;
                for (String impacted : impactReport.getAllImpactedNodes()) {
                    if (!impactReport.getChangedNodes().contains(impacted)) {
                        logger.info("[ASYNC]   [Impacted] {}", impacted);
                        impactedCount++;
                    }
                }
                if (impactedCount == 0) {
                    logger.info("[ASYNC]   (none)");
                }
                logger.info("[ASYNC] Impact Depth: {}", impactReport.getImpactDepth());
                logger.info("[ASYNC] Comment-only Changes: {}", impactReport.hasCommentOnlyChanges());
                logger.info("[ASYNC] Critical Methods: {}", impactReport.hasCriticalMethodChanges());
                logger.info("[ASYNC] Risk Score: {}", risk);
                logger.info("[ASYNC] ==========================================\n");

                // ===== STEP 8: Post comment to GitHub PR =====
                if (action != null && List.of("opened", "reopened", "synchronize").contains(action)) {
                    try {
                        logger.debug("[ASYNC] Posting impact analysis comment to PR#{}", prNumber);
                        gitHubCommentService.postComment(owner, repoName, prNumber, comment);
                        logger.info("[ASYNC] ✓ Posted impact analysis comment to PR#{}", prNumber);
                    } catch (Exception e) {
                        logger.error("[ASYNC] Failed to post comment to PR#{}: {}", prNumber, e.getMessage());
                    }
                } else {
                    logger.debug("[ASYNC] Skipping comment post (action: {}, PR action not in post list)", action);
                }
            }

        } catch (Exception e) {
            logger.error("[ASYNC] Error processing PR webhook for {}: {}", repoFullName, e.getMessage(), e);
        }
    }

    /**
     * Check if annotation indicates a critical method
     */
    private boolean isCriticalAnnotation(String annotation) {
        String[] criticalAnnotations = {
                "Transactional",
                "CacheEvict",
                "Cacheable",
                "CachePut",
                "Scheduled",
                "Async",
                "EventListener",
                "PreAuthorize",
                "Secured",
                "RolesAllowed",
                "PostMapping",
                "PutMapping",
                "DeleteMapping",
                "PatchMapping"
        };

        for (String critical : criticalAnnotations) {
            if (annotation.contains(critical)) {
                return true;
            }
        }
        return false;
    }
}
