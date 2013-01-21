//済 文字表示・スリープを一時的に組み込み
//済 待ち処理・カウントダウン
//済 タイマー処理
//→タイマー処理がUIスレッドからしか実行できない？？？要確認
//済 背景画像表示
//済 メッセージ表示箇所の調整・クリア処理
//→対象を指定したCanvasのクリア方法を明らかに ダブルバッファリングが鍵
//済 画面調整・画面表示のブラッシュアップ
//→背景は常に表示・もぐらは毎回再描画 SurfaceViewの仕様上、毎回再描画が必要
//TODO もぐら表示の実装
//TODO もぐらタッチの実装
//TODO １）タッチイベントを拾って、得点を内部に保存
//TODO ２）得点の表示・更新
//TODO ３）叩かれたもぐらの色変更・消えるタイミング変更
//TODO 結果画面表示 簡単でOK？
//TODO リソース開放
//TODO 連続プレイ時も問題がないか確認
//TODO 中断・再開が可能なよう実装

package jp.co.teng.android.moguratataki;


import jp.co.teng.android.moguratataki.DataLoader;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

public class GameView extends SurfaceView implements
Callback, Runnable{

	//初期化処理フラグ ユーザ入力待ちになるまではtrue
	//	private boolean initializing;
	private boolean surfaceCreated;

	private int status;
	//	private final int StatusNOP           = 0;
	private final int StatusInit          = 101; //内部状態の初期化中
	private final int StatusPrepare       = 102; //画面の初期化中
	private final int StatusReady         = 201; //初期化完了状態
	private final int StatusStarting      = 202; //次回開始待ち（カウントダウン中）
	private final int StatusPlaying       = 203; //起動中
	private final int StatusTimeup        = 204; //時間終了
	private final int StatusDisplayResult = 205; //結果表示処理中
	private final int StatusSelecting     = 206; //選択肢表示中
	private final int StatusExit          = 301; //終了処理中
	//	private final int StatusFling         = 9; //???
	//	private final int StatusPlaying       = 10;
	//	private final int StatusComplete      = 11;
	private final int StatusError         = 999;

	private final String MSG_START_WAITING      = "画面をタップで開始";
	private final String MSG_START_COUNTDOWN    = "開始まで：";
	private final String MSG_PLAYING            = "終了まで：";
	private final String MSG_TIMEUP            = "終了です。画面タップでもう一度";

	private final int LogD         = 1;
	private final int LogI         = 2;
	private final int LogW         = 3;
	private final int LogE         = 4;

	private final int sukima = 5;
	private final int rows  = 4;
	private final int cols  = 4;
	private final Long mogLifespan = (long)2000;

	private   Paint           textPaint;
	private   int             textSize;
	private   Paint           bgPaint;

	protected Bitmap imageBase;
	protected Bitmap imageMogura;
	private   DataLoader      loader;

	private   int             xOff;         // 駒表示時のXのオフセット
	private   int             yOff;         // 駒表示時のYのオフセット
	private   int             pWidth;       // 駒の幅
	private   int             pHeight;      // 駒の高さ
	private   float           scale;        // 座標計算用スケール

	private Mogura[] moguras;

	private final String APP_NAME         = "Moguratataki";

	private int atPlayStart;
	private int atPlayEnd;

	private SurfaceHolder   holder;
	private Thread thread;

	public GameView(Activity activity) {
		super(activity);
		init();
	}

	/**
	 * インスタンス初期化メソッド、コンストラクタから一度だけ呼ばれる
	 */
	private void init() {

		//TODO:デバッグコード
		outputLog(LogD, "init");

		holder = getHolder();
		holder.addCallback(this);

		surfaceCreated = false;
		thread         = null;

		loader         = new DataLoader(this);

		//		initializing = true;
		status = StatusInit;
		outputLog(LogD,"status change:" + status);

		//メインスレッド開始処理
		thread = new Thread(this);
		thread.start();
	}

	@Override
	public void run() {
		// TODO メインスレッド実装
		while(thread != null){
			//TODO:デバッグコード
			//			outputLog(LogD, "main thread runnning");

			switch(status){
			case StatusInit:
				//ゲーム開始準備（初回のみ）ブロック
				prepareGame();
				break;
			case StatusPrepare:
				//TODO StatusPrepareはInit中にまとめて実施？？？
				drowStartScreen();
				break;
			case StatusReady:
				//準備完了ブロック
				//メインスレッドではなにもしない。開始入力待ちになっているはず
				break;
			case StatusStarting:
				//開始処理ブロック
				//TODO ここでカウントダウン処理のトリガを引く。→UIスレッドからじゃないとうまくいかない。。
				//				startCountDown();
				//				repaint();
				break;
			case StatusPlaying:
				//再描画処理
				repaint();
				//もぐらポップアップ処理
				popMogura();
				break;
			case StatusTimeup:
				//プレイ終了処理(終了時のみ)
				exitPlaying();
				break;
			case StatusDisplayResult:
				//結果表示処理
				displayResult();
				break;
			case StatusSelecting:
				//選択肢表受注
				//メインスレッドではなにもしない。開始入力待ちになっているはず
				break;
			case StatusExit:
				//ゲーム終了処理
				finishGame();
				break;
			case StatusError:
				//エラー発生時処理
				break;
			default:
			}
		}
	}

	//***** 初期化処理 ******************************************************************

	/**
	 * ゲーム開始準備処理
	 */
	private void prepareGame(){
		if(!surfaceCreated){
			return;
		}

		//TODO prepareGameメソッド実装
		loader.loadImages(getContext());
		initGameParam();

		moguras = new Mogura[rows*cols];
		for (int i=0;i<rows*cols ;i++){
			moguras[i] = new Mogura();
		}

		status = StatusPrepare;
		outputLog(LogD,"status change:" + status);
	}

	/**
	 * 変数設定処理
	 */
	private void initGameParam(){
		//TODO initGameParamメソッド実装 画面サイズを取得して画像パラメータの最適化 など
		textSize  = 28;
		textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		textPaint.setStyle(Paint.Style.FILL);
		textPaint.setARGB(0xff,0xff,0xff,0xff);
		textPaint.setTextSize(textSize);

		bgPaint = new Paint();
		bgPaint.setStyle(Paint.Style.FILL);
		bgPaint.setARGB(0xff,0,0,0); //背景色

		//描画時の座標計算のための倍率、画面の横幅から決める
		if (getWidth() < 320) { // QVGA
			scale = 0.5F;
		} else if (getWidth() >= 320 && getWidth() < 480) { // HVGA
			scale = 0.67F;
		} else if (getWidth() >= 480 && getWidth() < 640) { // WVGA
			scale = 1.0F;
		} else if (getWidth() >= 640) {
			scale = (float)getWidth() / 480;
		}

		// X方向にセンタリングする為のオフセット
		xOff = ((getWidth() - imageBase.getWidth() * cols) / 2)
				- ((cols - 1) * sukima) ;

		// Y方向にセンタリングする為のオフセット
		yOff = ((getHeight() - imageBase.getHeight() * rows) / 2)
				- ((rows - 1) * sukima) ;

		pWidth  = imageBase.getWidth();
		pHeight = imageBase.getHeight();

	}

	//***** 終了時処理 ******************************************************************

	/**
	 * ゲーム終了処理
	 */
	private void finishGame(){
		//TODO finishGameメソッド実装
	}



	//***** 起動中・汎用処理 ******************************************************************

	/**
	 * 開始画面表示処理
	 */
	private void drowStartScreen(){
		//TODO drowStartScreenメソッド実装
		if (!surfaceCreated) {
			return;
		}

		Canvas canvas = null;
		try{
			synchronized (holder) {
				canvas = holder.lockCanvas();
				paintMessage(canvas,MSG_START_WAITING);
			}
		}catch(Exception e){
			outputLog(LogE, "message:", e);
		}finally{
			if (canvas != null) {
				holder.unlockCanvasAndPost(canvas);
			}
		}
		status = StatusReady;
		outputLog(LogD,"status change:" + status);
	}

	/**
	 * Clock開始処理（カウントダウンクロック・プレイクロックを連続して呼び出す）
	 */
	private void startClock(){
		//TODO startCountDownメソッド実装

		atPlayStart = 0;
		//TODO タイマー処理詳細化
		// 3秒カウントダウンする
		new GameTimer(3000,1000){
			//            TextView count_txt = (TextView)findViewById(R.id.textView1);
			// カウントダウン処理
			public void onTick(long millisUntilFinished){
				atPlayStart = (int)Math.round((double)millisUntilFinished/1000);
				repaint();
			}
			// カウントが0になった時の処理
			public void onFinish(){
				startPlayClock();
				status = StatusPlaying;
				outputLog(LogD,"status change:" + status);
			}
		}.start();

	}

	/**
	 * PlayClock開始処理
	 */
	private void startPlayClock(){
		//TODO startPlayClockメソッド実装
		Canvas canvas = null;
		try{
			synchronized (holder) {
				canvas = holder.lockCanvas();
				paintCells(canvas);
			}
		}catch(Exception e){
			outputLog(LogE, "message:", e);
		}finally{
			if (canvas != null) {
				holder.unlockCanvasAndPost(canvas);
			}
		}

		atPlayEnd = 0;
		//TODO タイマー処理詳細化
		new GameTimer(5000,1000){
			//            TextView count_txt = (TextView)findViewById(R.id.textView1);
			// カウントダウン処理
			public void onTick(long millisUntilFinished){
				atPlayEnd = (int)Math.round((double)millisUntilFinished/1000);
			}
			// カウントが0になった時の処理
			public void onFinish(){

				status = StatusTimeup;
				outputLog(LogD,"status change:" + status);
			}
		}.start();
	}

	/**
	 * PLAY終了処理
	 */
	private void exitPlaying(){
		//TODO exitPlayingメソッド実装
		//結果表示処理

		status = StatusDisplayResult;
		outputLog(LogD,"status change:" + status);
	}

	/**
	 * 結果・選択画面表示処理
	 */
	private void displayResult(){
		//TODO displayResultメソッド実装
		//結果表示処理

		//TODO テストコード
		Canvas canvas = null;
		try{
			synchronized (holder) {
				canvas = holder.lockCanvas();
				paintMessage(canvas,MSG_TIMEUP);
			}
		}catch(Exception e){
			outputLog(LogE, "message:", e);
		}finally{
			if (canvas != null) {
				holder.unlockCanvasAndPost(canvas);
			}
		}

		status = StatusSelecting;
		outputLog(LogD,"status change:" + status);
	}

	/**
	 * 画面再描画処理
	 */
	private void repaint(){
		//TODO repaintメソッド実装
		Canvas canvas = null;
		if(status == StatusStarting){
			if(atPlayStart == 0){
				return;
			}
			try{
				synchronized (holder) {
					canvas = holder.lockCanvas();
					paintCells(canvas);
					paintMessage(canvas, MSG_START_COUNTDOWN + atPlayStart);
				}
			}catch(Exception e){
				outputLog(LogE, "message:", e);
			}finally{
				if (canvas != null) {
					holder.unlockCanvasAndPost(canvas);
				}
			}
		}else if(status == StatusPlaying){
			if(atPlayEnd == 0){
				return;
			}
			try{
				synchronized (holder) {
					canvas = holder.lockCanvas();
					paintCells(canvas);
					paintMessage(canvas, MSG_PLAYING + atPlayEnd);
				}
			}catch(Exception e){
				outputLog(LogE, "message:", e);
			}finally{
				if (canvas != null) {
					holder.unlockCanvasAndPost(canvas);
				}
			}
		}
	}

	/**
	 * セルの表示
	 * @param canvas 描画時に使われるCanvas
	 * @param drawNumber 数字の駒も描く
	 */
	private void paintCells(Canvas canvas) {
		//TODO セルの表示※出来れば背景（一度だけ表示）にしたい
		//TODO あるいは、ここで初回以外はもぐら表示も行うか？
		int n;

		n  = 0;
		for (int y=0; y < rows; y++) {
			for (int x=0; x < cols; x++) {
				int   xx = x * pWidth + x * sukima + xOff;
				int   yy = y * pHeight + y * sukima + yOff;
				canvas.drawBitmap(imageBase,xx,yy,null);
				n++;
			}
		}
	}

	/**
	 * もぐらポップアップ処理
	 */
	private void popMogura(){
		//TODO popMoguraメソッド実装
	}

	/**
	 * モーダルメッセージ出力処理
	 */
	private void paintMessage(Canvas canvas ,String message) {
		int xx,yy;
		String str;

		str = message;

		Rect   bounds = new Rect();
		textPaint.getTextBounds(str,0,str.length(),bounds);
		xx = (getWidth() - bounds.width()) / 2;
		//		yy = (getHeight() - textSize) / 2;
		yy = textSize;
		canvas.drawRect(0, 0, getWidth(), textSize + 5, bgPaint);
		canvas.drawText(str,xx,yy,textPaint);
	}

	/**
	 * ログ出力処理
	 */
	private void outputLog(int level,String message){
		outputLog(level,message,null);
	}

	/**
	 * ログ出力処理
	 */
	private void outputLog(int level,String message,Throwable e){
		switch(level){
		case LogD:
			Log.d(APP_NAME,message);
			break;
		case LogI:
			Log.d(APP_NAME,message);
			break;
		case LogW:
			Log.d(APP_NAME,message);
			break;
		case LogE:
			Log.d(APP_NAME,message + "message:" + e.getMessage());
			break;
		}
	}

	//***** イベント処理 ******************************************************************

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		// TODO 自動生成されたメソッド・スタブ
		//GestureDetector実装？
		surfaceCreated = true;

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		// TODO 自動生成されたメソッド・スタブ
		surfaceCreated = false;
		//TODO スレッド解放処理はここでOK？
		thread = null;

	}

	//	@Override
	//	public boolean onKeyDown(int keyCode,KeyEvent event) {
	//		if(status == StatusReady){
	//			status = StatusStarting;
	//			outputLog(LogD,"status change:" + status);
	//			return super.onKeyDown(keyCode,event);
	//		}else if(status == StatusSelecting){
	//			status = StatusPrepare;
	//			outputLog(LogD,"status change:" + status);
	//			return super.onKeyDown(keyCode,event);
	//		}
	//		return true;
	//	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if(status == StatusReady){
			status = StatusStarting;
			outputLog(LogD,"status change:" + status);
			startClock();
			return super.onTouchEvent(event);
		}else if(status == StatusSelecting){
			status = StatusPrepare;
			outputLog(LogD,"status change:" + status);
			return super.onTouchEvent(event);
		}
		return true;
	}
}
