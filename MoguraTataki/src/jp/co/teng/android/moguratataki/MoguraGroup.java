package jp.co.teng.android.moguratataki;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.util.Log;

public class MoguraGroup {

	private List<Mogura> moguras = new ArrayList<Mogura>();
	private List<Mogura> liveMoguras = new ArrayList<Mogura>();
	private int rows;
	private int cols;

	private int livemgIndex;

	private Random random = new Random();
	private Long mogLifespan;
	private Long mogDeadspan;

	public void setMogLifespan(Long mogLifespan) {
		this.mogLifespan = mogLifespan;
	}

	public void setMogDeadspan(Long mogDeadspan) {
		this.mogDeadspan = mogDeadspan;
	}

	public MoguraGroup(int rows,int cols){
		this.rows = rows;
		this.cols = cols;
		this.livemgIndex = 0;
		for (int row=0; row < rows; row++) {
			for (int col=0; col < cols; col++) {
				moguras.add(new Mogura(row,col));
			}
		}
	}

	public boolean setMogAtacked(int row,int col){
		return moguras.get(getMogId(row,col)).setAtacked(mogDeadspan);
	}

	//とりあえず一番簡単なもぐら発生ロジック
	public void popMoguraAtRandom(){
		Mogura mog;
		for(int i = 0; i < moguras.size() ;i++){
			mog = moguras.get(i);
			if(mog.getStatus() == Mogura.MOG_DEAD){
				//TODO マシンスペックによってもぐら発生数が変わってしまうのはいかがなものか
				//→FPSに依存しないロジック：前回ループからの経過時間を基に計算？
				//→ループの負荷を減らす：前もぐらのループを回さない？
				if(random.nextInt(1000) > 980){
					mog.popup(mogLifespan);
					liveMoguras.add(mog);
					Log.d("Moguratataki", "[poped]" + " row:" + i/cols + " col:" + i%rows);
				}
			}
		}
	}

	public Mogura getLiveMogura(){
		Mogura mog;
		long curmills = System.currentTimeMillis();
		if( livemgIndex >= liveMoguras.size()){
			mog = null;
		}else{
			mog = liveMoguras.get(livemgIndex);
			if(mog.getStatus() != Mogura.MOG_DEAD && mog.getRemoveTime() < curmills){
				mog.setDead();
				liveMoguras.remove(livemgIndex);
			}else{
				livemgIndex++;
			}
		}
		return mog;
	}

	public void refleshIndex(){
		this.livemgIndex = 0;
	}

	public void reflesh(){
		for (int row=0; row < rows; row++) {
			for (int col=0; col < cols; col++) {
				moguras.get(getMogId(row,col)).reset();
			}
		}
		liveMoguras.clear();
		livemgIndex = 0;
	}

	private int getMogId(int row, int col){
		return row * cols + col;
	}
}
