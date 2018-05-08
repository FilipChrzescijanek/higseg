package pwr.chrzescijanek.filip.higseg.util;

import java.util.List;
import java.util.Map;

public class ModelDto {

	private final Integer type;
	private final List<String> clazzValues;
	private final String rules;
	private final Map<String, Double> means;
	private final Map<String, Double> variances;
	private final Map<String, Double> bottomValues;
	private final Map<String, Double> topValues;
	
	public ModelDto(Integer type, List<String> clazzValues, String rules, Map<String, Double> means,
			Map<String, Double> variances, Map<String, Double> bottomValues, Map<String, Double> topValues) {
		this.type = type;
		this.clazzValues = clazzValues;
		this.rules = rules;
		this.means = means;
		this.variances = variances;
		this.bottomValues = bottomValues;
		this.topValues = topValues;
	}

	public Integer getType() {
		return type;
	}

	public List<String> getClazzValues() {
		return clazzValues;
	}

	public String getRules() {
		return rules;
	}

	public Map<String, Double> getMeans() {
		return means;
	}

	public Map<String, Double> getVariances() {
		return variances;
	}

	public Map<String, Double> getBottomValues() {
		return bottomValues;
	}

	public Map<String, Double> getTopValues() {
		return topValues;
	}
	
}
