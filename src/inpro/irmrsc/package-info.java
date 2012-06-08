/**
 * Package for a NLU pipeline with a POS-tag PCFG syntax and RMRS rule semantics
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

The classes of this package are intended to be used to incrementally parse
strings of POS-tags with a probabilistic context-free grammar and monotonically
and synchronously enrich a semantic representation accordingly.<br />
As the semantic formalism we use Robust Minimal Recursion Semantics (RMRS), see
Ann Copestake, 2007. Semantic composition with (robust) minimal recursion semantics.
In: Proceedings of the Workshop on Deep Linguistic Processing 2007.<br />
The parser is a variant of that described in Brian Roark 2001 Ph.D. Thesis,
Department of Cognitive and Linguistic Sciences, Brown University.
<p />
The package is structured as follows:
<dl>
<dt>{@link inpro.irmrsc.simplepcfg}</dt>
<dd>holds the implementation of the probabilistic context-free grammar
with classes for the {@link inpro.irmrsc.simplepcfg.Symbol}s and 
{@link inpro.irmrsc.simplepcfg.Production}s of a
{@link inpro.irmrsc.simplepcfg.Grammar}</dd>

<dt>{@link inpro.irmrsc.parser}</dt>
<dd>holds the implementation of the incremental beam-searching top-down parser.
{@link inpro.irmrsc.parser.CandidateAnalysis} is the representation of a
syntactic derivation considered by the {@link inpro.irmrsc.parser.SITDBSParser}.
<br />
The corresponding IU processor for the parser is
{@link inpro.incremental.processor.TagParser}. The IUs are
{@link inpro.incremental.unit.CandidateAnalysisIU}s.
</dd>

<dt>{@link inpro.irmrsc.rmrs}</dt>
<dd>holds the implementation of RMRS with {@link inpro.irmrsc.rmrs.Formula}
being the main object of interest. Variables in the formula and its sub-
structures are treated as numeric IDs, that can be bound to 
{@link inpro.irmrsc.rmrs.Variable} objects in a
{@link inpro.irmrsc.rmrs.VariableEnvironment}.
<br />
The corresponding IU processor for the semantic combination is 
{@link inpro.incremental.processor.RMRSComposer}. The IUs are
{@link inpro.incremental.unit.FormulaIU}s.
<br />
The processor can be used in combination with a
{@link inpro.incremental.processor.RMRSComposer.Resolver} to resolve 
nominal expressions in a formula against objects in the world. Additionally
the composer can provide feedback to the parser, by requesting him to degrade
syntactic derivations not producing successfully referring semantic
representations.</dd>

<dt>{@link inpro.irmrsc.util}</dt>
<dd>provides helper classes for representing and loading the syntactic and 
semantic structures</dd>
</dl>
<p />
The intended module-pipeline is as follows: Input words are send to the
{@link inpro.incremental.processor.Tagger}, who adds {@link inpro.incremental.unit.TagIU}s,
with POS-tag and lemma information. The {@link inpro.incremental.processor.TagParser}
is incrementally fed with these tags, producing one 
{@link inpro.incremental.unit.CandidateAnalysisIU} for every derivation able to match
the input. These are then taken by the {@link inpro.incremental.processor.RMRSComposer}
to build {@link inpro.incremental.unit.FormulaIU}s.

<p />
The package-name stands for 'Incremental RMRS Construction'.

@author Andreas Peldszus
 */
package inpro.irmrsc;
