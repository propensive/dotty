package dotty.dokka

import dotty.tools.dotc.core.Contexts._
import dotty.tools.dotc.reporting.Diagnostic
import dotty.tools.dotc.reporting.ConsoleReporter
import dotty.tools.dotc.interfaces.Diagnostic.{ERROR, INFO, WARNING}
import dotty.dokka.test.BuildInfo
import org.junit.Assert._
import java.io.File


case class ReportedDiagnostics(errors: List[Diagnostic], warnings: List[Diagnostic], infos: List[Diagnostic]):
  def errorMsgs = errors.map(_.msg.rawMessage)
  def warningMsgs = warnings.map(_.msg.rawMessage)
  def infoMsgs = infos.map(_.msg.rawMessage)


extension (c: CompilerContext) def reportedDiagnostics: ReportedDiagnostics =
  val t = c.reporter.asInstanceOf[TestReporter]
  ReportedDiagnostics(t.errors.result, t.warnings.result, t.infos.result)

def assertNoWarning(diag: ReportedDiagnostics) = assertEquals("Warnings should be empty", Nil, diag.warningMsgs)
def assertNoErrors(diag: ReportedDiagnostics) = assertEquals("Erros should be empty", Nil, diag.errorMsgs)
def assertNoInfos(diag: ReportedDiagnostics) = assertEquals("Infos should be empty", Nil, diag.infoMsgs)

def assertMessagesAbout(messages: Seq[String])(patterns: String*) =
  patterns.foldLeft(messages){ (toCheck, pattern) =>
    val (matching, rest) = toCheck.partition(_.contains(pattern))
    assertTrue(
      s"Unable to find messages matching `$pattern`in $toCheck" +
        " (not that some methods may be filtered out by previous patterns",
      matching.nonEmpty
    )
    rest
  }

class TestReporter extends ConsoleReporter:
  val errors = List.newBuilder[Diagnostic]
  val warnings = List.newBuilder[Diagnostic]
  val infos = List.newBuilder[Diagnostic]


  override def doReport(dia: Diagnostic)(using Context): Unit = dia.level match
    case INFO =>
      infos += dia
    case ERROR =>
      errors += dia
      super.doReport(dia)
    case WARNING =>
      warnings += dia
      super.doReport(dia)

def testArgs(files: Seq[File] = Nil, dest: File = new File("notUsed")) = Scala3doc.Args(
          name = "Test Project Name",
          output = dest,
          tastyFiles = files
        )

def testContext = (new ContextBase).initialCtx.fresh.setReporter(new TestReporter)

def testDocContext = DocContext(testArgs(), testContext)

def tastyFiles(name: String) =
  def listFilesSafe(dir: File) = Option(dir.listFiles).getOrElse {
    throw AssertionError(s"$dir not found. The test name is incorrect or scala3doc-testcases were not recompiled.")
  }
  def collectFiles(dir: File): List[File] = listFilesSafe(dir).toList.flatMap {
      case f if f.isDirectory => collectFiles(f)
      case f if f.getName endsWith ".tasty" => f :: Nil
      case _ => Nil
    }
  collectFiles(File(s"${BuildInfo.test_testcasesOutputDir}/tests/$name"))