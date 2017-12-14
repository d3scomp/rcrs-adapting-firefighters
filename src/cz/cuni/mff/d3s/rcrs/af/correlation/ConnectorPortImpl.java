package cz.cuni.mff.d3s.rcrs.af.correlation;

import java.util.Set;

import cz.cuni.mff.d3s.metaadaptation.correlation.ConnectorPort;
import cz.cuni.mff.d3s.metaadaptation.correlation.Kind;

public class ConnectorPortImpl implements ConnectorPort {

	private final Set<String> assumedKnowledge;
	private final Kind kind;

	public ConnectorPortImpl(Set<String> assumedKnowledge, Kind kind) {
		if (assumedKnowledge == null) {
			throw new IllegalArgumentException(String.format("The \"%s\" argument is null.", "assumedKnowledge"));
		}

		this.assumedKnowledge = assumedKnowledge;
		this.kind = kind;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ConnectorPortImpl)) {
			return false;
		}

		ConnectorPortImpl other = (ConnectorPortImpl) obj;
		return this.assumedKnowledge.equals(other.assumedKnowledge) && this.kind.equals(other.kind);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return assumedKnowledge.hashCode() + 17 * kind.hashCode();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();

		builder.append("Assumed knowledge:\n)");
		for (String ek : assumedKnowledge) {
			builder.append("\t").append(ek).append("\n");
		}
		builder.append("Kind: ").append(kind).append("\n");

		return builder.toString();
	}

}
