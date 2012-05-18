/**
 * Some Sphinx frontend processors that come in handy when working
 * with Sphinx and InproTK:
 * 
 * <ul>
 * <li>An improved implementation of Sphinx' Speech Marker with much 
 * improved incremental behaviour (QuickSpeechmarker.java)
 * <li>A processor that reads audio from an RTP stream
 * (RtpRecvProcessor.java)
 * <li>A processor that splits off all frontend processing into a 
 * separate thread (ThreadingFrontendBuffer.java)
 * <li>A processor that logs passing audio to TEDview
 * </ul>
 * 
 * @author Timo Baumann
 *
<pre>
_____________________________________________________________
  _                                         _           _   
 (_)_ __  _ __  _ __ ___    _ __  _ __ ___ (_) ___  ___| |_ 
 | | '_ \| '_ \| '__/ _ \  | '_ \| '__/ _ \| |/ _ \/ __| __|
 | | | | | |_) | | | (_) | | |_) | | | (_) | |  __/ (__| |_ 
 |_|_| |_| .__/|_|  \___/  | .__/|_|  \___// |\___|\___|\__|
         |_|               |_|           |__/               
_____________________________________________________________
</pre>
 */
package inpro.sphinx.frontend;