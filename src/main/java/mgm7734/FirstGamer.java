package mgm7734;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.ggp.base.player.gamer.statemachine.sample.SampleGamer;
import org.ggp.base.util.statemachine.MachineState;
import org.ggp.base.util.statemachine.Move;
import org.ggp.base.util.statemachine.StateMachine;
import org.ggp.base.util.statemachine.exceptions.GoalDefinitionException;
import org.ggp.base.util.statemachine.exceptions.MoveDefinitionException;
import org.ggp.base.util.statemachine.exceptions.TransitionDefinitionException;

public class FirstGamer extends SampleGamer {

	private StateMachine sm;

	class MoveScore {
		public Move move;
		public int score;
	}

	@Override
	public Move stateMachineSelectMove(long timeout)
			throws TransitionDefinitionException, MoveDefinitionException,
			GoalDefinitionException {
		sm = getStateMachine();
		MoveScore best = findBestMove(getCurrentState(), 0, 100);
		return best.move;
	}

	private MoveScore findBestMove(MachineState state, int alpha, int beta)
			throws MoveDefinitionException, TransitionDefinitionException, GoalDefinitionException {
		MoveScore result = new MoveScore();
		Map<Move, List<MachineState>> nextStates = sm.getNextStates(state, getRole());
		for (Entry<Move, List<MachineState>> entry : nextStates.entrySet()) {
			List<MachineState> statesForMove = entry.getValue();
			int score = findMinScore(statesForMove, alpha, beta);
			if (score > alpha) {
				alpha = score;
				result.move = entry.getKey();
				if (alpha >= beta) {
					result.score = beta;
					return result;
				}
			}
		}
		result.score = alpha;
		return result;
	}

	private int findMinScore(List<MachineState> states, int alpha, int beta)
			throws MoveDefinitionException, TransitionDefinitionException,
			GoalDefinitionException {
		for (MachineState state : states) {
			int score = sm.isTerminal(state) ? sm.getGoal(state, getRole())
											 : findBestMove(state, alpha, beta).score;
			beta = Math.min(beta, score);
			if (beta <= alpha) {
				return alpha;
			}
		}
		return beta;
	}

}
