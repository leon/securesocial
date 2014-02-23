name := "reactivemongo-demo"

version := "1.0-SNAPSHOT"

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature",
  "-language:reflectiveCalls" // Using -> in routes causes scala compiler to complain, this fixes it
)

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots")
)

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play2-reactivemongo" % "0.10.2",
  "ws.securesocial" %% "securesocial" % "master-SNAPSHOT"
)     

play.Project.playScalaSettings
