/*
 * Copyright 2020, Seqera Labs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package nextflow.plugin

import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.Path

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.pf4j.DefaultPluginLoader
import org.pf4j.DefaultPluginManager
import org.pf4j.ManifestPluginDescriptorFinder
import org.pf4j.PluginDescriptorFinder
import org.pf4j.PluginLoader
import org.pf4j.PluginRepository
/**
 * Custom plugin manager creating tracking plugins with symlinks to
 * the parent repository
 *
 * @author Paolo Di Tommaso <paolo.ditommaso@gmail.com>
 */
@Slf4j
@CompileStatic
class LocalPluginManager extends DefaultPluginManager {

    LocalPluginManager(Path root) {
        super(root)
    }

    @Override
    protected PluginDescriptorFinder createPluginDescriptorFinder() {
        return new ManifestPluginDescriptorFinder()
    }

    @Override
    protected PluginLoader createPluginLoader() {
        return new DefaultPluginLoader(this)
    }

    @Override
    protected PluginRepository createPluginRepository() {
        return new LocalPluginRepository(getPluginsRoot())
    }

    /**
     * Override parent {@link DefaultPluginManager#loadPlugin(java.nio.file.Path)} method
     * creating a symlink the plugin path directory in the current plugin root
     *
     * @param pluginPath
     *      The file path where the plugin is stored unzipped) in the
     *      central plugins directory
     */
    @Override
    String loadPlugin(Path pluginPath) {
        if( !pluginPath )
            throw new IllegalArgumentException("Plugin path cannot be null")
        if( !Files.isDirectory(pluginPath) )
            throw new IllegalArgumentException("Plugin path must be a directory: $pluginPath")

        // create a symlink relative to the current root
        final symlink = getPluginsRoot().resolve(pluginPath.getFileName())
        createLink(symlink, pluginPath)
        super.loadPlugin(symlink)
    }

    private void createLink(Path symlink, Path pluginPath) {
        try {
            log.trace "Creating local plugins root link: $symlink → $pluginPath"
            Files.createSymbolicLink(symlink, pluginPath)
        }
        catch (FileAlreadyExistsException e) {
            log.debug "Deleting existing local plugins root link: $symlink"
            Files.delete(symlink)
            Files.createSymbolicLink(symlink, pluginPath)
        }
    }
}
