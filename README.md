# NOTE
This is a (hopefully temporary) Windows port of the sokrates tool created by Željko Obrenović.

# Sokrates

Know your code! The unexamined code is not worth maintaining!

For details and examples visit the website [sokrat.org](https://sokrates.org).

* Sokrates is built by Željko Obrenović. It implements his "examined code" vision on how to approach understanding of complex source code bases, in a pragmatic and efficient way.
* Sokrates is a code spelunking tool, inspired by the grep, adding structure on top of regex source code searches.
* Sokrates generates a number of reports that can help you understand your code.
* Sokrates comes with both command line interface and interactive GUI code explorer.

### Prerequirements
* Java 8+
* Maven

### Build

> mvn clean install

The build will create two jar files:
* the command line interface in the cli/target folder
* the interactive explorer in the codeexplorer/target folder
