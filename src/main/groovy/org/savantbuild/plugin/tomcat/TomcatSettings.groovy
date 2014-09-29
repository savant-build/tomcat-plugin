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

import java.nio.file.Path
import java.nio.file.Paths

/**
 * Settings for the Tomcat plugin.
 *
 * @author Brian Pontarelli
 */
class TomcatSettings {
  /**
   * The Tomcat version. This is used to fetch the tar.gz file and build Paths in this file. Defaults to {@code 8.0.12}.
   */
  String version = "8.0.12"

  /**
   * The directory that the Tomcat tar.gz file is exploded to. Defaults to {@code build}.
   */
  Path buildDirectory = Paths.get("build")

  /**
   * The directory that the Tomcat bin files are located. Defaults to {@code build/apache-tomcat-8.0.12/bin}.
   */
  Path buildBinDirectory = buildDirectory.resolve("apache-tomcat-${version}/bin")

  /**
   * The directory that the Tomcat config files are located. Defaults to {@code build/apache-tomcat-8.0.12/conf}.
   */
  Path buildConfDirectory = buildDirectory.resolve("apache-tomcat-${version}/conf")

  /**
   * The directory that the Tomcat web application is located. Defaults to {@code build/apache-tomcat-8.0.12/webapps/ROOT}.
   */
  Path buildWebDirectory = buildDirectory.resolve("apache-tomcat-${version}/webapps/ROOT")

  /**
   * The directory that the Tomcat conf files for the current project are located. Defaults to {@code src/main/tomcat/conf}.
   */
  Path configurationDirectory = Paths.get("src/main/tomcat/conf")

  /**
   * The directory that the Tomcat bin files for the current project are located. Defaults to {@code src/main/tomcat/bin}.
   */
  Path binDirectory = Paths.get("src/main/tomcat/bin")

  /**
   * The Tomcat dependency group that is used to fetch the Tomcat tar.gz file. Defaults to {@code tomcat}.
   */
  String dependencyGroup = "tomcat"

  /**
   * The Tomcat dependency ID that is used to fetch the Tomcat tar.gz file. Defaults to {@code org.apache.tomcat:apache-tomcat:8.0.12:tar.gz}.
   */
  String dependencyID = "org.apache.tomcat:apache-tomcat:${version}:tar.gz"

  /**
   * The web application directory in current project. Defaults to {@code web}.
   */
  Path webDirectory = Paths.get("web")

  void setBuildDirectory(Path buildDirectory) {
    this.buildDirectory = buildDirectory
    this.buildBinDirectory = buildDirectory.resolve("apache-tomcat-${version}/bin")
    this.buildConfDirectory = buildDirectory.resolve("apache-tomcat-${version}/conf")
    this.buildWebDirectory = buildDirectory.resolve("apache-tomcat-${version}/webapps/ROOT")
  }
}
