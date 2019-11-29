package nl.obren.sokrates.sourcecode.lang;

import nl.obren.sokrates.common.utils.ProgressFeedback;
import nl.obren.sokrates.sourcecode.SourceFile;
import nl.obren.sokrates.sourcecode.aspects.NamedSourceCodeAspect;
import nl.obren.sokrates.sourcecode.cleaners.CleanedContent;
import nl.obren.sokrates.sourcecode.dependencies.Dependency;
import nl.obren.sokrates.sourcecode.lang.scala.ScalaAnalyzer;
import nl.obren.sokrates.sourcecode.units.UnitInfo;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class ScalaAnalyzerTest {
    @Test
    public void cleanForLinesOfCodeCalculations() throws Exception {
        ScalaAnalyzer analyzer = new ScalaAnalyzer();
        String code = "/** Factory for [[mypackage.Person]] instances. */\n" +
                "object Person {\n" +
                "  /** Creates a person with a given name and age.\n" +
                "   * \n" +
                "   *  @param name their name\n" +
                "   *  @param age the age of the person to create \n" +
                "   */\n" +
                "  def apply(name: String, age: Int) = {}\n" +
                "  // Creates a person with a given name and birthdate\n" +
                "   // \n" +
                "   //  @param name their name\n" +
                "   //  @param birthDate the person's birthdate\n" +
                "   //  @return a new Person instance with the age determined by the \n" +
                "   //          birthdate and current date. \n" +
                "   //\n" +
                "  def apply(name: String, birthDate: java.util.Date) = {}\n" +
                "}";
        assertEquals(analyzer.cleanForLinesOfCodeCalculations(new SourceFile(new File("dummy.scala"), code)).getCleanedContent(), "object Person {\n" +
                "  def apply(name: String, age: Int) = {}\n" +
                "  def apply(name: String, birthDate: java.util.Date) = {}\n" +
                "}");
    }

    @Test
    public void cleanForDuplicationCalculations() throws Exception {
        ScalaAnalyzer analyzer = new ScalaAnalyzer();
        String code = "/** An example class. */\n" +
                "import java.util.{Date, Locale}\n" +
                "import java.text.DateFormat\n" +
                "import java.text.DateFormat._\n" +
                " // start object def\n" +
                "object FrenchDate {\n" +
                "  def main(args: Array[String]) {\n" +
                "    val now = new Date\n" +
                "    val df = getDateInstance(LONG, Locale.FRANCE)\n" +
                "    println(df format now)\n" +
                "  }\n" +
                "}";
        CleanedContent cleanedContent = analyzer.cleanForDuplicationCalculations(new SourceFile(new File("dummy.scala"),
                code));
        assertEquals(cleanedContent.getCleanedContent(), "object FrenchDate {\n" +
                "def main(args: Array[String]) {\n" +
                "val now = new Date\n" +
                "val df = getDateInstance(LONG, Locale.FRANCE)\n" +
                "println(df format now)");
        assertEquals(cleanedContent.getCleanedLinesCount(), 5);
        assertEquals(cleanedContent.getFileLineIndexes().size(), 5);
        assertEquals(cleanedContent.getFileLineIndexes().get(0).intValue(), 5);
        assertEquals(cleanedContent.getFileLineIndexes().get(1).intValue(), 6);
        assertEquals(cleanedContent.getFileLineIndexes().get(2).intValue(), 7);
        assertEquals(cleanedContent.getFileLineIndexes().get(3).intValue(), 8);
        assertEquals(cleanedContent.getFileLineIndexes().get(4).intValue(), 9);

    }

    @Test
    public void extractUnits1() throws Exception {
        ScalaAnalyzer analyzer = new ScalaAnalyzer();
        String code = "/** An example class. */\n" +
                "import java.util.{Date, Locale}\n" +
                "import java.text.DateFormat\n" +
                "import java.text.DateFormat._\n" +
                " // start object def\n" +
                "object FrenchDate {\n" +
                "  def main(args: Array[String]) {\n" +
                "    val now = new Date\n" +
                "    val df = getDateInstance(LONG, Locale.FRANCE)\n" +
                " // print\n" +
                "    if (true) println(df format now)\n" +
                "  }\n" +
                "}";
        List<UnitInfo> units = analyzer.extractUnits(new SourceFile(new File("dummy.scala"), code));
        assertEquals(units.size(), 1);
        assertEquals(units.get(0).getShortName(), "def main()");
        assertEquals(units.get(0).getLinesOfCode(), 5);
        assertEquals(units.get(0).getMcCabeIndex(), 2);
        assertEquals(units.get(0).getNumberOfParameters(), 1);
    }

    @Test
    public void extractUnits2() throws Exception {
        ScalaAnalyzer analyzer = new ScalaAnalyzer();
        String code = "/** An example class. */\n" +
                "import java.util.{Date, Locale}\n" +
                "import java.text.DateFormat\n" +
                "import java.text.DateFormat._\n" +
                " // start object def\n" +
                "object FrenchDate {\n" +
                "  def main(args: Array[String]) {\n" +
                "    // right!\n" +
                "    if (foo) bar else baz\n" +
                "    for (i <- 0 to 10) { ... }\n" +
                "    while (true) { println(\"Hello, World!\") }\n" +
                "    // wrong!\n" +
                "    if(foo) bar else baz\n" +
                "    for(i <- 0 to 10) { ... }\n" +
                "    while(true) { println(\"Hello, World!\") }" +
                "  }\n" +
                "}";
        List<UnitInfo> units = analyzer.extractUnits(new SourceFile(new File("dummy.scala"), code));
        assertEquals(units.size(), 1);
        assertEquals(units.get(0).getShortName(), "def main()");
        assertEquals(units.get(0).getLinesOfCode(), 7);
        assertEquals(units.get(0).getMcCabeIndex(), 7);
        assertEquals(units.get(0).getNumberOfParameters(), 1);
    }

    @Test
    public void extractDependencies() throws Exception {
        ScalaAnalyzer analyzer = new ScalaAnalyzer();
        String code1 = "/** An example class. */\n" +
                "package a\n" +
                "import java.util.{Date, Locale}\n" +
                "import java.text.DateFormat\n" +
                "import java.text.DateFormat._\n" +
                "import b\n" +
                " // start object def\n" +
                "object FrenchDate {\n" +
                "  def main(args: Array[String]) {\n" +
                "  }\n" +
                "}";
        String code2 = "/** An example class. */\n" +
                "package b\n" +
                "import java.util.{Date, Locale}\n" +
                "import java.text.DateFormat\n" +
                "import java.text.DateFormat._\n" +
                " // start object def\n" +
                "object FrenchDate {\n" +
                "  def main(args: Array[String]) {\n" +
                "  }\n" +
                "}";
        SourceFile sourceFile1 = new SourceFile(new File("dummy.scala"), code1);
        sourceFile1.getLogicalComponents().add(new NamedSourceCodeAspect("CompA"));
        SourceFile sourceFile2 = new SourceFile(new File("dummy.scala"), code2);
        sourceFile2.getLogicalComponents().add(new NamedSourceCodeAspect("CompB"));
        List<Dependency> dependencies = analyzer.extractDependencies(Arrays.asList(sourceFile1, sourceFile2), new ProgressFeedback()).getDependencies();
        assertEquals(dependencies.size(), 1);
        assertEquals(dependencies.get(0).getDependencyString(), "a -> b");
        assertEquals(dependencies.get(0).getComponentDependency(""), "CompA -> CompB");
    }

}
