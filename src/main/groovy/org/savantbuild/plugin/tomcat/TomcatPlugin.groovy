/*
 * Copyright (c) 2014-2016, Inversoft Inc., All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.savantbuild.plugin.tomcat

import java.nio.file.Files
import java.nio.file.Path

import org.savantbuild.domain.Project
import org.savantbuild.output.Output
import org.savantbuild.parser.groovy.GroovyTools
import org.savantbuild.plugin.dep.DependencyPlugin
import org.savantbuild.plugin.file.FilePlugin
import org.savantbuild.plugin.groovy.BaseGroovyPlugin
import org.savantbuild.runtime.RuntimeConfiguration

/**
 * Tomcat plugin.
 * <p/>
 * This plugin provides the ability to setup Tomcat to run a web application.
 *
 * @author Brian Pontarelli
 */
class TomcatPlugin extends BaseGroovyPlugin {
  FilePlugin filePlugin

  DependencyPlugin dependencyPlugin

  TomcatSettings settings = new TomcatSettings()

  TomcatPlugin(Project project, RuntimeConfiguration runtimeConfiguration, Output output) {
    super(project, runtimeConfiguration, output)
    filePlugin = new FilePlugin(project, runtimeConfiguration, output)
    dependencyPlugin = new DependencyPlugin(project, runtimeConfiguration, output)
  }

  /**
   * Creates the Tomcat directory inside the build directory. Here's an example of using the plugin.
   * <p>
   * <pre>
   *   tomcat.build()
   * </pre>
   */
  void build() {
    def dependencyGroup = project.dependencies.groups.get(settings.dependencyGroup)
    if (dependencyGroup == null || dependencyGroup.dependencies.size() > 1) {
      fail("You must specify an artifact for your Tomcat tarball in a group named [${settings.dependencyGroup}] with a single dependency like this" +
          "  group(name: \"${settings.dependencyGroup}\") {\n" +
          "    dependency(id: \"org.apache.tomcat:apache-tomcat:8.5.9:tar.gz\")\n" +
          "  }\n" +
          "\n This is just an example, your version may be different.")
    }

    def dependencyVersion = dependencyGroup.dependencies.get(0).version.toString()
    def depId = dependencyGroup.dependencies.get(0).toString()
    def path = dependencyPlugin.path(id: depId, group: settings.dependencyGroup)

    def buildDirectoryPath = project.directory.resolve("${settings.buildDirectory}")
    filePlugin.prune(dir: buildDirectoryPath.resolve("apache-tomcat"))
    filePlugin.prune(dir: buildDirectoryPath.resolve("apache-tomcat-${dependencyVersion}"))

    // Move apache-tomcat-{version} --> apache-tomcat.
    filePlugin.untar(file: path, to: settings.buildDirectory)
    Files.move(
        buildDirectoryPath.resolve("apache-tomcat-${dependencyVersion}"),
        buildDirectoryPath.resolve("apache-tomcat")
    )

    filePlugin.copy(to: settings.buildDirectory.resolve("apache-tomcat/conf")) {
      fileSet(dir: settings.confDirectory)
    }

    filePlugin.copy(to: settings.buildDirectory.resolve("apache-tomcat/bin")) {
      fileSet(dir: settings.binDirectory)
    }

    // Delete the target if it exists and symlink it
    if (Files.isDirectory(project.directory.resolve(settings.buildWebDirectory))) {
      filePlugin.prune(dir: settings.buildWebDirectory)
    } else if (Files.isSymbolicLink(project.directory.resolve(settings.buildWebDirectory))) {
      Files.deleteIfExists(settings.buildWebDirectory)
    }

    filePlugin.symlink(target: settings.webDirectory, link: settings.buildWebDirectory)
  }

  void start(Map<String, Object> attributes) {
    if (!GroovyTools.attributesValid(attributes, ["debug", "detach"], [], ["debug": Boolean.class, "detach": Boolean.class])) {
      fail("Invalid attributes for start() method. You can supply either the [debug] or [detach] attributes as booleans. For example:\n\n" +
          "  start(debug: true, detach: false)")
    }

    boolean debug = attributes["debug"] != null ? attributes["debug"] : false
    boolean detach = attributes["detach"] != null ? attributes["detach"] : false
    Path catalina = project.directory.resolve("${settings.buildDirectory}/apache-tomcat/bin/catalina.sh")
    if (Files.isRegularFile(catalina)) {
      String pid = getProcessId("${settings.buildDirectory}/apache-tomcat")
      if (pid == null) {
        if (detach) {
          def arg = debug ? "jpda run" : "run"
          Runtime.getRuntime().exec("./build/dist/fusionauth-app/apache-tomcat/bin/catalina.sh ${arg}")
          pid = getProcessId("build/dist/fusionauth-app/apache-tomcat")
          output.info("Tomcat started and is running under process Id [${pid}]")
        } else {
          if (debug) {
            new ProcessBuilder("${settings.buildDirectory}/apache-tomcat/bin/catalina.sh", "jpda", "run").inheritIO().start().waitFor()
          } else {
            new ProcessBuilder("${settings.buildDirectory}/apache-tomcat/bin/catalina.sh", "run").inheritIO().start().waitFor()
          }
        }
      } else {
        output.info("Tomcat is already running under process Id [${pid}]")
      }
    } else {
      output.info("Looks like you haven't run `sb tomcat` yet. How many times do I need to remind you? Hehe...")
    }
  }

  void status() {
    String pid = getProcessId("${settings.buildDirectory}/apache-tomcat")
    if (pid == null) {
      output.info("Stopped")
    } else {
      output.info("Running [${pid}]")
    }
  }

  void stop() {
    sendProcessSignal("TERM", "${settings.buildDirectory}/apache-tomcat")

    // Give it a second to shut down so it can release locks otherwise we puke sometimes on the next step
    Thread.sleep(500)
  }

  void threadDump() {
    sendProcessSignal("QUIT", "${settings.buildDirectory}/apache-tomcat")
  }

  private static String getProcessId(String processName) {
    return "ps -efww".execute()
        .text
        .split("\n")
        .find { it.contains processName }?.split()?.getAt(1) as Integer
  }

  private void sendProcessSignal(String signal, String processName) {
    def pid = getProcessId(processName)
    if (pid != null) {
      output.info("Sending [${signal}] signal to process [${pid}].")
      "kill -s ${signal} ${pid}".execute().consumeProcessOutput(System.out, System.err)
    }
  }
}
