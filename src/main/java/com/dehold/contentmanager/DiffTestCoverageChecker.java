package com.dehold.contentmanager;

import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.NodeList;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DiffTestCoverageChecker {

    public static void main(String[] args) throws Exception {
        double minCoverage = args.length > 0 ? Double.parseDouble(args[0]) : 0.80;

        // 1. Read exclusions from pom.xml
        List<Pattern> exclusionPatterns = readExclusionsFromPom();

        // 2. Read changed files
        List<String> changedFiles = getChangedFiles();
        List<String> changedClasses = changedFiles.stream()
                .filter(f -> f.endsWith(".java"))
                .map(f -> f.replace("src/main/java/", "").replace(".java", "").replace("/", "."))
                .filter(cls -> !isExcluded(cls, exclusionPatterns))
                .toList();

        if (changedClasses.isEmpty()) {
            System.out.println("No changed Java files. Skipping diff coverage check.");
            return;
        }

        // 3. Parse JaCoCo CSV
        File csv = new File("target/site/jacoco/jacoco.csv");
        if (!csv.exists()) {
            System.err.println("JaCoCo CSV report not found: " + csv.getAbsolutePath());
            System.exit(1);
        }

        Map<String, Double> coverageMap = parseJacocoCsv(csv);

        // 4. Compute diff-only coverage
        Map<String, Double> changedClassCoverage = new LinkedHashMap<>();
        for (String cls : changedClasses) {
            if (coverageMap.containsKey(cls)) {
                changedClassCoverage.put(cls, coverageMap.get(cls));
            }
        }

        double avgCoverage = changedClassCoverage.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0);

        System.out.println("\n=== Diff Coverage Report ===");
        System.out.println("Changed classes and their coverage:");
        changedClassCoverage.forEach((cls, cov) ->
                System.out.printf("  %s %s: %.1f%%\n",
                        cov < minCoverage ? "❌ " : "✅ ",
                        cls,
                        cov * 100)
        );

        System.out.printf("\nOverall diff coverage: %.1f%%\n", avgCoverage * 100);
        System.out.printf("Required minimum: %.1f%%\n\n", minCoverage * 100);

        if (avgCoverage < minCoverage) {
            List<String> failedClasses = changedClassCoverage.entrySet().stream()
                    .filter(e -> e.getValue() < minCoverage)
                    .map(e -> String.format("%s (%.1f%%)", e.getKey(), e.getValue() * 100))
                    .toList();

            System.err.println("❌  Diff coverage check FAILED.");
            System.err.println("Files below minimum coverage:");
            failedClasses.forEach(f -> System.err.println("  - " + f));
            System.exit(1);
        }

        System.out.println("✅ Diff coverage check PASSED.");
    }

    // Git diff
    private static List<String> getChangedFiles() throws Exception {
        Process p = new ProcessBuilder("git", "diff", "--name-only", "origin/main" + "...HEAD")
                .redirectErrorStream(true)
                .start();
        p.waitFor();
        return new String(p.getInputStream().readAllBytes())
                .lines()
                .collect(Collectors.toList());
    }

    private static Map<String, Double> parseJacocoCsv(File file) throws Exception {
        var map = new HashMap<String, Double>();
        List<String> lines = Files.readAllLines(file.toPath());

        for (int i = 1; i < lines.size(); i++) {
            String[] parts = lines.get(i).split(",");
            if (parts.length < 9) continue;

            String packageName = parts[1];
            String className = parts[2];
            String fullClassName = packageName + "." + className;

            int lineMissed = Integer.parseInt(parts[7]);
            int lineCovered = Integer.parseInt(parts[8]);
            int total = lineMissed + lineCovered;

            double ratio = total == 0 ? 1.0 : (double) lineCovered / total;
            map.put(fullClassName, ratio);
        }

        return map;
    }

    private static List<Pattern> readExclusionsFromPom() throws Exception {
        List<Pattern> patterns = new ArrayList<>();
        File pom = new File("pom.xml");

        if (!pom.exists()) {
            return patterns;
        }

        var factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setFeature("http://xml.org/sax/features/validation", false);

        var doc = factory.newDocumentBuilder().parse(pom);
        NodeList excludeNodes = doc.getElementsByTagName("exclude");

        for (int i = 0; i < excludeNodes.getLength(); i++) {
            String exclude = excludeNodes.item(i).getTextContent().trim();
            if (!exclude.isEmpty()) {
                String className = exclude
                        .replace(".class", "")
                        .replace("/", ".");
                patterns.add(Pattern.compile(Pattern.quote(className)));
            }
        }

        return patterns;
    }

    private static boolean isExcluded(String className, List<Pattern> exclusionPatterns) {
        return exclusionPatterns.stream().anyMatch(pattern -> pattern.matcher(className).matches());
    }
}

