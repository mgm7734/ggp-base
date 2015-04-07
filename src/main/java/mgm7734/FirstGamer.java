package mgm7734;

import java.util.List;

import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class FirstGamer extends SampleGamer {

	public int maxDepth = 3;
	public double discountFactor = 0.95;
	public int numProbes = 4;
	public long gracePeriod = 2000;

	private StateMachine sm;
	private long timeLimit;

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		sm = getStateMachine();
		timeLimit = timeout - gracePeriod;
		Move bestMove[] = {null};
		int score = findBestMove(getCurrentState(), 0, 100, 0, bestMove);
		System.out.printf("%s %d\n", bestMove[0], score);
		return bestMove[0];
	}

	private int findBestMove(MachineState state, int alpha, int beta, int level, Move[] bestMove)
			throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		if (sm.isTerminal(state)) {
			return sm.getGoal(state, getRole());
		}
		if (level > maxDepth || System.currentTimeMillis() > timeLimit) {
			double[] avgScore = { 0 };
			double[] avgDepth = { 0 };
			sm.getAverageDiscountedScoresFromRepeatedDepthCharges(
					state, avgScore, avgDepth, discountFactor, numProbes);
			int roleIx = sm.getRoleIndices().get(getRole());
			if (bestMove != null) {
				System.out.printf("depth=%d\n", level);
				bestMove[0] = sm.getRandomMove(state, getRole());
			}
			return (int)Math.round(avgScore[0]);
		}
		for (Move move : sm.getLegalMoves(state, getRole())) {
			int score = findMinScore(move, state, alpha, beta, level);
			if (score > alpha) {
				alpha = score;
				if (bestMove != null) {
					bestMove[0] = move;
				}
				if (alpha >= beta) {
					alpha = beta;
					break;
				}
			}
		}
		return alpha;
	}

	private int findMinScore(Move move, MachineState state, int alpha, int beta, int level)
			throws MoveDefinitionException, TransitionDefinitionException,
			GoalDefinitionException {
		for (List<Move> joinMove : sm.getLegalJointMoves(state, getRole(), move)) {
			MachineState nextState = sm.getNextState(state, joinMove);
			int score = findBestMove(nextState, alpha, beta, level+1, null);
			if (score < beta) {
				beta = score;
				if (beta <= alpha) {
					return alpha;
				}
			}
		}
		return beta;
	}

}
