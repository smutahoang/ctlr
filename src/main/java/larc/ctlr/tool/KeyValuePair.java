package larc.ctlr.tool;

public class KeyValuePair implements Comparable<KeyValuePair> {
	private int intKey;
	private String strKey;

	private int intValue;
	private double doubleValue;
	private String strValue;

	private enum ValueType {
		intType, doubleType, strType
	};

	private ValueType valueType;

	public KeyValuePair(int _intKey, int _intValue) {
		intKey = _intKey;
		intValue = _intValue;
		valueType = ValueType.intType;
	}

	public KeyValuePair(int _intKey, double _doubleValue) {
		intKey = _intKey;
		doubleValue = _doubleValue;
		valueType = ValueType.doubleType;
	}

	public KeyValuePair(String _strKey, double _doubleValue) {
		strKey = _strKey;
		doubleValue = _doubleValue;
		valueType = ValueType.strType;
	}

	public int getIntKey() {
		return intKey;
	}

	public String getStrKey() {
		return strKey;
	}

	public int getIntValue() {
		return intValue;
	}

	public double getDoubleValue() {
		return doubleValue;
	}

	public String getStrValue() {
		return strValue;
	}

	public int compareTo(KeyValuePair o) {
		if (valueType == ValueType.doubleType) {
			if (o.getDoubleValue() > doubleValue)
				return -1;
			if (o.getDoubleValue() < doubleValue)
				return 1;
			return 0;
		} else if (valueType == ValueType.intType) {
			if (o.getIntValue() > intValue)
				return -1;
			if (o.getIntValue() < intValue)
				return 1;
			return 0;
		} else {
			return strValue.compareTo(o.getStrValue());
		}
	}

}
