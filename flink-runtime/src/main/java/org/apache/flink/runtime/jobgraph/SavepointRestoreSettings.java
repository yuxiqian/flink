/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.flink.runtime.jobgraph;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.configuration.StateRecoveryOptions;
import org.apache.flink.core.execution.RecoveryClaimMode;

import javax.annotation.Nonnull;

import java.io.Serializable;
import java.util.Objects;

import static org.apache.flink.util.Preconditions.checkNotNull;

/** Savepoint restore settings. */
public class SavepointRestoreSettings implements Serializable {

    private static final long serialVersionUID = 87377506900849777L;

    /** No restore should happen. */
    private static final SavepointRestoreSettings NONE =
            new SavepointRestoreSettings(null, false, RecoveryClaimMode.NO_CLAIM);

    /** Savepoint restore path. */
    private final String restorePath;

    /**
     * Flag indicating whether non restored state is allowed if the savepoint contains state for an
     * operator that is not part of the job.
     */
    private final boolean allowNonRestoredState;

    private final @Nonnull RecoveryClaimMode recoveryClaimMode;

    /**
     * Creates the restore settings.
     *
     * @param restorePath Savepoint restore path.
     * @param allowNonRestoredState Ignore unmapped state.
     * @param recoveryClaimMode how to restore from the savepoint
     */
    private SavepointRestoreSettings(
            String restorePath,
            boolean allowNonRestoredState,
            @Nonnull RecoveryClaimMode recoveryClaimMode) {
        this.restorePath = restorePath;
        this.allowNonRestoredState = allowNonRestoredState;
        this.recoveryClaimMode = recoveryClaimMode;
    }

    /**
     * Returns whether to restore from savepoint.
     *
     * @return <code>true</code> if should restore from savepoint.
     */
    public boolean restoreSavepoint() {
        return restorePath != null;
    }

    /**
     * Returns the path to the savepoint to restore from.
     *
     * @return Path to the savepoint to restore from or <code>null</code> if should not restore.
     */
    public String getRestorePath() {
        return restorePath;
    }

    /**
     * Returns whether non restored state is allowed if the savepoint contains state that cannot be
     * mapped back to the job.
     *
     * @return <code>true</code> if non restored state is allowed if the savepoint contains state
     *     that cannot be mapped back to the job.
     */
    public boolean allowNonRestoredState() {
        return allowNonRestoredState;
    }

    /** Tells how to restore from the given savepoint. */
    public @Nonnull RecoveryClaimMode getRecoveryClaimMode() {
        return recoveryClaimMode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SavepointRestoreSettings that = (SavepointRestoreSettings) o;
        return allowNonRestoredState == that.allowNonRestoredState
                && Objects.equals(restorePath, that.restorePath)
                && Objects.equals(recoveryClaimMode, that.recoveryClaimMode);
    }

    @Override
    public int hashCode() {
        int result = restorePath != null ? restorePath.hashCode() : 0;
        result = 31 * result + recoveryClaimMode.hashCode();
        result = 31 * result + (allowNonRestoredState ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        if (restoreSavepoint()) {
            return "SavepointRestoreSettings.forPath("
                    + "restorePath='"
                    + restorePath
                    + '\''
                    + ", allowNonRestoredState="
                    + allowNonRestoredState
                    + ", recoveryClaimMode="
                    + recoveryClaimMode
                    + ')';
        } else {
            return "SavepointRestoreSettings.none()";
        }
    }

    // ------------------------------------------------------------------------

    public static SavepointRestoreSettings none() {
        return NONE;
    }

    public static SavepointRestoreSettings forPath(String savepointPath) {
        return forPath(
                savepointPath,
                StateRecoveryOptions.SAVEPOINT_IGNORE_UNCLAIMED_STATE.defaultValue());
    }

    public static SavepointRestoreSettings forPath(
            String savepointPath, boolean allowNonRestoredState) {
        checkNotNull(savepointPath, "Savepoint restore path.");
        return new SavepointRestoreSettings(
                savepointPath,
                allowNonRestoredState,
                StateRecoveryOptions.RESTORE_MODE.defaultValue());
    }

    public static SavepointRestoreSettings forPath(
            String savepointPath,
            boolean allowNonRestoredState,
            @Nonnull RecoveryClaimMode recoveryClaimMode) {
        checkNotNull(savepointPath, "Savepoint restore path.");
        return new SavepointRestoreSettings(
                savepointPath, allowNonRestoredState, recoveryClaimMode);
    }

    // -------------------------- Parsing to and from a configuration object
    // ------------------------------------

    public static void toConfiguration(
            final SavepointRestoreSettings savepointRestoreSettings,
            final Configuration configuration) {
        configuration.set(
                StateRecoveryOptions.SAVEPOINT_IGNORE_UNCLAIMED_STATE,
                savepointRestoreSettings.allowNonRestoredState());
        configuration.set(
                StateRecoveryOptions.RESTORE_MODE, savepointRestoreSettings.getRecoveryClaimMode());
        final String savepointPath = savepointRestoreSettings.getRestorePath();
        if (savepointPath != null) {
            configuration.set(StateRecoveryOptions.SAVEPOINT_PATH, savepointPath);
        }
    }

    public static SavepointRestoreSettings fromConfiguration(final ReadableConfig configuration) {
        final String savepointPath = configuration.get(StateRecoveryOptions.SAVEPOINT_PATH);
        final boolean allowNonRestored =
                configuration.get(StateRecoveryOptions.SAVEPOINT_IGNORE_UNCLAIMED_STATE);
        final RecoveryClaimMode recoveryClaimMode =
                configuration.get(StateRecoveryOptions.RESTORE_MODE);
        return savepointPath == null
                ? SavepointRestoreSettings.none()
                : SavepointRestoreSettings.forPath(
                        savepointPath, allowNonRestored, recoveryClaimMode);
    }
}
