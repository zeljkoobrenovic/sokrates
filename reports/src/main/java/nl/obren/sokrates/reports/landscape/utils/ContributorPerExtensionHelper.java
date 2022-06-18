package nl.obren.sokrates.reports.landscape.utils;

import nl.obren.sokrates.sourcecode.githistory.ContributorPerExtensionStats;
import nl.obren.sokrates.sourcecode.landscape.LandscapeConfiguration;
import nl.obren.sokrates.sourcecode.landscape.analysis.ContributorRepositories;
import nl.obren.sokrates.sourcecode.operations.ComplexOperation;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Stream;

public class ContributorPerExtensionHelper {
    public Stream<Pair<String, ContributorPerExtensionStats>> getContributorsPerExtensionStream(List<Pair<String, ContributorPerExtensionStats>> extensionUpdates) {
        return extensionUpdates.stream().filter(e -> e.getRight().getFileUpdates90Days() > 0).limit(20);
    }

    private static String transformEmail(LandscapeConfiguration configuration, String email) {
        if (configuration.getTransformContributorEmails().size() > 0) {
            ComplexOperation operation = new ComplexOperation(configuration.getTransformContributorEmails());
            return operation.exec(email);
        }

        return email;
    }

    public List<Pair<String, ContributorPerExtensionStats>> getContributorStatsPerExtension(LandscapeConfiguration configuration, ContributorRepositories contributorRepositories) {
        Map<String, Pair<String,ContributorPerExtensionStats>> updatesPerExtensionMap = new HashMap<>();

        contributorRepositories.getRepositories().stream().filter(p -> p.getCommits90Days() > 0).forEach(repository -> {
            repository.getRepositoryAnalysisResults().getAnalysisResults().getContributorsAnalysisResults().getCommitsPerExtensions().forEach(commitsPerExtension -> {
                String extension = commitsPerExtension.getExtension();
                String email = contributorRepositories.getContributor().getEmail();
                commitsPerExtension.getContributorPerExtensionStats().stream()
                        .filter(stats -> transformEmail(configuration, stats.getContributor()).equalsIgnoreCase(email))
                        .forEach(stats -> {
                            if (updatesPerExtensionMap.containsKey(extension)) {
                                ContributorPerExtensionStats existingStats = updatesPerExtensionMap.get(extension).getRight();
                                existingStats.setFileUpdates(existingStats.getFileUpdates() + stats.getFileUpdates());
                                existingStats.setFileUpdates30Days(existingStats.getFileUpdates30Days() + stats.getFileUpdates30Days());
                                existingStats.setFileUpdates90Days(existingStats.getFileUpdates90Days() + stats.getFileUpdates90Days());
                            } else {
                                ContributorPerExtensionStats newStats = new ContributorPerExtensionStats(email);
                                updatesPerExtensionMap.put(extension, Pair.of(extension, newStats));
                                newStats.setFileUpdates(stats.getFileUpdates());
                                newStats.setFileUpdates30Days(stats.getFileUpdates30Days());
                                newStats.setFileUpdates90Days(stats.getFileUpdates90Days());
                            }
                        });
            });
        });

        List<Pair<String,ContributorPerExtensionStats>> extensionUpdates = new ArrayList<>(updatesPerExtensionMap.values());
        Collections.sort(extensionUpdates, (a, b) -> b.getRight().getFileUpdates90Days() - a.getRight().getFileUpdates90Days());
        return extensionUpdates;
    }

    public String getBiggestExtension(LandscapeConfiguration configuration, ContributorRepositories contributorRepositories) {
        List<Pair<String, ContributorPerExtensionStats>> contributorStatsPerExtension = this.getContributorStatsPerExtension(configuration, contributorRepositories);

        if (contributorStatsPerExtension.size() > 0) {
            return contributorStatsPerExtension.get(0).getLeft();
        }

        return null;
    }
}
