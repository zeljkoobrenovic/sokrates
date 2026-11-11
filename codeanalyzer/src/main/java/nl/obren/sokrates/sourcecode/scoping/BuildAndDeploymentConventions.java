package nl.obren.sokrates.sourcecode.scoping;

import java.util.List;

/**
 * The standard conventions for build and deployment files (see the linguist sources listed in {@link ScopingConventions}).
 * Moved out of ScopingConventions, which keeps the other categories and applies them all.
 */
class BuildAndDeploymentConventions {
    private BuildAndDeploymentConventions() {
    }

    static void addTo(List<Convention> buildAndDeploymentFilesConventions) {
        buildAndDeploymentFilesConventions.add(new Convention("(.*/)?[.](idea|vscode|vs|gradle|mvn|settings|metadata|circleci)/.*", "", "Hidden VCS/tool directories"));

        buildAndDeploymentFilesConventions.add(new Convention(".*/[.]cpplint[.]py", "", "Linter"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]bash_[a-z]+", "", "Bash files"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]editorconfig", "", "Editor configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]npmrc", "", "NPM Config"));

        buildAndDeploymentFilesConventions.add(new Convention(".*/pom[.]xml", "", "Maven configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]nuspec", "", "NuSpec configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/build[.]xml", "", "Build configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/assembly[.]xml", "", "Maven assembly plugin configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/assembly/src[.]xml", "", "Maven assembly plugin configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]gradle", "", "Gradle configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[-]gradle[.]js", "", "Gradle configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]sh", "", "Scripts"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]bat", "", "Scripts"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/AndroidManifest[.]xml", "", "Scripts"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/pnpm.*[.]json", "", "pnpm configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/pnpm.*[.]ya?ml", "", "pnpm configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/package[.]json", "", "npm configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/package[-]lock[.]json", "", "npm configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/glide[.]yml", "", "Glide configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/glide[.]yaml", "", "Glide configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/glide[.]lock", "", "Glide configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/docker[-]compose[.]yaml", "", "Docker configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/docker[-]compose[.]yml", "", "Docker configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]dockerfile", "", "Docker configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/Dockerfile", "", "Docker configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/Dockerfile[.][a-zA-Z0-9._-]+", "", "Docker configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]mk", "", "Mk files"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]cvsignore", "", "CVS configuration files"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]git[a-z]+", "", "Git configuration files"));
        buildAndDeploymentFilesConventions.add(new Convention(".*([.]|/)webpack([.]|/).*", "", "Webpack configuration files"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]csproj", "", "C# repository files"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]vbproj", "", "VB repository files"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/[.]gitignore", "", "Git ignore files"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/[.]gitattributes", "", "Git attributes"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/[.]gitconfig", "", "Git config"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/[.]gitmodules", "", "Git modules"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]manifest", "", "Manifest files"));

        buildAndDeploymentFilesConventions.add(new Convention(".*/sonatype-settings[.]xml", "", "Sonatype configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/config/checkstyle/.*", "", "Checkstyle configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/checkstyle[.]xml", "", "Checkstyle configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/checkstyle.*", "", "Checkstyle configuration"));

        // ignore lists
        buildAndDeploymentFilesConventions.add(new Convention(".*/[.]atomignore", "", "Ignore list"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/[.]babelignore", "", "Ignore list"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/[.]bzrignore", "", "Ignore list"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/[.]coffeelintignore", "", "Ignore list"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/[.]cvsignore", "", "Ignore list"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/[.]dockerignore", "", "Ignore list"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/[.]eslintignore", "", "Ignore list"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/[.]gitignore", "", "Ignore list"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/[.]nodemonignore", "", "Ignore list"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/[.]npmignore", "", "Ignore list"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/[.]prettierignore", "", "Ignore list"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/[.]stylelintignore", "", "Ignore list"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/[.]vscodeignore", "", "Ignore list"));


        // Root- and nested-level tool/config dotfiles. These are real build/tooling configuration
        // (not source, but worth keeping visible), so they are scoped here rather than ignored.
        // The "([.].*)?" suffix catches family variants in one rule, e.g. .eslintrc / .eslintrc.json
        // / .eslintrc.js, or .babelrc / .babelrc.js. ".env" is the deliberate exception: it is
        // ignored (see addIgnoreConventions) because it typically holds local secrets.
        // Package-manager / registry / version config
        buildAndDeploymentFilesConventions.add(new Convention("(.*/)?[.]npmrc", "", "npm configuration"));
        buildAndDeploymentFilesConventions.add(new Convention("(.*/)?[.]yarnrc([.].*)?", "", "Yarn configuration"));
        buildAndDeploymentFilesConventions.add(new Convention("(.*/)?[.]nvmrc", "", "Node version"));
        buildAndDeploymentFilesConventions.add(new Convention("(.*/)?[.]node[-]version", "", "Node version"));
        buildAndDeploymentFilesConventions.add(new Convention("(.*/)?[.]ruby[-]version", "", "Ruby version"));
        buildAndDeploymentFilesConventions.add(new Convention("(.*/)?[.]python[-]version", "", "Python version"));
        buildAndDeploymentFilesConventions.add(new Convention("(.*/)?[.]tool[-]versions", "", "asdf tool versions"));
        buildAndDeploymentFilesConventions.add(new Convention("(.*/)?[.]gemrc", "", "RubyGems configuration"));
        buildAndDeploymentFilesConventions.add(new Convention("(.*/)?[.]pypirc", "", "PyPI configuration"));
        buildAndDeploymentFilesConventions.add(new Convention("(.*/)?[.]netrc", "", "netrc configuration"));
        // Linters / formatters
        buildAndDeploymentFilesConventions.add(new Convention("(.*/)?[.]eslintrc([.].*)?", "", "ESLint configuration"));
        buildAndDeploymentFilesConventions.add(new Convention("(.*/)?[.]eslintignore", "", "ESLint ignore"));
        buildAndDeploymentFilesConventions.add(new Convention("(.*/)?[.]prettierrc([.].*)?", "", "Prettier configuration"));
        buildAndDeploymentFilesConventions.add(new Convention("(.*/)?[.]prettierignore", "", "Prettier ignore"));
        buildAndDeploymentFilesConventions.add(new Convention("(.*/)?[.]stylelintrc([.].*)?", "", "Stylelint configuration"));
        buildAndDeploymentFilesConventions.add(new Convention("(.*/)?[.]editorconfig", "", "EditorConfig"));
        buildAndDeploymentFilesConventions.add(new Convention("(.*/)?[.]flake8", "", "Flake8 configuration"));
        buildAndDeploymentFilesConventions.add(new Convention("(.*/)?[.]pylintrc", "", "Pylint configuration"));
        buildAndDeploymentFilesConventions.add(new Convention("(.*/)?[.]rubocop[.]yml", "", "RuboCop configuration"));
        buildAndDeploymentFilesConventions.add(new Convention("(.*/)?[.]scalafmt[.]conf", "", "Scalafmt configuration"));
        buildAndDeploymentFilesConventions.add(new Convention("(.*/)?[.]clang[-]format", "", "clang-format configuration"));
        buildAndDeploymentFilesConventions.add(new Convention("(.*/)?[.]jshintrc", "", "JSHint configuration"));
        // Git metadata files
        buildAndDeploymentFilesConventions.add(new Convention("(.*/)?[.]gitkeep", "", "Git keep"));
        buildAndDeploymentFilesConventions.add(new Convention("(.*/)?[.]mailmap", "", "Git mailmap"));
        buildAndDeploymentFilesConventions.add(new Convention("(.*/)?[.]git[-]blame[-]ignore[-]revs", "", "Git blame ignore revs"));
        // Build / ignore tool config
        buildAndDeploymentFilesConventions.add(new Convention("(.*/)?[.]babelrc([.].*)?", "", "Babel configuration"));
        buildAndDeploymentFilesConventions.add(new Convention("(.*/)?[.]browserslistrc", "", "Browserslist configuration"));
        buildAndDeploymentFilesConventions.add(new Convention("(.*/)?[.]nycrc([.].*)?", "", "nyc configuration"));
        buildAndDeploymentFilesConventions.add(new Convention("(.*/)?[.]mocharc([.].*)?", "", "Mocha configuration"));
        buildAndDeploymentFilesConventions.add(new Convention("(.*/)?[.]swcrc", "", "SWC configuration"));
        buildAndDeploymentFilesConventions.add(new Convention("(.*/)?[.]npmignore", "", "npm ignore"));
        buildAndDeploymentFilesConventions.add(new Convention("(.*/)?[.]dockerignore", "", "Docker ignore"));
        buildAndDeploymentFilesConventions.add(new Convention("(.*/)?[.]gcloudignore", "", "gcloud ignore"));
        buildAndDeploymentFilesConventions.add(new Convention("(.*/)?[.]terraformignore", "", "Terraform ignore"));
        buildAndDeploymentFilesConventions.add(new Convention("(.*/)?[.]helmignore", "", "Helm ignore"));

        buildAndDeploymentFilesConventions.add(new Convention(".*[.]mak", "", "Make files"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]make", "", "Make files"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]mk", "", "Make files"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]mkfile", "", "Make files"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]dotsettings", "", ".Net settings files"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/jenkins/.*[.]groovy", "", "Jenkins files"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/fastlane/.*[.]rb", "", "Fastlane files"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]podspec", "", "Podspec files"));


        buildAndDeploymentFilesConventions.add(new Convention(".*/Jenkinsfile", "", "Jenkinsfile"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/Jenkinsfile[.][a-zA-Z0-9]+", "", "Jenkinsfile"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/Makefile", "", "Makefile"));

        buildAndDeploymentFilesConventions.add(new Convention(".*/buildscripts/*", "", "Build scripts"));

        // CI pipelines
        buildAndDeploymentFilesConventions.add(new Convention(".*/[.]github/workflows/.*[.]ya?ml", "", "GitHub Actions workflow"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/[.]github/actions/.*[.]ya?ml", "", "GitHub Actions"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/action[.]ya?ml", "", "GitHub composite action"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/[.]gitlab[-]ci[.]yml", "", "GitLab CI configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/[.]circleci/.*[.]ya?ml", "", "CircleCI configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/[.]travis[.]yml", "", "Travis CI configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/azure[-]pipelines[.]ya?ml", "", "Azure Pipelines configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/bitbucket[-]pipelines[.]yml", "", "Bitbucket Pipelines configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/[.]drone[.]yml", "", "Drone CI configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/appveyor[.]yml", "", "AppVeyor configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/[.]teamcity/.*", "", "TeamCity configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/cloudbuild[.]ya?ml", "", "Google Cloud Build configuration"));

        // Infrastructure as code
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]tf", "", "Terraform configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]tfvars", "", "Terraform variables"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]bicep", "", "Bicep configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/serverless[.]ya?ml", "", "Serverless Framework configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/Procfile", "", "Procfile"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/skaffold[.]ya?ml", "", "Skaffold configuration"));

        buildAndDeploymentFilesConventions.add(new Convention(".*[.]hcl", "", "HashiCorp Configuration Language file"));
        // Helm charts
        buildAndDeploymentFilesConventions.add(new Convention(".*/Chart[.]ya?ml", "", "Helm chart"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/templates/.*[.]tpl", "", "Helm template"));
        // Ansible
        buildAndDeploymentFilesConventions.add(new Convention(".*/playbook[.]ya?ml", "", "Ansible playbook"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/ansible[.]cfg", "", "Ansible configuration"));

        // Build systems
        buildAndDeploymentFilesConventions.add(new Convention(".*/CMakeLists[.]txt", "", "CMake configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]cmake", "", "CMake configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/BUILD", "", "Bazel build file"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/BUILD[.]bazel", "", "Bazel build file"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/WORKSPACE", "", "Bazel workspace"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/WORKSPACE[.]bazel", "", "Bazel workspace"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]bzl", "", "Bazel/Starlark file"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]bazel", "", "Bazel file"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]sbt", "", "sbt configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/Rakefile", "", "Rakefile"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]rake", "", "Rake task"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/Gemfile", "", "Bundler configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]gemspec", "", "RubyGems specification"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/Cargo[.]toml", "", "Cargo configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/go[.]mod", "", "Go modules configuration"));
        // Python build / packaging
        buildAndDeploymentFilesConventions.add(new Convention(".*/setup[.]py", "", "Python setup script"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/setup[.]cfg", "", "Python setup configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/pyproject[.]toml", "", "Python project configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/requirements[a-zA-Z0-9._-]*[.]txt", "", "Python requirements"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/Pipfile", "", "Pipenv configuration"));
        // .NET
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]sln", "", ".NET solution file"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]fsproj", "", "F# project file"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]props", "", "MSBuild props file"));
        buildAndDeploymentFilesConventions.add(new Convention(".*[.]targets", "", "MSBuild targets file"));

        // Lock files (resolved dependency manifests)
        buildAndDeploymentFilesConventions.add(new Convention(".*/yarn[.]lock", "", "Yarn lock file"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/Cargo[.]lock", "", "Cargo lock file"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/Gemfile[.]lock", "", "Bundler lock file"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/poetry[.]lock", "", "Poetry lock file"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/Pipfile[.]lock", "", "Pipenv lock file"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/composer[.]lock", "", "Composer lock file"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/composer[.]json", "", "Composer configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/Podfile", "", "CocoaPods configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/Podfile[.]lock", "", "CocoaPods lock file"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/go[.]sum", "", "Go modules checksum file"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/Cartfile", "", "Carthage configuration"));
        buildAndDeploymentFilesConventions.add(new Convention(".*/Cartfile[.]resolved", "", "Carthage resolved file"));
    }
}
