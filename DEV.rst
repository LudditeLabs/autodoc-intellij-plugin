Development notes
=================

This document describes various development aspects.

Release check list
------------------

Things to check before releasing new plugin version.

Create new project with python files and few non-python files
(for example, text or markdown or java) before testing.

Your OS must be one of the x64 Linux, Windows 7+ or MacOS Mavericks+.

Test main features
~~~~~~~~~~~~~~~~~~

* Remove existing plugin if already installed and install new one.
  It's a first test: it must propose to install platform bundle for your OS.
  Click on 'Click here'. It must start platform bundle download and show
  notification after bundle installation.

* Also check plugin description and name in plugins settings
 (*Settings -> Plugins*).

* Open python file. Run action *Code -> Autodoc file*.
  It must update the file with missing docs and show tool output in bottom
  side toolwindow. This window must have autodoc icon on tool button.

* Middle-click on the toolbar header must close it.

* Check menu *Edit*. There must be item *Undo Autodoc*. Run it. It must
  rollback autodoc modifications. Then run *Redo Autodoc*.

* Do the same but with keyboard shortcut.

* Undo changes. Run action *Code -> Autodoc project*. It must update all
  python files in the project and show output on bottom side.
  You can't rollback changes in this case.

* Open non python file and check  *Code -> Autodoc file* menu item.
  It must be disabled. *Autodoc project* is always enabled.

Test bundle downloading
~~~~~~~~~~~~~~~~~~~~~~~

At first, find where the plugin installed. Something like:
``/home/user/.PyCharm2017.2/config/plugins/autodoc-intellij-plugin/``.
``autodoc-pkg`` contains platform bundle.

* Remove ``autodoc-pkg`` and start IDE. It must ask to install platform
  bundle. Close it and go to autodoc settings ``Settings -> Tools -> Autodoc``.
  There must be only install button.

* Click install button. It must start bundle download like in previous test.
  After download you will see popup notification (bla bla installed) and
  more settings. Also there must be version number and update button.

* Click update button. It will show progress dialog and then message
  what everything is up to date.

* Close IDE. And open ``autodoc-pkg/metadata.json``. It's a simple text file
  in json format. You need to change ``version`` and ``lastModified``
  to lower values to make the plugin thinks what there is a new version
  available.

  For example, if ``"version":"0.3.7"`` then change to ``"version":"0.3.6"``.
  Set ``lastModified`` to 0. This will force version checking.

* Open IDE. It must show new version notification.
  NOTE: this may change in future. We may add delay between updates checking
  (for example, one in 3 hr).

* Click *Click here* in popup. Details dialog with new and current version
  must show.

* Click *Update* button. New version download must start.

* Click *Cancel*. It must stop downloading.

* Restart IDE and update bundle from update dialog.
  Open autodoc settings. It must show last version.

* Change ``version`` and ``lastModified`` like described above and restart IDE.

* It again must show new version popup, ignore or close it. Open autodoc
  settings. There must appear *Install new version* button.

* Click *Install new version* button. It must show details dialog like in
  prev steps.

* Click *Remind me later*. This just closes the dialog.

* Click *Install new version* button again and the *Update* button.
  Wait until download finished. It must show new version in
  *Platform bundle version* label and notification popup.
  *Install* button must be replaced with *check update* button.

Test unsupported platform
~~~~~~~~~~~~~~~~~~~~~~~~~

* Install plugin on x32 Linux or Windows or MacOS < 10.11.
  It must show error notification about unsupported platform.

Test bundles downloading with local server
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

It's possible to specify local server instead of S3 to test downloading
workflow (errors etc).

Local server is provided by
`s3bundle <https://bitbucket.org/ludditelabs/s3bundle>`_ tool.

See *Plugin properties* for info how to set custom S3 URL.

Plugin properties
-----------------

The plugin supports few customization properties.
You can set properties in a special file (*Help -> Edit Custom Properties*)
or add to VM options in Run/Debug configuration if you debug plugin.

See https://www.jetbrains.com/help/idea/file-idea-properties.html for more info.

The following options are supported:

* S3 URL:

  - ``ludditelabs.bundle.s3url``
  - ``ludditelabs.bundle.bucket``,
  - ``ludditelabs.bundle.folder``

  Allows to override S3 URL. Result url will be::

      <s3url>/<bucket>[/<folder>]

  All properties has default values so you may change only one of them.
  For example::

      -Dludditelabs.bundle.s3url=http://127.0.0.1:5000

  Where *127.0.0.1:5000* is a local server.

  See ``com.ludditelabs.intellij.common.bundle.S3Bundle`` for more info.

* Statistics:

  - ``ludditelabs.autodoc.statistics.upload_action`` - show menu item
    with action to force statistics upload.

  - ``ludditelabs.autodoc.statistics.url`` - statistics server URL.

  Example::

      -Dludditelabs.autodoc.statistics.upload_action=true
      -Dludditelabs.autodoc.statistics.url=http://127.0.0.1:5000/statistics/intellij_plugin

  See ``com.ludditelabs.intellij.autodoc.statistics`` for more info.
