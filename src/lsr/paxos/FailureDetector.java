package lsr.paxos;

import java.util.BitSet;
import java.util.logging.Logger;

import lsr.common.Dispatcher;
import lsr.common.Dispatcher.Priority;
import lsr.common.Dispatcher.PriorityTask;
import lsr.paxos.messages.Alive;
import lsr.paxos.messages.Message;
import lsr.paxos.messages.MessageType;
import lsr.paxos.network.MessageHandler;
import lsr.paxos.network.Network;
import lsr.paxos.storage.Storage;

/**
 * Represents failure detector thread. If the current process is the leader,
 * then this class is responsible for sending <code>ALIVE</code> message every
 * amount of time. Otherwise is responsible for suspecting the leader. If there
 * is no message received from leader, then the leader is suspected to crash,
 * and <code>Paxos</code> is notified about this event.
 * 
 */
class FailureDetector {
	/** How long to wait until suspecting the leader. In milliseconds */
	private final static int SUSPECT_TO = 5000;
	/** How long the leader waits until sending heartbeats. In milliseconds */
	private final static int SEND_TO = 1000;

	private final Dispatcher fdDispatcher;
	private final Network network;
	private final Paxos paxos;
	private final Storage storage;
	private MessageHandler innerListener;

	/* Either the suspect task or the send alive tasks */
	private PriorityTask task = null;

	/**
	 * Initializes new instance of <code>FailureDetector</code>.
	 * 
	 * @param paxos
	 *            - the paxos which should be notified about suspecting leader
	 * @param network
	 *            - used to send and receive messages
	 * @param storage
	 *            - storage containing all data about paxos
	 */
	public FailureDetector(Paxos paxos, Network network, Storage storage) {
		fdDispatcher = paxos.getDispatcher();
		innerListener = new InnerMessageHandler();
		this.network = network;
		// Any message received from the leader serves also as an ALIVE msg.
		Network.addMessageListener(MessageType.ANY, innerListener);
		// Sent messages used when in leader role: also count as ALIVE msg
		// so don't reset sending timeout.
		Network.addMessageListener(MessageType.SENT, innerListener);
		this.paxos = paxos;
		this.storage = storage;
	}

	public void start() {
		_logger.info("Starting failure detector");
		scheduleTask();
	}

	public void stop() {
		cancelTask();
	}

	/**
	 * Updates state of failure detector, due to leader change.
	 * 
	 * Called whenever the leader changes.
	 * 
	 * @param newLeader
	 *            - process id of the new leader
	 */
	public synchronized void leaderChange(int newLeader) {
		assert paxos.getDispatcher().amIInDispatcher();

		resetTimerTask();
	}

	private void scheduleTask() {
		assert task == null;
		// Sending alive messages takes precedence over other messages
		if (paxos.isLeader()) {
			task = fdDispatcher.scheduleAtFixedRate(new SendTask(),
					Priority.High, 0, SEND_TO);
		} else {
			task = fdDispatcher.schedule(new SuspectTask(), Priority.Normal,
					SUSPECT_TO);
		}
	}

	private void cancelTask() {
		if (task != null) {
			task.cancel();
			task = null;
		}
	}

	private void resetTimerTask() {
		cancelTask();
		scheduleTask();
	}

	private class SuspectTask implements Runnable {
		public void run() {
			assert fdDispatcher.amIInDispatcher();
			// The current leader is suspected to be crashed. We try to become a
			// leader.
			_logger.warning("Suspecting leader: " + paxos.getLeaderId());
			paxos.startProposer();
		}
	}

	private class SendTask implements Runnable {
		public void run() {
			assert fdDispatcher.amIInDispatcher();
			Alive alive = new Alive(storage.getStableStorage().getView(),
					storage.getLog().getNextId());
			network.sendToAll(alive);
		}
	}

	/**
	 * Intersects any message sent or received, used to reset the timeouts for
	 * sending and receiving ALIVE messages.
	 * 
	 * @author Nuno Santos (LSR)
	 * 
	 */
	private class InnerMessageHandler implements MessageHandler {
		public void onMessageReceived(Message message, final int sender) {
			// Do not hold a final reference to the full message
			final int view = message.getView();
			fdDispatcher.dispatch(new Runnable() {
				public void run() {
					// If we are the leader, we ignore this message
					// accessing storage not from DispatcherThread - check
					// correctness
					if (!paxos.isLeader()
							&& view == storage.getStableStorage().getView()
							&& sender == paxos.getLeaderId()) {
						resetTimerTask();
					}
				}
			}, Priority.High);
		}

		public void onMessageSent(Message message, final BitSet destinations) {
			final MessageType msgType = message.getType();
			fdDispatcher.dispatch(new Runnable() {
				public void run() {
					if (msgType == MessageType.Alive) {
						// No need to reset the timer if we just sent an alive
						// message
						return;
					}
					// If we are the leader and we sent a message to all, reset
					// the timeout.
					if (destinations.cardinality() == storage.getN()
							&& paxos.isLeader()) {
						resetTimerTask();
					}
				}
			});
		}
	}

	private final static Logger _logger = Logger
			.getLogger(FailureDetector.class.getCanonicalName());
}
