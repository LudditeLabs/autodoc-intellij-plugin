/*
 * Copyright 2018 Luddite Labs Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ludditelabs.intellij.autodoc.bundle;

import com.ludditelabs.intellij.common.bundle.LocalBundle;
import org.jetbrains.annotations.NotNull;


/**
 * This class represents autodoc local platform bundle.
 */
public class PluginLocalBundle extends LocalBundle {
    private static String NAME = "autodoc";
    private static String DISPLAY_NAME = "Autodoc";

    /**
     * Construct autodoc platform bundle.
     * @param rootPath Root path where to unpack the bundle.
     */
    public PluginLocalBundle(@NotNull String rootPath) {
        super(rootPath, NAME, DISPLAY_NAME);
    }
}
