/*
 *  Copyright 2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Amazon Software License (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at
 *
 *  http://aws.amazon.com/asl/
 *
 *  or in the "license" file accompanying this file. This file is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */
package software.amazon.kinesis.lifecycle;

import lombok.AllArgsConstructor;
import software.amazon.kinesis.lifecycle.events.LeaseLost;
import software.amazon.kinesis.lifecycle.events.RecordsReceived;
import software.amazon.kinesis.lifecycle.events.ShardCompleted;
import software.amazon.kinesis.lifecycle.events.ShutdownRequested;
import software.amazon.kinesis.lifecycle.events.Started;
import software.amazon.kinesis.processor.IRecordProcessor;
import software.amazon.kinesis.processor.IRecordProcessorCheckpointer;
import software.amazon.kinesis.processor.IShutdownNotificationAware;

@AllArgsConstructor
public class RecordProcessorShim implements RecordProcessorLifecycle {

    private final IRecordProcessor delegate;

    @Override
    public void started(Started started) {
        delegate.initialize(started.toInitializationInput());
    }

    @Override
    public void recordsReceived(RecordsReceived records) {
        delegate.processRecords(records.toProcessRecordsInput());
    }

    @Override
    public void leaseLost(LeaseLost leaseLost) {
        ShutdownInput shutdownInput = new ShutdownInput() {
            @Override
            public IRecordProcessorCheckpointer getCheckpointer() {
                throw new UnsupportedOperationException("Cannot checkpoint when the lease is lost");
            }
        }.withShutdownReason(ShutdownReason.ZOMBIE);

        delegate.shutdown(shutdownInput);
    }

    @Override
    public void shardCompleted(ShardCompleted shardCompleted) {
        ShutdownInput shutdownInput = new ShutdownInput().withCheckpointer(shardCompleted.getCheckpointer())
                .withShutdownReason(ShutdownReason.TERMINATE);
        delegate.shutdown(shutdownInput);
    }

    @Override
    public void shutdownRequested(ShutdownRequested shutdownRequested) {
        if (delegate instanceof IShutdownNotificationAware) {
            IShutdownNotificationAware aware = (IShutdownNotificationAware)delegate;
            aware.shutdownRequested(shutdownRequested.getCheckpointer());
        }
    }
}