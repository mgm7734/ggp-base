package mgm7734;

import java.util.Arrays;
import java.util.List;

import org.ggp.base.util.game.GameRepository;
import org.ggp.base.util.gdl.grammar.GdlPool;
import org.ggp.base.util.gdl.grammar.GdlTerm;
import org.ggp.base.util.match.Match;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class FirstGamerTest extends Assert {
	private static final int TIMEOUT = 1000;
	private HookedFirstGamer gx;
	private Match ticTacToe;

	class HookedFirstGamer extends FirstGamer {
	}

	@Before
	public void setup() throws Exception {
        ticTacToe = new Match("", -1, TIMEOUT, TIMEOUT, GameRepository.getDefaultRepository().getGame("ticTacToe"));
        gx = new HookedFirstGamer();
        gx.setMatch(ticTacToe);
        gx.setRoleName(GdlPool.getConstant("xplayer"));
        gx.metaGame(TIMEOUT);
	}

	@Test
	public void testSanityChecks() throws Exception {
		assertEquals("HookedFirstGamer", gx.getName());

        assertTrue(gx.selectMove(TIMEOUT) != null);
	}

	@Test
	public void testFullGame() throws Exception {
        HookedFirstGamer go = new HookedFirstGamer();
        go.setMatch(ticTacToe);
        go.setRoleName(GdlPool.getConstant("oplayer"));
        go.metaGame(TIMEOUT);

		for (int i = 0; i < 9; ++i) {
			try {
				List<GdlTerm> moves = Arrays.asList(gx.selectMove(TIMEOUT),
						go.selectMove(TIMEOUT));
				ticTacToe.appendMoves(moves);
				// System.out.printf("%d: %s\n", i, moves);

			} catch (Exception e) {
				fail("Must play 9 moves, got:" + e);
			}
		}
	}

}
