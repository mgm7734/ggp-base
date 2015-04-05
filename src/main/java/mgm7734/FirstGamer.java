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

	private StateMachine sm;

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		Move bestMove[] = {null};
		sm = getStateMachine();
		int score = findBestMove(getCurrentState(), 0, 100, bestMove);
//		System.out.printf("%s %d\n", bestMove[0], score);
		return bestMove[0];
	}

	private int findBestMove(MachineState state, int alpha, int beta, Move[] bestMove)
			throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		if (sm.isTerminal(state)) {
			return sm.getGoal(state, getRole());
		}
		for (Move move : sm.getLegalMoves(state, getRole())) {
			int score = findMinScore(move, state, alpha, beta);
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

	private int findMinScore(Move move, MachineState state, int alpha, int beta)
			throws MoveDefinitionException, TransitionDefinitionException,
			GoalDefinitionException {
		for (List<Move> joinMove : sm.getLegalJointMoves(state, getRole(), move)) {
			MachineState nextState = sm.getNextState(state, joinMove);
			int score = findBestMove(nextState, alpha, beta, null);
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
