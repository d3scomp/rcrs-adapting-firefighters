package cz.cuni.mff.d3s.rcrs.af.buildings;

import static cz.cuni.mff.d3s.rcrs.af.buildings.OccupancyData.OBSERVATION_LENGTH;
import static cz.cuni.mff.d3s.rcrs.af.buildings.OccupancyData.DAY_LENGTH;

import cz.cuni.mff.d3s.tss.arima.ArimaModel;
import cz.cuni.mff.d3s.tss.arima.ArimaOrder;

public class OccupancyModel {
	
	private int[] trainingDataSum;
	private int trainingDataCnt;
	private ArimaModel arimaModel;
	
	public OccupancyModel() {
		trainingDataSum = new int[OBSERVATION_LENGTH];
		for(int i = 0; i < OBSERVATION_LENGTH; i++) {
			trainingDataSum[i] = 0;
		}
		trainingDataCnt = 0;
		arimaModel = null;
	}
	
	public void trainModel(int arimaP, int arimaD, int arimaQ) {
		if(trainingDataCnt == 0) {
			throw new UnsupportedOperationException("Can't provide a model when there are no training data!");
		}
		
		arimaModel = new ArimaModel(getAveragedTrainingData(), DAY_LENGTH,
				new ArimaOrder(arimaP, arimaD, arimaQ));
	}
	
	public ArimaModel getModel() {
		if(arimaModel == null) {
			throw new UnsupportedOperationException("Can't provide a model without training!");
		}
		
		return arimaModel;
	}
	
	public void addTrainingData(int[] trainingData) {
		for(int i = 0; i < OBSERVATION_LENGTH; i++) {
			trainingDataSum[i] += trainingData[i];
		}
		trainingDataCnt++;
	}
	
	public double[] getAveragedTrainingData() {
		double[] data = new double[OBSERVATION_LENGTH];
		for(int i = 0; i < OBSERVATION_LENGTH; i++) {
			data[i] = trainingDataSum[i] / trainingDataCnt;
		}
		
		return data;
	}
}
