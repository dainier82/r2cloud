package ru.r2cloud.satellite.decoder;

import ru.r2cloud.jradio.Beacon;
import ru.r2cloud.jradio.BeaconSource;
import ru.r2cloud.jradio.FloatInput;
import ru.r2cloud.jradio.demod.AfskDemodulator;
import ru.r2cloud.jradio.gomx1.Gomx1;
import ru.r2cloud.jradio.gomx1.Gomx1Beacon;
import ru.r2cloud.model.ObservationRequest;
import ru.r2cloud.predict.PredictOreKit;
import ru.r2cloud.util.Configuration;

public class Gomx1Decoder extends TelemetryDecoder {

	public Gomx1Decoder(PredictOreKit predict, Configuration config) {
		super(predict, config);
	}

	@Override
	public BeaconSource<? extends Beacon> createBeaconSource(FloatInput source, ObservationRequest req) {
		AfskDemodulator demod = new AfskDemodulator(source, 4800, -1200, 3600, 2);
		return new Gomx1(demod);
	}
	
	@Override
	public Class<? extends Beacon> getBeaconClass() {
		return Gomx1Beacon.class;
	}
}
