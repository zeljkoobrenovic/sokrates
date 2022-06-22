package nl.obren.sokrates.sourcecode.landscape;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DefaultTags {

    public List<TagGroup> defaultTagGroups() {
        TagGroup buildTags = new TagGroup("build and dependencies management tools", "#B9D9EB");
        TagGroup ciCdTags = new TagGroup("CI/CD tools", "#7CB9E8");
        TagGroup techTags = new TagGroup("interesting technologies", "#A3C1AD");

        List<TagGroup> groups = new ArrayList<>();
        groups.add(buildTags);
        groups.add(ciCdTags);
        groups.add(techTags);

        buildTags.getRepositoryTags().add(newPathPatternTagInstance("maven", "general/maven", Arrays.asList("(|.*/)pom[.]xml")));
        buildTags.getRepositoryTags().add(newPathPatternTagInstance("npm", "general/npm", Arrays.asList("(|.*/)package[.]json")));
        buildTags.getRepositoryTags().add(newPathPatternTagInstance("yarn", "general/yarn", Arrays.asList("(|.*/)[.]yarnrc", "(|.*/)yarn[.]lock")));
        buildTags.getRepositoryTags().add(newPathPatternTagInstance("babel", "general/babel", Arrays.asList("(|.*/)[.]babel[.]config[.]json")));
        buildTags.getRepositoryTags().add(newPathPatternTagInstance("gradle", "general/gradle", Arrays.asList("(|.*/)build[.]gradle")));
        buildTags.getRepositoryTags().add(newPathPatternTagInstance("sbt", "general/sbt", Arrays.asList("(|.*/)build[.]sbt")));
        buildTags.getRepositoryTags().add(newPathPatternTagInstance("bazel", "general/bazel", Arrays.asList("(|.*/)BUILD[.]bazel")));
        buildTags.getRepositoryTags().add(newPathPatternTagInstance("pip", "general/python", Arrays.asList("(|.*/)pip[.]conf", "(|.*/)Pipfile")));
        buildTags.getRepositoryTags().add(newPathPatternTagInstance("nuget", "general/nuget", Arrays.asList("(|.*/)[.]nuget/.*")));
        buildTags.getRepositoryTags().add(newPathPatternTagInstance("aws codebuild", "aws/AWS-CodeBuild", Arrays.asList("(|.*/)buildspec[.]yml")));
        buildTags.getRepositoryTags().add(newPathPatternTagInstance("renovate", "general/renovate", Arrays.asList("(|.*/)renovate[.]json5?")));
        buildTags.getRepositoryTags().add(newPathPatternTagInstance("gemfile", "lang/ruby", Arrays.asList("(|.*/)Gemfile")));
        buildTags.getRepositoryTags().add(newPathPatternTagInstance("podfile", "general/cocoa-pods", Arrays.asList("(|.*/)Podfile")));
        buildTags.getRepositoryTags().add(newPathPatternTagInstance("make", "general/gnu", Arrays.asList("(|.*/)Makefile")));

        ciCdTags.getRepositoryTags().add(newPathPatternTagInstance("jenkins", "general/jenkins", Arrays.asList("(|.*/)Jenkinsfile", "(|.*/)Jenkinsfile[.][a-z0-9_]+")));
        ciCdTags.getRepositoryTags().add(newPathPatternTagInstance("travis", "general/travis", Arrays.asList("(|.*/)[.]travis[.]ya?ml")));
        ciCdTags.getRepositoryTags().add(newPathPatternTagInstance("github actions", "general/github-actions", Arrays.asList("(|.*/)[.]github[/]workflows[/].*")));

        techTags.getRepositoryTags().add(newPathPatternTagInstance("docker", "general/docker", Arrays.asList("(|.*/)Dockerfile")));
        techTags.getRepositoryTags().add(newPathPatternTagInstance("helm", "general/helm", Arrays.asList("(|.*/)helmfile[.]ya?ml")));

        return groups;
    }

    private RepositoryTag newPathPatternTagInstance(String name, String logo, List<String> patterns) {
        RepositoryTag tag = new RepositoryTag();

        tag.setTag(name);
        tag.setImageLink("https://zeljkoobrenovic.github.io/sokrates-media/" + logo + ".png");
        tag.setPathPatterns(patterns);

        return tag;
    }
}
