/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.api.internal.artifacts.ivyservice.ivyresolve.memcache

import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier
import org.gradle.api.internal.artifacts.ivyservice.ArtifactResolveContext
import org.gradle.api.internal.artifacts.ivyservice.BuildableArtifactResolveResult
import org.gradle.api.internal.artifacts.ivyservice.BuildableArtifactSetResolveResult
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.BuildableModuleVersionMetaDataResolveResult
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.LocalAwareModuleVersionRepository
import org.gradle.api.internal.artifacts.ivyservice.ivyresolve.ModuleSource
import org.gradle.api.internal.artifacts.metadata.DependencyMetaData
import org.gradle.api.internal.artifacts.metadata.ModuleVersionArtifactIdentifier
import org.gradle.api.internal.artifacts.metadata.ModuleVersionArtifactMetaData
import org.gradle.api.internal.artifacts.metadata.ModuleVersionMetaData
import spock.lang.Specification

import static org.gradle.api.internal.artifacts.DefaultModuleVersionSelector.newSelector

class CachedRepositoryTest extends Specification {

    def stats = new DependencyMetadataCacheStats()
    def cache = Mock(DependencyMetadataCache)
    def delegate = Mock(LocalAwareModuleVersionRepository)
    def repo = new CachedRepository(cache, delegate, stats)

    def lib = newSelector("org", "lib", "1.0")
    def dep = Stub(DependencyMetaData) { getRequested() >> lib }
    def result = Mock(BuildableModuleVersionMetaDataResolveResult)

    def "delegates"() {
        when:
        def id = repo.getId()
        def name = repo.getName()

        then:
        id == "x"
        name == "localRepo"
        1 * delegate.getId() >> "x"
        1 * delegate.getName() >> "localRepo"
    }

    def "retrieves and caches local dependencies"() {
        when:
        repo.getLocalDependency(dep, result)

        then:
        1 * cache.supplyLocalMetaData(lib, result) >> false
        1 * delegate.getLocalDependency(dep, result)
        1 * cache.newLocalDependencyResult(lib, result)
        0 * _
    }

    def "uses local dependencies from cache"() {
        when:
        repo.getLocalDependency(dep, result)

        then:
        1 * cache.supplyLocalMetaData(lib, result) >> true
        0 * _
    }

    def "retrieves and caches dependencies"() {
        when:
        repo.getDependency(dep, result)

        then:
        1 * cache.supplyMetaData(lib, result) >> false
        1 * delegate.getDependency(dep, result)
        1 * cache.newDependencyResult(lib, result)
        0 * _
    }

    def "uses dependencies from cache"() {
        when:
        repo.getDependency(dep, result)

        then:
        1 * cache.supplyMetaData(lib, result) >> true
        0 * _
    }

    def "retrieves, caches and uses module artifacts"() {
        def result = Mock(BuildableArtifactSetResolveResult)
        def id = DefaultModuleVersionIdentifier.newId("group", "name", "version")
        def moduleMetaData = Stub(ModuleVersionMetaData) {
            getId() >> id
        }
        def context = Stub(ArtifactResolveContext) {
            getId() >> "context"
        }
        final CachedModuleArtifactsKey key = new CachedModuleArtifactsKey(id, "context")
        when:
        repo.resolveModuleArtifacts(moduleMetaData, context, result)

        then:
        1 * cache.supplyModuleArtifacts(key, result) >> false
        1 * delegate.resolveModuleArtifacts(moduleMetaData, context, result)
        1 * cache.newModuleArtifacts(key, result)
        0 * _

        when:
        repo.resolveModuleArtifacts(moduleMetaData, context, result)

        then:
        1 * cache.supplyModuleArtifacts(key, result) >> true
        0 * _
    }

    def "retrieves and caches artifacts"() {
        def result = Mock(BuildableArtifactResolveResult)
        def artifactId = Stub(ModuleVersionArtifactIdentifier)
        def artifact = Stub(ModuleVersionArtifactMetaData) {
            getId() >> artifactId
        }
        def moduleSource = Mock(ModuleSource)

        when:
        repo.resolveArtifact(artifact, moduleSource, result)

        then:
        1 * cache.supplyArtifact(artifactId, result) >> false
        1 * delegate.resolveArtifact(artifact, moduleSource, result)
        1 * result.getFailure() >> null
        1 * cache.newArtifact(artifactId, result)
        0 * _
    }

    def "uses artifacts from cache"() {
        def result = Mock(BuildableArtifactResolveResult)
        def artifactId = Stub(ModuleVersionArtifactIdentifier)
        def artifact = Stub(ModuleVersionArtifactMetaData) {
            getId() >> artifactId
        }
        def moduleSource = Mock(ModuleSource)

        when:
        repo.resolveArtifact(artifact, moduleSource, result)

        then:
        1 * cache.supplyArtifact(artifactId, result) >> true
        0 * _
    }
}
