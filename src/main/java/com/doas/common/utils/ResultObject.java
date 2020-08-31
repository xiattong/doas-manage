package com.doas.common.utils;

import java.util.HashMap;
import java.util.Map;

public class ResultObject extends HashMap<String, Object> {
	private static final long serialVersionUID = 1L;

	public ResultObject() {
		put("code", 0);
		put("msg", "操作成功");
	}

	public static ResultObject error() {
		return error(1, "操作失败");
	}

	public static ResultObject error(String msg) {
		return error(500, msg);
	}

	public static ResultObject error(int code, String msg) {
		ResultObject r = new ResultObject();
		r.put("code", code);
		r.put("msg", msg);
		return r;
	}

	public static ResultObject ok(String msg) {
		ResultObject r = new ResultObject();
		r.put("msg", msg);
		return r;
	}

	public static ResultObject ok(Map<String, Object> map) {
		ResultObject r = new ResultObject();
		r.putAll(map);
		return r;
	}

	public static ResultObject ok() {
		return new ResultObject();
	}

	@Override
	public ResultObject put(String key, Object value) {
		super.put(key, value);
		return this;
	}
}
