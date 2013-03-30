package com.starkengine

import sbt._
import sbt.Keys._

object TexturePackerPlugin extends Plugin {
  val unmanagedAssets = SettingKey[File]("unmanaged-assets")
  val managedAssets = SettingKey[File]("managed-assets")
  val assets = SettingKey[File]("assets")

  private val FilePattern = """(.*)\.(.*)$""".r
  val ExcludedExtensions = Set("png", "jpg", "jpeg")
  val ExcludedFiles = Set("pack.json")
  val AcceptedResources = new SimpleFileFilter(file => !ExcludedFiles.contains(file.name) && (file.name match {
    case FilePattern(name, ext) => !ExcludedExtensions.contains(ext)
    case _ => false // Not a file?
  }))

  def recursiveListFiles(f: File): Array[File] = {
    val these = f.listFiles
    if (these == null) Array()
    else these ++ these.filter(_.isDirectory).flatMap(recursiveListFiles)
  }

  /* Unmanaged Resources */
  val processUnmanagedAssets = TaskKey[Unit]("process-unmanaged-assets", "Copies unmanaged assets to resources folder.")

  val processUnmanagedAssetsTask = processUnmanagedAssets <<= (assets, unmanagedAssets) map {
    (resources, unmanaged) =>
      val cacheDir = file("target/texture-packer-cache") /* Store cache information here */

      /* Get all files in the folder recursively */
      val files = recursiveListFiles(unmanaged).toSet

      /* Wrap actual function in a function that provides basic caching functionalities. */
      val cachedFun = FileFunction.cached(cacheDir / unmanaged.name, FilesInfo.lastModified, FilesInfo.exists) {
        (inFiles: Set[File]) => {
          println("At least one of the files in %s have changed. Copying content to %s.".format(unmanaged, resources))
          IO.copyDirectory(unmanaged, resources)
          inFiles.flatMap(Path.rebase(unmanaged, resources).andThen(_.toList))
        }
      }

      /* Call wrapped function with files in the current folder */
      cachedFun(files)
  }

  /* Texture Packer */
  val processManagedAssets = TaskKey[Unit]("process-managed-assets", "Runs libgdx's Texture Packer 2 on the managed-assets folder")
  val processManagedAssetsTask = processManagedAssets <<= (assets, managedAssets) map {
    (outputDir, inputDir) =>
      val cacheDir = file("target/texture-packer-cache") /* Store cache information here */

      /* Get all direct subdirectories of inputDir */
      val folders = inputDir.asFile.listFiles.filter(_.isDirectory)

      folders.foreach {
        folder =>
        /* Get all files in the folder recursively */
          val files = recursiveListFiles(folder).toSet

          /* Wrap actual function in a function that provides basic caching functionalities. */
          val cachedFun = FileFunction.cached(cacheDir / folder.name, FilesInfo.lastModified, FilesInfo.exists) {
            (inFiles: Set[File]) => runTexturePacker(folder, outputDir)
          }

          /* Call wrapped function with files in the current folder */
          cachedFun(files)
      }
  }

  /** Runs the texture packer on the given folder. */
  val runTexturePacker = (subfolder: File, outFolder: File) => {
    println("At least one of the files in %s have changed. Outputting TexturePacker result into %s.".format(subfolder, outFolder))

    // Run Texture Packer
    val args = Array(subfolder.toString, outFolder.toString, subfolder.getName)
    com.badlogic.gdx.tools.imagepacker.TexturePacker2.main(args)

    Set[File]()
  }

  val updateLibgdx = TaskKey[Unit]("update-texture-packer-gdx", "Updates libgdx for the Texture Packer")

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

      val libsDest = file("project/lib")

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

  val texturePackerSettings: Seq[Project.Setting[_]] = Seq(
    unmanagedAssets := file("common/src/main/unmanaged"),
    managedAssets := file("common/src/main/preprocess"),
    assets := file("common/src/main/resources"),
    processManagedAssetsTask,
    processUnmanagedAssetsTask,
    updateLibgdxTask,
    cleanFiles <+= assets { x => x },
    packageBin in Compile <<= packageBin in Compile dependsOn(processManagedAssets, processUnmanagedAssets)
  )
}
