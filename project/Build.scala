import sbt._
import sbt.Keys._

//import eu.diversit.sbt.plugin.WebDavPlugin._

object TexturePackerPluginBuild extends Build {

  val gdxVersion = "1.5.5"

  lazy val root = Project("root", file(".")).settings(/*WebDav.scopedSettings ++*/ Seq(
    name := "texture-packer-plugin",
    organization := "com.starkengine",
    version := "0.3",
    sbtPlugin := true,
    parallelExecution in Test := false,
    libraryDependencies := Seq(
      "com.badlogicgames.gdx" % "gdx" % gdxVersion,
      "com.badlogicgames.gdx" % "gdx-tools" % gdxVersion
    ),
    publishMavenStyle := true,
    publishArtifact in Test := false,
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value) Some("snapshots" at nexus + "content/repositories/snapshots")
      else Some("releases" at nexus + "service/local/staging/deploy/maven2")
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

  ): _*)

}
