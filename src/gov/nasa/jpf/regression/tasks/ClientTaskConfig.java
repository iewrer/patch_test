package gov.nasa.jpf.regression.tasks;



public class ClientTaskConfig {
	
	public enum Tasks {
		ImpactBranchCoverage,
		ImpactBasicBlockCoverage,
		ImpactPathFragmentCoverage,
		ImpactDataFlowCoverage,
		Debugging,
		PartitionEquivalence,
		PartitionEffectsEquivalence,
		FunctionalEquivalence
	}
	
	protected String analysisName;
	
	public static boolean impactBranchCoverage = false;
	public static boolean impactBasicBlockCoverage = false;
	public static boolean impactPathFragementCoverage = false;
	// this is the default version of DiSE and iDiSE
	public static boolean impactDataFlowCoverage = true;
	public static boolean debugging = false;
	public static boolean partitionEquivalence = false;
	public static boolean partitionEffectsEquivalence = false;
	public static boolean functionEquivalence = false;
	
	public ClientTaskConfig(String analysisName) {
		this.analysisName = analysisName;
		
	}
	
	public void initialize() {
		Tasks clientTask = Tasks.valueOf(analysisName);
		switch(clientTask) {
		case ImpactBranchCoverage: 
			ClientTaskConfig.impactBranchCoverage = true;
			break;
		case ImpactBasicBlockCoverage:
			ClientTaskConfig.impactBasicBlockCoverage = true;
			break;
		case ImpactPathFragmentCoverage:
			ClientTaskConfig.impactPathFragementCoverage = true;
			break;
		case ImpactDataFlowCoverage:
			ClientTaskConfig.impactDataFlowCoverage = true;
			break;
		case Debugging:
			ClientTaskConfig.debugging = true;
			break;
		case PartitionEquivalence: 
			ClientTaskConfig.partitionEquivalence = true;
			break;
		case PartitionEffectsEquivalence:
			ClientTaskConfig.partitionEffectsEquivalence = true;
			break;
		case FunctionalEquivalence : 
			ClientTaskConfig.functionEquivalence = true;
			break;
		}
	}
	
	public static boolean isImpactedBranchCoverage() {
		return ClientTaskConfig.impactBranchCoverage;
	}
	
	public static boolean isImpactedBasicBlockCoverage() {
		return ClientTaskConfig.impactBasicBlockCoverage;
	}
	
	public static boolean isImpactedPathFragCoverage(){ 
		return ClientTaskConfig.impactPathFragementCoverage;
	}
	
	public static boolean isImpactedDataFlowCoverage() {
		return ClientTaskConfig.impactDataFlowCoverage;
	}
	
	public static boolean isDebugging() {
		return ClientTaskConfig.debugging;
	}
	
	public static boolean isPartitionEquivalence(){ 
		return ClientTaskConfig.partitionEquivalence;
	}
}