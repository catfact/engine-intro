EngineIntro_NoiseSine {
	var synth;
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

			synth = Synth.new(\noise_sine, [], server);

			modBus = Bus.control(server, 1);

			synth.map(\mod, modBus);

			modSource = Synth.new(\saw, [\out, modBus], synth, \addBefore);


		}.play;
	}

	//-- glue
	amp { arg value; synth.set(\amp, value); }
	hz { arg value; synth.set(\hz, value); }
	modHz { arg value; modSource.set(\hz, value); }
	modScale { arg value; modSource.set(\scale, value); }
	modOffset{ arg value; modSource.set(\offset, value); }

	free {
		synth.free;
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
