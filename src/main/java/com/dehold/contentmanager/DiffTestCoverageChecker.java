package com.dehold.contentmanager;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class DiffTestCoverageChecker {

    public static void main(String[] args) throws Exception {
        double minCoverage = args.length > 0 ? Double.parseDouble(args[0]) : 0.80;

        // 1. Read changed files
        List<String> changedFiles = getChangedFiles();
        List<String> changedClasses = changedFiles.stream()
                .filter(f -> f.endsWith(".java"))
                .map(f -> f.replace("src/main/java/", "").replace(".java", "").replace("/", "."))
                .toList();

        if (changedClasses.isEmpty()) {
            System.out.println("No changed Java files. Skipping diff coverage check.");
            return;
        }

        // 2. Parse JaCoCo XML
        File xml = new File("target/site/jacoco/jacoco.xml");
        if (!xml.exists()) {
            System.err.println("JaCoCo XML report not found: " + xml.getAbsolutePath());
            System.exit(1);
        }

        Map<String, Double> coverageMap = parseJacoco(xml);

        // 3. Compute diff-only coverage
        List<Double> coverages = changedClasses.stream()
                .map(cls -> coverageMap.getOrDefault(cls, 0.0))
                .toList();

        double avgCoverage = coverages.stream().mapToDouble(Double::doubleValue).average().orElse(0);

        System.out.println("Changed classes: " + changedClasses);
        System.out.println("Diff coverage: " + avgCoverage);
        System.out.println("Required: " + minCoverage);

        if (avgCoverage < minCoverage) {
            System.err.println("❌ Diff coverage check FAILED.");
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

    // Parse JaCoCo XML
    private static Map<String, Double> parseJacoco(File file) throws Exception {
        var map = new HashMap<String, Double>();

        var factory = DocumentBuilderFactory.newInstance();
        // Disable DTD validation and external entity loading
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setFeature("http://xml.org/sax/features/validation", false);

        var doc = factory.newDocumentBuilder().parse(file);
        var nodes = doc.getElementsByTagName("package");

        for (int i = 0; i < nodes.getLength(); i++) {
            var pkg = nodes.item(i);
            var pkgName = pkg.getAttributes().getNamedItem("name").getNodeValue().replace("/", ".");

            var classes = pkg.getChildNodes();
            for (int j = 0; j < classes.getLength(); j++) {
                var c = classes.item(j);
                if (!c.getNodeName().equals("class")) continue;

                var className = pkgName + "." + c.getAttributes().getNamedItem("name").getNodeValue();

                var counters = c.getChildNodes();
                for (int k = 0; k < counters.getLength(); k++) {
                    var cnt = counters.item(k);
                    if (cnt.getNodeName().equals("counter") &&
                            cnt.getAttributes().getNamedItem("type").getNodeValue().equals("LINE")) {

                        int covered = Integer.parseInt(cnt.getAttributes().getNamedItem("covered").getNodeValue());
                        int missed = Integer.parseInt(cnt.getAttributes().getNamedItem("missed").getNodeValue());
                        double ratio = covered + missed == 0 ? 1.0 : (double) covered / (covered + missed);

                        map.put(className, ratio);
                    }
                }
            }
        }

        return map;
    }
}

