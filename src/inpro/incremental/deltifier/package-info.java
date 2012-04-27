/**
 * Package for deltifiers, The pluggable modules that convert
 * Sphinx' {@link edu.cmu.sphinx.result.Result} objects
 * to objects of the IU world. 
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

These modules are pluggable into 
{@link inpro.incremental.processor.CurrentASRHypothesis},
our starting point into the IU world. 
They convert Sphinx' {@link edu.cmu.sphinx.result.Result} objects
(which always contain the complete result up to the current
state) to a set of {@link inpro.incremental.unit.IU}
objects (words, syllables and segments) and 
{@link inpro.incremental.unit.EditMessage}s 
that explain which IUs have been added since the last call.
<p>
Deltifiers receive and process {@link edu.cmu.sphinx.result.Result}s
using the <code>void deltify(Result result)</code> operation defined in
in {@link inpro.incremental.deltifier.ASRWordDeltifier} 
(which additionally export operations such
as resetting and configuration (using Sphinx' configuration management).
<p>
The interface {@link inpro.incremental.deltifier.ASRResultKeeper} 
defines the output of the deltifiers: They may be queried for the most recent 
set of IUs, the edits since the preceding set, and the time (in frames) of that 
set.
<p>
All deltifiers must be descendants of 
{@link inpro.incremental.deltifier.ASRWordDeltifier}.
<p>
The following deltifiers are provided:
<dl>
<dt>{@link inpro.incremental.deltifier.ASRWordDeltifier}</dt>
<dd> defines a ``standard'' deltifier, which outputs
all the words from a Result object and generates edits for all
edits. 
<p>
The recognizer in Sphinx often changes opinion (often at every frame),
so that the output from this deltifier changes very frequently, often revoking
a word that will be back in the next frame, alternating between readings etc.
</dd>
<dt>{@link inpro.incremental.deltifier.FixedLagDeltifier}</dt>
<dd>
instead of interpreting the current {@link edu.cmu.sphinx.result.Result} 
up to the current time,
it leaves some <em>fixed lag</em> (measured in frames) at the end of the result. 
As most intermittent recognition errors happen at the right end of the 
hypothesis, the fixed lag is a simple and effective method to avoid these.
</dd>
<dt>{@link inpro.incremental.deltifier.SmoothingDeltifier}</dt>
<dd>
waits until an IU has reached a certain <em>maturity</em> (measured in frames),
before it is released to the outside world. This is very successful in reducing
the result jitter from ASR, but may occasionally lead to longer latencies if ASR
jitter is unfavourable (whereas latency is fixed for the FixedLagDeltifier). 
</dd>
<dt>{@link inpro.incremental.deltifier.NonIncrementalDeltifier}</dt>
<dd>switches off incrementality by only adding committed hypotheses</dd>
</dl>


@author Timo Baumann
 */
package inpro.incremental.deltifier;
