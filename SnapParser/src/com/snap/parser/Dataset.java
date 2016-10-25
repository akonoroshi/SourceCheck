package com.snap.parser;

import java.util.Date;

public abstract class Dataset {
	public final Date start;
	public final String dataDir, dataFile;

	public abstract Assignment[] all();

	public Dataset(Date start, String dataDir) {
		this.start = start;
		this.dataDir = dataDir;
		this.dataFile = dataDir + ".csv";
	}

}