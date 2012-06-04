/**
 * Some example systems built with InproTK, which however may not run out-of-the-box.
 * Also, these systems are more likely to suffer bitrot than those in demo.inpro, as 
 * we do not test them on a regular basis. 
 * 
 * <dl>
 * <dt>inpro.system.carchase
 * <dd>a system that incrementally comments on events in a simple car chase environment.
 * 
 * This system is further described in:<br>
 * Timo Baumann and David Schlangen (2012):<br>
 * "Inpro_iSS: A Component for Just-In-Time Incremental Speech Synthesis",<br>
 * in: <em>Proceedings of ACL 2012 System Demonstrations</em>, Jeju Island, South Korea. 
 * 
 * You need to set up our incrementality extensions to MaryTTS for this system to work.
 * 
 * <dt>inpro.system.completion
 * <dd>A system that is able to co-complete/shadow a user's turn.
 * 
 * This system is further described in:<br>
 * Timo Baumann and David Schlangen (2011):<br>
 * "Predicting the Micro-Timing of User Input for an Incremental Spoken Dialogue System that Completes a User's Ongoing Turn",<br>
 * in: <em>Proceedings of SigDial 2011</em>, Portland, USA.
 * 
 * <!--
 * <dt>inpro.system.pentormrs
 * <dd>a demonstration of incremental RMRS in the Pentomino domain.
 * 
 * This system is further described in:<br>
 * Andreas Peldszus, Okko Buß, Timo Baumann and David Schlangen (2012):<br>
 * "Joint Satisfaction of Syntactic and Pragmatic Constraints Improves Incremental Spoken Language Understanding",<br>
 * in: <em>Proceedings of EACL 2012</em>, Avignon, France. 
 * -->
 * 
 * <dt>inpro.system.calendar
 * <dd>a system that incrementally reads out calendar entries and is responsive to 
 * noise events that interfer with its own speech. 
 * 
 * This system is further described in:<br>
 * Hendrik Buschmeier, Timo Baumann, Benjamin Dorsch, Stefan Kopp and David Schlangen (2012):<br>
 * "Combining Incremental Language Generation and Incremental Speech Synthesis for Adaptive Information Presentation",<br>
 * in: <em>Proceedings of SigDial 2012</em>, Seoul, South Korea. 
 * 
 * This system is by default (using the ant file, or using Eclipse) not
 * built as it requires the (proprietary) Java SPUD library, courtesy of 
 * David DeVault. We will help you set up this system if you can assure us  
 * that you are allowed to use JavaSPUD. If not, reading the code shuold still
 * be somewhat educational to show how combined incremental NLG and speech synthesis 
 * can be realized in InproTK.
 * 
 * You need to set up our incrementality extensions to MaryTTS for this system to work.
 * </dl>
 */
package done.inpro.system;

