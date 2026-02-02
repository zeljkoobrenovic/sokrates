# Sokrates

Know your code! The unexamined code is not worth maintaining!

For details and examples visit the website [sokrates.dev](https://sokrates.dev).

* Sokrates is built by Željko Obrenović. It implements his "examined code" vision on how to approach understanding of complex source code bases, in a pragmatic and efficient way.
* Sokrates is ate two jar files:
* the command line interface in the cli/target folder
* the interactive explorer in the codeexplorer/target folder

## Kitnetic wrapper

This fork of the Sokrates repository has been modified to include a simple Python script developed by [Kitnetic](https://kitnetic.co.uk)
. The associated Docker container has been updated to execute this script instead of invoking the tool directly, simplifying the process of running the Sokrates scanner across multiple projects within a single workflow.

Please see the README.md in `./kitnetic-wrapper` for more  details