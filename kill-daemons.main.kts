#!/usr/bin/env kotlin
@file:DependsOn("com.github.ajalt.clikt:clikt-jvm:3.0.1")

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.output.TermUi

class App : CliktCommand() {

  enum class DaemonType {
    Gradle, Kotlin
  }

  data class Daemon(
    val type: DaemonType,
    val processId: Int,
    val version: String
  )

  override fun run() {
    val daemons = Runtime.getRuntime().exec("jps -mlvV").inputStream.bufferedReader()
      .readLines()
      .filter { line -> line.contains("gradle") || line.contains("kotlin") }
      .mapNotNull { line ->
        if(line.contains("kotlin")){
          println(line)
        }
        val processId = "(.*?) ".toRegex().find(line)!!.groupValues.drop(1).first().toInt()
        val gradleVersion =
          "org.gradle.launcher.daemon.bootstrap.GradleDaemon (.*?) ".toRegex()
            .find(line)?.groupValues?.drop(1)?.firstOrNull()
        if (gradleVersion == null) {
          val kotlinVersion =
            "kotlin-compiler-embeddable-(.*?)\\.jar".toRegex().find(line)?.groupValues?.drop(1)
              ?.firstOrNull()
          if (kotlinVersion != null) {
            Daemon(type = DaemonType.Kotlin, version = kotlinVersion, processId = processId)
          } else {
            null
          }
        } else {
          Daemon(type = DaemonType.Gradle, version = gradleVersion, processId = processId)
        }
      }
      .groupBy { it.type }
      .mapValues { it.value.sortedByDescending { it.version } }
      .flatMap { it.value }

    val promptItems = daemons
      .mapIndexed { index, daemon ->
        "[${index + 1}]\t${daemon.type}\t${daemon.version}"
      }
      .joinToString(separator = "\n")

    TermUi.echo(promptItems)

    val selectedDaemons = TermUi.prompt("Enter the numbers, separated by commas you want to kill") { input ->
      input.split(",").map { rawNumber ->
        val index = (rawNumber.toIntOrNull() ?: -1) - 1
        daemons.getOrElse(index) {
          throw UsageError("Invalid input")
        }
      }
    }!!
    selectedDaemons.forEach { daemon ->
      val command = if (TermUi.isWindows) {
        "taskkill /F /PID ${daemon.processId}"
      } else {
        "kill -9 ${daemon.processId}"
      }
      Runtime.getRuntime().exec(command)
    }
  }
}

App().main(args)
