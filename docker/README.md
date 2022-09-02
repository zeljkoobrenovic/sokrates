
Below the documentation to build and run sokrates without installing Java on your host.

### Build

```shell
# Default build uses Java 17
docker build -f docker/Dockerfile -t sokrates:latest .

# To use Java 11
docker build -f docker/Dockerfile \
  --build-arg JAVA_VERSION=11 \
  -t sokrates:11-latest # name it as you want
  .
```

There is space for improvements :
 - build only CLI
 - keep building everything, but expose everything


### Run it

```shell
# Analyse your current directory (expected to contain a .git directory)
# Note: it will create a file `git-history.txt` and a directory `_sokrates`
docker run -v "${PWD}":/repo -it sokrates:latest extractGitHistory
docker run -v "${PWD}":/repo -it sokrates:latest init
docker run -v "${PWD}":/repo -it sokrates:latest generateReports

# Then you can look at the HTML report in the $PWD/_sokrates directory
```

### Github organization support

TODO
