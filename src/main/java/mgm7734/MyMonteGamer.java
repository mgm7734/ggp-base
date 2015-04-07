package mgm7734;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class MyMonteGamer extends SampleGamer {

	// Parameters to adjust
	static final int gracePeriod = 1000;
	static final double exploreWt = 2;
	static final double discountFactor = 0.95;
	static final int numProbes = 5;

	Random rand = new Random(System.currentTimeMillis());

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		return mcts(timeout);
	}

	Move mcts(long timeout)
			throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		Node root = new Node(null, null);
		while (System.currentTimeMillis() + gracePeriod  < timeout) {
			System.out.println(">>> mcts iteration start");
			MachineState[] state = { getCurrentState() };
			Node currentNode = select(root, state);
			expand(currentNode, state[0]);
			currentNode = simulate(currentNode, state[0]);
			backPropagate(currentNode);
			root.dump();
		}
		Move bestMove = selectFinalMove(root);
		return bestMove;
	}

	private Move selectFinalMove(Node root) {
		Move bestMove = null;
		double bestUtility = -1;
		for (Node node : root.children) {
			if (node.utility > bestUtility) {
				bestUtility = node.utility;
				bestMove = node.jointMove.get(0);
			}
		}
		return bestMove;
	}

 	Node select(Node root, MachineState[] state)
 			throws TransitionDefinitionException {
 		Node node = root;
		while (true) {
			if (node.visits == 0)
				return node;
			for (Node child : node.children) {
				if (child.visits == 0) {
					state[0] = getStateMachine().getNextState(state[0], child.jointMove);
					return child;
				}
			}
			double score = -1;
			for (Node child : node.children) {
				if (child.terminal) continue;
				double newScore = score(child);
				if (newScore > score) {
					score = newScore;
					state[0] = getStateMachine().getNextState(state[0], child.jointMove);
					node = child;
				}
			}
		}
	}

 	private void expand(Node node, MachineState state)
 			throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
 		if (node.terminal) return;
 		for (List<Move> joinMove : getStateMachine().getLegalJointMoves(state)) {
			node.children.add(new Node(joinMove, node));
		}
	}

	private Node simulate(Node node, MachineState state)
			throws TransitionDefinitionException, MoveDefinitionException, GoalDefinitionException {
		if (node.terminal) return node;
		Node child = node.children.get(rand.nextInt(node.children.size()));
		MachineState childState = getStateMachine().getNextState(state, child.jointMove);
		double[] avgScore = {0};
		double[] avgDepth = {0};
		getStateMachine().getAverageDiscountedScoresFromRepeatedDepthCharges(childState, avgScore, avgDepth, discountFactor, numProbes);
		child.utility = (float)(avgScore[0] / 100 - 0.5) ;
		child.terminal = getStateMachine().isTerminal(childState);
		return child;
	}

	private void backPropagate(Node node) {
		double score = node.utility;
		while (node.parent != null) {
			node = node.parent;
			++node.visits;
			node.utility += score;
		}
	}

	double score(Node node) {
		return (node.utility / node.visits + Math.sqrt(exploreWt * Math.log(node.parent.visits) / node.visits)) ;
	}

	static class Node {
		public int visits = 0;
		public float utility = 0;
		public List<Node> children = new ArrayList<Node>();
		boolean terminal = false;

		public List<Move> jointMove;
		public Node parent;

		public Node(List<Move> jointMove, Node parent) {
			this.jointMove = jointMove;
			this.parent = parent;
		}

		@Override
		public String toString() {
			String move = jointMove == null ? "root" : jointMove.get(0).toString();
			return String.format("Node[id=%d, %s, utility=%g, visits=%d, #child=%d]",
					hashCode(), move, utility, visits, children.size());
		}

		public void dump() {
			dump(0);
		}
		void dump(int level) {
			for(int i = 0; i < level; ++i) System.out.printf("  ");
			System.out.println(this.toString());
			for( Node child : children) child.dump(level+1);
		}
	}
}
