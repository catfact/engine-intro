EngineIntro_NoiseSine {
	classvar numVoices = 16;
	var synth;
	var synthGroup;
	var modBus;
	var modSource;

	alloc {
		arg server;
		Routine {
			SynthDef.new('noise_sine', {
				arg out=0, amp=0.5, hz=110, mod=0;
				var carrier, modulator, hz_mod;
				modulator = [PinkNoise.ar, PinkNoise.ar];
				hz_mod = hz * (2 ** (modulator * mod));
				carrier = SinOsc.ar(hz_mod);
				Out.ar(out, carrier * amp);
			}).send(server);

			SynthDef.new(\saw, {
				arg out, hz, scale, offset;
				var signal;
				signal = LFSaw.kr(freq:hz, mul:scale, add:offset);
				Out.kr(out, signal);
			}).send(server);

			server.sync;
			synthGroup = Group.new(server);
			modSource = Synth.new(\saw, [\out, modBus], synthGroup, \addBefore);

			synth = Array.fill(numVoices, {
				arg index;
				var voice;
				voice = Synth.new(\noise_sine, [], synthGroup);
				voice.map(\mod, modBus);
				voice
			});

			modBus = Bus.control(server, 1);

		}.play;
	}

	//-- glue
	amp { arg value; synth.set(\amp, value); }
	hz { arg value; synth.set(\hz, value); }
	modHz { arg value; modSource.set(\hz, value); }
	modScale { arg value; modSource.set(\scale, value); }
	modOffset{ arg value; modSource.set(\offset, value); }

	free {
		synthGroup.free;
		modSource.free;
		modBus.free;
	}
}


Engine_Intro_NoiseSine : CroneEngine {
	var <>kernel; //an Emb_NoiseSine

	*new { arg context, doneCallback;
		^super.new(context, doneCallback);
	}

	alloc {
		kernel.alloc(Crone.server);

		//... add commands and polls

		addCommand(\hz, {
			arg msg;
			kernel.hz(msg[1]);
		});

		addCommand(\amp, {
			arg msg;
			kernel.amp(msg[1]);
		});
	}

	free {
		kernel.free;
	}

}
