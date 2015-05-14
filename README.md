# i18n

A Clojure library designed to make i18n easier. Provides convenience
functions to access the JVM's localization facilities and some guidance on
how to use the GNU `gettext` tools.

The `main.clj` in this repo contains some simple code that demonstrates how
to use the translation functions. Before you can use it, you need to run
`make msgfmt` to generate the necessary `ResourceBundles`.

Then you can do `lein run` or `LANG=de_DE lein run` to look at English and
German output.

## Developer usage

Any Clojure code that needs to generate human-readable text must use the
functions `puppetlabs.i18n.core/trs` and `puppetlabs.i18n.core/tru` to do
so. Use `trs` for messages that should be formatted in the system's locale,
for example log messages, and `tru` for messages that will be shown to the
current user, for example an error that happened processing a web request.

When you require `puppetlabs.i18n.core` into your namespace, you *must*
call it either `trs`/`tru` or `i18n/trs`/`i18n/tru` (these are the names
that `xgettext` will look for when it extracts strings) Typically, you
would have this in your namespace declaration

    (ns puppetlabs.myproject
      (:require [puppetlabs.i18n.core :as i18n :refer [trs tru]]))

You use `trs`/`tru` very similar to how you use `format`, except that the
format string must be a valid
[`java.text.MessageFormat`](https://docs.oracle.com/javase/8/docs/api/java/text/MessageFormat.html)
pattern. For example, you would write

    (println (trs "It takes {0} women {1} months to have a child" 3 9))

### Project setup

1. In your `project.clj`, add `puppetlabs/i18n` to the `:dependencies` and
   to the `plugins`
1. Run `lein i18n init`. This will
   * put a `Makefile.i18n` into `dev-resources/` in your project and
     include it into an existing toplevel `Makefile` resp. create a new one
     that does that. You should check these files into you source control
     system.
   * add hooks to the `compile` task that will refresh i18n
     data (equivalent of running `make i18n`)

This setup will ensure that the file `locales/messages.pot` and the
translations in `locales/LANG.po` are updated every time you compile your
project. Compiling your project will also regenerate the Java
`ResourceBundle` classes that your code needs to do translations.

You can manually regenerate these files by running `make i18n`. Additional
information about the Make targets is available through running `make
help`.

The i18n tools maintain files in two directories: message catalogs in
`locales/` and compiled translations in `resources/`. You should check the
files in `locales/` into source control, but not the ones in `resources/`.

## Translator usage

When a translator gets ready to translate messages, they need to update the
corresponding `.po` file. For example, to update German translations,
they'd run

    make locales/de.po

and then edit `locales/de.po`. The plugin actually performs the `make`
invocation above everytime you compile the project, so you should only have
to do it manually to add a PO file for a new locale. Translators should be
able to work off the PO files that are checked into source control, as they
are always kept 'fresh' by the plugin.

## Release usage

When it comes time to make a release, or if you want to use your code in a
different locale before then, you need to generate Java `ResourceBundle`
classes that contain the localized messages. This is done by running `make
msgfmt` on your project.

## Todo

* allow setting a thread-specific locale, and use that for l10n
* propagating locale to background threads
* figure out how to combine the message catalogs of multiple
  libraries/projects into one at release time (msgcat)
* add Ring middleware to do language negotiation based on the
  Accept-Language header and set the per-thread locale accordingly
