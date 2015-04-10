package mgm7734;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.Role;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

/*
 * # Notes
 * Assume sequential play, order determined by getRoles().
 * Won't work for multi-player with simultaneous moves or variable order.
 */

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

		if (!activeRole(getCurrentState()).equals(getRole())) {
			return getStateMachine().getRandomMove(getCurrentState(), getRole());
		}
		return mcts(timeout);
	}

	private Move mcts(long timeout)
			throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		Node root = new Node(null, null);
		expand(root, getCurrentState(), getRole());
		root.visits = 1;
		System.out.printf("%d, %d, %d\n", System.currentTimeMillis(), timeout, timeout - System.currentTimeMillis());
		while (System.currentTimeMillis() + gracePeriod  < timeout) {
			MachineState[] state = { getCurrentState() };
			Role[] role = {getRole()};
			Node node = select(root, state, role);
			if (node == null)
				break;
			expand(node, state[0], role[0]);
			MachineState result = simulate(node, state[0]);
			backPropagate(result, node, role[0]);
		}
		System.out.println(getCurrentState());
		dump(root, getRole(), 0);
		Move bestMove = selectFinalMove(root);
		return bestMove;
	}

	private Node select(Node node, MachineState[] state, Role[] role)
			throws TransitionDefinitionException, MoveDefinitionException {
		for (Node child : node.children) {
			if (child.visits == 0) {
				applyMove(child.move, state, role);
				return child;
			}
		}
		if (getStateMachine().isTerminal(state[0]))
			return null;

		double maxScore = -1;
		for (Node child : node.children) {
			double newScore = score(child);
			if (newScore > maxScore) {
				maxScore = newScore;
				node = child;
			}
		}
		applyMove(node.move, state, role);
		return select(node, state, role);
	}

	private void expand(Node node, MachineState state, Role role)
 			throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
 		if (getStateMachine().isTerminal(state)) return;
 		for (Move move : getStateMachine().getLegalMoves(state, role)) {
			node.children.add(new Node(move, node));
		}
	}

	private MachineState simulate(Node node, MachineState state)
			throws TransitionDefinitionException, MoveDefinitionException {
		return getStateMachine().performDepthCharge(state, null);
	}

	private void backPropagate(MachineState result, Node node, Role role)
			throws GoalDefinitionException {
		for ( ; node != null ; node = node.parent, role = nextRole(role, -1)) {
			++node.visits;
			node.utility += getStateMachine().getGoal(result, role) / 100.0;
		}
	}

	private Move selectFinalMove(Node root) {
		Move bestMove = null;
		double bestUtility = -1;
		for (Node node : root.children) {
			if (node.utility > bestUtility) {
				bestUtility = node.utility;
				bestMove = node.move;
			}
		}
		return bestMove;
	}

	double score(Node node) {
		return (node.utility / node.visits + Math.sqrt(Math.log(exploreWt * node.parent.visits) / node.visits)) ;
	}

	private void applyMove(Move move, MachineState[] state, Role[] role)
			throws MoveDefinitionException, TransitionDefinitionException {
		List<List<Move>> legalJointMoves = getStateMachine().getLegalJointMoves(state[0], role[0], move);
		assert legalJointMoves.size() == 1;
		state[0] = getStateMachine().getNextState(state[0], legalJointMoves.get(0));
		role[0] = nextRole(role[0], 1);
	}

	private Role nextRole(Role role, int direction) {
		int roleIx = getStateMachine().getRoleIndices().get(role);
		int numRoles = getStateMachine().getRoles().size();
		roleIx = (roleIx + direction + numRoles) % numRoles;
		return getStateMachine().getRoles().get(roleIx);
	}

	private Role activeRole(MachineState state) throws MoveDefinitionException {
		List<Move> aJointMove = getStateMachine().getRandomJointMove(state);
		while(true) {
			List<Move> anotherJointMove = getStateMachine().getRandomJointMove(state);
			for (int i = 0 ; i < aJointMove.size(); ++i) {
				if (!aJointMove.get(i).equals(anotherJointMove.get(i))) {
					return getStateMachine().getRoles().get(i);
				}
			}
		}
	}

	void dump(Node node, Role role, int level) {
		for(int i = 0; i < level; ++i) System.out.printf("  ");
		System.out.printf("%s %s\n", node.toString(), role);

		for( Node child : node.children) dump(child,  nextRole(role, 1), level+1);

	}

	static class Node {
		public int visits = 0;
		public float utility = 0;
		public List<Node> children = new ArrayList<Node>();
		boolean terminal = false;

		public Move move;
		public Node parent;

		public Node(Move move, Node parent) {
			this.move = move;
			this.parent = parent;
		}

		@Override
		public String toString() {
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
