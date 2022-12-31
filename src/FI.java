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

		// �^�C�}�ݒ�
		timer = ShortTimer.getShortTimer(c, 0, 200, true);
		timer.start();
	}

	public void resume() {
		// �ĊJ���A�^�C�}�N��
		timer.start();
	}

}

class FICanvas extends Canvas {
	/*
	 * ======== �}���`�v���b�g�t�H�[���p�ϐ� =====
	 */

	public int fontH = 0; // �t�H���g�̍���
	// public int RES_SIZE = 20; // ���\�[�X�T�C�Y
	public static final int RES_SIZE = 120000; // ���\�[�X�T�C�Y
	// �`�b�v�摜
	private Image chips;
	private Image bg;
	private MediaImage media;
	private Image[] mapChips; // �`�b�v�C���[�W
	private JarInflater jar;

	/*
	 * ======== �萔 ========
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
	 * ======== �V�X�e���ϐ� =====
	 */
	private Random random;

	protected int imageFilter; // �`�b�v�摜��ҏW����ԍ�
	// (�f�o�b�O�p) �g�[�N���\���t���O
	private boolean showToken;
	private int resSize; // ���\�[�X�T�C�Y

	/*
	 * ======== �[���ˑ��ϐ� �N�����ω����Ȃ� =====
	 */

	// �`�b�v�̒����ʒu
	private int cntX;
	private int cntY;

	private int chipsWidth; // �`�b�v�̕`�敝
	private int chipsHeight; // �`�b�v�̕`�捂��
	private int wWidth; // �E�B���h�E�̕�
	private boolean _load; // �}�X�^�f�[�^���_�E�����[�h�ς݂�

	/*
	 * ======== �}�X�^���1 �N�����ω����Ȃ� ======
	 */

	private String[] mgcs = new String[16]; // ���@��
	private String[] mgcEvents = new String[64]; // ���@�X�N���v�g
	private String[] menuEvents = new String[16]; // ���j���[�X�N���v�g
	private String[] mapTexts = new String[20]; // ��̒n�}�̓��e
	private String[] iName = new String[ITEM_LEN]; // �A�C�e����
	private int[] iType = new int[ITEM_LEN]; // �A�C�e�����
	private int[] iValue = new int[ITEM_LEN]; // �A�C�e�����ʒl
	private int[] iCost = new int[ITEM_LEN]; // �A�C�e�����i
	private int[] iAlign = new int[ITEM_LEN]; // �A�C�e������
	private byte[] iEvents = new byte[ITEM_LEN]; // �A�C�e���p�C�x���g���t�@�����X
	private int[] fireTable = { 2, 1, 0, -2, 4 }; // ���̃_���[�W���X�g
	private byte[][] raftTable; // ���ňړ��\�ȍ��W�ꗗ

	/*
	 * ======== �}�X�^���2 �n�}�ړ��ŕω����� ======
	 */

	private byte[][] xyMap = new byte[MAP][MAP]; // �n�}���
	private byte[] upChip; // ��l���̏�ɏd�Ȃ�`�b�v
	private boolean[] walkIn; // �ړ��E�s�̔���
	private byte[] eventNo; // �`�b�v�ˑ��C�x���g�ԍ�
	private byte[][] spotNo; // �X�|�b�g�C�x���g�ԍ�
	private String[] mapEvent; // �n�}�ˑ��C�x���g�X�N���v�g
	private byte[][] enemyPattern = new byte[7][16]; // �o���p�^�[��(�{�X�`�b�v�`��ɂ���p)
	private byte[][] enemyAlgo = new byte[10][4]; // �G�̐헪�A���S���Y��
	private byte[] enemyDrop = new byte[10]; // �G�����Ƃ��A�C�e��
	private byte[] enemyID = new byte[10]; // �GID(�����X�^�[�}�ӗp)
	private byte[] submaplens; // ���������}�b�v�J���l�ꗗ
	private byte[][] submaps; // ���������}�b�v�ꗗ

	/*
	 * ======== �L�����}�X�^��� �G�̏��̂ݕω����� =======
	 */

	private String[] names = new String[16]; // ���O
	private int[] hpis = new int[16]; // MAXHP
	private int[] hpds = new int[6]; // HP�㏸��
	private int[] mpis = new int[16]; // MAXMP
	private int[] mpds = new int[6]; // MP�㏸��
	private int[] atis = new int[16]; // �U����
	private int[] atds = new int[6]; // �U���͏㏸��
	private int[] dexs = new int[16]; // �f����
	private int[] xps = new int[16]; // �K�v�o���_(����)
	private int[] icns = new int[16]; // �A�C�R��
	private int[] dfes = new int[10]; // �G�h��_

	/*
	 * ======== �L�����ϐ� �Q�[������ɕω����� ======
	 */

	private int[] hps = new int[16]; // ����HP
	private int[] mps = new int[16]; // ����MP
	private int[] lvs = { 1, 1, 1, 1, 1, 1 }; // ���x��
	private int[][] eqs = new int[6][4]; // �����ꎮ
	private int exp; // ���T���f�X�̌��݌o���_
	private int gem; // ���ݏ�����
	private int[] members = new int[16]; // ���񏇔�
	private byte[] raftHist = { -1, -1, -1, -1 }; // ���̈ړ�����

	/*
	 * ======== �t�B�[���h�ϐ� �Q�[������ɕω����� ======
	 */

	// �ėpint�t�B�[���h
	private int[] mem = new int[16];

	// �`��ʒu�V�t�g
	private int sftX;
	private int sftY;

	// �t�B�[���h�p
	private boolean moving; // ���݃A�j���[�V�������쒆��

	// �V�[���Ǘ��p
	private int _scene; // �V�[���ϐ�
	private String[] lines = new String[W_ROW]; // �\���s
	private int[] selLines = new int[W_ROW]; // �I���s�߂�l
	private int lindex; // ���ݕ`��s
	private int _sel; // ���ݑI���s
	private int _page; // ���ݑI���y�[�W
	private int subWSts; // �T�u�E�B���h�E�`����e
	private int[] _args = new int[10]; // ���ݑI�𕨈ꗗ
	private int selectEnemy; // �G�I�𒆂��ǂ��� -1, 0: No 1: Yes
	private boolean showWindow; // �E�B���h�E���`�悳��Ă��邩�ǂ���
	public boolean useCache; // �t�B�[���h�`��ɃL���b�V�����g�p���Ă悢��
	private String[] parsedEvent; // �p�[�X�ΏۃC�x���g�z��
	private String[] saveList; // �Z�[�u�X���b�g�ꗗ�ێ�
	private int spotPos; // �X�|�b�g�C�x���g�̔����ꏊ��ێ�

	// �퓬�p
	private int[] initiatives = new int[16]; // �e�L�����C�j�V�A�e�B�u
	private int[] optAt = new int[16]; // �U���͏㏸
	private int[] optDf = new int[16]; // �h��͏㏸
	private int winXp; // �l���\��o���_
	private int winGem; // �l���\����z
	private int winItem; // �l���\��A�C�e��
	private int winScript = -1; // ��������s����X�N���v�g
	private int nextAttt = -1; // ���^�[���A�^�^�^�b�̑ΏۂɂȂ�L�����N�^�[
	/*
	 * �퓬�t���O ... �h�� 1, �����Ă��� 2
	 */
	private int[] optFlg = new int[16];

	/*
	 * �퓬���� ... ���͕��� 0, ���ɋ��� 1, ���͖��� 2 ���ŉ� 3 ���Ɏア 4, ��ԕω��͕��� 0, ��ԕω��ɋ��� 8,
	 * ��ԕω��Ɏア 16, �񕜖��@�Ɏア 32, ���X�U������� 64, ���������� 128, �퓬�]�����s�� 256
	 */
	private int[] align = new int[10]; // �퓬����

	// �e��t���O
	private boolean[] eventFlg = new boolean[256 + MAP * MAP]; // �Q�[�����C�x���g�p
	private int[] iAmount = new int[ITEM_LEN]; // �A�C�e��������
	public char resVersion = 0; // ���\�[�X�ǂݍ��ݗp�}�W�b�N�L�����N�^
	private long startTime; // �Q�[���J�n����
	private long gameTime; // �Q�[���v���C����
	public int[] freeImage = new int[4]; // ���R�`��̈�
	private long completeItem; // �A�C�e���R���v���[�g�t���O
	private long completeMonster; // �G�R���v���[�g�t���O
	private int slotIndex; // �O��Z�[�u�����Z�[�u�X���b�g

	public FICanvas() {

		// ���������@�̍쐬
		random = new Random();

		// �`�b�v�̒����ʒu�v�Z
		cntX = (getWidth() - CHIP) / 2;
		cntY = (getHeight() - CHIP) / 2;

		// �`�b�v�̕`�敝�E�����v�Z
		chipsWidth = cntX / CHIP + 1;
		chipsHeight = cntY / CHIP + 1;

		// �E�B���h�E�T�C�Y�̌v�Z
		wWidth = getWidth() - W_MARGIN * 2;

		// �t�H���g�̍����v�Z
		fontH = g$fontHeight();

		// �L���b�V���p�C���[�W�쐬
		try{
			g$createImage(null);
		}catch(Exception e){}

		// �\���s���N���A
		clearLines();

		// �����f�[�^�ǂݍ���
		try {
			loadMaster();
		} catch (Exception e) {
		}

		// �e�탁�����p�^�[���ݒ�
		for (int i = 0; i < members.length; i++) {
			members[i] = -1;
		}
		for (int i = 0; i < _args.length; i++) {
			_args[i] = -1;
		}

		// �I�[�v�j���O�V�[����
		setScene(0);

	}

	// �f�o�b�O�p���\�b�h
	void debug() {
		// �f�o�b�O�p�����ݒ�

	}

	public void processEvent(int type, int param) {
		// �f�o�b�O�p�Ƀg�[�N����\������?
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
		// �悭�ʉ߂��郍�W�b�N�Ȃ̂ŁA�������̂���switch���ŕ���
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
	// �V�[��0 �I�[�v�j���O
	/* ========================= */

	private void preludeEvent(int type, int param) {
		if (type == Display.TIMER_EXPIRED_EVENT) {
			return;
		}

		if (msgEvent(type, param)) {
			if (!_load) {
				// �����_�E�����[�h���ł��ĂȂ��ꍇ
			} else if (mem[M_ISL] != 0) {
				// �}�b�v�ǂݍ��݊J�n
				setScene(1);
			} else if (parsedEvent == mapEvent) {
				// �G���f�B���O�V�[��
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
							// �I�[�v�j���O�V�[�����́A�s���҂͕s��
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

		// ���\�[�X�_�E�����[�h�J�n
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
	// �V�[��1 �t�B�[���h�V�[��
	/* ========================= */

	private void fieldEvent(int type, int param) {
		// �t�B�[���h�V�[���̃C�x���g���[�h
		if (type == Display.KEY_PRESSED_EVENT) {
			if ((param == Display.KEY_SOFT1) || (param == Display.KEY_IAPP)) {
				setScene(2);
				g$draw(null, G_REPAINT, 0, 0, 0, null);
				return;
			}

			if (param == Display.KEY_SELECT) {
				// �X�|�b�g�C�x���g�̏���
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
				// �U��Ԃ�Ɗ댯�C�x���g
				// �`�b�v�ˑ��C�x���g�����̐��̏ꍇ�A�O�̕����̋L�����J�n
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
					// �㉺���E�������Ă����ꍇ
					moveAnime1();
				}

			} else {
				// �_���W�������������C�x���g�p(1)
				// ���݂̈ʒu���(16����)����قǂ܂ł̏��ƈ�����珈���J�n
				int oldPos = (mem[M_Y] / 8) * 4 + (mem[M_X] / 8);

				// ��l�����ړ����A�j���[�V�����ł���ꍇ
				boolean nomoved = moveAnime2();
				if (nomoved) {
					// �ړ����Ă��Ȃ���΁A�`�b�v�ˑ��C�x���g�͍s���Ȃ�
					return;
				}

				int event = 0;
				// �_���W�������������C�x���g�p(2)
				// ���݂̈ʒu���(16����)����قǂ܂ł̏��ƈ�����珈���J�n
				// ������5-2�����������Ȃ�
				int newPos = (mem[M_Y] / 8) * 4 + (mem[M_X] / 8);
				if (mem[M_ISL] == 5 && mem[M_RGN] == 2 && newPos != 0
						&& !eventFlg[FF_STABLE + newPos] && newPos != oldPos) {
					goFloor(newPos);
					event = 9;
				}

				// �`�b�v�ˑ��C�x���g�̂����A1..7��
				// �G�̏o���C�x���g
				if (event == 0) {
					event = eventNo[getXYMap(mem[M_X], mem[M_Y])];
				}
				// �G�o���m�F
				// �U��Ԃ�Ɗ댯�C�x���g�̏���
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

				// �`�b�v�ˑ��C�x���g��1..7�Ԃł���΁A
				// �G�o���p�^�[���ɉ����Ēn�}�C�x���g������
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
			// �퓬���͓��삵�Ȃ�
			// �Ώۂ����܂��Ă��鎞�����삵�Ȃ�

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
				// ���j���[����̋N���̏ꍇ�́A
				// �����𖞂����Ȃ��ꍇ�͓��삵�Ȃ�
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
	// �V�[��2 ���j���[�V�[��
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

			// ����z����h��
			if (index >= _args.length - 1) {
				_args[_args.length - 1] = -1;
			}
			reparse();
		}

		g$draw(null, G_REPAINT, 0, 0, 0, null);
	}

	/* ========================= */
	// �V�[��3 �퓬�V�[��
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
			// �G��I�����Ă���ꍇ�A�J�[�\���L�[�̓������ς��
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

		// �퓬�p�̉�ʑJ��
		if (transed) {
			if (selectEnemy <= 0) {
				// �G�I����ʂłȂ��Ƃ��́A�W���I���@�\���g�p
				command = selLines[_sel];
			}
			if (command == 99) {
				// �u99�v���A���Ă����Ƃ��́A��ʑJ�ڂ��s�킸��
				// �G�I�����[�h�Ɉڍs
				selectEnemy = 1;
				reparse();
				transed = false;
			}
		}

		// �G�̍s�����Ԃ̎��ɂ͏�ԑJ�ڂ��s�Ȃ�Ȃ�
		if (_args[1] >= 6) {
			parsing = true;
			transed = false;
			if (endButtle() < 0) {
				_args[0] = 10;
			}
		}

		if (transed) {
			_page = 0;
			// �G�I�����[�h�𒆗���
			selectEnemy = 0;
			int index;
			for (index = 0; index < _args.length; index++) {
				if (_args[index] < 0) {
					if (command < 0) {
						index--;
						// �퓬���́A�s���҂͕s��
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

			// �s���҂����Ȃ�(-1)�Ȃ�A���߂čs���҂�I��
			// �������A��d�\��������邽�߁A�L�[�����C�x���g���̂�
			if ((_args[1] < 0 || initiatives[id2mem(_args[1])] >= 99)
					&& type == Display.KEY_PRESSED_EVENT) {
				int endedButtle = endButtle();
				if (endedButtle == 0) {
					initButtle();
				} else {
					if (winXp + winItem + winGem == 0 && endedButtle >= 0) {
						//
						if (winScript >= 0) {
							// ������X�N���v�g���ݒ肳��Ă����
							// ������X�N���v�g�p�[�X�̂��߂�
							// ���b�Z�[�W�V�[���ֈړ�
							setScene(4);
							_args[0] = winScript;
						} else {
							// �퓬���A�G�����Ȃ��Ȃ��Ă���΃t�B�[���h�ɖ߂�
							setScene(1);
						}
					} else if (_args[0] != 11) {
						// �����܂��͔s�k�̏ꍇ
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
	// �V�[��4 ��b�V�[��
	/* ========================= */

	private void talkEvent(int type, int param) {
		if (type == Display.TIMER_EXPIRED_EVENT) {
			return;
		}

		boolean transed = msgEvent(type, param);
		if (transed) {
			if ((selLines[0] >= 0) && (_sel > 0)) {
				// ���������[�h
				_args[0] = selLines[0];
				_args[1] = selLines[_sel];
			} else {
				// �ʏ탂�[�h
				_args[0] = selLines[_sel];
			}
			if (_args[0] < 0) {
				setScene(1);
			} else {
				parse(mapEvent[_args[0]]);
			}
		}

		if (_scene != 6 && lines[0].equals("")) {
			// �f���V�[���łȂ��A���b�Z�[�W���������݂��Ȃ��Ȃ�A�t�B�[���h�V�[����
			setScene(1);
		}

		g$draw(null, G_REPAINT, 0, 0, 0, null);
	}

	/* ========================= */
	// �V�[��5 ���V�[��
	/* ========================= */

	private void raftEvent(int type, int param) {
		if (type == Display.TIMER_EXPIRED_EVENT) {
			return;
		}

		if (_args[0] <= 0) {
			if (msgEvent(type, param)) {
				int sel = selLines[_sel];
				if (sel == 98) {
					// �ړ��̏ꍇ

					boolean voyaged = false;
					// ���W����
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

					// �ړ�����
					if (voyaged) {
						for (int i = 0; i < 4; i++) {
							mem[i] = _args[i + 1];
						}
						loadMap(mem[M_ISL], mem[M_RGN]);
						setScene(1);
						g$draw(null, G_REPAINT, 0, 0, 0, null);
						return;
					}

					// �ړ����s
					_args[0] = -2;
				} else if (sel == 99) {
					// ���W���͂̏ꍇ
					_args[0] = 1;
				} else if (sel == 97) {
					// �߂�{�^���̏ꍇ
					setScene(1);
					g$draw(null, G_REPAINT, 0, 0, 0, null);
					return;
				} else if ((0 <= sel) && (sel <= raftTable.length)) {
					// ������͂̏ꍇ
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

			// ���W���̓V�[��
			if (param == Display.KEY_UP) {
				_args[_args[0]]--;
				// ���E�`�F�b�N
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
				// ���E�`�F�b�N
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
				// �I���{�^�����������Ƃ��̓��j���[�Ɉړ�
				_args[0] = 0;
				parse(mgcEvents[22]);
			}
		}

		g$draw(null, G_REPAINT, 0, 0, 0, null);
		// �Y�������畜�A
		if (_scene == 5 && _args[0] < 0)
			_args[0] = 0;
	}

	/* ========================= */
	// �V�[��6 �f���V�[��
	/* ========================= */
	private void demoEvent(int type, int param) {
		if (type != Display.TIMER_EXPIRED_EVENT) {
			return;
		}

		if (!moving) {
			// �ړ����A�j���[�V�������łȂ��ꍇ
			mem[M_QUOTA] = _args[1];
			moving = true;
			moveAnime1();
			if (_args[4] >= 0) {
				demoEvent(type, param);
			}
		} else {
			// ��l�����ړ����A�j���[�V�����ł���ꍇ
			moveAnime2();

			boolean finalFlag = false;

			if (_args[2] > 1) {
				// �ړ������v���X�̏ꍇ
				_args[2]--;
			} else if (_args[2] == 1) {
				// �ړ����v���X���Ȃ��Ȃ����ꍇ
				finalFlag = true;
			} else {
				// �ړ������}�C�i�X�̏ꍇ
				finalFlag = _args[2] * -1 != eventNo[getXYMap(mem[M_X],
						mem[M_Y])];
			}

			// �ړ��I���̏ꍇ
			if (finalFlag) {

				// �f����X�N���v�g������ꍇ�A�V�[��4
				// �����ꍇ�A�V�[��1
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
	// �V�[��7 ���C�u�����V�[��
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

			// ����z����h��
			if (index >= _args.length - 1) {
				_args[_args.length - 1] = -1;
			}
			reparse();

			// �߂������ɑO��I���������ڂ�I��
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
	// �S�V�[������
	/* ========================= */

	private boolean msgEvent(int type, int param) {
		if (type == Display.KEY_PRESSED_EVENT) {
			if (_sel <= 0) {
				// �I�����[�h�łȂ��Ƃ��́A�ǂ̃L�[�������Ă�����ʂɑJ��
				return true;
			}

			if (param == Display.KEY_SELECT) {
				// �I���L�[�Ŏ���ʂɑJ��
				return true;
			}

			if ((param == Display.KEY_SOFT1) || (param == Display.KEY_IAPP)) {
				// �\�t�g�L�[1��������CLEAR�L�[ �őO�̑J�ڂɖ߂�
				// �������AmenuEvents���p�[�X���Ă��鎞�̂�
				// (�܂胁�j���[�V�[���A�퓬�V�[���̂�)
				if (parsedEvent == menuEvents) {
					_sel = 0;
					return true;
				}
			}

			if (param == Display.KEY_RIGHT) {
				// �E�L�[�Ŏ��̃y�[�W��
				_page++;
				if (_args[0] >= 0) {
					reparse();
				}
			}

			if (param == Display.KEY_LEFT) {
				// ���L�[�őO�̃y�[�W��
				if (_page > 0) {
					_page--;
					if (_args[0] >= 0) {
						reparse();
					}
				}
			}

			if (param == Display.KEY_UP) {
				// �J�[�\�������Ɉړ��B
				// �����J�[�\����0�ȉ��ɂȂ�����A
				// �s�̏I���܂ŃJ�[�\�������Ɉړ�����
				// (�J�[�\�����[�v:��)
				if (--_sel < 1)
					while (isLine(++_sel + 1))
						;

			}

			if (param == Display.KEY_DOWN) {
				// �J�[�\��������Ɉړ��B
				// �����J�[�\�����s�̏I���ȏ�ɂȂ�����A
				// �J�[�\����1�ɖ߂�
				// (�J�[�\�����[�v:��)
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
		// ���j���[�I��������ꍇ�A�J�[�\�����ŏ��ɂ���
		if (_sel >= 1) {
			_sel = 1;
		}

		// �p�[�X�Ώۂ��Ȃ��ꍇ�A�����ɏI��
		if (parsedEvent == null) {
			return;
		}

		// �R�}���h�X�N���v�g�ԍ������̐��Ȃ�A
		// �R�}���h�X�N���v�g0�Ԃ����s
		if (_args[0] < 0) {
			parse(parsedEvent[0]);
		} else if (_args[0] < parsedEvent.length) {
			parse(parsedEvent[_args[0]]);
		}
	}

	/**
	 * �L�������ړ��O������ړ��ɐ؂�ւ��ۂ̃A�j���[�V��������
	 */
	private void moveAnime1() {
		sftX = calcXY(mem[M_QUOTA], 0);
		sftY = calcXY(mem[M_QUOTA], -1);
		int xx = mem[M_X] + sftX;
		int yy = mem[M_Y] + sftY;
		if (!isMap(xx, yy)) {
			// �i���֎~�̏ꏊ�ɂ͐i���ł��Ȃ�
			sftX = 0;
			sftY = 0;
			g$draw(null, G_REPAINT, 0, 0, 0, null);
			return;
		}
		if ((_scene == 1) && !walkIn[getXYMap(xx, yy)]) {
			// �V�[��1�̏ꍇ�A�i���֎~�`�b�v���i���ł��Ȃ�
			sftX = 0;
			sftY = 0;
		}
		g$draw(null, G_REPAINT, 0, 0, 0, null);
	}

	/**
	 * �L�����������ړ�������ړ��ɐ؂�ւ��ۂ� �A�j���[�V��������
	 *
	 * @return ���ۂɈړ��������ǂ����BfieldEvent�ɂă`�b�v�ˑ��C�x���g���������邩�ǂ����̔���Ɏg�p����Btrue���ړ����Ă��Ȃ�
	 *         false���ړ�����
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
	// �Q�[���V�X�e���p���[�e�B���e�B
	/* ========================= */
	/**
	 * �퓬���A�N���̍s�����O�ɌĂяo����� ����s���҂��������A�s���҂��N�����Ȃ�������initTurn()�����s���ăC�j�V�A�e�B�u������
	 * �s���҂��G�̏ꍇ�̓����_���ɍs����I�� �h����������� �����Ă���ꍇ�A������I��
	 */
	private void initButtle() {
		// �g�D���u�N�g�D���p�[�e�B�ɂ���ꍇ�A
		// �g�D���u�N�g�D�̗V�ѐ�p�C�j�V�A�e�B�u��p��
		if (id2mem(4) >= 0) {
			members[5] = 99;
		} else {
			members[5] = -1;
		}

		// �퓬���́A�K���s�����Ԃ̍ŏ��̃L�������s���҂Ƃ���
		// �s�����Ԉꏄ������A�Ăэs�����Ԃ����肷��
		int initiative = getInitiative();
		if (initiative < 0) {
			initTurn();
			initiative = getInitiative();
		}

		// �����s���ł���l�����Ȃ��ꍇ�A�����ɕԋp
		if (initiative < 0)
			return;

		// �����̏ꍇ�A�C�j�V�A�e�B�u�l��id�ɕϊ�
		if (initiative < 6) {
			initiative = members[initiative];
		}

		_args[1] = initiative;

		if (initiative >= 6) {
			// �G���s���҂̏ꍇ�́A�����_���ɑΏێ҂ƍs�������߂�
			_args[0] = 2;
			if (initiative < 16) {
				int algo = 0;
				if (initiatives[initiative] < 99) {
					algo = randi(4);
				}

				// �퓬�]���t�F�[�Y ���ݑI�������s���̃_���[�W���L�^����
				// optFlg��12����15���Ԏ؂肵�A���ꂼ��̃t���O/4����Ȃ�A
				// ���̃_���[�W���L�^����
				if (0 != (getAlign(mem2id(_args[1])) & 256)) {
					if ((optFlg[12 + algo] & 255) < 255) {
						optFlg[12 + algo] += 4;
					}
				}
				_args[2] = enemyAlgo[mem2id(_args[1]) - 6][algo];
			} else {
				// �g�D���u�N�g�D�̗V�ѐ�p�s��
				_args[2] = 31 + randi(4) + randi(4) - 3;
				// ������!! ����guard[]�Asleep[]�z��ɐG���Ă͂����Ȃ��I
				return;
			}
		}

		// �h�䂵�Ă鑮�����͂���
		optFlg[initiative] &= -2;

		// �����Ȃ��ꍇ�AmenuEvent[12](�����Ȃ�)��\��
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
			// �L�����I���̏ꍇ�Amembers[]�z��̒����܂Ń��[�v
			roopMax = 4;
		}

		for (int i = 0; i < roopMax; i++) {
			order = i;
			int amount = 0;
			// ���@���A����ɂ���ď����𕪉�
			if (cmNames == mgcs) {
				test = eventFlg[i + F_MAGIC]; // ���@�̏ꍇ
				if (i == 5) {
					// �u�C���v�̏ꍇ�A���ԂɃo���b�^�������true
					test = id2mem(3) >= 0;
				}
				if (i == 6) {
					// �u�E�[���x�c�v�̏ꍇ�A���҂̖X�q�������Ă����true
					test = getAmount(12) > 0;
				}
			}
			if (cmNames == iName) {
				amount = iAmount[i]; // ����E�����̏ꍇ
				if (parsedEvent == menuEvents) {
					for (int j = 0; j < 4; j++) {
						if ((0 <= _args[1]) && (_args[1] < 6)) {
							if (((1 << (j + 6)) & type) != 0) {
								if ((i > 0) && (eqs[_args[1]][j] == i)) {
									amount++; // ����ґ����i��ʂɊ܂߂�
								}
							}
						}
					}
				}
				test = amount > 0;
				test &= (iType[i] & type) >= type; // �����̏ꍇ�A�ӏ��E�����҂��l��
				test |= (type >= 0) && (i == 0); // �����̏ꍇ�A(�Ȃ�)������true
				test |= (type == 264) && (i == 48); // �o���b�^�o�g���r�L�j������
			}
			if (cmNames == names) {
				// �L�������I���̏ꍇ
				order = members[i];
				test = order >= 0;
			}
			if (cmNames == saveList) {
				// �Z�[�u�ꗗ�̏ꍇ
				test = true;
			}
			if (_scene == 7) {
				if (cmNames == iName) {
					// ���܂��E�A�C�e���ꗗ�̏ꍇ
					test = !mapEvent[i].equals("") && i != 0;
				}
			}
			if (test) {
				index++;

				if (index >= W_ROW) {
					nextPage = true; // ���y�[�W�����邩�ǂ���
					break;
				}

				if (index >= 1) {

					if (_scene == 7) {
						// ���܂��E�A�C�e���ꗗ�̏ꍇ�A�}�ӂɓo�^����Ă��Ȃ��A�C�e���́u0�v��
						if (cmNames == iName) {
							if ((completeItem & (1 << order)) == 0) {
								order = 0;
							}
						}
						// ���܂��E�����X�^�[�ꗗ�̏ꍇ�A�}�ӂɓo�^����Ă��Ȃ����͍̂Ō�̔ԍ�
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
						// �A�C�e����2�ȏ゠��Ȃ�A�~����\��
						lines[index] += " �~" + amount;
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
				lines[1] = "�I����������܂���";
			}
		}

		// �擪�̃��x���ɍ��E�y�[�W�ւ̈ړ��̃q���g������
		if ((_page > 0) && (index > 0)) {
			lines[0] += "��";
		}
		if (nextPage) {
			lines[0] += "��";
		}
	}

	/**
	 * �n�}�ꗗ��\������
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

		lines[0] = "�y��̒n�}�z";
		lines[1] = "���݂̒n�} (" + mem[M_ISL] + "-" + mem[M_RGN] + ")";
		selLines[1] = (mem[M_ISL] - 1) * 4 + (mem[M_RGN] - 1);
		for (int i = 2; i <= 5; i++) {
			lines[i] = (_page + 1) + "-" + (i - 1);
			selLines[i] = _page * 4 + i - 2;
		}

		// �擪�̃��x���ɍ��E�y�[�W�ւ̈ړ��̃q���g������
		if (_page > 0) {
			lines[0] += "��";
		}
		if (_page < 4) {
			lines[0] += "��";
		}
	}

	/**
	 * ���ł̈ړ��������X�g��\������
	 *
	 */
	private void raftHList() {
		StringBuffer buff = new StringBuffer();
		for (int i = 0; i < raftHist.length; i++) {
			if (raftHist[i] < 0)
				break;
			int idx = raftHist[i];
			buff.append("����");
			buff.append(i + 1);
			buff.append("[��:");
			buff.append(raftTable[idx][0]);
			buff.append(" AREA:");
			buff.append(raftTable[idx][1]);
			buff.append(" ��:");
			buff.append(format(raftTable[idx][2], 2, ' '));
			buff.append(" ��:");
			buff.append(format(raftTable[idx][3], 2, ' '));
			buff.append("]");

			lines[lindex] = buff.toString();
			buff.setLength(0);
			selLines[lindex] = idx;
			lindex++;
		}
	}

	/**
	 * �Ώێ҂Ƀ_���[�W��^����B�^�������ʁAHP��0�ȉ��ɂȂ����� �Ώێ҂͐�����E�B �_���[�W�ʂ�0�����ł���΁A0�_���[�W��^����(�񕜂͂��Ȃ�)
	 *
	 * @param point
	 *            �^����_���[�W
	 */

	private int damage(int point) {
		if (point < 0) {
			point = 0;
		}
		int dfid = mem2id(_args[3]);
		if ((optFlg[_args[3]] & 1) != 0) {
			point /= 2; // �h�䂵�Ă���΃_���[�W�͔���
		}
		lines[lindex++] = names[dfid] + " : " + point + "�_���[�W";
		hps[_args[3]] -= point;
		if (hps[_args[3]] <= 0) {
			kill(dfid);
		}

		// �퓬�]���t�F�[�Y optFlg12...15���Ԏ؂肵�A4�Ŋ������]�肪
		// ��̉ӏ�������΂����Ƀ_���[�W*256�����Z
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
	 * �����ɗ^����ꂽ�L�����N�^�[�̎��S�葱�����s��
	 *
	 * @param dfid
	 *            ���S����L�����N�^�[
	 */
	private void kill(int dfid) {
		lines[lindex++] = names[dfid] + " : ���S";
		initiatives[id2mem(_args[3])] = -1; // �C�j�V�A�e�B�u�폜
		if (_args[3] >= 6) {
			// �G��|�����ꍇ
			// �o���_����ѕ󕨔���
			int enm = members[_args[3]];
			winXp += xps[enm];
			int prise = enemyDrop[enm - 6];
			if ((prise > 0) && (randi(5) == 0)) {
				winItem = prise;
			}
			// �����X�^�[�}�ӓo�^�t���O��on
			eventFlg[F_MONSTER + enemyID[enm - 6]] = true;
		} else {
			hps[_args[3]] = 1;
			// �g�D���u�N�g�D���S�̏ꍇ�A�V�уC�j�V�A�e�B�u���폜
			if (dfid == 4) {
				members[5] = -1;
			}
		}
		members[id2mem(_args[3])] = -1; // �����o�[�폜
	}

	/**
	 * �A���P�~�A�̔��� _args[3]���Ώ�
	 *
	 * @return 0 ���ʂȂ� 1 �퓬����R���s 2 �����ϊ��\
	 */
	private int alchemia() {
		if (_scene == 3) {
			// �A���P�~�A�̒�R���� ���̃p�[�Z���e�[�W : 50 + (�Ώۂ�MaxMP - �p�҂�MaxMP) / 4
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
	 * �Ώێ҂���HP���񕜂���B�񕜂������ʁAHP���ő�HP���z������ �ő�HP�܂ŉ񕜂���B
	 * �񕜗ʂ�0�����ł���΁A0�_�񕜂���(�_���[�W�͗^���Ȃ�)
	 *
	 * @param point
	 *            �񕜂���|�C���g
	 */

	private void cure(int point) {
		int dfid = mem2id(_args[3]);
		if (point < 0) {
			point = 0;
		}
		if (getMaxHp(_args[3]) < hps[_args[3]] + point) {
			point = getMaxHp(_args[3]) - hps[_args[3]];
		}
		lines[lindex++] = names[dfid] + " : HP" + point + "��";
		hps[_args[3]] += point;

		// �퓬�]���t�F�[�Y optFlg12...15���Ԏ؂肵�A4�Ŋ������]�肪
		// ��̉ӏ�������΂����ɉ񕜗�*256�����Z
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
	 * �ő�HP��Ԃ��B�A�^���̏ꍇ�A�ő�HP�ɖh��_���v���X����
	 *
	 * @param id
	 *            �L����ID
	 * @return �ő�HP
	 */
	private int getMaxHp(int id) {
		int result = hpis[id];
		if (id < 6) {
			// �����L�����̏ꍇ�A���x���㏸�l��������
			result += hpds[id] * lvs[id] / 8;
		} else {
			// �G�L�����̏ꍇ�A�}�X�^�f�[�^���Q��
			result = hpis[members[id]];
		}
		if (id == 2) {
			// �A�^���̏ꍇ�A�h��_���ő�HP�ɉ�����
			result += getDefDf(id);
		}
		return result;
	}

	/**
	 * �ő�MP��Ԃ��B
	 *
	 * @param id
	 *            �L����ID
	 * @return �ő�MP
	 */
	private int getMaxMp(int id) {
		int result = mpis[id];
		if (id < 6) {
			// �����L�����̏ꍇ�A���x���㏸�l��������
			result += mpds[id] * lvs[id] / 8;
		} else {
			// �G�L�����̏ꍇ�A�}�X�^�f�[�^���Q��
			result = mpis[members[id]];
		}
		return result;
	}

	/**
	 * �U���͂�Ԃ��B�퓬���̈ꎞ�U���͉��Z�l���v���X�B�A�^���̏ꍇ�A����ɖh��͂��v���X�B�]���r�����Ă���ꍇ�A�����L�����͑����̍U���͂𑫂��Ȃ��B
	 *
	 * @param id
	 *            �L����ID
	 * @return �U����
	 */
	private int getDefAt(int id) {
		int result = atis[id];
		if (id < 6) {
			// �����L�����̏ꍇ�A���x���㏸�l��������
			result += atds[id] * lvs[id] / 8;
			// �]���r�����Ă��Ȃ��ꍇ�A����̒l���v���X
			if (!eventFlg[10]) {
				result += iValue[eqs[id][0]];
				if (id == 2) {
					// �A�^���̏ꍇ�A�h��_���U���͂ɉ�����
					result += getDefDf(id);
				}
			}
		} else {
			// �G�L�����̏ꍇ�A�}�X�^�f�[�^���Q��
			result = atis[members[id]];
		}

		// �퓬���̏㏸�␳���v���X
		result += safeArray(optAt, id, 0);
		return result;
	}

	/**
	 * �h��͂�Ԃ��B�퓬���̈ꎞ�h��͉��Z�l���v���X�B�]���r�����Ă���ꍇ�A�����L������0��Ԃ��B
	 *
	 * @param id
	 *            �L����ID
	 * @return �h���
	 */
	private int getDefDf(int id) {
		int result = 0;
		if (id >= 6) {
			// �G�L�����̏ꍇ�A���炩���ߐݒ肳�ꂽ�l��
			// �h��_�Ƃ��ĕԂ�
			result = dfes[members[id] - 6];
		} else {
			if (!eventFlg[10]) {
				// �]���r�����Ă��Ȃ����̂ݖh��̒l���v���X
				result = iValue[eqs[id][1]] + iValue[eqs[id][2]]
						+ iValue[eqs[id][3]];
			}
			if (id == 5) {
				// �A�x�V�F�̏ꍇ�A�U���͂̒l��h��_�Ƃ���
				result += getDefAt(id);
			}
		}
		// �퓬���̏㏸�␳���v���X
		result += safeArray(optDf, id, 0);
		return result;
	}

	/**
	 * �����l��Ԃ��B�]���r�����Ă���ꍇ�A�����I�Ƀ]���r������Ԃ��B
	 *
	 * @param id
	 *            �L����ID
	 * @return �����l
	 */
	private int getAlign(int id) {
		int result = 0;
		if (id >= 6) {
			// �G�L�����̏ꍇ�A���炩���ߐݒ肳�ꂽ�l��
			// �����l�Ƃ��ĕԂ�
			result = align[id - 6];
		} else {
			result = iAlign[eqs[id][0]] | iAlign[eqs[id][1]]
					| iAlign[eqs[id][2]] | iAlign[eqs[id][3]];
			// �]���r���t���O������Ƃ��̓]���r�������̂�
			if (eventFlg[10]) {
				result = 32;
			}
		}
		return result;
	}

	/**
	 * ���̃��x���A�b�v�ŕK�v�Ȓ�����Ԃ��B���T���f�X�̏ꍇ�A�݌v�o���_��Ԃ��B�o���b�^�̏ꍇ�A���ݏ�������1/16+1��Ԃ��B
	 *
	 * @param id
	 *            �L����ID
	 * @return �K�v����
	 */
	private int getNextXp(int id) {
		if ((id != 0) && (lvs[id] >= lvs[0])) {
			// ���Ԃ̃��x�������T���f�X�ȏ�̏ꍇ�A-1��Ԃ�
			return -1;
		}
		if (id == 3) {
			// �o���b�^�̏ꍇ���݂̏�������1/16 + 1������Ƃ���
			int next = gem / 16 + 1;
			if(next > 255){
				next = 255;
			}
			return next;
		}
		// ���T���f�X�̏ꍇ�A���x���v�Z��
		// ����Lv���ЂƂÂ��Z����
		// (�����͏���邪�A�o���_�͏���Ȃ�����)
		int latio = id == 0 ? lvs[0] : 1;
		return xps[id] * latio + lvs[id] * latio + 10 * (latio - 1);
	}

	/**
	 * �f�t�H���g������p���āA���S�ɔz��ɃA�N�Z�X���� �z��̃C���f�b�N�X�𒴂����ꍇ�A�f�t�H���g�l���K�p�����
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
	 * ���݂̏󋵂ɂ��킹�āA�K�؂ȃ��j���[���x����Ԃ�
	 *
	 * @return
	 */
	private String getLabel() {

		String offence = "";

		if (_scene == 0) {
			// �I�[�v�j���O�E�G���f�B���O�V�[���ł͕K���Q�[���^�C�g��
			offence = "5�̕�";
			// _args[0] ��-1�̏ꍇ�̓o�[�W��������ǉ�
			if (_args[0] <= 0) {
				offence += "      Ver 1." + format(VER, 2, '0') + "."
						+ format((int) resVersion, 4, '0');
			}
			return offence;
		}

		if (_scene == 7 && _args[1] == 2) {
			offence = "�A�C�e���ꗗ";
		} else if (_scene == 7 && _args[1] == 3) {
			offence = "�����X�^�[�ꗗ";
		} else if ((0 <= _args[1]) && (_args[1] < 6)) {
			offence = names[_args[1]];
		} else if ((6 <= _args[1]) && (_args[1] < 16)) {
			offence = names[members[_args[1]]];
		}

		String command = "";

		if (_args[0] == 1) {
			command = "�U��";
		}

		if (_args[0] == 2) {
			command = "���@";
			if ((0 <= _args[2]) && (_args[2] < 16)) {
				command = mgcs[_args[2]];
			}
		}
		if (_args[0] == 3) {
			command = "����";
			if (_args[2] >= 0) {
				command = iName[_args[2]];
			}
		}
		if (_args[0] == 6) {
			command = "����";
			if (_args[2] == 0)
				command = "����";
			if (_args[2] == 1)
				command = "��";
			if (_args[2] == 2)
				command = "������";
			if (_args[2] == 3)
				command = "������";
		}

		if (!lines[lindex].equals("")) {
			command = lines[lindex];
		}

		String delim = "";
		if ((!offence.equals("")) && (!command.equals(""))) {
			delim = "��";
		}
		if ((offence.equals("")) && (command.equals(""))) {
			delim = "���j���[";
		}

		// �s�������łɊm�肵�Ă���ꍇ(_args[2]�ɒl�������Ă���ꍇ)
		// �t�H�[�}�b�g��ύX
		if (_args[2] >= 0) {
			return offence + " : " + command;
		}

		return "�y" + offence + delim + command + "�z";

	}

	/**
	 * ����z��̒��Ɏw�肵�����������邩�ǂ�������
	 *
	 * @param id
	 *            �w�肵������
	 * @param args
	 *            �z��(_args�܂���member)
	 * @param offset
	 * @return true : ���������� false : ���Ȃ�
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
	 * �A�C�e���̒ǉ��E�폜�������s��
	 *
	 * @param iam
	 *            �A�C�e��������
	 * @param iid
	 *            �A�C�e�����
	 */
	private void addItem(int iam, int iid) {
		if (_scene == 1)
			setScene(4);
		if (iam > 0) {
			lines[lindex++] = iName[iid] + "����ɓ��ꂽ";
		}
		if (iam < 0) {
			lines[lindex++] = iName[iid] + "��������";
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
		lines[lindex++] = names[id] + "�̓��x��" + lvs[id] + "�ɂȂ���";
		lines[lindex++] = "HP�㏸ : " + hp;
		lines[lindex++] = "MP�㏸ : " + mp;
		lines[lindex++] = "�U���͏㏸ : " + at;
		hps[id] += hp;
		mps[id] += mp;

		// ���э���Lv10�ɂȂ�ƃ}�t�@�C�A���o����
		if ((id == 1) && (lvs[id] == 10)) {
			eventFlg[F_MAGIC + 8] = true;
			lines[lindex++] = "���@�u�}�t�@�C�A�v����ɓ��ꂽ�I";
		}
	}

	/**
	 * ���ݐ퓬���I�����Ă��邩�ǂ�����Ԃ��B ����(member[0..5]�����݂��Ȃ��ꍇ��-1�A
	 * �G(members[6..16])�����݂��Ȃ��ꍇ��1�A ���w�c�Ƃ����݂���ꍇ��0
	 *
	 * @return ����(�G���݂��Ȃ�)1�A�����Ȃ�0�A�s�k(�������݂��Ȃ�)-1
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
	 * @return ���݈�ԃC�j�V�A�e�B�u�������L�����ԍ���Ԃ��B���ׂẴL�������s���I����Ԃ������ꍇ�A-1��Ԃ�
	 */
	private int getInitiative() {
		int maxDex = -1;
		for (int i = 0; i < initiatives.length; i++) {
			// ���łɂ��Ȃ��L�����N�^�[�̓C�j�V�A�e�B�u��ݒ肵�Ȃ�
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
	 * ���͂��ꂽID����A����̔ԍ���Ԃ� ����ɕғ�����Ă��Ȃ������ꍇ�A-1��Ԃ�
	 *
	 * @param id
	 *            �L�����ԍ�
	 * @return ����ԍ�
	 */

	private int id2mem(int id) {
		if ((6 <= id) && (id < 16)) {
			// �G�L�����̏ꍇ�A�ԍ������̂܂ܕԂ�
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
	 * ���͂��ꂽ����ԍ�����A�L�����ԍ���Ԃ��B�G�L�����̏ꍇ�̂݃}�X�^�f�[�^��Ԃ�
	 *
	 * @param mem
	 *            ����ԍ�
	 * @return �L������
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
	 * �퓬���A1�^�[���̍ŏ��ɍs���ׂ����������s
	 *
	 */
	private void initTurn() {
		// �C�j�V�A�e�B�u�v�Z
		for (int i = 0; i < members.length; i++) {
			if (members[i] >= 0) {
				initiatives[i] = safeArray(dexs, members[i], 0) + randi(8);
			} else {
				initiatives[i] = -1;
			}
		}
	}

	/**
	 * �����̑I��Ώۂ������_���Ɍ���
	 *
	 * @return �I�����ꂽ������ID
	 */

	private int chooseTarget() {
		int party = 3;
		while ((members[party] < 0) && (party >= 0))
			party--;
		int temp = randi(party + 1);
		return members[randi(temp + 1)];
	}

	/**
	 * ���w�̒��ň�ԏ������Ώۂ�����
	 *
	 * @return �I�����ꂽID
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
	 * �G�������_���Ō���
	 *
	 * @return �I�����ꂽ�G��ID
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
	 * �G�I���B�G�����Ȃ��������X�L�b�v����
	 *
	 * @param plus
	 *            �I�����̉��Z�l
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
	 * �G�̏���HP�⏉��MP���Đݒ肷��
	 *
	 * @param i
	 */
	private void setupEnemy(int i) {
		hps[i] = getMaxHp(i);
		mps[i] = getMaxMp(i);

		// �o���_0�̃����X�^�[�́A�o����������Ő}�Ӄ��X�g����
		if (xps[members[i]] == 0) {
			eventFlg[F_MONSTER + members[i]] = true;
		}
	}

	/**
	 * �n�}�ω��t���O���l�����āA�w�肳�ꂽ���W����`�b�v�ԍ����擾����
	 *
	 * @param x
	 * @param y
	 */
	private byte getXYMap(int x, int y) {
		byte chip = 0;
		if (isMap(x, y)) {
			chip = xyMap[y][x];
		}
		// �n�}�ω��t���O�ɂ��`�b�v�ω�
		if (eventFlg[F_MAP + y * MAP + x]) {
			chip++;
		}
		chip = (byte) (chip % walkIn.length);
		return chip;
	}

	/**
	 * ���ݑI�𒆂̃A�C�e���������͖��@No��Ԃ��B
	 *
	 * @return
	 */
	private int getMNo() {
		int result = _args[2];
		// �I���������@���A�A�C�e�����ŏ�������
		if (_args[0] == 3) {
			result = iEvents[result];
		}
		// �C�x���g�u16�v�̃A�C�e���Ȃ�A�A�C�e���ԍ���Ԃ�
		if (result == 16) {
			result = _args[2];
		}
		return result;
	}

	/**
	 * ���W���n�}�̑Ó��Ȓl�ȓ������肷��
	 *
	 * @param level
	 *            ���E�G���A�EXY���W�̂����ǂꂩ
	 * @param param
	 *            ���͂���l
	 * @return �Ó��Ȓl�ł����true
	 */
	private boolean isMatrix(int level, int param) {
		if (level == 1) {
			// ���ԍ��̏ꍇ
			return ((1 <= param) && (param <= 5));
		} else if (level == 2) {
			// �G���A�ԍ��̏ꍇ
			return ((1 <= param) && (param <= 4));
		} else {
			// X�AY���W�̏ꍇ
			return ((0 <= param) && (param <= 31));
		}
	}

	/**
	 * ���ړ��������X�V����
	 *
	 * @param idx
	 *            ���̈ړ��C���f�b�N�X
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
	 * �����i���܂߂��A���݂̏����A�C�e����
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
	// ���������}�b�v�֘A
	/* ========================= */

	/**
	 * ���������}�b�v��ݒ肷��
	 *
	 * @param pos
	 *            �}�b�v�S�̂�4�~4���������l�ŁA0..15�̐��l�ŕ\��
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
	// �����񃆁[�e�B���e�B
	/* ========================= */

	// �������E�l�Ƀt�H�[�}�b�g
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

	// �X�N���v�g�̎��s
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
				// ���[�v�̏�������
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
						// �o���b�^�̓���C�x���g����
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
						// 4/3�̏��Α��u�C�x���g������
						if ((mem[M_ISL] == 4) && (mem[M_RGN] == 3)
								&& (eventFlg[F_MAP + 3])) {
							damage = 0;
						}
						if (damage >= 0) {
							// �o���b�^�̓���C�x���g����
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
							lines[lindex++] = "����������";
						} else {
							lines[lindex++] = "�����Ɏ��s!!";
						}
					} else if (token.equals("buy?")) {
						if (gem >= args[idx]) {
							gem -= args[idx];
							args[idx] = 1;
						} else {
							lines[lindex++] = "����������Ȃ�";
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
						// �Ώۂ̒�R�l�v�Z
						int result = 0;
						int al = getAlign(mem2id(_args[3])) & 24;
						if (al == 8) {
							// �����l��8�Ȃ�A�������ɒ�R
							result = 1;
						} else if (al == 16) {
							// �����l��16�Ȃ�A�������Ɍ��ʂ���
						} else if (randi(10) < args[idx]) {
							// ����ȊO�Ȃ�A�����~10%�̊m���Ő���
							result = 1;
						}
						args[idx] = result;
					} else if (token.equals("dispell?")) {
						// 1/2�̊m���Ŏ���������
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
						// ���҂̖X�q�𑕔����Ă���L�����͖��@����+3
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
						// ��𑮐��������Ă���G��1/4�̊m���ōU�������
						// // 4/3�̊�ׂ��C�x���g������
						if (((getAlign(mem2id(_args[3])) & 64) == 64)
								|| ((mem[M_ISL] == 4) && (mem[M_RGN] == 3) && (eventFlg[F_MAP + 275]))) {
							if (randi(4) == 0) {
								lines[lindex++] = names[mem2id(_args[3])]
										+ " : ��𐬌��I";
								skip = true;
							}
						}
					} else if (token.equals("getRc")) {
						// ����̌��ɂ���ꍇ�̒��ڃ_���[�W�␳
						int member = id2mem(_args[3]);
						if (member < 6) {
							args[++idx] = member * -2;
						} else {
							args[++idx] = 0;
						}
					} else if (token.equals("calcClit")) {
						// �N���e�B�J���q�b�g�̃_���[�W�v�Z
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
						// �]���r��MP�������Ȃ�
						if (!setFlag || (getAlign(mem2id(_args[3])) & 32) == 0) {
							args[15] = idx;
							args[14] = 3;
							intVal(mps, args, setFlag);
							// �ő�E�ŏ�MP���l���ɓ����
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
						// �ő�E�ŏ�HP���l���ɓ����
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
							// �A�^���̏ꍇ�A�ő�HP�𒴉߂��邩����
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
						c = menuEvents[0].toCharArray(); // ���C�����j���[�ɖ߂�
						continue;
					} else if (token.equals("levelUp")) {
						levelUp(_args[1]);
					} else if (token.equals("endAction")) {
						endAction(true);
						end = true; // �p�[�X�����̍s�Œ�~
					} else if (token.equals("endEnemy")) {
						if (_args[1] >= 6
								|| initiatives[id2mem(_args[1])] >= 99) {
							endAction(true);
						}
						end = true; // �p�[�X�����̍s�Œ�~
					} else if (token.equals("fetch")) {
						fetch();
					} else if (token.equals("encounter")) {
						for (int ii = 6; ii < 16; ii++) {
							if ((idx >= 0) && (members[ii] < 0)) {
								members[ii] = args[idx--];
								setupEnemy(ii);
							}
						}
						// �퓬�V�[���ȊO��������A�퓬�V�[���ɕύX
						if (_scene != 3) {
							setScene(3);
							useCache = false;
						}
					} else if (token.equals("save")) {
						args[++idx] = saveGame(_args[2]);
					} else if (token.equals("delGame")) {
						clearSave(_args[2]);
					} else if (token.equals("raft")) {
						// ���V�[���ɂ���
						setScene(5);
					} else if (token.equals("inn")) {
						for (int ii = 0; ii < 6; ii++) {
							hps[ii] = getMaxHp(ii);
							// �]���r�����Ă���Ƃ���mp�񕜂��Ȃ�
							if (!eventFlg[10]) {
								mps[ii] = getMaxMp(ii);
							}
						}
					} else if (token.equals("attt")) {
						// �A�^�^�^�b�̘A���U������
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
						// �C�x���g�t���O���}�b�v�t���O�ɑΉ��Â���
						int eflg = 0;
						int mflg = 0;
						for (; idx >= 0; idx -= 2) {
							mflg = args[idx];
							eflg = args[idx - 1];
							eventFlg[mflg + F_MAP] = eventFlg[eflg];
						}
					} else if (token.equals("argsMapping")) {
						// _args[4]�ȍ~�Ɉ���4�����蓖�Ă�
						_args[7] = args[idx--];
						_args[6] = args[idx--];
						_args[5] = args[idx--];
						_args[4] = args[idx--];
					} else if (token.equals("flagRect")) {
						// �n�}��̋�`�͈͂Ƀt���O��K�p����
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
						// mem[M_QUOTA]�̏㉺���E��map�̉��Z�l{-32,+32,-1,+1}�ɕϊ�
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
		// �t�B�[���h�V�[���̂܂܏I�������ꍇ�A�p�[�X�X�N���v�g�����Z�b�g
		if (_scene == 1) {
			setScene(1);
		}
	}

	/**
	 * �umemberList�v���p�[�X���ꂽ�Ƃ��̏��� �G�I���t���O�����̒l�̂Ƃ��͓G�̑I����ʁA ����ȊO�̂Ƃ��͌��݂̃p�[�e�B�̑I�����
	 * �A�^�^�^�b���s���̂Ƃ��͓G�������������_���ɑI��
	 */
	private void memberList() {
		if (safeArray(initiatives, _args[1], -1) >= 256) {
			// �A�^�^�^�b���s���̂Ƃ�
			if (selectEnemy > 0) {
				while (members[_args[3]] >= 0) {
					_args[3] = randi(10) + 6;
				}
			} else {
				while (members[_args[3]] >= 0) {
					_args[3] = randi(4);
				}
			}
			lines[lindex++] = names[mem2id(_args[3])] + "���^�[�Q�b�g�ɂ���";
			return;
		}

		if (selectEnemy > 0) {
			if (_sel + 6 >= 0) {
				// �G�̖��O��1��ڂɕ\��
				enemySelCursor(0);
				lines[lindex++] = names[members[_sel + 6]];
			}
			if (_sel < 5) {
				// ������I������@�\��\��
				lines[lindex++] = "�@(���L�[�Ŗ�����I��)";
			}
		} else {
			idList(names, 0);
			if ((_scene == 3)) {
				// �퓬�V�[���̏ꍇ�A�G��I������@�\��\��
				lines[lindex] = "���G��I��";
				selLines[lindex] = 99;
				lindex++;
			}
		}
	}

	/**
	 * �����������鎞�̏��� �擾�o���l�A�A�C�e����0�ɂ��Đ퓬���I������
	 *
	 * @return ��₵���G�����Ȃ����ɂ�false ����ȊO�̓G������Ƃ��ɂ�true(������X�N���v�g���N���A����)
	 */
	private boolean run1() {
		int rockOnly = 0; // ����㩑Ή��B�G�����1�C�������݂��Ă��鎞�͓����Ă�����
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
	 * parse()���\�b�h�p���[�e�B���e�B�BsetFlag��true�Ȃ�Ώ۔z��ɑ���A false�Ȃ�Ώ۔z�񂩂�l��ǂݍ���
	 * parse()���\�b�h��idx�ɓ�����ϐ���args[15]�ɕێ������̂ŁA���炩���� args[15]��idx�̒l���Z�b�g����K�v������
	 * �܂��Aargs[14]�̒l�őΏەϐ������򂷂�B
	 * args[14]��0�����̒l�Ȃ�_args[]�z���Ώۂ̒l�Ƃ���B�Ⴆ��args[14]==3�Ȃ�ΏۂƂȂ�l��_args[3]
	 * args[14]�����̒l�Ȃ�idx(���Ȃ킿args[15])�̒l���g�p����
	 *
	 * @param val
	 *            �ΏۂƂȂ�int�z��
	 * @param args
	 *            parse()���\�b�h��args
	 * @param setFlg
	 *            parse()���\�b�h��setFlg
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
	 * parse()���\�b�h�p���[�e�B���e�B�BsetFlag��true�Ȃ�Ώ۔z��ɑ���A false�Ȃ�Ώ۔z�񂩂�l��ǂݍ���
	 * parse()���\�b�h��idx�ɓ�����ϐ���args[15]�ɕێ������̂ŁA���炩���� args[15]��idx�̒l���Z�b�g����K�v������
	 * �܂��Aargs[14]�̒l�őΏەϐ������򂷂�B
	 * args[14]��0�����̒l�Ȃ�_args[]�z���Ώۂ̒l�Ƃ���B�Ⴆ��args[14]==3�Ȃ�ΏۂƂȂ�l��_args[3]
	 * args[14]�����̒l�Ȃ�idx(���Ȃ킿args[15])�̒l���g�p����
	 *
	 * @param val
	 *            �ΏۂƂȂ�int�z��
	 * @param args
	 *            parse()���\�b�h��args
	 * @param setFlg
	 *            parse()���\�b�h��setFlg
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
	 * �S�̍U���p�t�F�b�`�֐� _args[4]�ɓG�������͖����̐擪�̑���ԍ�����͂���
	 * �t�F�b�`�֐����Ăяo�����тɁA_args[4]�Ɏ��̑���ԍ������͂���� �G�������͖����̍ŏI����ԍ��ɂ���������ƁA_args[4]��-1��Ԃ�
	 */
	private void fetch() {
		// _args[4]�̒l��������Ȃ��悤�ɃL���b�v
		_args[5] = 0;
		// �L�����N�^�[�����݂���ꏊ�܂ő���
		for (; _args[4] < 16; _args[4]++) {
			if (members[_args[4]] >= 0)
				break;
		}

		// ���݂̃J�[�\����Ώێ҂ɐݒ�
		_args[3] = _args[4];
		// �����̏ꍇid�ɕϊ�
		if (_args[4] < 4)
			_args[3] = members[_args[4]];

		// ���̑Ώۂ̏ꏊ��_args[4]�ɐݒ�
		for (_args[4]++; _args[4] < 17; _args[4]++) {
			if (_args[4] == 4 || _args[4] >= 16) {
				// �G�������̏I���n�_�܂Ńt�F�b�`�����畉�̐���ݒ�
				_args[4] = -1;
				break;
			}
			if (members[_args[4]] >= 0) {
				break;
			}
		}
	}

	/**
	 * �s���I���֐� (1) ���@���g���Ă����ꍇ�AMP������� (2) �A�C�e�����g���Ă����ꍇ�A�A�C�e���������
	 * (3)�s���҃C�j�V�A�`�u��-1�ɂ��� (4) �s���g�����U�N�V���������Z�b�g���� (5) ���������񂾏ꍇ�A���񂾑�����J��グ�� (6)
	 * �I���������s��
	 *
	 * @param terminate
	 *            false�̏ꍇ�A������v�Z�����A�s���I���͍s��Ȃ�
	 */
	private void endAction(boolean terminate) {
		// �s���҂Ȃ��̏ꍇ�A�����ɏI��
		if (_args[1] < 0) {
			return;
		}

		// MP����
		if ((_args[0] == 2) && (_args[2] < 16)) {
			mps[_args[1]] -= (_args[2] + 10) / 4;
			if (mps[_args[1]] < 0) {
				mps[_args[1]] = 0;
			}
		}

		// �A�C�e������
		if ((_args[0] == 3) && ((iType[_args[2]] & 1024) != 0)
				&& (iAmount[_args[2]] > 0)) {
			if (--iAmount[_args[2]] < 0) {
				iAmount[_args[2]] = 0;
			}

		}

		// ������v�Z����ꍇ�A�����ŏI��
		if (!terminate)
			return;

		// �C�j�V�A�`�u�̏I��
		int mem = id2mem(_args[1]);
		if (mem < 0)
			mem = 5; // �g�D���u�N�g�D�̗V�яI��

		initiatives[mem] -= 100;
		if (initiatives[mem] < 0) {
			// �s���I��

			// �퓬�]���t�F�[�Y optFlg12...15���Ԏ؂肵�A4�Ŋ������]�肪
			// ��̉ӏ�������΂����������ɂ���
			try {
				if (0 != (getAlign(mem2id(_args[1])) & 256)) {
					for (int i = 12; i < 16; i++) {
						optFlg[i] += (optFlg[i] & 4);
					}
				}
			} catch (Exception e) {
			}

			// �S�s�������Z�b�g
			for (int k = 0; k < _args.length; k++) {
				_args[k] = -1;
			}
		}

		// ���������񂾏ꍇ�̑���J�グ
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

		// ���̃A�^�^�^�b�̑Ώۂ�I��
		int atttMem = id2mem(nextAttt);
		if (atttMem >= 0) {
			initiatives[atttMem] = 299;
		}
		nextAttt = -1;

		_sel = 0;
	}

	/* ========================= */
	// I/O���[�e�B���e�B
	/* ========================= */

	private void loadMaster() {
		DataInputStream dis = null;
		DataOutputStream dos = null;
		try {

			if(!r$getVer()){
				return;
			}

			// ���\�[�X�t�@�C����jar ����W�J
			r$open();
			dis = r$load("init.sav");

			byte[] saveData = new byte[SAVE_SIZE];
			dis.read(saveData);
			dis.close();

			dos = r$saveSlot(4 + RES_SIZE);

			dos.write(saveData);
			_load = true;
			r$closeSave(dos);

			// �摜�ǂ݂���
			loadImage();

			dis = r$load("members.data");
			// �L�����f�[�^�̕������}�X�^����ǂݎ��
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
			// ���@�f�[�^�̕������}�X�^����ǂݎ��
			for (int i = 0; i < mgcs.length; i++) {
				mgcs[i] = dis.readUTF();
			}
			dis.close();
			dis = r$load("mevents.str");
			// ���@����ѓG�\�̓f�[�^�̕������}�X�^����ǂݎ��
			// for (int i = 0; i < mgcEvents.length; i++) {
			for (int i = 0; i < MEVENTS_SIZE; i++) {
				mgcEvents[i] = dis.readUTF();
			}
			dis.close();
			// ���j���[�f�[�^�̕������}�X�^����ǂݎ��
			dis = r$load("menuevents.str");
			for (int i = 0; i < menuEvents.length; i++) {
				menuEvents[i] = dis.readUTF();
			}
			dis.close();

			// �A�C�e���f�[�^�������}�X�^����ǂݎ��
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

			// ��̒n�}�f�[�^�������}�X�^����ǂݎ��
			dis = r$load("maptexts.str");
			for (int i = 0; i < 20; i++) {
				mapTexts[i] = dis.readUTF();
			}
			dis.close();

			// ���̍��W�������}�X�^����ǂݎ��
			dis = r$load("raft.data");
			int dataLen = dis.readByte();
			raftTable = new byte[dataLen][4];
			for (int i = 0; i < dataLen; i++) {
				for (int j = 0; j < 4; j++) {
					raftTable[i][j] = dis.readByte();
				}
			}
			dis.close();

			// �A�C�e���E�G�R���v���[�g�t���O�ǂݍ���
			// ���\�[�X�ő�l - 20(long2��+�ŏI�ۑ��X���b�g)�̈ʒu����ǂݍ���
			dis = r$loadSlot(RES_SIZE - 20);
			completeItem = dis.readLong();
			completeMonster = dis.readLong();
			slotIndex = dis.readInt();

			_load = true; // �}�X�^�_�E�����[�h�ς݂Ƃ���
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
	 * jar�A�[�J�C�u����摜�𒊏o���A�K�v������΃t�B���^�[�����܂��܂�
	 *
	 * @return
	 * @throws Exception
	 */
	protected void loadImage() throws Exception {
		DataInputStream dis;
		// �摜�f�[�^��jar����擾
		dis = r$load("chips.gif");
		int dataSize = dis.available();
		byte[] imageData = new byte[dataSize];
		dis.read(imageData);
		dis.close();

		// �摜�t�B���^���K�v�Ȃ̂ł���΁A�����Őݒ�
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
	 * �V�K�n�}��ǂݍ���
	 *
	 * @param isl
	 *            ���ԍ�
	 * @param rgn
	 *            �G���A�ԍ�
	 */
	private void loadMap(int isl, int rgn) {
		DataInputStream dis = null;

		try {
			// �n�}�f�[�^��ǂݍ���
			String mapNo = isl + "" + rgn;

			// �f�[�^��ێ����Ă���Jar�t�@�C��
			r$open();

			// �σf�[�^�̃f�[�^��(�g���܂킵)
			int dataLen = 0;

			// Jar�t�@�C������f�[�^��ǂݏo��object(�g���܂킵)
			// �G�o���X�N���v�g���܂ޒn�}�C�x���g��ǂݍ���
			dis = r$load(mapNo + "mapEvent.str");

			dataLen = dis.readByte();
			mapEvent = new String[dataLen];
			for (int i = 0; i < dataLen; i++) {
				mapEvent[i] = dis.readUTF();
			}
			dis.close();

			// �G���f�B���O�V�[���₨�܂��V�[�����A�C�x���g�f�[�^�ǂݍ��݂݂̂ŏI������
			if (isl == 0) {
				saveList = null;
				return;
			}

			// �n�}����ǂݍ���
			dis = r$load(mapNo + ".map");
			for (int y = 0; y < MAP; y++) {
				for (int x = 0; x < MAP; x++) {
					xyMap[y][x] = dis.readByte();
				}
			}
			dis.close();

			// 5-1�����摜�t�B���^���s��
			int filter = 0;
			if (isl == 5 && rgn == 1) {
				filter = 1;
			}
			if (filter != imageFilter) {
				imageFilter = filter;
				loadImage();
			}
			// ���������}�b�v�N���A
			submaplens = null;
			submaps = null;
			// 5-2�������������}�b�v��ǂݍ���
			if (isl == 5 && rgn == 2) {
				// ���������}�b�v�J���x�ꗗ�ǂݍ���
				dis = r$load("pack.submaplen");
				int submaplenlen = dis.readByte();
				submaplens = new byte[submaplenlen];
				for (int i = 0; i < submaplenlen; i++) {
					submaplens[i] = dis.readByte();
				}
				dis.close();

				// ���������}�b�v�ꗗ�ǂݍ���
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
			// mapChips = new Image[dataLen]; // �`�b�v�f�[�^�쐬
			upChip = new byte[dataLen]; // ��`�b�v�z��쐬
			walkIn = new boolean[dataLen]; // �i���۔z�񐶐�
			eventNo = new byte[dataLen]; // �n�}�C�x���g�z�񐶐�

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

			// �G�p���@�C�x���g��ǂݍ���
			dis = r$load(mapNo + "mevents.str");

			dataLen = dis.readByte();
			for (int i = 0; i < dataLen; i++) {
				mgcEvents[MEVENTS_SIZE + i] = dis.readUTF();
			}
			dis.close();

			// �G�o���p�^�[���ǂݍ���
			dis = r$load(mapNo + ".eptn");

			for (int i = 0; i < 7; i++) {
				for (int j = 0; j < 16; j++) {
					enemyPattern[i][j] = dis.readByte();
				}
			}
			dis.close();

			// �G�f�[�^�ǂݍ���
			dis = r$load(mapNo + ".enm");

			int dataLen2 = dis.readByte();
			for (int i = 0; i < dataLen2; i++) {
				enemyID[i] = dis.readByte();
				for (int j = 0; j < 4; j++) {
					enemyAlgo[i][j] = dis.readByte();
				}
			}
			dis.close();

			// �G�}�X�^�ƃf�[�^���ƍ�
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

			// �X�|�b�g�C�x���g�ǂݍ���
			dis = r$load(mapNo + ".spot");

			dataLen = dis.readByte();
			spotNo = new byte[dataLen][3];
			for (int i = 0; i < dataLen; i++) {
				spotNo[i][0] = dis.readByte();
				spotNo[i][1] = dis.readByte();
				spotNo[i][2] = dis.readByte();
			}

			// �}�b�v�ω��t���O�̏�����
			clearMapFlg();

			// �ėp�t���O�̏�����(�I�v�V�����ȍ~�A�e���|�[�g�t���O�ȑO�̂�)
			for (int i = M_OPT; i < M_TELEPORT; i++) {
				mem[i] = -1;
			}

			// �n�}�̏����X�N���v�g�ǂݍ���
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
	 * �`�b�v�������ƂɃ`�b�v��Image�f�[�^���쐬����
	 *
	 * @param index
	 *            �`�b�v��ݒ肷��z��̍���
	 * @param base
	 *            �w�i�F�̍���
	 * @param half
	 *            �΂ߔw�i�̕����ƐF�̍���
	 * @param obj
	 *            ��l���̉��ɕ`�悳���摜
	 * @param upObj
	 *            ��l���̏�ɕ`�悳���摜
	 */
	private void setChipImage(int index, byte base, byte half, byte obj,
			byte upObj) {
		Object g = g$draw(null, G_SHIFT_BUFFER, index, 0, 0, null);
		// ��{�F�`��
		g$draw(g, G_SET_COLOR, base, 0, 0, null);
		g$draw(g, G_FILL_RECT, 0, 0, CHIP, null);

		// �΂߃`�b�v�`��
		if (half != 0) {
			g$draw(g, G_SET_COLOR, half % 16, 0, 0, null);
			g$draw(g, G_FILL_POLIGON, half / 16, 0, 0, null);
		}
		// �`�b�v�I�u�W�F�N�g�`��
		if (obj != 0) {
			g$draw(g, G_DRAW_CHIP, obj, 0, 0, null);
		}

		// ��`�b�v�I�u�W�F�N�g�`��
		if (upObj != 0) {
			g$draw(g, G_DRAW_CHIP, upObj, 0, 0, null);
			upChip[index] = upObj;
		}

	}

	/**
	 * �Q�[���̏�ԃZ�[�u
	 *
	 */
	private int saveGame(int slot) {
		try {
			DataOutputStream dos;

			// �V�K�X���b�g�̏ꍇ�A�X���b�g�T�C�Y���X�V
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
			// ���݂̎��Ԃ̕ۑ�
			long now = System.currentTimeMillis();
			dos.writeLong(now);
			size += 8;

			// ���ݍ��W�̕ۑ�1
			dos.writeByte((byte) mem[M_ISL]); // ���ԍ��̕ۑ�
			dos.writeByte((byte) mem[M_RGN]); // �n��ԍ��̕ۑ�
			size += 1;
			size += 1;

			// �v���C���Ԃ̕ۑ�
			gameTime += now - startTime;
			dos.writeLong(gameTime);
			size += 8;

			// ��l���L�����̏�ԕێ�
			for (int i = 0; i < 6; i++) {
				dos.writeInt(lvs[i]); // ����Lv�̕ۑ�
				dos.writeInt(hps[i]); // ����HP�̕ۑ�
				dos.writeInt(mps[i]); // ����MP�̕ۑ�
				for (int j = 0; j < 4; j++) {
					dos.writeByte((byte) eqs[i][j]); // ���ݑ����i�̕ۑ�
				}
				size += 4;
				size += 4;
				size += 4;
				size += 4;
			}

			dos.writeInt(exp); // ���݌o���_�̕ۑ�
			dos.writeInt(gem); // ���ݏ������̕ۑ�
			size += 4;
			size += 4;

			// ������̕ۑ�
			for (int i = 0; i < 4; i++) {
				dos.writeByte((byte) members[i]);
				size += 1;
			}

			// ���ݍ��W�̕ۑ�2
			dos.writeByte((byte) mem[M_X]); // x���W�̕ۑ�
			dos.writeByte((byte) mem[M_Y]); // y���W�̕ۑ�
			size += 1;
			size += 1;

			// �������̕ۑ�
			for (int i = 0; i < raftHist.length; i++) {
				dos.writeByte(raftHist[i]);
				size += 1;
			}

			// �e��t���O�̕ۑ�
			// �C�x���g�t���O�̕ۑ�(256+1024bit����̂ŁA8��Int�ɕ���
			for (int i = 0; i < 40; i++) {
				dos.writeInt(mapBits(eventFlg, i));
				size += 4;
			}

			// �A�C�e���ێ����̕ۑ�
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

			// ����N�������X�V
			startTime = now;

			// SaveSlot�ԍ��ێ�
			slotIndex = slot;
			dos = r$saveSlot(RES_SIZE - 4);
			dos.writeInt(slotIndex);
			r$closeSave(dos);

			// �Z�[�u����ύX���邽�߁A�Z�[�u�X���b�g�ꗗ���X�V
			saveList = null;

			return 1;

		} catch (Exception e) {
			System.out.println(e);
		}
		return 0;
	}

	/**
	 * boolean�z����o�C�g�^�ɕϊ����ĕԂ��BSAVE�̎��Ɏg�p
	 *
	 * @param bits
	 *            boolean�z��
	 * @param pos
	 *            �z��ǂݍ��݊J�n�ʒu�B32bit���ƂɎw��
	 * @return �ϊ����ꂽ�o�C�g�^�̒l
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

			// �ۑ������̓ǂݎ��(�̂�)
			dis.readLong();

			// ���ݍ��W�̓Ǎ�
			mem[M_ISL] = dis.readByte(); // ���ԍ��̓Ǎ�
			mem[M_RGN] = dis.readByte(); // �n��ԍ��̓Ǎ�

			// �v���C���Ԃ̓Ǎ�
			gameTime = dis.readLong();

			// ��l���L�����̏�Ԏ擾
			for (int i = 0; i < 6; i++) {
				lvs[i] = dis.readInt(); // ����Lv�̓Ǎ�
				hps[i] = dis.readInt(); // ����HP�̓Ǎ�
				mps[i] = dis.readInt(); // ����MP�̓Ǎ�
				for (int j = 0; j < 4; j++) {
					eqs[i][j] = dis.readByte(); // ���ݑ����i�̓Ǎ�
				}
			}

			exp = dis.readInt(); // ���݌o���_�̓Ǎ�
			gem = dis.readInt(); // ���ݏ������̓Ǎ�

			// ������̓Ǎ�
			for (int i = 0; i < 4; i++) {
				members[i] = dis.readByte();
			}

			// ���ݍ��W�̓Ǎ�
			mem[M_X] = dis.readByte(); // x���W�̓Ǎ�
			mem[M_Y] = dis.readByte(); // y���W�̓Ǎ�

			// ���ړ������̓Ǎ�
			for (int i = 0; i < raftHist.length; i++) {
				raftHist[i] = dis.readByte();
			}

			// �}�b�v�̓ǂݍ��݂̂��ƂɃ}�b�v�t���O���w�肷�邽�߁A
			// ���̈ʒu��loadMap�����s
			loadMap(mem[M_ISL], mem[M_RGN]);

			// �e��t���O�̓Ǎ�
			for (int i = 0; i < 40; i++) {
				// �C�x���g�t���O�̓Ǎ�
				unmapBits(eventFlg, dis.readInt(), i);
			}

			// �A�C�e���ێ����̓Ǎ�
			for (int i = 0; i < ITEM_LEN; i++) {
				iAmount[i] = dis.readInt();
			}

			dis.close();

			// ��l���̌����������ɂ���
			mem[M_QUOTA] = 3;

			// �Q�[���J�n�����̍X�V
			startTime = System.currentTimeMillis();

			// SaveSlot�ԍ��ێ�
			slotIndex = slot;
			saveList = null;

			if (slot == 0)
				debug();
		} catch (Exception e) {

		}
	}

	/**
	 * boolean�z���int�̒l��������B�ǂݍ��݂̍ۂɎg�p�B
	 *
	 * @param bits
	 *            �������boolean�z��
	 * @param map
	 *            �Q�Ƃ���int�^�̃f�[�^
	 * @param pos
	 *            �z��̑���J�n�ʒu�B32bit���ƂɎw��
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
	 * �Z�[�u�X���b�g�ꗗ���ŐV�̏�ԂɍX�V
	 *
	 */
	private void updateSaveList() {
		int count = 1;
		saveList = null;
		try {
			DataInputStream dis = r$loadSlot(RES_SIZE);

			// �Z�[�u�X���b�g���̓ǂݍ���
			count += dis.readInt();

			saveList = new String[count];

			// �ŏ��̃X���b�g���f�t�H���g�f�[�^�X���b�g�Ƃ��ēǂݔ�΂�
			dis.skip(SAVE_SIZE);

			Date date = new Date();
			Calendar time = Calendar.getInstance();
			for (int i = 1; i < count; i++) {
				// ���ԏ��̎擾
				date.setTime(dis.readLong());
				time.setTime(date);

				// ���ݍ��W�̎擾
				byte isl = dis.readByte();
				byte rgn = dis.readByte();

				// �v���C���Ԃ̎擾
				long gameTime = dis.readLong();

				// ���ݎ�l�����x���̎擾
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
		saveList[0] = "�V�K�f�[�^";
	}

	/**
	 * SaveList���Ԏ؂肵�ă����X�^�[�ꗗ���쐬
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

			// �G�f�[�^��_args�Ƀ}�b�s���O����X�N���v�g���}�b�v�C�x���g�ɒ���
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
	 * �Z�[�u�X���b�g���폜
	 *
	 * @param slot
	 *            �폜�ΏۃX���b�g(���݂͑S�폜����)
	 */
	private void clearSave(int slot) {
		try {
			// ���݂�Save�X���b�g�����v��
			DataInputStream dis = null;
			DataOutputStream dos = null;

			dis = r$loadSlot(RES_SIZE);
			int slotSize = dis.readInt();
			dis.close();

			// �w��Save�X���b�g�ȍ~���R�s�[
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

			// Save�X���b�g��1����
			dos = r$saveSlot(RES_SIZE);
			dos.writeInt(slotSize - 1);
			r$closeSave(dos);

			// �Z�[�u����ύX���邽�߁A�Z�[�u�X���b�g�ꗗ���X�V
			saveList = null;
		} catch (Exception e) {
		}
	}

	/**
	 * �A�C�e���}�ӁE�����X�^�[�}�Ӄt���O�ɃN���A���̃f�[�^��ǉ�
	 */
	private void completed() {
		// ����1�ȏ㏊�����Ă���A�C�e�����A�C�e���}�Ӄt���O�ɒǉ�
		for (int i = 0; i < 64; i++) {
			if (getAmount(i) > 0) {
				completeItem |= (1 << i);
			}
		}

		// ���ł��|���������X�^�[�������X�^�[�}�Ӄt���O�ɒǉ�
		for (int i = 0; i < 64; i++) {
			if (eventFlg[F_MONSTER + i]) {
				completeMonster |= (1 << i);
			}
		}

		// �e��}�Ӄt���O���Z�[�u�f�[�^�ɏ�������
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
	 * �󔠉�����̕\�� �w�肵���t���O����t���O�܂ł̊Ԃł�����true�����������𒲍����A������\��
	 *
	 * @param begin
	 *            �󔠃t���O�J�n�ʒu
	 * @param end
	 *            �󔠃t���O�I���ʒu
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
		lines[lindex++] = "�󔠉���� " + (rate / 100) + "."
				+ format(rate % 100, 2, '0') + "% (" + got + "/" + all + ")";
	}

	/**
	 * �퓬�Ȃǂœ���C�x���g������ꍇ�̃C�x���gID���n�[�h�R�[�f�B���O�ŕԂ�
	 *
	 * @return
	 */
	private int specialEvent() {
		// �o���b�^��|�������A�C�x���gID41��Ԃ�
		try {
			if (enemyID[mem2id(_args[3]) - 6] == 13) {
				return 41;
			}
		} catch (Exception e) {
		}
		return -1;
	}

	/* ========================= */
	// �V�[���Ǘ����[�e�B���e�B
	/* ========================= */

	private void setScene(int scene) {
		_scene = scene;

		if (scene == 0) {
			if (mem[M_RGN] > 0) {
				// �G���f�B���O�V�[��
				setSoftLabel(SOFT_KEY_1, "");
				setSoftLabel(SOFT_KEY_2, "");

				loadMap(mem[M_ISL], mem[M_RGN]);
				parsedEvent = mapEvent;
			} else {
				// �I�[�v�j���O�V�[��
				setSoftLabel(SOFT_KEY_1, "�߂�");
				setSoftLabel(SOFT_KEY_2, "D/L");

				// �p�[�X�ΏۃC�x���g���I�[�v�j���O�Ǝ��̂��̂ɐݒ�
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
				lines[0] = "5�̕�";
				lines[1] = "����_�E�����[�h����Ă��܂���";
				lines[2] = "SOFTKEY2�������ĉ�����";
			}

			// �w�i�摜�L���b�V����j��
			useCache = false;
		}

		if (scene == 1) {
			// �t�B�[���h�V�[��
			setSoftLabel(SOFT_KEY_1, "MENU");
			setSoftLabel(SOFT_KEY_2, "");
			int i = 0;
			// ���j���[�p�O���[�o�������̃N���A
			for (i = 0; i < _args.length; i++) {
				_args[i] = -1;
			}
			// �G�ԍ��̃N���A
			for (i = 6; i < members.length; i++) {
				members[i] = -1;
			}
			// �C�j�V�A�e�B�u�E�U���E�h��͏㏸�̃N���A
			for (i = 0; i < initiatives.length; i++) {
				initiatives[i] = -1;
				optAt[i] = 0;
				optDf[i] = 0;
				optFlg[i] = 0;
			}
			// ��l�����ӃI�u�W�F�N�g�̃N���A
			for (i = M_OPT; i < M_OPT + 5; i++) {
				mem[i] = 0;
			}
			// �����̋����ύX�̃N���A
			mem[M_QUOTA_FORCE] = -1;
			// ���R�`����N���A
			for (i = 0; i < freeImage.length; i++) {
				freeImage[i] = 0;
			}

			// �o���_�E�󕨂̃N���A
			winXp = 0;
			winGem = 0;
			winItem = 0;
			winScript = -1;
			// ���@�̕���̉���
			eventFlg[F_MAGIC + 6] = false;
			// ���S�L�����N�^�[��HP1�ŕ���
			for (i = 0; i < 6; i++) {
				if (hps[i] <= 0)
					hps[i] = 1;
			}
			// ��l�������Ȃ��ꍇ�A��l����HP1�ŕ���������
			if (!inMember(0, members, 0)) {
				for (i = 0; i < 4; i++) {
					if (members[i] < 0) {
						members[i] = 0;
						break;
					}
				}
			}
			// �p�[�X�ΏۃC�x���g��mapEvent�ɕύX
			parsedEvent = mapEvent;
		}

		if (scene == 2) {
			// �ʏ탁�j���[�V�[��
			setSoftLabel(SOFT_KEY_1, "�߂�");
			setSoftLabel(SOFT_KEY_2, "");
			// �p�[�X�ΏۃC�x���g��menuEvent�ɕύX
			parsedEvent = menuEvents;
			reparse();
		}

		if (scene == 3) {
			// �퓬�V�[��
			setSoftLabel(SOFT_KEY_1, "�߂�");
			setSoftLabel(SOFT_KEY_2, "");
			// �p�[�X�ΏۃC�x���g��menuEvent�ɕύX
			parsedEvent = menuEvents;
			lines[lindex++] = "�G�����ꂽ!";
			// �z�C�b�X���𑕔����Ă��郁���o�[�͍ŏ��ɍs��
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
			// �g�[�N�V�[��
			setSoftLabel(SOFT_KEY_1, "");
			setSoftLabel(SOFT_KEY_2, "");
			// �p�[�X�ΏۃC�x���g��mapEvent�ɕύX
			parsedEvent = mapEvent;
		}

		if (scene == 5) {
			// ���V�[��
			setSoftLabel(SOFT_KEY_1, "�߂�");
			setSoftLabel(SOFT_KEY_2, "");
			parsedEvent = null;
			for (int i = 0; i < 4; i++) {
				_args[i + 1] = mem[i];
			}
		}

		if (scene == 6) {
			// �f���V�[��
			setSoftLabel(SOFT_KEY_1, "");
			setSoftLabel(SOFT_KEY_2, "");

			// �w�i�摜�L���b�V����j��
			useCache = false;
		}

		if (scene == 7) {
			// ���܂��V�[��
			setSoftLabel(SOFT_KEY_1, "�߂�");
			setSoftLabel(SOFT_KEY_2, "");

			saveList = null;
			clearLines();
			// �p�[�X�ΏۃC�x���g��menuEvent�ɕύX
			parsedEvent = menuEvents;

			// �w�i�摜�L���b�V����j��
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
	 * �\���s�����ׂċ󗓂ɂ��� �X�ɑI�����ʂ̒l�����ׂăf�t�H���g�l�ɂ��� �܂��A���R�`����N���A����
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
	 * �S�}�b�v�t���O����������
	 *
	 */
	private void clearMapFlg() {
		for (int i = 0; i < MAP * MAP; i++) {
			eventFlg[F_MAP + i] = false;
		}
	}

	/**
	 * ��:0 ��:1 �E:2 ��:3 �Ƃ�����������X���AY���̒l�����o��
	 * X�������o���ꍇ�A���������Ă����-1�A�E�������Ă����1�A�㉺�������Ă����0
	 * Y�������o���ꍇ�A��������Ă����-1�A���������Ă����1�A���E�������Ă����0
	 *
	 * @param quota
	 *            ��l���̌����Ă������
	 * @param xy
	 *            0�̎���X���A-1�̎���Y�������o��
	 * @return -1..1�܂ł̒l��Ԃ�
	 */
	private int calcXY(int quota, int xy) {
		return (quota + xy + 1 & 1) * (quota + xy - 1);
	}

	/**
	 * �����Ɏw�肳�ꂽ���� - 1������Ƃ���������Ԃ� random.nextInt(int)�̂Ȃ�������
	 *
	 * @param limit
	 *            �����̏��
	 * @return ����������Ƃ��闐���𐮐��ŕԂ�
	 */
	private int randi(int limit) {
		return (random.nextInt() & Integer.MAX_VALUE) % limit;
	}

	/**
	 * x��y���n�}�͈͓̔����ǂ�����Ԃ�
	 *
	 * @param x
	 *            x���W�̒l
	 * @param y
	 *            y���W�̒l
	 * @return �n�}�͈͓̔����ǂ���
	 */
	private boolean isMap(int x, int y) {
		return ((0 <= x) && (x < MAP) && (0 <= y) && (y < MAP));
	}

	/* ========================= */
	// �`��p���[�e�B���e�B
	/* ========================= */
	public void paintAll(Object g) {
		if (((_scene == 5) && (_args[0] == -2)) || _scene == 7) {
			// �Y����
			paintDrifting();
			showWindow = false;
		}

		// �V�[��1�E6�ŃE�B���h�E���`�悳��Ă���΁A�E�B���h�E������
		// �V�[��1�E6�ȊO�ŃE�B���h�E���`�悳��Ă��Ȃ���΁A�`��
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
			// �I�[�v�j���O�V�[���E�G���f�B���O�V�[���̓f����ʂ�`��
			paintDemo(g);
		} else if (_scene == 7) {
			g$draw(g, G_FLIP_BUFFER, 0, 0, 0, null);
		} else {
			paintField(g);
		}

		if (_scene == 0 || _scene == 7) {
			// �I�[�v�j���O�E�G���f�B���O���Ȃ牽���`�悵�Ȃ�
		} else if (_scene == 3) {
			// �퓬���Ȃ�A�G�L�����A�C�R����`��
			// g$draw(g, G_TRANSLATE, 1, fontH * W_ROW + 2 + W_MARGIN, 0, null);
			for (int i = 6; i < 16; i++) {
				if (members[i] >= 0) {
					paintEnemy(g, i);
				}
			}
		} else {
			// �퓬���łȂ���΁A��l����`��
			paintMan(g);
		}

		// ���`��t���O������΁A����`��
		if (mem[M_RAINBOW] > 0) {
			paintRainbow(g);
		}

		if (showWindow) {
			// ���C���E�B���h�E�����̕`��
			g$draw(g, G_TRANSLATE, W_MARGIN, W_MARGIN, 0, null);
			paintLines(g);

			// �T�u�E�B���h�E�����̕`��
			g$draw(g, G_TRANSLATE, W_MARGIN, getHeight() - W_MARGIN - W_SUB_ROW
					* fontH, 0, null);
			if (_scene == 0) {
				// �I�[�v�j���O�E�G���f�B���O�̓T�u�E�B���h�E��`�悵�Ȃ�
			} else if (_scene == 5) {
				// ���V�[���̎��͍��W�\��
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
			// �{�X�`�b�v�����݂���Ȃ�A�{�X�`�b�v�`��
			if (enemyPattern[6][0] < 0) {
				paintBoss(g, enemyPattern[6][1], enemyPattern[6][2],
						enemyPattern[6][3]);
			}
		}

	}

	private void paintWindowSet() {
		Object g = g$draw(null, G_SHIFT_BUFFER, -1, 0, 0, null);
		// ���C���E�B���h�E�`��
		g$draw(g, G_TRANSLATE, W_MARGIN, W_MARGIN, 0, null);
		paintWindow(g, fontH * W_ROW + 2);

		// �T�u�E�B���h�E�`��
		g$draw(g, G_TRANSLATE, W_MARGIN, getHeight() - W_MARGIN - W_SUB_ROW
				* fontH, 0, null);
		paintWindow(g, fontH * W_SUB_ROW + 2);
	}

	private void paintDrifting() {
		Object g = g$draw(null, G_SHIFT_BUFFER, -1, 0, 0, null);
		// �C�̕`��
		g$draw(g, G_SET_COLOR, 1, 0, 0, null);
		g$draw(g, G_FILL_RECT, 0, 0, getWidth(), null);
		if (_scene == 5) {
			// ���̕`��
			g$draw(g, G_DRAW_CHIP, 22, cntX, cntY, null);
		}
		if (_scene == 7) {
			if (_args[1] == 2) {
				// �A�C�e���ꗗ�̎��͕󔠕`��
				if (_args[2] < 0) {
					g$draw(g, G_DRAW_CHIP, 65, cntX, cntY, null);
				} else {
					g$draw(g, G_DRAW_CHIP, 66, cntX, cntY, null);
				}
			}

			if (_args[1] == 3) {
				// �����X�^�[�ꗗ�̏ꍇ�͟B�������X�^�[��`��
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
			// �L���b�V�����g�p���A�������ړ����Ȃ��Ȃ�A
			// �L���b�V���̉摜�����̂܂܏o�͂��邾��
			g$draw(g, G_FLIP_BUFFER, 0, 0, 0, null);
			return;
		}

		boolean writeCache = ((sftXY & 1) == 0);
		Object gg = g;

		if (writeCache) {
			// sftX��sftY�����ɋ����Ȃ�(���������̃V�t�g�����Ă��Ȃ�)
			// �L���b�V���ɏ�������
			gg = g$draw(g, G_SHIFT_BUFFER, -1, 0, 0, null);
		}

		int dx = sftX * CHIP / -2; // ���ɂ����h�b�g��
		int dy = sftY * CHIP / -2; // �c�ɂ����h�b�g��

		if (useCache) {
			// �L���b�V�����g�p���Ă���ꍇ�A
			// �L���b�V���̃C���[�W���摜�ɔ��f
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
			// �E�B���h�E��`�悷��ɂ�������炸�L���b�V�����g��Ȃ��ꍇ�A
			// ��ʂɃE�B���h�E���ĕ`�悷��
			paintWindowSet();
		}

		if (writeCache) {
			// �L���b�V���ɏ������ޏꍇ�A
			// �L���b�V���̉摜����ʂɔ��f
			g$draw(g, G_FLIP_BUFFER, 0, 0, 0, null);
		}

		useCache = true;
	}

	private void paintMan(Object g) {
		// ��l���摜����������
		int x = mem[M_QUOTA] * 2;
		if (moving) {
			x++;
		}
		// M_QUOTA_FORCE�Ɍ������Z�b�g����Ă���΂��̌����ɋ����ύX
		if (mem[M_QUOTA_FORCE] >= 0) {
			x = mem[M_QUOTA_FORCE] * 2;
		}
		// ��l���`�b�v�͈͓̔��Ȃ�A�`��
		if (0 <= x && x <= 7) {
			g$draw(g, G_DRAW_CHIP, x, cntX, cntY, null);
		}

		// ��l�����ӂ̃I�u�W�F�N�g����������
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

		// ��l���̏�ɂ̂�`�b�v����������
		if (((sftX | sftY) & 1) == 1) {
			// �����ړ����Ă���̂Ȃ�
			paintUpChip(g, mem[M_X] + sftX, mem[M_Y] + sftY, cntX + sftX * CHIP
					/ 2, cntY + sftY * CHIP / 2);
			paintUpChip(g, mem[M_X], mem[M_Y], cntX - sftX * CHIP / 2, cntY
					- sftY * CHIP / 2);
		} else {
			// �ړ����Ă��Ȃ��̂Ȃ�
			paintUpChip(g, mem[M_X] + sftX / 2, mem[M_Y] + sftY / 2, cntX, cntY);
		}
	}

	/**
	 * ��l���̏�ɂ̂�`�b�v��`�悷��
	 *
	 * @param g
	 *            �`��Ώ�
	 * @param x
	 *            �}�b�v��̍��WX
	 * @param y
	 *            �}�b�v��̍��WY
	 * @param pntX
	 *            �`��J�n�n�_�s�N�Z��X
	 * @param pntY
	 *            �`��J�n�n�_�s�N�Z��Y
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
	 * �{�X�G�p�`�b�v��`�悷��(�퓬������у��j���[�\�����ɂ͌���Ȃ�)
	 *
	 * @param g
	 *            �`��Ώ�
	 * @param chip
	 *            �`�b�v�ԍ�
	 * @param x
	 *            �}�b�v��̍��WX
	 * @param y
	 *            �}�b�v��̍��WY
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
	 * �L�����N�^�[�ꗗ�̏���`�悷��
	 *
	 * @param g
	 *            �`��Ώۂ̃O���t�B�b�N�I�u�W�F�N�g
	 * @param list
	 *            �L�����N�^�[�ꗗ��\�����l�̃��X�g
	 * @param offset
	 *            �L�����N�^�[�ꗗ���X�g�����Ԃ��琔���n�߂邩
	 */
	private void paintCharData(Object g, int[] list, int offset) {

		int tabStopH = g$fontWidth(g, "�g�D���u�N�g�D ");
		int tabStopM = g$fontWidth(g, "�g�D���u�N�g�D 999/999 ");
		paintChars(g, "�y���O�z", 0, fontH);
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
		paintChars(g, format(gem, 10, ' ') + "[������]", tabStopH, fontH * 6);
	}

	/**
	 * �L�����N�^�[�̏ڍ׃f�[�^��`�悷��
	 *
	 * @param g
	 *            �`��Ώۂ̃O���t�B�b�N�I�u�W�F�N�g
	 * @param index
	 *            �L�����N�^�[�ԍ�
	 */
	private void paintCharDatum(Object g, int index) {
		g$draw(g, G_DRAW_CHIP, icns[index], 0, 0, null);
		String exline = "";
		if (index == 0) {
			exline = "�o���_  " + exp + "/" + getNextXp(index);
		} else if (getNextXp(index) >= 0) {
			// ���T���f�X�̃��x�����Ⴂ�ꍇ�A����̒������x����
			exline = "�K�v����  " + getNextXp(index);
		} else {
			// ���T���f�X�Ɠ������x���ȏ�̏ꍇ�A�����x�����͕s�\
			exline = "�x���s�v";
		}
		paintChars(g, names[index] + " :Lv" + lvs[index], CHIP + 1, fontH);
		paintChars(g, "HP: " + format(hps[index], 3, ' ') + "/"
				+ format(getMaxHp(index), 3, ' '), CHIP + 1, fontH * 2);
		paintChars(g, "MP: " + format(mps[index], 3, ' ') + "/"
				+ format(getMaxMp(index), 3, ' '), CHIP + 1, fontH * 3);
		paintChars(g, "�U: " + getDefAt(index) + " �h: " + getDefDf(index)
				+ " ��: " + dexs[index], CHIP + 1, fontH * 4);
		paintChars(g, exline, CHIP + 1, fontH * 5);
	}

	/**
	 * �A�C�e���f�[�^�̏ڍׂ�`�悷��
	 *
	 * @param g
	 *            �`��Ώۂ̃O���t�B�b�N�I�u�W�F�N�g
	 * @param index
	 *            �A�C�e���ԍ�
	 */
	private void paintItemDatum(Object g, int index) {
		StringBuffer buff = new StringBuffer();

		buff.append('�y');
		if (index > 0) {
			buff.append(iName[index]);
		}
		buff.append('�z');
		paintChars(g, buff.toString(), 0, fontH);
		buff.setLength(0);

		// �A�C�e����ʂɑ������郉�x����\��
		buff.append('[');
		if (index > 0) {
			if (index == 48) {
				// �o�g���r�L�j�͓������ɍ���
				buff.append("������");
			} else if ((iType[index] & 64) != 0) {
				buff.append(" ���� ");
			} else if ((iType[index] & 128) != 0) {
				buff.append("�@���@");
			} else if ((iType[index] & 256) != 0) {
				buff.append("������");
			} else if ((iType[index] & 512) != 0) {
				buff.append("������");
			} else if ((iType[index] & 1024) != 0) {
				buff.append("���Օi");
			} else {
				buff.append("���̑�");
			}
		} else {
			buff.append("�@�@�@");
		}
		buff.append(']');

		// �����i�̏ꍇ�A�����ł���L������\��
		// �����i�̏ꍇ�A���ʒl��\��
		if (index > 0 && ((iType[index] & (64 + 128 + 256 + 512)) != 0)) {
			for (int i = 0; i < 6; i++) {
				if ((iType[index] & (1 << i)) != 0) {
					buff.append(names[i].charAt(0));
				} else if (index == 48 && i == 2) {
					// �o�g���r�L�j�̏ꍇ�o���b�^�������ɂ���
					buff.append('�o');
				} else {
					buff.append('�@');
				}
			}
			buff.append(" ���ʒl:");
			buff.append(iValue[index]);
		}

		paintChars(g, buff.toString(), 0, fontH * 2);
		buff.setLength(0);

		buff.append("���i:");
		if (index > 0) {
			buff.append(iCost[index]);
		}
		paintChars(g, buff.toString(), 0, fontH * 3);
	}

	/**
	 * �����X�^�[�f�[�^�̏ڍׂ�`�悷��
	 *
	 * @param g
	 *            �`��Ώۂ̃O���t�B�b�N�I�u�W�F�N�g
	 * @param index
	 *            �����X�^�[�ԍ�
	 */
	private void paintMonsterDatum(Object g, int index) {
		StringBuffer buff = new StringBuffer();
		if (index >= saveList.length) {
			// �����@�����X�^�[�̏ꍇ�A�X�e�[�^�X�ڍוs��
			index = -1;
		}

		// �e��X�e�[�^�X�m�F
		String name = "";
		String hpLine = "HP: ";
		String mpLine = "MP: ";
		String atStr = "  ";
		String dfStr = "  ";
		String dxStr = "  ";
		String exLine = "�o���_: ";
		String alLine = "";
		if (index >= 0) {
			name = saveList[index];
			hpLine += _args[4];
			mpLine += (_args[5] / (256 * 256 * 256));
			atStr = format((_args[5] / (256 * 256)) & 0xff, 2, ' ');
			dfStr = format((_args[5] / 256) & 0xff, 2, ' ');
			dxStr = format(_args[5] & 0xff, 2, ' ');

			// �o���_�\��
			exLine += (_args[7] / 256) & 0xff;

			// �푰�\��
			exLine += " �푰:";
			if (_args[8] == 0) {
				exLine += "����";
			} else if (_args[8] == 1) {
				exLine += "�A��";
			} else if (_args[8] == 2) {
				exLine += "�d��";
			} else if (_args[8] == 3) {
				exLine += "���b";
			} else if (_args[8] == 4) {
				exLine += "�}�W�b�N�A�C�e��";
			} else if (_args[8] == 5) {
				exLine += "�A���f�b�h";
			} else if (_args[8] == 6) {
				exLine += "�g���b�v";
			} else if (_args[8] == 7) {
				exLine += "����";
			} else if (_args[8] == 8) {
				exLine += "�l��";
			} else if (_args[8] == 9) {
				exLine += "����";
			}

			// �����\��
			if ((_args[6] & 7) == 1) {
				alLine += "���ɋ��� ";
			}
			if ((_args[6] & 7) == 2) {
				alLine += "���͖��� ";
			}
			if ((_args[6] & 7) == 3) {
				alLine += "���͉� ";
			}
			if ((_args[6] & 7) == 4) {
				alLine += "���Ɏア ";
			}
			if ((_args[6] & 24) == 8) {
				alLine += "�ω��ɋ��� ";
			}
			if ((_args[6] & 24) == 16) {
				alLine += "�ω��Ɏア ";
			}
			if ((_args[6] & 32) == 32) {
				alLine += "�������� ";
			}
			if ((_args[6] & 64) == 64) {
				alLine += "������� ";
			}
			if ((_args[6] & 128) == 128) {
				alLine += "���������� ";
			}
		}
		buff.append("�U:");
		buff.append(atStr);
		buff.append(" �h:");
		buff.append(dfStr);
		buff.append(" ��:");
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
	 * ���ŕʂ̓��ɍs���ۂ̍��W�I��
	 *
	 * @param g
	 */
	private void paintRaftSelect(Object g) {
		// ���x���̕\��
		paintChars(g, " ��   AREA ��    ��", 0, fontH);

		// �I�𗓂̕\��
		StringBuffer line = new StringBuffer();
		for (int i = 1; i <= 4; i++) {
			if (i == _args[0]) {
				line.append(" [");
			} else {
				line.append("  ");
			}

			if (i <= 2) {
				// ���ԍ��A�G���A�ԍ��̏ꍇ
				line.append(String.valueOf(_args[i]));
			} else {
				// X���W�AY���W�̏ꍇ
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
			// ���R�`�悪�L���Ȃ�A���R�`��
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
	// Doja3.5�݂̂Ŏg�p����O���t�B�b�N�p���\�b�h
	// �}���`�v���b�g�t�H�[���\�[�X�ł͕ʂ̓��e�ɂȂ�
	/* ========================= */

	/**
	 * �I�[�v�j���O�E�G���f�B���O�f����ʂ�`��
	 *
	 * @param g
	 *            �`��ΏۃO���t�B�b�N�I�u�W�F�N�g
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
	 * ���������̃E�B���h�E��`��
	 *
	 * @param g
	 *            �`��ΏۃO���t�B�b�N�I�u�W�F�N�g
	 * @param wHeight
	 *            �E�B���h�E�̍���
	 */

	protected void paintWindow(Object gg, int wHeight) {
		// �E�B���h�E��`��
		int[] pixels = new int[wWidth * wHeight];

		Graphics g = (Graphics) gg;
		g.getRGBPixels(0, 0, wWidth, wHeight, pixels, 0);
		for (int i = 0; i < pixels.length; i++) {
			pixels[i] = (pixels[i] >> 1) & 0x7F7F7F;
		}
		g.setRGBPixels(0, 0, wWidth, wHeight, pixels, 0);
	}

	/**
	 * �w�i�ɓ���`��
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
	// i�A�v���݊����C���[
	// �}���`�v���b�g�t�H�[���\�[�X�ł͌Ăяo�����̊e���\�b�h���Ăяo��
	/* ========================= */

	// �O���t�B�b�N�֘A
	protected void g$createImage(byte[] imageData) throws Exception {
		// ������null�Ȃ�L���b�V���pImage�f�[�^�̍쐬
		if(imageData == null){
			bg = Image.createImage(getWidth(), getHeight());
			return;
		}
		// null�łȂ��Ȃ�`�b�v�摜�̓ǂݍ���
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
			// �摜�S�̂����炷
			g.copyArea(0, 0, 240, 240, a1, a2);
		}
		if (command == FICanvas.G_FILL_POLIGON) {
			// �O�p�`�`��
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

	// ���\�[�X�֘A
	/**
	 * ����_�E�����[�h�B���\�[�X�Ɏ��܂肫��Ȃ��摜�f�[�^���T�[�o���� �_�E�����[�h���A�X�N���b�`�p�b�h�ɕۑ�����
	 *
	 * @return �_�E�����[�h���� : true ���s : false
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

			// ���\�[�X�f�[�^�������݁B�������A���\�[�X�o�[�W����(2byte)��
			// ���\�[�X�f�[�^�T�C�Y(4byte)���̌v6byte�͓ǂݔ�΂�
			dos = r$saveSlot(6);
			process = 5;

			// ���\�[�X�̃f�[�^�T�C�Y���擾
			resSize = (int) http.getLength();
			process = 6;

			// 1��Ɏ擾�ł���f�[�^�T�C�Y
			int available = dis.available();
			process = 7;

			// ���\�[�X�f�[�^���擾
			byte[] imgData = new byte[available];
			while (dis.read(imgData) >= 0) {
				dos.write(imgData);
			}
			process = 8;
			dis.close();
			process = 9;
			dos.close();
			process = 10;

			// �_�E�����[�h�������\�[�X�t�@�C����ǂݍ���
			r$open();
			process = 11;

			// �Ăя������݃I�[�v��
			dos = r$saveSlot(0);
			process = 12;

			// ���\�[�X�o�[�W��������������
			dis = r$load(".ver");
			process = 13;
			int resVer = dis.readChar();
			process = 14;
			dis.close();
			dos.writeChar(resVer);
			System.out.println("size=" + resSize + ":resver=" + resVer);
			process = 15;

			// ���\�[�X�f�[�^�T�C�Y����������
			dos.writeInt(resSize);
			dos.close();
			process = 16;

			// ���\�[�X�̏����Z�[�u�t�@�C����ǂݎ��
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

			// �_�E�����[�h�ɐ���������true��ԋp
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
		// Jar�f�[�^��ǂݍ���
		DataInputStream dis = Connector.openDataInputStream("scratchpad:///0");

		// �X�N���b�`�p�b�h�̐擪�̃��\�[�X�o�[�W�������v���O�����̃o�[�W������
		// ��v���Ă��Ȃ���΁A����ǂݍ��݂�����Ă��Ȃ��Ƃ݂Ȃ���
		// �f�[�^���T�[�o����_�E�����[�h
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

		// ���\�[�X�T�C�Y���܂��擾���Ă��Ȃ���΁A���̒i�K�Ŏ擾
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
