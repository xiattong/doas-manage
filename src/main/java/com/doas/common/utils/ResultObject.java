package com.doas.common.utils;

import java.util.LinkedHashMap;

/**
 * 返回结果封装
 * @author xiattong
 */
public class ResultObject extends LinkedHashMap<String, Object> {
	private static final long serialVersionUID = 1L;

	public static ResultObject basic() {
		ResultObject r = new ResultObject();
		r.put("code", 0);
		return r;
	}

	public static ResultObject ok() {
		ResultObject r = new ResultObject();
		r.put("code", 1);
		r.put("msg", "操作成功");
		return r;
	}

	public static void setOk(ResultObject result) {
		result.put("code", 1);
		result.put("msg", "操作成功");
	}

	public static ResultObject error(String msg) {
		ResultObject r = new ResultObject();
		r.put("code", -1);
		r.put("msg", msg);
		return r;
	}

	@Override
	public ResultObject put(String key, Object value) {
		super.put(key, value);
		return this;
	}
}
