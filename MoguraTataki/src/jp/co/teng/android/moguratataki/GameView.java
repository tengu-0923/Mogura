//TODO いったん、中断・再開のことは考えずにやる。動くようになってから考える

//済 文字表示・スリープを一時的に組み込み
//済 待ち処理・カウントダウン
//TODO タイマー処理
 //→タイマー処理がUIスレッドからしか実行できない？？？要確認
//TODO 画面表示・リソース開放
//TODO 画面調整・画面表示のブラッシュアップ


package jp.co.teng.android.moguratataki;


import android.app.Activity;
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
	private final String MSG_START_COUNTDOWN    = "開始します";
	private final String MSG_PLAYING            = "起動中";
	private final String MSG_TIMEUP            = "終了です";

	private final int LogD         = 1;
	private final int LogI         = 2;
	private final int LogW         = 3;
	private final int LogE         = 4;

	private   Paint           textPaint;
	private   int             textSize;

	private final String APP_NAME         = "Moguratataki";

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
				//TODO ここでカウントダウン処理のトリガを引く。UIスレッドからじゃないとうまくいかない。。
//				startCountDown();
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
		//TODO prepareGameメソッド実装
		initGameParam();
		loadImages();

		status = StatusPrepare;
		outputLog(LogD,"status change:" + status);
	}

	/**
	 * 変数設定処理
	 */
	private void initGameParam(){
		//TODO initGameParamメソッド実装 画面サイズを取得して画像パラメータの最適化 など
		textSize  = 32;
		textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		textPaint.setStyle(Paint.Style.FILL);
		textPaint.setARGB(0xff,0xff,0xff,0xff);
		textPaint.setTextSize(textSize);


	}

	/**
	 * 画像初期読み込み処理
	 */
	private void loadImages(){
		//TODO loadImagesメソッド実装

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

		//TODO タイマー処理詳細化
		// 3秒カウントダウンする
		new GameTimer(3000,1000){
			//            TextView count_txt = (TextView)findViewById(R.id.textView1);
			// カウントダウン処理
			public void onTick(long millisUntilFinished){
				Canvas canvas = null;
				try{
					synchronized (holder) {
						canvas = holder.lockCanvas();
						paintMessage(canvas, Long.toString(Math.round((double)millisUntilFinished/1000)));
//						paintMessage(canvas, "ZZZZZZZZZZZZZZZZZZZ");
					}
				}catch(Exception e){
					outputLog(LogE, "message:", e);
				}finally{
					if (canvas != null) {
						holder.unlockCanvasAndPost(canvas);
					}
				}
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
		//TODO startCountDownメソッド実装

		//TODO タイマー処理詳細化
		// 3秒カウントダウンする
		new GameTimer(5000,1000){
			//            TextView count_txt = (TextView)findViewById(R.id.textView1);
			// カウントダウン処理
			public void onTick(long millisUntilFinished){
				Canvas canvas = null;
				try{
					synchronized (holder) {
						canvas = holder.lockCanvas();
						paintMessage(canvas, Long.toString(Math.round((double)millisUntilFinished/1000)));
					}
				}catch(Exception e){
					outputLog(LogE, "message:", e);
				}finally{
					if (canvas != null) {
						holder.unlockCanvasAndPost(canvas);
					}
				}
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
		yy = (getHeight() - textSize) / 2;
		canvas.drawColor(0,Mode.CLEAR);
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

	@Override
	public boolean onKeyDown(int keyCode,KeyEvent event) {
		//TODO 開始待ちの時の入力制御処理
		if(status == StatusReady){
			status = StatusStarting;
			outputLog(LogD,"status change:" + status);
		}else if(status == StatusSelecting){
			//TODO 終了後の時の入力制御処理
			status = StatusPrepare;
			outputLog(LogD,"status change:" + status);
		}
		return super.onKeyDown(keyCode,event);
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		//TODO 開始待ちの時の入力制御処理
		if(status == StatusReady){
			status = StatusStarting;
			outputLog(LogD,"status change:" + status);
			startClock();
		}else if(status == StatusSelecting){
			//TODO 終了後の時の入力制御処理
			status = StatusPrepare;
			outputLog(LogD,"status change:" + status);
		}
		return true;

	}
}
