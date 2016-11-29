package edu.isnap.hint;

import java.util.Date;

import edu.isnap.dataset.Assignment;
import edu.isnap.dataset.Dataset;

public abstract class ConfigurableAssignment extends Assignment implements Configurable {

	public ConfigurableAssignment(Dataset dataset, String name, Date end, boolean hasIDs,
			boolean graded, Assignment prequel) {
		super(dataset, name, end, hasIDs, graded, prequel);
	}

	public ConfigurableAssignment(Dataset dataset, String name, Date end, boolean hasNodeIDs) {
		super(dataset, name, end, hasNodeIDs);
	}
}