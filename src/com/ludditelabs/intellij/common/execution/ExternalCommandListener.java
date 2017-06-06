package com.ludditelabs.intellij.common.execution;

import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.openapi.util.Key;
import com.intellij.util.Consumer;


/**
 * Listener for the external command.
 *
 * It provides access to the external command process events and the command
 * result.
 */
public class ExternalCommandListener implements ProcessListener, Consumer<ExternalCommandResult> {
    @Override
    public void startNotified(ProcessEvent event) {

    }

    @Override
    public void processTerminated(ProcessEvent event) {

    }

    @Override
    public void processWillTerminate(ProcessEvent event, boolean willBeDestroyed) {

    }

    @Override
    public void onTextAvailable(ProcessEvent event, Key outputType) {

    }

    /**
     * Consume external command result.
     *
     * @param result external command result info.
     */
    @Override
    public void consume(ExternalCommandResult result) {

    }
}
