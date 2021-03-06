resolvers ++= Seq(
  Resolver.bintrayIvyRepo("epfl-lara", "sbt-plugins"),
  Resolver.bintrayRepo("epfl-lara", "smt-z3"),
  Resolver.mavenLocal
)

val StainlessVersion = "0.7.1"

addSbtPlugin("ch.epfl.lara" % "sbt-stainless" % StainlessVersion)
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.4.0")

