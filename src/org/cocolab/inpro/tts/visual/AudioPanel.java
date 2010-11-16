package org.cocolab.inpro.tts.visual;

import java.awt.Color;
import java.awt.Graphics;
import java.io.IOException;

import javax.sound.sampled.AudioInputStream;

import edu.cmu.sphinx.frontend.FrontEnd;
import edu.cmu.sphinx.frontend.util.StreamDataSource;
import edu.cmu.sphinx.tools.audio.AudioData;
import edu.cmu.sphinx.tools.audio.AudioPlayer;
import edu.cmu.sphinx.tools.audio.SpectrogramPanel;
import edu.cmu.sphinx.tools.audio.Utils;
import edu.cmu.sphinx.util.props.ConfigurationManager;

/** 
 * a SpectrogramPanel which also handles playing audio
 * @author timo 
 */
@SuppressWarnings("serial")
class AudioPanel extends SpectrogramPanel {
	private AudioPlayer player;

	AudioPanel() {
		ConfigurationManager cm = new ConfigurationManager(VisualTTS.class.getResource("spectrogram.config.xml"));
		FrontEnd fe = (FrontEnd) cm.lookup("frontEnd");
		StreamDataSource sds = (StreamDataSource) cm.lookup("streamDataSource");
		this.frontEnd = fe;
		this.dataSource = sds;
		this.audio = new AudioData();
		this.player = new AudioPlayer(audio);
		player.start();
	}

	public void setZoom(float zoom) {
		zoomSet(zoom);
	}
	
	@Override
	public void paint(Graphics g) {
		super.paint(g); // start out painting the spectrogram
		g.setColor(Color.RED);
		
	}

	public void setAudio(AudioInputStream ais) {
		try {
			audio.setAudioData(Utils.toSignedPCM(ais));
			computeSpectrogram();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void playAudio() {
		player.play(0, audio.getAudioData().length);
	}
	
}