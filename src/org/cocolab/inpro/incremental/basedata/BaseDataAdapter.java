package org.cocolab.inpro.incremental.basedata;

import edu.cmu.sphinx.frontend.BaseDataProcessor;
import edu.cmu.sphinx.frontend.Data;
import edu.cmu.sphinx.frontend.DataProcessingException;
import edu.cmu.sphinx.frontend.Signal;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Component;
import edu.cmu.sphinx.util.props.S4String;

public class BaseDataAdapter extends BaseDataProcessor {

	@S4Component(type = BaseData.class)
	public static final String PROP_BASE_DATA = "baseData";
	BaseData baseData = null;
	
	@S4String(mandatory = true)
	public static final String PROP_DATA_TYPE = "dataType";
	String dataType = "";
	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException {
		baseData = (BaseData) ps.getComponent(PROP_BASE_DATA);
		dataType = ps.getString(PROP_DATA_TYPE);
	}
	
	@Override
	public Data getData() throws DataProcessingException {
		Data d = getPredecessor().getData();
		if (!(d instanceof Signal) && (baseData != null))
			baseData.addData(d, dataType);
		return d;
	}

}
