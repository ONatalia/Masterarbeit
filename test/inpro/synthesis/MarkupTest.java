package inpro.synthesis;

import inpro.apps.SimpleMonitor;
import inpro.audio.AudioUtils;
import inpro.audio.DispatchStream;
import inpro.incremental.unit.IU;
import inpro.incremental.unit.PhraseIU;
import inpro.incremental.unit.SegmentIU;
import inpro.synthesis.hts.IUBasedFullPStream;
import inpro.synthesis.hts.VocodingAudioStream;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


/** test ability to synthesize from marked-up text */
public class MarkupTest {
	
	DispatchStream d;

	@Before public void setup() {
		System.setProperty("inpro.tts.language", "de");
		System.setProperty("inpro.tts.voice", "bits1-hsmm");
		MaryAdapter.getInstance();
		d = SimpleMonitor.setupDispatcher();
	}
	
	@After public void waitUntilDone() {
		// wait for synthesis:
		d.waitUntilDone();
	}
	
	@Test
	public void testNoProsody() {
		d.playTTS("Dies ist noch ein Satz, mach Platz.", true);
		waitUntilDone();
	}

	@Test public void testBasicProsody() {
		// test some prosodic changes:
		List<PhraseIU> phraseIUs = MaryAdapter.getInstance().text2PhraseIUs("Dies ist <t accent='H*'>noch</t> ein <t accent='none'>Satz</t>, mach Platz.");
		d.playStream(AudioUtils.get16kAudioStreamForVocodingStream(new VocodingAudioStream(new IUBasedFullPStream(phraseIUs.get(0)), MaryAdapter5internal.getDefaultHMMData(), true)), true);
	}

	@Test public void testFullProsodySimple() {
		// mary's "Willkommen..." with all details in the XML
		String lotsOfDetail = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><maryxml xmlns=\"http://mary.dfki.de/2002/MaryXML\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"0.5\" xml:lang=\"de\">\n<p>\n<s>\n<phrase>\n<t accent=\"L+H*\" g2p_method=\"lexicon\" ph=\"v I l - ' k O - m @ n\" pos=\"NN\">\nWillkommen\n<syllable ph=\"v I l\">\n<ph d=\"75\" end=\"0.074999996\" f0=\"(9,163)(18,161)(27,160)(36,159)(45,160)(54,161)(63,162)(72,164)(81,166)(90,170)(100,173)\" p=\"v\"/>\n<ph d=\"60\" end=\"0.13499999\" f0=\"(8,176)(16,178)(25,180)(33,181)(41,182)(50,183)(58,183)(66,183)(75,182)(83,179)(91,175)(100,173)\" p=\"I\"/>\n<ph d=\"60\" end=\"0.195\" f0=\"(8,174)(16,176)(25,177)(33,178)(41,177)(50,177)(58,176)(66,175)(75,173)(83,170)(91,167)(100,164)\" p=\"l\"/>\n</syllable>\n<syllable accent=\"L+H*\" ph=\"k O\" stress=\"1\">\n<ph d=\"110\" end=\"0.305\" p=\"k\"/>\n<ph d=\"70\" end=\"0.375\" f0=\"(7,185)(14,184)(21,181)(28,179)(35,179)(42,179)(50,180)(57,180)(64,181)(71,181)(78,182)(85,183)(92,184)(100,185)\" p=\"O\"/>\n</syllable>\n<syllable ph=\"m @ n\">\n<ph d=\"70\" end=\"0.445\" f0=\"(7,190)(14,195)(21,197)(28,197)(35,198)(42,199)(50,200)(57,200)(64,201)(71,202)(78,202)(85,203)(92,204)(100,207)\" p=\"m\"/>\n<ph d=\"65\" end=\"0.51\" f0=\"(7,210)(15,211)(23,210)(30,210)(38,210)(46,210)(53,210)(61,209)(69,208)(76,207)(84,206)(92,202)(100,197)\" p=\"@\"/>\n<ph d=\"60\" end=\"0.57\" f0=\"(8,201)(16,204)(25,203)(33,202)(41,202)(50,201)(58,200)(66,198)(75,195)(83,191)(91,182)(100,175)\" p=\"n\"/>\n</syllable>\n</t>\n<t g2p_method=\"lexicon\" ph=\"' ? I n\" pos=\"APPR\">\nin\n<syllable ph=\"? I n\" stress=\"1\">\n<ph d=\"30\" end=\"0.59999996\" f0=\"(25,172)(50,170)(75,168)(100,166)\" p=\"?\"/>\n<ph d=\"45\" end=\"0.645\" f0=\"(11,145)(22,150)(33,157)(44,165)(55,167)(66,168)(77,167)(88,166)(100,164)\" p=\"I\"/>\n<ph d=\"60\" end=\"0.705\" f0=\"(8,167)(16,169)(25,169)(33,169)(41,168)(50,167)(58,166)(66,164)(75,163)(83,161)(91,159)(100,157)\" p=\"n\"/>\n</syllable>\n</t>\n<t g2p_method=\"lexicon\" ph=\"' d E 6\" pos=\"ART\">\nder\n<syllable ph=\"d E 6\" stress=\"1\">\n<ph d=\"35\" end=\"0.74\" f0=\"(33,154)(66,152)(100,151)\" p=\"d\"/>\n<ph d=\"35\" end=\"0.77500004\" f0=\"(14,169)(28,164)(42,163)(57,162)(71,161)(85,160)(100,159)\" p=\"E\"/>\n<ph d=\"50\" end=\"0.82500005\" f0=\"(10,158)(20,157)(30,156)(40,156)(50,155)(60,155)(70,154)(80,152)(90,150)(100,147)\" p=\"6\"/>\n</syllable>\n</t>\n<t accent=\"L+H*\" g2p_method=\"lexicon\" ph=\"' v E l t\" pos=\"NN\">\nWelt\n<syllable accent=\"L+H*\" ph=\"v E l t\" stress=\"1\">\n<ph d=\"50\" end=\"0.87500006\" f0=\"(10,147)(20,148)(30,148)(40,148)(50,149)(60,149)(70,149)(80,150)(90,155)(100,158)\" p=\"v\"/>\n<ph d=\"80\" end=\"0.95500004\" f0=\"(6,158)(12,157)(18,157)(25,157)(31,158)(37,158)(43,159)(50,160)(56,160)(62,161)(68,161)(75,162)(81,163)(87,164)(93,166)(100,169)\" p=\"E\"/>\n<ph d=\"70\" end=\"1.0250001\" f0=\"(7,171)(14,173)(21,174)(28,175)(35,176)(42,177)(50,177)(57,178)(64,178)(71,177)(78,176)(85,175)(92,173)(100,172)\" p=\"l\"/>\n<ph d=\"90\" end=\"1.1150001\" p=\"t\"/>\n</syllable>\n</t>\n<t g2p_method=\"lexicon\" ph=\"' d E 6\" pos=\"ART\">\nder\n<syllable ph=\"d E 6\" stress=\"1\">\n<ph d=\"50\" end=\"1.1650001\" p=\"d\"/>\n<ph d=\"35\" end=\"1.2\" f0=\"(14,174)(28,166)(42,163)(57,161)(71,160)(85,159)(100,158)\" p=\"E\"/>\n<ph d=\"50\" end=\"1.25\" f0=\"(10,156)(20,155)(30,155)(40,154)(50,154)(60,153)(70,152)(80,150)(90,149)(100,147)\" p=\"6\"/>\n</syllable>\n</t>\n<t accent=\"^H*\" g2p_method=\"compound\" ph=\"' S p R a: x - z Y n - t e: - z @\" pos=\"NN\">\nSprachsynthese\n<syllable accent=\"^H*\" ph=\"S p R a: x\" stress=\"1\">\n<ph d=\"110\" end=\"1.36\" p=\"S\"/>\n<ph d=\"60\" end=\"1.42\" p=\"p\"/>\n<ph d=\"70\" end=\"1.49\" f0=\"(8,154)(16,150)(25,143)(33,139)(41,139)(50,140)(58,143)(66,146)(75,150)(83,152)(91,152)(100,152)\" p=\"R\"/>\n<ph d=\"125\" end=\"1.615\" f0=\"(4,154)(8,154)(12,154)(16,154)(20,154)(24,154)(28,154)(32,154)(36,155)(40,155)(44,155)(48,155)(52,155)(56,155)(60,155)(64,154)(68,154)(72,154)(76,153)(80,152)(84,152)(88,151)(92,149)(96,147)(100,144)\" p=\"a:\"/>\n<ph d=\"95\" end=\"1.71\" f0=\"(33,142)(66,140)(100,139)\" p=\"x\"/>\n</syllable>\n<syllable ph=\"z Y n\">\n<ph d=\"100\" end=\"1.8100001\" p=\"z\"/>\n<ph d=\"65\" end=\"1.875\" f0=\"(7,156)(15,153)(23,151)(30,150)(38,148)(46,147)(53,146)(61,145)(69,144)(76,143)(84,142)(92,142)(100,142)\" p=\"Y\"/>\n<ph d=\"55\" end=\"1.93\" f0=\"(9,144)(18,144)(27,144)(36,144)(45,143)(54,142)(63,140)(72,139)(81,138)(90,137)(100,136)\" p=\"n\"/>\n</syllable>\n<syllable ph=\"t e:\">\n<ph d=\"95\" end=\"2.0249999\" p=\"t\"/>\n<ph d=\"100\" end=\"2.1249998\" f0=\"(5,166)(10,162)(15,157)(20,153)(25,151)(30,150)(35,149)(40,149)(45,148)(50,147)(55,145)(60,143)(65,141)(70,139)(75,138)(80,136)(85,134)(90,132)(95,131)(100,129)\" p=\"e:\"/>\n</syllable>\n<syllable ph=\"z @\">\n<ph d=\"120\" end=\"2.2449996\" f0=\"(50,127)(100,127)\" p=\"z\"/>\n<ph d=\"90\" end=\"2.3349996\" f0=\"(7,125)(15,122)(23,119)(30,118)(38,117)(46,117)(53,115)(61,114)(69,113)(76,113)(84,112)(92,112)(100,112)\" p=\"@\"/>\n</syllable>\n</t>\n<t pos=\"$.\">\n!\n</t>\n<boundary breakindex=\"5\" duration=\"400\" tone=\"L-%\"/>\n</phrase>\n</s>\n</p>\n</maryxml>\n";
		List<PhraseIU> phraseIUs = MaryAdapter.getInstance().fullySpecifiedMarkup2PhraseIUs(lotsOfDetail);
		d.playStream(AudioUtils.get16kAudioStreamForVocodingStream(new VocodingAudioStream(new IUBasedFullPStream(phraseIUs.get(0)), MaryAdapter5internal.getDefaultHMMData(), true)), true);
	}
	
	@Test public void testFullProsodyComplex() {
		// transcription of actual speech
		//lotsOfDetail =      "<maryxml xmlns=\"http://mary.dfki.de/2002/MaryXML\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"0.5\" xml:lang=\"de\"><p><s><phrase><t g2p_method=\"lexicon\" ph=\"' ? a l - z o:\" pos=\"ADV\">also<syllable ph=\"? a l\" stress=\"1\"><ph d=\"58\" end=\"0.057549\" f0=\"(16,--undefined--)(32,--undefined--)(48,--undefined--)(64,--undefined--)(80,--undefined--)(96,--undefined--)\" p=\"?\" /><ph d=\"38\" end=\"0.09512\" f0=\"(20,232.38)(40,233.69)(60,232.13)(80,231.82)(100,229.39)\" p=\"a\" /><ph d=\"26\" end=\"0.120967\" f0=\"(33,224.9)(66,220.52)(99,219.75)\" p=\"l\" /></syllable><syllable ph=\"z o:\"><ph d=\"34\" end=\"0.154513\" f0=\"(20,218.98)(40,222.38)(60,221.96)(80,223.76)(100,226.17)\" p=\"z\" /><ph d=\"57\" end=\"0.211156\" f0=\"(12,227.02)(24,226.74)(36,225.93)(48,224.96)(60,224.09)(72,224.07)(84,223.67)(96,224.29)\" p=\"o:\" /></syllable></t><t g2p_method=\"lexicon\" ph=\"' n\" pos=\"ADV\">n<syllable ph=\"n\" stress=\"1\"><ph d=\"48\" end=\"0.259\" f0=\"(16,228.93)(32,229.25)(48,229.05)(64,227.8)(80,225.14)(96,227.4)\" p=\"n\" /></syllable></t><t g2p_method=\"lexicon\" ph=\"' h a: - b I C\" pos=\"VAFIN\">habich<syllable ph=\"h a:\" stress=\"1\"><ph d=\"16\" end=\"0.274949\" f0=\"(50,228.18)(100,228.91)\" p=\"h\" /><ph d=\"43\" end=\"0.317843\" f0=\"(16,230.27)(32,229.46)(48,226.05)(64,221.75)(80,218.81)(96,214.96)\" p=\"a:\" /></syllable><syllable ph=\"b I C\" stress=\"1\"><ph d=\"39\" end=\"0.356889\" f0=\"(20,210.42)(40,208.15)(60,210.39)(80,214.48)(100,216.04)\" p=\"b\" /><ph d=\"50\" end=\"0.406933\" f0=\"(14,219.61)(28,218.42)(42,217.78)(56,217.37)(70,216.73)(84,211.97)(98,210.06)\" p=\"I\" /><ph d=\"68\" end=\"0.474575\" f0=\"(11,--undefined--)(22,--undefined--)(33,--undefined--)(44,--undefined--)(55,--undefined--)(66,--undefined--)(77,--undefined--)(88,--undefined--)(99,--undefined--)\" p=\"C\" /></syllable></t><t g2p_method=\"lexicon\" ph=\"' n e:\" pos=\"XY\">ne<syllable ph=\"n e:\" stress=\"1\"><ph d=\"54\" end=\"1.412898\" f0=\"(14,274.48)(28,265.93)(42,231.83)(56,222.49)(70,227.03)(84,229.11)(98,228.81)\" p=\"n\" /><ph d=\"79\" end=\"1.491539\" f0=\"(10,229.4)(20,229.4)(30,230.13)(40,230.58)(50,230.56)(60,230.28)(70,230.26)(80,229.18)(90,226.54)(100,222.88)\" p=\"e:\" /></syllable></t><t g2p_method=\"lexicon\" ph=\"' g a:\" pos=\"ADV\">gar<syllable ph=\"g a:\" stress=\"1\"><ph d=\"56\" end=\"1.547082\" f0=\"(12,208.28)(24,--undefined--)(36,--undefined--)(48,--undefined--)(60,--undefined--)(72,--undefined--)(84,263.24)(96,255.55)\" p=\"g\" /><ph d=\"131\" end=\"1.678517\" f0=\"(5,230.52)(10,227.43)(15,227.13)(20,227.03)(25,226.65)(30,226.14)(35,225.98)(40,226.05)(45,226.54)(50,226.3)(55,226.04)(60,225.66)(65,225.75)(70,225.45)(75,224.26)(80,223.42)(85,222.83)\" p=\"a:\" /></syllable></t><t accent=\"L+H*\" g2p_method=\"rules\" ph=\"' n I C\" pos=\"PTKNEG\">nich<syllable accent=\"L+H*\" ph=\"n I C\" stress=\"1\"><ph d=\"40\" end=\"1.718662\" f0=\"(16,222.11)(32,226.24)(48,224.69)(64,224.36)(80,224.27)(96,224.57)\" p=\"n\" /><ph d=\"45\" end=\"1.763207\" f0=\"(20,221.38)(40,220.78)(60,220.18)(80,219.66)(100,217.69)\" p=\"I\" /><ph d=\"33\" end=\"1.796203\" f0=\"(20,214.42)(40,207.79)(60,207.34)(80,--undefined--)(100,--undefined--)\" p=\"C\" /></syllable></t><t g2p_method=\"lexicon\" ph=\"' d a n\" pos=\"ADV\">dann<syllable ph=\"d a n\" stress=\"1\"><ph d=\"47\" end=\"1.843673\" f0=\"(16,--undefined--)(32,--undefined--)(48,--undefined--)(64,--undefined--)(80,--undefined--)(96,--undefined--)\" p=\"d\" /><ph d=\"31\" end=\"1.874469\" f0=\"(25,--undefined--)(50,259.77)(75,233.5)(100,217.85)\" p=\"a\" /><ph d=\"46\" end=\"1.920114\" f0=\"(16,217.23)(32,217.68)(48,218.17)(64,217.99)(80,217.06)(96,217.75)\" p=\"n\" /></syllable></t><t g2p_method=\"lexicon\" ph=\"' h a: - b I C\" pos=\"VAFIN\">habich<syllable ph=\"h a:\" stress=\"1\"><ph d=\"22\" end=\"1.942111\" f0=\"(33,220.17)(66,224.34)(99,221.69)\" p=\"h\" /><ph d=\"49\" end=\"1.991055\" f0=\"(14,219.91)(28,219.31)(42,219.06)(56,218.78)(70,217.88)(84,214.91)(98,208.1)\" p=\"a:\" /></syllable><syllable ph=\"b I C\" stress=\"1\"><ph d=\"40\" end=\"2.030651\" f0=\"(20,208.68)(40,208.58)(60,210.81)(80,208.01)(100,214.13)\" p=\"b\" /><ph d=\"49\" end=\"2.080145\" f0=\"(14,232.22)(28,221.79)(42,217.3)(56,216.14)(70,213.54)(84,202.58)(98,191.15)\" p=\"I\" /><ph d=\"70\" end=\"2.149986\" f0=\"(11,--undefined--)(22,--undefined--)(33,--undefined--)(44,--undefined--)(55,--undefined--)(66,--undefined--)(77,--undefined--)(88,--undefined--)(99,--undefined--)\" p=\"C\" /></syllable></t><t accent=\"L+H*\" g2p_method=\"lexicon\" ph=\"' f I 6 - ts I C\" pos=\"CARD\">vierzig<syllable accent=\"L+H*\" ph=\"f I 6\" stress=\"1\"><ph d=\"83\" end=\"2.233026\" f0=\"(9,--undefined--)(18,--undefined--)(27,--undefined--)(36,--undefined--)(45,--undefined--)(54,--undefined--)(63,--undefined--)(72,--undefined--)(81,--undefined--)(90,--undefined--)(99,--undefined--)\" p=\"f\" /><ph d=\"71\" end=\"2.303968\" f0=\"(10,--undefined--)(20,283.63)(30,269.65)(40,249.06)(50,249.7)(60,252.6)(70,254.91)(80,257.17)(90,258.22)(100,256.15)\" p=\"I\" /><ph d=\"31\" end=\"2.335314\" f0=\"(25,237.21)(50,--undefined--)(75,--undefined--)(100,--undefined--)\" p=\"6\" /></syllable><syllable ph=\"ts I C\"><ph d=\"58\" end=\"2.393783\" f0=\"(12,--undefined--)(24,--undefined--)(36,--undefined--)(48,--undefined--)(60,--undefined--)(72,--undefined--)(84,--undefined--)(96,347.7)\" p=\"ts\" /><ph d=\"42\" end=\"2.435578\" f0=\"(20,313.48)(40,295.76)(60,290.85)(80,278.14)(100,269.73)\" p=\"I\" /><ph d=\"49\" end=\"2.484181\" f0=\"(14,--undefined--)(28,--undefined--)(42,--undefined--)(56,--undefined--)(70,--undefined--)(84,--undefined--)(98,--undefined--)\" p=\"C\" /></syllable></t><t accent=\"H*\" g2p_method=\"lexicon\" ph=\"k v a: - ' d R a: t - m e: - t 6\" pos=\"NN\">quadratmeter<syllable ph=\"k v a:\"><ph d=\"43\" end=\"2.527417\" f0=\"(20,--undefined--)(40,--undefined--)(60,--undefined--)(80,--undefined--)(100,--undefined--)\" p=\"k\" /><ph d=\"52\" end=\"2.579661\" f0=\"(14,--undefined--)(28,--undefined--)(42,--undefined--)(56,--undefined--)(70,--undefined--)(84,--undefined--)(98,324.05)\" p=\"v\" /><ph d=\"81\" end=\"2.660501\" f0=\"(9,299.57)(18,285.55)(27,283.22)(36,283.27)(45,282.75)(54,282.4)(63,281.7)(72,280.93)(81,280.22)(90,278.74)(99,277.49)\" p=\"a:\" /></syllable><syllable accent=\"H*\" ph=\"d R a: t\" stress=\"1\"><ph d=\"31\" end=\"2.691848\" f0=\"(25,--undefined--)(50,--undefined--)(75,--undefined--)(100,316.18)\" p=\"d\" /><ph d=\"39\" end=\"2.730893\" f0=\"(20,291.93)(40,282.11)(60,281.47)(80,282.11)(100,282.88)\" p=\"R\" /><ph d=\"95\" end=\"2.825456\" f0=\"(7,283.92)(14,284.8)(21,284.82)(28,284.03)(35,284.11)(42,284.63)(49,284.17)(56,284.06)(63,284.26)(70,284.46)(77,284.77)(84,284.18)(91,282.53)\" p=\"a:\" /><ph d=\"74\" end=\"2.899147\" f0=\"(10,278.72)(20,284.02)(30,--undefined--)(40,--undefined--)(50,--undefined--)(60,--undefined--)(70,--undefined--)(80,--undefined--)(90,325.22)(100,322.11)\" p=\"t\" /></syllable><syllable ph=\"m e:\"><ph d=\"46\" end=\"2.945342\" f0=\"(16,307.72)(32,294.93)(48,291.58)(64,291.97)(80,291.74)(96,294.77)\" p=\"m\" /><ph d=\"100\" end=\"3.04543\" f0=\"(7,297.84)(14,296.01)(21,295.88)(28,295.91)(35,295.48)(42,295.24)(49,295.35)(56,296.15)(63,296.9)(70,297.27)(77,297.38)(84,295.88)(91,292.77)\" p=\"e:\" /></syllable><syllable ph=\"t 6\"><ph d=\"82\" end=\"3.12792\" f0=\"(9,284.13)(18,--undefined--)(27,--undefined--)(36,--undefined--)(45,--undefined--)(54,--undefined--)(63,--undefined--)(72,--undefined--)(81,--undefined--)(90,--undefined--)(99,--undefined--)\" p=\"t\" /><ph d=\"212\" end=\"3.339755\" f0=\"(3,--undefined--)(6,325.04)(9,327.62)(12,320.29)(15,313.2)(18,311.03)(21,310.41)(24,310.07)(27,309.9)(30,310.13)(33,310.97)(36,312.67)(39,314.69)(42,316.5)(45,318.77)(48,321.98)(51,324.31)(54,326.25)(57,331.44)(60,340.59)(63,346.41)(66,346.42)(69,346.33)(72,346.7)(75,346.53)(78,340.6)(81,339.83)\" p=\"6\" /></syllable></t><boundary breakindex=\"5\" duration=\"400\" tone=\"L-%\" /></phrase></s></p></maryxml>";
        String lotsOfDetail = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><maryxml xmlns=\"http://mary.dfki.de/2002/MaryXML\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"0.5\" xml:lang=\"de\"><p><s><phrase><t g2p_method=\"lexicon\" ph=\"' ? a l - z o:\" pos=\"ADV\">also<syllable ph=\"? a l\" stress=\"1\"><ph d=\"58\" end=\"0.057549\" f0=\"(16,--undefined--)(32,--undefined--)(48,--undefined--)(64,--undefined--)(80,--undefined--)(96,--undefined--)\" p=\"?\" /><ph d=\"38\" end=\"0.09512\" f0=\"(20,232.38)(40,233.69)(60,232.13)(80,231.82)(100,229.39)\" p=\"a\" /><ph d=\"26\" end=\"0.120967\" f0=\"(33,224.9)(66,220.52)(99,219.75)\" p=\"l\" /></syllable><syllable ph=\"z o:\"><ph d=\"34\" end=\"0.154513\" f0=\"(20,218.98)(40,222.38)(60,221.96)(80,223.76)(100,226.17)\" p=\"z\" /><ph d=\"57\" end=\"0.211156\" f0=\"(12,227.02)(24,226.74)(36,225.93)(48,224.96)(60,224.09)(72,224.07)(84,223.67)(96,224.29)\" p=\"o:\" /></syllable></t><t g2p_method=\"lexicon\" ph=\"' n\" pos=\"ADV\">n<syllable ph=\"n\" stress=\"1\"><ph d=\"48\" end=\"0.259\" f0=\"(16,228.93)(32,229.25)(48,229.05)(64,227.8)(80,225.14)(96,227.4)\" p=\"n\" /></syllable></t><t g2p_method=\"lexicon\" ph=\"' h a: - b I C\" pos=\"VAFIN\">habich<syllable ph=\"h a:\" stress=\"1\"><ph d=\"16\" end=\"0.274949\" f0=\"(50,228.18)(100,228.91)\" p=\"h\" /><ph d=\"43\" end=\"0.317843\" f0=\"(16,230.27)(32,229.46)(48,226.05)(64,221.75)(80,218.81)(96,214.96)\" p=\"a:\" /></syllable><syllable ph=\"b I C\" stress=\"1\"><ph d=\"69\" end=\"0.356889\" f0=\"(20,210.42)(40,208.15)(60,210.39)(80,214.48)(100,216.04)\" p=\"b\" /><ph d=\"220\" end=\"0.406933\" f0=\"(14,219.61)(28,218.42)(42,217.78)(56,217.37)(70,216.73)(84,211.97)(98,210.06)\" p=\"I\" /><ph d=\"248\" end=\"0.474575\" f0=\"(11,--undefined--)(22,--undefined--)(33,--undefined--)(44,--undefined--)(55,--undefined--)(66,--undefined--)(77,--undefined--)(88,--undefined--)(99,--undefined--)\" p=\"C\" /></syllable></t><t g2p_method=\"rules\" ph=\"' f Y - m @ n\" pos=\"VVINF\">fümmen<syllable ph=\"f Y\" stress=\"1\"><ph d=\"151\" end=\"0.625983\" f0=\"(5,--undefined--)(10,--undefined--)(15,--undefined--)(20,--undefined--)(25,--undefined--)(30,--undefined--)(35,--undefined--)(40,--undefined--)(45,--undefined--)(50,--undefined--)(55,--undefined--)(60,--undefined--)(65,--undefined--)(70,--undefined--)(75,--undefined--)(80,--undefined--)(85,--undefined--)(90,--undefined--)(95,--undefined--)(100,--undefined--)\" p=\"f\" /><ph d=\"61\" end=\"0.687025\" f0=\"(12,--undefined--)(24,288.2)(36,268.14)(48,258.65)(60,257.67)(72,261.17)(84,264.56)(96,265.59)\" p=\"Y\" /></syllable><syllable ph=\"m @ n\"><ph d=\"63\" end=\"0.750268\" f0=\"(12,271.32)(24,272.75)(36,274.57)(48,276.76)(60,278.44)(72,280.07)(84,282.25)(96,286.06)\" p=\"m\" /><ph d=\"38\" end=\"0.788213\" f0=\"(20,292.01)(40,293.1)(60,294.96)(80,295.73)(100,295.07)\" p=\"@\" /><ph d=\"169\" end=\"0.957043\" f0=\"(4,291.53)(8,292.98)(12,290.79)(16,290.11)(20,289.79)(24,289.63)(28,290.14)(32,291.02)(36,291.2)(40,290.91)(44,291.4)(48,292.08)(52,292.51)(56,292.18)(60,293.23)(64,299.46)(68,302.8)(72,295.89)(76,294.05)(80,300.26)(84,301.57)(88,299.92)(92,300.5)\" p=\"n\" /></syllable></t><boundary breakindex=\"5\" duration=\"100\" tone=\"H-%\" /><t g2p_method=\"rules\" ph=\"'? E: m\" pos=\"XY\">ähm<syllable accent =\"H*\" ph=\"? E: m\" stress=\"1\"><ph d=\"17\" f0=\"(100,--undefined--)\" p=\"?\" /><ph d=\"204\" f0=\"(16,250)(32,248)(48,244)(64,240)(80,248)(96,247)\" p=\"E:\" /><ph d=\"194\" f0=\"(16,240)(32,240)(48,240)(64,240)(80,240)(96,240)\" p=\"m\" /></syllable></t><boundary breakindex=\"5\" duration=\"100\" tone=\"H-%\" /><t g2p_method=\"lexicon\" ph=\"' n e:\" pos=\"XY\">ne<syllable ph=\"n e:\" stress=\"1\"><ph d=\"54\" end=\"1.412898\" f0=\"(14,274.48)(28,265.93)(42,231.83)(56,222.49)(70,227.03)(84,229.11)(98,228.81)\" p=\"m\" /><ph d=\"79\" end=\"1.491539\" f0=\"(10,229.4)(20,229.4)(30,230.13)(40,230.58)(50,230.56)(60,230.28)(70,230.26)(80,229.18)(90,226.54)(100,222.88)\" p=\"e:\" /></syllable></t><t g2p_method=\"lexicon\" ph=\"' g a:\" pos=\"ADV\">gar<syllable ph=\"g a:\" stress=\"1\"><ph d=\"56\" end=\"1.547082\" f0=\"(12,208.28)(24,--undefined--)(36,--undefined--)(48,--undefined--)(60,--undefined--)(72,--undefined--)(84,263.24)(96,255.55)\" p=\"g\" /><ph d=\"131\" end=\"1.678517\" f0=\"(5,230.52)(10,227.43)(15,227.13)(20,227.03)(25,226.65)(30,226.14)(35,225.98)(40,226.05)(45,226.54)(50,226.3)(55,226.04)(60,225.66)(65,225.75)(70,225.45)(75,224.26)(80,223.42)(85,222.83)\" p=\"a:\" /></syllable></t><t accent=\"L+H*\" g2p_method=\"rules\" ph=\"' n I C\" pos=\"PTKNEG\">nich<syllable accent=\"L+H*\" ph=\"n I C\" stress=\"1\"><ph d=\"40\" end=\"1.718662\" f0=\"(16,222.11)(32,226.24)(48,224.69)(64,224.36)(80,224.27)(96,224.57)\" p=\"n\" /><ph d=\"45\" end=\"1.763207\" f0=\"(20,221.38)(40,220.78)(60,220.18)(80,219.66)(100,217.69)\" p=\"I\" /><ph d=\"33\" end=\"1.796203\" f0=\"(20,214.42)(40,207.79)(60,207.34)(80,--undefined--)(100,--undefined--)\" p=\"C\" /></syllable></t><t g2p_method=\"lexicon\" ph=\"' d a n\" pos=\"ADV\">dann<syllable ph=\"d a n\" stress=\"1\"><ph d=\"47\" end=\"1.843673\" f0=\"(16,--undefined--)(32,--undefined--)(48,--undefined--)(64,--undefined--)(80,--undefined--)(96,--undefined--)\" p=\"d\" /><ph d=\"31\" end=\"1.874469\" f0=\"(25,--undefined--)(50,259.77)(75,233.5)(100,217.85)\" p=\"a\" /><ph d=\"46\" end=\"1.920114\" f0=\"(16,217.23)(32,217.68)(48,218.17)(64,217.99)(80,217.06)(96,217.75)\" p=\"n\" /></syllable></t><t g2p_method=\"lexicon\" ph=\"' h a: - b I C\" pos=\"VAFIN\">habich<syllable ph=\"h a:\" stress=\"1\"><ph d=\"22\" end=\"1.942111\" f0=\"(33,220.17)(66,224.34)(99,221.69)\" p=\"h\" /><ph d=\"49\" end=\"1.991055\" f0=\"(14,219.91)(28,219.31)(42,219.06)(56,218.78)(70,217.88)(84,214.91)(98,208.1)\" p=\"a:\" /></syllable><syllable ph=\"b I C\" stress=\"1\"><ph d=\"40\" end=\"2.030651\" f0=\"(20,208.68)(40,208.58)(60,210.81)(80,208.01)(100,214.13)\" p=\"b\" /><ph d=\"49\" end=\"2.080145\" f0=\"(14,232.22)(28,221.79)(42,217.3)(56,216.14)(70,213.54)(84,202.58)(98,191.15)\" p=\"I\" /><ph d=\"70\" end=\"2.149986\" f0=\"(11,--undefined--)(22,--undefined--)(33,--undefined--)(44,--undefined--)(55,--undefined--)(66,--undefined--)(77,--undefined--)(88,--undefined--)(99,--undefined--)\" p=\"C\" /></syllable></t><t accent=\"L+H*\" g2p_method=\"lexicon\" ph=\"' f I 6 - ts I C\" pos=\"CARD\">vierzig<syllable accent=\"L+H*\" ph=\"f I 6\" stress=\"1\"><ph d=\"83\" end=\"2.233026\" f0=\"(9,--undefined--)(18,--undefined--)(27,--undefined--)(36,--undefined--)(45,--undefined--)(54,--undefined--)(63,--undefined--)(72,--undefined--)(81,--undefined--)(90,--undefined--)(99,--undefined--)\" p=\"f\" /><ph d=\"71\" end=\"2.303968\" f0=\"(10,--undefined--)(20,283.63)(30,269.65)(40,249.06)(50,249.7)(60,252.6)(70,254.91)(80,257.17)(90,258.22)(100,256.15)\" p=\"I\" /><ph d=\"31\" end=\"2.335314\" f0=\"(25,237.21)(50,--undefined--)(75,--undefined--)(100,--undefined--)\" p=\"6\" /></syllable><syllable ph=\"ts I C\"><ph d=\"58\" end=\"2.393783\" f0=\"(12,--undefined--)(24,--undefined--)(36,--undefined--)(48,--undefined--)(60,--undefined--)(72,--undefined--)(84,--undefined--)(96,347.7)\" p=\"ts\" /><ph d=\"42\" end=\"2.435578\" f0=\"(20,313.48)(40,295.76)(60,290.85)(80,278.14)(100,269.73)\" p=\"I\" /><ph d=\"49\" end=\"2.484181\" f0=\"(14,--undefined--)(28,--undefined--)(42,--undefined--)(56,--undefined--)(70,--undefined--)(84,--undefined--)(98,--undefined--)\" p=\"C\" /></syllable></t><t accent=\"H*\" g2p_method=\"lexicon\" ph=\"k v a: - ' d R a: t - m e: - t 6\" pos=\"NN\">quadratmeter<syllable ph=\"k v a:\"><ph d=\"43\" end=\"2.527417\" f0=\"(20,--undefined--)(40,--undefined--)(60,--undefined--)(80,--undefined--)(100,--undefined--)\" p=\"k\" /><ph d=\"52\" end=\"2.579661\" f0=\"(14,--undefined--)(28,--undefined--)(42,--undefined--)(56,--undefined--)(70,--undefined--)(84,--undefined--)(98,324.05)\" p=\"v\" /><ph d=\"81\" end=\"2.660501\" f0=\"(9,299.57)(18,285.55)(27,283.22)(36,283.27)(45,282.75)(54,282.4)(63,281.7)(72,280.93)(81,280.22)(90,278.74)(99,277.49)\" p=\"a:\" /></syllable><syllable accent=\"H*\" ph=\"d R a: t\" stress=\"1\"><ph d=\"31\" end=\"2.691848\" f0=\"(25,--undefined--)(50,--undefined--)(75,--undefined--)(100,316.18)\" p=\"d\" /><ph d=\"39\" end=\"2.730893\" f0=\"(20,291.93)(40,282.11)(60,281.47)(80,282.11)(100,282.88)\" p=\"R\" /><ph d=\"95\" end=\"2.825456\" f0=\"(7,283.92)(14,284.8)(21,284.82)(28,284.03)(35,284.11)(42,284.63)(49,284.17)(56,284.06)(63,284.26)(70,284.46)(77,284.77)(84,284.18)(91,282.53)\" p=\"a:\" /><ph d=\"74\" end=\"2.899147\" f0=\"(10,278.72)(20,284.02)(30,--undefined--)(40,--undefined--)(50,--undefined--)(60,--undefined--)(70,--undefined--)(80,--undefined--)(90,325.22)(100,322.11)\" p=\"t\" /></syllable><syllable ph=\"m e:\"><ph d=\"46\" end=\"2.945342\" f0=\"(16,307.72)(32,294.93)(48,291.58)(64,291.97)(80,291.74)(96,294.77)\" p=\"m\" /><ph d=\"100\" end=\"3.04543\" f0=\"(7,297.84)(14,296.01)(21,295.88)(28,295.91)(35,295.48)(42,295.24)(49,295.35)(56,296.15)(63,296.9)(70,297.27)(77,297.38)(84,295.88)(91,292.77)\" p=\"e:\" /></syllable><syllable ph=\"t 6\"><ph d=\"82\" end=\"3.12792\" f0=\"(9,284.13)(18,--undefined--)(27,--undefined--)(36,--undefined--)(45,--undefined--)(54,--undefined--)(63,--undefined--)(72,--undefined--)(81,--undefined--)(90,--undefined--)(99,--undefined--)\" p=\"t\" /><ph d=\"212\" end=\"3.339755\" f0=\"(3,--undefined--)(6,325.04)(9,327.62)(12,320.29)(15,313.2)(18,311.03)(21,310.41)(24,310.07)(27,309.9)(30,310.13)(33,310.97)(36,312.67)(39,314.69)(42,316.5)(45,318.77)(48,321.98)(51,324.31)(54,326.25)(57,331.44)(60,340.59)(63,346.41)(66,346.42)(69,346.33)(72,346.7)(75,346.53)(78,340.6)(81,339.83)\" p=\"6\" /></syllable></t><boundary breakindex=\"5\" duration=\"400\" tone=\"L-%\" /></phrase></s></p></maryxml>";
//		String lotsOfDetail = "﻿<?xml version=\"1.0\" encoding=\"UTF-8\"?><maryxml xmlns=\"http://mary.dfki.de/2002/MaryXML\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"0.5\" xml:lang=\"de\"><p><s><phrase><t g2p_method=\"lexicon\" ph=\"' ? a l - z o:\" pos=\"ADV\">also\n<syllable ph=\"? a l\" stress=\"1\">\n<ph d=\"58\" end=\"0.057549\" f0=\"(16,--undefined--)(32,--undefined--)(48,--undefined--)(64,--undefined--)(80,--undefined--)(96,--undefined--)\" p=\"?\" />\n<ph d=\"38\" end=\"0.09512\" f0=\"(20,232.38)(40,233.69)(60,232.13)(80,231.82)(100,229.39)\" p=\"a\" />\n<ph d=\"26\" end=\"0.120967\" f0=\"(33,224.9)(66,220.52)(99,219.75)\" p=\"l\" />\n</syllable>\n<syllable ph=\"z o:\">\n<ph d=\"34\" end=\"0.154513\" f0=\"(20,218.98)(40,222.38)(60,221.96)(80,223.76)(100,226.17)\" p=\"z\" />\n<ph d=\"57\" end=\"0.211156\" f0=\"(12,227.02)(24,226.74)(36,225.93)(48,224.96)(60,224.09)(72,224.07)(84,223.67)(96,224.29)\" p=\"o:\" />\n</syllable>\n</t>\n\n\n<t g2p_method=\"lexicon\" ph=\"' n\" pos=\"ADV\">\nn\n<syllable ph=\"n\" stress=\"1\">\n<ph d=\"48\" end=\"0.259\" f0=\"(16,228.93)(32,229.25)(48,229.05)(64,227.8)(80,225.14)(96,227.4)\" p=\"n\" />\n</syllable>\n</t>\n\n\n<t g2p_method=\"lexicon\" ph=\"' h a: - b I C\" pos=\"VAFIN\">\nhabich\n<syllable ph=\"h a:\" stress=\"1\">\n<ph d=\"16\" end=\"0.274949\" f0=\"(50,228.18)(100,228.91)\" p=\"h\" />\n<ph d=\"43\" end=\"0.317843\" f0=\"(16,230.27)(32,229.46)(48,226.05)(64,221.75)(80,218.81)(96,214.96)\" p=\"a:\" />\n\n\n</syllable>\n\n<syllable ph=\"b I C\" stress=\"1\">\n<ph d=\"69\" end=\"0.356889\" f0=\"(20,210.42)(40,208.15)(60,210.39)(80,214.48)(100,216.04)\" p=\"b\" />\n<ph d=\"220\" end=\"0.406933\" f0=\"(14,219.61)(28,218.42)(42,217.78)(56,217.37)(70,216.73)(84,211.97)(98,210.06)\" p=\"I\" />\n<ph d=\"248\" end=\"0.474575\" f0=\"(11,--undefined--)(22,--undefined--)(33,--undefined--)(44,--undefined--)(55,--\nundefined--)(66,--undefined--)(77,--undefined--)(88,--undefined--)(99,--undefined--)\" p=\"C\" />\n</syllable>\n</t>\n\n\n\n\n<t g2p_method=\"rules\" ph=\"' f Y - m @ n\" pos=\"VVINF\">\nf&#252;mmen\n<syllable ph=\"f Y\" stress=\"1\">\n<ph d=\"151\" end=\"0.625983\" f0=\"(5,--undefined--)(10,--undefined--)(15,--undefined--)(20,--undefined--)(25,--undefined--)(30,--undefined--)(35,--undefined--)(40,--undefined--)(45,--undefined--)(50,--undefined--)(55,--undefined--)(60,--undefined--)(65,--undefined--)(70,--undefined--)(75,--undefined--)(80,--undefined--)(85,--undefined--)(90,--undefined--)(95,--undefined--)(100,--undefined--)\" p=\"f\" />\n<ph d=\"61\" end=\"0.687025\" f0=\"(12,--undefined--)(24,288.2)(36,268.14)(48,258.65)(60,257.67)(72,261.17)(84,264.56)(96,265.59)\" p=\"Y\" />\n</syllable>\n<syllable ph=\"m @ n\">\n<ph d=\"63\" end=\"0.750268\" f0=\"(12,271.32)(24,272.75)(36,274.57)(48,276.76)(60,278.44)(72,280.07)(84,282.25)(96,286.06)\" p=\"m\" />\n<ph d=\"38\" end=\"0.788213\" f0=\"(20,292.01)(40,293.1)(60,294.96)(80,295.73)(100,295.07)\" p=\"@\" />\n<ph d=\"169\" end=\"0.957043\" f0=\"(4,291.53)(8,292.98)(12,290.79)(16,290.11)(20,289.79)(24,289.63)(28,290.14)(32,291.02)(36,291.2)(40,290.91)(44,291.4)(48,292.08)(52,292.51)(56,292.18)(60,293.23)(64,299.46)(68,302.8)(72,295.89)(76,294.05)(80,300.26)(84,301.57)(88,299.92)(92,300.5)\" p=\"n\" />\n</syllable>\n</t>\n\n<boundary breakindex=\"5\" duration=\"100\" tone=\"H-%\" />\n\n\n\n<t g2p_method=\"rules\" ph=\"'? E: m\" pos=\"XY\">\nähm\n<syllable accent =\"H*\" ph=\"? E: m\" stress=\"1\">\n<ph d=\"17\" f0=\"(100,--undefined--)\" p=\"?\" />\n<ph d=\"204\" f0=\"(16,250)(32,248)(48,244)(64,240)(80,248)(96,247)\" p=\"E:\" />\n<ph d=\"194\" f0=\"(16,240)(32,240)(48,240)(64,240)(80,240)(96,240)\" p=\"m\" />\n</syllable>\n</t>\n\n\n\n<boundary breakindex=\"5\" duration=\"100\" tone=\"H-%\" />\n\n<t g2p_method=\"lexicon\" ph=\"' n e:\" pos=\"XY\">\nne\n<syllable ph=\"n e:\" stress=\"1\">\n<ph d=\"54\" end=\"1.412898\" f0=\"(14,274.48)(28,265.93)(42,231.83)(56,222.49)(70,227.03)(84,229.11)(98,228.81)\" p=\"m\" />\n<ph d=\"79\" end=\"1.491539\" f0=\"(10,229.4)(20,229.4)(30,230.13)(40,230.58)(50,230.56)(60,230.28)(70,230.26)(80,229.18)(90,226.54)(100,222.88)\" p=\"e:\" />\n</syllable>\n</t>\n<t g2p_method=\"lexicon\" ph=\"' g a:\" pos=\"ADV\">\ngar\n<syllable ph=\"g a:\" stress=\"1\">\n<ph d=\"56\" end=\"1.547082\" f0=\"(12,208.28)(24,--undefined--)(36,--undefined--)(48,--undefined--)(60,--undefined--)(72,--undefined--)(84,263.24)(96,255.55)\" p=\"g\" />\n<ph d=\"131\" end=\"1.678517\" f0=\"(5,230.52)(10,227.43)(15,227.13)(20,227.03)(25,226.65)(30,226.14)(35,225.98)(40,226.05)(45,226.54)(50,226.3)(55,226.04)(60,225.66)(65,225.75)(70,225.45)(75,224.26)(80,223.42)(85,222.83)\" p=\"a:\" />\n\n</syllable>\n</t>\n<t accent=\"L+H*\" g2p_method=\"rules\" ph=\"' n I C\" pos=\"PTKNEG\">\nnich\n<syllable accent=\"L+H*\" ph=\"n I C\" stress=\"1\">\n<ph d=\"40\" end=\"1.718662\" f0=\"(16,222.11)(32,226.24)(48,224.69)(64,224.36)(80,224.27)(96,224.57)\" p=\"n\" />\n<ph d=\"45\" end=\"1.763207\" f0=\"(20,221.38)(40,220.78)(60,220.18)(80,219.66)(100,217.69)\" p=\"I\" />\n<ph d=\"33\" end=\"1.796203\" f0=\"(20,214.42)(40,207.79)(60,207.34)(80,--undefined--)(100,--undefined--)\" p=\"C\" />\n</syllable>\n</t>\n<t g2p_method=\"lexicon\" ph=\"' d a n\" pos=\"ADV\">\ndann\n<syllable ph=\"d a n\" stress=\"1\">\n<ph d=\"47\" end=\"1.843673\" f0=\"(16,--undefined--)(32,--undefined--)(48,--undefined--)(64,--undefined--)(80,--undefined--)(96,--undefined--)\" p=\"d\" />\n<ph d=\"31\" end=\"1.874469\" f0=\"(25,--undefined--)(50,259.77)(75,233.5)(100,217.85)\" p=\"a\" />\n<ph d=\"46\" end=\"1.920114\" f0=\"(16,217.23)(32,217.68)(48,218.17)(64,217.99)(80,217.06)(96,217.75)\" p=\"n\" />\n</syllable>\n</t>\n<t g2p_method=\"lexicon\" ph=\"' h a: - b I C\" pos=\"VAFIN\">\nhabich\n<syllable ph=\"h a:\" stress=\"1\">\n<ph d=\"22\" end=\"1.942111\" f0=\"(33,220.17)(66,224.34)(99,221.69)\" p=\"h\" />\n<ph d=\"49\" end=\"1.991055\" f0=\"(14,219.91)(28,219.31)(42,219.06)(56,218.78)(70,217.88)(84,214.91)(98,208.1)\" p=\"a:\" />\n\n\n</syllable>\n\n<syllable ph=\"b I C\" stress=\"1\">\n<ph d=\"40\" end=\"2.030651\" f0=\"(20,208.68)(40,208.58)(60,210.81)(80,208.01)(100,214.13)\" p=\"b\" />\n<ph d=\"49\" end=\"2.080145\" f0=\"(14,232.22)(28,221.79)(42,217.3)(56,216.14)(70,213.54)(84,202.58)(98,191.15)\" p=\"I\" />\n<ph d=\"70\" end=\"2.149986\" f0=\"(11,--undefined--)(22,--undefined--)(33,--undefined--)(44,--undefined--)(55,--undefined--)(66,--undefined--)(77,--undefined--)(88,--undefined--)(99,--undefined--)\" p=\"C\" />\n</syllable>\n</t>\n\n<t accent=\"L+H*\" g2p_method=\"lexicon\" ph=\"' f I 6 - ts I C\" pos=\"CARD\">\nvierzig\n<syllable accent=\"L+H*\" ph=\"f I 6\" stress=\"1\">\n<ph d=\"83\" end=\"2.233026\" f0=\"(9,--undefined--)(18,--undefined--)(27,--undefined--)(36,--undefined--)(45,--undefined--)(54,--undefined--)(63,--undefined--)(72,--undefined--)(81,--undefined--)(90,--undefined--)(99,--undefined--)\" p=\"f\" />\n<ph d=\"71\" end=\"2.303968\" f0=\"(10,--undefined--)(20,283.63)(30,269.65)(40,249.06)(50,249.7)(60,252.6)(70,254.91)(80,257.17)(90,258.22)(100,256.15)\" p=\"I\" />\n<ph d=\"31\" end=\"2.335314\" f0=\"(25,237.21)(50,--undefined--)(75,--undefined--)(100,--undefined--)\" p=\"6\" />\n</syllable>\n<syllable ph=\"ts I C\">\n<ph d=\"58\" end=\"2.393783\" f0=\"(12,--undefined--)(24,--undefined--)(36,--undefined--)(48,--undefined--)(60,--undefined--)(72,--undefined--)(84,--undefined--)(96,347.7)\" p=\"ts\" />\n<ph d=\"42\" end=\"2.435578\" f0=\"(20,313.48)(40,295.76)(60,290.85)(80,278.14)(100,269.73)\" p=\"I\" />\n<ph d=\"49\" end=\"2.484181\" f0=\"(14,--undefined--)(28,--undefined--)(42,--undefined--)(56,--undefined--)(70,--undefined--)(84,--undefined--)(98,--undefined--)\" p=\"C\" />\n</syllable>\n</t>\n<t accent=\"H*\" g2p_method=\"lexicon\" ph=\"k v a: - ' d R a: t - m e: - t 6\" pos=\"NN\">\nquadratmeter\n<syllable ph=\"k v a:\">\n<ph d=\"43\" end=\"2.527417\" f0=\"(20,--undefined--)(40,--undefined--)(60,--undefined--)(80,--undefined--)(100,--\nundefined--)\" p=\"k\" />\n<ph d=\"52\" end=\"2.579661\" f0=\"(14,--undefined--)(28,--undefined--)(42,--undefined--)(56,--undefined--)(70,--\nundefined--)(84,--undefined--)(98,324.05)\" p=\"v\" />\n<ph d=\"81\" end=\"2.660501\" f0=\"(9,299.57)(18,285.55)(27,283.22)(36,283.27)(45,282.75)(54,282.4)(63,281.7)(72,280.93)\n(81,280.22)(90,278.74)(99,277.49)\" p=\"a:\" />\n</syllable>\n<syllable accent=\"H*\" ph=\"d R a: t\" stress=\"1\">\n<ph d=\"31\" end=\"2.691848\" f0=\"(25,--undefined--)(50,--undefined--)(75,--undefined--)(100,316.18)\" p=\"d\" />\n<ph d=\"39\" end=\"2.730893\" f0=\"(20,291.93)(40,282.11)(60,281.47)(80,282.11)(100,282.88)\" p=\"R\" />\n<ph d=\"95\" end=\"2.825456\" f0=\"(7,283.92)(14,284.8)(21,284.82)(28,284.03)(35,284.11)(42,284.63)(49,284.17)(56,284.06)\n(63,284.26)(70,284.46)(77,284.77)(84,284.18)(91,282.53)\" p=\"a:\" />\n<ph d=\"74\" end=\"2.899147\" f0=\"(10,278.72)(20,284.02)(30,--undefined--)(40,--undefined--)(50,--undefined--)(60,--\nundefined--)(70,--undefined--)(80,--undefined--)(90,325.22)(100,322.11)\" p=\"t\" />\n</syllable>\n<syllable ph=\"m e:\">\n<ph d=\"46\" end=\"2.945342\" f0=\"(16,307.72)(32,294.93)(48,291.58)(64,291.97)(80,291.74)(96,294.77)\" p=\"m\" />\n<ph d=\"100\" end=\"3.04543\" f0=\"(7,297.84)(14,296.01)(21,295.88)(28,295.91)(35,295.48)(42,295.24)(49,295.35)(56,296.15)\n(63,296.9)(70,297.27)(77,297.38)(84,295.88)(91,292.77)\" p=\"e:\" />\n</syllable>\n<syllable ph=\"t 6\">\n<ph d=\"82\" end=\"3.12792\" f0=\"(9,284.13)(18,--undefined--)(27,--undefined--)(36,--undefined--)(45,--undefined--)(54,--\nundefined--)(63,--undefined--)(72,--undefined--)(81,--undefined--)(90,--undefined--)(99,--undefined--)\" p=\"t\" />\n<ph d=\"212\" end=\"3.339755\" f0=\"(3,--undefined--)(6,325.04)(9,327.62)(12,320.29)(15,313.2)(18,311.03)(21,310.41)\n(24,310.07)(27,309.9)(30,310.13)(33,310.97)(36,312.67)(39,314.69)(42,316.5)(45,318.77)(48,321.98)(51,324.31)(54,326.25)\n(57,331.44)(60,340.59)(63,346.41)(66,346.42)(69,346.33)(72,346.7)(75,346.53)(78,340.6)(81,339.83)\" p=\"6\" />\n</syllable>\n</t>\n<boundary breakindex=\"5\" duration=\"400\" tone=\"L-%\" />\n</phrase>\n</s>\n</p></maryxml>";
		List<PhraseIU >phraseIUs = MaryAdapter.getInstance().fullySpecifiedMarkup2PhraseIUs(lotsOfDetail);
		IU.IUUpdateListener segPrinter = new SegPrinter();
		for (PhraseIU phrase : phraseIUs) {
			for (SegmentIU seg : phrase.getSegments()) {
				seg.addUpdateListener(segPrinter);
			}
		}
		d.playStream(AudioUtils.get16kAudioStreamForVocodingStream(new VocodingAudioStream(new IUBasedFullPStream(phraseIUs.get(0)), MaryAdapter5internal.getDefaultHMMData(), true)), true);
	}
	
	private class SegPrinter implements IU.IUUpdateListener {
		@Override
		public void update(IU updatedIU) {
			System.err.println(updatedIU.toString() + " with progress status " + updatedIU.getProgress());
		}
	}

}
