package jp.co.teng.android.moguratataki;

import android.app.Activity;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

public class GameView extends SurfaceView implements
	Callback, Runnable{

    public GameView(Activity activity) {
        super(activity);
        init();
    }

    /**
     * 初期化メソッド、コンストラクタから一度だけ呼ばれる
     */
    private void init() {

    }

	@Override
	public void run() {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		// TODO 自動生成されたメソッド・スタブ

	}

}
