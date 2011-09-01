package stmbench7.correctness.opacity;

import java.util.ArrayList;

import stmbench7.BenchThread;
import stmbench7.OperationId;
import stmbench7.Setup;
import stmbench7.ThreadRandom;
import stmbench7.annotations.NonAtomic;
import stmbench7.core.OperationFailedException;
import stmbench7.core.RuntimeError;

/**
 * Replays sequentially a concurrent execution. Used to check
 * whether a given concurrent execution ensures opacity, i.e.,
 * whether the synchronization method used in the execution
 * was correctly synchronizing threads during this execution.
 */
@NonAtomic
public class SequentialReplayThread extends BenchThread {

	public SequentialReplayThread(Setup setup, double[] operationCDF,
			ArrayList<ReplayLogEntry> replayLog) {
		super(setup, operationCDF);
		this.replayLog = replayLog;
	}

	public void run() {
		int i = 1;
		//for(ReplayLogEntry entry : replayLog)
		//	System.err.println(i++ + " @ " + OperationId.values()[entry.opNum] + "\t--\t" + entry.timestamp + "\t" + entry.threadNum + "." + entry.localTs + "\t" + entry.readOnly);
		i=0;
		int opNum = 1, numOfOps = replayLog.size();
		for(ReplayLogEntry entry : replayLog) {
			System.err.print("Operation " + (opNum++) + " out of " + numOfOps + "\r");
			short threadNum = entry.threadNum;
			ThreadRandom.setVirtualThreadNumber(threadNum);

			//System.out.println(++i);
			int operationNumber = getNextOperationNumber();

			//System.out.println(entry.threadNum + " -- " + OperationId.values()[entry.opNum] +
			//	" / " + OperationId.values()[operationNumber]);
			if(operationNumber != entry.opNum)
				throw new RuntimeError("ThreadRandom skew");

			int result = 0;
			boolean failed = false;

			try {
				// JVSTM: Considering how the benchmark works, it makes no sense
				// 	to save the state of the pseudo-random generator state
				//	and then restore it if the operation fails.
				//ThreadRandom.saveState();
				result = operations[operationNumber].execute();
			}
			catch(OperationFailedException e) {
				failed = true;
				// JVSTM: Read comment above.
				//ThreadRandom.restoreState();
			}

			if(result != entry.result || failed != entry.failed) {
				String opName = OperationId.values()[operationNumber].toString();
				throw new RuntimeError("Different operation result in the sequential execution (" +
						"operation " + opName + "): " +
						"Sequential: result = " + result + ", failed = " + failed + ". " +
						"Concurrent: result = " + entry.result + ", failed = " + entry.failed + ".");
			}
		}
		System.err.println();
	}
}
