package xander.gfws.data;

public class KNNParms {

	public static enum SelectBy {
		KNN, RANGE, HITS_WITHIN_KNN_VISITS;
	}
	
	private SelectBy selectBy       = SelectBy.KNN;
	private int minK                = 1;
	private int maxK                = 20;
	private double kPercentOfTotal  = 0.12d;
	private double visitKPercentOfHitK = 10d;  // by default, 10x as many visits as hits
	private double[] rangeSpan;
	private int knnWhenSelectionEmpty   = 1;
	double visitDistance;
	
	public KNNParms(int minK, int maxK, double kPercentOfTotal) {
		this.minK = minK;
		this.maxK = maxK;
		this.kPercentOfTotal = kPercentOfTotal;
	}
	
	public KNNParms(int rangeDimensions) {
		this.selectBy = SelectBy.RANGE;
		this.rangeSpan = new double[rangeDimensions];
	}
	
	public KNNParms(double... rangeSpan) {
		this.selectBy = SelectBy.RANGE;
		this.rangeSpan = rangeSpan;
	}
	
	public void setSelectBy(SelectBy selectBy) {
		this.selectBy = selectBy;
	}

	public boolean isSelectByKNN() {
		return selectBy == SelectBy.KNN;
	}
	
	public boolean isSelectByRange() {
		return selectBy == SelectBy.RANGE;
	}
	
	public boolean isSelectByHitsWithinKNNVisits() {
		return selectBy == SelectBy.HITS_WITHIN_KNN_VISITS;
	}
	
	public double[] getRangeSpan() {
		return rangeSpan;
	}

	public void setRangeSpan(double[] rangeSpan) {
		this.rangeSpan = rangeSpan;
	}

	public int getKnnWhenSelectionEmpty() {
		return knnWhenSelectionEmpty;
	}

	public void setKnnWhenSelectionEmpty(int knnWhenSelectionEmpty) {
		this.knnWhenSelectionEmpty = knnWhenSelectionEmpty;
	}

	public void setVisitKPercentOfHitK(double visitKPercentOfHitK) {
		this.visitKPercentOfHitK = visitKPercentOfHitK;
	}

	public int getK(int totalPoints) {
		int hitK = (int)Math.round(kPercentOfTotal*(double)totalPoints);
		return Math.max(Math.min(hitK, maxK), minK);
	}
	
	public int getVisitKAsPercentOfHitK(int totalHits) {
		int hitK = getK(totalHits);
		return (int)Math.round(visitKPercentOfHitK*(double)hitK);
	}
}
