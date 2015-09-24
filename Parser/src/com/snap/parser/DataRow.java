package com.snap.parser;

import java.io.Serializable;
import java.util.Date;

import com.snap.data.Snapshot;

public class DataRow implements Serializable {
	private static final long serialVersionUID = 1L;
	
	public final Snapshot snapshot;
	public final Date timestamp;
	
	@SuppressWarnings("unused")
	private DataRow() { 
		this(null, null);
	}
	
	public DataRow(Date timestamp, String snapshotXML) {
		this.timestamp = timestamp;
		this.snapshot = Snapshot.parse(null, snapshotXML);
	}
	
	
}