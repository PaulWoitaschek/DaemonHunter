#!/usr/bin/env kotlin
@file:DependsOn("com.github.ajalt.clikt:clikt-jvm:5.0.3")

import App.Daemon
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.mordant.terminal.ConversionResult
import com.github.ajalt.mordant.terminal.Prompt
import com.github.ajalt.mordant.terminal.Terminal

private fun outdatedDaemons(daemons: List<Daemon>): List<Daemon> {
  return daemons.groupBy { it.type }
    .map { (_, daemons) ->
      val largestVersion = daemons.maxByOrNull { it.version }
        ?.version
      if (daemons.size > 1) {
        daemons.filter { it.version != largestVersion }
      } else {
        emptyList()
      }
    }
    .flatten()
}

class KillDaemonPrompt(terminal: Terminal, private val daemons: List<Daemon>) : Prompt<List<Daemon>>(
  prompt = "Enter the numbers, separated by commas you want to kill",
  terminal = terminal,
  default = outdatedDaemons(daemons),
) {

  override fun renderValue(value: List<Daemon>): String {
    return daemons.mapIndexedNotNull { index, daemon ->
      if (daemon in value) {
        index + 1
      } else {
        null
      }
    }.joinToString(separator = ",")
  }

  override fun convert(input: String): ConversionResult<List<Daemon>> {
    val result = input.split(",").map { rawNumber ->
      val index = (rawNumber.toIntOrNull() ?: -1) - 1
      daemons.getOrNull(index)
        ?: return ConversionResult.Invalid("Invalid input")
    }
    return ConversionResult.Valid(result)
  }

}

class App : CliktCommand() {


  enum class DaemonType {
    Gradle, Kotlin
  }

  data class Version(
    val major: Int,
    val minor: Int,
    val patch: Int?
  ) : Comparable<Version> {

    override fun compareTo(other: Version): Int {
      return compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })
    }
  }

  data class Daemon(
    val type: DaemonType,
    val processId: Int,
    val version: String
  )

  override fun run() {
    val daemons: List<Daemon> = Runtime.getRuntime().exec(arrayOf("jps", "-mlvV")).inputStream.bufferedReader()
      .readLines()
      .filter { line -> line.contains("gradle") || line.contains("kotlin") }
      .mapNotNull { line ->
        parseDaemon(line)
      }
      .groupBy { it.type }
      .mapValues { (_, daemons) ->
        daemons.sortedByDescending { daemon -> daemon.version }
      }
      .flatMap { it.value }

    if (daemons.isEmpty()) {
      echo("No daemons detected.")
      return
    }

    val promptItems = daemons
      .mapIndexed { index, daemon ->
        "[${index + 1}]\t${daemon.type}\t${daemon.version}"
      }
      .joinToString(separator = "\n")
    echo(promptItems)

    val daemonsToKill: List<Daemon> = KillDaemonPrompt(terminal, daemons).ask()
      ?: return

    daemonsToKill.forEach { daemon ->
      val command = if (System.getProperty("os.name")?.contains("win", ignoreCase = true) == true) {
        "taskkill /F /PID ${daemon.processId}"
      } else {
        "kill -9 ${daemon.processId}"
      }
      Runtime.getRuntime().exec(command)
      echo("killed ${daemon.type}\t${daemon.version}")
    }
  }

  private fun parseDaemon(line: String): Daemon? {
    val processId = "(.*?) ".toRegex().find(line)!!.groupValues.drop(1).first().toInt()
    parseGradleVersion(line)?.let { gradleVersion ->
      return Daemon(type = DaemonType.Gradle, version = gradleVersion, processId = processId)
    }
    parseKotlinVersion(line)?.let { kotlinVersion ->
      return Daemon(type = DaemonType.Kotlin, version = kotlinVersion, processId = processId)
    }
    return null
  }

  private fun parseKotlinVersion(line: String): String? {
    return "kotlin-compiler-embeddable-(.*?)\\.jar".toRegex().find(line)?.groupValues?.drop(1)
      ?.firstOrNull()
  }

  private fun parseGradleVersion(line: String): String? {
    return "org.gradle.launcher.daemon.bootstrap.GradleDaemon (.*?) ".toRegex()
      .find(line)?.groupValues?.drop(1)?.firstOrNull()
  }

}


App().main(args)

