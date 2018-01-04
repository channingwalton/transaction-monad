inThisBuild(Seq(
  organization := "com.casualmiracles",
  name := "transaction-monad",
  scalaVersion := "2.12.4",
  scalaBinaryVersion := "2.12",
))

lazy val commonSettings = Seq(
  scalacOptions ++= Seq(
    "-deprecation",                      // Emit warning and location for usages of deprecated APIs.
    "-encoding", "utf-8",                // Specify character encoding used by source files.
    "-explaintypes",                     // Explain type errors in more detail.
    "-feature",                          // Emit warning and location for usages of features that should be imported explicitly.
    "-language:existentials",            // Existential types (besides wildcard types) can be written and inferred
    "-language:experimental.macros",     // Allow macro definition (besides implementation and application)
    "-language:higherKinds",             // Allow higher-kinded types
    "-language:implicitConversions",     // Allow definition of implicit functions called views
    "-unchecked",                        // Enable additional warnings where generated code depends on assumptions.
    "-Xcheckinit",                       // Wrap field accessors to throw an exception on uninitialized access.
    "-Xfatal-warnings",                  // Fail the compilation if there are any warnings.
    "-Xfuture",                          // Turn on future language features.
    "-Xlint:adapted-args",               // Warn if an argument list is modified to match the receiver.
    "-Xlint:by-name-right-associative",  // By-name parameter of right associative operator.
    "-Xlint:constant",                   // Evaluation of a constant arithmetic expression results in an error.
    "-Xlint:delayedinit-select",         // Selecting member of DelayedInit.
    "-Xlint:doc-detached",               // A Scaladoc comment appears to be detached from its element.
    "-Xlint:inaccessible",               // Warn about inaccessible types in method signatures.
    "-Xlint:infer-any",                  // Warn when a type argument is inferred to be `Any`.
    "-Xlint:missing-interpolator",       // A string literal appears to be missing an interpolator id.
    "-Xlint:nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Xlint:nullary-unit",               // Warn when nullary methods return Unit.
    "-Xlint:option-implicit",            // Option.apply used implicit view.
    "-Xlint:package-object-classes",     // Class or object defined in package object.
    "-Xlint:poly-implicit-overload",     // Parameterized overloaded implicit methods are not visible as view bounds.
    "-Xlint:private-shadow",             // A private field (or class parameter) shadows a superclass field.
    "-Xlint:stars-align",                // Pattern sequence wildcard must align with sequence component.
    "-Xlint:type-parameter-shadow",      // A local type parameter shadows a type already in scope.
    "-Xlint:unsound-match",              // Pattern match may not be typesafe.
    "-Yno-adapted-args",                 // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
    "-Ypartial-unification",             // Enable partial unification in type constructor inference
    "-Ywarn-dead-code",                  // Warn when dead code is identified.
    "-Ywarn-extra-implicit",             // Warn when more than one implicit parameter section is defined.
    "-Ywarn-inaccessible",               // Warn about inaccessible types in method signatures.
    "-Ywarn-infer-any",                  // Warn when a type argument is inferred to be `Any`.
    "-Ywarn-nullary-override",           // Warn when non-nullary `def f()' overrides nullary `def f'.
    "-Ywarn-nullary-unit",               // Warn when nullary methods return Unit.
    "-Ywarn-numeric-widen",              // Warn when numerics are widened.
    "-Ywarn-unused:implicits",           // Warn if an implicit parameter is unused.
    "-Ywarn-unused:imports",             // Warn if an import selector is not referenced.
    "-Ywarn-unused:locals",              // Warn if a local definition is unused.
    "-Ywarn-unused:params",              // Warn if a value parameter is unused.
    "-Ywarn-unused:patvars",             // Warn if a variable bound in a pattern is unused.
    "-Ywarn-unused:privates",            // Warn if a private member is unused.
    "-Ywarn-value-discard"               // Warn when non-Unit expression results are unused.
  )
)

lazy val coreDependencies = Seq(
  "org.typelevel" %% "cats-core" % "1.0.0",
  compilerPlugin("org.spire-math" %% "kind-projector" % "0.9.4"),
  "org.typelevel" %% "cats-testkit" % "1.0.0" % Test
)

def publishSettings: Seq[Setting[_]] = Seq(
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  publishMavenStyle := true,
  publishArtifact in Test := false,
  pomIncludeRepository := { _ ⇒ false },
  pomIncludeRepository := Function.const(false),
  homepage := Some(url("http://underscore.io")),
  publishTo in ThisBuild := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "content/repositories/snapshots")
    else
      Some("releases" at nexus + "service/local/staging/deploy/maven2")
  },
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/channingwalton/transaction-monad"),
      "scm:git@github.com/channingwalton/transaction-monad.git"
    )
  ),
  developers := List(
    Developer(
      id    = "channingwalton",
      name  = "Channing Walton",
      email = "channing.walton@undercsore.io",
      url   = url("http://underscore.io/")
    )
  ),
  pomExtra := <licenses>
    <license>
      <name>MIT License</name>
      <url>http://www.opensource.org/licenses/mit-license/</url>
      <distribution>repo</distribution>
    </license>
  </licenses>)

lazy val noPublishSettings = Seq(
  skip in publish := true
)

lazy val root =
  project.in(file("."))
    .settings(noPublishSettings)
      .aggregate(core, catsEffect, experiments)

lazy val coreSettings = commonSettings

lazy val core = project.in(file("core"))
                .settings(publishSettings)
                .settings(moduleName := "transaction-core")
                .settings(coreSettings:_*)
                .settings(libraryDependencies := coreDependencies)

lazy val catsEffectSettings = commonSettings

lazy val catsEffect = project.in(file("cats-effect"))
  .settings(publishSettings)
  .dependsOn(core % "test->test;compile->compile")
  .settings(moduleName := "transaction-cats-effect")
  .settings(catsEffectSettings:_*)
  .settings(
      libraryDependencies := coreDependencies ++
        Seq("org.typelevel" %% "cats-effect" % "0.5"))

lazy val exampleSettings = commonSettings

lazy val examples = project.in(file("examples"))
  .settings(noPublishSettings)
  .settings(moduleName := "transaction-examples")
  .settings(exampleSettings:_*)
  .dependsOn(catsEffect)
  .settings(
    libraryDependencies := coreDependencies ++
      Seq(
        "org.tpolecat" %% "doobie-core" % "0.5.0-M11",
        "org.tpolecat" %% "doobie-h2" % "0.5.0-M11"))

lazy val experimentsSettings = commonSettings

lazy val experiments = project.in(file("experiments"))
  .settings(noPublishSettings)
  .settings(moduleName := "transaction-experiments")
  .settings(experimentsSettings:_*)
  .settings(
    libraryDependencies := coreDependencies)