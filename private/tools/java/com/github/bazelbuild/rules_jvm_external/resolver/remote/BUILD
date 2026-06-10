load("@rules_java//java:java_library.bzl", "java_library")
load("@rules_jvm_external//:defs.bzl", "artifact")

java_library(
    name = "remote",
    srcs = [
        "DownloadResult.java",
        "Downloader.java",
        "EmptyResponse.java",
        "FileCacheMetadataService.java",
        "HttpDownloader.java",
        "LocalMetadataService.java",
        "UriNotFoundException.java",
    ],
    visibility = [
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/maven:__pkg__",
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver:__subpackages__",
        "//tests/com/github/bazelbuild/rules_jvm_external/resolver:__subpackages__",
    ],
    resources = [
        "META-INF/services/com.github.bazelbuild.rules_jvm_external.resolver.remote.MetadataService",
    ],
    deps = [
        ":api",
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external",
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/jar:IndexJar-lib",
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver",
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/events",
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/netrc",
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver/ui",
        artifact(
            "com.google.code.gson:gson",
            repository_name = "rules_jvm_external_deps",
        ),
        artifact(
            "com.google.guava:guava",
            repository_name = "rules_jvm_external_deps",
        ),
        artifact(
            "org.apache.maven:maven-model",
            repository_name = "rules_jvm_external_deps",
        ),
        artifact(
            "org.codehaus.plexus:plexus-utils",
            repository_name = "rules_jvm_external_deps",
        ),
        artifact(
            "org.codehaus.plexus:plexus-xml",
            repository_name = "rules_jvm_external_deps",
        ),
    ],
)

java_library(
    name = "api",
    srcs = [
        "DependencyMetadata.java",
        "MetadataService.java",
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver:Artifact.java",
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external/resolver:ResolutionRequest.java",
    ],
    visibility = [
        "//visibility:public",
    ],
    deps = [
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external",
        artifact(
            "com.google.guava:guava",
            repository_name = "rules_jvm_external_deps",
        ),
    ],
    exports = [
        "//private/tools/java/com/github/bazelbuild/rules_jvm_external",
    ],
)
