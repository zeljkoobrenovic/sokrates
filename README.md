# Sokrates

Know your code! The unexamined code is not worth maintaining!

For details and examples visit the website [sokrates.dev](https://sokrates.dev).

* Sokrates is built by Željko Obrenović. It implements his "examined code" vision on how to approach understanding of complex source code bases, in a pragmatic and efficient way.
* Sokrates is a code spelunking tool, inspired by the grep, adding structure on top of regex source code searches.
* Sokrates generates a number of reports that can help you understand your code.
* Sokrates comes with both command line interface and interactive GUI code explorer.

### Prerequirements
* Java
* Maven

### Build

> mvn clean install

The build will create two jar files:
* the command line interface in the cli/target folder
* the interactive explorer in the codeexplorer/target folder

### Docker

Build the docker image:
> docker build -t sokrates .

Run init command:
> docker run -v "$(pwd):/code" -w /code sokrates init

## Kitnetic wrapper

This fork of the Sokrates repository has been modified to include a simple Python script developed by [Kitnetic](https://kitnetic.co.uk)
. The associated Docker container has been updated to execute this script instead of invoking the tool directly, simplifying the process of running the Sokrates scanner across multiple projects within a single workflow.

Please see the README.md in `./kitnetic-wrapper` for more  details