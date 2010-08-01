package org.cocolab.inpro.incremental.listener;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.cocolab.inpro.incremental.PushBuffer;
import org.cocolab.inpro.incremental.unit.EditMessage;
import org.cocolab.inpro.incremental.unit.EditType;
import org.cocolab.inpro.incremental.unit.IU;

public class IUNetworkToDOT extends PushBuffer {

	PrintStream outStream = System.out;

	@Override
	public void hypChange(Collection<? extends IU> ius,
			List<? extends EditMessage<? extends IU>> edits) {
		if (edits != null && !edits.isEmpty() && edits.get(0).getType().equals(EditType.COMMIT)) {
			Set<IU> processedIUs = new HashSet<IU>();
			Queue<IU> iuProcessingQueue = new ArrayDeque<IU>();
			List<IU> iuList = new ArrayList<IU>(ius);
			Collections.reverse(iuList);
			iuProcessingQueue.addAll(iuList);
			
			printHead();
			while (!iuProcessingQueue.isEmpty()) {
				IU iu = iuProcessingQueue.remove();
				if (!processedIUs.contains(iu)) {
					printNode(iu);
					IU sll = iu.getSameLevelLink();
					if (sll != null) {
						printSLL(iu, sll);
						iuProcessingQueue.add(sll);
					}
					List<? extends IU> grin = iu.groundedIn();
					if (grin != null) {
						for (IU gr : grin) {
							printGrin(iu, gr);
						}
						iuProcessingQueue.addAll(grin);
					}
				}
				processedIUs.add(iu);
			}
			printTail();
		}
	}
	
	private void printHead() {
		outStream.println("digraph iunetwork {");
		outStream.println("rankdir=RL;");
	}

	private void printNode(IU iu) {
		int id = iu.getID();
		String label = iu.getClass().getSimpleName() + "\\n" + iu.toPayLoad();
		outStream.println("\"" + id + "\" [label=\"" + label + "\" shape=box, style=rounded];");
	}
	
	private void printSLL(IU iu, IU sll) {
		int id1 = iu.getID();
		int id2 = sll.getID();
		outStream.println("\"" + id1 + "\" -> \"" + id2 + "\";");
	}

	private void printGrin(IU iu, IU gr) {
		int id1 = iu.getID();
		int id2 = gr.getID();
		outStream.println("\"" + id1 + "\" -> \"" + id2 + "\" [constraint=false];");
	}
	
	private void printTail() {
		outStream.println("}");
	}

}
