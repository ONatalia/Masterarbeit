//** UpcommingEvent(s)
//templates for now

intro lr(st([cat: s],
		<lst(folgende),lst(termine), lst(liegen), lst(an),lst(signdp)>),
		comp,
		<>,
		<eintro(E)>,
		$ 1, § 2)
		
intro lr(st([cat: s],
		<lst(die), lst(termine), lst(sind),lst(signdp)>),
		comp,
		<>,
		<eintro(E)>,
		$ 1, § 0)
		
mid lr(st([cat: s],
		<st([cat: terminsuf, ref: E ],
			<lst(hast), lst(du)>)
		>),
		comp,
		<>,
		<emid(E)>,
		$ 1, § 0)
	

mid lr(st([cat: lex],
		<lst(den),lst(termin)>),
		postmod([cat: terminsuf, ref: E ]),
		<>,
		<>,
		$ 0, § 2)

//** time

// optional "isfrom"
isfrom lr(st([cat: lex], 
		< lst(von)>),
		premod([cat: from]),
		<>,
		<>,
		$ 0, § 1)
		
// state time period
ctime lr(st([cat: s],
		<st([cat: from],<st([cat: untiltime, ref: E],
			<st([cat:time, ref: D])>)
		>)>),
		comp,
		<>,
		<isfrom(E,D)>,
		$ 0 , § 0)
			
// state time period start			
timeuntil lr(st([cat: untiltime, ref: E],
		<lst(bis),st([cat:time, ref: D])>),
		comp,
		<>,
		<isuntil(E,D)>,
		$ 0 , § 2)

// state time period start as postmod		
timeuntil lr(st([cat: untiltime, ref: E],
		<lst(bis),st([cat:time, ref: D])>),
		postmod([cat: untiltime,ref:E]),
		<>,
		<isuntil(E,D)>,
		$ 0 , § 2)

			
// state time period, some special construct to leave out "uhr" at start
ctime lr(st([cat: s],
		<st([cat: from],<lst(varhour,H)>),
		  lst(bis),st([cat:time, ref: D2])>),
		comp,
		<>,
		<isfrom(E,D1),isuntil(E,D2),hastime(D1,H)>,
		$ 0 , § 2)

//  state time, hour+min
time lr(st([cat: time, ref: D],
		<lst(varhour,H),lst(uhr),lst(varmin,M)>),
		comp,
		<>,
		<hastime(D,H,M)>)


// state time, only full hours 
time lr(st([cat: time, ref: D],
		<lst(varhour,H),lst(uhr)>),
		comp,
		<>,
		<hastime(D,H)>)

//** date
// short description of date
date lr(st([cat: s],
		<st([cat:comming, ref: D],<lst(am)>),
		  st([cat:day,ref:D])>),
		comp,
		<>,
		<isat(E,D),isnext(D),hasdaynr(D,W),hasmonth(D,M)>,
		$ 2, § 0)

			
// full description of date
date lr(st([cat: s],
		<st([cat:comming, ref: D],<lst(am)>),
		  st([cat:day,ref:D]),
		  st([cat:date, ref: D])>),
		comp,
		<>,
		<isat(E,D)>,
		$ 2, § 1)
			
// concrete date, e.g. "den 25. Maerz"
datespec lr(st([cat: date, ref:D],
		<lst(den),lst(vardaynr,W),lst(varmonthname,M)>),
		comp,
		<>,
		<hasdaynr(D,W),hasmonth(D,M)>,
		$ 0, § 2)

// skipping the date as it is already known (month and day)			
skipdate lr(st([cat: s],
		<st([cat: dateadd],
		  <lst(danach),lst(signdp)>)>),
		comp,
		<hasdaynr(D1,W),hasmonth(D1,M)>,
		<isat(E,D2),hasdaynr(D2,W),hasmonth(D2,M)>,
		$ 0, § 0)
			
// additional verbosity
skipdateadd lr(st([cat: lex],
		<lst(am),lst(selben),lst(tag)>),
		premod([cat: dateadd]),
		<>,
		<>,
		$ 0, § 2)

// special case for moved event, it was moved to the same date
skipdate lr(st([cat: s],
		<lst(signnop)>),
		comp,
		<mmid(I), hasdaynr(D1,W),hasmonth(D1,M)>,
		<isat(E2,D2),hasdaynr(D2,W),hasmonth(D2,M)>,
		$ -3, § 0)

// date is today	
today lr(st([cat: s],
		<st([cat:datered,ref:D],
		  <lst(heute)>)>),
		comp,
		<>,
		<today(D),isat(E,D),hasday(D,W),hasdaynr(D,WNR),hasmonth(D,M)>,
		$ 1, § 0)

// date is tmr
tomorrow lr(st([cat: s],
		<st([cat:datered,ref:D],
		  <lst(morgen)>)>),
		comp,
		<>,
		<istmr(D),isat(E,D),hasday(D,W),hasdaynr(D,WNR),hasmonth(D,M)>,
		$ 1, § 0)
			
// adding redundant date information
datespecred lr(st([cat: datered, ref:D],
		<lst(signnop),st([cat: date,ref:D])>),
		postmod([cat:datered,ref:D]),
		<>,
		<>,
		$ 0, § 0)

// optional word to indicate date comming soon
comming lr(st([cat: lex,ref: D], 
		< lst(kommenden)>),
		postmod([cat: comming,ref: D]),
		<>,
		<isnext(D)>,
		$ 0, § 1)

// dayname of the day
day lr(st([cat:day, ref:D],
		<lst(vardayname,W)>),
		comp,
		<>,
		<hasday(D,W)>)

//** combined date+time

// skipping date and time for directly following event
skipall lr(st([cat: s],
		<lst(direkt),lst(danach),lst(bis),st([cat:time, ref: D1])>),
		comp,
		<isuntil(E1,D0)>,
		<isat(E2,D0),isfrom(E2,D0),isuntil(E2,D1)>,
		$ -2, § 0)

// skipping date but extra verbose time for directly following event
skipall lr(st([cat: s],
		<lst(direkt),lst(danach),lst(von),st([cat: time, ref: D0]),lst(bis),st([cat:time, ref: D1])>),
		comp,
		<isuntil(E1,D0)>,
		<isat(E2,D0),isfrom(E2,D0),isuntil(E2,D1)>,
		$ -2, § 4)

// skipping date and time for event following one hour later
skipall lr(st([cat: s],
		<lst(eine),lst(stunde),lst(danach),st([cat: untiltime, ref: E2])>),
		comp,
		<isuntil(E1,D0)>,
		<isat(E2,D1),deltahour(E1,E2),isfrom(E2,D1)>,
		$ -2, § 0)
			
// skipping date and time for event following some minutes later
skipall lr(st([cat: s],
		<lst(deltamin,X),lst(minuten),st([cat: untiltime, ref: E2],
		  <lst(danach)>)>),
		comp,
		<isuntil(E1,D0)>,
		<isat(E2,D1),deltamin(E1,E2,X),isfrom(E2,D1)>,
		$ -2, § 0)
			
// skipping date and time for event following some hours later
skipall lr(st([cat: s],
		<lst(deltahours,X),lst(stunden),lst(danach),st([cat: untiltime, ref: E2])>),
		comp,
		<isuntil(E1,D0)>,
		<isat(E2,D1),deltahours(E1,E2,X),isfrom(E2,D1)>,
		$ -2, § 0)

//**  Title
//different ways to announce subject, just template for now
						
subject lr(st([cat: lex],
		<lst(mit),lst(dem)>),
		premod([cat:subjectpre2]),
		<>,
		<>,
		$ 0, § 1)
			
subject lr(st([cat: lex],
		<st([cat: subjectpre2],
		  <lst(betreff)>)>),
		premod([cat:subjectpre1]),
		<>,
		<>,
		$ 0, § 1)

subject lr(st([cat: s],
		<st([cat: subjectpre1],
		  <lst(varsubject,T)>)>),
		comp,
		<>,
		<hassubject(E,T)>,
		$ 0, § 0)
			
//** DetectedConflict

intro lr(st([cat: s],
		<lst(der),lst(nächste),lst(termin)>),
		comp,
		<>,
		<cintro(E)>,
		$ 1, § 2)
		
mid lr(st([cat: s],
		<lst(führt), lst(zu), lst(einem),lst(konflikt),lst(mit),lst(dem),lst(termin),lst(signdp)>),
		comp,
		<>,
		<cmid(E)>,
		$ 1, § 2)
		
mid lr(st([cat: s],
		<lst(überschneidet),lst(sich),lst(mit),lst(dem),lst(termin),lst(signdp)>),
		comp,
		<>,
		<cmid(E)>,
		$ 1, § 1)
		
//** MovedEvent

intro lr(st([cat: s],
		<lst(der),lst(termin)>),
		comp,
		<>,
		<mintro(E)>,
		$ 1, § 2)
		
mid lr(st([cat: s],
		<lst(wurde),lst(verschoben),lst(auf),lst(eine),lst(neue),lst(zeit)  >),
		comp,
		<>,
		<mmid(E)>,
		$ 1, § 2)

mid lr(st([cat: s],
		<lst(liegt),lst(nun)>),
		comp,
		<>,
		<mmid(E)>,
		$ 1, § 1)
		
//** div

intermission lr(st([cat: s],
		<st([cat:aehm],
		  <lst(aehemm)>)>),
		comp,
		<>,
		<inter(E)>,
		$ 1, § 0)
					
intermission lr(st([cat: lex],
		<lst(ich), lst(meine)>),
		postmod([cat:aehm]),
		<>,
		<>,
		$ 0, § 2)
					
and lr(st([cat:root],
		< lst(und),st([cat:s])>),
		comp,
		<>,
		<and(U)>,
		$ 1, § 0)
					
endpoint lr(st([cat:root],
		< st([cat:s]),lst(signp)>),
		comp,
		<>,
		<fin(U)>,
		$ 1, § 0)

start lr(st([cat:root],
		< st([cat:s])>),
		comp,
		<>,
		<>,
		$ 0, § 0)
