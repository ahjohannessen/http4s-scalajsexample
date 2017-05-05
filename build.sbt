lazy val commonSettings = {
  organization := "org.http4s"
  version := "0.0.1-SNAPSHOT"
  scalaVersion := "2.12.2"
}

val Http4sVersion = "0.17.0-M2"
val utestV = "0.4.5"
val scalaJsDomV = "0.9.1"

// This function allows triggered compilation to run only when scala files changes
// It lets change static files freely
def includeInTrigger(f: java.io.File): Boolean =
  f.isFile && {
    val name = f.getName.toLowerCase
    name.endsWith(".scala") || name.endsWith(".js")
  }

lazy val shared =
  (crossProject.crossType(CrossType.Pure) in file("shared"))
    .settings(commonSettings)
    .settings(
      libraryDependencies ++= Seq(
        "com.lihaoyi" %%% "scalatags" % "0.6.5"
      )
    ) // set up settings specific to the JS project
    .jsConfigure(_ enablePlugins ScalaJSWeb)
lazy val sharedJvm = shared.jvm
lazy val sharedJs = shared.js

lazy val backend = (project in file("backend"))
  .settings(
    name := "http4s-scalajsexample-backend"
  )
  .enablePlugins(SbtWeb)
  .settings(commonSettings)
  .settings(
    libraryDependencies ++= Seq(
      "org.http4s"     %% "http4s-blaze-server" % Http4sVersion,
      "org.http4s"     %% "http4s-circe"        % Http4sVersion,
      "org.http4s"     %% "http4s-dsl"          % Http4sVersion,
      "ch.qos.logback" % "logback-classic"      % "1.2.3"
    ),
    scalaJSProjects := Seq(frontend),
    pipelineStages in Assets := Seq(scalaJSPipeline),
    compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
    WebKeys.packagePrefix in Assets := "public/",
    managedClasspath in Runtime += (packageBin in Assets).value
//    // Allows to read the generated JS on client
//    resources in Compile += (fastOptJS in (frontend, Compile)).value.data,
//    // Lets the backend to read the .map file for js
//    resources in Compile += (fastOptJS in (frontend, Compile)).value
//      .map((x: sbt.File) => new File(x.getAbsolutePath + ".map"))
//      .data,
//    // Lets the server read the jsdeps file
//    (managedResources in Compile) += (artifactPath in (frontend, Compile, packageJSDependencies)).value,
//    // do a fastOptJS on reStart
//    reStart := (reStart dependsOn (fastOptJS in (frontend, Compile))).evaluated,
//    // This settings makes reStart to rebuild if a scala.js file changes on the client
//    watchSources ++= (watchSources in frontend).value,
//    // On recompilation only consider changes to .scala and .js files
//    watchSources ~= { t: Seq[java.io.File] =>
//      { t.filter(includeInTrigger) }
//    },
//    // Support stopping the running server
//    mainClass in reStart := Some("org.http4s.scalajsexample.Server")
  )
  .dependsOn(sharedJvm)

lazy val frontend = (project in file("frontend"))
  .settings(
    name := "http4s-scalajsexample-frontend"
  )
  .enablePlugins(ScalaJSPlugin)
  .enablePlugins(ScalaJSWeb)
  .settings(commonSettings)
  .settings(
    scalaJSUseMainModuleInitializer := true,
    // use uTest framework for tests
    testFrameworks += new TestFramework("utest.runner.Framework"),
    jsDependencies += RuntimeDOM,
    skip in packageJSDependencies := false,
//    // Put the jsdeps file on a place reachable for the server
    crossTarget in (Compile, packageJSDependencies) := (resourceManaged in Assets).value,
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-dom" % scalaJsDomV,
      "com.lihaoyi" %%% "utest"        % utestV % Test
    )

  )
  .dependsOn(sharedJs)
