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

package org.apache.flink.runtime.rest.messages.checkpoints;

import org.apache.flink.runtime.checkpoint.AbstractCheckpointStats;
import org.apache.flink.runtime.checkpoint.CheckpointStatsStatus;
import org.apache.flink.runtime.checkpoint.CheckpointType;
import org.apache.flink.runtime.checkpoint.CompletedCheckpointStats;
import org.apache.flink.runtime.checkpoint.FailedCheckpointStats;
import org.apache.flink.runtime.checkpoint.PendingCheckpointStats;
import org.apache.flink.runtime.checkpoint.SavepointType;
import org.apache.flink.runtime.checkpoint.SnapshotType;
import org.apache.flink.runtime.checkpoint.TaskStateStats;
import org.apache.flink.runtime.jobgraph.JobVertexID;
import org.apache.flink.runtime.rest.messages.ResponseBody;
import org.apache.flink.runtime.rest.messages.json.JobVertexIDKeyDeserializer;
import org.apache.flink.runtime.rest.messages.json.JobVertexIDKeySerializer;
import org.apache.flink.util.CollectionUtil;
import org.apache.flink.util.Preconditions;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonCreator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonSubTypes;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.annotation.JsonSerialize;

import javax.annotation.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

/** Statistics for a checkpoint. Required for visibility of RestAPICheckpointType. */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "className")
@JsonSubTypes({
    @JsonSubTypes.Type(
            value = CheckpointStatistics.CompletedCheckpointStatistics.class,
            name = "completed"),
    @JsonSubTypes.Type(
            value = CheckpointStatistics.FailedCheckpointStatistics.class,
            name = "failed"),
    @JsonSubTypes.Type(
            value = CheckpointStatistics.PendingCheckpointStatistics.class,
            name = "in_progress")
})
public class CheckpointStatistics implements ResponseBody {

    public static final String FIELD_NAME_ID = "id";

    public static final String FIELD_NAME_STATUS = "status";

    public static final String FIELD_NAME_IS_SAVEPOINT = "is_savepoint";

    public static final String FIELD_NAME_SAVEPOINT_FORMAT = "savepointFormat";

    public static final String FIELD_NAME_TRIGGER_TIMESTAMP = "trigger_timestamp";

    public static final String FIELD_NAME_LATEST_ACK_TIMESTAMP = "latest_ack_timestamp";

    public static final String FIELD_NAME_CHECKPOINTED_SIZE = "checkpointed_size";

    /**
     * The accurate name of this field should be 'checkpointed_data_size', keep it as before to not
     * break backwards compatibility for old web UI.
     *
     * @see <a href="https://issues.apache.org/jira/browse/FLINK-13390">FLINK-13390</a>
     */
    public static final String FIELD_NAME_STATE_SIZE = "state_size";

    public static final String FIELD_NAME_DURATION = "end_to_end_duration";

    public static final String FIELD_NAME_ALIGNMENT_BUFFERED = "alignment_buffered";

    public static final String FIELD_NAME_PROCESSED_DATA = "processed_data";

    public static final String FIELD_NAME_PERSISTED_DATA = "persisted_data";

    public static final String FIELD_NAME_NUM_SUBTASKS = "num_subtasks";

    public static final String FIELD_NAME_NUM_ACK_SUBTASKS = "num_acknowledged_subtasks";

    public static final String FIELD_NAME_TASKS = "tasks";

    public static final String FIELD_NAME_CHECKPOINT_TYPE = "checkpoint_type";

    @JsonProperty(FIELD_NAME_ID)
    private final long id;

    @JsonProperty(FIELD_NAME_STATUS)
    private final CheckpointStatsStatus status;

    @JsonProperty(FIELD_NAME_IS_SAVEPOINT)
    private final boolean savepoint;

    @JsonProperty(FIELD_NAME_SAVEPOINT_FORMAT)
    @Nullable
    private final String savepointFormat;

    @JsonProperty(FIELD_NAME_TRIGGER_TIMESTAMP)
    private final long triggerTimestamp;

    @JsonProperty(FIELD_NAME_LATEST_ACK_TIMESTAMP)
    private final long latestAckTimestamp;

    @JsonProperty(FIELD_NAME_CHECKPOINTED_SIZE)
    private final long checkpointedSize;

    @JsonProperty(FIELD_NAME_STATE_SIZE)
    private final long stateSize;

    @JsonProperty(FIELD_NAME_DURATION)
    private final long duration;

    @JsonProperty(FIELD_NAME_ALIGNMENT_BUFFERED)
    private final long alignmentBuffered;

    @JsonProperty(FIELD_NAME_PROCESSED_DATA)
    private final long processedData;

    @JsonProperty(FIELD_NAME_PERSISTED_DATA)
    private final long persistedData;

    @JsonProperty(FIELD_NAME_NUM_SUBTASKS)
    private final int numSubtasks;

    @JsonProperty(FIELD_NAME_NUM_ACK_SUBTASKS)
    private final int numAckSubtasks;

    @JsonProperty(FIELD_NAME_CHECKPOINT_TYPE)
    private final RestAPICheckpointType checkpointType;

    @JsonProperty(FIELD_NAME_TASKS)
    @JsonSerialize(keyUsing = JobVertexIDKeySerializer.class)
    private final Map<JobVertexID, TaskCheckpointStatistics> checkpointStatisticsPerTask;

    @JsonCreator
    private CheckpointStatistics(
            @JsonProperty(FIELD_NAME_ID) long id,
            @JsonProperty(FIELD_NAME_STATUS) CheckpointStatsStatus status,
            @JsonProperty(FIELD_NAME_IS_SAVEPOINT) boolean savepoint,
            @JsonProperty(FIELD_NAME_SAVEPOINT_FORMAT) String savepointFormat,
            @JsonProperty(FIELD_NAME_TRIGGER_TIMESTAMP) long triggerTimestamp,
            @JsonProperty(FIELD_NAME_LATEST_ACK_TIMESTAMP) long latestAckTimestamp,
            @JsonProperty(FIELD_NAME_CHECKPOINTED_SIZE) long checkpointedSize,
            @JsonProperty(FIELD_NAME_STATE_SIZE) long stateSize,
            @JsonProperty(FIELD_NAME_DURATION) long duration,
            @JsonProperty(FIELD_NAME_ALIGNMENT_BUFFERED) long alignmentBuffered,
            @JsonProperty(FIELD_NAME_PROCESSED_DATA) long processedData,
            @JsonProperty(FIELD_NAME_PERSISTED_DATA) long persistedData,
            @JsonProperty(FIELD_NAME_NUM_SUBTASKS) int numSubtasks,
            @JsonProperty(FIELD_NAME_NUM_ACK_SUBTASKS) int numAckSubtasks,
            @JsonProperty(FIELD_NAME_CHECKPOINT_TYPE) RestAPICheckpointType checkpointType,
            @JsonDeserialize(keyUsing = JobVertexIDKeyDeserializer.class)
                    @JsonProperty(FIELD_NAME_TASKS)
                    Map<JobVertexID, TaskCheckpointStatistics> checkpointStatisticsPerTask) {
        this.id = id;
        this.status = Preconditions.checkNotNull(status);
        this.savepoint = savepoint;
        this.savepointFormat = savepointFormat;
        this.triggerTimestamp = triggerTimestamp;
        this.latestAckTimestamp = latestAckTimestamp;
        this.checkpointedSize = checkpointedSize;
        this.stateSize = stateSize;
        this.duration = duration;
        this.alignmentBuffered = alignmentBuffered;
        this.processedData = processedData;
        this.persistedData = persistedData;
        this.numSubtasks = numSubtasks;
        this.numAckSubtasks = numAckSubtasks;
        this.checkpointType = Preconditions.checkNotNull(checkpointType);
        this.checkpointStatisticsPerTask = Preconditions.checkNotNull(checkpointStatisticsPerTask);
    }

    public long getId() {
        return id;
    }

    public CheckpointStatsStatus getStatus() {
        return status;
    }

    public boolean isSavepoint() {
        return savepoint;
    }

    public long getTriggerTimestamp() {
        return triggerTimestamp;
    }

    public long getLatestAckTimestamp() {
        return latestAckTimestamp;
    }

    public long getCheckpointedSize() {
        return checkpointedSize;
    }

    public long getStateSize() {
        return stateSize;
    }

    public long getDuration() {
        return duration;
    }

    public int getNumSubtasks() {
        return numSubtasks;
    }

    public int getNumAckSubtasks() {
        return numAckSubtasks;
    }

    public RestAPICheckpointType getCheckpointType() {
        return checkpointType;
    }

    @Nullable
    public Map<JobVertexID, TaskCheckpointStatistics> getCheckpointStatisticsPerTask() {
        return checkpointStatisticsPerTask;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        CheckpointStatistics that = (CheckpointStatistics) o;
        return id == that.id
                && savepoint == that.savepoint
                && Objects.equals(savepointFormat, that.savepointFormat)
                && triggerTimestamp == that.triggerTimestamp
                && latestAckTimestamp == that.latestAckTimestamp
                && stateSize == that.stateSize
                && duration == that.duration
                && alignmentBuffered == that.alignmentBuffered
                && processedData == that.processedData
                && persistedData == that.persistedData
                && numSubtasks == that.numSubtasks
                && numAckSubtasks == that.numAckSubtasks
                && status == that.status
                && Objects.equals(checkpointType, that.checkpointType)
                && Objects.equals(checkpointStatisticsPerTask, that.checkpointStatisticsPerTask);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                id,
                status,
                savepoint,
                savepointFormat,
                triggerTimestamp,
                latestAckTimestamp,
                stateSize,
                duration,
                alignmentBuffered,
                processedData,
                persistedData,
                numSubtasks,
                numAckSubtasks,
                checkpointType,
                checkpointStatisticsPerTask);
    }

    // -------------------------------------------------------------------------
    // Static factory methods
    // -------------------------------------------------------------------------

    public static CheckpointStatistics generateCheckpointStatistics(
            AbstractCheckpointStats checkpointStats, boolean includeTaskCheckpointStatistics) {
        Preconditions.checkNotNull(checkpointStats);

        Map<JobVertexID, TaskCheckpointStatistics> checkpointStatisticsPerTask;

        if (includeTaskCheckpointStatistics) {
            Collection<TaskStateStats> taskStateStats = checkpointStats.getAllTaskStateStats();

            checkpointStatisticsPerTask =
                    CollectionUtil.newHashMapWithExpectedSize(taskStateStats.size());

            for (TaskStateStats taskStateStat : taskStateStats) {
                checkpointStatisticsPerTask.put(
                        taskStateStat.getJobVertexId(),
                        new TaskCheckpointStatistics(
                                checkpointStats.getCheckpointId(),
                                checkpointStats.getStatus(),
                                taskStateStat.getLatestAckTimestamp(),
                                taskStateStat.getCheckpointedSize(),
                                taskStateStat.getStateSize(),
                                taskStateStat.getEndToEndDuration(
                                        checkpointStats.getTriggerTimestamp()),
                                0,
                                taskStateStat.getProcessedDataStats(),
                                taskStateStat.getPersistedDataStats(),
                                taskStateStat.getNumberOfSubtasks(),
                                taskStateStat.getNumberOfAcknowledgedSubtasks()));
            }
        } else {
            checkpointStatisticsPerTask = Collections.emptyMap();
        }
        String savepointFormat = null;
        SnapshotType snapshotType = checkpointStats.getProperties().getCheckpointType();
        if (snapshotType instanceof SavepointType) {
            savepointFormat = ((SavepointType) snapshotType).getFormatType().name();
        }
        if (checkpointStats instanceof CompletedCheckpointStats) {
            final CompletedCheckpointStats completedCheckpointStats =
                    ((CompletedCheckpointStats) checkpointStats);

            return new CompletedCheckpointStatistics(
                    completedCheckpointStats.getCheckpointId(),
                    completedCheckpointStats.getStatus(),
                    snapshotType.isSavepoint(),
                    savepointFormat,
                    completedCheckpointStats.getTriggerTimestamp(),
                    completedCheckpointStats.getLatestAckTimestamp(),
                    completedCheckpointStats.getCheckpointedSize(),
                    completedCheckpointStats.getStateSize(),
                    completedCheckpointStats.getEndToEndDuration(),
                    0,
                    completedCheckpointStats.getProcessedData(),
                    completedCheckpointStats.getPersistedData(),
                    completedCheckpointStats.getNumberOfSubtasks(),
                    completedCheckpointStats.getNumberOfAcknowledgedSubtasks(),
                    RestAPICheckpointType.valueOf(
                            completedCheckpointStats.getProperties().getCheckpointType(),
                            completedCheckpointStats.isUnalignedCheckpoint()),
                    checkpointStatisticsPerTask,
                    completedCheckpointStats.getExternalPath(),
                    completedCheckpointStats.isDiscarded());
        } else if (checkpointStats instanceof FailedCheckpointStats) {
            final FailedCheckpointStats failedCheckpointStats =
                    ((FailedCheckpointStats) checkpointStats);

            return new FailedCheckpointStatistics(
                    failedCheckpointStats.getCheckpointId(),
                    failedCheckpointStats.getStatus(),
                    failedCheckpointStats.getProperties().isSavepoint(),
                    savepointFormat,
                    failedCheckpointStats.getTriggerTimestamp(),
                    failedCheckpointStats.getLatestAckTimestamp(),
                    failedCheckpointStats.getCheckpointedSize(),
                    failedCheckpointStats.getStateSize(),
                    failedCheckpointStats.getEndToEndDuration(),
                    0,
                    failedCheckpointStats.getProcessedData(),
                    failedCheckpointStats.getPersistedData(),
                    failedCheckpointStats.getNumberOfSubtasks(),
                    failedCheckpointStats.getNumberOfAcknowledgedSubtasks(),
                    RestAPICheckpointType.valueOf(
                            failedCheckpointStats.getProperties().getCheckpointType(),
                            failedCheckpointStats.isUnalignedCheckpoint()),
                    checkpointStatisticsPerTask,
                    failedCheckpointStats.getFailureTimestamp(),
                    failedCheckpointStats.getFailureMessage());
        } else if (checkpointStats instanceof PendingCheckpointStats) {
            final PendingCheckpointStats pendingCheckpointStats =
                    ((PendingCheckpointStats) checkpointStats);

            return new PendingCheckpointStatistics(
                    pendingCheckpointStats.getCheckpointId(),
                    pendingCheckpointStats.getStatus(),
                    pendingCheckpointStats.getProperties().isSavepoint(),
                    savepointFormat,
                    pendingCheckpointStats.getTriggerTimestamp(),
                    pendingCheckpointStats.getLatestAckTimestamp(),
                    pendingCheckpointStats.getCheckpointedSize(),
                    pendingCheckpointStats.getStateSize(),
                    pendingCheckpointStats.getEndToEndDuration(),
                    0,
                    pendingCheckpointStats.getProcessedData(),
                    pendingCheckpointStats.getPersistedData(),
                    pendingCheckpointStats.getNumberOfSubtasks(),
                    pendingCheckpointStats.getNumberOfAcknowledgedSubtasks(),
                    RestAPICheckpointType.valueOf(
                            pendingCheckpointStats.getProperties().getCheckpointType(),
                            pendingCheckpointStats.isUnalignedCheckpoint()),
                    checkpointStatisticsPerTask);
        } else {
            throw new IllegalArgumentException(
                    "Given checkpoint stats object of type "
                            + checkpointStats.getClass().getName()
                            + " cannot be converted.");
        }
    }

    /**
     * Backward compatibility layer between internal {@link CheckpointType} and a field used in
     * {@link CheckpointStatistics}.
     */
    public enum RestAPICheckpointType {
        CHECKPOINT,
        UNALIGNED_CHECKPOINT,
        SAVEPOINT,
        SYNC_SAVEPOINT;

        public static RestAPICheckpointType valueOf(
                SnapshotType checkpointType, boolean isUnalignedCheckpoint) {
            if (checkpointType.isSavepoint()) {
                Preconditions.checkArgument(
                        !isUnalignedCheckpoint,
                        "Currently the savepoint doesn't support unaligned checkpoint.");
                SavepointType savepointType = (SavepointType) checkpointType;
                return savepointType.isSynchronous() ? SYNC_SAVEPOINT : SAVEPOINT;
            }
            if (isUnalignedCheckpoint) {
                return UNALIGNED_CHECKPOINT;
            }
            return CHECKPOINT;
        }
    }

    // ---------------------------------------------------------------------
    // Static inner classes
    // ---------------------------------------------------------------------

    /** Statistics for a completed checkpoint. */
    public static final class CompletedCheckpointStatistics extends CheckpointStatistics {

        public static final String FIELD_NAME_EXTERNAL_PATH = "external_path";

        public static final String FIELD_NAME_DISCARDED = "discarded";

        @JsonProperty(FIELD_NAME_EXTERNAL_PATH)
        @Nullable
        private final String externalPath;

        @JsonProperty(FIELD_NAME_DISCARDED)
        private final boolean discarded;

        @JsonCreator
        public CompletedCheckpointStatistics(
                @JsonProperty(FIELD_NAME_ID) long id,
                @JsonProperty(FIELD_NAME_STATUS) CheckpointStatsStatus status,
                @JsonProperty(FIELD_NAME_IS_SAVEPOINT) boolean savepoint,
                @JsonProperty(FIELD_NAME_SAVEPOINT_FORMAT) String savepointFormat,
                @JsonProperty(FIELD_NAME_TRIGGER_TIMESTAMP) long triggerTimestamp,
                @JsonProperty(FIELD_NAME_LATEST_ACK_TIMESTAMP) long latestAckTimestamp,
                @JsonProperty(FIELD_NAME_CHECKPOINTED_SIZE) long checkpointedSize,
                @JsonProperty(FIELD_NAME_STATE_SIZE) long stateSize,
                @JsonProperty(FIELD_NAME_DURATION) long duration,
                @JsonProperty(FIELD_NAME_ALIGNMENT_BUFFERED) long alignmentBuffered,
                @JsonProperty(FIELD_NAME_PROCESSED_DATA) long processedData,
                @JsonProperty(FIELD_NAME_PERSISTED_DATA) long persistedData,
                @JsonProperty(FIELD_NAME_NUM_SUBTASKS) int numSubtasks,
                @JsonProperty(FIELD_NAME_NUM_ACK_SUBTASKS) int numAckSubtasks,
                @JsonProperty(FIELD_NAME_CHECKPOINT_TYPE) RestAPICheckpointType checkpointType,
                @JsonDeserialize(keyUsing = JobVertexIDKeyDeserializer.class)
                        @JsonProperty(FIELD_NAME_TASKS)
                        Map<JobVertexID, TaskCheckpointStatistics> checkpointingStatisticsPerTask,
                @JsonProperty(FIELD_NAME_EXTERNAL_PATH) @Nullable String externalPath,
                @JsonProperty(FIELD_NAME_DISCARDED) boolean discarded) {
            super(
                    id,
                    status,
                    savepoint,
                    savepointFormat,
                    triggerTimestamp,
                    latestAckTimestamp,
                    checkpointedSize,
                    stateSize,
                    duration,
                    alignmentBuffered,
                    processedData,
                    persistedData,
                    numSubtasks,
                    numAckSubtasks,
                    checkpointType,
                    checkpointingStatisticsPerTask);

            this.externalPath = externalPath;
            this.discarded = discarded;
        }

        @Nullable
        public String getExternalPath() {
            return externalPath;
        }

        public boolean isDiscarded() {
            return discarded;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            CompletedCheckpointStatistics that = (CompletedCheckpointStatistics) o;
            return discarded == that.discarded && Objects.equals(externalPath, that.externalPath);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), externalPath, discarded);
        }
    }

    /** Statistics for a failed checkpoint. */
    public static final class FailedCheckpointStatistics extends CheckpointStatistics {

        public static final String FIELD_NAME_FAILURE_TIMESTAMP = "failure_timestamp";

        public static final String FIELD_NAME_FAILURE_MESSAGE = "failure_message";

        @JsonProperty(FIELD_NAME_FAILURE_TIMESTAMP)
        private final long failureTimestamp;

        @JsonProperty(FIELD_NAME_FAILURE_MESSAGE)
        @Nullable
        private final String failureMessage;

        @JsonCreator
        public FailedCheckpointStatistics(
                @JsonProperty(FIELD_NAME_ID) long id,
                @JsonProperty(FIELD_NAME_STATUS) CheckpointStatsStatus status,
                @JsonProperty(FIELD_NAME_IS_SAVEPOINT) boolean savepoint,
                @JsonProperty(FIELD_NAME_SAVEPOINT_FORMAT) String savepointFormat,
                @JsonProperty(FIELD_NAME_TRIGGER_TIMESTAMP) long triggerTimestamp,
                @JsonProperty(FIELD_NAME_LATEST_ACK_TIMESTAMP) long latestAckTimestamp,
                @JsonProperty(FIELD_NAME_CHECKPOINTED_SIZE) long checkpointedSize,
                @JsonProperty(FIELD_NAME_STATE_SIZE) long stateSize,
                @JsonProperty(FIELD_NAME_DURATION) long duration,
                @JsonProperty(FIELD_NAME_ALIGNMENT_BUFFERED) long alignmentBuffered,
                @JsonProperty(FIELD_NAME_PROCESSED_DATA) long processedData,
                @JsonProperty(FIELD_NAME_PERSISTED_DATA) long persistedData,
                @JsonProperty(FIELD_NAME_NUM_SUBTASKS) int numSubtasks,
                @JsonProperty(FIELD_NAME_NUM_ACK_SUBTASKS) int numAckSubtasks,
                @JsonProperty(FIELD_NAME_CHECKPOINT_TYPE) RestAPICheckpointType checkpointType,
                @JsonDeserialize(keyUsing = JobVertexIDKeyDeserializer.class)
                        @JsonProperty(FIELD_NAME_TASKS)
                        Map<JobVertexID, TaskCheckpointStatistics> checkpointingStatisticsPerTask,
                @JsonProperty(FIELD_NAME_FAILURE_TIMESTAMP) long failureTimestamp,
                @JsonProperty(FIELD_NAME_FAILURE_MESSAGE) @Nullable String failureMessage) {
            super(
                    id,
                    status,
                    savepoint,
                    savepointFormat,
                    triggerTimestamp,
                    latestAckTimestamp,
                    checkpointedSize,
                    stateSize,
                    duration,
                    alignmentBuffered,
                    processedData,
                    persistedData,
                    numSubtasks,
                    numAckSubtasks,
                    checkpointType,
                    checkpointingStatisticsPerTask);

            this.failureTimestamp = failureTimestamp;
            this.failureMessage = failureMessage;
        }

        public long getFailureTimestamp() {
            return failureTimestamp;
        }

        @Nullable
        public String getFailureMessage() {
            return failureMessage;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }
            FailedCheckpointStatistics that = (FailedCheckpointStatistics) o;
            return failureTimestamp == that.failureTimestamp
                    && Objects.equals(failureMessage, that.failureMessage);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), failureTimestamp, failureMessage);
        }
    }

    /** Statistics for a pending checkpoint. */
    public static final class PendingCheckpointStatistics extends CheckpointStatistics {

        @JsonCreator
        public PendingCheckpointStatistics(
                @JsonProperty(FIELD_NAME_ID) long id,
                @JsonProperty(FIELD_NAME_STATUS) CheckpointStatsStatus status,
                @JsonProperty(FIELD_NAME_IS_SAVEPOINT) boolean savepoint,
                @JsonProperty(FIELD_NAME_SAVEPOINT_FORMAT) String savepointFormat,
                @JsonProperty(FIELD_NAME_TRIGGER_TIMESTAMP) long triggerTimestamp,
                @JsonProperty(FIELD_NAME_LATEST_ACK_TIMESTAMP) long latestAckTimestamp,
                @JsonProperty(FIELD_NAME_CHECKPOINTED_SIZE) long checkpointedSize,
                @JsonProperty(FIELD_NAME_STATE_SIZE) long stateSize,
                @JsonProperty(FIELD_NAME_DURATION) long duration,
                @JsonProperty(FIELD_NAME_ALIGNMENT_BUFFERED) long alignmentBuffered,
                @JsonProperty(FIELD_NAME_PROCESSED_DATA) long processedData,
                @JsonProperty(FIELD_NAME_PERSISTED_DATA) long persistedData,
                @JsonProperty(FIELD_NAME_NUM_SUBTASKS) int numSubtasks,
                @JsonProperty(FIELD_NAME_NUM_ACK_SUBTASKS) int numAckSubtasks,
                @JsonProperty(FIELD_NAME_CHECKPOINT_TYPE) RestAPICheckpointType checkpointType,
                @JsonDeserialize(keyUsing = JobVertexIDKeyDeserializer.class)
                        @JsonProperty(FIELD_NAME_TASKS)
                        Map<JobVertexID, TaskCheckpointStatistics> checkpointingStatisticsPerTask) {
            super(
                    id,
                    status,
                    savepoint,
                    savepointFormat,
                    triggerTimestamp,
                    latestAckTimestamp,
                    checkpointedSize,
                    stateSize,
                    duration,
                    alignmentBuffered,
                    processedData,
                    persistedData,
                    numSubtasks,
                    numAckSubtasks,
                    checkpointType,
                    checkpointingStatisticsPerTask);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode());
        }
    }
}
