import sbt._
import sbt.Keys._
import eu.diversit.sbt.plugin.WebDavPlugin._

object TexturePackerPluginBuild extends Build {
  lazy val publishSettings = WebDav.scopedSettings ++ Seq[Project.Setting[_]](
    version := "0.2",
    publishMavenStyle := true,
    publishArtifact in Test := false,
    publishTo <<= version {
      (v: String) =>
        val nexus = "https://oss.sonatype.org/"
        if (v.trim.endsWith("SNAPSHOT"))
          Some("snapshots" at nexus + "content/repositories/snapshots")
        else
          Some("releases" at nexus + "service/local/staging/deploy/maven2")
    },
    credentials += {
      val credsFile = (Path.userHome / ".credentials")
      (if (credsFile.exists) Credentials(credsFile)
      else Credentials(file("/private/belfry/.credentials/.credentials")))
    },
    pomIncludeRepository := {
      _ => false
    },
    pomExtra := (
      <url>https://github.com/tlazaro/texture-packer-plugin</url>
        <licenses>
          <license>
            <name>BSD-style</name>
            <url>http://www.opensource.org/licenses/bsd-license.php</url>
            <distribution>repo</distribution>
          </license>
        </licenses>
        <scm>
          <url>git@github.com:tlazaro/texture-packer-plugin.git</url>
          <connection>scm:git@github.com:tlazaro/texture-packer-plugin.git</connection>
        </scm>
        <developers>
          <developer>
            <id>tlazaro</id>
            <name>Tomas Lazaro</name>
            <url>https://github.com/tlazaro</url>
          </developer>
        </developers>)
  )

  lazy val root = Project("root", file(".")) settings (publishSettings ++ Seq(
    name := "texture-packer-plugin",
    organization := "com.starkengine",
    sbtPlugin := true,
    parallelExecution in Test := false,
    updateLibgdxTask
  ): _*)

  val updateLibgdx = TaskKey[Unit]("update-gdx", "Updates libgdx")

  val updateLibgdxTask = updateLibgdx <<= streams map {
    (s: TaskStreams) =>
      import Process._
      import java.io._
      import java.net.URL
      import java.util.regex.Pattern

      // Declare names
      val baseUrl = "http://libgdx.badlogicgames.com/nightlies"
      val gdxName = "libgdx-nightly-latest"

      // Fetch the file.
      s.log.info("Pulling %s" format (gdxName))
      s.log.warn("This may take a few minutes...")
      val zipName = "%s.zip" format (gdxName)
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
