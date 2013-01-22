//済 文字表示・スリープを一時的に組み込み
//済 待ち処理・カウントダウン
//済 タイマー処理
//→タイマー処理がUIスレッドからしか実行できない？？？要確認
//済 背景画像表示
//済 メッセージ表示箇所の調整・クリア処理
//→対象を指定したCanvasのクリア方法を明らかに ダブルバッファリングが鍵
//済 画面調整・画面表示のブラッシュアップ
//→背景は常に表示・もぐらは毎回再描画 SurfaceViewの仕様上、毎回再描画が必要
//済 もぐら表示の実装　もぐら側に表示内容を返すメソッドを持たせる
//◆もぐらタッチの実装
//済 １）タッチイベントを拾って、得点を内部に保存
//済 ２）得点の表示・更新
//済 ３）叩かれたもぐらの色変更・消えるタイミング変更
//  結果画面表示 簡単でOK？
//済 連続プレイ時も問題がないか確認
//→preparePlayに各種リフレッシュ処理を追加
//TODO リソース開放
//TODO 中断・再開が可能なよう実装
//TODO 設定画面（難しくなければ やるとしても、ブランチを作る）

package jp.co.teng.android.moguratataki;

import jp.co.teng.android.moguratataki.DataLoader;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.util.Log;
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
	private final int StatusSetting       = 102; //設定の初期化中
	private final int StatusPrepare       = 103; //画面の初期化中
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
	private final Long mogLifespan = (long)1500;
	private final Long mogDeadspan = (long)300;

	private   Paint           textPaint;
	private   int             textSize;
	private   Paint           bgPaint;
	private   Paint           retryPaint;
	private   Paint           mogPaint;

	protected Bitmap imageBase;
	protected Bitmap imageMogura;
	protected Bitmap imageAtacked;
	private   DataLoader      loader;

	private   int             xOff;         // 駒表示時のXのオフセット
	private   int             yOff;         // 駒表示時のYのオフセット
	private   int             pWidth;       // 駒の幅
	private   int             pHeight;      // 駒の高さ
	private   float           scale;        // 座標計算用スケール

	//	private Mogura[] moguras;
	private MoguraGroup moguraGp;

	private final String APP_NAME         = "Moguratataki";

	private int atPlayStart;
	private int atPlayEnd;

	private int score;

	private SurfaceHolder   holder;
	private Thread thread;

	private final String MSG_RETRY = "ここを押してリトライ";
	private int retryY;
	private int retryX;
	private int retryRectWidth;
	private int retryRectHeight;



	public GameView(Activity activity) {
		super(activity);
		init();
	}

	/**
	 * インスタンス初期化メソッド、コンストラクタから一度だけ呼ばれる
	 */
	private void init() {

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
		while(thread != null){

			switch(status){
			case StatusInit:
				//ゲーム開始準備（初回のみ）ブロック
				initBase();
				break;
			case StatusSetting:
				//設定初期化ブロック
				initPlaySetting();
				break;
			case StatusPrepare:
				preparePlay();
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
				//もぐらポップアップ処理
				moguraGp.popMoguraAtRandom();
				//再描画処理
				repaint();
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
	 * 初期化処理２（
	 */
	private void initBase(){
		if(!surfaceCreated){
			return;
		}

		loader.loadImages(getContext());
		initBasicParam();

		status = StatusSetting;
		outputLog(LogD,"status change:" + status);
	}

	/**
	 * 基本設定 変数初期化処理
	 */
	private void initBasicParam(){
		textSize  = 28;
		textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
		textPaint.setStyle(Paint.Style.FILL);
		textPaint.setARGB(0xff,0xff,0xff,0xff);
		textPaint.setTextSize(textSize);

		bgPaint = new Paint();
		bgPaint.setStyle(Paint.Style.FILL);
		bgPaint.setARGB(0xff,0,0,0); //背景色

		retryPaint = new Paint();
		retryPaint.setStyle(Paint.Style.FILL);
		retryPaint.setARGB(0xff,50,50,50); //背景色

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
	 * プレイ設定初期化処理
	 */
	private void initPlaySetting(){
		mogPaint = new Paint();
		mogPaint.setARGB(0xff,0,0,0);

		moguraGp = new MoguraGroup(rows,cols);
		moguraGp.setMogLifespan(mogLifespan);
		moguraGp.setMogDeadspan(mogDeadspan);

		//TODO 各種パラメータは現状ハードコーディング

		Rect   bounds = new Rect();
		textPaint.getTextBounds(MSG_RETRY,0,MSG_RETRY.length(),bounds);
		retryX = (getWidth() - bounds.width()) / 2;
		retryY = getHeight() - (textSize + 10);
		retryRectWidth = retryX + bounds.width();
		retryRectHeight = retryY + textSize + 10;

		status = StatusPrepare;
		outputLog(LogD,"status change:" + status);
	}

	/**
	 * プレイ準備
	 */
	private void preparePlay(){
		if (!surfaceCreated) {
			return;
		}

		score = 0;
		moguraGp.reflesh();

		//開始メッセージ表示
		Canvas canvas = null;
		try{
			synchronized (holder) {
				canvas = holder.lockCanvas();
				canvas.drawColor(0, Mode.CLEAR);
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

		atPlayStart = 0;
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
		new GameTimer(10000,1000){
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
		//結果表示処理

		status = StatusDisplayResult;
		outputLog(LogD,"status change:" + status);
	}

	/**
	 * 結果・選択画面表示処理
	 */
	private void displayResult(){
		//結果表示処理

		//TODO repaintにまとめるか
		Canvas canvas = null;
		try{
			synchronized (holder) {
				canvas = holder.lockCanvas();
				canvas.drawColor(0, Mode.CLEAR);
				paintCells(canvas);
				paintMoguras(canvas);
				paintMessage(canvas,MSG_TIMEUP);
				paintScore(canvas, score);
				paintRetryMsg(canvas, MSG_RETRY);
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
		Canvas canvas = null;
		if(status == StatusStarting){
			if(atPlayStart == 0){
				return;
			}
			try{
				synchronized (holder) {
					canvas = holder.lockCanvas();
					canvas.drawColor(0, Mode.CLEAR);
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
					canvas.drawColor(0, Mode.CLEAR);
					paintCells(canvas);
					paintMoguras(canvas);
					paintMessage(canvas, MSG_PLAYING + atPlayEnd);
					paintScore(canvas, score);
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
		for (int y=0; y < rows; y++) {
			for (int x=0; x < cols; x++) {
				int   xx = x * pWidth + x * sukima + xOff;
				int   yy = y * pHeight + y * sukima + yOff;
				canvas.drawBitmap(imageBase,xx,yy,null);
			}
		}
	}

	/**
	 * モグの表示
	 * @param canvas 描画時に使われるCanvas
	 */
	private void paintMoguras(Canvas canvas) {
		int x;
		int y;
		int xx;
		int yy;
		int alpha;
		long curmills = System.currentTimeMillis();
		Mogura mog;
		while((mog = moguraGp.getLiveMogura()) != null){
			switch(mog.getStatus()){
			case Mogura.MOG_ALLIVE:
				x = mog.getCol();
				y = mog.getRow();
				xx = x * pWidth + x * sukima + xOff;
				yy = y * pHeight + y * sukima + yOff;
				alpha = (int)(((double)(mog.getRemoveTime() - curmills) / (double)mogLifespan) * 255);
				mogPaint.setAlpha(alpha);
				canvas.drawBitmap(imageMogura,xx,yy,mogPaint);
				break;
			case Mogura.MOG_ATACKED:
				x = mog.getCol();
				y = mog.getRow();
				xx = x * pWidth + x * sukima + xOff;
				yy = y * pHeight + y * sukima + yOff;
				alpha = (int)(((double)(mog.getRemoveTime() - curmills) / (double)mogDeadspan) * 255);
				mogPaint.setAlpha(alpha);
				canvas.drawBitmap(imageAtacked,xx,yy,mogPaint);
				break;
			default:
				break;
			}
		}
		moguraGp.refleshIndex();
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
		canvas.drawRect(0, 0, getWidth(), yy + 5, bgPaint);
		canvas.drawText(str,xx,yy,textPaint);
	}

	/**
	 * スコア表示処理
	 */
	private void paintScore(Canvas canvas ,int score) {
		int xx,yy;

		String str = "スコア:" + score;

		Rect   bounds = new Rect();
		textPaint.getTextBounds(str,0,str.length(),bounds);
		xx = (getWidth() - bounds.width()) / 2;
		//		yy = (getHeight() - textSize) / 2;
		yy = textSize * 2 + 10;
		canvas.drawRect(0, textSize + 5, getWidth(), yy + 5, bgPaint);
		canvas.drawText(str,xx,yy,textPaint);
	}

	/**
	 * リトライメッセージ表示
	 */
	private void paintRetryMsg(Canvas canvas ,String message) {
		String str;

		str = message;

//		retryX = (getWidth() - bounds.width()) / 2;
//		retryY = getHeight() - (textSize + 10);
		canvas.drawRect(retryX, retryY, retryRectWidth, retryRectHeight, retryPaint);
		canvas.drawText(str,retryX,retryY + (textSize + 3),textPaint);
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
			Log.e(APP_NAME,message + "message:" + e.getMessage());
			e.printStackTrace();
			break;
		}
	}

	//***** イベント処理 ******************************************************************

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		// TODO 自動生成されたメソッド・スタブ
		//GestureDetector実装？
		surfaceCreated = true;

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
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
			//TODO 指定の領域をタッチしたかどうかの判断 もっとスマートなやり方があるはず
			if(retryX <= event.getX() && event.getX() <= retryRectWidth &&
					retryY <= event.getY() && event.getY() <= retryRectHeight){
				status = StatusPrepare;
				outputLog(LogD,"status change:" + status);
				return super.onTouchEvent(event);
			}
			return super.onTouchEvent(event);
		}else if(status == StatusPlaying){
			//			xx = x * pWidth + x * sukima + xOff;
			//			yy = y * pHeight + y * sukima + yOff;
			int col = (int)((event.getX() - xOff) / (pWidth + sukima));
			int row = (int)((event.getY() - yOff) / (pHeight + sukima));
			outputLog(LogD, "[touched]" + " row:" + row + " col:" + col);
			if(row < rows && col < cols && moguraGp.setMogAtacked(row, col)){
				//Atackedに変更された場合
				outputLog(LogD, "[touched][Atacked]" + " row:" + row + " col:" + col);
				score++;
				return super.onTouchEvent(event);
			}
			//Atackedに変更されなかった（ステータスがAliveじゃない）場合
			outputLog(LogD, "[touched][Unatacked]" + " row:" + row + " col:" + col);
			return super.onTouchEvent(event);
		}
		return true;
	}
}
