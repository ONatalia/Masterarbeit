package inpro.pitch.util;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class ShortestPathTest {

	@Test
	public void testCalculate() {
		Integer[] nodes = new Integer[5];
		for (int i = 0; i < 5; i++) {
			nodes[i] = Integer.valueOf(i);
		}
		ShortestPath<Integer> sp = new ShortestPath<Integer>();
		sp.connect(nodes[0], nodes[1], 2);
		sp.connect(nodes[0], nodes[2], 4);
		sp.connect(nodes[0], nodes[3], 7);
		sp.connect(nodes[1], nodes[3], 3);
		sp.connect(nodes[2], nodes[3], 2);
		sp.connect(nodes[2], nodes[4], 3);
		sp.connect(nodes[3], nodes[4], 1);
		sp.setStart(nodes[0]);
		sp.setTarget(nodes[4]);
		List<Integer> result = sp.calculate();
		
		assertEquals(result, Arrays.<Integer>asList(0, 1, 3, 4));
	}

}
