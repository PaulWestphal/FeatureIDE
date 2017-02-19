package de.ovgu.featureide.examples.elevator.core.controller;

import de.ovgu.featureide.examples.elevator.core.model.Elevator;

public interface ITickListener {

	void onTick(Elevator elevator);
	
	//#if FIFO
	void onRequestFinished(Elevator elevator, Request request);
	//#endif
}
