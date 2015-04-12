package mgm7734;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.gdl.grammar.GdlTerm;
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
	static final int gracePeriod = 500;
	static final double exploreWt = 2;
	static final double discountFactor = 0.95;
//	static final int numProbes = 5;

	Random rand = new Random(System.currentTimeMillis());
	private Node previousChoice;

	@Override
	public void stateMachineMetaGame(long timeout) {
		previousChoice = null;
	}

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
		Node root = initializeRoot();
		System.out.printf("%d, %d, %d\n", System.currentTimeMillis(), timeout, timeout - System.currentTimeMillis());
		while (System.currentTimeMillis() + gracePeriod  < timeout) {
			MachineState[] state = { getCurrentState() };
			Role[] role = {getRole()};
			Node node = select(root, state, role);
			if (node == null) {
				break;
			}
			expand(node, state[0], role[0]);

			int[] depth = {-1};
			MachineState result = simulate(node, state[0], depth);
			backPropagate(result, depth, node, role[0]);
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
		if (getStateMachine().isTerminal(state[0])) {
			System.out.println(">>>>>>>>>>>>>>>>>>> Terminal node breakl");
			return null;
		}

		double maxScore = -1;
		Node nextNode = null;
		for (Node child : node.children) {
			double newScore = score(child);
			if (newScore > maxScore) {
				MachineState[] tmpState = {state[0]};
				Role[] tmpRole = {role[0]};
				applyMove(child.move, tmpState, tmpRole);
				if (!getStateMachine().isTerminal(tmpState[0])) {
					maxScore = newScore;
					nextNode = child;
				}
			}
		}
		if (nextNode == null) {
			return node;
		}
		applyMove(nextNode.move, state, role);
		return select(nextNode, state, role);
	}

	private void expand(Node node, MachineState state, Role role)
 			throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		try {
		if (getStateMachine().isTerminal(state)) return;
		List<Move> legalMoves = getStateMachine().getLegalMoves(state, role);
		node.children = new ArrayList<Node>(legalMoves.size());
		for (Move move : legalMoves) {
			node.children.add(new Node(move, node));
		}
		} catch(MoveDefinitionException ex) {
			System.out.println("aha!");
		}
	}

	private MachineState simulate(Node node, MachineState state, int[] depth)
			throws TransitionDefinitionException, MoveDefinitionException {
		return getStateMachine().performDepthCharge(state, depth);
	}

	private void backPropagate(MachineState result, int[] depth, Node node, Role role)
			throws GoalDefinitionException {
		final double accumulatedDiscountFactor = Math.pow(discountFactor, depth[0]);
		for ( ; node != null ; node = node.parent, role = nextRole(role, -1)) {
			++node.visits;
			node.utility += getStateMachine().getGoal(result, role)* discountFactor / 100.0;
		}
	}

	private Move selectFinalMove(Node root) {
		Move bestMove = null;
		previousChoice = null;
		double bestUtility = -1;
		for (Node node : root.children) {
			if (node.utility > bestUtility) {
				bestUtility = node.utility;
				bestMove = node.move;
				previousChoice = node;
			}
		}
		return bestMove;
	}

	private Node initializeRoot() throws MoveDefinitionException,
			TransitionDefinitionException, GoalDefinitionException {

		List<GdlTerm> prevJointMove = getMatch().getMostRecentMoves();
		int myRoleIx = getStateMachine().getRoleIndices().get(getRole());

		Node node = previousChoice;
		int roleIx = myRoleIx;

		while(true) {
			if (node == null) {
				node = new Node(null, null);
				expand(node, getCurrentState(), getRole());
				node.visits = 1;
				return node;
			}
			roleIx = (roleIx+1) % prevJointMove.size();
			if (roleIx == myRoleIx) {
				System.out.println("**** Found pre-existing tree!");
				return node;
			}
			Node matchingChild = null;
			for (Node child : node.children) {
				if (child.move.getContents().equals(prevJointMove.get(roleIx))) {
					matchingChild = child;
					break;
				}
			}
			node = matchingChild;
		}
	}


	static double score(Node node) {
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
		if (node.children != null)
			for( Node child : node.children) {
				if (child.visits > 0)
				dump(child,  nextRole(role, 1), level+1);
			}

	}

	static class Node {
		public int visits = 0;
		public float utility = 0;
		public List<Node> children;

		public Move move;
		public Node parent;

		public Node(Move move, Node parent) {
			this.move = move;
			this.parent = parent;
		}

		@Override
		public String toString() {
			double score = visits>0 && parent != null ? score(this) : -1;
			int numKids = children == null ? -1 : children.size();
			double value = (visits > 0) ? utility / visits : -1;
			return String.format("Node[id=%d, %s, value=%g, score=%g visits=%d, #child=%d]",
					hashCode(), move, value, score, visits, numKids);
		}

		public void dump() {
			dump(0);
		}
		void dump(int level) {
			for(int i = 0; i < level; ++i) System.out.printf("  ");
			System.out.println(this.toString());
			for( Node child : children) if (child.children != null) child.dump(level+1);
		}
	}
}
