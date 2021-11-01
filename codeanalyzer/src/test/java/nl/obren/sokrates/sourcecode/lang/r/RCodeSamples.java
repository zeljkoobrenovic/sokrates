/*
 * Copyright (c) 2021 Željko Obrenović. All rights reserved.
 */

package nl.obren.sokrates.sourcecode.lang.r;

public class RCodeSamples {
    protected final static String EXAMPLE_1 = "# function example - get measures of central tendency\n" +
            "# and spread for a numeric vector x. The user has a\n" +
            "# choice of measures and whether the results are printed.\n" +
            "\n" +
            "mysummary <- function(x,npar=TRUE,print=TRUE) {\n" +
            "  if (!npar) {\n" +
            "    center <- mean(x); spread <- sd(x)\n" +
            "  } else {\n" +
            "    center <- median(x); spread <- mad(x)\n" +
            "  }\n" +
            "  if (print & !npar) {\n" +
            "    cat(\"Mean=\", center, \"\\n\", \"SD=\", spread, \"\\n\")\n" +
            "  } else if (print & npar) {\n" +
            "    cat(\"Median=\", center, \"\\n\", \"MAD=\", spread, \"\\n\")\n" +
            "  }\n" +
            "  result <- list(center=center,spread=spread)\n" +
            "  return(result)\n" +
            "}\n" +
            "\n" +
            "# invoking the function\n" +
            "set.seed(1234)\n" +
            "x <- rpois(500, 4)\n" +
            "y <- mysummary(x)\n" +
            "Median= 4\n" +
            "MAD= 1.4826\n" +
            "# y$center is the median (4)\n" +
            "# y$spread is the median absolute deviation (1.4826)\n" +
            "\n" +
            "y <- mysummary(x, npar=FALSE, print=FALSE)\n" +
            "# no output\n" +
            "# y$center is the mean (4.052)\n" +
            "# y$spread is the standard deviation (2.01927)";

    protected final static String EXAMPLE_1_CLEANED = "mysummary <- function(x,npar=TRUE,print=TRUE) {\n" +
            "  if (!npar) {\n" +
            "    center <- mean(x); spread <- sd(x)\n" +
            "  } else {\n" +
            "    center <- median(x); spread <- mad(x)\n" +
            "  }\n" +
            "  if (print & !npar) {\n" +
            "    cat(\"Mean=\", center, \"\\n\", \"SD=\", spread, \"\\n\")\n" +
            "  } else if (print & npar) {\n" +
            "    cat(\"Median=\", center, \"\\n\", \"MAD=\", spread, \"\\n\")\n" +
            "  }\n" +
            "  result <- list(center=center,spread=spread)\n" +
            "  return(result)\n" +
            "}\n" +
            "set.seed(1234)\n" +
            "x <- rpois(500, 4)\n" +
            "y <- mysummary(x)\n" +
            "Median= 4\n" +
            "MAD= 1.4826\n" +
            "y <- mysummary(x, npar=FALSE, print=FALSE)";

    protected final static String EXAMPLE_1_CLEANED_FOR_DUPLICATION = "mysummary <- function(x,npar=TRUE,print=TRUE) {\n" +
            "if (!npar) {\n" +
            "center <- mean(x); spread <- sd(x)\n" +
            "} else {\n" +
            "center <- median(x); spread <- mad(x)\n" +
            "}\n" +
            "if (print & !npar) {\n" +
            "cat(\"Mean=\", center, \"\\n\", \"SD=\", spread, \"\\n\")\n" +
            "} else if (print & npar) {\n" +
            "cat(\"Median=\", center, \"\\n\", \"MAD=\", spread, \"\\n\")\n" +
            "}\n" +
            "result <- list(center=center,spread=spread)\n" +
            "return(result)\n" +
            "}\n" +
            "set.seed(1234)\n" +
            "x <- rpois(500, 4)\n" +
            "y <- mysummary(x)\n" +
            "Median= 4\n" +
            "MAD= 1.4826\n" +
            "y <- mysummary(x, npar=FALSE, print=FALSE)";

    protected final static String EXAMPLE_1_UNIT_CLEANED_BODY = "mysummary <- function(x,npar=TRUE,print=TRUE) {\n" +
            "  if (!npar) {\n" +
            "    center <- mean(x); spread <- sd(x)\n" +
            "  } else {\n" +
            "    center <- median(x); spread <- mad(x)\n" +
            "  }\n" +
            "  if (print & !npar) {\n" +
            "    cat(\"\", center, \"\", \"\", spread, \"\")\n" +
            "  } else if (print & npar) {\n" +
            "    cat(\"\", center, \"\", \"\", spread, \"\")\n" +
            "  }\n" +
            "  result <- list(center=center,spread=spread)\n" +
            "  return(result)\n" +
            "}\n";
}
