package com.impactai.impactai.service;

import com.impactai.impactai.model.PRChangeInfo;
import com.impactai.impactai.parser.ParsedDependencyNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.impactai.impactai.util.GraphUtils.extractChangedNodeIdsFromPR;

@Service
public class WebhookProcessingService {

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
     * Process ping event asynchronously
     */
    @Async("webhookExecutor")
    public void processPingAsync(String owner, String repoName, String repoFullName,
                                 String defaultBranch, String repoLocalPath) {
        try {
            System.out.println("[ASYNC] Starting baseline setup for: " + repoFullName);

            // Check if already parsed (in case of re-ping)
            if (repoMetadataService.isRepoFullyParsed(repoFullName)) {
                System.out.println("[ASYNC] Repo already has baseline, skipping.");
                return;
            }

            // Parse entire repo to build baseline graph
            List<ParsedDependencyNode> allParsedNodes = repoParserService.parseFullRepo(repoLocalPath);

            // Build and store baseline graph
            graphBuilderService.build(allParsedNodes);

            // Mark as ready
            repoMetadataService.markRepoAsFullyParsed(repoFullName, "ping-" + System.currentTimeMillis());

            System.out.println("[ASYNC] ✓ Baseline complete! Parsed " + allParsedNodes.size() + " nodes for " + repoFullName);

        } catch (Exception e) {
            System.err.println("[ASYNC] Error during ping baseline setup: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Process PR webhook asynchronously
     */
    @Async("webhookExecutor")
    public void processPRAsync(String owner, String repoName, String repoFullName,
                               String defaultBranch, int prNumber, String headSha,
                               String action, String repoLocalPath) {
        try {
            System.out.println("[ASYNC] Starting PR processing for: " + repoFullName + " PR#" + prNumber);

            List<ParsedDependencyNode> allParsedNodes;

            // Check if baseline exists
            if (!repoMetadataService.isRepoFullyParsed(repoFullName)) {
                System.out.println("[ASYNC] === NO BASELINE FOUND: PERFORMING FULL SCAN ===");

                // Parse entire repo to build baseline
                allParsedNodes = repoParserService.parseFullRepo(repoLocalPath);
                System.out.println("[ASYNC] Full scan complete. Parsed " + allParsedNodes.size() + " nodes.");

                // Build baseline graph
                graphBuilderService.build(allParsedNodes);

                // Mark repo as scanned
                repoMetadataService.markRepoAsFullyParsed(repoFullName, headSha);

                System.out.println("[ASYNC] ✓ Baseline initialized for " + repoFullName);
                return;  // Skip impact analysis for baseline creation

            } else {
                System.out.println("[ASYNC] === INCREMENTAL PARSING (PR DIFF ONLY) ===");

                // Fetch changed files for this PR
                List<PRChangeInfo> changedFiles = prFileFetcher.fetchChangedFiles(owner, repoName, prNumber);

                // Convert GitHub relative paths to absolute local paths
                List<String> absolutePaths = new ArrayList<>();
                for (PRChangeInfo info : changedFiles) {
                    if (info.getFilePath().endsWith(".java")) {
                        String absolutePath = repoLocalPath + File.separator + info.getFilePath();
                        absolutePaths.add(absolutePath);
                        System.out.println("[ASYNC] Mapped: " + info.getFilePath() + " -> " + absolutePath);
                    }
                }

                // Parse only changed files
                allParsedNodes = dependencyParserService.parseChangedFiles(absolutePaths);
                System.out.println("[ASYNC] Incremental parse complete. Parsed " + allParsedNodes.size() + " nodes.");

                // Build/update in-memory dependency graph
                graphBuilderService.build(allParsedNodes);

                // Extract changed node IDs
                List<String> changedNodeIds = extractChangedNodeIdsFromPR(changedFiles, allParsedNodes);

                // Run impact analysis
                ImpactAnalysisService.ImpactReport impactReport = impactAnalysisService.analyzeImpact(
                        graphBuilderService.getGraph(), changedNodeIds);
                String risk = impactAnalysisService.calculateRisk(impactReport);
                String comment = impactReportFormatter.formatComment(impactReport, risk);

                // Print summary
                System.out.println("\n[ASYNC] ========= IMPACT ANALYSIS RESULT =========");
                System.out.println("[ASYNC] PR #" + prNumber + " - Changed Nodes:");
                for (String changed : impactReport.getChangedNodes()) {
                    System.out.println("[ASYNC]   [Changed] " + changed);
                }
                System.out.println("[ASYNC] Impacted (Class & Methods, incl. transitive):");
                for (String impacted : impactReport.getAllImpactedNodes()) {
                    if (!impactReport.getChangedNodes().contains(impacted)) {
                        System.out.println("[ASYNC]   [Impacted] " + impacted);
                    }
                }
                System.out.println("[ASYNC] Maximum Impact Depth: " + impactReport.getImpactDepth());
                System.out.println("[ASYNC] Risk Score: " + risk);
                System.out.println("[ASYNC] ==========================================\n");

                // Post comment for relevant actions
                if (action != null && List.of("opened", "reopened", "synchronize").contains(action)) {
                    try {
                        gitHubCommentService.postComment(owner, repoName, prNumber, comment);
                        System.out.println("[ASYNC] ✓ Posted impact analysis comment to PR #" + prNumber);
                    } catch (Exception e) {
                        System.err.println("[ASYNC] Failed to post comment: " + e.getMessage());
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("[ASYNC] Error processing PR webhook: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
