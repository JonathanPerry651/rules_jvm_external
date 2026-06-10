// Copyright 2024 The Bazel Authors. All rights reserved.
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

package com.github.bazelbuild.rules_jvm_external.resolver.remote;

import com.github.bazelbuild.rules_jvm_external.Coordinates;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;


public class FileCacheMetadataService implements MetadataService {
  private static final String CACHE_DIR_ENV = "RJE_METADATA_CACHE_DIR";
  private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
  private MetadataService delegate;

  @Override
  public void initialize(MetadataService defaultService) {
    this.delegate = defaultService;
  }

  private String getCacheDir() {
    String cacheDir = System.getenv(CACHE_DIR_ENV);
    if (cacheDir == null || cacheDir.isEmpty()) {
      cacheDir = System.getProperty("rules_jvm_external.metadata_cache_dir");
    }
    return cacheDir;
  }

  @Override
  public DependencyMetadata getMetadata(Coordinates coords, Collection<URI> repositories) {
    String cacheDir = getCacheDir();
    if (cacheDir == null || cacheDir.isEmpty()) {
      if (delegate != null) {
        return delegate.getMetadata(coords, repositories);
      }
      return null;
    }

    Path cachePath = Paths.get(cacheDir).resolve(getCacheFilename(coords));
    if (Files.exists(cachePath)) {
      try (Reader reader = Files.newBufferedReader(cachePath)) {
        DependencyMetadata metadata = gson.fromJson(reader, DependencyMetadata.class);
        if (metadata != null) {
          System.out.println("Metadata cache HIT for " + coords);
        }
        return metadata;
      } catch (IOException e) {
        System.err.println("Failed to read metadata from cache: " + cachePath + " - " + e.getMessage());
      }
    }

    // Cache miss or read failed
    if (delegate != null) {
      DependencyMetadata metadata = delegate.getMetadata(coords, repositories);
      if (metadata != null) {
        putMetadata(coords, metadata);
      }
      return metadata;
    }

    return null;
  }

  @Override
  public void putMetadata(Coordinates coords, DependencyMetadata metadata) {
    String cacheDir = getCacheDir();
    if (cacheDir == null || cacheDir.isEmpty()) {
      return;
    }

    Path cacheDirName = Paths.get(cacheDir);
    Path cachePath = cacheDirName.resolve(getCacheFilename(coords));

    try {
      Files.createDirectories(cacheDirName);
      try (Writer writer = Files.newBufferedWriter(cachePath)) {
        gson.toJson(metadata, writer);
        System.out.println("Metadata cache POPULATED for " + coords);
      }
    } catch (IOException e) {
      System.err.println("Failed to write metadata to cache: " + cachePath + " - " + e.getMessage());
    }
  }

  private String getCacheFilename(Coordinates coords) {
    StringBuilder sb = new StringBuilder();
    sb.append(coords.getGroupId().replace(':', '_').replace('/', '_'))
      .append("__")
      .append(coords.getArtifactId().replace(':', '_').replace('/', '_'))
      .append("__")
      .append(coords.getVersion());
    if (coords.getClassifier() != null && !coords.getClassifier().isEmpty()) {
      sb.append("__").append(coords.getClassifier());
    }
    if (coords.getExtension() != null && !coords.getExtension().isEmpty()) {
      sb.append("__").append(coords.getExtension());
    }
    sb.append(".json");
    return sb.toString();
  }
}
