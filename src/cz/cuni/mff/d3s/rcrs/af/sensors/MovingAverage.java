package cz.cuni.mff.d3s.rcrs.af.sensors;

import static cz.cuni.mff.d3s.rcrs.af.Configuration.MOVING_AVERAGE_LENGTH;

public class MovingAverage {
	
	private double[] values;
	private int index;
	private boolean isFilled;
	
	public MovingAverage() {
		values = new double[MOVING_AVERAGE_LENGTH];
		index = 0;
		isFilled = false;
	}
	
	public void addValue(double value) {
		values[index] = value;
		index = (index + 1) % MOVING_AVERAGE_LENGTH;
		
		if(!isFilled && index == 0) {
			isFilled = true;
		}
	}
	
	public double getAverage() {
		if(isFilled) {
			double sum = 0;
			for(int i = 0; i < MOVING_AVERAGE_LENGTH; i++) {
				sum += values[i];
			}
			return sum / MOVING_AVERAGE_LENGTH;
		}
		
		if(index == 0) {
			return 0;
		}
		
		double sum = 0;
		for(int i = 0; i < index; i++) {
			sum += values[i];
		}
		return sum / index;
	}

}
