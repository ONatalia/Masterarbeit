/**
A GUI to manipulate (improve, mimic, parody) Mary TTS.
The TTS's unit and prosody model can be manipulated and
this can be resynthesized. For example, TTS errors can be
corrected, prosody can be improved, etc.

<h2>TODO before release:</h2> 
<ul>
<li>change to Mary 4.1.1 (which supports duration and pitch adaptation with HMM voices)</li>
<ul>
	<li>however, HMM voices have a continuous pitch track, which would be horribly annoying</li>
</ul>
<li>Mary-Anbindung konfigurierbar machen</li>
<li>make audio more responsive</li>
<ul>
	<li>abort playing/streaming if another edit is about to happen</li>
	<li>option to not automatically play after every edit</li>
	<li>use mp3 for streaming?</li>
</li>
<li>support different languages (english, who cares about tibetan?)</li> 
<li>tatsächliche Intonationskurve errechnen und auch darstellen &rarr; mphf, beschissen</li>
	<li>mithilfe von l2fprod (liegt aufm Desktop)</li>
<li>"Wussten Sie schon"-Box für unentdeckbare Features<ul>
	<li>Grenzen und Pitchpunkte durch drag/drop verschieben</li>
	<li>Shift beim verschieben ändert nur diese Grenze, nicht alle folgenden</li>
	<li>bei Rechtsklick im Segmentteil erscheint ein Pop-Up Menü mit dem Segmente eingefügt, geändert und gelöscht werden können.</li>
	<li>Mit den Knöpfen oben links können PHO-Dateien importiert und exportiert, sowie Audiodateien gespeichert werden</li>
	<li>...</li>
</ul></li>
<li>see also <a href="http://pascal.kgw.tu-berlin.de/expressive-speech/online/synthesis/german/en/ia-en.php">http://pascal.kgw.tu-berlin.de/expressive-speech/online/synthesis/german/en/ia-en.php</a></li>
<li>see also <a href="http://kitt.cl.uzh.ch/clab/dialogsysteme/ilap_mary/">http://kitt.cl.uzh.ch/clab/dialogsysteme/ilap_mary/</a></li>
</ul>

<h3>DONE</h3>
<ul>
<li>show a dialogue on mary-error (server not up, etc.)</li>
<li>generate a default filename for saving</li>
<li>allow manual editing of label file</li>
</ul>

@author Timo Baumann
@version 0.9
 */
package org.cocolab.inpro.synthesis.visual;