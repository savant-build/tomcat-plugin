/*
 * Copyright (c) 2014, Inversoft Inc., All Rights Reserved
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

import org.savantbuild.domain.Project
import org.savantbuild.output.Output
import org.savantbuild.plugin.dep.DependencyPlugin
import org.savantbuild.plugin.file.FilePlugin
import org.savantbuild.plugin.groovy.BaseGroovyPlugin
import org.savantbuild.runtime.RuntimeConfiguration

import java.nio.file.Files
import java.nio.file.Path

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
    Path path = dependencyPlugin.path(id: settings.dependencyID, group: settings.dependencyGroup)
    filePlugin.untar(file: path, to: settings.buildDirectory)

    filePlugin.copy(to: settings.buildConfDirectory) {
      fileSet(dir: settings.configurationDirectory)
    }

    filePlugin.copy(to: settings.buildBinDirectory) {
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
}
