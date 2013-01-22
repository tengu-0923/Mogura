package jp.co.teng.android.moguratataki;

import android.util.Log;

public class Mogura {

	public static final int MOG_DEAD = 0;
	public static final int MOG_ALLIVE = 1;
	public static final int MOG_ATACKED = 2;

	private int row;
	private int col;

	private long removeTime;
//	private long atackedTime;
	private int status;

	public Mogura(int row,int col){
		this.status = MOG_DEAD;
		this.row = row;
		this.col = col;
	}

	public int getRow() {
		return row;
	}

	public int getCol() {
		return col;
	}

	//	public long getPopTime() {
	//		return popTime;
	//	}
	public long getRemoveTime() {
		return removeTime;
	}
//	public long getAtackedTime() {
//		return atackedTime;
//	}
	public int getStatus() {
		return status;
	}

	public boolean setAtacked(long mogDeadspan) {
		Log.d("Moguratataki", "[touched]" + " status:" + status + " row:" + row + " col:" + col);
		if(this.status == MOG_ALLIVE){
			long curmills = System.currentTimeMillis();
			this.status = MOG_ATACKED;
//			this.atackedTime = curmills;
			this.removeTime = curmills + mogDeadspan;
			return true;
		}
		return false;
	}

	public void setDead() {
		this.status = MOG_DEAD;
	}

	public void reset(long poppingMills) {
		this.status = MOG_ALLIVE;
		//		this.popTime = popTime;
		this.removeTime = System.currentTimeMillis() + poppingMills;
		Log.d("Moguratataki", "[poped][reseted]" + " row:" + row + " col:" + col);

	}
}