package nl.obren.sokrates.sourcecode.landscape;

import nl.obren.sokrates.sourcecode.core.TagRule;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DefaultProjectTags {

    public List<ProjectTagGroup> defaultTagGroups() {
        ProjectTagGroup buildTags = new ProjectTagGroup("build and dependencies management tools", "#B9D9EB");
        ProjectTagGroup ciCdTags = new ProjectTagGroup("CI/CD tools", "#7CB9E8");
        ProjectTagGroup techTags = new ProjectTagGroup("interesting technologies", "#A3C1AD");

        List<ProjectTagGroup> groups = new ArrayList<>();
        groups.add(buildTags);
        groups.add(ciCdTags);
        groups.add(techTags);

        buildTags.getProjectTags().add(newPathPatternTagInstance("maven", Arrays.asList("(|.*/)pom[.]xml")));
        buildTags.getProjectTags().add(newPathPatternTagInstance("npm", Arrays.asList("(|.*/)package[.]json")));
        buildTags.getProjectTags().add(newPathPatternTagInstance("yarn", Arrays.asList("(|.*/)[.]yarnrc", "(|.*/)yarn[.]lock")));
        buildTags.getProjectTags().add(newPathPatternTagInstance("bable", Arrays.asList("(|.*/)[.]babel[.]config[.]json")));
        buildTags.getProjectTags().add(newPathPatternTagInstance("gradle", Arrays.asList("(|.*/)build[.]gradle")));
        buildTags.getProjectTags().add(newPathPatternTagInstance("sbt", Arrays.asList("(|.*/)build[.]sbt")));
        buildTags.getProjectTags().add(newPathPatternTagInstance("bazel", Arrays.asList("(|.*/)BUILD[.]bazel")));
        buildTags.getProjectTags().add(newPathPatternTagInstance("pip", Arrays.asList("(|.*/)pip[.]conf", "(|.*/)Pipfile")));
        buildTags.getProjectTags().add(newPathPatternTagInstance("nuget", Arrays.asList("(|.*/)[.]nuget/.*")));
        buildTags.getProjectTags().add(newPathPatternTagInstance("aws codebuild", Arrays.asList("(|.*/)buildspec[.]yml")));
        buildTags.getProjectTags().add(newPathPatternTagInstance("renovate", Arrays.asList("(|.*/)renovate[.]json")));
        buildTags.getProjectTags().add(newPathPatternTagInstance("gemfile", Arrays.asList("(|.*/)Gemfile")));
        buildTags.getProjectTags().add(newPathPatternTagInstance("Podfile", Arrays.asList("(|.*/)Podfile")));

        ciCdTags.getProjectTags().add(newPathPatternTagInstance("jenkins", Arrays.asList("(|.*/)Jenkinsfile")));
        ciCdTags.getProjectTags().add(newPathPatternTagInstance("travis", Arrays.asList("(|.*/)[.]travis[.]yml")));
        ciCdTags.getProjectTags().add(newPathPatternTagInstance("github actions", Arrays.asList("(|.*/)[.]github[/]workflows[/].*")));

        techTags.getProjectTags().add(newPathPatternTagInstance("docker", Arrays.asList("(|.*/)Dockerfile")));

        return groups;
    }

    private ProjectTag newPathPatternTagInstance(String name, List<String> patterns) {
        ProjectTag tag = new ProjectTag();

        tag.setTag(name);
        tag.setPathPatterns(patterns);

        return tag;
    }
}
