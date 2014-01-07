package inpro.incremental.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.util.JAXBResult;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import inpro.incremental.unit.SysInstallmentIU;
import inpro.incremental.util.TTSUtil.Paragraph;
import inpro.synthesis.MaryAdapter;

import marytts.htsengine.HTSModel;

import org.junit.Test;

import com.sun.xml.internal.ws.policy.privateutil.PolicyUtils.Collections;

public class TTSUtilTest {

	@Test
	public void test() throws JAXBException, TransformerException, IOException {
		MaryAdapter ma = MaryAdapter.getInstance();

		String testUtterance = "Nimm bitte das rote Kreuz.";
		InputStream is = ma.text2maryxml(testUtterance);
		BufferedReader in = new BufferedReader(new InputStreamReader(is));
		String line = null;
		while((line = in.readLine()) != null) {
			System.err.println(line); // produce reference output
		}
		is = ma.text2maryxml(testUtterance);
		
		TTSUtil.wordIUsFromMaryXML(is, null);

		JAXBContext context = JAXBContext.newInstance(Paragraph.class);
		JAXBResult result = new JAXBResult(context);
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer t = tf.newTransformer(new StreamSource(TTSUtil.class.getResourceAsStream("mary2simple.xsl")));
		is = ma.text2maryxml(testUtterance);
		t.transform(new StreamSource(is), result);
		
		Paragraph paragraph = (Paragraph) result.getResult(); //unmarshaller.unmarshal(is);
		
		System.err.println(paragraph.toString());
		Marshaller marshaller = context.createMarshaller();
		marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
		marshaller.marshal(paragraph, System.out);
		System.out.println((new SysInstallmentIU(testUtterance)).deepToString());
	}
	
}
