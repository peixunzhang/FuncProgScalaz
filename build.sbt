scalaVersion in ThisBuild := "2.12.17"
scalacOptions in ThisBuild ++= Seq(
  "-language:_",
  "-Ypartial-unification",
  "-Xfatal-warnings"
)

val derivingVersion = "1.0.0"


libraryDependencies ++= Seq(
  "com.github.mpilquist" %% "simulacrum" % "0.13.0",
  "org.scalaz" %% "scalaz-core" % "7.2.26",
  "eu.timepit" %% "refined-scalaz" % "0.10.3",
  "org.scalaz" %% "scalaz-deriving"            % derivingVersion,
  // "org.scalaz" %% "scalaz-deriving-magnolia"   % derivingVersion,
  // "org.scalaz" %% "scalaz-deriving-scalacheck" % derivingVersion,
  "org.scalaz" %% "scalaz-deriving-jsonformat" % derivingVersion
)

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.7")
addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)
