import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.Calendar;
import java.util.Date;
import java.util.Random;

import javax.microedition.io.Connector;

import com.nttdocomo.io.HttpConnection;
import com.nttdocomo.ui.Canvas;
import com.nttdocomo.ui.Display;
import com.nttdocomo.ui.Font;
import com.nttdocomo.ui.Graphics;
import com.nttdocomo.ui.IApplication;
import com.nttdocomo.ui.Image;
import com.nttdocomo.ui.MediaImage;
import com.nttdocomo.ui.MediaManager;
import com.nttdocomo.ui.ShortTimer;
import com.nttdocomo.util.JarInflater;

public class FI extends IApplication {
	ShortTimer timer;

	public void start() {
		FICanvas c = new FICanvas();
		Display.setCurrent(c);

		// タイマ設定
		timer = ShortTimer.getShortTimer(c, 0, 200, true);
		timer.start();
	}

	public void resume() {
		// 再開時、タイマ起動
		timer.start();
	}

}

class FICanvas extends Canvas {
	/*
	 * ======== マルチプラットフォーム用変数 =====
	 */

	public int fontH = 0; // フォントの高さ
	// public int RES_SIZE = 20; // リソースサイズ
	public static final int RES_SIZE = 120000; // リソースサイズ
	// チップ画像
	private Image chips;
	private Image bg;
	private MediaImage media;
	private Image[] mapChips; // チップイメージ
	private JarInflater jar;

	/*
	 * ======== 定数 ========
	 */
	public static final int VER = 4;
	public static final int CHIP = 32;
	public static final int MAP = 32;
	public static final int W_MARGIN = 6;
	public static final int W_ROW = 8;
	public static final int W_SUB_ROW = 6;
	public static final int ITEM_LEN = 64;
	public static final int SAVE_SIZE = 548;
	public static final int MEVENTS_SIZE = 40;
	public static final int F_MAGIC = 240;
	public static final int F_MONSTER = 170;
	public static final int F_MAP = 256;
	public static final int M_ISL = 0;
	public static final int M_RGN = 1;
	public static final int M_X = 2;
	public static final int M_Y = 3;
	public static final int M_QUOTA = 4;
	public static final int M_QUOTA2 = 5;
	public static final int M_OPT = 6;
	public static final int M_QUOTA_FORCE = 11;
	public static final int M_RAINBOW = 12;
	public static final int M_FINDEX = 13;
	public static final int M_TELEPORT = 15;
	public static final int FF_STABLE = 54;
	public static final int FF_SUBMAP = 70;
	public static final int G_DRAW_STRING = 0;
	public static final int G_DRAW_RECT = 1;
	public static final int G_FILL_RECT = 2;
	public static final int G_TRANSLATE = 3;
	public static final int G_SET_COLOR = 4;
	public static final int G_DRAW_CHIP = 5;
	public static final int G_FLIP_BUFFER = 6;
	public static final int G_SHIFT_BUFFER = 7;
	public static final int G_COPY_AREA = 8;
	public static final int G_FILL_POLIGON = 9;
	public static final int G_REPAINT = 10;
	public static final int G_FREE_IMAGE = 11;

	/*
	 * ======== システム変数 =====
	 */
	private Random random;

	protected int imageFilter; // チップ画像を編集する番号
	// (デバッグ用) トークン表示フラグ
	private boolean showToken;
	private int resSize; // リソースサイズ

	/*
	 * ======== 端末依存変数 起動中変化しない =====
	 */

	// チップの中央位置
	private int cntX;
	private int cntY;

	private int chipsWidth; // チップの描画幅
	private int chipsHeight; // チップの描画高さ
	private int wWidth; // ウィンドウの幅
	private boolean _load; // マスタデータがダウンロード済みか

	/*
	 * ======== マスタ情報1 起動中変化しない ======
	 */

	private String[] mgcs = new String[16]; // 魔法名
	private String[] mgcEvents = new String[64]; // 魔法スクリプト
	private String[] menuEvents = new String[16]; // メニュースクリプト
	private String[] mapTexts = new String[20]; // 宝の地図の内容
	private String[] iName = new String[ITEM_LEN]; // アイテム名
	private int[] iType = new int[ITEM_LEN]; // アイテム種別
	private int[] iValue = new int[ITEM_LEN]; // アイテム効果値
	private int[] iCost = new int[ITEM_LEN]; // アイテム価格
	private int[] iAlign = new int[ITEM_LEN]; // アイテム属性
	private byte[] iEvents = new byte[ITEM_LEN]; // アイテム用イベントリファレンス
	private int[] fireTable = { 2, 1, 0, -2, 4 }; // 炎のダメージリスト
	private byte[][] raftTable; // 筏で移動可能な座標一覧

	/*
	 * ======== マスタ情報2 地図移動で変化する ======
	 */

	private byte[][] xyMap = new byte[MAP][MAP]; // 地図情報
	private byte[] upChip; // 主人公の上に重なるチップ
	private boolean[] walkIn; // 移動可・不可の判定
	private byte[] eventNo; // チップ依存イベント番号
	private byte[][] spotNo; // スポットイベント番号
	private String[] mapEvent; // 地図依存イベントスクリプト
	private byte[][] enemyPattern = new byte[7][16]; // 出現パターン(ボスチップ描画にも代用)
	private byte[][] enemyAlgo = new byte[10][4]; // 敵の戦略アルゴリズム
	private byte[] enemyDrop = new byte[10]; // 敵が落とすアイテム
	private byte[] enemyID = new byte[10]; // 敵ID(モンスター図鑑用)
	private byte[] submaplens; // 自動生成マップ開放値一覧
	private byte[][] submaps; // 自動生成マップ一覧

	/*
	 * ======== キャラマスタ情報 敵の情報のみ変化する =======
	 */

	private String[] names = new String[16]; // 名前
	private int[] hpis = new int[16]; // MAXHP
	private int[] hpds = new int[6]; // HP上昇率
	private int[] mpis = new int[16]; // MAXMP
	private int[] mpds = new int[6]; // MP上昇率
	private int[] atis = new int[16]; // 攻撃力
	private int[] atds = new int[6]; // 攻撃力上昇率
	private int[] dexs = new int[16]; // 素早さ
	private int[] xps = new int[16]; // 必要経験点(賃金)
	private int[] icns = new int[16]; // アイコン
	private int[] dfes = new int[10]; // 敵防御点

	/*
	 * ======== キャラ変数 ゲーム中常に変化する ======
	 */

	private int[] hps = new int[16]; // 現在HP
	private int[] mps = new int[16]; // 現在MP
	private int[] lvs = { 1, 1, 1, 1, 1, 1 }; // レベル
	private int[][] eqs = new int[6][4]; // 装備一式
	private int exp; // モサメデスの現在経験点
	private int gem; // 現在所持金
	private int[] members = new int[16]; // 隊列順番
	private byte[] raftHist = { -1, -1, -1, -1 }; // 筏の移動履歴

	/*
	 * ======== フィールド変数 ゲーム中常に変化する ======
	 */

	// 汎用intフィールド
	private int[] mem = new int[16];

	// 描画位置シフト
	private int sftX;
	private int sftY;

	// フィールド用
	private boolean moving; // 現在アニメーション動作中か

	// シーン管理用
	private int _scene; // シーン変数
	private String[] lines = new String[W_ROW]; // 表示行
	private int[] selLines = new int[W_ROW]; // 選択行戻り値
	private int lindex; // 現在描画行
	private int _sel; // 現在選択行
	private int _page; // 現在選択ページ
	private int subWSts; // サブウィンドウ描画内容
	private int[] _args = new int[10]; // 現在選択物一覧
	private int selectEnemy; // 敵選択中かどうか -1, 0: No 1: Yes
	private boolean showWindow; // ウィンドウが描画されているかどうか
	public boolean useCache; // フィールド描画にキャッシュを使用してよいか
	private String[] parsedEvent; // パース対象イベント配列
	private String[] saveList; // セーブスロット一覧保持
	private int spotPos; // スポットイベントの発生場所を保持

	// 戦闘用
	private int[] initiatives = new int[16]; // 各キャライニシアティブ
	private int[] optAt = new int[16]; // 攻撃力上昇
	private int[] optDf = new int[16]; // 防御力上昇
	private int winXp; // 獲得予定経験点
	private int winGem; // 獲得予定金額
	private int winItem; // 獲得予定アイテム
	private int winScript = -1; // 勝利後実行するスクリプト
	private int nextAttt = -1; // 次ターンアタタタッの対象になるキャラクター
	/*
	 * 戦闘フラグ ... 防御 1, 眠っている 2
	 */
	private int[] optFlg = new int[16];

	/*
	 * 戦闘属性 ... 炎は普通 0, 炎に強い 1, 炎は無効 2 炎で回復 3 炎に弱い 4, 状態変化は普通 0, 状態変化に強い 8,
	 * 状態変化に弱い 16, 回復魔法に弱い 32, 時々攻撃を回避 64, 呪文無効化 128, 戦闘評価を行う 256
	 */
	private int[] align = new int[10]; // 戦闘属性

	// 各種フラグ
	private boolean[] eventFlg = new boolean[256 + MAP * MAP]; // ゲーム内イベント用
	private int[] iAmount = new int[ITEM_LEN]; // アイテム所持数
	public char resVersion = 0; // リソース読み込み用マジックキャラクタ
	private long startTime; // ゲーム開始時刻
	private long gameTime; // ゲームプレイ時間
	public int[] freeImage = new int[4]; // 自由描画領域
	private long completeItem; // アイテムコンプリートフラグ
	private long completeMonster; // 敵コンプリートフラグ
	private int slotIndex; // 前回セーブしたセーブスロット

	public FICanvas() {

		// 乱数生成機の作成
		random = new Random();

		// チップの中央位置計算
		cntX = (getWidth() - CHIP) / 2;
		cntY = (getHeight() - CHIP) / 2;

		// チップの描画幅・高さ計算
		chipsWidth = cntX / CHIP + 1;
		chipsHeight = cntY / CHIP + 1;

		// ウィンドウサイズの計算
		wWidth = getWidth() - W_MARGIN * 2;

		// フォントの高さ計算
		fontH = g$fontHeight();

		// キャッシュ用イメージ作成
		try{
			g$createImage(null);
		}catch(Exception e){}

		// 表示行をクリア
		clearLines();

		// 初期データ読み込み
		try {
			loadMaster();
		} catch (Exception e) {
		}

		// 各種メモリパターン設定
		for (int i = 0; i < members.length; i++) {
			members[i] = -1;
		}
		for (int i = 0; i < _args.length; i++) {
			_args[i] = -1;
		}

		// オープニングシーンに
		setScene(0);

	}

	// デバッグ用メソッド
	void debug() {
		// デバッグ用初期設定

	}

	public void processEvent(int type, int param) {
		// デバッグ用にトークンを表示する?
		if ((type == Display.KEY_PRESSED_EVENT)
				&& (param == Display.KEY_ASTERISK)) {
			showToken = !showToken;
			return;
		}
		if ((type == Display.KEY_PRESSED_EVENT) && (param == Display.KEY_POUND)) {
			System.out.print("[");
			for (int i = 0; i < _args.length; i++) {
				System.out.print("," + _args[i]);
			}
			System.out.println("]");
			return;
		}
		if ((type == Display.KEY_PRESSED_EVENT)
				&& (param == Display.KEY_CAMERA)) {
			for (int i = 0; i < 256; i++) {
				System.out.println(eventFlg[i] + "\t");
			}
			return;
		}
		// よく通過するロジックなので、高速化のためswitch文で分岐
		switch (_scene) {
		case 0:
			preludeEvent(type, param);
			break;
		case 1:
			fieldEvent(type, param);
			break;
		case 2:
			menuEvent(type, param);
			break;
		case 3:
			buttleEvent(type, param);
			break;
		case 4:
			talkEvent(type, param);
			break;
		case 5:
			raftEvent(type, param);
			break;
		case 6:
			demoEvent(type, param);
			break;
		case 7:
			libEvent(type, param);
		}
	}

	/* ========================= */
	// シーン0 オープニング
	/* ========================= */

	private void preludeEvent(int type, int param) {
		if (type == Display.TIMER_EXPIRED_EVENT) {
			return;
		}

		if (msgEvent(type, param)) {
			if (!_load) {
				// 初期ダウンロードができてない場合
			} else if (mem[M_ISL] != 0) {
				// マップ読み込み開始
				setScene(1);
			} else if (parsedEvent == mapEvent) {
				// エンディングシーン
				_args[0]++;
				reparse();
			} else {
				int command = selLines[_sel];
				_page = 0;
				int index = 0;

				for (; index < _args.length; index++) {
					if (_args[index] < 0) {
						if (command < 0) {
							index--;
							// オープニングシーン中は、行動者は不変
							if (index == 1) {
								index = 0;
							}
						}
						break;
					}
				}

				if (0 <= index && index < _args.length) {
					_args[index] = command;
				}

				reparse();
			}
		}

		// リソースダウンロード開始
		if ((type == Display.KEY_PRESSED_EVENT) && (param == Display.KEY_SOFT2)) {
			resSize = 0;
			_load = false;
			download();
			if (!_load)
				return;
			loadMaster();
			setScene(0);
		}
		g$draw(null, G_REPAINT, 0, 0, 0, null);
	}

	/* ========================= */
	// シーン1 フィールドシーン
	/* ========================= */

	private void fieldEvent(int type, int param) {
		// フィールドシーンのイベントモード
		if (type == Display.KEY_PRESSED_EVENT) {
			if ((param == Display.KEY_SOFT1) || (param == Display.KEY_IAPP)) {
				setScene(2);
				g$draw(null, G_REPAINT, 0, 0, 0, null);
				return;
			}

			if (param == Display.KEY_SELECT) {
				// スポットイベントの処理
				if ((_args[0] = checkSpot()) >= 0) {
					reparse();
					if (!useCache) {
						g$draw(null, G_REPAINT, 0, 0, 0, null);
					}
				}
			}

		}

		if (type == Display.TIMER_EXPIRED_EVENT) {
			if (!moving) {
				// 振り返ると危険イベント
				// チップ依存イベントが負の数の場合、前の方向の記憶を開始
				if (eventNo[getXYMap(mem[M_X], mem[M_Y])] < 0) {
					mem[M_QUOTA2] = mem[M_QUOTA];
				} else {
					mem[M_QUOTA2] = -1;
				}

				int keyPadState = getKeypadState();

				if ((keyPadState & (1 << Display.KEY_LEFT)) != 0) {
					mem[M_QUOTA] = 0;
					moving = true;
				}
				if ((keyPadState & (1 << Display.KEY_RIGHT)) != 0) {
					mem[M_QUOTA] = 2;
					moving = true;
				}
				if ((keyPadState & (1 << Display.KEY_UP)) != 0) {
					mem[M_QUOTA] = 1;
					moving = true;
				}
				if ((keyPadState & (1 << Display.KEY_DOWN)) != 0) {
					mem[M_QUOTA] = 3;
					moving = true;
				}
				if (moving) {
					// 上下左右を押していた場合
					moveAnime1();
				}

			} else {
				// ダンジョン自動生成イベント用(1)
				// 現在の位置情報(16分割)が先ほどまでの情報と違ったら処理開始
				int oldPos = (mem[M_Y] / 8) * 4 + (mem[M_X] / 8);

				// 主人公が移動中アニメーションである場合
				boolean nomoved = moveAnime2();
				if (nomoved) {
					// 移動していなければ、チップ依存イベントは行われない
					return;
				}

				int event = 0;
				// ダンジョン自動生成イベント用(2)
				// 現在の位置情報(16分割)が先ほどまでの情報と違ったら処理開始
				// ただし5-2しか処理しない
				int newPos = (mem[M_Y] / 8) * 4 + (mem[M_X] / 8);
				if (mem[M_ISL] == 5 && mem[M_RGN] == 2 && newPos != 0
						&& !eventFlg[FF_STABLE + newPos] && newPos != oldPos) {
					goFloor(newPos);
					event = 9;
				}

				// チップ依存イベントのうち、1..7は
				// 敵の出現イベント
				if (event == 0) {
					event = eventNo[getXYMap(mem[M_X], mem[M_Y])];
				}
				// 敵出現確認
				// 振り返ると危険イベントの処理
				if (mem[M_QUOTA2] >= 0) {
					if (mem[M_QUOTA2] != mem[M_QUOTA]) {
						event = 8;
					} else {
						return;
					}
				}

				if (event <= 0) {
					return;
				}

				// チップ依存イベントが1..7番であれば、
				// 敵出現パターンに応じて地図イベントを処理
				if (event < 8) {
					event = enemyPattern[event - 1][randi(16)];
				}

				if (event > 0) {
					_args[0] = event;
					reparse();
					_args[0] = -1;
				}

				if (!useCache || (_scene != 1)) {
					g$draw(null, G_REPAINT, 0, 0, 0, null);
				}
			}
		}
	}

	private byte checkSpot() {
		int xx = mem[M_X] + calcXY(mem[M_QUOTA], 0);
		int yy = mem[M_Y] + calcXY(mem[M_QUOTA], -1);
		if (isMap(xx, yy) && (_scene != 3) && (_args[3] < 0)) {
			// 戦闘中は動作しない
			// 対象が決まっている時も動作しない

			byte spot = -1;
			for (int i = 0; i < spotNo.length; i++) {
				if ((spotNo[i][0] == xx) && (spotNo[i][1] == yy)) {
					spot = spotNo[i][2];
					break;
				} else if ((spotNo[i][0] < 0)
						&& (spotNo[i][2] == eventNo[getXYMap(xx, yy)])) {
					spot = spotNo[i][2];
					break;
				}
			}

			if (spot >= 0) {
				// メニューからの起動の場合は、
				// 条件を満たさない場合は動作しない
				if (_scene == 2) {
					if (mapEvent[spot].charAt(0) != '@') {
						return -1;
					}
				}
				spotPos = yy * MAP + xx;
				return spot;
			}
		}
		return -1;
	}

	/* ========================= */
	// シーン2 メニューシーン
	/* ========================= */

	private void menuEvent(int type, int param) {
		if (type == Display.TIMER_EXPIRED_EVENT) {
			return;
		}

		boolean transed = msgEvent(type, param);
		if (transed) {
			int command = selLines[_sel];
			_page = 0;
			int index = 0;

			for (; index < _args.length; index++) {
				if (_args[index] < 0) {
					if (command < 0) {
						index--;
					}
					break;
				}
			}
			if (index < 0) {
				setScene(1);
				g$draw(null, G_REPAINT, 0, 0, 0, null);
				return;
			}

			_args[index] = command;

			// 上限越えを防ぐ
			if (index >= _args.length - 1) {
				_args[_args.length - 1] = -1;
			}
			reparse();
		}

		g$draw(null, G_REPAINT, 0, 0, 0, null);
	}

	/* ========================= */
	// シーン3 戦闘シーン
	/* ========================= */

	private void buttleEvent(int type, int param) {
		if (type == Display.TIMER_EXPIRED_EVENT) {
			return;
		}

		if ((type == Display.KEY_RELEASED_EVENT) && (_sel == 0)) {
			return;
		}

		boolean transed = false;
		boolean parsing = false;
		int command = 0;
		if ((selectEnemy > 0) && (type == Display.KEY_PRESSED_EVENT)) {
			// 敵を選択している場合、カーソルキーの動きが変わる
			if (members[_sel + 6] < 0) {
				enemySelCursor(1);
			}

			if (type == Display.KEY_PRESSED_EVENT) {
				if (param == Display.KEY_LEFT) {
					enemySelCursor(-1);
				}

				if (param == Display.KEY_RIGHT) {
					enemySelCursor(1);
				}

				if (param == Display.KEY_UP) {
					if (_sel >= 5) {
						enemySelCursor(-5);
					} else {
						_sel = 1;
						selectEnemy = -1;
						reparse();
					}
				}

				if (param == Display.KEY_DOWN) {
					if (_sel < 5) {
						enemySelCursor(5);
					}
				}

				if (param == Display.KEY_SELECT) {
					command = _sel + 6;
					transed = true;
				}

				if (param == Display.KEY_SOFT1) {
					command = -1;
					transed = true;
				}

				if (param == Display.KEY_IAPP) {
					command = -1;
					transed = true;
				}

			}
		} else {
			transed = msgEvent(type, param);
		}

		// 戦闘用の画面遷移
		if (transed) {
			if (selectEnemy <= 0) {
				// 敵選択場面でないときは、標準選択機能を使用
				command = selLines[_sel];
			}
			if (command == 99) {
				// 「99」が帰ってきたときは、画面遷移を行わずに
				// 敵選択モードに移行
				selectEnemy = 1;
				reparse();
				transed = false;
			}
		}

		// 敵の行動順番の時には状態遷移を行なわない
		if (_args[1] >= 6) {
			parsing = true;
			transed = false;
			if (endButtle() < 0) {
				_args[0] = 10;
			}
		}

		if (transed) {
			_page = 0;
			// 敵選択モードを中立に
			selectEnemy = 0;
			int index;
			for (index = 0; index < _args.length; index++) {
				if (_args[index] < 0) {
					if (command < 0) {
						index--;
						// 戦闘中は、行動者は不変
						if (index == 1) {
							index = 0;
						}
					}
					break;
				}
			}

			if (index < 0) {
				index = 0;
			}

			_args[index] = command;

			if (index >= _args.length - 1) {
				_args[_args.length - 1] = -1;
			}

			// 行動者がいない(-1)なら、改めて行動者を選択
			// ただし、二重表示を避けるため、キー押下イベント時のみ
			if ((_args[1] < 0 || initiatives[id2mem(_args[1])] >= 99)
					&& type == Display.KEY_PRESSED_EVENT) {
				int endedButtle = endButtle();
				if (endedButtle == 0) {
					initButtle();
				} else {
					if (winXp + winItem + winGem == 0 && endedButtle >= 0) {
						//
						if (winScript >= 0) {
							// 勝利後スクリプトが設定されていれば
							// 勝利後スクリプトパースのために
							// メッセージシーンへ移動
							setScene(4);
							_args[0] = winScript;
						} else {
							// 戦闘中、敵がいなくなっていればフィールドに戻る
							setScene(1);
						}
					} else if (_args[0] != 11) {
						// 勝利または敗北の場合
						_args[0] = 10;
					}
				}
			}
			if (_scene != 1) {
				parsing = true;
			}
		}

		if (parsing) {
			reparse();
		}

		g$draw(null, G_REPAINT, 0, 0, 0, null);
	}

	/* ========================= */
	// シーン4 会話シーン
	/* ========================= */

	private void talkEvent(int type, int param) {
		if (type == Display.TIMER_EXPIRED_EVENT) {
			return;
		}

		boolean transed = msgEvent(type, param);
		if (transed) {
			if ((selLines[0] >= 0) && (_sel > 0)) {
				// 引数つきモード
				_args[0] = selLines[0];
				_args[1] = selLines[_sel];
			} else {
				// 通常モード
				_args[0] = selLines[_sel];
			}
			if (_args[0] < 0) {
				setScene(1);
			} else {
				parse(mapEvent[_args[0]]);
			}
		}

		if (_scene != 6 && lines[0].equals("")) {
			// デモシーンでなく、メッセージが何も存在しないなら、フィールドシーンへ
			setScene(1);
		}

		g$draw(null, G_REPAINT, 0, 0, 0, null);
	}

	/* ========================= */
	// シーン5 筏シーン
	/* ========================= */

	private void raftEvent(int type, int param) {
		if (type == Display.TIMER_EXPIRED_EVENT) {
			return;
		}

		if (_args[0] <= 0) {
			if (msgEvent(type, param)) {
				int sel = selLines[_sel];
				if (sel == 98) {
					// 移動の場合

					boolean voyaged = false;
					// 座標判定
					for (byte i = 0; i < raftTable.length; i++) {
						for (int j = 0; j < 4; j++) {
							if (raftTable[i][j] != _args[j + 1]) {
								break;
							}
							if (j == 3) {
								voyaged = true;
								updateRaftHList(i);
							}
						}
					}

					// 移動成功
					if (voyaged) {
						for (int i = 0; i < 4; i++) {
							mem[i] = _args[i + 1];
						}
						loadMap(mem[M_ISL], mem[M_RGN]);
						setScene(1);
						g$draw(null, G_REPAINT, 0, 0, 0, null);
						return;
					}

					// 移動失敗
					_args[0] = -2;
				} else if (sel == 99) {
					// 座標入力の場合
					_args[0] = 1;
				} else if (sel == 97) {
					// 戻るボタンの場合
					setScene(1);
					g$draw(null, G_REPAINT, 0, 0, 0, null);
					return;
				} else if ((0 <= sel) && (sel <= raftTable.length)) {
					// 履歴入力の場合
					for (int i = 0; i < 4; i++) {
						_args[i + 1] = raftTable[sel][i];
					}
				}

				parse(mgcEvents[22]);
			}

		} else {
			if (type == Display.KEY_RELEASED_EVENT) {
				return;
			}

			// 座標入力シーン
			if (param == Display.KEY_UP) {
				_args[_args[0]]--;
				// 境界チェック
				if (_args[_args[0]] < 1) {
					if (_args[0] == 1) {
						_args[_args[0]] = 5;
					} else if (_args[0] == 2) {
						_args[_args[0]] = 4;
					} else if (_args[_args[0]] < 0) {
						_args[_args[0]] = 31;
					}
				}
			}

			if (param == Display.KEY_DOWN) {
				_args[_args[0]]++;
				// 境界チェック
				if (!isMatrix(_args[0], _args[_args[0]])) {
					if (_args[0] <= 2) {
						_args[_args[0]] = 1;
					} else {
						_args[_args[0]] = 0;
					}
				}
			}

			if (param == Display.KEY_LEFT) {
				_args[0]--;
				if (_args[0] < 1)
					_args[0] = 4;
			}

			if (param == Display.KEY_RIGHT) {
				_args[0]++;
				if (_args[0] > 4)
					_args[0] = 1;
			}

			if ((0 <= param) && (param <= 9)) {
				_args[_args[0]] %= 10;
				_args[_args[0]] *= 10;
				_args[_args[0]] += param;
				if (!isMatrix(_args[0], _args[_args[0]])) {
					_args[_args[0]] = param;
				}
				if (!isMatrix(_args[0], _args[_args[0]])) {
					_args[_args[0]] = 1;
				}
			}

			if (param == Display.KEY_SELECT) {
				// 選択ボタンを押したときはメニューに移動
				_args[0] = 0;
				parse(mgcEvents[22]);
			}
		}

		g$draw(null, G_REPAINT, 0, 0, 0, null);
		// 漂流中から復帰
		if (_scene == 5 && _args[0] < 0)
			_args[0] = 0;
	}

	/* ========================= */
	// シーン6 デモシーン
	/* ========================= */
	private void demoEvent(int type, int param) {
		if (type != Display.TIMER_EXPIRED_EVENT) {
			return;
		}

		if (!moving) {
			// 移動中アニメーション中でない場合
			mem[M_QUOTA] = _args[1];
			moving = true;
			moveAnime1();
			if (_args[4] >= 0) {
				demoEvent(type, param);
			}
		} else {
			// 主人公が移動中アニメーションである場合
			moveAnime2();

			boolean finalFlag = false;

			if (_args[2] > 1) {
				// 移動数がプラスの場合
				_args[2]--;
			} else if (_args[2] == 1) {
				// 移動数プラスがなくなった場合
				finalFlag = true;
			} else {
				// 移動数がマイナスの場合
				finalFlag = _args[2] * -1 != eventNo[getXYMap(mem[M_X],
						mem[M_Y])];
			}

			// 移動終了の場合
			if (finalFlag) {

				// デモ後スクリプトがある場合、シーン4
				// 無い場合、シーン1
				if (_args[3] < 0) {
					setScene(1);
				} else {
					setScene(4);
					parse(parsedEvent[_args[3]]);
					if ((lines[0].equals("")) && (_scene == 4)) {
						useCache = false;
						setScene(1);
					}
				}
				g$draw(null, G_REPAINT, 0, 0, 0, null);
			}
		}
	}

	/* ========================= */
	// シーン7 ライブラリシーン
	/* ========================= */

	private void libEvent(int type, int param) {
		if (type == Display.TIMER_EXPIRED_EVENT) {
			return;
		}

		boolean transed = msgEvent(type, param);
		if (transed) {
			int command = selLines[_sel];
			int index = 0;

			for (; index < _args.length; index++) {
				if (_args[index] < 0) {
					if (command < 0) {
						index--;
					}
					break;
				}
			}
			if (index < 1) {
				mem[M_RGN] = 0;
				setScene(0);
				g$draw(null, G_REPAINT, 0, 0, 0, null);
				return;
			}

			_args[index] = command;

			int returned = (_page * (W_ROW - 1) + (_sel - 1)) * -1 - 1;

			// 上限越えを防ぐ
			if (index >= _args.length - 1) {
				_args[_args.length - 1] = -1;
			}
			reparse();

			// 戻った時に前回選択した項目を選ぶ
			if (index == 2) {
				if (command < 0) {
					int retCommand = command * -1 - 1;
					_page = retCommand / (W_ROW - 1);
					_sel = retCommand % (W_ROW - 1) + 1;
				} else {
					selLines[0] = returned;
				}
			} else {
				_page = 0;
			}
		}

		g$draw(null, G_REPAINT, 0, 0, 0, null);
	}

	/* ========================= */
	// 全シーン共通
	/* ========================= */

	private boolean msgEvent(int type, int param) {
		if (type == Display.KEY_PRESSED_EVENT) {
			if (_sel <= 0) {
				// 選択モードでないときは、どのキーを押しても次画面に遷移
				return true;
			}

			if (param == Display.KEY_SELECT) {
				// 選択キーで次画面に遷移
				return true;
			}

			if ((param == Display.KEY_SOFT1) || (param == Display.KEY_IAPP)) {
				// ソフトキー1もしくはCLEARキー で前の遷移に戻る
				// ただし、menuEventsをパースしている時のみ
				// (つまりメニューシーン、戦闘シーンのみ)
				if (parsedEvent == menuEvents) {
					_sel = 0;
					return true;
				}
			}

			if (param == Display.KEY_RIGHT) {
				// 右キーで次のページへ
				_page++;
				if (_args[0] >= 0) {
					reparse();
				}
			}

			if (param == Display.KEY_LEFT) {
				// 左キーで前のページへ
				if (_page > 0) {
					_page--;
					if (_args[0] >= 0) {
						reparse();
					}
				}
			}

			if (param == Display.KEY_UP) {
				// カーソルを一つ上に移動。
				// もしカーソルが0以下になったら、
				// 行の終わりまでカーソルを下に移動する
				// (カーソルループ:上)
				if (--_sel < 1)
					while (isLine(++_sel + 1))
						;

			}

			if (param == Display.KEY_DOWN) {
				// カーソルを一つ下に移動。
				// もしカーソルが行の終わり以上になったら、
				// カーソルを1に戻す
				// (カーソルループ:下)
				if (!isLine(++_sel))
					_sel = 1;
			}

			if ((1 <= param) && (param <= 9)) {
				if (isLine(param)) {
					_sel = param;
				}
			}

		}

		if (type == Display.KEY_RELEASED_EVENT) {
			if ((1 <= param) && (param <= 9)) {
				if (isLine(param)) {
					_sel = param;
					return true;
				}
			}
		}
		return false;
	}

	private void reparse() {
		// メニュー選択がある場合、カーソルを最初にする
		if (_sel >= 1) {
			_sel = 1;
		}

		// パース対象がない場合、即座に終了
		if (parsedEvent == null) {
			return;
		}

		// コマンドスクリプト番号が負の数なら、
		// コマンドスクリプト0番を実行
		if (_args[0] < 0) {
			parse(parsedEvent[0]);
		} else if (_args[0] < parsedEvent.length) {
			parse(parsedEvent[_args[0]]);
		}
	}

	/**
	 * キャラが移動前→一歩移動に切り替わる際のアニメーション処理
	 */
	private void moveAnime1() {
		sftX = calcXY(mem[M_QUOTA], 0);
		sftY = calcXY(mem[M_QUOTA], -1);
		int xx = mem[M_X] + sftX;
		int yy = mem[M_Y] + sftY;
		if (!isMap(xx, yy)) {
			// 進入禁止の場所には進入できない
			sftX = 0;
			sftY = 0;
			g$draw(null, G_REPAINT, 0, 0, 0, null);
			return;
		}
		if ((_scene == 1) && !walkIn[getXYMap(xx, yy)]) {
			// シーン1の場合、進入禁止チップも進入できない
			sftX = 0;
			sftY = 0;
		}
		g$draw(null, G_REPAINT, 0, 0, 0, null);
	}

	/**
	 * キャラが半歩移動→一歩移動に切り替わる際の アニメーション処理
	 *
	 * @return 実際に移動したかどうか。fieldEventにてチップ依存イベントを処理するかどうかの判定に使用する。true→移動していない
	 *         false→移動した
	 */
	private boolean moveAnime2() {
		sftX *= 2;
		sftY *= 2;
		moving = false;

		g$draw(null, G_REPAINT, 0, 0, 0, null);

		boolean result = (sftX | sftY) == 0;

		mem[M_X] += sftX / 2;
		mem[M_Y] += sftY / 2;
		sftX = 0;
		sftY = 0;

		return result;
	}

	/* ========================= */
	// ゲームシステム用ユーティリティ
	/* ========================= */
	/**
	 * 戦闘中、誰かの行動直前に呼び出される 次回行動者を検索し、行動者が誰もいなかったらinitTurn()を実行してイニシアティブ初期化
	 * 行動者が敵の場合はランダムに行動を選択 防御を解除する 眠っている場合、睡眠を選択
	 */
	private void initButtle() {
		// トゥンブクトゥがパーティにいる場合、
		// トゥンブクトゥの遊び専用イニシアティブを用意
		if (id2mem(4) >= 0) {
			members[5] = 99;
		} else {
			members[5] = -1;
		}

		// 戦闘中は、必ず行動順番の最初のキャラを行動者とする
		// 行動順番一巡したら、再び行動順番を決定する
		int initiative = getInitiative();
		if (initiative < 0) {
			initTurn();
			initiative = getInitiative();
		}

		// もう行動できる人がいない場合、即座に返却
		if (initiative < 0)
			return;

		// 味方の場合、イニシアティブ値をidに変換
		if (initiative < 6) {
			initiative = members[initiative];
		}

		_args[1] = initiative;

		if (initiative >= 6) {
			// 敵が行動者の場合は、ランダムに対象者と行動を決める
			_args[0] = 2;
			if (initiative < 16) {
				int algo = 0;
				if (initiatives[initiative] < 99) {
					algo = randi(4);
				}

				// 戦闘評価フェーズ 現在選択した行動のダメージを記録する
				// optFlgの12から15を間借りし、それぞれのフラグ/4が奇数なら、
				// そのダメージを記録する
				if (0 != (getAlign(mem2id(_args[1])) & 256)) {
					if ((optFlg[12 + algo] & 255) < 255) {
						optFlg[12 + algo] += 4;
					}
				}
				_args[2] = enemyAlgo[mem2id(_args[1]) - 6][algo];
			} else {
				// トゥンブクトゥの遊び専用行動
				_args[2] = 31 + randi(4) + randi(4) - 3;
				// ※注意!! 後ろのguard[]、sleep[]配列に触ってはいけない！
				return;
			}
		}

		// 防御してる属性をはずす
		optFlg[initiative] &= -2;

		// 動けない場合、menuEvent[12](動けない)を表示
		if ((optFlg[initiative] & 2) != 0) {
			_args[0] = 12;
		}
	}

	private void idList(String[] cmNames, int type) {
		_sel = 1;
		if (_page < 0) {
			_page = 0;
		}
		int index = _page * (W_ROW - 1) * -1;
		boolean test = false;
		boolean nextPage = false;
		int order = 0;
		int roopMax = cmNames.length;
		if (cmNames == names) {
			// キャラ選択の場合、members[]配列の長さまでループ
			roopMax = 4;
		}

		for (int i = 0; i < roopMax; i++) {
			order = i;
			int amount = 0;
			// 魔法か、道具かによって処理を分化
			if (cmNames == mgcs) {
				test = eventFlg[i + F_MAGIC]; // 魔法の場合
				if (i == 5) {
					// 「砲撃」の場合、仲間にバレッタがいればtrue
					test = id2mem(3) >= 0;
				}
				if (i == 6) {
					// 「ウーレベ…」の場合、賢者の帽子を持っていればtrue
					test = getAmount(12) > 0;
				}
			}
			if (cmNames == iName) {
				amount = iAmount[i]; // 道具・装備の場合
				if (parsedEvent == menuEvents) {
					for (int j = 0; j < 4; j++) {
						if ((0 <= _args[1]) && (_args[1] < 6)) {
							if (((1 << (j + 6)) & type) != 0) {
								if ((i > 0) && (eqs[_args[1]][j] == i)) {
									amount++; // 動作者装備品を量に含める
								}
							}
						}
					}
				}
				test = amount > 0;
				test &= (iType[i] & type) >= type; // 装備の場合、箇所・装備者を考慮
				test |= (type >= 0) && (i == 0); // 装備の場合、(なし)を強制true
				test |= (type == 264) && (i == 48); // バレッタバトルビキニ胴装備
			}
			if (cmNames == names) {
				// キャラ名選択の場合
				order = members[i];
				test = order >= 0;
			}
			if (cmNames == saveList) {
				// セーブ一覧の場合
				test = true;
			}
			if (_scene == 7) {
				if (cmNames == iName) {
					// おまけ・アイテム一覧の場合
					test = !mapEvent[i].equals("") && i != 0;
				}
			}
			if (test) {
				index++;

				if (index >= W_ROW) {
					nextPage = true; // 次ページがあるかどうか
					break;
				}

				if (index >= 1) {

					if (_scene == 7) {
						// おまけ・アイテム一覧の場合、図鑑に登録されていないアイテムは「0」番
						if (cmNames == iName) {
							if ((completeItem & (1 << order)) == 0) {
								order = 0;
							}
						}
						// おまけ・モンスター一覧の場合、図鑑に登録されていないものは最後の番号
						if (cmNames == saveList) {
							if ((completeMonster & (1 << order)) == 0) {
								lines[index] = iName[0];
								order = mapEvent.length - 1;
							}
						}
					}
					lindex++;
					try {
						lines[index] = cmNames[order];
					} catch (Exception e) {
					}
					selLines[index] = order;
					if ((cmNames == iName) && (amount > 1) && (_scene != 7)) {
						// アイテムが2個以上あるなら、×個数を表示
						lines[index] += " ×" + amount;
					}
				}
			}
		}

		if (index <= 0) {
			if (_page > 0) {
				_page--;
				idList(cmNames, type);
			} else {
				_sel = 0;
				lines[1] = "選択肢がありません";
			}
		}

		// 先頭のラベルに左右ページへの移動のヒントをつける
		if ((_page > 0) && (index > 0)) {
			lines[0] += "←";
		}
		if (nextPage) {
			lines[0] += "→";
		}
	}

	/**
	 * 地図一覧を表示する
	 *
	 */
	private void mapList() {
		_sel = 1;
		if (_page < 0) {
			_page = 0;
		}
		if (_page > 4) {
			_page = 4;
		}

		lines[0] = "【宝の地図】";
		lines[1] = "現在の地図 (" + mem[M_ISL] + "-" + mem[M_RGN] + ")";
		selLines[1] = (mem[M_ISL] - 1) * 4 + (mem[M_RGN] - 1);
		for (int i = 2; i <= 5; i++) {
			lines[i] = (_page + 1) + "-" + (i - 1);
			selLines[i] = _page * 4 + i - 2;
		}

		// 先頭のラベルに左右ページへの移動のヒントをつける
		if (_page > 0) {
			lines[0] += "←";
		}
		if (_page < 4) {
			lines[0] += "→";
		}
	}

	/**
	 * 筏での移動履歴リストを表示する
	 *
	 */
	private void raftHList() {
		StringBuffer buff = new StringBuffer();
		for (int i = 0; i < raftHist.length; i++) {
			if (raftHist[i] < 0)
				break;
			int idx = raftHist[i];
			buff.append("履歴");
			buff.append(i + 1);
			buff.append("[島:");
			buff.append(raftTable[idx][0]);
			buff.append(" AREA:");
			buff.append(raftTable[idx][1]);
			buff.append(" 東:");
			buff.append(format(raftTable[idx][2], 2, ' '));
			buff.append(" 南:");
			buff.append(format(raftTable[idx][3], 2, ' '));
			buff.append("]");

			lines[lindex] = buff.toString();
			buff.setLength(0);
			selLines[lindex] = idx;
			lindex++;
		}
	}

	/**
	 * 対象者にダメージを与える。与えた結果、HPが0以下になったら 対象者は戦線離脱。 ダメージ量が0未満であれば、0ダメージを与える(回復はしない)
	 *
	 * @param point
	 *            与えるダメージ
	 */

	private int damage(int point) {
		if (point < 0) {
			point = 0;
		}
		int dfid = mem2id(_args[3]);
		if ((optFlg[_args[3]] & 1) != 0) {
			point /= 2; // 防御していればダメージは半減
		}
		lines[lindex++] = names[dfid] + " : " + point + "ダメージ";
		hps[_args[3]] -= point;
		if (hps[_args[3]] <= 0) {
			kill(dfid);
		}

		// 戦闘評価フェーズ optFlg12...15を間借りし、4で割った余りが
		// 奇数の箇所があればそこにダメージ*256を加算
		if (0 != (getAlign(mem2id(_args[1])) & 256)) {
			for (int i = 12; i < 16; i++) {
				if ((optFlg[i] & 4) != 0) {
					optFlg[i] += point * 256;
					break;
				}
			}
		}

		return point;
	}

	/**
	 * 引数に与えられたキャラクターの死亡手続きを行う
	 *
	 * @param dfid
	 *            死亡するキャラクター
	 */
	private void kill(int dfid) {
		lines[lindex++] = names[dfid] + " : 死亡";
		initiatives[id2mem(_args[3])] = -1; // イニシアティブ削除
		if (_args[3] >= 6) {
			// 敵を倒した場合
			// 経験点および宝物判定
			int enm = members[_args[3]];
			winXp += xps[enm];
			int prise = enemyDrop[enm - 6];
			if ((prise > 0) && (randi(5) == 0)) {
				winItem = prise;
			}
			// モンスター図鑑登録フラグをon
			eventFlg[F_MONSTER + enemyID[enm - 6]] = true;
		} else {
			hps[_args[3]] = 1;
			// トゥンブクトゥ死亡の場合、遊びイニシアティブも削除
			if (dfid == 4) {
				members[5] = -1;
			}
		}
		members[id2mem(_args[3])] = -1; // メンバー削除
	}

	/**
	 * アルケミアの判定 _args[3]が対象
	 *
	 * @return 0 効果なし 1 戦闘中抵抗失敗 2 物質変換可能
	 */
	private int alchemia() {
		if (_scene == 3) {
			// アルケミアの抵抗判定 次のパーセンテージ : 50 + (対象のMaxMP - 術者のMaxMP) / 4
			int regist = 50 + (getMaxMp(_args[3]) - getMaxMp(_args[1])) / 4;
			if (regist <= randi(100)) {
				if (_args[3] >= 6) {
					winGem += getMaxHp(_args[3]) / 16 + 1;
				}
				kill(_args[3]);
				return 1;
			}
		} else {
			if ((iAlign[_args[3]] & 256) == 256) {
				addItem(-1, _args[3]);
				addItem(1, _args[3] + 1);
				return 2;
			}
		}
		return 0;
	}

	/**
	 * 対象者をのHPを回復する。回復した結果、HPが最大HPを越えたら 最大HPまで回復する。
	 * 回復量が0未満であれば、0点回復する(ダメージは与えない)
	 *
	 * @param point
	 *            回復するポイント
	 */

	private void cure(int point) {
		int dfid = mem2id(_args[3]);
		if (point < 0) {
			point = 0;
		}
		if (getMaxHp(_args[3]) < hps[_args[3]] + point) {
			point = getMaxHp(_args[3]) - hps[_args[3]];
		}
		lines[lindex++] = names[dfid] + " : HP" + point + "回復";
		hps[_args[3]] += point;

		// 戦闘評価フェーズ optFlg12...15を間借りし、4で割った余りが
		// 奇数の箇所があればそこに回復量*256を加算
		if (0 != (getAlign(mem2id(_args[1])) & 256)) {
			for (int i = 12; i < 16; i++) {
				if ((optFlg[i] & 4) != 0) {
					optFlg[i] += point * 256;
					break;
				}
			}
		}
	}

	/**
	 * 最大HPを返す。アタルの場合、最大HPに防御点をプラスする
	 *
	 * @param id
	 *            キャラID
	 * @return 最大HP
	 */
	private int getMaxHp(int id) {
		int result = hpis[id];
		if (id < 6) {
			// 味方キャラの場合、レベル上昇値を加える
			result += hpds[id] * lvs[id] / 8;
		} else {
			// 敵キャラの場合、マスタデータを参照
			result = hpis[members[id]];
		}
		if (id == 2) {
			// アタルの場合、防御点を最大HPに加える
			result += getDefDf(id);
		}
		return result;
	}

	/**
	 * 最大MPを返す。
	 *
	 * @param id
	 *            キャラID
	 * @return 最大MP
	 */
	private int getMaxMp(int id) {
		int result = mpis[id];
		if (id < 6) {
			// 味方キャラの場合、レベル上昇値を加える
			result += mpds[id] * lvs[id] / 8;
		} else {
			// 敵キャラの場合、マスタデータを参照
			result = mpis[members[id]];
		}
		return result;
	}

	/**
	 * 攻撃力を返す。戦闘中の一時攻撃力加算値をプラス。アタルの場合、さらに防御力もプラス。ゾンビ化している場合、味方キャラは装備の攻撃力を足せない。
	 *
	 * @param id
	 *            キャラID
	 * @return 攻撃力
	 */
	private int getDefAt(int id) {
		int result = atis[id];
		if (id < 6) {
			// 味方キャラの場合、レベル上昇値を加える
			result += atds[id] * lvs[id] / 8;
			// ゾンビ化していない場合、武器の値をプラス
			if (!eventFlg[10]) {
				result += iValue[eqs[id][0]];
				if (id == 2) {
					// アタルの場合、防御点を攻撃力に加える
					result += getDefDf(id);
				}
			}
		} else {
			// 敵キャラの場合、マスタデータを参照
			result = atis[members[id]];
		}

		// 戦闘中の上昇補正をプラス
		result += safeArray(optAt, id, 0);
		return result;
	}

	/**
	 * 防御力を返す。戦闘中の一時防御力加算値をプラス。ゾンビ化している場合、味方キャラは0を返す。
	 *
	 * @param id
	 *            キャラID
	 * @return 防御力
	 */
	private int getDefDf(int id) {
		int result = 0;
		if (id >= 6) {
			// 敵キャラの場合、あらかじめ設定された値を
			// 防御点として返す
			result = dfes[members[id] - 6];
		} else {
			if (!eventFlg[10]) {
				// ゾンビ化していない時のみ防具の値をプラス
				result = iValue[eqs[id][1]] + iValue[eqs[id][2]]
						+ iValue[eqs[id][3]];
			}
			if (id == 5) {
				// アベシェの場合、攻撃力の値を防御点とする
				result += getDefAt(id);
			}
		}
		// 戦闘中の上昇補正をプラス
		result += safeArray(optDf, id, 0);
		return result;
	}

	/**
	 * 属性値を返す。ゾンビ化している場合、強制的にゾンビ属性を返す。
	 *
	 * @param id
	 *            キャラID
	 * @return 属性値
	 */
	private int getAlign(int id) {
		int result = 0;
		if (id >= 6) {
			// 敵キャラの場合、あらかじめ設定された値を
			// 属性値として返す
			result = align[id - 6];
		} else {
			result = iAlign[eqs[id][0]] | iAlign[eqs[id][1]]
					| iAlign[eqs[id][2]] | iAlign[eqs[id][3]];
			// ゾンビ化フラグがあるときはゾンビ化属性のみ
			if (eventFlg[10]) {
				result = 32;
			}
		}
		return result;
	}

	/**
	 * 次のレベルアップで必要な賃金を返す。モサメデスの場合、累計経験点を返す。バレッタの場合、現在所持金の1/16+1を返す。
	 *
	 * @param id
	 *            キャラID
	 * @return 必要賃金
	 */
	private int getNextXp(int id) {
		if ((id != 0) && (lvs[id] >= lvs[0])) {
			// 仲間のレベルがモサメデス以上の場合、-1を返す
			return -1;
		}
		if (id == 3) {
			// バレッタの場合現在の所持金の1/16 + 1を賃金とする
			int next = gem / 16 + 1;
			if(next > 255){
				next = 255;
			}
			return next;
		}
		// モサメデスの場合、レベル計算に
		// 現在Lvをひとつづつ加算する
		// (お金は消費するが、経験点は消費しないため)
		int latio = id == 0 ? lvs[0] : 1;
		return xps[id] * latio + lvs[id] * latio + 10 * (latio - 1);
	}

	/**
	 * デフォルト引数を用いて、安全に配列にアクセスする 配列のインデックスを超えた場合、デフォルト値が適用される
	 *
	 * @param array
	 * @param index
	 * @param defaulte
	 * @return
	 */
	private int safeArray(int[] array, int index, int defaulte) {
		if ((index < 0) || array.length <= index) {
			return defaulte;
		}
		return array[index];
	}

	/**
	 * 現在の状況にあわせて、適切なメニューラベルを返す
	 *
	 * @return
	 */
	private String getLabel() {

		String offence = "";

		if (_scene == 0) {
			// オープニング・エンディングシーンでは必ずゲームタイトル
			offence = "5つの宝島";
			// _args[0] が-1の場合はバージョン情報を追加
			if (_args[0] <= 0) {
				offence += "      Ver 1." + format(VER, 2, '0') + "."
						+ format((int) resVersion, 4, '0');
			}
			return offence;
		}

		if (_scene == 7 && _args[1] == 2) {
			offence = "アイテム一覧";
		} else if (_scene == 7 && _args[1] == 3) {
			offence = "モンスター一覧";
		} else if ((0 <= _args[1]) && (_args[1] < 6)) {
			offence = names[_args[1]];
		} else if ((6 <= _args[1]) && (_args[1] < 16)) {
			offence = names[members[_args[1]]];
		}

		String command = "";

		if (_args[0] == 1) {
			command = "攻撃";
		}

		if (_args[0] == 2) {
			command = "魔法";
			if ((0 <= _args[2]) && (_args[2] < 16)) {
				command = mgcs[_args[2]];
			}
		}
		if (_args[0] == 3) {
			command = "道具";
			if (_args[2] >= 0) {
				command = iName[_args[2]];
			}
		}
		if (_args[0] == 6) {
			command = "装備";
			if (_args[2] == 0)
				command = "武器";
			if (_args[2] == 1)
				command = "盾";
			if (_args[2] == 2)
				command = "胴装備";
			if (_args[2] == 3)
				command = "頭装備";
		}

		if (!lines[lindex].equals("")) {
			command = lines[lindex];
		}

		String delim = "";
		if ((!offence.equals("")) && (!command.equals(""))) {
			delim = "≫";
		}
		if ((offence.equals("")) && (command.equals(""))) {
			delim = "メニュー";
		}

		// 行動がすでに確定している場合(_args[2]に値が入っている場合)
		// フォーマットを変更
		if (_args[2] >= 0) {
			return offence + " : " + command;
		}

		return "【" + offence + delim + command + "】";

	}

	/**
	 * ある配列の中に指定した味方がいるかどうか判定
	 *
	 * @param id
	 *            指定した味方
	 * @param args
	 *            配列(_argsまたはmember)
	 * @param offset
	 * @return true : 味方がいる false : いない
	 */
	private boolean inMember(int id, int[] args, int offset) {
		for (int i = 0; i < 4; i++) {
			if (args[i + offset] == id) {
				return true;
			}
		}
		return false;
	}

	/**
	 * アイテムの追加・削除処理を行う
	 *
	 * @param iam
	 *            アイテム増減量
	 * @param iid
	 *            アイテム種類
	 */
	private void addItem(int iam, int iid) {
		if (_scene == 1)
			setScene(4);
		if (iam > 0) {
			lines[lindex++] = iName[iid] + "を手に入れた";
		}
		if (iam < 0) {
			lines[lindex++] = iName[iid] + "を失った";
		}
		iAmount[iid] += iam;
	}

	private void levelUp(int id) {
		int hp = getMaxHp(id);
		int mp = getMaxMp(id);
		int at = getDefAt(id);
		lvs[id]++;
		hp = getMaxHp(id) - hp;
		mp = getMaxMp(id) - mp;
		at = getDefAt(id) - at;
		lines[lindex++] = names[id] + "はレベル" + lvs[id] + "になった";
		lines[lindex++] = "HP上昇 : " + hp;
		lines[lindex++] = "MP上昇 : " + mp;
		lines[lindex++] = "攻撃力上昇 : " + at;
		hps[id] += hp;
		mps[id] += mp;

		// 金紗叉がLv10になるとマファイアを覚える
		if ((id == 1) && (lvs[id] == 10)) {
			eventFlg[F_MAGIC + 8] = true;
			lines[lindex++] = "魔法「マファイア」を手に入れた！";
		}
	}

	/**
	 * 現在戦闘が終了しているかどうかを返す。 味方(member[0..5]が存在しない場合は-1、
	 * 敵(members[6..16])が存在しない場合は1、 両陣営とも存在する場合は0
	 *
	 * @return 勝利(敵存在しない)1、決着なし0、敗北(味方存在しない)-1
	 */
	private int endButtle() {
		int won = 0;
		int lost = 0;
		for (int i = 0; i < 16; i++) {
			if (members[i] >= 0) {
				if (i < 6) {
					won = 2;
				} else {
					lost = -1;
				}
			}
		}
		return won + lost - 1;
	}

	/**
	 *
	 * @return 現在一番イニシアティブが高いキャラ番号を返す。すべてのキャラが行動終了状態だった場合、-1を返す
	 */
	private int getInitiative() {
		int maxDex = -1;
		for (int i = 0; i < initiatives.length; i++) {
			// すでにいないキャラクターはイニシアティブを設定しない
			if (members[i] < 0) {
				initiatives[i] = -1;
				continue;
			}
			if (initiatives[i] >= 0) {
				if ((maxDex < 0) || (initiatives[maxDex] < initiatives[i])) {
					maxDex = i;
				}
			}
		}
		return maxDex;
	}

	/**
	 * 入力されたIDから、隊列の番号を返す 隊列に編入されていなかった場合、-1を返す
	 *
	 * @param id
	 *            キャラ番号
	 * @return 隊列番号
	 */

	private int id2mem(int id) {
		if ((6 <= id) && (id < 16)) {
			// 敵キャラの場合、番号をそのまま返す
			return id;
		}
		for (int i = 0; i < 4; i++) {
			if (members[i] == id) {
				return i;
			}
		}
		return -1;
	}

	/**
	 * 入力された隊列番号から、キャラ番号を返す。敵キャラの場合のみマスタデータを返す
	 *
	 * @param mem
	 *            隊列番号
	 * @return キャラ番
	 */
	private int mem2id(int mem) {
		if (mem >= 99) {
			return 4;
		} else if (mem >= 6) {
			return members[mem];
		} else {
			return mem;
		}
	}

	/**
	 * 戦闘中、1ターンの最初に行うべき処理を実行
	 *
	 */
	private void initTurn() {
		// イニシアティブ計算
		for (int i = 0; i < members.length; i++) {
			if (members[i] >= 0) {
				initiatives[i] = safeArray(dexs, members[i], 0) + randi(8);
			} else {
				initiatives[i] = -1;
			}
		}
	}

	/**
	 * 味方の選択対象をランダムに決定
	 *
	 * @return 選択された味方のID
	 */

	private int chooseTarget() {
		int party = 3;
		while ((members[party] < 0) && (party >= 0))
			party--;
		int temp = randi(party + 1);
		return members[randi(temp + 1)];
	}

	/**
	 * 自陣の中で一番傷ついた対象を決定
	 *
	 * @return 選択されたID
	 */
	private int chooseWeaker() {
		int target = _args[1];
		int begin = 0;
		int end = 4;
		if (target >= 6) {
			begin = 6;
			end = 16;
		}
		int maxDamage = -1;
		for (int i = begin; i < end; i++) {
			if (members[i] >= 0) {
				int damage = getMaxHp(i) - hps[i];
				if (i < 6) {
					damage = getMaxHp(members[i]) - hps[members[i]];
				}
				if (maxDamage < damage) {
					target = i;
					maxDamage = damage;
				}
			}
		}
		if (target < 6) {
			return members[target];
		}
		return target;
	}

	/**
	 * 敵をランダムで決定
	 *
	 * @return 選択された敵のID
	 */
	private int chooseEnemy() {
		int target = 6;
		for (int i = 0; i < 128; i++) {
			target = randi(10) + 6;
			if (members[target] >= 0) {
				break;
			}
		}
		return target;
	}

	/**
	 * 敵選択。敵がいない部分をスキップする
	 *
	 * @param plus
	 *            選択肢の加算値
	 */
	private void enemySelCursor(int plus) {
		for (int i = 0; i < 11; i++) {
			_sel += plus;
			_sel = (_sel + 10) % 10;
			if (safeArray(members, _sel + 6, -1) >= 0) {
				break;
			}
			if (plus == 0) {
				plus = 1;
			}
		}
	}

	/**
	 * 敵の初期HPや初期MPを再設定する
	 *
	 * @param i
	 */
	private void setupEnemy(int i) {
		hps[i] = getMaxHp(i);
		mps[i] = getMaxMp(i);

		// 経験点0のモンスターは、出会っただけで図鑑リスト入り
		if (xps[members[i]] == 0) {
			eventFlg[F_MONSTER + members[i]] = true;
		}
	}

	/**
	 * 地図変化フラグを考慮して、指定された座標からチップ番号を取得する
	 *
	 * @param x
	 * @param y
	 */
	private byte getXYMap(int x, int y) {
		byte chip = 0;
		if (isMap(x, y)) {
			chip = xyMap[y][x];
		}
		// 地図変化フラグによりチップ変化
		if (eventFlg[F_MAP + y * MAP + x]) {
			chip++;
		}
		chip = (byte) (chip % walkIn.length);
		return chip;
	}

	/**
	 * 現在選択中のアイテムもしくは魔法Noを返す。
	 *
	 * @return
	 */
	private int getMNo() {
		int result = _args[2];
		// 選択肢が魔法か、アイテムかで処理分岐
		if (_args[0] == 3) {
			result = iEvents[result];
		}
		// イベント「16」のアイテムなら、アイテム番号を返す
		if (result == 16) {
			result = _args[2];
		}
		return result;
	}

	/**
	 * 座標が地図の妥当な値以内か判定する
	 *
	 * @param level
	 *            島・エリア・XY座標のうちどれか
	 * @param param
	 *            入力する値
	 * @return 妥当な値であればtrue
	 */
	private boolean isMatrix(int level, int param) {
		if (level == 1) {
			// 島番号の場合
			return ((1 <= param) && (param <= 5));
		} else if (level == 2) {
			// エリア番号の場合
			return ((1 <= param) && (param <= 4));
		} else {
			// X、Y座標の場合
			return ((0 <= param) && (param <= 31));
		}
	}

	/**
	 * 筏移動履歴を更新する
	 *
	 * @param idx
	 *            筏の移動インデックス
	 */
	private void updateRaftHList(byte idx) {
		byte[] temp = new byte[raftHist.length];
		System.arraycopy(raftHist, 0, temp, 0, raftHist.length);

		raftHist[0] = idx;
		int j = 1;
		for (int i = 0; i < temp.length; i++) {
			if (temp[i] != idx) {
				raftHist[j] = temp[i];
				j++;
			}
			if (j >= raftHist.length)
				break;
		}
	}

	/**
	 * 装備品を含めた、現在の所持アイテム数
	 *
	 * @param item
	 * @return
	 */
	private int getAmount(int item) {
		int result = 0;
		for (int i = 0; i < 6; i++) {
			for (int j = 0; j < 4; j++) {
				if (eqs[i][j] == item) {
					result++;
				}
			}
		}
		return result + iAmount[item];
	}

	/* ========================= */
	// 自動生成マップ関連
	/* ========================= */

	/**
	 * 自動生成マップを設定する
	 *
	 * @param pos
	 *            マップ全体を4×4分割した値で、0..15の数値で表す
	 * @param index
	 */
	private void setSubMap(int pos, int index) {
		int x = pos % 4;
		int y = pos / 4;
		x = x * 8;
		y = y * 256;
		int pos2 = y + x;
		for (int i = 0; i < 8; i++) {
			for (int j = 0; j < 8; j++) {
				if ((submaps[index][i] & (1 << j)) != 0) {
					eventFlg[F_MAP + pos2 + j + i * 32] = true;
				}
			}
		}
	}

	private int getWallIndex(int pos, int aquota) {
		int offset = (aquota / 4) * 24;
		int quota = aquota % 4;
		if (quota % 2 == 0) {
			if (pos % 4 == quota * 3 / 2) {
				return -1;
			}
			return pos - (pos / 4) + (quota / 2) - 1 + offset;
		} else {
			int pos2 = pos + quota * 2 + 6;
			if (pos2 < 12 || 24 <= pos2) {
				return -1;
			}
			return pos2 + offset;
		}
	}

	private boolean calcWall(int pos, int quota) {
		int windex = getWallIndex(pos, quota);
		if (windex < 0) {
			return quota < 4;
		} else {
			return eventFlg[FF_SUBMAP + windex];
		}
	}

	private boolean fitWall(int pos, int wall) {
		for (int i = 0; i < 4; i++) {
			if (calcWall(pos, i)) {
				if ((wall & (1 << i)) == 0) {
					return false;
				} else {
					continue;
				}
			}
			if (calcWall(pos, i + 4) && ((wall & (1 << i)) != 0)) {
				return false;
			}
		}
		return true;
	}

	private int setWall(int pos, int wall, boolean set) {
		int freeIndex = 0;
		for (int i = 0; i < 4; i++) {
			int offset = 0;
			if (((1 << i) & wall) == 0) {
				offset = 24;
			}
			int windex = getWallIndex(pos, i + offset / 6);
			if (windex >= 0) {
				if (set) {
					eventFlg[FF_SUBMAP + windex] = true;
				} else {
					if (offset > 0) {
						if (eventFlg[FF_SUBMAP + windex]) {
							freeIndex--;
						} else {
							freeIndex++;
						}
					}
				}
			}
		}
		return freeIndex;
	}

	private void goFloor(int pos) {
		for (int i = 0; i < 128; i++) {
			int wall = randi(16);
			if (fitWall(pos, wall)) {
				int free = setWall(pos, wall, false);
				if (mem[M_FINDEX] + free > 0) {
					mem[M_FINDEX] += free;
					setWall(pos, wall, true);
					setSubMap(pos, wall);
					break;
				}
			}
		}
		eventFlg[FF_STABLE + pos] = true;
	}

	/* ========================= */
	// 文字列ユーティリティ
	/* ========================= */

	// 文字を右詰にフォーマット
	private String format(int data, int len, char pad) {
		StringBuffer result = new StringBuffer();
		String sdata = data + "";
		for (int i = len; i > 0; i--) {
			if (sdata.length() < i) {
				result.append(pad);
			} else {
				result.append(sdata.charAt(sdata.length() - i));
			}
		}
		return result.toString();
	}

	// スクリプトの実行
	private void parse(String string) {
		char[] c = string.toCharArray();
		StringBuffer buffer = new StringBuffer();
		int[] args = new int[16];
		int idx = -1;
		boolean skip = false;
		boolean escape = false;
		boolean end = false;
		boolean setFlag = false;

		int i = 0;
		while (i < c.length) {
			if (i == 0) {
				// ループの初期処理
				clearLines();
				lindex = 0;
				_sel = 0;
				buffer.setLength(0);
				idx = -1;
				skip = false;
			}

			if (escape) {
				buffer.append(c[i]);
				escape = false;
			} else if (c[i] == '\n') {
				if (buffer.length() > 0) {
					lines[lindex] = buffer.toString();
					lindex++;
					if (_scene == 1) {
						setScene(4);
					}
				}
				skip = false;
				buffer.setLength(0);
				if (lindex >= W_ROW) {
					lindex = 0;
					break;
				}
				if (end) {
					break;
				}
			} else if (skip) {
				i++;
				continue;
			} else if (c[i] == '\\') {
				escape = true;
			} else if (c[i] == '}') {
				int startWith = buffer.toString().indexOf("${");
				if (startWith >= 0) {
					String key = buffer.toString().substring(startWith + 2);
					String value = "";
					if (key.equals("eqs")) {
						value = iName[eqs[_args[1]][args[idx--]]];
					} else if (key.equals("label")) {
						value = getLabel();
					} else if (key.equals("of")) {
						value = names[mem2id(_args[1])];
					} else if (key.equals("df")) {
						value = names[mem2id(_args[3])];
					} else if (key.equals("iName")) {
						value = iName[args[idx--]];
					} else if (key.equals("value")) {
						value = args[idx--] + "";
					}
					buffer.delete(startWith, buffer.length());
					buffer.append(value);
				} else {
					buffer.append(c[i]);
				}
			} else if (c[i] == ' ') {
				try {
					String token = buffer.toString();
					if (showToken) {
						System.out.println(token);
					}
					if (token.equals("+")) {
						int result = args[idx--] + args[idx--];
						args[++idx] = result;
					} else if (token.equals("-")) {
						int result = args[idx--] - args[idx--];
						args[++idx] = result;
					} else if (token.equals("*")) {
						int result = args[idx--] * args[idx--];
						args[++idx] = result;
					} else if (token.equals("/")) {
						int result = 0;
						try {
							result = args[idx--] / args[idx--];
						} catch (Exception e) {
						}
						args[++idx] = result;
					} else if (token.equals("%")) {
						int result = 0;
						try {
							result = args[idx--] % args[idx--];
						} catch (Exception e) {
						}
						args[++idx] = result;
						args[++idx] = result;
					} else if (token.equals("&")) {
						int result = args[idx--] & args[idx--];
						args[++idx] = result;
					} else if (token.equals("|")) {
						int result = args[idx--] | args[idx--];
						args[++idx] = result;
					} else if (token.equals("^")) {
						int result = args[idx--] ^ args[idx--];
						args[++idx] = result;
					} else if (token.equals("<<")) {
						int result = args[idx--] << args[idx--];
						args[++idx] = result;
					} else if (token.equals(">>")) {
						int result = args[idx--] >> args[idx--];
						args[++idx] = result;
					} else if (token.equals("<")) {
						int result = args[idx--] > args[idx--] ? 1 : 0;
						args[++idx] = result;
					} else if (token.equals(">")) {
						int result = args[idx--] < args[idx--] ? 1 : 0;
						args[++idx] = result;
					} else if (token.equals("==")) {
						int result = args[idx--] == args[idx--] ? 1 : 0;
						args[++idx] = result;
					} else if (token.equals("cp")) {
						int value = args[idx];
						args[++idx] = value;
					} else if (token.equals(":")) {
						selLines[lindex] = args[idx--];
						_sel = 1;
					} else if (token.equals("#")) {
						selLines[0] = args[idx--];
					} else if (token.equals("@")) {
						skip = (_scene != 2);
					} else if (token.equals("rand")) {
						args[++idx] = randi(4) + randi(4) - 3;
					} else if (token.equals("randi")) {
						args[idx] = randi(args[idx]);
					} else if (token.equals("sum")) {
						int result = 0;
						do {
							result += args[idx];
						} while (--idx >= 0);
						args[0] = result;
						idx = 0;
					} else if (token.equals("if")) {
						skip = (args[idx--] == 0);
					} else if (token.equals("not")) {
						args[idx] = (args[idx] == 0) ? 1 : 0;
					} else if (token.equals("end")) {
						end = true;
					} else if (token.equals("set")) {
						setFlag = true;
					} else if (token.equals("clear")) {
						idx = -1;
					} else if (token.equals("damage")) {
						// バレッタの特殊イベント処理
						int eventid = specialEvent();
						if (eventid == 41) {
							if (hps[6] <= args[idx]) {
								i = 0;
								c = mgcEvents[41].toCharArray();
								continue;
							}
						}
						args[idx] = damage(args[idx]);
					} else if (token.equals("kill")) {
						kill(mem2id(_args[3]));
					} else if (token.equals("cure")) {
						cure(args[idx--]);
					} else if (token.equals("fireDamage")) {
						int firealign = getAlign(mem2id(_args[3])) & 7;
						int damage = args[idx--]
								* safeArray(fireTable, firealign, 2) / 2;
						// 4/3の消火装置イベントを処理
						if ((mem[M_ISL] == 4) && (mem[M_RGN] == 3)
								&& (eventFlg[F_MAP + 3])) {
							damage = 0;
						}
						if (damage >= 0) {
							// バレッタの特殊イベント処理
							int eventid = specialEvent();
							if (eventid == 41) {
								if (hps[6] <= damage) {
									i = 0;
									c = mgcEvents[41].toCharArray();
									continue;
								}
							}
							damage(damage);
						} else {
							cure(-damage);
						}
					} else if (token.equals("run")) {
						if (args[idx--] > randi(10)) {
							int runner = args[idx--];
							if (runner < 6) {
								if (run1()) {
									winScript = -1;
								}
							} else {
								members[runner] = -1;
							}
							lines[lindex++] = "立ち去った";
						} else {
							lines[lindex++] = "逃走に失敗!!";
						}
					} else if (token.equals("buy?")) {
						if (gem >= args[idx]) {
							gem -= args[idx];
							args[idx] = 1;
						} else {
							lines[lindex++] = "お金が足りない";
							args[idx] = 0;
						}
					} else if (token.equals("resetButtle")) {
						run1();
					} else if (token.equals("spend?")) {
						args[++idx] = ((_args[2] >= 16) || (mps[_args[1]] >= (_args[2] + 10) / 4)) ? 1
								: 0;
					} else if (token.equals("spend!")) {
						endAction(false);
					} else if (token.equals("regist?")) {
						// 対象の抵抗値計算
						int result = 0;
						int al = getAlign(mem2id(_args[3])) & 24;
						if (al == 8) {
							// 属性値が8なら、無条件に抵抗
							result = 1;
						} else if (al == 16) {
							// 属性値が16なら、無条件に効果あり
						} else if (randi(10) < args[idx]) {
							// それ以外なら、引数×10%の確率で成功
							result = 1;
						}
						args[idx] = result;
					} else if (token.equals("dispell?")) {
						// 1/2の確率で呪文無効化
						args[++idx] = randi(2)
								* ((getAlign(mem2id(_args[3])) & 128) / 128);
					} else if (token.equals("comp?")) {
						args[++idx] = (completeItem & completeMonster) == 0 ? 0
								: 1;
					} else if (token.equals("comp!")) {
						completed();
					} else if (token.equals("chestComp?")) {
						int ends = args[idx--];
						int begins = args[idx--];
						chestCompleted(begins, ends);
					} else if (token.equals("getMNo")) {
						args[++idx] = getMNo();
					} else if (token.equals("getMf")) {
						int mf = getMaxMp(_args[1]) / 4;
						// 賢者の帽子を装備しているキャラは魔法効果+3
						if (_args[1] < 6)
							if (eqs[_args[1]][3] == 12)
								mf += 3;
						args[++idx] = mf;
					} else if (token.equals("getNextXp")) {
						args[++idx] = getNextXp(_args[1]);
					} else if (token.equals("getMaxHp")) {
						args[++idx] = getMaxHp(_args[3]);
					} else if (token.equals("getMaxMp")) {
						args[++idx] = getMaxMp(_args[3]);
					} else if (token.equals("getWinScript")) {
						args[++idx] = winScript;
					} else if (token.equals("winXp")) {
						args[++idx] = winXp;
					} else if (token.equals("winGem")) {
						args[++idx] = winGem;
					} else if (token.equals("winItem")) {
						args[++idx] = winItem;
					} else if (token.equals("getLv")) {
						args[++idx] = lvs[_args[1]];
					} else if (token.equals("getItemEvent")) {
						args[idx] = iEvents[args[idx]];
					} else if (token.equals("getSubWindow")) {
						args[++idx] = subWSts;
					} else if (token.equals("getCost")) {
						args[idx] = iCost[args[idx]];
					} else if (token.equals("getXp")) {
						args[++idx] = exp;
					} else if (token.equals("getGem")) {
						args[++idx] = gem;
					} else if (token.equals("getChip")) {
						int y = args[idx--];
						int x = args[idx--];
						args[++idx] = getXYMap(x, y);
					} else if (token.equals("getArgs")) {
						args[idx] = _args[args[idx]];
						if (showToken)
							System.out.println("**[" + args[idx] + "]**");
					} else if (token.equals("spot")) {
						args[++idx] = checkSpot();
					} else if (token.equals("spotPos")) {
						args[++idx] = spotPos;
					} else if (token.equals("chipEvent")) {
						args[idx] = eventNo[args[idx]];
					} else if (token.equals("atdf")) {
						args[++idx] = getDefAt(_args[1]) - getDefDf(_args[3]);
						// 回避属性をもっている敵は1/4の確率で攻撃を回避
						// // 4/3の眼潰しイベントを処理
						if (((getAlign(mem2id(_args[3])) & 64) == 64)
								|| ((mem[M_ISL] == 4) && (mem[M_RGN] == 3) && (eventFlg[F_MAP + 275]))) {
							if (randi(4) == 0) {
								lines[lindex++] = names[mem2id(_args[3])]
										+ " : 回避成功！";
								skip = true;
							}
						}
					} else if (token.equals("getRc")) {
						// 隊列の後ろにいる場合の直接ダメージ補正
						int member = id2mem(_args[3]);
						if (member < 6) {
							args[++idx] = member * -2;
						} else {
							args[++idx] = 0;
						}
					} else if (token.equals("calcClit")) {
						// クリティカルヒットのダメージ計算
						int damage = args[idx];
						if (damage < 0)
							damage = 0;
						if (randi(4) == 0)
							damage++;
						args[idx] = damage;
					} else if (token.equals("flg")) {
						if (setFlag) {
							eventFlg[args[idx--]] = (args[idx--] != 0);
							setFlag = false;
						} else {
							args[idx] = eventFlg[args[idx]] ? 1 : 0;
						}
					} else if (token.equals("mem")) {
						if (setFlag) {
							mem[args[idx--]] = args[idx--];
							setFlag = false;
							if (_scene == 1) {
								useCache = false;
								showWindow = false;
							}
						} else {
							args[idx] = mem[args[idx]];
						}
					} else if (token.equals("setSubWindow")) {
						subWSts = args[idx--];
					} else if (token.equals("setXp")) {
						exp = args[idx--];
					} else if (token.equals("mp")) {
						// ゾンビはMP増減しない
						if (!setFlag || (getAlign(mem2id(_args[3])) & 32) == 0) {
							args[15] = idx;
							args[14] = 3;
							intVal(mps, args, setFlag);
							// 最大・最小MPを考慮に入れる
							if (mps[_args[3]] < 0)
								mps[_args[3]] = 0;
							if (mps[_args[3]] > getMaxMp(_args[3]))
								mps[_args[3]] = getMaxMp(_args[3]);
							idx = args[15];
							setFlag = false;
						}
					} else if (token.equals("hp")) {
						args[15] = idx;
						args[14] = 3;
						intVal(hps, args, setFlag);
						// 最大・最小HPを考慮に入れる
						if (hps[_args[3]] < 1)
							hps[_args[3]] = 1;
						if (hps[_args[3]] > getMaxHp(_args[3]))
							hps[_args[3]] = getMaxHp(_args[3]);
						idx = args[15];
						setFlag = false;
					} else if (token.equals("at")) {
						args[15] = idx;
						args[14] = 3;
						intVal(optAt, args, setFlag);
						idx = args[15];
						setFlag = false;
					} else if (token.equals("df")) {
						args[15] = idx;
						args[14] = 3;
						intVal(optDf, args, setFlag);
						idx = args[15];
						setFlag = false;
					} else if (token.equals("optFlg")) {
						args[15] = idx;
						args[14] = 3;
						intVal(optFlg, args, setFlag);
						idx = args[15];
						setFlag = false;
					} else if (token.equals("member")) {
						if (setFlag)
							initiatives[idx] = -1;
						args[15] = idx;
						args[14] = -1;
						intVal(members, args, setFlag);
						idx = args[15];
						setFlag = false;
					} else if (token.equals("drop")) {
						args[15] = idx;
						args[14] = -1;
						byteVal(enemyDrop, args, setFlag);
						idx = args[15];
						setFlag = false;
					} else if (token.equals("chip")) {
						int chip = args[idx--];
						if (setFlag) {
							xyMap[chip / MAP][chip % MAP] = (byte) args[idx--];
						} else {
							args[++idx] = xyMap[chip / MAP][chip % MAP];
						}
						setFlag = false;
					} else if (token.equals("walkIn?")) {
						args[idx] = walkIn[getXYMap(args[idx] % MAP, args[idx]
								/ MAP)] ? 1 : 0;
					} else if (token.equals("setWinScript")) {
						winScript = args[idx--];
					} else if (token.equals("setGem")) {
						gem = args[idx--];
					} else if (token.equals("setMap")) {
						useCache = false;
						loadMap(mem[M_ISL], mem[M_RGN]);
					} else if (token.equals("setArgs")) {
						_args[args[idx--]] = args[idx--];
					} else if (token.equals("getAmount")) {
						args[idx] = getAmount(args[idx]);
					} else if (token.equals("item")) {
						if (setFlag) {
							iAmount[args[idx--]] = args[idx--];
							setFlag = false;
						} else {
							args[idx] = iAmount[args[idx]];
						}
					} else if (token.equals("memberList")) {
						memberList();
					} else if (token.equals("addItem")) {
						int iam = args[idx--];
						int iid = args[idx--];
						addItem(iam, iid);
					} else if (token.equals("alchemia")) {
						args[++idx] = alchemia();
					} else if (token.equals("calcAlgo")) {
						int maxIndex = 0;
						int maxValue = 0;
						for (int j = 0; j < 4; j++) {
							if ((optFlg[j + 12] & 255) != 0) {
								int value = (optFlg[j + 12] / 256)
										/ ((optFlg[j + 12] & 255) / 8);
								if (maxValue < value) {
									maxValue = value;
									maxIndex = j;
								}
							}
						}
						args[++idx] = maxIndex;
					} else if (token.equals("magicList")) {
						idList(mgcs, 0);
					} else if (token.equals("itemList")) {
						idList(iName, args[idx--]);
					} else if (token.equals("saveList")) {
						boolean init = saveList == null;
						if (init) {
							updateSaveList();
							_page = slotIndex / (W_ROW - 1);
						}
						idList(saveList, 0);
						if (init) {
							_sel = slotIndex % (W_ROW - 1) + 1;
						}
					} else if (token.equals("monsterList")) {
						if (saveList == null) {
							updateMonsterList();
						}
						idList(saveList, 0);
					} else if (token.equals("mapList")) {
						mapList();
					} else if (token.equals("raftHList")) {
						raftHList();
					} else if (token.equals("raftHist")) {
						updateRaftHList((byte) args[idx--]);
					} else if (token.equals("clrMPF")) {
						clearMapFlg();
					} else if (token.equals("clrFlg")) {
						int istart = args[idx--];
						int iend = args[idx--];
						for (int ii = istart; ii >= iend; ii--) {
							eventFlg[ii] = false;
						}
					} else if (token.equals("loadMEvent")) {
						i = 0;
						c = mgcEvents[args[idx--]].toCharArray();
						continue;
					} else if (token.equals("loadMapEvent")) {
						i = 0;
						c = mapEvent[args[idx--]].toCharArray();
						if (_scene == 3)
							_scene = 4;
						if (_args[0] == 15)
							_scene = 4;
						continue;
					} else if (token.equals("loadDefault")) {
						i = 0;
						_args[3] = -2;
						c = parsedEvent[_args[0]].toCharArray();
						continue;
					} else if (token.equals("go")) {
						_args[2] = args[idx--];
						_args[1] = args[idx--];
						setScene(6);
						break;
					} else if (token.equals("gameOver")) {
						setScene(0);
						end = true;
					} else if (token.equals("loadGame")) {
						loadGame(_args[2]);
					} else if (token.equals("loadService")) {
						if (_scene != 7) {
							loadMap(0, _args[1]);
							setScene(7);
						}
					} else if (token.equals("goFloor")) {
						goFloor((mem[M_X] / 8) + (mem[M_Y] / 8) * 4);
					} else if (token.equals("treasureMap")) {
						i = 0;
						c = mapTexts[args[idx--]].toCharArray();
					} else if (token.equals("mem2id")) {
						args[++idx] = mem2id(_args[1]);
					} else if (token.equals("id2mem")) {
						args[++idx] = id2mem(_args[1]);
					} else if (token.equals("selectEnemy")) {
						if ((_scene == 3) && (selectEnemy == 0)) {
							selectEnemy = 1;
						}
					} else if (token.equals("eq")) {
						args[idx] = eqs[_args[1]][args[idx]];
					} else if (token.equals("equip")) {
						iAmount[eqs[_args[1]][_args[2]]]++;
						iAmount[_args[3]]--;
						iAmount[0] = 0;
						eqs[_args[1]][_args[2]] = _args[3];
						if (_args[1] == 2) {
							// アタルの場合、最大HPを超過するか判定
							if (hps[_args[1]] > getMaxHp(_args[1])) {
								hps[_args[1]] = getMaxHp(_args[1]);
							}
						}
						_args[3] = -1;
						_args[2] = -1;
						i = 0;
						c = menuEvents[_args[0]].toCharArray();
						continue;
					} else if (token.equals("vChange")) {
						for (int vchange = 0; vchange < 4; vchange++) {
							if (_args[vchange + 1] == 99) {
								members[vchange] = -1;
							} else {
								members[vchange] = _args[vchange + 1];
							}
							_args[vchange + 1] = -1;
						}
						_args[0] = -1;
						i = 0;
						c = menuEvents[0].toCharArray(); // メインメニューに戻る
						continue;
					} else if (token.equals("levelUp")) {
						levelUp(_args[1]);
					} else if (token.equals("endAction")) {
						endAction(true);
						end = true; // パースをこの行で停止
					} else if (token.equals("endEnemy")) {
						if (_args[1] >= 6
								|| initiatives[id2mem(_args[1])] >= 99) {
							endAction(true);
						}
						end = true; // パースをこの行で停止
					} else if (token.equals("fetch")) {
						fetch();
					} else if (token.equals("encounter")) {
						for (int ii = 6; ii < 16; ii++) {
							if ((idx >= 0) && (members[ii] < 0)) {
								members[ii] = args[idx--];
								setupEnemy(ii);
							}
						}
						// 戦闘シーン以外だったら、戦闘シーンに変更
						if (_scene != 3) {
							setScene(3);
							useCache = false;
						}
					} else if (token.equals("save")) {
						args[++idx] = saveGame(_args[2]);
					} else if (token.equals("delGame")) {
						clearSave(_args[2]);
					} else if (token.equals("raft")) {
						// 筏シーンにする
						setScene(5);
					} else if (token.equals("inn")) {
						for (int ii = 0; ii < 6; ii++) {
							hps[ii] = getMaxHp(ii);
							// ゾンビ化しているときはmp回復しない
							if (!eventFlg[10]) {
								mps[ii] = getMaxMp(ii);
							}
						}
					} else if (token.equals("attt")) {
						// アタタタッの連続攻撃処理
						nextAttt = _args[3];
					} else if (token.equals("retarget")) {
						if (_args[1] >= 6) {
							_args[3] = chooseTarget();
						} else if (initiatives[id2mem(_args[1])] >= 99) {
							_args[3] = chooseEnemy();
						}
					} else if (token.equals("entarget")) {
						if (_args[1] >= 6
								|| initiatives[id2mem(_args[1])] >= 99) {
							_args[3] = chooseWeaker();
						}
					} else if (token.equals("selftarget")) {
						if (_args[1] >= 6
								|| initiatives[id2mem(_args[1])] >= 99) {
							_args[3] = _args[1];
						}
					} else if (token.equals("buttle?")) {
						args[++idx] = _scene == 3 ? 1 : 0;
					} else if (token.equals("memSelect?")) {
						args[idx] = inMember(args[idx], _args, 1) ? 1 : 0;
					} else if (token.equals("noChoice?")) {
						args[++idx] = lindex <= 1 ? 1 : 0;
					} else if (token.equals("zombie?")) {
						args[++idx] = (getAlign(mem2id(_args[3])) & 32) == 0 ? 0
								: 1;
					} else if (token.equals("flagMapping")) {
						// イベントフラグをマップフラグに対応づける
						int eflg = 0;
						int mflg = 0;
						for (; idx >= 0; idx -= 2) {
							mflg = args[idx];
							eflg = args[idx - 1];
							eventFlg[mflg + F_MAP] = eventFlg[eflg];
						}
					} else if (token.equals("argsMapping")) {
						// _args[4]以降に引数4つを割り当てる
						_args[7] = args[idx--];
						_args[6] = args[idx--];
						_args[5] = args[idx--];
						_args[4] = args[idx--];
					} else if (token.equals("flagRect")) {
						// 地図上の矩形範囲にフラグを適用する
						int rectHeight = args[idx--];
						int rectWidth = args[idx--];
						int start = args[idx--];
						boolean flag = args[idx--] != 0;
						for (int h = 0; h < rectHeight; h++) {
							for (int w = 0; w < rectWidth; w++) {
								eventFlg[F_MAP + start + h * MAP + w] = flag;
							}
						}
					} else if (token.equals("q2map")) {
						// mem[M_QUOTA]の上下左右をmapの加算値{-32,+32,-1,+1}に変換
						args[idx] = args[idx] = (((args[idx] >> 1) * 2) - 1)
								* ((args[idx] & 1) * (MAP - 1) + 1);
					} else if (token.equals("freeImage")) {
						freeImage[3] = args[idx--];
						freeImage[2] = args[idx--];
						freeImage[1] = args[idx--];
						freeImage[0] = args[idx--];
					} else if (token.equals("dump")) {
						System.out.println("==dump==");
						for (int j = 0; j < args.length; j++) {
							if (j == idx) {
								System.out.print(">");
							}
							System.out.println(args[j]);
						}
					} else {
						try {
							args[++idx] = Integer.parseInt(token);
						} catch (Exception e) {
							System.err.println("token error!! \"" + token
									+ "\"");
							System.out.println(string);
							System.out.println(i);
						}
					}
					buffer.setLength(0);
				} catch (Exception e) {
					System.out.println("error occured at token '" + buffer
							+ "'");
				}
			} else {
				buffer.append(c[i]);
			}
			i++;
		}
		// フィールドシーンのまま終了した場合、パーススクリプトをリセット
		if (_scene == 1) {
			setScene(1);
		}
	}

	/**
	 * 「memberList」がパースされたときの処理 敵選択フラグが正の値のときは敵の選択画面、 それ以外のときは現在のパーティの選択画面
	 * アタタタッ実行中のときは敵か味方をランダムに選択
	 */
	private void memberList() {
		if (safeArray(initiatives, _args[1], -1) >= 256) {
			// アタタタッ実行中のとき
			if (selectEnemy > 0) {
				while (members[_args[3]] >= 0) {
					_args[3] = randi(10) + 6;
				}
			} else {
				while (members[_args[3]] >= 0) {
					_args[3] = randi(4);
				}
			}
			lines[lindex++] = names[mem2id(_args[3])] + "をターゲットにした";
			return;
		}

		if (selectEnemy > 0) {
			if (_sel + 6 >= 0) {
				// 敵の名前を1列目に表示
				enemySelCursor(0);
				lines[lindex++] = names[members[_sel + 6]];
			}
			if (_sel < 5) {
				// 味方を選択する機能を表示
				lines[lindex++] = "　(↑キーで味方を選択)";
			}
		} else {
			idList(names, 0);
			if ((_scene == 3)) {
				// 戦闘シーンの場合、敵を選択する機能を表示
				lines[lindex] = "▽敵を選択";
				selLines[lindex] = 99;
				lindex++;
			}
		}
	}

	/**
	 * 味方が逃げる時の処理 取得経験値、アイテムを0にして戦闘を終了する
	 *
	 * @return 大岩しか敵がいない時にはfalse それ以外の敵がいるときにはtrue(勝利後スクリプトをクリアする)
	 */
	private boolean run1() {
		int rockOnly = 0; // 大岩の罠対応。敵が大岩1匹だけ存在している時は逃げても勝利
		int ii = 0;
		for (; ii < _args.length; ii++) {
			_args[ii] = -1;
		}
		for (ii = 6; ii < 16; ii++) {
			if (rockOnly == 0 && members[ii] > 6
					&& enemyID[members[ii] - 6] == 44) {
				rockOnly = 1;
			} else if (members[ii] >= 0) {
				rockOnly = -1;
			}
			members[ii] = -1;
		}

		if (rockOnly <= 0) {
			winXp = 0;
			winItem = 0;
			winGem = 0;

			for (int i = 0; i < initiatives.length; i++) {
				initiatives[i] = -1;
			}
		}

		return rockOnly <= 0;
	}

	/**
	 * parse()メソッド用ユーティリティ。setFlagがtrueなら対象配列に代入、 falseなら対象配列から値を読み込み
	 * parse()メソッドのidxに当たる変数はargs[15]に保持されるので、あらかじめ args[15]にidxの値をセットする必要がある
	 * また、args[14]の値で対象変数が分岐する。
	 * args[14]が0か正の値なら_args[]配列を対象の値とする。例えばargs[14]==3なら対象となる値は_args[3]
	 * args[14]が負の値ならidx(すなわちargs[15])の値を使用する
	 *
	 * @param val
	 *            対象となるint配列
	 * @param args
	 *            parse()メソッドのargs
	 * @param setFlg
	 *            parse()メソッドのsetFlg
	 */
	private void intVal(int[] val, int[] args, boolean setFlag) {
		if (args[14] >= 0) {
			if (setFlag) {
				val[_args[args[14]]] = args[args[15]--];
			} else {
				args[++args[15]] = val[_args[args[14]]];
			}
		} else {
			if (setFlag) {
				val[args[args[15]--]] = args[args[15]--];
			} else {
				args[args[15]] = val[args[args[15]]];
			}
		}
	}

	/**
	 * parse()メソッド用ユーティリティ。setFlagがtrueなら対象配列に代入、 falseなら対象配列から値を読み込み
	 * parse()メソッドのidxに当たる変数はargs[15]に保持されるので、あらかじめ args[15]にidxの値をセットする必要がある
	 * また、args[14]の値で対象変数が分岐する。
	 * args[14]が0か正の値なら_args[]配列を対象の値とする。例えばargs[14]==3なら対象となる値は_args[3]
	 * args[14]が負の値ならidx(すなわちargs[15])の値を使用する
	 *
	 * @param val
	 *            対象となるint配列
	 * @param args
	 *            parse()メソッドのargs
	 * @param setFlg
	 *            parse()メソッドのsetFlg
	 */
	private void byteVal(byte[] val, int[] args, boolean setFlag) {
		if (args[14] >= 0) {
			if (setFlag) {
				val[_args[args[14]]] = (byte) args[args[15]--];
			} else {
				args[++args[15]] = val[_args[args[14]]];
			}
		} else {
			if (setFlag) {
				val[args[args[15]--]] = (byte) args[args[15]--];
			} else {
				args[args[15]] = val[args[args[15]]];
			}
		}
	}

	/**
	 * 全体攻撃用フェッチ関数 _args[4]に敵もしくは味方の先頭の隊列番号を入力する
	 * フェッチ関数を呼び出すたびに、_args[4]に次の隊列番号が入力される 敵もしくは味方の最終隊列番号にさしかかると、_args[4]は-1を返す
	 */
	private void fetch() {
		// _args[4]の値が消されないようにキャップ
		_args[5] = 0;
		// キャラクターが存在する場所まで走査
		for (; _args[4] < 16; _args[4]++) {
			if (members[_args[4]] >= 0)
				break;
		}

		// 現在のカーソルを対象者に設定
		_args[3] = _args[4];
		// 味方の場合idに変換
		if (_args[4] < 4)
			_args[3] = members[_args[4]];

		// 次の対象の場所を_args[4]に設定
		for (_args[4]++; _args[4] < 17; _args[4]++) {
			if (_args[4] == 4 || _args[4] >= 16) {
				// 敵か味方の終了地点までフェッチしたら負の数を設定
				_args[4] = -1;
				break;
			}
			if (members[_args[4]] >= 0) {
				break;
			}
		}
	}

	/**
	 * 行動終了関数 (1) 魔法を使っていた場合、MPを消費する (2) アイテムを使っていた場合、アイテムを消費する
	 * (3)行動者イニシアチブを-1にする (4) 行動トランザクションをリセットする (5) 味方が死んだ場合、死んだ隊列を繰り上げる (6)
	 * 選択解除を行う
	 *
	 * @param terminate
	 *            falseの場合、消費だけ計算をし、行動終了は行わない
	 */
	private void endAction(boolean terminate) {
		// 行動者なしの場合、即座に終了
		if (_args[1] < 0) {
			return;
		}

		// MP消費
		if ((_args[0] == 2) && (_args[2] < 16)) {
			mps[_args[1]] -= (_args[2] + 10) / 4;
			if (mps[_args[1]] < 0) {
				mps[_args[1]] = 0;
			}
		}

		// アイテム消費
		if ((_args[0] == 3) && ((iType[_args[2]] & 1024) != 0)
				&& (iAmount[_args[2]] > 0)) {
			if (--iAmount[_args[2]] < 0) {
				iAmount[_args[2]] = 0;
			}

		}

		// 消費だけ計算する場合、ここで終了
		if (!terminate)
			return;

		// イニシアチブの終了
		int mem = id2mem(_args[1]);
		if (mem < 0)
			mem = 5; // トゥンブクトゥの遊び終了

		initiatives[mem] -= 100;
		if (initiatives[mem] < 0) {
			// 行動終了

			// 戦闘評価フェーズ optFlg12...15を間借りし、4で割った余りが
			// 奇数の箇所があればそこを偶数にする
			try {
				if (0 != (getAlign(mem2id(_args[1])) & 256)) {
					for (int i = 12; i < 16; i++) {
						optFlg[i] += (optFlg[i] & 4);
					}
				}
			} catch (Exception e) {
			}

			// 全行動をリセット
			for (int k = 0; k < _args.length; k++) {
				_args[k] = -1;
			}
		}

		// 味方が死んだ場合の隊列繰上げ
		int ii = 3;
		while (ii > 0) {
			if ((members[ii] >= 0) && (members[ii - 1] < 0)) {
				initiatives[ii - 1] = initiatives[ii];
				members[ii - 1] = members[ii];
				initiatives[ii] = -1;
				members[ii] = -1;
				ii = 3;
			} else {
				ii--;
			}
		}

		// 次のアタタタッの対象を選ぶ
		int atttMem = id2mem(nextAttt);
		if (atttMem >= 0) {
			initiatives[atttMem] = 299;
		}
		nextAttt = -1;

		_sel = 0;
	}

	/* ========================= */
	// I/Oユーティリティ
	/* ========================= */

	private void loadMaster() {
		DataInputStream dis = null;
		DataOutputStream dos = null;
		try {

			if(!r$getVer()){
				return;
			}

			// リソースファイルをjar から展開
			r$open();
			dis = r$load("init.sav");

			byte[] saveData = new byte[SAVE_SIZE];
			dis.read(saveData);
			dis.close();

			dos = r$saveSlot(4 + RES_SIZE);

			dos.write(saveData);
			_load = true;
			r$closeSave(dos);

			// 画像読みこみ
			loadImage();

			dis = r$load("members.data");
			// キャラデータの分だけマスタ情報を読み取る
			for (int i = 0; i < 6; i++) {
				names[i] = dis.readUTF();
				hpis[i] = dis.readByte();
				hpds[i] = dis.readByte();
				mpis[i] = dis.readByte();
				mpds[i] = dis.readByte();
				atis[i] = dis.readByte();
				atds[i] = dis.readByte();
				dexs[i] = dis.readByte();
				xps[i] = dis.readByte();
				icns[i] = dis.readByte();
			}
			dis.close();
			dis = r$load("magics.str");
			// 魔法データの分だけマスタ情報を読み取る
			for (int i = 0; i < mgcs.length; i++) {
				mgcs[i] = dis.readUTF();
			}
			dis.close();
			dis = r$load("mevents.str");
			// 魔法および敵能力データの分だけマスタ情報を読み取る
			// for (int i = 0; i < mgcEvents.length; i++) {
			for (int i = 0; i < MEVENTS_SIZE; i++) {
				mgcEvents[i] = dis.readUTF();
			}
			dis.close();
			// メニューデータの分だけマスタ情報を読み取る
			dis = r$load("menuevents.str");
			for (int i = 0; i < menuEvents.length; i++) {
				menuEvents[i] = dis.readUTF();
			}
			dis.close();

			// アイテムデータ分だけマスタ情報を読み取る
			dis = r$load("items.data");
			for (int i = 0; i < ITEM_LEN; i++) {
				iName[i] = dis.readUTF();
				iType[i] = dis.readInt();
				iValue[i] = dis.readInt();
				iCost[i] = dis.readInt();
				iAlign[i] = dis.readInt();
				iEvents[i] = dis.readByte();
			}
			dis.close();

			// 宝の地図データ分だけマスタ情報を読み取る
			dis = r$load("maptexts.str");
			for (int i = 0; i < 20; i++) {
				mapTexts[i] = dis.readUTF();
			}
			dis.close();

			// 筏の座標分だけマスタ情報を読み取る
			dis = r$load("raft.data");
			int dataLen = dis.readByte();
			raftTable = new byte[dataLen][4];
			for (int i = 0; i < dataLen; i++) {
				for (int j = 0; j < 4; j++) {
					raftTable[i][j] = dis.readByte();
				}
			}
			dis.close();

			// アイテム・敵コンプリートフラグ読み込み
			// リソース最大値 - 20(long2個分+最終保存スロット)の位置から読み込む
			dis = r$loadSlot(RES_SIZE - 20);
			completeItem = dis.readLong();
			completeMonster = dis.readLong();
			slotIndex = dis.readInt();

			_load = true; // マスタダウンロード済みとする
		} catch (Exception e) {
			System.out.println(e);
		} finally {
			Runtime.getRuntime().gc();
			try {
				dis.close();
				r$close();
			} catch (Exception e) {
				System.out.println(e);
			}
		}
	}

	/**
	 * jarアーカイブから画像を抽出し、必要があればフィルターをかまします
	 *
	 * @return
	 * @throws Exception
	 */
	protected void loadImage() throws Exception {
		DataInputStream dis;
		// 画像データをjarから取得
		dis = r$load("chips.gif");
		int dataSize = dis.available();
		byte[] imageData = new byte[dataSize];
		dis.read(imageData);
		dis.close();

		// 画像フィルタが必要なのであれば、ここで設定
		if (imageFilter == 1) {
			imageData[0x16] = (byte) 0xc0;
			imageData[0x17] = (byte) 0xc0;
			imageData[0x18] = (byte) 0xc0;
			imageData[0x19] = (byte) 0xff;
			imageData[0x1a] = (byte) 0xff;
			imageData[0x1b] = (byte) 0xff;
		}

		g$createImage(imageData);
	}

	/**
	 * 新規地図を読み込む
	 *
	 * @param isl
	 *            島番号
	 * @param rgn
	 *            エリア番号
	 */
	private void loadMap(int isl, int rgn) {
		DataInputStream dis = null;

		try {
			// 地図データを読み込み
			String mapNo = isl + "" + rgn;

			// データを保持しているJarファイル
			r$open();

			// 可変データのデータ量(使いまわし)
			int dataLen = 0;

			// Jarファイルからデータを読み出すobject(使いまわし)
			// 敵出現スクリプトを含む地図イベントを読み込み
			dis = r$load(mapNo + "mapEvent.str");

			dataLen = dis.readByte();
			mapEvent = new String[dataLen];
			for (int i = 0; i < dataLen; i++) {
				mapEvent[i] = dis.readUTF();
			}
			dis.close();

			// エンディングシーンやおまけシーン時、イベントデータ読み込みのみで終了する
			if (isl == 0) {
				saveList = null;
				return;
			}

			// 地図情報を読み込み
			dis = r$load(mapNo + ".map");
			for (int y = 0; y < MAP; y++) {
				for (int x = 0; x < MAP; x++) {
					xyMap[y][x] = dis.readByte();
				}
			}
			dis.close();

			// 5-1だけ画像フィルタを行う
			int filter = 0;
			if (isl == 5 && rgn == 1) {
				filter = 1;
			}
			if (filter != imageFilter) {
				imageFilter = filter;
				loadImage();
			}
			// 自動生成マップクリア
			submaplens = null;
			submaps = null;
			// 5-2だけ自動生成マップを読み込む
			if (isl == 5 && rgn == 2) {
				// 自動生成マップ開放度一覧読み込み
				dis = r$load("pack.submaplen");
				int submaplenlen = dis.readByte();
				submaplens = new byte[submaplenlen];
				for (int i = 0; i < submaplenlen; i++) {
					submaplens[i] = dis.readByte();
				}
				dis.close();

				// 自動生成マップ一覧読み込み
				dis = r$load("pack.submap");
				submaps = new byte[submaplenlen][8];
				for (int i = 0; i < submaplenlen; i++) {
					for (int j = 0; j < 8; j++) {
						submaps[i][j] = dis.readByte();
					}
				}
				dis.close();
			}

			dis = r$load(mapNo + ".chip");
			dataLen = dis.readByte();

			g$setMapChip(dataLen * -1);
			// mapChips = new Image[dataLen]; // チップデータ作成
			upChip = new byte[dataLen]; // 上チップ配列作成
			walkIn = new boolean[dataLen]; // 進入可否配列生成
			eventNo = new byte[dataLen]; // 地図イベント配列生成

			for (int i = 0; i < dataLen; i++) {
				g$setMapChip(i);
				// mapChips[i] = new BufferedImage(CHIP, CHIP,
				// BufferedImage.TYPE_INT_RGB);
				setChipImage(i, dis.readByte(), dis.readByte(), dis.readByte(),
						dis.readByte());
				walkIn[i] = dis.readBoolean();
				eventNo[i] = dis.readByte();
			}
			dis.close();

			// 敵用魔法イベントを読み込み
			dis = r$load(mapNo + "mevents.str");

			dataLen = dis.readByte();
			for (int i = 0; i < dataLen; i++) {
				mgcEvents[MEVENTS_SIZE + i] = dis.readUTF();
			}
			dis.close();

			// 敵出現パターン読み込み
			dis = r$load(mapNo + ".eptn");

			for (int i = 0; i < 7; i++) {
				for (int j = 0; j < 16; j++) {
					enemyPattern[i][j] = dis.readByte();
				}
			}
			dis.close();

			// 敵データ読み込み
			dis = r$load(mapNo + ".enm");

			int dataLen2 = dis.readByte();
			for (int i = 0; i < dataLen2; i++) {
				enemyID[i] = dis.readByte();
				for (int j = 0; j < 4; j++) {
					enemyAlgo[i][j] = dis.readByte();
				}
			}
			dis.close();

			// 敵マスタとデータを照合
			dis = r$load("master.enemy");

			dataLen = dis.readByte();
			byte eid = 0;
			String name = "";
			int hp = 0;
			byte mp = 0;
			byte at = 0;
			byte df = 0;
			byte dx = 0;
			int al = 0;
			byte xp = 0;
			byte drop = 0;
			byte icon = 0;
			for (int j = 0; j < dataLen; j++) {
				eid = dis.readByte();
				name = dis.readUTF();
				hp = dis.readInt();
				mp = dis.readByte();
				at = dis.readByte();
				df = dis.readByte();
				dx = dis.readByte();
				al = dis.readInt();
				xp = dis.readByte();
				drop = dis.readByte();
				icon = dis.readByte();
				for (int i = 0; i < dataLen2; i++) {
					if (eid == enemyID[i]) {
						names[i + 6] = name;
						atis[i + 6] = at;
						dfes[i] = df;
						dexs[i + 6] = dx;
						align[i] = al;
						xps[i + 6] = xp;
						enemyDrop[i] = drop;
						icns[i + 6] = icon;
						hpis[i + 6] = hp;
						hps[i + 6] = hp;
						mpis[i + 6] = mp;
						mps[i + 6] = mp;
					}
				}
			}
			dis.close();

			// スポットイベント読み込み
			dis = r$load(mapNo + ".spot");

			dataLen = dis.readByte();
			spotNo = new byte[dataLen][3];
			for (int i = 0; i < dataLen; i++) {
				spotNo[i][0] = dis.readByte();
				spotNo[i][1] = dis.readByte();
				spotNo[i][2] = dis.readByte();
			}

			// マップ変化フラグの初期化
			clearMapFlg();

			// 汎用フラグの初期化(オプション以降、テレポートフラグ以前のみ)
			for (int i = M_OPT; i < M_TELEPORT; i++) {
				mem[i] = -1;
			}

			// 地図の初期スクリプト読み込み
			parsedEvent = mapEvent;
			_args[0] = 0;
			reparse();
			_args[0] = -1;

			debug();
			// System.out.println(Runtime.getRuntime().freeMemory());
		} catch (Exception e) {
			System.out.println(e);
		} finally {
			Runtime.getRuntime().gc();
			try {
				r$close();
				dis.close();
			} catch (Exception e) {
				System.out.println(e);
			}
		}
	}

	/**
	 * チップ情報をもとにチップのImageデータを作成する
	 *
	 * @param index
	 *            チップを設定する配列の項番
	 * @param base
	 *            背景色の項番
	 * @param half
	 *            斜め背景の方向と色の項番
	 * @param obj
	 *            主人公の下に描画される画像
	 * @param upObj
	 *            主人公の上に描画される画像
	 */
	private void setChipImage(int index, byte base, byte half, byte obj,
			byte upObj) {
		Object g = g$draw(null, G_SHIFT_BUFFER, index, 0, 0, null);
		// 基本色描画
		g$draw(g, G_SET_COLOR, base, 0, 0, null);
		g$draw(g, G_FILL_RECT, 0, 0, CHIP, null);

		// 斜めチップ描画
		if (half != 0) {
			g$draw(g, G_SET_COLOR, half % 16, 0, 0, null);
			g$draw(g, G_FILL_POLIGON, half / 16, 0, 0, null);
		}
		// チップオブジェクト描画
		if (obj != 0) {
			g$draw(g, G_DRAW_CHIP, obj, 0, 0, null);
		}

		// 上チップオブジェクト描画
		if (upObj != 0) {
			g$draw(g, G_DRAW_CHIP, upObj, 0, 0, null);
			upChip[index] = upObj;
		}

	}

	/**
	 * ゲームの状態セーブ
	 *
	 */
	private int saveGame(int slot) {
		try {
			DataOutputStream dos;

			// 新規スロットの場合、スロットサイズを更新
			int slotCount = 0;
			if (slot == 0) {
				DataInputStream dis = r$loadSlot(RES_SIZE);
				slotCount = dis.readInt();
				slotCount++;
				dis.close();
				slot = slotCount;
			}

			dos = r$saveSlot(4 + slot * SAVE_SIZE + RES_SIZE);

			int size = 0;
			// 現在の時間の保存
			long now = System.currentTimeMillis();
			dos.writeLong(now);
			size += 8;

			// 現在座標の保存1
			dos.writeByte((byte) mem[M_ISL]); // 島番号の保存
			dos.writeByte((byte) mem[M_RGN]); // 地区番号の保存
			size += 1;
			size += 1;

			// プレイ時間の保存
			gameTime += now - startTime;
			dos.writeLong(gameTime);
			size += 8;

			// 主人公キャラの状態保持
			for (int i = 0; i < 6; i++) {
				dos.writeInt(lvs[i]); // 現在Lvの保存
				dos.writeInt(hps[i]); // 現在HPの保存
				dos.writeInt(mps[i]); // 現在MPの保存
				for (int j = 0; j < 4; j++) {
					dos.writeByte((byte) eqs[i][j]); // 現在装備品の保存
				}
				size += 4;
				size += 4;
				size += 4;
				size += 4;
			}

			dos.writeInt(exp); // 現在経験点の保存
			dos.writeInt(gem); // 現在所持金の保存
			size += 4;
			size += 4;

			// 隊列情報の保存
			for (int i = 0; i < 4; i++) {
				dos.writeByte((byte) members[i]);
				size += 1;
			}

			// 現在座標の保存2
			dos.writeByte((byte) mem[M_X]); // x座標の保存
			dos.writeByte((byte) mem[M_Y]); // y座標の保存
			size += 1;
			size += 1;

			// 筏履歴の保存
			for (int i = 0; i < raftHist.length; i++) {
				dos.writeByte(raftHist[i]);
				size += 1;
			}

			// 各種フラグの保存
			// イベントフラグの保存(256+1024bitあるので、8つのIntに分割
			for (int i = 0; i < 40; i++) {
				dos.writeInt(mapBits(eventFlg, i));
				size += 4;
			}

			// アイテム保持数の保存
			for (int i = 0; i < ITEM_LEN; i++) {
				dos.writeInt(iAmount[i]);
				size += 4;
			}

			System.out.println("SAVE_SIZE=" + size);
			r$closeSave(dos);

			// new data
			if(slotCount > 0){
				dos = r$saveSlot(RES_SIZE);
				dos.writeInt(slotCount);
				r$closeSave(dos);
			}

			// 初回起動時刻更新
			startTime = now;

			// SaveSlot番号保持
			slotIndex = slot;
			dos = r$saveSlot(RES_SIZE - 4);
			dos.writeInt(slotIndex);
			r$closeSave(dos);

			// セーブ情報を変更するため、セーブスロット一覧を更新
			saveList = null;

			return 1;

		} catch (Exception e) {
			System.out.println(e);
		}
		return 0;
	}

	/**
	 * boolean配列をバイト型に変換して返す。SAVEの時に使用
	 *
	 * @param bits
	 *            boolean配列
	 * @param pos
	 *            配列読み込み開始位置。32bitごとに指定
	 * @return 変換されたバイト型の値
	 */
	private int mapBits(boolean[] bits, int pos) {
		int result = 0;
		for (int i = 0; i < 32; i++) {
			if ((i + pos * 32) >= bits.length) {
				break;
			}
			if (bits[i + pos * 32]) {
				result |= 1 << i;
			}
		}
		return result;
	}

	private void loadGame(int slot) {
		try {
			DataInputStream dis = r$loadSlot(4 + slot * SAVE_SIZE + RES_SIZE);

			// 保存時刻の読み取り(捨て)
			dis.readLong();

			// 現在座標の読込
			mem[M_ISL] = dis.readByte(); // 島番号の読込
			mem[M_RGN] = dis.readByte(); // 地区番号の読込

			// プレイ時間の読込
			gameTime = dis.readLong();

			// 主人公キャラの状態取得
			for (int i = 0; i < 6; i++) {
				lvs[i] = dis.readInt(); // 現在Lvの読込
				hps[i] = dis.readInt(); // 現在HPの読込
				mps[i] = dis.readInt(); // 現在MPの読込
				for (int j = 0; j < 4; j++) {
					eqs[i][j] = dis.readByte(); // 現在装備品の読込
				}
			}

			exp = dis.readInt(); // 現在経験点の読込
			gem = dis.readInt(); // 現在所持金の読込

			// 隊列情報の読込
			for (int i = 0; i < 4; i++) {
				members[i] = dis.readByte();
			}

			// 現在座標の読込
			mem[M_X] = dis.readByte(); // x座標の読込
			mem[M_Y] = dis.readByte(); // y座標の読込

			// 筏移動履歴の読込
			for (int i = 0; i < raftHist.length; i++) {
				raftHist[i] = dis.readByte();
			}

			// マップの読み込みのあとにマップフラグを指定するため、
			// この位置でloadMapを実行
			loadMap(mem[M_ISL], mem[M_RGN]);

			// 各種フラグの読込
			for (int i = 0; i < 40; i++) {
				// イベントフラグの読込
				unmapBits(eventFlg, dis.readInt(), i);
			}

			// アイテム保持数の読込
			for (int i = 0; i < ITEM_LEN; i++) {
				iAmount[i] = dis.readInt();
			}

			dis.close();

			// 主人公の向きを南向きにする
			mem[M_QUOTA] = 3;

			// ゲーム開始時刻の更新
			startTime = System.currentTimeMillis();

			// SaveSlot番号保持
			slotIndex = slot;
			saveList = null;

			if (slot == 0)
				debug();
		} catch (Exception e) {

		}
	}

	/**
	 * boolean配列にintの値を代入する。読み込みの際に使用。
	 *
	 * @param bits
	 *            代入するboolean配列
	 * @param map
	 *            参照するint型のデータ
	 * @param pos
	 *            配列の代入開始位置。32bitごとに指定
	 */
	private void unmapBits(boolean[] bits, int map, int pos) {
		for (int i = 0; i < 32; i++) {
			if ((i + pos * 32) >= bits.length) {
				break;
			}
			bits[i + pos * 32] = ((map & (1 << i)) != 0);
		}
	}

	/**
	 * セーブスロット一覧を最新の状態に更新
	 *
	 */
	private void updateSaveList() {
		int count = 1;
		saveList = null;
		try {
			DataInputStream dis = r$loadSlot(RES_SIZE);

			// セーブスロット数の読み込み
			count += dis.readInt();

			saveList = new String[count];

			// 最初のスロットをデフォルトデータスロットとして読み飛ばし
			dis.skip(SAVE_SIZE);

			Date date = new Date();
			Calendar time = Calendar.getInstance();
			for (int i = 1; i < count; i++) {
				// 時間情報の取得
				date.setTime(dis.readLong());
				time.setTime(date);

				// 現在座標の取得
				byte isl = dis.readByte();
				byte rgn = dis.readByte();

				// プレイ時間の取得
				long gameTime = dis.readLong();

				// 現在主人公レベルの取得
				int lv = dis.readInt();

				int iGameTime = (int) gameTime / 60000;
				int gameMin = iGameTime % 60;
				int gameHour = iGameTime / 60;

				saveList[i] = time.get(Calendar.YEAR) + "/";
				saveList[i] += format((time.get(Calendar.MONTH) + 1), 2, '0')
						+ "/";
				saveList[i] += format(time.get(Calendar.DATE), 2, '0') + " ";
				saveList[i] += format(time.get(Calendar.HOUR_OF_DAY), 2, '0')
						+ ":";
				saveList[i] += format(time.get(Calendar.MINUTE), 2, '0') + " ";
				saveList[i] += "[" + isl + "-" + rgn + "] ";
				saveList[i] += "Lv " + format(lv, 2, ' ');
				saveList[i] += " " + format(gameHour, 3, ' ');
				saveList[i] += ":" + format(gameMin, 2, '0');
				dis.skip(SAVE_SIZE - 22);
			}
			dis.close();
		} catch (Exception e) {
			System.out.println(e);
		}
		if (saveList == null) {
			saveList = new String[count];
		}
		saveList[0] = "新規データ";
	}

	/**
	 * SaveListを間借りしてモンスター一覧を作成
	 *
	 * @throws Exception
	 */
	private void updateMonsterList() throws Exception {
		r$open();
		DataInputStream dis = r$load("master.enemy");

		byte dataLen = dis.readByte();
		saveList = new String[dataLen];
		String name = "";
		StringBuffer buff = new StringBuffer();
		for (int j = 0; j < dataLen; j++) {
			dis.readByte();
			name = dis.readUTF();
			int hp = dis.readInt();
			int stat = dis.readInt();
			int al = dis.readInt();

			byte xp = dis.readByte();
			dis.readByte();
			byte icon = dis.readByte();
			int priz = xp * 256 + icon;

			// 敵データを_argsにマッピングするスクリプトをマップイベントに注入
			buff.append(hp);
			buff.append(' ');
			buff.append(stat);
			buff.append(' ');
			buff.append(al);
			buff.append(' ');
			buff.append(priz);
			buff.append(" argsMapping ");
			buff.append(mapEvent[j]);
			mapEvent[j] = buff.toString();
			buff.setLength(0);
			saveList[j] = name;
		}
		dis.close();
		r$close();
	}

	/**
	 * セーブスロットを削除
	 *
	 * @param slot
	 *            削除対象スロット(現在は全削除する)
	 */
	private void clearSave(int slot) {
		try {
			// 現在のSaveスロット数を計測
			DataInputStream dis = null;
			DataOutputStream dos = null;

			dis = r$loadSlot(RES_SIZE);
			int slotSize = dis.readInt();
			dis.close();

			// 指定Saveスロット以降をコピー
			int tailSize = slotSize - slot;
			if (tailSize > 0) {
				byte[] temp = new byte[SAVE_SIZE * tailSize];
				dis = r$loadSlot(RES_SIZE + 4 + SAVE_SIZE * (slot + 1));
				dis.read(temp);
				dis.close();

				dos = r$saveSlot(RES_SIZE + 4 + SAVE_SIZE * slot);
				dos.write(temp);
				r$closeSave(dos);
			}

			// Saveスロット数1減少
			dos = r$saveSlot(RES_SIZE);
			dos.writeInt(slotSize - 1);
			r$closeSave(dos);

			// セーブ情報を変更するため、セーブスロット一覧を更新
			saveList = null;
		} catch (Exception e) {
		}
	}

	/**
	 * アイテム図鑑・モンスター図鑑フラグにクリア時のデータを追加
	 */
	private void completed() {
		// 現在1つ以上所持しているアイテムをアイテム図鑑フラグに追加
		for (int i = 0; i < 64; i++) {
			if (getAmount(i) > 0) {
				completeItem |= (1 << i);
			}
		}

		// 一回でも倒したモンスターをモンスター図鑑フラグに追加
		for (int i = 0; i < 64; i++) {
			if (eventFlg[F_MONSTER + i]) {
				completeMonster |= (1 << i);
			}
		}

		// 各種図鑑フラグをセーブデータに書き込み
		DataOutputStream dos = null;
		try {
			dos = r$saveSlot(RES_SIZE - 20);
			dos.writeLong(completeItem);
			dos.writeLong(completeMonster);
		} catch (Exception e) {
		} finally {
			try {
				r$closeSave(dos);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 宝箱回収率の表示 指定したフラグからフラグまでの間でいくつtrueがあったかを調査し、割合を表示
	 *
	 * @param begin
	 *            宝箱フラグ開始位置
	 * @param end
	 *            宝箱フラグ終了位置
	 */
	private void chestCompleted(int begin, int end) {
		int got = 0;
		int all = 0;
		for (int i = begin; i <= end; i++) {
			all++;
			if (eventFlg[i]) {
				got++;
			}
		}
		int rate = (got * 10000) / all;
		lines[lindex++] = "宝箱回収率 " + (rate / 100) + "."
				+ format(rate % 100, 2, '0') + "% (" + got + "/" + all + ")";
	}

	/**
	 * 戦闘などで特殊イベントがある場合のイベントIDをハードコーディングで返す
	 *
	 * @return
	 */
	private int specialEvent() {
		// バレッタを倒した時、イベントID41を返す
		try {
			if (enemyID[mem2id(_args[3]) - 6] == 13) {
				return 41;
			}
		} catch (Exception e) {
		}
		return -1;
	}

	/* ========================= */
	// シーン管理ユーティリティ
	/* ========================= */

	private void setScene(int scene) {
		_scene = scene;

		if (scene == 0) {
			if (mem[M_RGN] > 0) {
				// エンディングシーン
				setSoftLabel(SOFT_KEY_1, "");
				setSoftLabel(SOFT_KEY_2, "");

				loadMap(mem[M_ISL], mem[M_RGN]);
				parsedEvent = mapEvent;
			} else {
				// オープニングシーン
				setSoftLabel(SOFT_KEY_1, "戻る");
				setSoftLabel(SOFT_KEY_2, "D/L");

				// パース対象イベントをオープニング独自のものに設定
				parsedEvent = menuEvents;
			}
			saveList = null;

			clearLines();
			for (int i = 0; i < _args.length; i++) {
				_args[i] = -1;
			}
			if (_load) {
				reparse();
			} else {
				lines[0] = "5つの宝島";
				lines[1] = "初回ダウンロードされていません";
				lines[2] = "SOFTKEY2を押して下さい";
			}

			// 背景画像キャッシュを破棄
			useCache = false;
		}

		if (scene == 1) {
			// フィールドシーン
			setSoftLabel(SOFT_KEY_1, "MENU");
			setSoftLabel(SOFT_KEY_2, "");
			int i = 0;
			// メニュー用グローバル引数のクリア
			for (i = 0; i < _args.length; i++) {
				_args[i] = -1;
			}
			// 敵番号のクリア
			for (i = 6; i < members.length; i++) {
				members[i] = -1;
			}
			// イニシアティブ・攻撃・防御力上昇のクリア
			for (i = 0; i < initiatives.length; i++) {
				initiatives[i] = -1;
				optAt[i] = 0;
				optDf[i] = 0;
				optFlg[i] = 0;
			}
			// 主人公周辺オブジェクトのクリア
			for (i = M_OPT; i < M_OPT + 5; i++) {
				mem[i] = 0;
			}
			// 向きの強制変更のクリア
			mem[M_QUOTA_FORCE] = -1;
			// 自由描画をクリア
			for (i = 0; i < freeImage.length; i++) {
				freeImage[i] = 0;
			}

			// 経験点・宝物のクリア
			winXp = 0;
			winGem = 0;
			winItem = 0;
			winScript = -1;
			// 魔法の封印の解除
			eventFlg[F_MAGIC + 6] = false;
			// 死亡キャラクターはHP1で復活
			for (i = 0; i < 6; i++) {
				if (hps[i] <= 0)
					hps[i] = 1;
			}
			// 主人公がいない場合、主人公をHP1で復活させる
			if (!inMember(0, members, 0)) {
				for (i = 0; i < 4; i++) {
					if (members[i] < 0) {
						members[i] = 0;
						break;
					}
				}
			}
			// パース対象イベントをmapEventに変更
			parsedEvent = mapEvent;
		}

		if (scene == 2) {
			// 通常メニューシーン
			setSoftLabel(SOFT_KEY_1, "戻る");
			setSoftLabel(SOFT_KEY_2, "");
			// パース対象イベントをmenuEventに変更
			parsedEvent = menuEvents;
			reparse();
		}

		if (scene == 3) {
			// 戦闘シーン
			setSoftLabel(SOFT_KEY_1, "戻る");
			setSoftLabel(SOFT_KEY_2, "");
			// パース対象イベントをmenuEventに変更
			parsedEvent = menuEvents;
			lines[lindex++] = "敵が現れた!";
			// ホイッスルを装備しているメンバーは最初に行動
			for (int i = 0; i < 4; i++) {
				if (members[i] == -1) {
					break;
				}
				if (eqs[members[i]][3] == 22) {
					initiatives[i] = 64;
					break;
				}
			}
		}

		if (scene == 4) {
			// トークシーン
			setSoftLabel(SOFT_KEY_1, "");
			setSoftLabel(SOFT_KEY_2, "");
			// パース対象イベントをmapEventに変更
			parsedEvent = mapEvent;
		}

		if (scene == 5) {
			// 筏シーン
			setSoftLabel(SOFT_KEY_1, "戻る");
			setSoftLabel(SOFT_KEY_2, "");
			parsedEvent = null;
			for (int i = 0; i < 4; i++) {
				_args[i + 1] = mem[i];
			}
		}

		if (scene == 6) {
			// デモシーン
			setSoftLabel(SOFT_KEY_1, "");
			setSoftLabel(SOFT_KEY_2, "");

			// 背景画像キャッシュを破棄
			useCache = false;
		}

		if (scene == 7) {
			// おまけシーン
			setSoftLabel(SOFT_KEY_1, "戻る");
			setSoftLabel(SOFT_KEY_2, "");

			saveList = null;
			clearLines();
			// パース対象イベントをmenuEventに変更
			parsedEvent = menuEvents;

			// 背景画像キャッシュを破棄
			useCache = false;
		}
	}

	private boolean isLine(int line) {
		if (line < 1)
			return false;
		if (line >= W_ROW)
			return false;
		if (lines[line].equals(""))
			return false;
		return true;
	}

	/**
	 * 表示行をすべて空欄にする 更に選択結果の値もすべてデフォルト値にする また、自由描画もクリアする
	 */
	private void clearLines() {
		for (int i = 0; i < W_ROW; i++) {
			lines[i] = "";
			selLines[i] = -1;
		}
		for (int i = 0; i < freeImage.length; i++) {
			freeImage[i] = 0;
		}
	}

	/**
	 * 全マップフラグを消去する
	 *
	 */
	private void clearMapFlg() {
		for (int i = 0; i < MAP * MAP; i++) {
			eventFlg[F_MAP + i] = false;
		}
	}

	/**
	 * 左:0 上:1 右:2 下:3 という数字からX軸、Y軸の値を取り出す
	 * X軸を取り出す場合、左を向いていると-1、右を向いていると1、上下を向いていると0
	 * Y軸を取り出す場合、上を向いていると-1、下を向いていると1、左右を向いていると0
	 *
	 * @param quota
	 *            主人公の向いている向き
	 * @param xy
	 *            0の時はX軸、-1の時はY軸を取り出す
	 * @return -1..1までの値を返す
	 */
	private int calcXY(int quota, int xy) {
		return (quota + xy + 1 & 1) * (quota + xy - 1);
	}

	/**
	 * 引数に指定された整数 - 1を上限とした乱数を返す random.nextInt(int)のない環境向け
	 *
	 * @param limit
	 *            乱数の上限
	 * @return 引数を上限とする乱数を整数で返す
	 */
	private int randi(int limit) {
		return (random.nextInt() & Integer.MAX_VALUE) % limit;
	}

	/**
	 * xとyが地図の範囲内かどうかを返す
	 *
	 * @param x
	 *            x座標の値
	 * @param y
	 *            y座標の値
	 * @return 地図の範囲内かどうか
	 */
	private boolean isMap(int x, int y) {
		return ((0 <= x) && (x < MAP) && (0 <= y) && (y < MAP));
	}

	/* ========================= */
	// 描画用ユーティリティ
	/* ========================= */
	public void paintAll(Object g) {
		if (((_scene == 5) && (_args[0] == -2)) || _scene == 7) {
			// 漂流中
			paintDrifting();
			showWindow = false;
		}

		// シーン1・6でウィンドウが描画されていれば、ウィンドウを消す
		// シーン1・6以外でウィンドウが描画されていなければ、描画
		if (showWindow) {
			if ((_scene == 1) || (_scene == 6)) {
				useCache = false;
				showWindow = false;
			}
		} else {
			if ((_scene != 1) && (_scene != 6)) {
				showWindow = true;
				if (_scene != 0) {
					paintWindowSet();
				}
			}
		}

		if (_scene == 0) {
			// オープニングシーン・エンディングシーンはデモ画面を描画
			paintDemo(g);
		} else if (_scene == 7) {
			g$draw(g, G_FLIP_BUFFER, 0, 0, 0, null);
		} else {
			paintField(g);
		}

		if (_scene == 0 || _scene == 7) {
			// オープニング・エンディング中なら何も描画しない
		} else if (_scene == 3) {
			// 戦闘中なら、敵キャラアイコンを描画
			// g$draw(g, G_TRANSLATE, 1, fontH * W_ROW + 2 + W_MARGIN, 0, null);
			for (int i = 6; i < 16; i++) {
				if (members[i] >= 0) {
					paintEnemy(g, i);
				}
			}
		} else {
			// 戦闘中でなければ、主人公を描画
			paintMan(g);
		}

		// 虹描画フラグがあれば、虹を描画
		if (mem[M_RAINBOW] > 0) {
			paintRainbow(g);
		}

		if (showWindow) {
			// メインウィンドウ文字の描画
			g$draw(g, G_TRANSLATE, W_MARGIN, W_MARGIN, 0, null);
			paintLines(g);

			// サブウィンドウ文字の描画
			g$draw(g, G_TRANSLATE, W_MARGIN, getHeight() - W_MARGIN - W_SUB_ROW
					* fontH, 0, null);
			if (_scene == 0) {
				// オープニング・エンディングはサブウィンドウを描画しない
			} else if (_scene == 5) {
				// 筏シーンの時は座標表示
				paintRaftSelect(g);
			} else if (_scene == 7) {
				if (_args[1] == 2) {
					paintItemDatum(g, _args[2]);
				} else if (_args[1] == 3) {
					paintMonsterDatum(g, _args[2]);
				}
			} else if (_scene != 2) {
				paintCharData(g, members, 0);
			} else if (subWSts == 0) {
				paintCharData(g, members, 0);
			} else if (subWSts == 2) {
				paintCharData(g, _args, 1);
			} else if (_args[1] >= 0) {
				paintCharDatum(g, _args[1]);
			} else if (_args[0] >= 0) {
				if (safeArray(selLines, _sel, -1) >= 0) {
					paintCharDatum(g, selLines[_sel]);
				} else {
					paintCharData(g, members, 0);
				}
			}
		} else {
			// ボスチップが存在するなら、ボスチップ描画
			if (enemyPattern[6][0] < 0) {
				paintBoss(g, enemyPattern[6][1], enemyPattern[6][2],
						enemyPattern[6][3]);
			}
		}

	}

	private void paintWindowSet() {
		Object g = g$draw(null, G_SHIFT_BUFFER, -1, 0, 0, null);
		// メインウィンドウ描画
		g$draw(g, G_TRANSLATE, W_MARGIN, W_MARGIN, 0, null);
		paintWindow(g, fontH * W_ROW + 2);

		// サブウィンドウ描画
		g$draw(g, G_TRANSLATE, W_MARGIN, getHeight() - W_MARGIN - W_SUB_ROW
				* fontH, 0, null);
		paintWindow(g, fontH * W_SUB_ROW + 2);
	}

	private void paintDrifting() {
		Object g = g$draw(null, G_SHIFT_BUFFER, -1, 0, 0, null);
		// 海の描画
		g$draw(g, G_SET_COLOR, 1, 0, 0, null);
		g$draw(g, G_FILL_RECT, 0, 0, getWidth(), null);
		if (_scene == 5) {
			// 筏の描画
			g$draw(g, G_DRAW_CHIP, 22, cntX, cntY, null);
		}
		if (_scene == 7) {
			if (_args[1] == 2) {
				// アイテム一覧の時は宝箱描画
				if (_args[2] < 0) {
					g$draw(g, G_DRAW_CHIP, 65, cntX, cntY, null);
				} else {
					g$draw(g, G_DRAW_CHIP, 66, cntX, cntY, null);
				}
			}

			if (_args[1] == 3) {
				// モンスター一覧の場合は檻かモンスターを描画
				if (_args[2] < 0 || 56 <= _args[2]) {
					g$draw(g, G_DRAW_CHIP, 68, cntX, cntY, null);
				} else {
					g$draw(g, G_DRAW_CHIP, _args[7] & 0xff, cntX, cntY, null);
				}
			}
		}
	}

	private void paintField(Object g) {
		int sftXY = sftX | sftY;

		if (useCache && sftXY == 0) {
			// キャッシュを使用し、しかも移動しないなら、
			// キャッシュの画像をそのまま出力するだけ
			g$draw(g, G_FLIP_BUFFER, 0, 0, 0, null);
			return;
		}

		boolean writeCache = ((sftXY & 1) == 0);
		Object gg = g;

		if (writeCache) {
			// sftXとsftYが共に偶数なら(半分だけのシフトをしていない)
			// キャッシュに書き込む
			gg = g$draw(g, G_SHIFT_BUFFER, -1, 0, 0, null);
		}

		int dx = sftX * CHIP / -2; // 横にずれるドット数
		int dy = sftY * CHIP / -2; // 縦にずれるドット数

		if (useCache) {
			// キャッシュを使用している場合、
			// キャッシュのイメージを画像に反映
			if (writeCache) {
				g$draw(gg, G_COPY_AREA, dx, dy, 0, null);
			} else {
				g$draw(g, G_FLIP_BUFFER, dx, dy, 0, null);
			}
		}

		for (int y = -chipsHeight + sftY / 2; y <= chipsHeight + sftY / 2; y++) {
			for (int x = -chipsWidth + sftX / 2; x <= chipsWidth + sftX / 2; x++) {
				if (!useCache || (y + sftY < -chipsHeight)
						|| (y + sftY > chipsHeight) || (x + sftX < -chipsWidth)
						|| (x + sftX > chipsWidth)) {
					int xx = mem[M_X] + x;
					int yy = mem[M_Y] + y;
					if (!isMap(xx, yy)) {
						xx = 0;
						yy = 0;
					}

					g$draw(gg, G_DRAW_CHIP, getXYMap(xx, yy), x * CHIP + cntX
							+ dx, y * CHIP + cntY + dy, "");
				}
			}
		}

		if (showWindow && !useCache) {
			// ウィンドウを描画するにもかかわらずキャッシュを使わない場合、
			// 画面にウィンドウを再描画する
			paintWindowSet();
		}

		if (writeCache) {
			// キャッシュに書き込む場合、
			// キャッシュの画像を画面に反映
			g$draw(g, G_FLIP_BUFFER, 0, 0, 0, null);
		}

		useCache = true;
	}

	private void paintMan(Object g) {
		// 主人公画像を書き込む
		int x = mem[M_QUOTA] * 2;
		if (moving) {
			x++;
		}
		// M_QUOTA_FORCEに向きがセットされていればその向きに強制変更
		if (mem[M_QUOTA_FORCE] >= 0) {
			x = mem[M_QUOTA_FORCE] * 2;
		}
		// 主人公チップの範囲内なら、描画
		if (0 <= x && x <= 7) {
			g$draw(g, G_DRAW_CHIP, x, cntX, cntY, null);
		}

		// 主人公周辺のオブジェクトを書き込む
		for (int i = 0; i < 4; i++) {
			int ochip = mem[M_OPT + i];
			if (ochip > 0) {
				g$draw(g, G_DRAW_CHIP, ochip, cntX + calcXY(mem[M_QUOTA], 0)
						* CHIP, cntY + calcXY(mem[M_QUOTA], -1) * CHIP, null);
			}
		}
		int ochip2 = mem[M_OPT + 4];
		if (ochip2 > 0) {
			g$draw(g, G_DRAW_CHIP, ochip2, cntX, cntY, null);
		}

		// 主人公の上にのるチップを書き込む
		if (((sftX | sftY) & 1) == 1) {
			// 半歩移動しているのなら
			paintUpChip(g, mem[M_X] + sftX, mem[M_Y] + sftY, cntX + sftX * CHIP
					/ 2, cntY + sftY * CHIP / 2);
			paintUpChip(g, mem[M_X], mem[M_Y], cntX - sftX * CHIP / 2, cntY
					- sftY * CHIP / 2);
		} else {
			// 移動していないのなら
			paintUpChip(g, mem[M_X] + sftX / 2, mem[M_Y] + sftY / 2, cntX, cntY);
		}
	}

	/**
	 * 主人公の上にのるチップを描画する
	 *
	 * @param g
	 *            描画対象
	 * @param x
	 *            マップ上の座標X
	 * @param y
	 *            マップ上の座標Y
	 * @param pntX
	 *            描画開始地点ピクセルX
	 * @param pntY
	 *            描画開始地点ピクセルY
	 */
	private void paintUpChip(Object g, int x, int y, int pntX, int pntY) {
		if (isMap(x, y)) {
			int upc = upChip[getXYMap(x, y)];
			if (upc != 0) {
				g$draw(g, G_DRAW_CHIP, upc, pntX, pntY, null);
			}
		}
	}

	/**
	 * ボス敵用チップを描画する(戦闘中およびメニュー表示中には現れない)
	 *
	 * @param g
	 *            描画対象
	 * @param chip
	 *            チップ番号
	 * @param x
	 *            マップ上の座標X
	 * @param y
	 *            マップ上の座標Y
	 */
	private void paintBoss(Object g, int chip, int x, int y) {
		if (isMap(x, y)) {
			int xx = cntX - CHIP * ((mem[M_X] - x) * 2 + sftX) / 2;
			int yy = cntY - CHIP * ((mem[M_Y] - y) * 2 + sftY) / 2;
			if (-32 < xx && xx < getWidth() && -32 < yy && yy < getHeight()) {
				g$draw(g, G_DRAW_CHIP, chip, xx, yy, null);
			}
		}
	}

	/**
	 * キャラクター一覧の情報を描画する
	 *
	 * @param g
	 *            描画対象のグラフィックオブジェクト
	 * @param list
	 *            キャラクター一覧を表す数値のリスト
	 * @param offset
	 *            キャラクター一覧リストを何番から数え始めるか
	 */
	private void paintCharData(Object g, int[] list, int offset) {

		int tabStopH = g$fontWidth(g, "トゥンブクトゥ ");
		int tabStopM = g$fontWidth(g, "トゥンブクトゥ 999/999 ");
		paintChars(g, "【名前】", 0, fontH);
		paintChars(g, "[HP]", tabStopH, fontH);
		paintChars(g, "[MP]", tabStopM, fontH);
		for (int i = 0; i < 4; i++) {
			if (list[i + offset] < 0) {
				continue;
			}
			paintChars(g, names[list[i + offset]], 0, fontH * (i + 2));
			paintChars(g, format(hps[list[i + offset]], 3, ' ') + "/"
					+ format(getMaxHp(list[i + offset]), 3, ' '), tabStopH,
					fontH * (i + 2));
			paintChars(g, format(mps[list[i + offset]], 3, ' ') + "/"
					+ format(getMaxMp(list[i + offset]), 3, ' '), tabStopM,
					fontH * (i + 2));
		}
		paintChars(g, format(gem, 10, ' ') + "[所持金]", tabStopH, fontH * 6);
	}

	/**
	 * キャラクターの詳細データを描画する
	 *
	 * @param g
	 *            描画対象のグラフィックオブジェクト
	 * @param index
	 *            キャラクター番号
	 */
	private void paintCharDatum(Object g, int index) {
		g$draw(g, G_DRAW_CHIP, icns[index], 0, 0, null);
		String exline = "";
		if (index == 0) {
			exline = "経験点  " + exp + "/" + getNextXp(index);
		} else if (getNextXp(index) >= 0) {
			// モサメデスのレベルより低い場合、所定の賃金を支払う
			exline = "必要賃金  " + getNextXp(index);
		} else {
			// モサメデスと同じレベル以上の場合、賃金支払いは不能
			exline = "支払不要";
		}
		paintChars(g, names[index] + " :Lv" + lvs[index], CHIP + 1, fontH);
		paintChars(g, "HP: " + format(hps[index], 3, ' ') + "/"
				+ format(getMaxHp(index), 3, ' '), CHIP + 1, fontH * 2);
		paintChars(g, "MP: " + format(mps[index], 3, ' ') + "/"
				+ format(getMaxMp(index), 3, ' '), CHIP + 1, fontH * 3);
		paintChars(g, "攻: " + getDefAt(index) + " 防: " + getDefDf(index)
				+ " 早: " + dexs[index], CHIP + 1, fontH * 4);
		paintChars(g, exline, CHIP + 1, fontH * 5);
	}

	/**
	 * アイテムデータの詳細を描画する
	 *
	 * @param g
	 *            描画対象のグラフィックオブジェクト
	 * @param index
	 *            アイテム番号
	 */
	private void paintItemDatum(Object g, int index) {
		StringBuffer buff = new StringBuffer();

		buff.append('【');
		if (index > 0) {
			buff.append(iName[index]);
		}
		buff.append('】');
		paintChars(g, buff.toString(), 0, fontH);
		buff.setLength(0);

		// アイテム種別に相当するラベルを表示
		buff.append('[');
		if (index > 0) {
			if (index == 48) {
				// バトルビキニは胴装備に詐称
				buff.append("胴装備");
			} else if ((iType[index] & 64) != 0) {
				buff.append(" 武器 ");
			} else if ((iType[index] & 128) != 0) {
				buff.append("　盾　");
			} else if ((iType[index] & 256) != 0) {
				buff.append("胴装備");
			} else if ((iType[index] & 512) != 0) {
				buff.append("頭装備");
			} else if ((iType[index] & 1024) != 0) {
				buff.append("消耗品");
			} else {
				buff.append("その他");
			}
		} else {
			buff.append("　　　");
		}
		buff.append(']');

		// 装備品の場合、装備できるキャラを表示
		// 装備品の場合、効果値を表示
		if (index > 0 && ((iType[index] & (64 + 128 + 256 + 512)) != 0)) {
			for (int i = 0; i < 6; i++) {
				if ((iType[index] & (1 << i)) != 0) {
					buff.append(names[i].charAt(0));
				} else if (index == 48 && i == 2) {
					// バトルビキニの場合バレッタも装備可にする
					buff.append('バ');
				} else {
					buff.append('　');
				}
			}
			buff.append(" 効果値:");
			buff.append(iValue[index]);
		}

		paintChars(g, buff.toString(), 0, fontH * 2);
		buff.setLength(0);

		buff.append("価格:");
		if (index > 0) {
			buff.append(iCost[index]);
		}
		paintChars(g, buff.toString(), 0, fontH * 3);
	}

	/**
	 * モンスターデータの詳細を描画する
	 *
	 * @param g
	 *            描画対象のグラフィックオブジェクト
	 * @param index
	 *            モンスター番号
	 */
	private void paintMonsterDatum(Object g, int index) {
		StringBuffer buff = new StringBuffer();
		if (index >= saveList.length) {
			// 未発掘モンスターの場合、ステータス詳細不明
			index = -1;
		}

		// 各種ステータス確認
		String name = "";
		String hpLine = "HP: ";
		String mpLine = "MP: ";
		String atStr = "  ";
		String dfStr = "  ";
		String dxStr = "  ";
		String exLine = "経験点: ";
		String alLine = "";
		if (index >= 0) {
			name = saveList[index];
			hpLine += _args[4];
			mpLine += (_args[5] / (256 * 256 * 256));
			atStr = format((_args[5] / (256 * 256)) & 0xff, 2, ' ');
			dfStr = format((_args[5] / 256) & 0xff, 2, ' ');
			dxStr = format(_args[5] & 0xff, 2, ' ');

			// 経験点表示
			exLine += (_args[7] / 256) & 0xff;

			// 種族表示
			exLine += " 種族:";
			if (_args[8] == 0) {
				exLine += "動物";
			} else if (_args[8] == 1) {
				exLine += "植物";
			} else if (_args[8] == 2) {
				exLine += "妖精";
			} else if (_args[8] == 3) {
				exLine += "魔獣";
			} else if (_args[8] == 4) {
				exLine += "マジックアイテム";
			} else if (_args[8] == 5) {
				exLine += "アンデッド";
			} else if (_args[8] == 6) {
				exLine += "トラップ";
			} else if (_args[8] == 7) {
				exLine += "魔族";
			} else if (_args[8] == 8) {
				exLine += "人物";
			} else if (_args[8] == 9) {
				exLine += "精霊";
			}

			// 属性表示
			if ((_args[6] & 7) == 1) {
				alLine += "炎に強い ";
			}
			if ((_args[6] & 7) == 2) {
				alLine += "炎は無効 ";
			}
			if ((_args[6] & 7) == 3) {
				alLine += "炎は回復 ";
			}
			if ((_args[6] & 7) == 4) {
				alLine += "炎に弱い ";
			}
			if ((_args[6] & 24) == 8) {
				alLine += "変化に強い ";
			}
			if ((_args[6] & 24) == 16) {
				alLine += "変化に弱い ";
			}
			if ((_args[6] & 32) == 32) {
				alLine += "成仏する ";
			}
			if ((_args[6] & 64) == 64) {
				alLine += "回避する ";
			}
			if ((_args[6] & 128) == 128) {
				alLine += "呪文無効化 ";
			}
		}
		buff.append("攻:");
		buff.append(atStr);
		buff.append(" 防:");
		buff.append(dfStr);
		buff.append(" 早:");
		buff.append(dxStr);
		String statLine = buff.toString();
		buff.setLength(0);

		paintChars(g, name, 0, fontH);
		paintChars(g, hpLine, 0, fontH * 2);
		paintChars(g, mpLine, 0, fontH * 3);
		paintChars(g, statLine, 0, fontH * 4);
		paintChars(g, exLine, 0, fontH * 5);
		paintChars(g, alLine, 0, fontH * 6);
	}

	/**
	 * 筏で別の島に行く際の座標選択
	 *
	 * @param g
	 */
	private void paintRaftSelect(Object g) {
		// ラベルの表示
		paintChars(g, " 島   AREA 東    南", 0, fontH);

		// 選択欄の表示
		StringBuffer line = new StringBuffer();
		for (int i = 1; i <= 4; i++) {
			if (i == _args[0]) {
				line.append(" [");
			} else {
				line.append("  ");
			}

			if (i <= 2) {
				// 島番号、エリア番号の場合
				line.append(String.valueOf(_args[i]));
			} else {
				// X座標、Y座標の場合
				line.append(format(_args[i], 2, ' '));
			}

			if (i == _args[0]) {
				line.append("] ");
			} else {
				line.append("  ");
			}
		}
		paintChars(g, line.toString(), 0, fontH * 2);
	}

	private void paintLines(Object g) {
		String cap = "";

		g$draw(g, G_SET_COLOR, 7, 0, 0, null);
		int lineH = 0;
		for (int i = 0; i < lines.length; i++) {
			if (lines[i].equals(""))
				break;
			if ((selectEnemy <= 0) && (i > 0) && (_sel > 0)) {
				if (i == _sel) {
					cap = "[" + i + "]";
				} else {
					cap = " " + i + " ";
				}
			}
			lineH += fontH;
			paintChars(g, cap + lines[i], 0, lineH);
		}
		if (freeImage[2] > 0 && freeImage[3] > 0) {
			// 自由描画が有効なら、自由描画
			g$draw(g, G_FREE_IMAGE, lineH + fontH, 0, 0, null);
		}
	}

	private void paintChars(Object g, String string, int x, int y) {
		g$draw(g, G_SET_COLOR, 7, 0, 0, null);
		g$draw(g, G_DRAW_STRING, x, y, 0, string);
	}

	private void paintEnemy(Object g, int index) {
		int posX = ((index - 6) % 5) * (CHIP + 3) + 1;
		int posY = ((index - 6) / 5) * (CHIP + 3) + fontH * W_ROW + 2
				+ W_MARGIN;
		g$draw(g, G_DRAW_CHIP, icns[members[index]], posX, posY, null);
		if (selectEnemy > 0) {
			if (_sel + 6 == index) {
				g$draw(g, G_SET_COLOR, 7, 0, 0, null);
				g$draw(g, 1, posX - 1, posY - 1, CHIP + 2, null);
				g$draw(g, 1, posX + 1, posY + 1, CHIP - 2, null);
				g$draw(g, 4, 0, 0, 0, null); // setColor WHITE
				g$draw(g, 1, posX, posY, CHIP, null);
			}

		}
	}

	/* ========================= */
	// Doja3.5のみで使用するグラフィック用メソッド
	// マルチプラットフォームソースでは別の内容になる
	/* ========================= */

	/**
	 * オープニング・エンディングデモ画面を描画
	 *
	 * @param g
	 *            描画対象グラフィックオブジェクト
	 */
	private void paintDemo(Object g) {
		if (!useCache) {
			Graphics gg = bg.getGraphics();
			for (int i = 0; i < 180; i++) {
				gg.setColor(Graphics.getColorOfRGB(i * 3 / 4, i, 255));
				gg.drawLine(0, i, getWidth(), i);
			}
			for (int i = 0; i < 10; i++) {
				gg.setColor(Graphics.getColorOfRGB(0, i * 10 + 155,
						i * 10 + 155));
				gg.drawLine(0, i + 180, getWidth(), i + 180);
			}

			gg.setColor(Graphics.getColorOfName(Graphics.AQUA));
			gg.fillRect(0, 190, getWidth(), getHeight() - 30);

			for (int i = 0; i < 20; i++) {
				gg.setColor(Graphics.getColorOfRGB(i * 11 + 25, 255,
						255 - i * 3));
				gg.drawLine(0, getHeight() - 30 + i, getWidth(), getHeight()
						- 30 + i);
			}

			gg.setColor(Graphics.getColorOfRGB(245, 255, 255 - 20 * 3));
			gg.fillRect(0, getHeight() - 10, getWidth(), getHeight());

			if (chips != null) {
				if (mem[M_RGN] == 1) {
					g$draw(gg, G_DRAW_CHIP, 2, getWidth() - 40,
							getHeight() - 35, null);
				} else {
					g$draw(gg, G_DRAW_CHIP, 24, getWidth() - 40,
							getHeight() - 35, null);
				}
			}

		}
		useCache = true;
		g$draw(g, G_FLIP_BUFFER, 0, 0, 0, null);
	}

	/**
	 * 半透明黒のウィンドウを描画
	 *
	 * @param g
	 *            描画対象グラフィックオブジェクト
	 * @param wHeight
	 *            ウィンドウの高さ
	 */

	protected void paintWindow(Object gg, int wHeight) {
		// ウィンドウを描画
		int[] pixels = new int[wWidth * wHeight];

		Graphics g = (Graphics) gg;
		g.getRGBPixels(0, 0, wWidth, wHeight, pixels, 0);
		for (int i = 0; i < pixels.length; i++) {
			pixels[i] = (pixels[i] >> 1) & 0x7F7F7F;
		}
		g.setRGBPixels(0, 0, wWidth, wHeight, pixels, 0);
	}

	/**
	 * 背景に虹を描画
	 *
	 * @param g
	 */
	protected void paintRainbow(Object gg) {
		Graphics g = (Graphics) gg;
		int[] colors = new int[3];

		for (int i = 0; i < 160; i++) {
			for (int c = 0; c < 3; c++) {
				int line = (c - 1) * -64 + i;
				colors[c] = line % 192;
				if (64 < line && line < 192)
					colors[c] = 128 - line;
				if (colors[c] < 0)
					colors[c] = 0;
				if (colors[c] > 32)
					colors[c] = 32;
				colors[c] *= 6;
				if (colors[c] == (64 * 3))
					--colors[c];
			}
			int[] pixels = new int[160];
			g.getRGBPixels(i + cntX - 64, 0, 1, 160, pixels, 0);
			for (int y = 0; y < 160; y++) {
				int pixel = pixels[y];
				int red = pixel / (256 * 256 * 4);
				int green = (pixel / (256 * 4)) & 63;
				int blue = (pixel / 4) & 63;
				red = red + colors[0];
				green = green + colors[1];
				blue = blue + colors[2];

				pixels[y] = (red * (256 * 256)) + (green * 256) + blue;
			}
			g.setRGBPixels(i + cntX - 64, 0, 1, 160, pixels, 0);
		}

	}

	/* ========================= */
	// iアプリ互換レイヤー
	// マルチプラットフォームソースでは呼び出し元の各メソッドを呼び出す
	/* ========================= */

	// グラフィック関連
	protected void g$createImage(byte[] imageData) throws Exception {
		// 引数がnullならキャッシュ用Imageデータの作成
		if(imageData == null){
			bg = Image.createImage(getWidth(), getHeight());
			return;
		}
		// nullでないならチップ画像の読み込み
		if (media != null) {
			media.unuse();
			media.dispose();
		}
		media = MediaManager.getImage(imageData);
		media.use();
		chips = media.getImage();
	}

	protected int g$fontWidth(Object g, String str) {
		return Font.getDefaultFont().stringWidth(str);
	}

	protected int g$fontHeight() {
		return Font.getDefaultFont().getHeight();
	}

	protected void g$setMapChip(int index) {
		if (index < 0) {
			mapChips = new Image[-index];
		} else {
			mapChips[index] = Image.createImage(CHIP, CHIP);
		}
	}

	protected Object g$draw(Object gg, int command, int a1, int a2, int a3,
			String string) {
		Graphics g = (Graphics) gg;
		if (command == FICanvas.G_DRAW_STRING) {
			g.drawString(string, a1, a2);
		}
		if (command == FICanvas.G_DRAW_RECT) {
			g.drawRect(a1, a2, a3, a3);
		}
		if (command == FICanvas.G_FILL_RECT) {
			g.fillRect(a1, a2, a3, a3);
		}
		if (command == FICanvas.G_TRANSLATE) {
			g.setOrigin(a1, a2);
		}
		if (command == FICanvas.G_SET_COLOR) {
			g.setColor(Graphics.getColorOfName(a1));
		}
		if (command == FICanvas.G_DRAW_CHIP) {
			if (chips != null) {
				if (string == null) {
					g.drawImage(chips, a2, a3, (a1 % 10) * CHIP, (a1 / 10)
							* CHIP, CHIP, CHIP);
				} else {
					g.drawImage(mapChips[a1], a2, a3);
				}
			}
		}
		if (command == FICanvas.G_FLIP_BUFFER) {
			g.drawImage(bg, a1, a2);
		}
		if (command == FICanvas.G_SHIFT_BUFFER) {
			if (a1 < 0) {
				return bg.getGraphics();
			} else {
				return mapChips[a1].getGraphics();
			}
		}
		if (command == FICanvas.G_COPY_AREA) {
			// 画像全体をずらす
			g.copyArea(0, 0, 240, 240, a1, a2);
		}
		if (command == FICanvas.G_FILL_POLIGON) {
			// 三角形描画
			int[] xs = { 0, CHIP, 0 };
			int[] ys = { 0, 0, CHIP };
			for (int pt = 0; pt < 3; pt++) {
				if (pt == (a1)) {
					xs[pt] = CHIP;
					ys[pt] = CHIP;
				}
			}
			g.fillPolygon(xs, ys, 3);
		}
		if (command == FICanvas.G_REPAINT) {
			Graphics gr = getGraphics();
			gr.lock();
			paintAll(gr);
			gr.unlock(false);
		}
		if(command == FICanvas.G_FREE_IMAGE){
			((Graphics) g).drawImage(chips, 0, a1, freeImage[0],
					freeImage[1], freeImage[2], freeImage[3]);		}
		return null;

	}

	// リソース関連
	/**
	 * 初回ダウンロード。リソースに収まりきれない画像データをサーバから ダウンロードし、スクラッチパッドに保存する
	 *
	 * @return ダウンロード成功 : true 失敗 : false
	 * @throws Exception
	 */
	protected void download() {
		HttpConnection http = null;
		DataInputStream dis = null;
		DataOutputStream dos = null;
		int process = 0;
		try {
			String host = IApplication.getCurrentApp().getSourceURL();
			http = (HttpConnection) Connector.open(host + "FI.jar",
					Connector.READ);
			http.setRequestMethod(HttpConnection.GET);
			_sel = 0;
			lines[1] = "now loading...";
			lines[2] = "";
			g$draw(null, G_REPAINT, 0, 0, 0, null);
			process = 1;
			http.connect();
			process = 2;
			g$draw(null, G_REPAINT, 0, 0, 0, null);
			process = 3;
			dis = http.openDataInputStream();
			process = 4;

			// リソースデータ書き込み。ただし、リソースバージョン(2byte)と
			// リソースデータサイズ(4byte)分の計6byteは読み飛ばす
			dos = r$saveSlot(6);
			process = 5;

			// リソースのデータサイズを取得
			resSize = (int) http.getLength();
			process = 6;

			// 1回に取得できるデータサイズ
			int available = dis.available();
			process = 7;

			// リソースデータを取得
			byte[] imgData = new byte[available];
			while (dis.read(imgData) >= 0) {
				dos.write(imgData);
			}
			process = 8;
			dis.close();
			process = 9;
			dos.close();
			process = 10;

			// ダウンロードしたリソースファイルを読み込み
			r$open();
			process = 11;

			// 再び書き込みオープン
			dos = r$saveSlot(0);
			process = 12;

			// リソースバージョンを書き込み
			dis = r$load(".ver");
			process = 13;
			int resVer = dis.readChar();
			process = 14;
			dis.close();
			dos.writeChar(resVer);
			System.out.println("size=" + resSize + ":resver=" + resVer);
			process = 15;

			// リソースデータサイズを書き込み
			dos.writeInt(resSize);
			dos.close();
			process = 16;

			// リソースの初期セーブファイルを読み取り
			dis = r$load("init.sav");
			process = 17;

			byte[] saveData = new byte[SAVE_SIZE];
			dis.read(saveData);
			dis.close();
			process = 18;

			dos = r$saveSlot(4 + RES_SIZE);
			process = 19;

			dos.write(saveData);
			r$closeSave(dos);
			process = 20;

			// ダウンロードに成功したらtrueを返却
			_load = true;
		} catch (Exception e) {
			System.out.println(e);
			lines[1] = e.toString();
			lines[2] = "process=" + process;
			lines[3] = "";
		} finally {
			try {
				http.close();
			} catch (Exception e) {
			}
			try {
				dis.close();
			} catch (Exception e) {
			}
			try {
				dos.close();
			} catch (Exception e) {
			}
			r$close();
		}
	}

	protected boolean r$getVer() throws Exception{
		// Jarデータを読み込み
		DataInputStream dis = Connector.openDataInputStream("scratchpad:///0");

		// スクラッチパッドの先頭のリソースバージョンがプログラムのバージョンに
		// 一致していなければ、初回読み込みがされていないとみなして
		// データをサーバからダウンロード
		resVersion = dis.readChar();
		if (VER != (resVersion / 100)) {
			System.out.println("version unmatch:" + VER + "."
					+ (int) resVersion);
			dis.close();
			_load = false;
			return false;
		}
		dis.close();
		return true;
	}

	protected void r$open() throws Exception {
		DataInputStream dis = null;

		// リソースサイズをまだ取得していなければ、この段階で取得
		if (resSize == 0) {
			dis = Connector.openDataInputStream("scratchpad:///0;pos=2");
			resSize = dis.readInt();
		} else {
			dis = Connector.openDataInputStream("scratchpad:///0;pos=6");
		}

		byte[] resData = new byte[resSize];
		dis.read(resData);
		dis.close();
		jar = new JarInflater(resData);
	}

	protected void r$close() {
		jar.close();
		jar = null;
	}

	protected void r$closeSave(DataOutputStream dos) throws Exception {
		dos.close();
	}

	private DataInputStream r$load(String fname) throws Exception {
		return new DataInputStream(jar.getInputStream(fname));
	}

	private DataInputStream r$loadSlot(int pos) throws Exception {
		return Connector.openDataInputStream("scratchpad:///0;pos=" + pos);
	}

	private DataOutputStream r$saveSlot(int pos) throws Exception {
		return Connector.openDataOutputStream("scratchpad:///0;pos=" + pos);
	}

	public void paint(Graphics arg0) {
		g$draw(null, G_REPAINT, 0, 0, 0, null);
	}
}
