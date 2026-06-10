// Copyright 2026 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.github.bazelbuild.rules_jvm_external.resolver.maven;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import com.github.bazelbuild.rules_jvm_external.resolver.remote.DependencyMetadata;
import com.github.bazelbuild.rules_jvm_external.resolver.remote.FileCacheMetadataService;
import com.github.bazelbuild.rules_jvm_external.resolver.remote.LocalMetadataService;
import com.github.bazelbuild.rules_jvm_external.resolver.remote.MetadataService;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class MetadataServiceTest {

  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  @Before
  public void setUp() {
    System.clearProperty("rules_jvm_external.metadata_cache_dir");
  }

  @After
  public void tearDown() {
    System.clearProperty("rules_jvm_external.metadata_cache_dir");
  }

  @Test
  public void testDependencyMetadataProperties() {
    Set<URI> repos = ImmutableSet.of(URI.create("https://repo1.maven.org/maven2"));
    Set<String> packages = ImmutableSet.of("com.mycorp.pkg");
    Set<String> classes = ImmutableSet.of("com.mycorp.pkg.MyClass");
    Map<String, Set<String>> services = new HashMap<>();
    services.put("com.mycorp.MySPI", ImmutableSet.of("com.mycorp.pkg.MyClass"));

    DependencyMetadata dm = new DependencyMetadata("abc123sha", repos, packages, classes, services);

    assertEquals("abc123sha", dm.getSha256());
    assertEquals(repos, dm.getRepositories());
    assertEquals(packages, dm.getPackages());
    assertEquals(classes, dm.getClasses());
    assertEquals(services, dm.getServices());
  }

  @Test
  public void testFileCacheMetadataServiceCacheHitAndMiss() throws Exception {
    Path cacheDir = tempFolder.newFolder("metadata-cache").toPath();
    System.setProperty("rules_jvm_external.metadata_cache_dir", cacheDir.toAbsolutePath().toString());

    Coordinates coords = new Coordinates("org.hamcrest:hamcrest-core:1.3");
    Collection<URI> repos = Collections.singletonList(URI.create("https://repo1.maven.org/maven2"));

    DependencyMetadata dm = new DependencyMetadata(
        "sha256hash",
        ImmutableSet.copyOf(repos),
        ImmutableSet.of("org.hamcrest"),
        ImmutableSet.of("org.hamcrest.Core"),
        Collections.emptyMap()
    );

    // Create a stub delegate service to return metadata on miss
    MetadataService delegate = new MetadataService() {
      @Override
      public DependencyMetadata getMetadata(Coordinates c, Collection<URI> repositories) {
        return dm;
      }
    };

    FileCacheMetadataService fileCache = new FileCacheMetadataService();
    fileCache.initialize(delegate);

    // First call: Cache Miss. Should delegate, populate cache, and return metadata
    DependencyMetadata metadataMissResult = fileCache.getMetadata(coords, repos);
    assertNotNull(metadataMissResult);
    assertEquals("sha256hash", metadataMissResult.getSha256());

    // Verify cache file was written to disk
    assertTrue(Files.list(cacheDir).count() > 0);

    // Second call: Cache Hit. We remove/null out the delegate to ensure it's not called
    fileCache.initialize(null);
    DependencyMetadata metadataHitResult = fileCache.getMetadata(coords, repos);
    assertNotNull(metadataHitResult);
    assertEquals("sha256hash", metadataHitResult.getSha256());
  }

  @Test
  public void testFileCacheMetadataServiceNoCacheDirPassThrough() throws Exception {
    Coordinates coords = new Coordinates("org.hamcrest:hamcrest-core:1.3");
    Collection<URI> repos = Collections.singletonList(URI.create("https://repo1.maven.org/maven2"));

    DependencyMetadata dm = new DependencyMetadata(
        "sha256hash",
        ImmutableSet.copyOf(repos),
        ImmutableSet.of("org.hamcrest"),
        ImmutableSet.of("org.hamcrest.Core"),
        Collections.emptyMap()
    );

    MetadataService delegate = new MetadataService() {
      @Override
      public DependencyMetadata getMetadata(Coordinates c, Collection<URI> repositories) {
        return dm;
      }
    };

    FileCacheMetadataService fileCache = new FileCacheMetadataService();
    fileCache.initialize(delegate);

    // With no cache dir configured, it should behave as a simple pass-through to delegate
    DependencyMetadata result = fileCache.getMetadata(coords, repos);
    assertNotNull(result);
    assertEquals("sha256hash", result.getSha256());
  }
}
