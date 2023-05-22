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
import java.nio.file.Paths

import org.savantbuild.dep.domain.Artifact
import org.savantbuild.dep.domain.Dependencies
import org.savantbuild.dep.domain.DependencyGroup
import org.savantbuild.dep.domain.License
import org.savantbuild.dep.workflow.FetchWorkflow
import org.savantbuild.dep.workflow.PublishWorkflow
import org.savantbuild.dep.workflow.Workflow
import org.savantbuild.dep.workflow.process.CacheProcess
import org.savantbuild.dep.workflow.process.URLProcess
import org.savantbuild.domain.Project
import org.savantbuild.domain.Version
import org.savantbuild.io.FileTools
import org.savantbuild.output.Output
import org.savantbuild.output.SystemOutOutput
import org.savantbuild.runtime.BuildFailureException
import org.savantbuild.runtime.RuntimeConfiguration
import org.testng.annotations.BeforeMethod
import org.testng.annotations.BeforeSuite
import org.testng.annotations.Test

import static org.testng.Assert.assertEquals
import static org.testng.Assert.assertTrue

/**
 * Tests the tomcat plugin.
 *
 * @author Brian Pontarelli
 */
class TomcatPluginTest {
  public static Path projectDir

  Output output

  Project project

  @BeforeSuite
  static void beforeSuite() {
    projectDir = Paths.get("")
    if (!Files.isRegularFile(projectDir.resolve("LICENSE"))) {
      projectDir = Paths.get("../tomcat-plugin")
    }
  }

  @BeforeMethod
  void beforeMethod() {
    output = new SystemOutOutput(true)
    output.enableDebug()

    project = new Project(projectDir.resolve("test-project"), output)
    project.group = "org.savantbuild.test"
    project.name = "test-project"
    project.version = new Version("1.0")
    project.licenses.add(License.parse("ApacheV2_0", null))

    project.dependencies = new Dependencies(new DependencyGroup("tomcat", false, new Artifact("org.apache.tomcat:apache-tomcat:8.0.12:tar.gz")))

    def cacheDir = projectDir.resolve("build/cache")
    project.workflow = new Workflow(
        new FetchWorkflow(output,
            new CacheProcess(output, cacheDir.toString(), cacheDir.toString()),
            new URLProcess(output, "https://repository.savantbuild.org", null, null)
        ),
        new PublishWorkflow(
            new CacheProcess(output, cacheDir.toString(), cacheDir.toString())
        ),
        output
    )
  }

  @Test(expectedExceptions = BuildFailureException.class)
  void build_missingDependencyGroup() {
    FileTools.prune(project.directory.resolve("build"))

    project.dependencies = new Dependencies(new DependencyGroup("elasticsearch", false, new Artifact("org.apache.tomcat:apache-tomcat:8.0.12:tar.gz")))
    TomcatPlugin plugin = new TomcatPlugin(project, new RuntimeConfiguration(), output)
    plugin.build()
  }

  @Test(expectedExceptions = BuildFailureException.class)
  void build_tooManyDependencies() {
    FileTools.prune(project.directory.resolve("build"))

    project.dependencies = new Dependencies(
        new DependencyGroup("tomcat", false,
            new Artifact("org.apache.tomcat:apache-tomcat:8.0.12:tar.gz"),
            new Artifact("org.apache.tomcat:apache-tomcat:8.5.9:tar.gz")
        ))

    TomcatPlugin plugin = new TomcatPlugin(project, new RuntimeConfiguration(), output)
    plugin.build()
  }

  @Test
  void build_alternateGroupName() {
    FileTools.prune(project.directory.resolve("build"))

    project.dependencies = new Dependencies(new DependencyGroup("foobar", false, new Artifact("org.apache.tomcat:apache-tomcat:8.0.12:tar.gz")))

    TomcatPlugin plugin = new TomcatPlugin(project, new RuntimeConfiguration(), output)
    plugin.settings.dependencyGroup = "foobar"
    plugin.build()

    assertFilesEqual("build/apache-tomcat/conf/server.xml", "src/main/tomcat/conf/server.xml")
    assertFilesEqual("build/apache-tomcat/bin/setenv.sh", "src/main/tomcat/bin/setenv.sh")
    assertTrue(Files.isDirectory(project.directory.resolve("build/apache-tomcat/lib")))
    assertTrue(Files.isDirectory(project.directory.resolve("build/apache-tomcat/webapps")))
    assertTrue(Files.isSymbolicLink(project.directory.resolve("build/apache-tomcat/webapps/ROOT")))
  }


  @Test
  void build() {
    FileTools.prune(project.directory.resolve("build"))

    TomcatPlugin plugin = new TomcatPlugin(project, new RuntimeConfiguration(), output)
    plugin.build()

    assertFilesEqual("build/apache-tomcat/conf/server.xml", "src/main/tomcat/conf/server.xml")
    assertFilesEqual("build/apache-tomcat/bin/setenv.sh", "src/main/tomcat/bin/setenv.sh")
    assertTrue(Files.isDirectory(project.directory.resolve("build/apache-tomcat/lib")))
    assertTrue(Files.isDirectory(project.directory.resolve("build/apache-tomcat/webapps")))
    assertTrue(Files.isSymbolicLink(project.directory.resolve("build/apache-tomcat/webapps/ROOT")))
  }

  private void assertFilesEqual(String file1, String file2) {
    assertEquals(new String(Files.readAllBytes(project.directory.resolve(file1))), new String(Files.readAllBytes(project.directory.resolve(file2))))
  }
}
