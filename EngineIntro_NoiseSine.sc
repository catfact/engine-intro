EngineIntro_NoiseSine {
	classvar numVoices = 16;
	classvar delayTime = 32;
	var <synth;
	var <synthGroup;
	var <modBus;
	var <modSource;
	var <delayBuffer;
	var <auxBus;
	var <fxSynth;

	alloc {
		arg server;

		Routine {
			SynthDef.new('noise_sine', {
				arg out=0, amp=0.5, hz=110, mod=0, pan=0,
				outAux, auxLevel=0;
				var carrier, modulator, hz_mod, output;
				modulator = [PinkNoise.ar, PinkNoise.ar];
				hz_mod = hz * (2 ** (modulator * mod));
				carrier = SinOsc.ar(hz_mod);
				output = Pan2.ar(carrier, pan, amp);
				Out.ar(out, output);
				Out.ar(outAux, output * auxLevel);
			}).send(server);

			SynthDef.new(\saw, {
				arg out, hz, scale, offset;
				var signal;
				signal = LFSaw.kr(freq:hz, mul:scale, add:offset);
				Out.kr(out, signal);
			}).send(server);

			SynthDef.new(\pingpong_delay, {
				arg buf_l, buf_r, in=0, out=0,
				amp=0.5, fb=0, time_l=1, time_r=1;
				var input, localin, del, del_l, del_r,
				time_lag_l, time_lag_r;
				input = In.ar(in, 2);
				localin = LocalIn.ar(2);
				time_lag_l = Lag.ar(K2A.ar(time_l), 0.01);
				time_lag_r = Lag.ar(K2A.ar(time_r), 0.01);
				del_l = BufDelayC.ar(buf_l, input[0] + (fb * localin[1]), time_lag_l);
				del_r = BufDelayC.ar(buf_r, input[1] + (fb * localin[0]), time_lag_r);
				del = [del_l, del_r];
				LocalOut.ar(del);
				Out.ar(out, del * amp);
			}).send(server);

			delayBuffer = Array.fill(2, {
				Buffer.alloc(server, server.sampleRate * delayTime);
			});

			server.sync;
			synthGroup = Group.new(server);

			modBus = Bus.control(server, 1);
			modSource = Synth.new(\saw, [\out, modBus], synthGroup, \addBefore);

			synth = Array.fill(numVoices, {
				arg index;
				var voice;
				voice = Synth.new(\noise_sine, [
					\outAux, auxBus
				], synthGroup);
				voice.map(\mod, modBus);
				voice
			});

			fxSynth = Synth.new(\pingpong_delay, [
				\in, auxBus,
				\buf_l, delayBuffer[0].bufnum,
				\buf_r, delayBuffer[1].bufnum
			], synthGroup, \addAfter);

		}.play;
	}

	//-- glue
	amp { arg index, value; synth[index].set(\amp, value); }
	hz { arg index, value; synth[index].set(\hz, value); }
	pan { arg index, value; synth[index].set(\pan, value); }
	send { arg index, value; synth[index].set(\auxLevel, value); }

	modHz { arg value; modSource.set(\hz, value); }
	modScale { arg value; modSource.set(\scale, value); }
	modOffset{ arg value; modSource.set(\offset, value); }

	delay_amp { arg val; fxSynth.set(\amp, val); }
	delay_fb { arg val; fxSynth.set(\fb, val); }
	delay_time_l { arg val; fxSynth.set(\time_l, val); }
	delay_time_r { arg val; fxSynth.set(\time_r, val); }

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

	addVoiceParam {
		arg name;
		addCommand(name, "if", {
			arg msg;
			//kernel.hz(msg[1], msg[2]);
			kernel.hz(msg[1], msg[2]);
		});
	}

	alloc {
		kernel.alloc(Crone.server);

		addCommand(\hz, "if", {
			arg msg;
			kernel.hz(msg[1], msg[2]);
		});

		addCommand(\amp, "if", {
			arg msg;
			kernel.amp(msg[1], msg[2]);
		});

		addCommand(\pan, "if", {
			arg msg;
			kernel.pan(msg[1], msg[2]);
		});


		addCommand(\send, "if", {
			arg msg;
			kernel.send(msg[1], msg[2]);
		});

		addCommand(\delay_amp, "f", {
			arg msg;
			kernel.amp(msg[1]);
		});
		addCommand(\delay_fb, "f", {
			arg msg;
			kernel.fb(msg[1]);
		});
		addCommand(\delay_time, "f", {
			arg msg;
			kernel.time(msg[1]);
		});
		addCommand(\delay_time, "f", {
			arg msg;
			kernel.time(msg[1]);
		});

	}
	free {
		kernel.free;
	}

}
