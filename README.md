Texture Packer Plugin
=====================

An SBT plugin that automatically packages images using LibGdx's TexturePacker.

What it does
============

LibGdx's TexturePacker
----------------------

In OpenGL, a texture is bound, some drawing is done, another texture is bound, more drawing is done, etc. Binding the
texture is relatively expensive, so it is ideal to store many smaller images on a larger image, bind the larger texture
once, then draw portions of it many times. libgdx has a TexturePacker2 class which is a command line application that
packs many smaller images on to larger images. It stores the locations of the smaller images so they are easily
referenced by name in your application using the TextureAtlas class.

Also in some graphic cards it is required that textures have a width and height of a power of two (i.e. 128x256, 512x64,
1024x1024). It is a waste of time to keep track of all your images making sure they fulfill that requirement. The
TexturePacker packages images in that way by default, releasing us from that problem.

More information [here](http://code.google.com/p/libgdx/wiki/TexturePacker).

The plugin
----------

The plugin provides a task that runs the TexturePacker. That task uses sbt's built in capability of caching it's input
and output. That means the task remembers what input it was fed and the output it produced. Running it multiple times
without changing either will not trigger the TexturePacker process more than once.

It also hooks the task into the standard sbt `package` task. That means that every time the application
runs it will trigger the TexturePacker task and package images. That way every time the images change the application
content will be up to date. If nothing changed it will run immediately.

In the end you add, remove or modify your images as you please and run the application without worries. Everything will
always be there neatly packaged.

Read before using or risk losing files
======================================

Resources in a project are not only images. Text files, sounds, fonts, etc may be found as the resources. Thus some
files will be processed or derivatives and others will be the original. SBT expects all resources to be placed in a
specific folder. It is not a good idea to have everything mixed up. It is also a problem for version control systems to
not separate those files.

**WARNING**: The plugin assumes the final resources folder to be disposable (the one SBT will look for resources to package into
the jars). As such there is a hook on the `clean` task that will delete all the files in that folder. Make sure you move
your files out of that folder before using the plugin.

The input files to be processed are designated as such by being placed in the ''managedAssets'' folder. Sometimes
there are images that are used in different ways and we may not want to process them. Same applies to all other files
that are ignored by the TexturePacker. Such files that are input but are not meant to be processed should be placed in
the ''managedAssets'' folder.

The output folder is the `assets` folder and should match with a folder that SBT expects to find resources to be
packaged. Usually `src/main/resources`. That is the folder that will be subject to deletion.

These are the corresponding settings with their default values:

    unmanagedAssets := file("common/src/main/unmanaged")
    managedAssets := file("common/src/main/preprocess")
    assets := file("common/src/main/resources")

Notice that you may override them as you please. The default is based on the [ibgdx-sbt-project](https://github.com/ajhager/libgdx-sbt-project.g8)
template. That templates shows the best way to use LibGdx with SBT and Scala.

Ignoring the output folder
--------------------------

Remember to add this to your .gitignore or ignore file of your version controlling system.

    # Real resources should go into 'unmanaged'
    /common/src/main/resources/

Using the plugin
================

Project configuration
---------------------

First step is to add the plugin to the current project. Read the [SBT documentation on Plugins](http://www.scala-sbt.org/release/docs/Extending/Plugins#using-a-binary-sbt-plugin)
for more information.

The plugin is now hosted on Maven Central so there is no need to add a resolver.

On the `project/plugins.sbt` file add the following line:

    addSbtPlugin("com.starkengine" % "texture-packer-plugin" % "0.2")

The plugin is now part of the project but still won't do anything. Next step is to actually add it to the settings of
the project.

Assuming the `project/build.scala` looks something like this:

    import com.starkengine.TexturePackerPlugin

    ...

    lazy val common = Project("common", file("common")) settings(
        sharedSettings ++ TexturePackerPlugin.texturePackerSettings ++ Seq(
        ...
    ) :_*)

All you have to do is import TexturePackerPlugin into scope and then add the settings to the project that will have the
resources. Those settings include the paths to the special folders, the hook to automatically package resources, the
hook for the clean task and a task to update the version of LibGdx the plugin calls when using the TexturePacker.

Notice that you may override the folder paths settings like this:

    lazy val common = Project("common", file("common")) settings(
        sharedSettings ++ TexturePackerPlugin.texturePackerSettings ++ Seq(
          ...,
          TexturePackerPlugin.unmanagedAssets := file("my/way/better/path/for/unmanaged-assets"),
          TexturePackerPlugin.managedAssets := file("my/way/better/path/for/managed-assets"),
          TexturePackerPlugin.assets := file(""my/way/better/path/for/output")
    ) :_*)

First time running
------------------

The plugin actually needs the gdx.jar and the gdx-tools.jar to be able to run. Since none are on the official maven
repository it has to be done manually. There is a task that comes in the settings of the plugin that will be available
on the project you add those settings to. The task to be run from SBT is `update-texture-packer-gdx`.

What that task does it simply downloading the latest nightly build and unpackage those two jars into the ''project/lib''
folder so it can use it. Notice that it doesn't affect the gdx version the rest of your application uses. Those jars are
only used by the build project.

Remember it could could be done manually if you prefer. The download may take a while since the nightly builds package
is roughly 45MB. The plugin only uses two jars that amount to 3.3 MB.

Placing your resources
----------------------

The process that runs the TexturePacker command will fire it for each folder tree inside the `managedAssets` folder.
That means that each of those sub-trees will end up in a different texture atlas. So you must have a tree structure like
this:

    common/src/main/preprocess
    |-- gui
    |   |-- screen1
    |   |   `-- screen1.png
    |   `-- screen2
    |       `-- screen2.png
    |-- characters
    |   |-- mario.png
    |   `-- luigi.png
    `-- items
        |-- star.png
        `-- mushroom.png
    
That setup will produce an atlas for each: `gui.atlas`, `characters.atlas` and `items.atlas`. Do not place images laying around
in the root of `preprocess` as those files will be ignored. Files nested deep into these folders will still end up in the
mentioned atlases. To know how to use those atlases follow the usual LibGdx practices.

Ready
-----

Now the project is ready. Just run it or trigger `package` in your project and watch the packager go. Verify your
images and resources end up in the destination folder.
