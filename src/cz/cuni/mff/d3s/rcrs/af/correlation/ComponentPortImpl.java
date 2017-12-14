package cz.cuni.mff.d3s.rcrs.af.correlation;

import java.util.Set;

import cz.cuni.mff.d3s.metaadaptation.correlation.ComponentPort;

public class ComponentPortImpl implements ComponentPort {
	
	private final Set<String> exposedKnowledge;

	public ComponentPortImpl(Set<String> exposedKnowledge) {
		if (exposedKnowledge == null) {
			throw new IllegalArgumentException(String.format("The \"%s\" argument is null.", "exposedKnowledge"));
		}

		this.exposedKnowledge = exposedKnowledge;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ComponentPortImpl)) {
			return false;
		}

		ComponentPortImpl other = (ComponentPortImpl) obj;
		return this.exposedKnowledge.equals(other.exposedKnowledge);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return exposedKnowledge.hashCode();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		builder.append("Exposed knowledge:\n)");
		for (String ek : exposedKnowledge) {
			builder.append("\t").append(ek).append("\n");
		}

		return builder.toString();
	}

	public Set<String> getExposedKnowledge() {
		return exposedKnowledge;
	}
}
