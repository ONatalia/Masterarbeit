package inpro.nlu;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;

import org.junit.Test;

public class AVMComposerTest {

	@Test
	public void test() throws MalformedURLException {
		//For debug only...
		System.out.println("Starting AVM Composer.");
		AVMComposer composer = new AVMComposer(AVMComposerTest.class.getResource("AVMStructure").toString(), 
											   AVMComposerTest.class.getResource("AVMWorldList").toString());
//		System.out.println("World contains following objects:");
//		for (AVM avm : composer.worldList) {
//			System.out.println(avm.toString());
//		}

		// Below is a demonstration of what should happen when tags come in.

		ArrayList<AVPair> avps = new ArrayList<AVPair>();
		
		avps.add(new AVPair("act", "take"));
		avps.add(new AVPair("yesno", "yes"));
		avps.add(new AVPair("yesno", "no"));

		for (AVPair avp : avps) {
			System.out.println("Adding tag AVPair '" + avp.toString() + "'.");
			if (composer.getAvmList() != null) {
				composer.compose(avp);
				composer.printAVMs();
			} 
			if (composer.getAvmList() != null) {
				ArrayList<AVM> resolvedList = composer.resolve();
				if (resolvedList.size() > 0) {
					System.out.println("Found these that resolve...");
					for (AVM avm : resolvedList)
					System.out.println(avm.toString());
				} else {
					System.out.println("Nothing resolves...");
				}
			}
		}
		System.out.println();
		System.out.println("Done! Continue composing by entering any AVPair (e.g. ord:1). Enter 'exit' to stop or 'new' to restart'");
	}
	
	static void interactiveTest() throws IOException {
		BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
		String line = stdin.readLine();
		AVMComposer composer = new AVMComposer(AVMComposerTest.class.getResource("AVMStructure").toString(), 
				   AVMComposerTest.class.getResource("AVMWorldList").toString());
		while (!line.equals("exit")) {
			if (line.equals("new")) {  
				composer = new AVMComposer(AVMComposerTest.class.getResource("AVMStructure").toString(), 
						   AVMComposerTest.class.getResource("AVMWorldList").toString());
			} else {
				composer.compose(new AVPair(line));
			}
			composer.printAVMs();
			line = stdin.readLine();
		}
		System.exit(0);
	}

}
