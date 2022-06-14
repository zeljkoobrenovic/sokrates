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

        buildTags.getProjectTags().add(newPathPatternTagInstance("maven", "general/maven", Arrays.asList("(|.*/)pom[.]xml")));
        buildTags.getProjectTags().add(newPathPatternTagInstance("npm", "general/npm", Arrays.asList("(|.*/)package[.]json")));
        buildTags.getProjectTags().add(newPathPatternTagInstance("yarn", "general/yarn", Arrays.asList("(|.*/)[.]yarnrc", "(|.*/)yarn[.]lock")));
        buildTags.getProjectTags().add(newPathPatternTagInstance("babel", "general/babel", Arrays.asList("(|.*/)[.]babel[.]config[.]json")));
        buildTags.getProjectTags().add(newPathPatternTagInstance("gradle", "general/gradle", Arrays.asList("(|.*/)build[.]gradle")));
        buildTags.getProjectTags().add(newPathPatternTagInstance("sbt", "general/sbt", Arrays.asList("(|.*/)build[.]sbt")));
        buildTags.getProjectTags().add(newPathPatternTagInstance("bazel", "general/bazel", Arrays.asList("(|.*/)BUILD[.]bazel")));
        buildTags.getProjectTags().add(newPathPatternTagInstance("pip", "general/python", Arrays.asList("(|.*/)pip[.]conf", "(|.*/)Pipfile")));
        buildTags.getProjectTags().add(newPathPatternTagInstance("nuget", "general/nuget", Arrays.asList("(|.*/)[.]nuget/.*")));
        buildTags.getProjectTags().add(newPathPatternTagInstance("aws codebuild", "aws/AWS-CodeBuild", Arrays.asList("(|.*/)buildspec[.]yml")));
        buildTags.getProjectTags().add(newPathPatternTagInstance("renovate", "general/renovate", Arrays.asList("(|.*/)renovate[.]json5?")));
        buildTags.getProjectTags().add(newPathPatternTagInstance("gemfile", "lang/ruby", Arrays.asList("(|.*/)Gemfile")));
        buildTags.getProjectTags().add(newPathPatternTagInstance("podfile", "general/cocoa-pods", Arrays.asList("(|.*/)Podfile")));
        buildTags.getProjectTags().add(newPathPatternTagInstance("make", "general/gnu", Arrays.asList("(|.*/)Makefile")));

        ciCdTags.getProjectTags().add(newPathPatternTagInstance("jenkins", "general/jenkins", Arrays.asList("(|.*/)Jenkinsfile")));
        ciCdTags.getProjectTags().add(newPathPatternTagInstance("travis", "general/travis", Arrays.asList("(|.*/)[.]travis[.]ya?ml")));
        ciCdTags.getProjectTags().add(newPathPatternTagInstance("github actions", "general/github-actions", Arrays.asList("(|.*/)[.]github[/]workflows[/].*")));

        techTags.getProjectTags().add(newPathPatternTagInstance("docker", "general/docker", Arrays.asList("(|.*/)Dockerfile")));
        techTags.getProjectTags().add(newPathPatternTagInstance("helm", "general/helm", Arrays.asList("(|.*/)helmfile[.]ya?ml")));

        return groups;
    }

    private ProjectTag newPathPatternTagInstance(String name, String logo, List<String> patterns) {
        ProjectTag tag = new ProjectTag();

        tag.setTag(name);
        tag.setImageLink("https://zeljkoobrenovic.github.io/sokrates-media/" + logo + ".png");
        tag.setPathPatterns(patterns);

        return tag;
    }
}
