/*
 * Copyright 2016 the original author or authors.
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

package org.gradle.kotlin.dsl.provider

import org.gradle.api.Project
import org.gradle.api.initialization.dsl.ScriptHandler
import org.gradle.api.internal.initialization.ClassLoaderScope
import org.gradle.api.internal.initialization.ScriptHandlerInternal

import org.gradle.configuration.ScriptPlugin
import org.gradle.configuration.ScriptPluginFactory

import org.gradle.groovy.scripts.ScriptSource

import org.gradle.plugin.use.internal.PluginRequestApplicator
import org.gradle.kotlin.dsl.support.EmbeddedKotlinProvider

import javax.inject.Inject


class KotlinScriptPluginFactory @Inject internal constructor(
    val classPathProvider: KotlinScriptClassPathProvider,
    val kotlinCompiler: CachingKotlinCompiler,
    val pluginRequestApplicator: PluginRequestApplicator,
    val embeddedKotlinProvider: EmbeddedKotlinProvider) : ScriptPluginFactory {

    override fun create(
        scriptSource: ScriptSource, scriptHandler: ScriptHandler,
        targetScope: ClassLoaderScope, baseScope: ClassLoaderScope,
        topLevelScript: Boolean): ScriptPlugin =

        KotlinScriptPlugin(
            scriptSource,
            compile(scriptSource, scriptHandler, targetScope, baseScope, topLevelScript))

    private
    fun compile(
        scriptSource: ScriptSource, scriptHandler: ScriptHandler,
        targetScope: ClassLoaderScope, baseScope: ClassLoaderScope,
        topLevelScript: Boolean): (Project) -> Unit =

        compilerFor(scriptSource, scriptHandler, targetScope, baseScope, topLevelScript).run {
            if (topLevelScript && inClassPathMode())
                compileForClassPath()
            else
                compile()
        }

    private
    fun compilerFor(
        scriptSource: ScriptSource, scriptHandler: ScriptHandler,
        targetScope: ClassLoaderScope, baseScope: ClassLoaderScope,
        topLevelScript: Boolean) =

        KotlinBuildScriptCompiler(
            kotlinCompiler,
            scriptSource, topLevelScript,
            scriptHandler as ScriptHandlerInternal,
            pluginRequestApplicator,
            baseScope, targetScope,
            classPathProvider,
            embeddedKotlinProvider)

    private
    fun inClassPathMode() =
        System.getProperty(modeSystemPropertyName) == classPathMode

    companion object {
        val modeSystemPropertyName = "org.gradle.kotlin.dsl.provider.mode"
        val classPathMode = "classpath"
    }
}
