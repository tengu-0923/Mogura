package jp.co.teng.android.moguratataki;

import java.util.Date;

public class Mogura {

	private Date popTime;
	private Date removeTime;
	private boolean isAtacked;

	public Date getPopTime() {
		return popTime;
	}
	public Date getRemoveTime() {
		return removeTime;
	}
	public boolean isAtacked() {
		return isAtacked;
	}

	public void setAtacked() {
		this.isAtacked = true;
	}
	public void reset(Date popTime,long poppingMills) {
		this.isAtacked = false;
		this.popTime = popTime;
		this.removeTime = new Date(popTime.getTime() + poppingMills);
	}
}