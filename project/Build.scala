import sbt._
import sbt.Keys._

object TexturePackerPluginBuild extends Build {
  val sharedSettings = Seq[Setting[_]](
    resolvers += "cloudbees snapshots" at "https://repository-belfry.forge.cloudbees.com/snapshot",
    credentials += {
      val credsFile = (Path.userHome / ".ivy2" / ".credentials")
      (if (credsFile.exists) Credentials(credsFile)
       else Credentials(file("/private/belfry/.credentials/.credentials")))
    }
  )

  lazy val root = Project("root", file(".")) settings(sharedSettings ++ Seq(
    name := "texture-packer-plugin",
    organization := "com.starkengine",
    sbtPlugin := true,
    parallelExecution in Test := false,
    updateLibgdxTask
  ) :_*)

  val updateLibgdx = TaskKey[Unit]("update-gdx", "Updates libgdx")

  val updateLibgdxTask = updateLibgdx <<= streams map { (s: TaskStreams) =>
    import Process._
    import java.io._
    import java.net.URL
    import java.util.regex.Pattern

    // Declare names
    val baseUrl = "http://libgdx.badlogicgames.com/nightlies"
    val gdxName = "libgdx-nightly-latest"

    // Fetch the file.
    s.log.info("Pulling %s" format(gdxName))
    s.log.warn("This may take a few minutes...")
    val zipName = "%s.zip" format(gdxName)
    val zipFile = new java.io.File(zipName)
    val url = new URL("%s/%s" format(baseUrl, zipName))
    IO.download(url, zipFile)

    val libsDest = file("lib")

    var extractedFiles = Set[File]()
    // Extract jars into their respective lib folders.
    s.log.info("Extracting common libs")
    val commonFilter = new ExactFilter("gdx.jar")
    extractedFiles ++= IO.unzip(zipFile, libsDest, commonFilter)

    s.log.info("Extracting extension libs")
    val extensionsFilter = new ExactFilter("extensions/gdx-tools/gdx-tools.jar")
    val extensions = IO.unzip(zipFile, libsDest, extensionsFilter)
    extractedFiles ++= extensions

    extensions.foreach(f => IO.move(f, libsDest / f.getName))
    (libsDest / "extensions/gdx-tools").delete
    (libsDest / "extensions").delete

    s.log.info("Extracted files: " + extractedFiles)

    // Destroy the file.
    zipFile.delete
    s.log.info("Update complete")
  }
}
