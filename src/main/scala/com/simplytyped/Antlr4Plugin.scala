package com.simplytyped

import sbt._
import Keys._

object Antlr4Plugin extends Plugin {
  val Antlr4 = config("antlr4")

  val generate = TaskKey[Seq[File]]("generate")
  val copyTokens = TaskKey[Seq[File]]("copy-tokens")
  val antlr4Dependency = SettingKey[ModuleID]("antlr4-dependency")

  def antlr4GeneratorTask : Def.Initialize[Task[Seq[File]]] = Def.task {
    val cachedCompile = FileFunction.cached(streams.value.cacheDirectory / "antlr4", FilesInfo.lastModified, FilesInfo.exists) {
      in : Set[File] => runAntlr(in, (javaSource in Antlr4).value, (managedClasspath in Compile).value.files, streams.value.log)
    }
    cachedCompile(((sourceDirectory in Antlr4).value ** "*.g4").get.toSet).toSeq
  }

  def antlr4CopyTokensTask : Def.Initialize[Task[Seq[File]]] = Def.task {
    val srcBase = (javaSource in Antlr4).value
    val tokens = (srcBase ** "*.tokens").get.toSeq
    tokens
  }

  def runAntlr(srcFiles: Set[File], targetDir: File, classpath: Seq[File], log: Logger) = {
    val args = Seq("-cp", Path.makeString(classpath), "org.antlr.v4.Tool", "-o", targetDir.toString) 
    val exitCode = Process("java", args++srcFiles.map{_.toString}) ! log
    if(exitCode != 0) sys.error(s"Antlr4 failed with exit code $exitCode")
    (targetDir ** "*.java").get.toSet
  }

  val antlr4Settings = inConfig(Antlr4)(Seq(
    sourceDirectory <<= (sourceDirectory in Compile) {_ / "antlr4"},
    javaSource <<= (sourceManaged in Compile) {_ / "java"},
    generate <<= antlr4GeneratorTask,
    copyTokens <<= antlr4CopyTokensTask,
    antlr4Dependency := "org.antlr" % "antlr4" % "4.1"
  )) ++ Seq(
    unmanagedSourceDirectories in Compile <+= (sourceDirectory in Antlr4),
    sourceGenerators in Compile <+= (generate in Antlr4),
    resourceGenerators in Compile <+= (copyTokens in Antlr4),
    cleanFiles <+= (javaSource in Antlr4),
    libraryDependencies <+= (antlr4Dependency in Antlr4)
  )
}