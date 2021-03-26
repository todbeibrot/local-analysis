package la;

import featurecat.lizzie.Lizzie;
import featurecat.lizzie.rules.BoardHistoryList;
import featurecat.lizzie.rules.BoardHistoryNode;
import featurecat.lizzie.rules.Stone;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.util.Arrays;

public class LocalAnalysis {

  // static variables:
  private static final int BOARDSIZE =
      19; // it's currently not planned to support other board sizes

  private enum Area { // we will split the board into disjoint areas
    EMPTY,
    PROTECTEDAREA,
    MAINAREABLACK,
    MAINAREAWHITE,
    EYESPACEBLACK,
    EYESPACEWHITE,
    SEPARATIONGROUPS
  }

  private static final char[] alphabet = {
    'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
    'u'
  };

  // areas:
  // TODO find a way to return to board position
  // private final Stone[] boardCopy;	//copy of board position before analysis
  private Stone[] board; // board position which we will change during analysis
  private Area[] area;

  // points:
  private int[] startCoordinates; // two points which we started with
  private int[]
      wallCornerPoint; // Corner Points for wall. 0=left bottom,1=right bottom,2=left top,3=right
  // top
  private int[]
      alivePoints; // stones which you can mark. at the start of the analysis they will connect to a
  // living group on the outside.
  // they represent the possibility to run out with a group. so the inner player is save if he
  // reaches this spot.
  private int changingPoint; // point there we changed the last stone to get an even position.
  // if we want to change more stones, we will search first in the neighborhood of this field
  private int eyeSpaceBlackPoint; // just needed if we want to change Ko Threats.
  private int eyeSpaceWhitePoint;

  // variables
  private int maxAlivePoints;
  private int
      wallDistance; // between analyzed position and surrounding stones there will be a void so the
  // wall will not affect the local position
  private int blackKoThreats; // builds Ko threats for Black if positive and for White if negative
  private int
      maxKoThreatsBlack; // maximal amount of Ko threats for Black. depends on size of Whites eye
  // area.
  private int maxKoThreatsWhite; // analog for White
  private int
      mirror; // if some bugs appear we can mirror the position then start again and mirror back,
  // range from 0 to 7
  private boolean blackToMove; // true = Blacks turn, false = Whites turn
  private boolean
      playerInsideBlack; // surrounded player who tries to live. wall will be in the opposite
  // colour. true=Black, false=White
  private boolean placeBlackStones; // color of single placed stones. true=black,false=white
  private boolean
      placeStonesAlternating; // color of single placed stones alternating or not. will change
  // placeBlackStones if true.
  private boolean
      placeLivingGroupesVertical; // in this case a living group will be a 5x3-block of stones. for
  // false it's a 3x5-block.
  private boolean allowKo;

  private int dummy;
  private boolean dummy2;

  public LocalAnalysis(
      String coordinate1,
      String coordinate2,
      int playerInside,
      int playerToMove,
      int blackKoThreats2) {
    int a = 0;
    String[] letters = new String[2];
    String[] numbers = new String[2];
    startCoordinates = new int[2];

    // convert String coordinates to Int
    String[] splittedCoordinates = {coordinate1, coordinate2};
    if (splittedCoordinates.length != 2) {
      // ERROR
    }

    for (int i = 0; i < 2; i++) {
      numbers[i] = splittedCoordinates[i].replaceAll("[^\\.0123456789]", "");
      letters[i] = splittedCoordinates[i].replaceAll("[^\\.abcdefghjklmnopqrst]", "");
    }
    // TODO ueberpruefen, ob eingabe sinn macht

    for (int i = 0; i < 2; i++) {
      startCoordinates[i] = Integer.parseInt(numbers[i]) - 1;
      startCoordinates[i] *= BOARDSIZE;
      startCoordinates[i] += (int) (-'a' + letters[i].charAt(0));
      if (startCoordinates[i] % BOARDSIZE >= 9 || letters[i].charAt(0) == 't') {
        startCoordinates[i]--; // cause i is missing in alphabet
      }
    }

    if (Lizzie.frame.localAnalysisFrame.board.inAnalysisMode())
      Lizzie.frame.localAnalysisFrame.board.toggleAnalysis(); // stop ponder while initiation
    loadConfig(playerInside, playerToMove, blackKoThreats2);

    // TODO: check if 19x19-board;

    placeStonesAlternating = false;
    area = new Area[BOARDSIZE * BOARDSIZE];
    for (int i = 0; i < BOARDSIZE * BOARDSIZE; i++) {
      area[i] = Area.EMPTY;
    }
    copyBoard();
    protectFieldSquareAndSetWall(
        startCoordinates[0],
        startCoordinates[1]); // protects the selected area and sets corner points for the wall
    clearBoard(); // clear board except selected area

    buildEyesForWallAndAlivePoints(); // save marked stones and get wall alive

    // set main areas
    placeBlackStones = true;
    for (int i = 0; i < BOARDSIZE / 2 + BOARDSIZE % 2; i++) { // black
      for (int j = 0; j < BOARDSIZE; j++) {
        a = j * BOARDSIZE + i;
        if (area[a] != Area.PROTECTEDAREA) {
          area[a] = Area.MAINAREABLACK;
          board[a] = Stone.BLACK;
        }
      }
    }

    placeBlackStones = false;
    for (int i = BOARDSIZE / 2 + BOARDSIZE % 2; i < BOARDSIZE; i++) { // white
      for (int j = 0; j < BOARDSIZE; j++) {
        a = j * BOARDSIZE + i;
        if (area[a] != Area.PROTECTEDAREA) {
          area[a] = Area.MAINAREAWHITE;
          board[a] = Stone.WHITE;
        }
      }
    }
    changingPoint = BOARDSIZE * BOARDSIZE;
    separateMainAreas(); // separates main areas from rest of board
    createEyeSpace(); // searches eye space for main areas
    createEyes(true); // builds eyes for main areas
    createEyes2(); // builds eyes for parts of main area which arn't connected to the main part
    // for example if the main area gets divided in two parts by the Separation group
    libertyCheck(); // check if we have a legal board position
    buildRealBoard(); // place the stones so we can see it

    placeBlackStones = Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK);
    Lizzie.frame.localAnalysisFrame.board.toggleAnalysis(); // stop ponder while initiation
  }

  public boolean isAllowKo() {
    return allowKo;
  }

  public boolean isBlackToMove() {
    return blackToMove;
  }

  public boolean isPlayerInsideBlack() {
    return playerInsideBlack;
  }

  public boolean isPlaceBlackStones() {
    return placeBlackStones;
  }

  public int getWallDistance() {
    return wallDistance;
  }

  // change settings:
  public void changeBlackToMove() {
    blackToMove = !blackToMove;
    refreshBoard();
  }

  public void changePlaceStonesAlternating() {
    placeStonesAlternating = !placeStonesAlternating;
  }

  public void changePlayerInsideBlack() {
    playerInsideBlack = !playerInsideBlack;
    restart();
  }

  public void changePlaceBlackStones() {
    placeBlackStones = !placeBlackStones;
  }

  public void changePlaceLivingGroupesVertical() {
    placeLivingGroupesVertical = !placeLivingGroupesVertical;
  }

  public void changeAllowKo() {
    allowKo = !allowKo;
    restart();
  }

  public void changeBlackKoThreats(int k) {
    setBlackKoThreats(blackKoThreats + k);
  }

  public void setBlackKoThreatsMax() {
    setBlackKoThreats(maxKoThreatsBlack);
  }

  public void setBlackKoThreatsMin() {
    setBlackKoThreats(-maxKoThreatsWhite);
  }

  /** @param k Number of new Ko threats, negative for White Ko threats */
  public void setBlackKoThreats(int k) {
    if (mirror != 0) {
      restart();
    }
    if (!allowKo) {
      blackKoThreats = 0;
    }
    if (k > maxKoThreatsBlack) {
      blackKoThreats = maxKoThreatsBlack;
    }
    if (-k > maxKoThreatsWhite) {
      blackKoThreats = -maxKoThreatsWhite;
    } else {
      blackKoThreats = k;
    }
    createEyes(false);
    refreshBoard();
  }

  // tools to change the position:

  // TODO max for w
  public void setWallDistance(int w) {
    wallDistance = w;
    restart();
  }

  //	public void protectField(int a) {
  //		area[a] = Area.PROTECTEDAREA;
  //	}
  //
  //	public void unprotectField( int a ) {
  //		area[a] = Area.EMPTY;
  //	}
  //

  public void setStoneOnRealBoard(int a[]) {
    setStoneOnRealBoard(translateCoordinates(a));
  }

  public void deleteStoneOnRealBoard(int a[]) {
    deleteStoneOnRealBoard(translateCoordinates(a));
  }

  public void setStoneLineOnRealBoard(int a[], int b[]) {
    setStoneLineOnRealBoard(translateCoordinates(a), translateCoordinates(b));
  }

  public void setGroupHorizontalOnRealBoard(int a[]) {
    setGroupHorizontalOnRealBoard(translateCoordinates(a));
  }

  public void setGroupVerticalOnRealBoard(int a[]) {
    setGroupVerticalOnRealBoard(translateCoordinates(a));
  }

  public void setStoneOnRealBoard(int a) {
    if (placeBlackStones) {
      if (board[a] == Stone.BLACK) {
        deleteStone(a);
      } else {
        setStone(a);
      }
    } else {
      if (board[a] == Stone.WHITE) {
        deleteStone(a);
      } else {
        setStone(a);
      }
    }
    refreshBoard();
  }

  public void deleteStoneOnRealBoard(int a) {
    deleteStone(a);
    refreshBoard();
  }

  public void setStoneLineOnRealBoard(int a, int b) {
    setStoneLine(a, b);
    refreshBoard();
  }

  public void setGroupHorizontalOnRealBoard(int a) {
    placeLivingGroupesVertical = false;
    setUnprotectedLivingGroup(a);
    refreshBoard();
  }

  public void setGroupVerticalOnRealBoard(int a) {
    placeLivingGroupesVertical = true;
    setUnprotectedLivingGroup(a);
    refreshBoard();
  }

  /** place a single stone at a, overwrite a if there is already a stone */
  private void setStone(int a) {
    if (a < 0 || a >= BOARDSIZE * BOARDSIZE) {
      return;
    }
    if (placeBlackStones) {
      board[a] = Stone.BLACK;
    } else {
      board[a] = Stone.WHITE;
    }
    if (placeStonesAlternating) {
      placeBlackStones = !placeBlackStones;
    }
  }

  /** deletes a stone at a */
  private void deleteStone(int a) {
    if (a < 0 || a >= BOARDSIZE * BOARDSIZE) {
      return;
    }
    board[a] = Stone.EMPTY;
  }

  /** places a single stone at a and protects it */
  private void setProtectedStone(int a) {
    if (a < 0 || a > BOARDSIZE * BOARDSIZE) {
      return;
    }
    if (placeBlackStones) {
      board[a] = Stone.BLACK;
    } else {
      board[a] = Stone.WHITE;
    }
    if (placeStonesAlternating) {
      placeBlackStones = !placeBlackStones;
    }
    area[a] = Area.PROTECTEDAREA;
  }

  // TODO more dynamic
  /** places stones between a and b and protects them. */
  private void setStoneLine(int a, int b) {
    placeStonesAlternating = false;
    if (xCoord(a) == xCoord(b)) {
      int xa = xCoord(a);
      int max = Math.max(yCoord(a), yCoord(b));
      for (int i = Math.min(yCoord(a), yCoord(b)); i <= max; i++) {
        setProtectedStone(getField(xa, i));
      }
    }
    if (yCoord(a) == yCoord(b)) {
      int ya = yCoord(a);
      int max = Math.max(xCoord(a), xCoord(b));
      for (int i = Math.min(xCoord(a), xCoord(b)); i <= max; i++) {
        setProtectedStone(getField(i, ya));
      }
    }
  }

  /** sets area of fields between a and b to areatype. */
  private void setAreaLine(int a, int b, Area areatype) {
    placeStonesAlternating = false;
    if (xCoord(a) == xCoord(b)) {
      int xa = xCoord(a);
      int max = Math.max(yCoord(a), yCoord(b));
      for (int i = Math.min(yCoord(a), yCoord(b)); i <= max; i++) {
        area[getField(xa, i)] = areatype;
      }
    }
    if (yCoord(a) == yCoord(b)) {
      int ya = yCoord(a);
      int max = Math.max(xCoord(a), xCoord(b));
      for (int i = Math.min(xCoord(a), xCoord(b)); i <= max; i++) {
        area[getField(i, ya)] = areatype;
      }
    }
  }

  /** places a living group around a and protects it */
  private void setLivingGroup(int a) {
    placeStonesAlternating = false;
    // if a is located in a corner
    if (a == 0) {
      setProtectedStone(0);
      area[1] = Area.PROTECTEDAREA;
      deleteStone(1);
      setProtectedStone(2);
      setProtectedStone(2 + BOARDSIZE);
      setProtectedStone(1 + BOARDSIZE);
      area[BOARDSIZE] = Area.PROTECTEDAREA;
      deleteStone(BOARDSIZE);
      setProtectedStone(1 + 2 * BOARDSIZE);
      setProtectedStone(2 * BOARDSIZE);
      return;
    }
    if (a == BOARDSIZE - 1) {
      setProtectedStone(BOARDSIZE - 1);
      area[BOARDSIZE - 2] = Area.PROTECTEDAREA;
      deleteStone(BOARDSIZE - 2);
      setProtectedStone(BOARDSIZE - 3);
      setProtectedStone(2 * BOARDSIZE - 3);
      setProtectedStone(2 * BOARDSIZE - 2);
      area[2 * BOARDSIZE - 1] = Area.PROTECTEDAREA;
      deleteStone(2 * BOARDSIZE - 1);
      setProtectedStone(3 * BOARDSIZE - 2);
      setProtectedStone(3 * BOARDSIZE - 1);
      return;
    }
    if (a == BOARDSIZE * BOARDSIZE - BOARDSIZE) {
      setProtectedStone(BOARDSIZE * BOARDSIZE - BOARDSIZE);
      area[BOARDSIZE * BOARDSIZE - BOARDSIZE + 1] = Area.PROTECTEDAREA;
      deleteStone(BOARDSIZE * BOARDSIZE - BOARDSIZE + 1);
      setProtectedStone(BOARDSIZE * BOARDSIZE - BOARDSIZE + 2);
      setProtectedStone(BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE + 2);
      setProtectedStone(BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE + 1);
      area[BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE] = Area.PROTECTEDAREA;
      deleteStone(BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE);
      setProtectedStone(BOARDSIZE * BOARDSIZE - 3 * BOARDSIZE + 1);
      setProtectedStone(BOARDSIZE * BOARDSIZE - 3 * BOARDSIZE);
      return;
    }
    if (a == BOARDSIZE * BOARDSIZE - 1) {
      setProtectedStone(BOARDSIZE * BOARDSIZE - 1);
      area[BOARDSIZE * BOARDSIZE - 2] = Area.PROTECTEDAREA;
      deleteStone(BOARDSIZE * BOARDSIZE - 2);
      setProtectedStone(BOARDSIZE * BOARDSIZE - 3);
      setProtectedStone(BOARDSIZE * BOARDSIZE - BOARDSIZE - 3);
      setProtectedStone(BOARDSIZE * BOARDSIZE - BOARDSIZE - 2);
      area[BOARDSIZE * BOARDSIZE - BOARDSIZE - 1] = Area.PROTECTEDAREA;
      deleteStone(BOARDSIZE * BOARDSIZE - BOARDSIZE - 1);
      setProtectedStone(BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE - 2);
      setProtectedStone(BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE - 1);
      return;
    }
    // if a is located at the edge
    if (a < BOARDSIZE) {
      for (int i = xCoord(a) - 2; i <= xCoord(a) + 2; i++) {
        for (int j = yCoord(a); j <= yCoord(a) + 1; j++) {
          if (j != yCoord(a)
              || (i != xCoord(a) + 1 && i != xCoord(a) - 1) && i >= 0 && i < BOARDSIZE) {
            setProtectedStone(getField(i, j));
          } else {
            if (i >= 0 && i < BOARDSIZE) {
              area[getField(i, j)] = Area.PROTECTEDAREA;
              deleteStone(getField(i, j));
            }
          }
        }
      }
      return;
    }
    if (a % BOARDSIZE == 0) {
      for (int i = xCoord(a); i <= xCoord(a) + 1; i++) {
        for (int j = yCoord(a) - 2; j <= yCoord(a) + 2; j++) {
          if (i != xCoord(a)
              || (j != yCoord(a) + 1 && j != yCoord(a) - 1) && j >= 0 && j < BOARDSIZE) {
            setProtectedStone(getField(i, j));
          } else {
            if (j >= 0 && j < BOARDSIZE) {
              area[getField(i, j)] = Area.PROTECTEDAREA;
              deleteStone(getField(i, j));
            }
          }
        }
      }
      return;
    }
    if (a % BOARDSIZE == BOARDSIZE - 1) {
      for (int i = xCoord(a) - 1; i <= xCoord(a); i++) {
        for (int j = yCoord(a) - 2; j <= yCoord(a) + 2; j++) {
          if (i != xCoord(a)
              || ((j != yCoord(a) + 1) && (j != yCoord(a) - 1)) && j >= 0 && j < BOARDSIZE) {
            setProtectedStone(getField(i, j));
          } else {
            if (j >= 0 && j < BOARDSIZE) {
              area[getField(i, j)] = Area.PROTECTEDAREA;
              deleteStone(getField(i, j));
            }
          }
        }
      }
      return;
    }
    if (a >= BOARDSIZE * BOARDSIZE - BOARDSIZE) {
      for (int i = xCoord(a) - 2; i <= xCoord(a) + 2; i++) {
        for (int j = yCoord(a) - 1; j <= yCoord(a); j++) {
          if (j != yCoord(a)
              || (i != xCoord(a) + 1 && i != xCoord(a) - 1) && i >= 0 && i < BOARDSIZE) {
            setProtectedStone(getField(i, j));
          } else {
            if (i >= 0 && i < BOARDSIZE) {
              area[getField(i, j)] = Area.PROTECTEDAREA;
              deleteStone(getField(i, j));
            }
          }
        }
      }
      return;
    }
    // if a is not in a corner or at the edge
    if (!placeLivingGroupesVertical) {
      for (int i = xCoord(a) - 1; i <= xCoord(a) + 1; i++) {
        for (int j = yCoord(a) - 2; j <= yCoord(a) + 2; j++) {
          if (i != xCoord(a)
              || ((j != yCoord(a) + 1) && (j != yCoord(a) - 1) && j >= 0 && j < BOARDSIZE)) {
            setProtectedStone(getField(i, j));
          } else {
            if (isFieldInBoard(i, j)) {
              area[getField(i, j)] = Area.PROTECTEDAREA;
              deleteStone(getField(i, j));
            }
          }
        }
      }
      return;
    } else {
      for (int i = xCoord(a) - 2; i <= xCoord(a) + 2; i++) {
        for (int j = yCoord(a) - 1; j <= yCoord(a) + 1; j++) {
          if (j != yCoord(a)
              || (i != xCoord(a) + 1 && i != xCoord(a) - 1) && i >= 0 && i < BOARDSIZE) {
            setProtectedStone(getField(i, j));
          } else {
            if (isFieldInBoard(i, j)) {
              area[getField(i, j)] = Area.PROTECTEDAREA;
              deleteStone(getField(i, j));
            }
          }
        }
      }
      return;
    }
  }

  /** places a living group around a without protecting it */
  private void setUnprotectedLivingGroup(int a) {
    placeStonesAlternating = false;
    // if a is located in a corner
    if (a == 0) {
      setStone(0);
      deleteStone(1);
      setStone(2);
      setStone(2 + BOARDSIZE);
      setStone(1 + BOARDSIZE);
      deleteStone(BOARDSIZE);
      setStone(1 + 2 * BOARDSIZE);
      setStone(2 * BOARDSIZE);
      return;
    }
    if (a == BOARDSIZE - 1) {
      setStone(BOARDSIZE - 1);
      deleteStone(BOARDSIZE - 2);
      setStone(BOARDSIZE - 3);
      setStone(2 * BOARDSIZE - 3);
      setStone(2 * BOARDSIZE - 2);
      deleteStone(2 * BOARDSIZE - 1);
      setStone(3 * BOARDSIZE - 2);
      setStone(3 * BOARDSIZE - 1);
      return;
    }
    if (a == BOARDSIZE * BOARDSIZE - BOARDSIZE) {
      setStone(BOARDSIZE * BOARDSIZE - BOARDSIZE);
      deleteStone(BOARDSIZE * BOARDSIZE - BOARDSIZE + 1);
      setStone(BOARDSIZE * BOARDSIZE - BOARDSIZE + 2);
      setStone(BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE + 2);
      setStone(BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE + 1);
      deleteStone(BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE);
      setStone(BOARDSIZE * BOARDSIZE - 3 * BOARDSIZE + 1);
      setStone(BOARDSIZE * BOARDSIZE - 3 * BOARDSIZE);
      return;
    }
    if (a == BOARDSIZE * BOARDSIZE - 1) {
      setStone(BOARDSIZE * BOARDSIZE - 1);
      deleteStone(BOARDSIZE * BOARDSIZE - 2);
      setStone(BOARDSIZE * BOARDSIZE - 3);
      setStone(BOARDSIZE * BOARDSIZE - BOARDSIZE - 3);
      setStone(BOARDSIZE * BOARDSIZE - BOARDSIZE - 2);
      deleteStone(BOARDSIZE * BOARDSIZE - BOARDSIZE - 1);
      setStone(BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE - 2);
      setStone(BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE - 1);
      return;
    }
    // if a is located at the edge
    if (a < BOARDSIZE) {
      for (int i = xCoord(a) - 2; i <= xCoord(a) + 2; i++) {
        for (int j = yCoord(a); j <= yCoord(a) + 1; j++) {
          if (j != yCoord(a)
              || (i != xCoord(a) + 1 && i != xCoord(a) - 1) && i >= 0 && i < BOARDSIZE) {
            setStone(getField(i, j));
          } else {
            if (i >= 0 && i < BOARDSIZE) {
              deleteStone(getField(i, j));
            }
          }
        }
      }
      return;
    }
    if (a % BOARDSIZE == 0) {
      for (int i = xCoord(a); i <= xCoord(a) + 1; i++) {
        for (int j = yCoord(a) - 2; j <= yCoord(a) + 2; j++) {
          if (i != xCoord(a)
              || (j != yCoord(a) + 1 && j != yCoord(a) - 1) && j >= 0 && j < BOARDSIZE) {
            setStone(getField(i, j));
          } else {
            if (j >= 0 && j < BOARDSIZE) {
              deleteStone(getField(i, j));
            }
          }
        }
      }
      return;
    }
    if (a % BOARDSIZE == BOARDSIZE - 1) {
      for (int i = xCoord(a) - 1; i <= xCoord(a); i++) {
        for (int j = yCoord(a) - 2; j <= yCoord(a) + 2; j++) {
          if (i != xCoord(a)
              || ((j != yCoord(a) + 1) && (j != yCoord(a) - 1)) && j >= 0 && j < BOARDSIZE) {
            setStone(getField(i, j));
          } else {
            if (j >= 0 && j < BOARDSIZE) {
              deleteStone(getField(i, j));
            }
          }
        }
      }
      return;
    }
    if (a >= BOARDSIZE * BOARDSIZE - BOARDSIZE) {
      for (int i = xCoord(a) - 2; i <= xCoord(a) + 2; i++) {
        for (int j = yCoord(a) - 1; j <= yCoord(a); j++) {
          if (j != yCoord(a)
              || (i != xCoord(a) + 1 && i != xCoord(a) - 1) && i >= 0 && i < BOARDSIZE) {
            setStone(getField(i, j));
          } else {
            if (i >= 0 && i < BOARDSIZE) {
              deleteStone(getField(i, j));
            }
          }
        }
      }
      return;
    }
    // if a is not in a corner or at the edge
    if (placeLivingGroupesVertical) {
      for (int i = xCoord(a) - 1; i <= xCoord(a) + 1; i++) {
        for (int j = yCoord(a) - 2; j <= yCoord(a) + 2; j++) {
          if (i != xCoord(a)
              || ((j != yCoord(a) + 1) && (j != yCoord(a) - 1) && j >= 0 && j < BOARDSIZE)) {
            setStone(getField(i, j));
          } else {
            if (isFieldInBoard(i, j)) {
              deleteStone(getField(i, j));
            }
          }
        }
      }
      return;
    } else {
      for (int i = xCoord(a) - 2; i <= xCoord(a) + 2; i++) {
        for (int j = yCoord(a) - 1; j <= yCoord(a) + 1; j++) {
          if (j != yCoord(a)
              || (i != xCoord(a) + 1 && i != xCoord(a) - 1) && i >= 0 && i < BOARDSIZE) {
            setStone(getField(i, j));
          } else {
            if (isFieldInBoard(i, j)) {
              deleteStone(getField(i, j));
            }
          }
        }
      }
      return;
    }
  }

  /**
   * mark points so they get connected to a living group on the outside. use again remove the mark.
   */
  public void markAlive(int a) {
    for (int i = 0; i < maxAlivePoints; i++) {
      if (alivePoints[i] == BOARDSIZE * BOARDSIZE + 1) {
        alivePoints[i] = a;
        return;
      }
      if (a == alivePoints[i]) {
        alivePoints[i] = BOARDSIZE * BOARDSIZE + 1;
        return;
      } else {
        // Fehlerausgabe: zu viele alivepoints gesetzt
        return;
      }
    }
  }

  // TODO: search for points of PROTECETEDAREA and SEPARATIONAREA which can be changed
  /**
   * moves the border between main areas to change the point difference. if positive Black gains p
   * points. else he looses p points.
   *
   * @param p number of points Black gains and White looses. If negative Black looses p points and
   *     White wins p Points
   */
  public void moveBorder(int p) {
    if (p == 0) {
      refreshBoard();
      return;
    }
    if (changingPoint == BOARDSIZE * BOARDSIZE) {
      if (p > 0) {
        for (int i = 0; i < BOARDSIZE * BOARDSIZE; i++) {
          if (area[i] == Area.MAINAREAWHITE) {
            if (check4Neighborhood(i, Area.MAINAREABLACK)
                && area[i] != Area.EYESPACEWHITE
                && !check4Neighborhood(i, Area.SEPARATIONGROUPS)
                && !check4Neighborhood(i, Area.PROTECTEDAREA)) {
              changingPoint = i;
              placeBlackStones = true;
              setStone(changingPoint);
              area[changingPoint] = Area.MAINAREABLACK;
              if (localLibertyCheck(changingPoint)) {
                moveBorder(p - 1);
                return;
              } else {
                area[changingPoint] = Area.MAINAREAWHITE;
                placeBlackStones = false;
                setStone(changingPoint);
                placeBlackStones = true;
              }
            }
          }
        }
        printError(39);
        return;
        // ERROR kein punkt gefunden, um grenze zu verschieben
      }
      if (p < 0) {
        for (int i = 0; i < BOARDSIZE * BOARDSIZE; i++) {
          if (area[i] == Area.MAINAREABLACK) {
            if (check4Neighborhood(i, Area.MAINAREAWHITE)
                && area[i] != Area.EYESPACEBLACK
                && !check4Neighborhood(i, Area.SEPARATIONGROUPS)
                && !check4Neighborhood(i, Area.PROTECTEDAREA)) {
              changingPoint = i;
              placeBlackStones = false;
              setStone(changingPoint);
              area[changingPoint] = Area.MAINAREAWHITE;
              if (localLibertyCheck(changingPoint)) {
                moveBorder(p + 1);
                return;
              } else {
                area[changingPoint] = Area.MAINAREABLACK;
                placeBlackStones = true;
                setStone(changingPoint);
                placeBlackStones = false;
              }
            }
          }
        }
        // ERROR kein punkt gefunden, um grenze zu verschieben
        printError(39);
        return;
      }
    }
    if (p > 0) {
      placeBlackStones = true;
      if (area[changingPoint] == Area.MAINAREAWHITE
          && !check4Neighborhood(changingPoint, Area.PROTECTEDAREA)
          && !check4Neighborhood(changingPoint, Area.SEPARATIONGROUPS)) {
        area[changingPoint] = Area.MAINAREABLACK;
        setStone(changingPoint);
      } else {
        if (changingPoint % BOARDSIZE != 0
            && area[changingPoint - 1] == Area.MAINAREAWHITE
            && !check4Neighborhood(changingPoint - 1, Area.PROTECTEDAREA)
            && !check4Neighborhood(changingPoint - 1, Area.SEPARATIONGROUPS)) {
          area[changingPoint - 1] = Area.MAINAREABLACK;
          changingPoint = changingPoint - 1;
          setStone(changingPoint);
        } else {
          if (changingPoint >= BOARDSIZE
              && area[changingPoint - BOARDSIZE] == Area.MAINAREAWHITE
              && !check4Neighborhood(changingPoint - BOARDSIZE, Area.PROTECTEDAREA)
              && !check4Neighborhood(changingPoint - BOARDSIZE, Area.SEPARATIONGROUPS)) {
            area[changingPoint - BOARDSIZE] = Area.MAINAREABLACK;
            changingPoint = changingPoint - BOARDSIZE;
            setStone(changingPoint);
          } else {
            if (changingPoint <= BOARDSIZE * BOARDSIZE - BOARDSIZE
                && area[changingPoint + BOARDSIZE] == Area.MAINAREAWHITE
                && !check4Neighborhood(changingPoint + BOARDSIZE, Area.PROTECTEDAREA)
                && !check4Neighborhood(changingPoint + BOARDSIZE, Area.SEPARATIONGROUPS)) {
              area[changingPoint + BOARDSIZE] = Area.MAINAREABLACK;
              changingPoint = changingPoint + BOARDSIZE;
              setStone(changingPoint);
            } else {
              if (changingPoint % BOARDSIZE != BOARDSIZE - 1
                  && area[changingPoint + 1] == Area.MAINAREAWHITE
                  && !check4Neighborhood(changingPoint + 1, Area.PROTECTEDAREA)
                  && !check4Neighborhood(changingPoint + 1, Area.SEPARATIONGROUPS)) {
                area[changingPoint + 1] = Area.MAINAREABLACK;
                changingPoint = changingPoint + 1;
                setStone(changingPoint);
              } else { // we didn't find a changing point in our neighborhood. so we check the whole
                // board
                for (int i = 0; i < BOARDSIZE * BOARDSIZE; i++) {
                  if (area[i] == Area.MAINAREAWHITE) {
                    if (check4Neighborhood(i, Area.MAINAREABLACK)
                        && !check4Neighborhood(i, Area.PROTECTEDAREA)
                        && !check4Neighborhood(i, Area.SEPARATIONGROUPS)) {
                      setStone(i);
                      if (localLibertyCheck(i)) {
                        area[i] = Area.MAINAREABLACK;
                        changingPoint = i;
                        break;
                      } else {
                        area[i] = Area.MAINAREAWHITE;
                        placeBlackStones = false;
                        setStone(i);
                        placeBlackStones = true;
                      }
                    }
                  }
                }
                // ERROR kein punkt gefunden, um grenze zu verschieben
              }
            }
          }
        }
      }
      if (localLibertyCheck(changingPoint)) {
        moveBorder(p - 1);
        return;
      } else {
        area[changingPoint] = Area.MAINAREAWHITE;
        placeBlackStones = false;
        setStone(changingPoint);
        placeBlackStones = true;
        changingPoint = BOARDSIZE * BOARDSIZE;
        moveBorder(p);
        return;
      }
    } else { // if white gets more points
      placeBlackStones = false;
      if (area[changingPoint] == Area.MAINAREABLACK
          && !check4Neighborhood(changingPoint, Area.PROTECTEDAREA)
          && !check4Neighborhood(changingPoint, Area.SEPARATIONGROUPS)) {
        area[changingPoint] = Area.MAINAREAWHITE;
        setStone(changingPoint);
      } else {
        if (changingPoint % BOARDSIZE != 0
            && area[changingPoint - 1] == Area.MAINAREABLACK
            && !check4Neighborhood(changingPoint - 1, Area.PROTECTEDAREA)
            && !check4Neighborhood(changingPoint - 1, Area.SEPARATIONGROUPS)) {
          area[changingPoint - 1] = Area.MAINAREAWHITE;
          changingPoint = changingPoint - 1;
          setStone(changingPoint);
        } else {
          if (changingPoint >= BOARDSIZE
              && area[changingPoint - BOARDSIZE] == Area.MAINAREABLACK
              && !check4Neighborhood(changingPoint - BOARDSIZE, Area.PROTECTEDAREA)
              && !check4Neighborhood(changingPoint - BOARDSIZE, Area.SEPARATIONGROUPS)) {
            area[changingPoint - BOARDSIZE] = Area.MAINAREAWHITE;
            changingPoint = changingPoint - BOARDSIZE;
            setStone(changingPoint);
          } else {
            if (changingPoint <= BOARDSIZE * BOARDSIZE - BOARDSIZE
                && area[changingPoint + BOARDSIZE] == Area.MAINAREABLACK
                && !check4Neighborhood(changingPoint + BOARDSIZE, Area.PROTECTEDAREA)
                && !check4Neighborhood(changingPoint + BOARDSIZE, Area.SEPARATIONGROUPS)) {
              area[changingPoint + BOARDSIZE] = Area.MAINAREAWHITE;
              changingPoint = changingPoint + BOARDSIZE;
              setStone(changingPoint);
            } else {
              if (changingPoint % BOARDSIZE != BOARDSIZE - 1
                  && area[changingPoint + 1] == Area.MAINAREABLACK
                  && !check4Neighborhood(changingPoint + 1, Area.PROTECTEDAREA)
                  && !check4Neighborhood(changingPoint + 1, Area.SEPARATIONGROUPS)) {
                area[changingPoint + 1] = Area.MAINAREAWHITE;
                changingPoint = changingPoint + 1;
                setStone(changingPoint);
              } else { // we didn't find a changing point in our neighborhood. so we check the whole
                // board
                for (int i = 0; i < BOARDSIZE * BOARDSIZE; i++) {
                  if (area[i] == Area.MAINAREABLACK) {
                    if (check4Neighborhood(i, Area.MAINAREAWHITE)
                        && !check4Neighborhood(i, Area.PROTECTEDAREA)
                        && !check4Neighborhood(i, Area.SEPARATIONGROUPS)) {
                      placeBlackStones = false;
                      setStone(i);
                      if (localLibertyCheck(i)) {
                        area[i] = Area.MAINAREAWHITE;
                        changingPoint = i;
                        break;
                      } else {
                        area[i] = Area.MAINAREABLACK;
                        placeBlackStones = true;
                        setStone(i);
                      }
                    }
                  }
                }
                // ERROR kein punkt gefunden, um grenze zu verschieben
              }
            }
          }
        }
      }
      if (localLibertyCheck(changingPoint)) {
        moveBorder(p + 1);
        return;
      } else {
        area[changingPoint] = Area.MAINAREABLACK;
        placeBlackStones = true;
        setStone(changingPoint);
        placeBlackStones = false;
        changingPoint = BOARDSIZE * BOARDSIZE;
        moveBorder(p);
        return;
      }
    }
  }

  /** mirrors the position, starts again and mirrors black. Might help if some bugs appear */
  public void mirror() {
    if (mirror == 7) {
      mirror = 0;
    } else {
      mirror++;
    }
    restart();
  }

  /**
   * Returns to the point in the game there we started the Local Analysis. Deletes the branch of the
   * Local Analysis
   */
  public void returnToGame() {
    //			int ret =
    //		            JOptionPane.showConfirmDialog(
    //		                null,
    //		                "This will delete all moves and branches of the Local Analysis. Make sure
    // that you are in a Branch of the Local Analysis!",
    //		                "Delete",
    //		                JOptionPane.OK_CANCEL_OPTION);
    //	        if (ret != JOptionPane.OK_OPTION) {
    //	        	return;
    //	        }
    BoardHistoryList history = Lizzie.frame.localAnalysisFrame.board.getHistory();
    BoardHistoryNode currentNode = history.getCurrentHistoryNode();
    while (currentNode.previous().isPresent()) {
      BoardHistoryNode pre = currentNode.previous().get();
      Lizzie.frame.localAnalysisFrame.board.previousMove();
      int idx = pre.indexOfNode(currentNode);
      pre.deleteChild(idx);
      history = Lizzie.frame.localAnalysisFrame.board.getHistory();
      currentNode = history.getCurrentHistoryNode();
    }
    Lizzie.frame.localAnalysisFrame.board.restoreMoveNumber();
  }

  // TODO really load from Configs
  private void loadConfig(int playerInside, int playerToMove, int blackKoThreats2) {
    maxAlivePoints = Lizzie.config.maxAlivePoints;
    wallDistance =
        Lizzie.config
            .wallDistance; // between analyzed position and surrounding stones there will be a void
    // so the wall will not affect the local position
    blackKoThreats =
        blackKoThreats2; // builds Ko threats for Black if positive and for White if negative
    maxKoThreatsBlack =
        0; // maximal amount of Ko threats for Black. depends on size of Whites eye area.
    maxKoThreatsWhite = 0; // analog for White
    mirror = 0;
    switch (playerInside) {
      case 0:
        // ERROR sollte im Dialog behandelt werden
      case 1:
        // TODO automatische Bestimmung von playerInside
      case 2:
        playerInsideBlack = true;
        break;
      case 3:
        playerInsideBlack = false;
        break;
    }
    placeBlackStones = true; // color of single placed stones. true=black,false=white
    placeStonesAlternating =
        false; // color of single placed stones alternating or not. will change placeBlackStones if
    // true.
    placeLivingGroupesVertical =
        true; // in this case a living group will be a 5x3-block of stones. for false it's a
    // 3x5-block.
    allowKo = Lizzie.config.allowKo;
    switch (playerToMove) {
      case 0: // ERROR sollte im Dialog behandelt werden
      case 1: // TODO same as before
      case 2:
        blackToMove = true;
        break;
      case 3:
        blackToMove = false;
        break;
      case 4:
        blackToMove = playerInsideBlack;
        break;
      case 5:
        blackToMove = playerInsideBlack;
        break;
    }
  }

  /** deletes every stone which is not Area.PROTECTEDAREA */
  private void clearBoard() {
    for (int i = 0; i < BOARDSIZE * BOARDSIZE; i++) {
      if (area[i] != Area.PROTECTEDAREA) {
        board[i] = Stone.EMPTY;
        area[i] = Area.EMPTY;
      }
    }
  }

  /**
   * copy current board position twists also the board cause we use different coordinates than
   * Lizzie
   */
  private void copyBoard() {
    Stone[] boardCopy = Lizzie.frame.localAnalysisFrame.board.getStones();
    board = new Stone[BOARDSIZE * BOARDSIZE];
    for (int i = 0; i < BOARDSIZE * BOARDSIZE; i++) {
      board[i] = boardCopy[getField(BOARDSIZE - 1 - yCoord(i), xCoord(i))];
    }
    if (mirror != 0) {
      if (mirror % 4 != 0) {
        for (int i = 0; i < mirror % 4; i++) {
          mirror2();
        }
      }
      if (mirror > 3) {
        mirror1();
      }
    }
  }

  /** mirror symmetrical use twice to get the same position */
  private void mirror1() {
    //			Stone[] boardCopy = new Stone[board.length];
    //			for(int a = 0; a < board.length; a++) {
    //				boardCopy[a] = board[a];
    //			}
    //			for(int a = 0; a < board.length; a++) {
    //				board[a] = boardCopy[B * yCoord(a) + B - 1 - xCoord(a)];
    //			}
    //			startCoordinates[0] = B * yCoord(startCoordinates[0]) + B - 1 -
    // xCoord(startCoordinates[0]);
    //			startCoordinates[1] = B * yCoord(startCoordinates[1]) + B - 1 -
    // xCoord(startCoordinates[1]);
    return;
  }

  /** spin clockwise use 4 times to get the same position */
  private void mirror2() {
    Stone[] boardCopy = new Stone[board.length];
    for (int a = 0; a < board.length; a++) {
      boardCopy[a] = board[a];
    }
    for (int a = 0; a < board.length; a++) {
      board[a] = boardCopy[BOARDSIZE * (BOARDSIZE - 1 - xCoord(a)) + yCoord(a)];
    }
    startCoordinates[0] =
        BOARDSIZE * xCoord(startCoordinates[0]) + BOARDSIZE - 1 - yCoord(startCoordinates[0]);
    startCoordinates[1] =
        BOARDSIZE * xCoord(startCoordinates[1]) + BOARDSIZE - 1 - yCoord(startCoordinates[1]);
    return;
  }

  /** creates eyes for main areas */
  // TODO 3x6 augenraum für nicht ecke
  private void createEyes(boolean searchArea) {
    if (blackKoThreats > 0) {
      blackKoThreats = Math.min(blackKoThreats, maxKoThreatsBlack);
    } else {
      blackKoThreats = Math.max(blackKoThreats, -maxKoThreatsWhite);
    }
    if (!createBlackEyes(searchArea)) {
      printError(26);
      // ERROR
    }
    if (!createWhiteEyes(searchArea)) {
      printError(27);
      // ERROR
    }
  }

  /**
   * creates eyes for Blacks main area
   *
   * @param searchArea if true we have to search for the eye space
   */
  private boolean createBlackEyes(boolean searchArea) {
    placeBlackStones = true;
    // find our EYEROOM
    int startPoint;
    if (searchArea) {
      startPoint = findAreaType(Area.EYESPACEBLACK);
      eyeSpaceBlackPoint = startPoint;
    } else {
      startPoint = eyeSpaceBlackPoint;
    }
    if (startPoint == BOARDSIZE * BOARDSIZE) {
      // Fehlermeldung: kein augenraum gefunden
      return false;
    }
    int height = 0;
    int width = 0;
    while ((xCoord(startPoint) - width) > 0 && area[startPoint - width - 1] == Area.EYESPACEBLACK) {
      width++;
    }
    while ((xCoord(startPoint) + width) < BOARDSIZE - 1
        && area[startPoint + width + 1] == Area.EYESPACEBLACK) {
      width++;
    }
    while (yCoord(startPoint) - height > 0
        && area[startPoint - height * BOARDSIZE - BOARDSIZE] == Area.EYESPACEBLACK) {
      height++;
    }
    while (startPoint + height * BOARDSIZE + BOARDSIZE < BOARDSIZE * BOARDSIZE
        && area[startPoint + height * BOARDSIZE + BOARDSIZE] == Area.EYESPACEBLACK) {
      height++;
    }
    width++;
    height++;
    if (width < 2 || width > 6) {
      // ERROR
    }
    if (height < 2 || height > 6) {
      // ERROR
    }
    changeArea(startPoint, Area.EYESPACEBLACK, Stone.BLACK);

    placeBlackStones = false;
    switch (blackKoThreats) {
      case 3:
      case 2:
      case 1:
      case 0:
        placeBlackStones = true;
        if (isCorner(startPoint)) {
          if (startPoint == 0) {
            if (checkSpaceForLivingGroup(0, Area.EYESPACEBLACK)) {
              setUnprotectedLivingGroup(0);
              return true;
            }
            if (checkSpaceForLivingGroup(1, Area.EYESPACEBLACK)) {
              setUnprotectedLivingGroup(1);
              return true;
            }
            if (checkSpaceForLivingGroup(BOARDSIZE, Area.EYESPACEBLACK)) {
              setUnprotectedLivingGroup(BOARDSIZE);
              return true;
            }
          }
          if (startPoint == BOARDSIZE * BOARDSIZE - BOARDSIZE) {
            if (checkSpaceForLivingGroup(BOARDSIZE * BOARDSIZE - BOARDSIZE, Area.EYESPACEBLACK)) {
              setUnprotectedLivingGroup(BOARDSIZE * BOARDSIZE - BOARDSIZE);
              return true;
            }
            if (checkSpaceForLivingGroup(
                BOARDSIZE * BOARDSIZE - BOARDSIZE + 1, Area.EYESPACEBLACK)) {
              setUnprotectedLivingGroup(BOARDSIZE * BOARDSIZE - BOARDSIZE + 1);
              return true;
            }
            if (checkSpaceForLivingGroup(
                BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE, Area.EYESPACEBLACK)) {
              setUnprotectedLivingGroup(BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE);
              return true;
            }
          }
        }
        if (isBorder(startPoint)) {
          if (checkSpaceForLivingGroup(startPoint - 2, Area.EYESPACEBLACK)) {
            setUnprotectedLivingGroup(startPoint - 2);
            return true;
          }
          if (checkSpaceForLivingGroup(startPoint + 2, Area.EYESPACEBLACK)) {
            setUnprotectedLivingGroup(startPoint + 2);
            return true;
          }
          if (checkSpaceForLivingGroup(startPoint + 2 * BOARDSIZE, Area.EYESPACEBLACK)) {
            setUnprotectedLivingGroup(startPoint + 2 * BOARDSIZE);
            return true;
          }
          if (checkSpaceForLivingGroup(startPoint - 2 * BOARDSIZE, Area.EYESPACEBLACK)) {
            setUnprotectedLivingGroup(startPoint - 2 * BOARDSIZE);
            return true;
          }
        }
        placeLivingGroupesVertical = true;
        if (checkSpaceForLivingGroup(startPoint + 2 * BOARDSIZE + 1, Area.EYESPACEBLACK)) {
          setUnprotectedLivingGroup(startPoint + 2 * BOARDSIZE + 1);
          return true;
        }
        placeLivingGroupesVertical = false;
        if (checkSpaceForLivingGroup(startPoint + BOARDSIZE + 2, Area.EYESPACEBLACK)) {
          setUnprotectedLivingGroup(startPoint + BOARDSIZE + 2);
          return true;
        }
        return false;
      case -1:
        if (height < width) {
          if (isCorner(startPoint)) {
            if (startPoint == 0) {
              switch (height) {
                case 2:
                  switch (width) {
                    case 5:
                    case 6:
                      deleteStone(0);
                      deleteStone(1);
                      deleteStone(2);
                      deleteStone(3);
                      return true;
                  }
                case 3:
                  switch (width) {
                    case 4:
                    case 5:
                    case 6:
                      deleteStone(0);
                      deleteStone(1);
                      deleteStone(2);
                      deleteStone(BOARDSIZE + 2);
                      return true;
                  }
              }
            }
            if (startPoint == BOARDSIZE * BOARDSIZE - BOARDSIZE) {
              switch (height) {
                case 2:
                  switch (width) {
                    case 5:
                    case 6:
                      deleteStone(BOARDSIZE * BOARDSIZE - BOARDSIZE);
                      deleteStone(BOARDSIZE * BOARDSIZE - BOARDSIZE + 1);
                      deleteStone(BOARDSIZE * BOARDSIZE - BOARDSIZE + 2);
                      deleteStone(BOARDSIZE * BOARDSIZE - BOARDSIZE + 3);
                      return true;
                  }
                case 3:
                  switch (width) {
                    case 4:
                    case 5:
                    case 6:
                      deleteStone(BOARDSIZE * BOARDSIZE - BOARDSIZE);
                      deleteStone(BOARDSIZE * BOARDSIZE - BOARDSIZE + 1);
                      deleteStone(BOARDSIZE * BOARDSIZE - BOARDSIZE + 2);
                      deleteStone(BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE + 2);
                      return true;
                  }
              }
            }
          }
          if (isBorder(startPoint)) {
            if (startPoint < BOARDSIZE) {
              switch (height) {
                case 2:
                  switch (width) {
                    case 6:
                      deleteStone(startPoint + 1);
                      deleteStone(startPoint + 2);
                      deleteStone(startPoint + 3);
                      deleteStone(startPoint + 4);
                      return true;
                  }
                case 3:
                  switch (width) {
                    case 5:
                    case 6:
                      deleteStone(startPoint + 1);
                      deleteStone(startPoint + 2);
                      deleteStone(startPoint + 3);
                      deleteStone(startPoint + BOARDSIZE + 1);
                      return true;
                  }
              }
            }
            if (startPoint >= BOARDSIZE * BOARDSIZE - BOARDSIZE) {
              switch (height) {
                case 2:
                  switch (width) {
                    case 6:
                      deleteStone(startPoint + 1);
                      deleteStone(startPoint + 2);
                      deleteStone(startPoint + 3);
                      deleteStone(startPoint + 4);
                      return true;
                  }
                case 3:
                  switch (width) {
                    case 5:
                    case 6:
                      deleteStone(startPoint + 1);
                      deleteStone(startPoint + 2);
                      deleteStone(startPoint + 3);
                      deleteStone(startPoint + BOARDSIZE + 1);
                      return true;
                  }
              }
            }
            if (startPoint % BOARDSIZE == 0) {
              // ERROR
            }
          }
          switch (height) {
            case 3:
              switch (width) {
                case 6:
                  deleteStone(startPoint + BOARDSIZE + 1);
                  deleteStone(startPoint + BOARDSIZE + 2);
                  deleteStone(startPoint + BOARDSIZE + 3);
                  deleteStone(startPoint + BOARDSIZE + 4);
                  return true;
              }
            case 4:
              switch (width) {
                case 5:
                  deleteStone(startPoint + BOARDSIZE + 1);
                  deleteStone(startPoint + BOARDSIZE + 2);
                  deleteStone(startPoint + BOARDSIZE + 3);
                  deleteStone(startPoint + 2 * BOARDSIZE + 4);
                  return true;
              }
          }
        } else {
          if (isCorner(startPoint)) {
            if (startPoint == 0) {
              switch (width) {
                case 2:
                  switch (height) {
                    case 5:
                    case 6:
                      deleteStone(0);
                      deleteStone(BOARDSIZE);
                      deleteStone(2 * BOARDSIZE);
                      deleteStone(3 * BOARDSIZE);
                      return true;
                  }
                case 3:
                  switch (height) {
                    case 4:
                    case 5:
                    case 6:
                      deleteStone(0);
                      deleteStone(BOARDSIZE);
                      deleteStone(2 * BOARDSIZE);
                      deleteStone(2 * BOARDSIZE + 1);
                      return true;
                  }
              }
            }
            if (startPoint == BOARDSIZE * BOARDSIZE - BOARDSIZE) {
              switch (width) {
                case 2:
                  switch (height) {
                    case 5:
                    case 6:
                      deleteStone(BOARDSIZE * BOARDSIZE - BOARDSIZE);
                      deleteStone(BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE);
                      deleteStone(BOARDSIZE * BOARDSIZE - 3 * BOARDSIZE);
                      deleteStone(BOARDSIZE * BOARDSIZE - 4 * BOARDSIZE);
                      return true;
                  }
                case 3:
                  switch (height) {
                    case 4:
                    case 5:
                    case 6:
                      deleteStone(BOARDSIZE * BOARDSIZE - BOARDSIZE);
                      deleteStone(BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE);
                      deleteStone(BOARDSIZE * BOARDSIZE - 3 * BOARDSIZE);
                      deleteStone(BOARDSIZE * BOARDSIZE - 3 * BOARDSIZE + 1);
                      return true;
                  }
              }
            }
          }
          if (isBorder(startPoint)) {
            if (startPoint < BOARDSIZE) {
              // ERROR
            }
            if (startPoint >= BOARDSIZE * BOARDSIZE - BOARDSIZE) {
              // ERROR
            }
            if (startPoint % BOARDSIZE == 0) {
              switch (height) {
                case 2:
                  switch (width) {
                    case 6:
                      deleteStone(startPoint + BOARDSIZE);
                      deleteStone(startPoint + 2 * BOARDSIZE);
                      deleteStone(startPoint + 3 * BOARDSIZE);
                      deleteStone(startPoint + 4 * BOARDSIZE);
                      return true;
                  }
                case 3:
                  switch (width) {
                    case 5:
                    case 6:
                      deleteStone(startPoint + BOARDSIZE);
                      deleteStone(startPoint + 2 * BOARDSIZE);
                      deleteStone(startPoint + 3 * BOARDSIZE);
                      deleteStone(startPoint + BOARDSIZE + 1);
                      return true;
                  }
              }
            }
          }
          switch (width) {
            case 3:
              switch (height) {
                case 6:
                  deleteStone(startPoint + BOARDSIZE + 1);
                  deleteStone(startPoint + 2 * BOARDSIZE + 1);
                  deleteStone(startPoint + 3 * BOARDSIZE + 1);
                  deleteStone(startPoint + 4 * BOARDSIZE + 1);
                  return true;
              }
            case 4:
              switch (height) {
                case 5:
                case 6:
                  deleteStone(startPoint + BOARDSIZE + 1);
                  deleteStone(startPoint + 2 * BOARDSIZE + 1);
                  deleteStone(startPoint + 3 * BOARDSIZE + 1);
                  deleteStone(startPoint + 3 * BOARDSIZE + 2);
                  return true;
              }
          }
        }
      case -2:
        if (height < width) {
          if (isCorner(startPoint)) {
            if (startPoint == 0) {
              switch (height) {
                case 3:
                  switch (width) {
                    case 4:
                    case 5:
                    case 6:
                      setStone(0);
                      setStone(1);
                      deleteStone(2);
                      deleteStone(BOARDSIZE);
                      setStone(BOARDSIZE + 2);
                      return true;
                  }
              }
            }
            if (startPoint == BOARDSIZE * BOARDSIZE - BOARDSIZE) {
              switch (height) {
                case 3:
                  switch (width) {
                    case 4:
                    case 5:
                    case 6:
                      setStone(BOARDSIZE * BOARDSIZE - BOARDSIZE);
                      setStone(BOARDSIZE * BOARDSIZE - BOARDSIZE + 1);
                      deleteStone(BOARDSIZE * BOARDSIZE - BOARDSIZE + 2);
                      deleteStone(BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE);
                      setStone(BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE + 2);
                      return true;
                  }
              }
            }
          }
          if (isBorder(startPoint)) {
            if (startPoint < BOARDSIZE) {
              switch (height) {
                case 3:
                case 4:
                  switch (width) {
                    case 5:
                    case 6:
                      setStone(startPoint + 1);
                      deleteStone(startPoint + 3);
                      setStone(startPoint + BOARDSIZE + 1);
                      deleteStone(startPoint + BOARDSIZE + 2);
                      setStone(startPoint + BOARDSIZE + 3);
                      return true;
                  }
              }
            }
            if (startPoint >= BOARDSIZE * BOARDSIZE - BOARDSIZE) {
              switch (height) {
                case 3:
                case 4:
                  switch (width) {
                    case 5:
                    case 6:
                      setStone(startPoint + 1);
                      deleteStone(startPoint + 3);
                      setStone(startPoint - BOARDSIZE + 1);
                      deleteStone(startPoint - BOARDSIZE + 2);
                      setStone(startPoint - BOARDSIZE + 3);
                      return true;
                  }
              }
            }
            if (startPoint % BOARDSIZE == 0) {
              // ERROR
            }
          }
          switch (height) {
            case 4:
              switch (width) {
                case 5:
                case 6:
                  deleteStone(startPoint + BOARDSIZE + 1);
                  setStone(startPoint + BOARDSIZE + 2);
                  setStone(startPoint + BOARDSIZE + 3);
                  setStone(startPoint + 2 * BOARDSIZE + 1);
                  deleteStone(startPoint + 2 * BOARDSIZE + 3);
                  return true;
              }
          }
        } else {
          if (isCorner(startPoint)) {
            if (startPoint == 0) {
              switch (width) {
                case 3:
                  switch (height) {
                    case 4:
                    case 5:
                    case 6:
                      setStone(0);
                      setStone(BOARDSIZE);
                      deleteStone(2 * BOARDSIZE);
                      deleteStone(1);
                      setStone(2 * BOARDSIZE + 1);
                      return true;
                  }
              }
            }
            if (startPoint == BOARDSIZE * BOARDSIZE - BOARDSIZE) {
              switch (width) {
                case 3:
                  switch (height) {
                    case 4:
                    case 5:
                    case 6:
                      setStone(BOARDSIZE * BOARDSIZE - BOARDSIZE);
                      setStone(BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE);
                      deleteStone(BOARDSIZE * BOARDSIZE - 3 * BOARDSIZE);
                      deleteStone(BOARDSIZE * BOARDSIZE - BOARDSIZE + 1);
                      setStone(BOARDSIZE * BOARDSIZE - 3 * BOARDSIZE + 1);
                      return true;
                  }
              }
            }
          }
          if (isBorder(startPoint)) {
            if (startPoint < BOARDSIZE) {
              // ERROR
            }
            if (startPoint >= BOARDSIZE * BOARDSIZE - BOARDSIZE) {
              // ERROR
            }
            if (startPoint % BOARDSIZE == 0) {
              switch (width) {
                case 3:
                case 4:
                  switch (height) {
                    case 5:
                    case 6:
                      deleteStone(startPoint + BOARDSIZE);
                      setStone(startPoint + BOARDSIZE + 1);
                      deleteStone(startPoint + 2 * BOARDSIZE + 1);
                      setStone(startPoint + 3 * BOARDSIZE);
                      setStone(startPoint + 3 * BOARDSIZE + 1);
                      return true;
                  }
              }
            }
          }
          switch (width) {
            case 4:
              switch (height) {
                case 5:
                case 6:
                  setStone(startPoint + BOARDSIZE + 1);
                  deleteStone(startPoint + BOARDSIZE + 2);
                  setStone(startPoint + 2 * BOARDSIZE + 1);
                  setStone(startPoint + 3 * BOARDSIZE + 1);
                  deleteStone(startPoint + 2 * BOARDSIZE);
                  return true;
              }
          }
        }
      case -3:
        if (height < width) {
          if (isCorner(startPoint)) {
            if (startPoint == 0) {
              switch (height) {
                case 3:
                  switch (width) {
                    case 5:
                    case 6:
                      deleteStone(0);
                      deleteStone(1);
                      deleteStone(2);
                      deleteStone(3);
                      setStone(BOARDSIZE);
                      setStone(BOARDSIZE + 1);
                      deleteStone(BOARDSIZE + 3);
                      return true;
                  }
              }
            }
            if (startPoint == BOARDSIZE * BOARDSIZE - BOARDSIZE) {
              switch (height) {
                case 3:
                  switch (width) {
                    case 5:
                    case 6:
                      deleteStone(BOARDSIZE * BOARDSIZE - BOARDSIZE);
                      deleteStone(BOARDSIZE * BOARDSIZE - BOARDSIZE + 1);
                      deleteStone(BOARDSIZE * BOARDSIZE - BOARDSIZE + 2);
                      deleteStone(BOARDSIZE * BOARDSIZE - BOARDSIZE + 3);
                      setStone(BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE);
                      setStone(BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE + 1);
                      deleteStone(BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE + 3);
                      return true;
                  }
              }
            }
          }
          if (isBorder(startPoint)) {
            if (startPoint < BOARDSIZE) {
              switch (height) {
                case 3:
                  switch (width) {
                    case 6:
                      deleteStone(startPoint + 1);
                      setStone(startPoint + 2);
                      deleteStone(startPoint + 3);
                      deleteStone(startPoint + BOARDSIZE + 2);
                      deleteStone(startPoint + BOARDSIZE + 3);
                      setStone(startPoint + BOARDSIZE + 4);
                      return true;
                  }
                case 4:
                  switch (width) {
                    case 5:
                    case 6:
                      deleteStone(startPoint + BOARDSIZE + 1);
                      deleteStone(startPoint + BOARDSIZE + 2);
                      setStone(startPoint + BOARDSIZE + 3);
                      setStone(startPoint + 2 * BOARDSIZE + 1);
                      deleteStone(startPoint + 2 * BOARDSIZE + 2);
                      deleteStone(startPoint + 2 * BOARDSIZE + 3);
                      return true;
                  }
              }
            }
            if (startPoint >= BOARDSIZE * BOARDSIZE - BOARDSIZE) {
              switch (height) {
                case 3:
                  switch (width) {
                    case 6:
                      deleteStone(startPoint + 1);
                      setStone(startPoint + 2);
                      deleteStone(startPoint + 3);
                      deleteStone(startPoint - BOARDSIZE + 2);
                      deleteStone(startPoint - BOARDSIZE + 3);
                      setStone(startPoint - BOARDSIZE + 4);
                      return true;
                  }
                case 4:
                  switch (width) {
                    case 5:
                    case 6:
                      deleteStone(startPoint - BOARDSIZE + 1);
                      deleteStone(startPoint - BOARDSIZE + 2);
                      setStone(startPoint - BOARDSIZE + 3);
                      setStone(startPoint - 2 * BOARDSIZE + 1);
                      deleteStone(startPoint - 2 * BOARDSIZE + 2);
                      deleteStone(startPoint - 2 * BOARDSIZE + 3);
                      return true;
                  }
              }
            }
            if (startPoint % BOARDSIZE == 0) {
              // ERROR
            }
          }
          switch (height) {
            case 4:
              switch (width) {
                case 5:
                case 6:
                  deleteStone(startPoint + BOARDSIZE + 1);
                  deleteStone(startPoint + BOARDSIZE + 2);
                  setStone(startPoint + BOARDSIZE + 3);
                  setStone(startPoint + 2 * BOARDSIZE + 1);
                  deleteStone(startPoint + 2 * BOARDSIZE + 2);
                  deleteStone(startPoint + 2 * BOARDSIZE + 3);
                  return true;
              }
          }
        } else {
          if (isCorner(startPoint)) {
            if (startPoint == 0) {
              switch (width) {
                case 3:
                case 4:
                  switch (height) {
                    case 5:
                    case 6:
                      deleteStone(0);
                      deleteStone(BOARDSIZE);
                      deleteStone(2 * BOARDSIZE);
                      deleteStone(3 * BOARDSIZE);
                      setStone(1);
                      setStone(BOARDSIZE + 1);
                      deleteStone(3 * BOARDSIZE + 1);
                      return true;
                  }
              }
            }
            if (startPoint == BOARDSIZE * BOARDSIZE - BOARDSIZE) {
              switch (width) {
                case 3:
                case 4:
                  switch (height) {
                    case 5:
                    case 6:
                      deleteStone(startPoint);
                      deleteStone(startPoint - BOARDSIZE);
                      deleteStone(startPoint - 2 * BOARDSIZE);
                      deleteStone(startPoint - 3 * BOARDSIZE);
                      setStone(startPoint + 1);
                      setStone(startPoint - BOARDSIZE + 1);
                      deleteStone(startPoint - 3 * BOARDSIZE + 1);
                      return true;
                  }
              }
            }
          }
          if (isBorder(startPoint)) {
            if (startPoint < BOARDSIZE) {
              // ERROR
            }
            if (startPoint >= BOARDSIZE * BOARDSIZE - BOARDSIZE) {
              // ERROR
            }
            if (startPoint % BOARDSIZE == 0) {
              switch (width) {
                case 3:
                  switch (height) {
                    case 6:
                      deleteStone(startPoint + BOARDSIZE);
                      setStone(startPoint + 2 * BOARDSIZE);
                      deleteStone(startPoint + 3 * BOARDSIZE);
                      deleteStone(startPoint + 2 * BOARDSIZE + 1);
                      deleteStone(startPoint + 3 * BOARDSIZE + 1);
                      setStone(startPoint + 4 * BOARDSIZE + 1);
                      return true;
                    case 4:
                      switch (height) {
                        case 5:
                        case 6:
                          setStone(startPoint + BOARDSIZE + 1);
                          deleteStone(startPoint + 2 * BOARDSIZE + 1);
                          deleteStone(startPoint + 3 * BOARDSIZE + 1);
                          deleteStone(startPoint + BOARDSIZE + 1);
                          deleteStone(startPoint + 2 * BOARDSIZE + 1);
                          setStone(startPoint + 3 * BOARDSIZE + 1);
                          return true;
                      }
                  }
              }
            }
            switch (width) {
              case 4:
                switch (height) {
                  case 5:
                  case 6:
                    setStone(startPoint + BOARDSIZE + 1);
                    deleteStone(startPoint + 2 * BOARDSIZE + 1);
                    deleteStone(startPoint + 3 * BOARDSIZE + 1);
                    deleteStone(startPoint + BOARDSIZE + 1);
                    deleteStone(startPoint + 2 * BOARDSIZE + 1);
                    setStone(startPoint + 3 * BOARDSIZE + 1);
                    return true;
                }
            }
          }
        }
    }
    return false;
  }

  /** creates eyes for Blacks main area */
  private boolean createWhiteEyes(boolean searchArea) {
    placeBlackStones = false;
    // find our EYEROOM
    int startPoint;
    if (searchArea) {
      startPoint = findAreaType(Area.EYESPACEWHITE);
      eyeSpaceWhitePoint = startPoint;
    } else {
      startPoint = eyeSpaceWhitePoint;
    }
    if (startPoint == BOARDSIZE * BOARDSIZE) {
      // Fehlermeldung: kein augenraum gefunden
      return false;
    }
    int height = 0;
    int width = 0;

    while ((xCoord(startPoint) - width) > 0 && area[startPoint - width - 1] == Area.EYESPACEWHITE) {
      width++;
    }
    while ((xCoord(startPoint) + width) < BOARDSIZE - 1
        && area[startPoint + width + 1] == Area.EYESPACEWHITE) {
      width++;
    }
    while (yCoord(startPoint) - height > 0
        && area[startPoint - height * BOARDSIZE - BOARDSIZE] == Area.EYESPACEWHITE) {
      height++;
    }
    while (startPoint + height * BOARDSIZE + BOARDSIZE < BOARDSIZE * BOARDSIZE
        && area[startPoint + height * BOARDSIZE + BOARDSIZE] == Area.EYESPACEWHITE) {
      height++;
    }
    width++;
    height++;
    if (width < 2 || width > 6) {
      return false;
    }
    if (height < 2 || height > 6) {
      return false;
    }
    changeArea(startPoint, Area.EYESPACEWHITE, Stone.WHITE);

    placeBlackStones = true;
    switch (blackKoThreats) {
      case -3:
      case -2:
      case -1:
      case 0:
        placeBlackStones = false;
        if (isCorner(startPoint)) {
          if (startPoint == BOARDSIZE - 1) {
            if (checkSpaceForLivingGroup(BOARDSIZE - 1, Area.EYESPACEWHITE)) {
              setUnprotectedLivingGroup(BOARDSIZE - 1);
              return true;
            }
            if (checkSpaceForLivingGroup(BOARDSIZE - 2, Area.EYESPACEWHITE)) {
              setUnprotectedLivingGroup(BOARDSIZE - 2);
              return true;
            }
            if (checkSpaceForLivingGroup(2 * BOARDSIZE - 1, Area.EYESPACEWHITE)) {
              setUnprotectedLivingGroup(2 * BOARDSIZE - 1);
              return true;
            }
          }
          if (startPoint == BOARDSIZE * BOARDSIZE - 1) {
            if (checkSpaceForLivingGroup(BOARDSIZE * BOARDSIZE - 1, Area.EYESPACEWHITE)) {
              setUnprotectedLivingGroup(BOARDSIZE * BOARDSIZE - 1);
              return true;
            }
            if (checkSpaceForLivingGroup(BOARDSIZE * BOARDSIZE - 2, Area.EYESPACEWHITE)) {
              setUnprotectedLivingGroup(BOARDSIZE * BOARDSIZE - 2);
              return true;
            }
            if (checkSpaceForLivingGroup(
                BOARDSIZE * BOARDSIZE - BOARDSIZE - 1, Area.EYESPACEWHITE)) {
              setUnprotectedLivingGroup(BOARDSIZE * BOARDSIZE - BOARDSIZE - 1);
              return true;
            }
          }
        }
        if (isBorder(startPoint)) {
          if (checkSpaceForLivingGroup(startPoint - 2, Area.EYESPACEWHITE)) {
            setUnprotectedLivingGroup(startPoint - 2);
            return true;
          }
          if (checkSpaceForLivingGroup(startPoint + 2, Area.EYESPACEWHITE)) {
            setUnprotectedLivingGroup(startPoint + 2);
            return true;
          }
          if (checkSpaceForLivingGroup(startPoint + 2 * BOARDSIZE, Area.EYESPACEWHITE)) {
            setUnprotectedLivingGroup(startPoint + 2 * BOARDSIZE);
            return true;
          }
          if (checkSpaceForLivingGroup(startPoint - 2 * BOARDSIZE, Area.EYESPACEWHITE)) {
            setUnprotectedLivingGroup(startPoint - 2 * BOARDSIZE);
            return true;
          }
        }
        placeLivingGroupesVertical = true;
        if (checkSpaceForLivingGroup(startPoint + 2 * BOARDSIZE + 1, Area.EYESPACEWHITE)) {
          setUnprotectedLivingGroup(startPoint + 2 * BOARDSIZE + 1);
          return true;
        }
        placeLivingGroupesVertical = false;
        if (checkSpaceForLivingGroup(startPoint + BOARDSIZE + 2, Area.EYESPACEWHITE)) {
          setUnprotectedLivingGroup(startPoint + BOARDSIZE + 2);
          return true;
        }
        return false;
      case 1:
        if (height < width) {
          if (isCorner(startPoint)) {
            if (startPoint == BOARDSIZE - 1) {
              switch (height) {
                case 2:
                  switch (width) {
                    case 5:
                    case 6:
                      deleteStone(BOARDSIZE - 1);
                      deleteStone(BOARDSIZE - 2);
                      deleteStone(BOARDSIZE - 3);
                      deleteStone(BOARDSIZE - 4);
                      return true;
                  }
                case 3:
                  switch (width) {
                    case 4:
                    case 5:
                    case 6:
                      deleteStone(BOARDSIZE - 1);
                      deleteStone(BOARDSIZE - 2);
                      deleteStone(BOARDSIZE - 3);
                      deleteStone(2 * BOARDSIZE - 3);
                      return true;
                  }
              }
            }
            if (startPoint == BOARDSIZE * BOARDSIZE - 1) {
              switch (height) {
                case 2:
                  switch (width) {
                    case 5:
                    case 6:
                      deleteStone(BOARDSIZE * BOARDSIZE - 1);
                      deleteStone(BOARDSIZE * BOARDSIZE - 2);
                      deleteStone(BOARDSIZE * BOARDSIZE - 3);
                      deleteStone(BOARDSIZE * BOARDSIZE - 4);
                      return true;
                  }
                case 3:
                  switch (width) {
                    case 4:
                    case 5:
                    case 6:
                      deleteStone(BOARDSIZE * BOARDSIZE - 1);
                      deleteStone(BOARDSIZE * BOARDSIZE - 2);
                      deleteStone(BOARDSIZE * BOARDSIZE - 3);
                      deleteStone(BOARDSIZE * BOARDSIZE - BOARDSIZE - 3);
                      return true;
                  }
              }
            }
          }
          if (isBorder(startPoint)) {
            if (startPoint < BOARDSIZE) {
              switch (height) {
                case 2:
                  switch (width) {
                    case 6:
                      deleteStone(startPoint + 1);
                      deleteStone(startPoint + 2);
                      deleteStone(startPoint + 3);
                      deleteStone(startPoint + 4);
                      return true;
                  }
                case 3:
                  switch (width) {
                    case 5:
                    case 6:
                      deleteStone(startPoint + 1);
                      deleteStone(startPoint + 2);
                      deleteStone(startPoint + 3);
                      deleteStone(startPoint + BOARDSIZE + 1);
                      return true;
                  }
              }
            }
            if (startPoint >= BOARDSIZE * BOARDSIZE - BOARDSIZE) {
              switch (height) {
                case 2:
                  switch (width) {
                    case 6:
                      deleteStone(startPoint + 1);
                      deleteStone(startPoint + 2);
                      deleteStone(startPoint + 3);
                      deleteStone(startPoint + 4);
                      return true;
                  }
                case 3:
                  switch (width) {
                    case 5:
                    case 6:
                      deleteStone(startPoint + 1);
                      deleteStone(startPoint + 2);
                      deleteStone(startPoint + 3);
                      deleteStone(startPoint + BOARDSIZE + 1);
                      return true;
                  }
              }
            }
            if (startPoint % BOARDSIZE == 0) {
              // ERROR
            }
          }
          switch (height) {
            case 3:
              switch (width) {
                case 6:
                  deleteStone(startPoint + BOARDSIZE + 1);
                  deleteStone(startPoint + BOARDSIZE + 2);
                  deleteStone(startPoint + BOARDSIZE + 3);
                  deleteStone(startPoint + BOARDSIZE + 4);
                  return true;
              }
            case 4:
              switch (width) {
                case 5:
                  deleteStone(startPoint + BOARDSIZE + 1);
                  deleteStone(startPoint + BOARDSIZE + 2);
                  deleteStone(startPoint + BOARDSIZE + 3);
                  deleteStone(startPoint + 2 * BOARDSIZE + 4);
                  return true;
              }
          }
        } else {
          if (isCorner(startPoint)) {
            if (startPoint == BOARDSIZE - 1) {
              switch (width) {
                case 2:
                  switch (height) {
                    case 5:
                    case 6:
                      deleteStone(BOARDSIZE - 1);
                      deleteStone(2 * BOARDSIZE - 1);
                      deleteStone(3 * BOARDSIZE - 1);
                      deleteStone(4 * BOARDSIZE - 1);
                      return true;
                  }
                case 3:
                  switch (height) {
                    case 4:
                    case 5:
                    case 6:
                      deleteStone(BOARDSIZE - 1);
                      deleteStone(2 * BOARDSIZE - 1);
                      deleteStone(3 * BOARDSIZE - 1);
                      deleteStone(3 * BOARDSIZE - 2);
                      return true;
                  }
              }
            }
            if (startPoint == BOARDSIZE * BOARDSIZE - 1) {
              switch (width) {
                case 2:
                  switch (height) {
                    case 5:
                    case 6:
                      deleteStone(BOARDSIZE * BOARDSIZE - 1);
                      deleteStone(BOARDSIZE * BOARDSIZE - BOARDSIZE - 1);
                      deleteStone(BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE - 1);
                      deleteStone(BOARDSIZE * BOARDSIZE - 3 * BOARDSIZE - 1);
                      return true;
                  }
                case 3:
                  switch (height) {
                    case 4:
                    case 5:
                    case 6:
                      deleteStone(BOARDSIZE * BOARDSIZE - 1);
                      deleteStone(BOARDSIZE * BOARDSIZE - BOARDSIZE - 1);
                      deleteStone(BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE - 1);
                      deleteStone(BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE - 2);
                      return true;
                  }
              }
            }
          }
          if (isBorder(startPoint)) {
            if (startPoint < BOARDSIZE) {
              // ERROR
            }
            if (startPoint >= BOARDSIZE * BOARDSIZE - BOARDSIZE) {
              // ERROR
            }
            if (startPoint % BOARDSIZE == BOARDSIZE - 1) {
              switch (height) {
                case 2:
                  switch (width) {
                    case 6:
                      deleteStone(startPoint + BOARDSIZE);
                      deleteStone(startPoint + 2 * BOARDSIZE);
                      deleteStone(startPoint + 3 * BOARDSIZE);
                      deleteStone(startPoint + 4 * BOARDSIZE);
                      return true;
                  }
                case 3:
                  switch (width) {
                    case 5:
                    case 6:
                      deleteStone(startPoint + BOARDSIZE);
                      deleteStone(startPoint + 2 * BOARDSIZE);
                      deleteStone(startPoint + 3 * BOARDSIZE);
                      deleteStone(startPoint + BOARDSIZE - 1);
                      return true;
                  }
              }
            }
          }
          switch (width) {
            case 3:
              switch (height) {
                case 6:
                  deleteStone(startPoint + BOARDSIZE + 1);
                  deleteStone(startPoint + 2 * BOARDSIZE + 1);
                  deleteStone(startPoint + 3 * BOARDSIZE + 1);
                  deleteStone(startPoint + 4 * BOARDSIZE + 1);
                  return true;
              }
            case 4:
              switch (height) {
                case 5:
                case 6:
                  deleteStone(startPoint + BOARDSIZE + 1);
                  deleteStone(startPoint + 2 * BOARDSIZE + 1);
                  deleteStone(startPoint + 3 * BOARDSIZE + 1);
                  deleteStone(startPoint + 3 * BOARDSIZE + 2);
                  return true;
              }
          }
        }
      case 2:
        if (height < width) {
          if (isCorner(startPoint)) {
            if (startPoint == BOARDSIZE - 1) {
              switch (height) {
                case 3:
                  switch (width) {
                    case 4:
                    case 5:
                    case 6:
                      setStone(BOARDSIZE - 1);
                      setStone(BOARDSIZE - 2);
                      deleteStone(BOARDSIZE - 3);
                      deleteStone(2 * BOARDSIZE - 1);
                      setStone(2 * BOARDSIZE - 3);
                      return true;
                  }
              }
            }
            if (startPoint == BOARDSIZE * BOARDSIZE - 1) {
              switch (height) {
                case 3:
                  switch (width) {
                    case 4:
                    case 5:
                    case 6:
                      setStone(BOARDSIZE * BOARDSIZE - 1);
                      setStone(BOARDSIZE * BOARDSIZE - 2);
                      deleteStone(BOARDSIZE * BOARDSIZE - 3);
                      deleteStone(BOARDSIZE * BOARDSIZE - BOARDSIZE - 1);
                      setStone(BOARDSIZE * BOARDSIZE - BOARDSIZE - 3);
                      return true;
                  }
              }
            }
          }
          if (isBorder(startPoint)) {
            if (startPoint < BOARDSIZE) {
              switch (height) {
                case 3:
                case 4:
                  switch (width) {
                    case 5:
                    case 6:
                      setStone(startPoint + 1);
                      deleteStone(startPoint + 3);
                      setStone(startPoint + BOARDSIZE + 1);
                      deleteStone(startPoint + BOARDSIZE + 2);
                      setStone(startPoint + BOARDSIZE + 3);
                      return true;
                  }
              }
            }
            if (startPoint >= BOARDSIZE * BOARDSIZE - BOARDSIZE) {
              switch (height) {
                case 3:
                case 4:
                  switch (width) {
                    case 5:
                    case 6:
                      setStone(startPoint + 1);
                      deleteStone(startPoint + 3);
                      setStone(startPoint - BOARDSIZE + 1);
                      deleteStone(startPoint - BOARDSIZE + 2);
                      setStone(startPoint - BOARDSIZE + 3);
                      return true;
                  }
              }
            }
            if (startPoint % BOARDSIZE == 0) {
              // ERROR
            }
          }
          switch (height) {
            case 4:
              switch (width) {
                case 5:
                case 6:
                  deleteStone(startPoint + BOARDSIZE + 1);
                  setStone(startPoint + BOARDSIZE + 2);
                  setStone(startPoint + BOARDSIZE + 3);
                  setStone(startPoint + 2 * BOARDSIZE + 1);
                  deleteStone(startPoint + 2 * BOARDSIZE + 3);
                  return true;
              }
          }
        } else {
          if (isCorner(startPoint)) {
            if (startPoint == BOARDSIZE - 1) {
              switch (width) {
                case 3:
                  switch (height) {
                    case 4:
                    case 5:
                    case 6:
                      setStone(BOARDSIZE - 1);
                      setStone(2 * BOARDSIZE - 1);
                      deleteStone(3 * BOARDSIZE - 1);
                      deleteStone(BOARDSIZE - 2);
                      setStone(3 * BOARDSIZE - 2);
                      return true;
                  }
              }
            }
            if (startPoint == BOARDSIZE * BOARDSIZE - 1) {
              switch (width) {
                case 3:
                  switch (height) {
                    case 4:
                    case 5:
                    case 6:
                      setStone(BOARDSIZE * BOARDSIZE - 1);
                      setStone(BOARDSIZE * BOARDSIZE - BOARDSIZE - 1);
                      deleteStone(BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE - 1);
                      deleteStone(BOARDSIZE * BOARDSIZE - 2);
                      setStone(BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE - 2);
                      return true;
                  }
              }
            }
          }
          if (isBorder(startPoint)) {
            if (startPoint < BOARDSIZE) {
              // ERROR
            }
            if (startPoint >= BOARDSIZE * BOARDSIZE - BOARDSIZE) {
              // ERROR
            }
            if (startPoint % BOARDSIZE == BOARDSIZE - 1) {
              switch (width) {
                case 3:
                case 4:
                  switch (height) {
                    case 5:
                    case 6:
                      deleteStone(startPoint + BOARDSIZE);
                      setStone(startPoint + BOARDSIZE - 1);
                      deleteStone(startPoint + 2 * BOARDSIZE - 1);
                      setStone(startPoint + 3 * BOARDSIZE);
                      setStone(startPoint + 3 * BOARDSIZE - 1);
                      return true;
                  }
              }
            }
          }
          switch (width) {
            case 4:
              switch (height) {
                case 5:
                case 6:
                  setStone(startPoint + BOARDSIZE - 1);
                  deleteStone(startPoint + BOARDSIZE - 2);
                  setStone(startPoint + 2 * BOARDSIZE - 1);
                  setStone(startPoint + 3 * BOARDSIZE - 1);
                  deleteStone(startPoint + 2 * BOARDSIZE);
                  return true;
              }
          }
        }
      case 3:
        if (height < width) {
          if (isCorner(startPoint)) {
            if (startPoint == BOARDSIZE - 1) {
              switch (height) {
                case 3:
                  switch (width) {
                    case 5:
                    case 6:
                      deleteStone(BOARDSIZE - 1);
                      deleteStone(BOARDSIZE - 2);
                      deleteStone(BOARDSIZE - 3);
                      deleteStone(BOARDSIZE - 4);
                      setStone(2 * BOARDSIZE - 1);
                      setStone(2 * BOARDSIZE - 2);
                      deleteStone(2 * BOARDSIZE - 4);
                      return true;
                  }
              }
            }
            if (startPoint == BOARDSIZE * BOARDSIZE - 1) {
              switch (height) {
                case 3:
                  switch (width) {
                    case 5:
                    case 6:
                      deleteStone(BOARDSIZE * BOARDSIZE - 1);
                      deleteStone(BOARDSIZE * BOARDSIZE - 2);
                      deleteStone(BOARDSIZE * BOARDSIZE - 3);
                      deleteStone(BOARDSIZE * BOARDSIZE - 4);
                      setStone(BOARDSIZE * BOARDSIZE - BOARDSIZE - 1);
                      setStone(BOARDSIZE * BOARDSIZE - BOARDSIZE - 2);
                      deleteStone(BOARDSIZE * BOARDSIZE - BOARDSIZE - 4);
                      return true;
                  }
              }
            }
          }
          if (isBorder(startPoint)) {
            if (startPoint < BOARDSIZE) {
              switch (height) {
                case 3:
                  switch (width) {
                    case 6:
                      deleteStone(startPoint + 1);
                      setStone(startPoint + 2);
                      deleteStone(startPoint + 3);
                      deleteStone(startPoint + BOARDSIZE + 2);
                      deleteStone(startPoint + BOARDSIZE + 3);
                      setStone(startPoint + BOARDSIZE + 4);
                      return true;
                  }
                case 4:
                  switch (width) {
                    case 5:
                    case 6:
                      deleteStone(startPoint + BOARDSIZE + 1);
                      deleteStone(startPoint + BOARDSIZE + 2);
                      setStone(startPoint + BOARDSIZE + 3);
                      setStone(startPoint + 2 * BOARDSIZE + 1);
                      deleteStone(startPoint + 2 * BOARDSIZE + 2);
                      deleteStone(startPoint + 2 * BOARDSIZE + 3);
                      return true;
                  }
              }
            }
            if (startPoint >= BOARDSIZE * BOARDSIZE - BOARDSIZE) {
              switch (height) {
                case 3:
                  switch (width) {
                    case 6:
                      deleteStone(startPoint + 1);
                      setStone(startPoint + 2);
                      deleteStone(startPoint + 3);
                      deleteStone(startPoint - BOARDSIZE + 2);
                      deleteStone(startPoint - BOARDSIZE + 3);
                      setStone(startPoint - BOARDSIZE + 4);
                      return true;
                  }
                case 4:
                  switch (width) {
                    case 5:
                    case 6:
                      deleteStone(startPoint - BOARDSIZE + 1);
                      deleteStone(startPoint - BOARDSIZE + 2);
                      setStone(startPoint - BOARDSIZE + 3);
                      setStone(startPoint - 2 * BOARDSIZE + 1);
                      deleteStone(startPoint - 2 * BOARDSIZE + 2);
                      deleteStone(startPoint - 2 * BOARDSIZE + 3);
                      return true;
                  }
              }
            }
            if (startPoint % BOARDSIZE == BOARDSIZE - 1) {
              // ERROR
            }
          }
          switch (height) {
            case 4:
              switch (width) {
                case 5:
                case 6:
                  deleteStone(startPoint + BOARDSIZE + 1);
                  deleteStone(startPoint + BOARDSIZE + 2);
                  setStone(startPoint + BOARDSIZE + 3);
                  setStone(startPoint + 2 * BOARDSIZE + 1);
                  deleteStone(startPoint + 2 * BOARDSIZE + 2);
                  deleteStone(startPoint + 2 * BOARDSIZE + 3);
                  return true;
              }
          }
        } else {
          if (isCorner(startPoint)) {
            if (startPoint == BOARDSIZE - 1) {
              switch (width) {
                case 3:
                case 4:
                  switch (height) {
                    case 5:
                    case 6:
                      deleteStone(BOARDSIZE - 1);
                      deleteStone(2 * BOARDSIZE - 1);
                      deleteStone(3 * BOARDSIZE - 1);
                      deleteStone(4 * BOARDSIZE - 1);
                      setStone(BOARDSIZE - 2);
                      setStone(2 * BOARDSIZE - 2);
                      deleteStone(4 * BOARDSIZE - 2);
                      return true;
                  }
              }
            }
            if (startPoint == BOARDSIZE * BOARDSIZE - 1) {
              switch (width) {
                case 3:
                case 4:
                  switch (height) {
                    case 5:
                    case 6:
                      deleteStone(startPoint);
                      deleteStone(startPoint - BOARDSIZE);
                      deleteStone(startPoint - 2 * BOARDSIZE);
                      deleteStone(startPoint - 3 * BOARDSIZE);
                      setStone(startPoint - 1);
                      setStone(startPoint - BOARDSIZE - 1);
                      deleteStone(startPoint - 3 * BOARDSIZE - 1);
                      return true;
                  }
              }
            }
          }
          if (isBorder(startPoint)) {
            if (startPoint < BOARDSIZE) {
              // ERROR
            }
            if (startPoint >= BOARDSIZE * BOARDSIZE - BOARDSIZE) {
              // ERROR
            }
            if (startPoint % BOARDSIZE == BOARDSIZE - 1) {
              switch (width) {
                case 3:
                  switch (height) {
                    case 6:
                      deleteStone(startPoint + BOARDSIZE);
                      setStone(startPoint + 2 * BOARDSIZE);
                      deleteStone(startPoint + 3 * BOARDSIZE);
                      deleteStone(startPoint + 2 * BOARDSIZE - 1);
                      deleteStone(startPoint + 3 * BOARDSIZE - 1);
                      setStone(startPoint + 4 * BOARDSIZE - 1);
                      return true;
                    case 4:
                      switch (height) {
                        case 5:
                        case 6:
                          setStone(startPoint + BOARDSIZE - 1);
                          deleteStone(startPoint + 2 * BOARDSIZE - 1);
                          deleteStone(startPoint + 3 * BOARDSIZE - 1);
                          deleteStone(startPoint + BOARDSIZE - 1);
                          deleteStone(startPoint + 2 * BOARDSIZE - 1);
                          setStone(startPoint + 3 * BOARDSIZE - 1);
                          return true;
                      }
                  }
              }
            }
            switch (width) {
              case 4:
                switch (height) {
                  case 5:
                  case 6:
                    setStone(startPoint + BOARDSIZE + 1);
                    deleteStone(startPoint + 2 * BOARDSIZE + 1);
                    deleteStone(startPoint + 3 * BOARDSIZE + 1);
                    deleteStone(startPoint + BOARDSIZE + 1);
                    deleteStone(startPoint + 2 * BOARDSIZE + 1);
                    setStone(startPoint + 3 * BOARDSIZE + 1);
                    return true;
                }
            }
          }
        }
    }
    return false;
  }

  /**
   * builds eyes for parts of main area which arn't connected to the main part for example if the
   * main area gets divided in two parts by the Separation group
   */
  private void createEyes2() {
    boolean[] eyelessArea = new boolean[BOARDSIZE * BOARDSIZE];
    boolean[] checkBoard = new boolean[BOARDSIZE * BOARDSIZE];

    for (int a = 0; a < BOARDSIZE * BOARDSIZE; a++) {
      if (area[a] == Area.MAINAREABLACK) {
        if (!checkBoard[a]) {
          Arrays.fill(checkBoard, false);
          if (!connectToArea2(a, checkBoard, eyelessArea, Area.EYESPACEBLACK, Stone.BLACK)) {
            Arrays.fill(checkBoard, false);
            if (playerInsideBlack
                ? !connectToArea2(a, checkBoard, eyelessArea, Area.SEPARATIONGROUPS, Stone.BLACK)
                : !connectToArea2(a, checkBoard, eyelessArea, Area.PROTECTEDAREA, Stone.BLACK)) {
              for (int b = 0; b < BOARDSIZE * BOARDSIZE; b++) {
                if (eyelessArea[b] && area[b] == Area.MAINAREABLACK) {
                  if (checkSpaceForLivingGroupAndChangeType(
                      b, Area.MAINAREABLACK, Area.EYESPACEBLACK)) {
                    placeBlackStones = true;
                    setUnprotectedLivingGroup(b);
                    Arrays.fill(eyelessArea, false);
                    break;
                  }
                }
                if (b
                    == BOARDSIZE * BOARDSIZE
                        - 1) { // if we can't get the group alive, we will change the color of the
                  // group
                  printError(36);
                  placeBlackStones = false;
                  for (int c = 0; c < BOARDSIZE * BOARDSIZE; c++) {
                    if (eyelessArea[c]) {
                      changeArea(c, Area.MAINAREAWHITE, Stone.WHITE);
                    }
                  }
                  Arrays.fill(checkBoard, false);
                  for (int c = 0; c < BOARDSIZE * BOARDSIZE; c++) {
                    if (playerInsideBlack) {
                      if (connectToArea(c, checkBoard, eyelessArea, Area.PROTECTEDAREA, Stone.WHITE)
                          && connectToArea(
                              c, checkBoard, eyelessArea, Area.EYESPACEWHITE, Stone.WHITE)) {
                        // TODO
                        printError(37);
                      }
                    } else {
                      if (connectToArea(
                              c, checkBoard, eyelessArea, Area.SEPARATIONGROUPS, Stone.WHITE)
                          && connectToArea(
                              c, checkBoard, eyelessArea, Area.EYESPACEWHITE, Stone.WHITE)) {
                        // TODO
                        printError(37);
                      }
                    }
                    Arrays.fill(eyelessArea, false);
                  }
                }
              }
            }
          }
          Arrays.fill(checkBoard, false);
        }
      }
      if (area[a] == Area.MAINAREAWHITE) {
        placeBlackStones = false;
        if (!checkBoard[a]) {
          Arrays.fill(checkBoard, false);
          if (!connectToArea2(a, checkBoard, eyelessArea, Area.EYESPACEWHITE, Stone.WHITE)) {
            Arrays.fill(checkBoard, false);
            if (!playerInsideBlack
                ? !connectToArea2(a, checkBoard, eyelessArea, Area.SEPARATIONGROUPS, Stone.WHITE)
                : !connectToArea2(a, checkBoard, eyelessArea, Area.PROTECTEDAREA, Stone.WHITE)) {
              for (int b = 0; b < BOARDSIZE * BOARDSIZE; b++) {
                if (eyelessArea[b] && area[b] == Area.MAINAREAWHITE) {
                  if (checkSpaceForLivingGroupAndChangeType(
                      b, Area.MAINAREAWHITE, Area.EYESPACEWHITE)) {
                    placeBlackStones = false;
                    setUnprotectedLivingGroup(b);
                    Arrays.fill(eyelessArea, false);
                    break;
                  }
                }
                if (b
                    == BOARDSIZE * BOARDSIZE
                        - 1) { // if we can't get the group alive, we will change the color of the
                  // group
                  printError(36);
                  placeBlackStones = true;
                  for (int c = 0; c < BOARDSIZE * BOARDSIZE; c++) {
                    if (eyelessArea[c]) {
                      changeArea(c, Area.MAINAREABLACK, Stone.BLACK);
                    }
                  }
                  Arrays.fill(checkBoard, false);
                  for (int c = 0; c < BOARDSIZE * BOARDSIZE; c++) {
                    if (!playerInsideBlack) {
                      if (connectToArea(c, checkBoard, eyelessArea, Area.PROTECTEDAREA, Stone.BLACK)
                          && connectToArea(
                              c, checkBoard, eyelessArea, Area.EYESPACEBLACK, Stone.BLACK)) {
                        // TODO
                        printError(37);
                      }
                    } else {
                      if (connectToArea(
                              c, checkBoard, eyelessArea, Area.SEPARATIONGROUPS, Stone.BLACK)
                          && connectToArea(
                              c, checkBoard, eyelessArea, Area.EYESPACEBLACK, Stone.BLACK)) {
                        // TODO
                        printError(37);
                      }
                    }
                    Arrays.fill(eyelessArea, false);
                  }
                }
              }
              Arrays.fill(checkBoard, false);
            }
          }
        }
      }
    }
  }

  /** searches in main areas space to make eyes */
  private void createEyeSpace() {
    if (!createEyeSpaceBlack()) {
      // ERROR nicht genug raum für augen
      printError(28);
    }
    if (!createEyeSpaceWhite()) {
      // ERROR nicht genug raum für augen
      printError(29);
    }
  }

  /**
   * searches eyespace for blacks main area
   *
   * @return true if found some eyespace
   */
  private boolean createEyeSpaceBlack() {
    placeBlackStones = true;
    if (allowKo) {
      if (checkSquareAndChange(0, 3, 5, Area.MAINAREABLACK, Area.EYESPACEBLACK)) {
        maxKoThreatsWhite = 3;
        return true;
      }
      if (checkSquareAndChange(0, 5, 3, Area.MAINAREABLACK, Area.EYESPACEBLACK)) {
        maxKoThreatsWhite = 3;
        return true;
      }
      if (checkSquareAndChange(
          BOARDSIZE * BOARDSIZE - 5 * BOARDSIZE, 3, 5, Area.MAINAREABLACK, Area.EYESPACEBLACK)) {
        maxKoThreatsWhite = 3;
        return true;
      }
      if (checkSquareAndChange(
          BOARDSIZE * BOARDSIZE - 3 * BOARDSIZE, 5, 3, Area.MAINAREABLACK, Area.EYESPACEBLACK)) {
        maxKoThreatsWhite = 3;
        return true;
      }
      for (int i = 0; i < BOARDSIZE; i++) {
        if (checkSquareAndChange(i * BOARDSIZE, 3, 6, Area.MAINAREABLACK, Area.EYESPACEBLACK)) {
          maxKoThreatsWhite = 3;
          return true;
        }
        if (checkSquareAndChange(i, 6, 3, Area.MAINAREABLACK, Area.EYESPACEBLACK)) {
          maxKoThreatsWhite = 3;
          return true;
        }
        if (checkSquareAndChange(
            BOARDSIZE * BOARDSIZE - 1 - i, 6, 3, Area.MAINAREABLACK, Area.EYESPACEBLACK)) {
          maxKoThreatsWhite = 3;
          return true;
        }
        if (checkSquareAndChange(i * BOARDSIZE, 4, 5, Area.MAINAREABLACK, Area.EYESPACEBLACK)) {
          maxKoThreatsWhite = 3;
          return true;
        }
        if (checkSquareAndChange(i, 5, 4, Area.MAINAREABLACK, Area.EYESPACEBLACK)) {
          maxKoThreatsWhite = 3;
          return true;
        }
        if (checkSquareAndChange(
            BOARDSIZE * BOARDSIZE - 1 - i, 5, 4, Area.MAINAREABLACK, Area.EYESPACEBLACK)) {
          maxKoThreatsWhite = 3;
          return true;
        }
      }
      for (int a = 0; a < BOARDSIZE * BOARDSIZE; a++) {
        if (checkSquareAndChange(a, 4, 5, Area.MAINAREABLACK, Area.EYESPACEBLACK)) {
          maxKoThreatsWhite = 3;
          return true;
        }
        if (checkSquareAndChange(a, 5, 4, Area.MAINAREABLACK, Area.EYESPACEBLACK)) {
          maxKoThreatsWhite = 3;
          return true;
        }
      }
      // if we don't find enough space for 3 Ko threats, try to find enough space for 2 Ko threats
      if (checkSquareAndChange(0, 3, 4, Area.MAINAREABLACK, Area.EYESPACEBLACK)) {
        maxKoThreatsWhite = 2;
        return true;
      }
      if (checkSquareAndChange(0, 4, 3, Area.MAINAREABLACK, Area.EYESPACEBLACK)) {
        maxKoThreatsWhite = 2;
        return true;
      }
      if (checkSquareAndChange(
          BOARDSIZE * BOARDSIZE - 4 * BOARDSIZE, 3, 4, Area.MAINAREABLACK, Area.EYESPACEBLACK)) {
        maxKoThreatsWhite = 2;
        return true;
      }
      if (checkSquareAndChange(
          BOARDSIZE * BOARDSIZE - 3 * BOARDSIZE, 4, 3, Area.MAINAREABLACK, Area.EYESPACEBLACK)) {
        maxKoThreatsWhite = 2;
        return true;
      }
      for (int i = 0; i < BOARDSIZE; i++) {
        if (checkSquareAndChange(i * BOARDSIZE, 3, 5, Area.MAINAREABLACK, Area.EYESPACEBLACK)) {
          maxKoThreatsWhite = 2;
          return true;
        }
        if (checkSquareAndChange(i, 5, 3, Area.MAINAREABLACK, Area.EYESPACEBLACK)) {
          maxKoThreatsWhite = 2;
          return true;
        }
        if (checkSquareAndChange(
            BOARDSIZE * BOARDSIZE - 1 - i, 5, 3, Area.MAINAREABLACK, Area.EYESPACEBLACK)) {
          maxKoThreatsWhite = 2;
          return true;
        }
      }
      // if we don't find enough space for 2 Ko threats, try to find enough space for 1 Ko threats
      if (checkSquareAndChange(0, 2, 5, Area.MAINAREABLACK, Area.EYESPACEBLACK)) {
        maxKoThreatsWhite = 1;
        return true;
      }
      if (checkSquareAndChange(0, 5, 2, Area.MAINAREABLACK, Area.EYESPACEBLACK)) {
        maxKoThreatsWhite = 1;
        return true;
      }
      if (checkSquareAndChange(
          BOARDSIZE * BOARDSIZE - 5 * BOARDSIZE, 2, 5, Area.MAINAREABLACK, Area.EYESPACEBLACK)) {
        maxKoThreatsWhite = 1;
        return true;
      }
      if (checkSquareAndChange(
          BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE, 5, 2, Area.MAINAREABLACK, Area.EYESPACEBLACK)) {
        maxKoThreatsWhite = 1;
        return true;
      }
      for (int i = 0; i < BOARDSIZE; i++) {
        if (checkSquareAndChange(i * BOARDSIZE, 2, 6, Area.MAINAREABLACK, Area.EYESPACEBLACK)) {
          maxKoThreatsWhite = 1;
          return true;
        }
        if (checkSquareAndChange(i, 6, 2, Area.MAINAREABLACK, Area.EYESPACEBLACK)) {
          maxKoThreatsWhite = 1;
          return true;
        }
        if (checkSquareAndChange(
            BOARDSIZE * BOARDSIZE - 1 - i, 6, 2, Area.MAINAREABLACK, Area.EYESPACEBLACK)) {
          maxKoThreatsWhite = 1;
          return true;
        }
      }
      for (int a = 0; a < BOARDSIZE * BOARDSIZE; a++) {
        if (checkSquareAndChange(a, 3, 6, Area.MAINAREABLACK, Area.EYESPACEBLACK)) {
          maxKoThreatsWhite = 1;
          return true;
        }
        if (checkSquareAndChange(a, 6, 3, Area.MAINAREABLACK, Area.EYESPACEBLACK)) {
          maxKoThreatsWhite = 1;
          return true;
        }
      }
    }
    // if we don't allow Ko or don't find enough space for a Ko threat
    if (checkSpaceForLivingGroupAndChangeType(0, Area.MAINAREABLACK, Area.EYESPACEBLACK)) {
      return true;
    }
    if (checkSpaceForLivingGroupAndChangeType(
        BOARDSIZE * BOARDSIZE - BOARDSIZE, Area.MAINAREABLACK, Area.EYESPACEBLACK)) {
      return true;
    }
    for (int a = 0; a < BOARDSIZE * BOARDSIZE; a++) {
      if (isBorder(a)) {
        if (checkSpaceForLivingGroupAndChangeType(a, Area.MAINAREABLACK, Area.EYESPACEBLACK)) {
          return true;
        }
      }
    }
    for (int a = 0; a < BOARDSIZE * BOARDSIZE; a++) {
      if (checkSpaceForLivingGroupAndChangeType(a, Area.MAINAREABLACK, Area.EYESPACEBLACK)) {
        return true;
      }
    }
    return false;
  }

  /**
   * searches eyespace for whites main area
   *
   * @return true if found some eyespace
   */
  private boolean createEyeSpaceWhite() {
    placeBlackStones = false;
    if (allowKo) {
      if (checkSquareAndChange(BOARDSIZE - 3, 3, 5, Area.MAINAREAWHITE, Area.EYESPACEWHITE)) {
        maxKoThreatsBlack = 3;
        return true;
      }
      if (checkSquareAndChange(BOARDSIZE - 5, 5, 3, Area.MAINAREAWHITE, Area.EYESPACEWHITE)) {
        maxKoThreatsBlack = 3;
        return true;
      }
      if (checkSquareAndChange(
          BOARDSIZE * BOARDSIZE - 5 * BOARDSIZE - 3,
          3,
          5,
          Area.MAINAREAWHITE,
          Area.EYESPACEWHITE)) {
        maxKoThreatsBlack = 3;
        return true;
      }
      if (checkSquareAndChange(
          BOARDSIZE * BOARDSIZE - 3 * BOARDSIZE - 5,
          5,
          3,
          Area.MAINAREAWHITE,
          Area.EYESPACEWHITE)) {
        maxKoThreatsBlack = 3;
        return true;
      }
      for (int i = 0; i < BOARDSIZE; i++) {
        if (checkSquareAndChange(
            i * BOARDSIZE + BOARDSIZE - 1, 3, 6, Area.MAINAREAWHITE, Area.EYESPACEWHITE)) {
          maxKoThreatsBlack = 3;
          return true;
        }
        if (checkSquareAndChange(i, 6, 3, Area.MAINAREAWHITE, Area.EYESPACEWHITE)) {
          maxKoThreatsBlack = 3;
          return true;
        }
        if (checkSquareAndChange(
            BOARDSIZE * BOARDSIZE - 1 - i, 6, 3, Area.MAINAREAWHITE, Area.EYESPACEWHITE)) {
          maxKoThreatsBlack = 3;
          return true;
        }
        if (checkSquareAndChange(
            i * BOARDSIZE + BOARDSIZE - 1, 4, 5, Area.MAINAREAWHITE, Area.EYESPACEWHITE)) {
          maxKoThreatsBlack = 3;
          return true;
        }
        if (checkSquareAndChange(i, 5, 4, Area.MAINAREAWHITE, Area.EYESPACEWHITE)) {
          maxKoThreatsBlack = 3;
          return true;
        }
        if (checkSquareAndChange(
            BOARDSIZE * BOARDSIZE - 1 - i, 5, 4, Area.MAINAREAWHITE, Area.EYESPACEWHITE)) {
          maxKoThreatsBlack = 3;
          return true;
        }
      }
      for (int a = 0; a < BOARDSIZE * BOARDSIZE; a++) {
        if (checkSquareAndChange(a, 4, 5, Area.MAINAREAWHITE, Area.EYESPACEWHITE)) {
          maxKoThreatsBlack = 3;
          return true;
        }
        if (checkSquareAndChange(a, 5, 4, Area.MAINAREAWHITE, Area.EYESPACEWHITE)) {
          maxKoThreatsBlack = 3;
          return true;
        }
      }
      // if we don't find enough space for 3 Ko threats, try to find enough space for 2 Ko threats
      if (checkSquareAndChange(BOARDSIZE - 3, 3, 4, Area.MAINAREAWHITE, Area.EYESPACEWHITE)) {
        maxKoThreatsBlack = 2;
        return true;
      }
      if (checkSquareAndChange(BOARDSIZE - 4, 4, 3, Area.MAINAREAWHITE, Area.EYESPACEWHITE)) {
        maxKoThreatsBlack = 2;
        return true;
      }
      if (checkSquareAndChange(
          BOARDSIZE * BOARDSIZE - 4 * BOARDSIZE - 3,
          3,
          4,
          Area.MAINAREAWHITE,
          Area.EYESPACEWHITE)) {
        maxKoThreatsBlack = 2;
        return true;
      }
      if (checkSquareAndChange(
          BOARDSIZE * BOARDSIZE - 3 * BOARDSIZE - 4,
          4,
          3,
          Area.MAINAREAWHITE,
          Area.EYESPACEWHITE)) {
        maxKoThreatsBlack = 2;
        return true;
      }
      for (int i = 0; i < BOARDSIZE; i++) {
        if (checkSquareAndChange(
            i * BOARDSIZE + BOARDSIZE - 1, 3, 5, Area.MAINAREAWHITE, Area.EYESPACEWHITE)) {
          maxKoThreatsBlack = 2;
          return true;
        }
        if (checkSquareAndChange(i, 5, 3, Area.MAINAREAWHITE, Area.EYESPACEWHITE)) {
          maxKoThreatsBlack = 2;
          return true;
        }
        if (checkSquareAndChange(
            BOARDSIZE * BOARDSIZE - 1 - i, 5, 3, Area.MAINAREAWHITE, Area.EYESPACEWHITE)) {
          maxKoThreatsBlack = 2;
          return true;
        }
      }
      // if we don't find enough space for 2 Ko threats, try to find enough space for 1 Ko threats
      if (checkSquareAndChange(BOARDSIZE - 2, 2, 5, Area.MAINAREAWHITE, Area.EYESPACEWHITE)) {
        maxKoThreatsBlack = 1;
        return true;
      }
      if (checkSquareAndChange(BOARDSIZE - 5, 5, 2, Area.MAINAREAWHITE, Area.EYESPACEWHITE)) {
        maxKoThreatsBlack = 1;
        return true;
      }
      if (checkSquareAndChange(
          BOARDSIZE * BOARDSIZE - 5 * BOARDSIZE - 2,
          2,
          5,
          Area.MAINAREAWHITE,
          Area.EYESPACEWHITE)) {
        maxKoThreatsBlack = 1;
        return true;
      }
      if (checkSquareAndChange(
          BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE - 5,
          5,
          2,
          Area.MAINAREAWHITE,
          Area.EYESPACEWHITE)) {
        maxKoThreatsBlack = 1;
        return true;
      }
      for (int i = 0; i < BOARDSIZE; i++) {
        if (checkSquareAndChange(
            i * BOARDSIZE + BOARDSIZE - 1, 2, 6, Area.MAINAREAWHITE, Area.EYESPACEWHITE)) {
          maxKoThreatsBlack = 1;
          return true;
        }
        if (checkSquareAndChange(i, 6, 2, Area.MAINAREAWHITE, Area.EYESPACEWHITE)) {
          maxKoThreatsBlack = 1;
          return true;
        }
        if (checkSquareAndChange(
            BOARDSIZE * BOARDSIZE - 1 - i, 6, 2, Area.MAINAREAWHITE, Area.EYESPACEWHITE)) {
          maxKoThreatsBlack = 1;
          return true;
        }
      }
      for (int a = 0; a < BOARDSIZE * BOARDSIZE; a++) {
        if (checkSquareAndChange(a, 3, 6, Area.MAINAREAWHITE, Area.EYESPACEWHITE)) {
          maxKoThreatsBlack = 1;
          return true;
        }
        if (checkSquareAndChange(a, 6, 3, Area.MAINAREAWHITE, Area.EYESPACEWHITE)) {
          maxKoThreatsBlack = 1;
          return true;
        }
      }
    }
    // if we don't allow Ko or don't find enough space for a Ko threat
    maxKoThreatsBlack = 0;
    if (checkSpaceForLivingGroupAndChangeType(
        BOARDSIZE * BOARDSIZE - 1, Area.MAINAREAWHITE, Area.EYESPACEWHITE)) {
      return true;
    }
    if (checkSpaceForLivingGroupAndChangeType(
        BOARDSIZE - 1, Area.MAINAREAWHITE, Area.EYESPACEWHITE)) {
      return true;
    }
    for (int a = 0; a < BOARDSIZE * BOARDSIZE; a++) {
      if (isBorder(a)) {
        if (checkSpaceForLivingGroupAndChangeType(a, Area.MAINAREABLACK, Area.EYESPACEBLACK)) {
          return true;
        }
      }
    }
    for (int a = 0; a < BOARDSIZE * BOARDSIZE; a++) {
      if (checkSpaceForLivingGroupAndChangeType(a, Area.MAINAREAWHITE, Area.EYESPACEWHITE)) {
        return true;
      }
    }
    return false;
  }

  private boolean buildEyesForWallHelper() {
    if (playerInsideBlack) {
      for (int i = 0; i < 8; i++) {
        for (int j = 0; j < BOARDSIZE; j++) {
          if (check8Neighborhood(getField(i, j), Area.PROTECTEDAREA)) {
            if (yCoord(wallCornerPoint[0])
                != 0) // reset Area status of wall to EMPTY so we can use the wall stones for a
              // living group
              setAreaLine(wallCornerPoint[0], wallCornerPoint[1], Area.EMPTY);
            if (xCoord(wallCornerPoint[1]) != BOARDSIZE - 1)
              setAreaLine(wallCornerPoint[1], wallCornerPoint[3], Area.EMPTY);
            if (yCoord(wallCornerPoint[2]) != BOARDSIZE - 1)
              setAreaLine(wallCornerPoint[2], wallCornerPoint[3], Area.EMPTY);
            if (xCoord(wallCornerPoint[2]) != 0)
              setAreaLine(wallCornerPoint[2], wallCornerPoint[0], Area.EMPTY);
            if (checkSpaceForLivingGroupAndChangeType2(
                getField(i, j), Area.PROTECTEDAREA, Area.PROTECTEDAREA)) {
              setLivingGroup(getField(i, j));
            }
            if (yCoord(wallCornerPoint[0]) != 0) // protect the wall again
            setAreaLine(wallCornerPoint[0], wallCornerPoint[1], Area.PROTECTEDAREA);
            if (xCoord(wallCornerPoint[1]) != BOARDSIZE - 1)
              setAreaLine(wallCornerPoint[1], wallCornerPoint[3], Area.PROTECTEDAREA);
            if (yCoord(wallCornerPoint[2]) != BOARDSIZE - 1)
              setAreaLine(wallCornerPoint[2], wallCornerPoint[3], Area.PROTECTEDAREA);
            if (xCoord(wallCornerPoint[2]) != 0)
              setAreaLine(wallCornerPoint[2], wallCornerPoint[0], Area.PROTECTEDAREA);
            return true;
          }
        }
      }
      for (int i = 8; i < BOARDSIZE; i++) {
        for (int j = 0; j < BOARDSIZE; j++) {
          if (check8Neighborhood(getField(i, j), Area.PROTECTEDAREA)) {
            if (yCoord(wallCornerPoint[0])
                != 0) // reset Area status of wall to EMPTY so we can use the wall stones for a
              // living group
              setAreaLine(wallCornerPoint[0], wallCornerPoint[1], Area.EMPTY);
            if (xCoord(wallCornerPoint[1]) != BOARDSIZE - 1)
              setAreaLine(wallCornerPoint[1], wallCornerPoint[3], Area.EMPTY);
            if (yCoord(wallCornerPoint[2]) != BOARDSIZE - 1)
              setAreaLine(wallCornerPoint[2], wallCornerPoint[3], Area.EMPTY);
            if (xCoord(wallCornerPoint[2]) != 0)
              setAreaLine(wallCornerPoint[2], wallCornerPoint[0], Area.EMPTY);
            if (checkSpaceForLivingGroupAndChangeType2(
                getField(i, j), Area.PROTECTEDAREA, Area.PROTECTEDAREA)) {
              setLivingGroup(getField(i, j));
            }
            if (yCoord(wallCornerPoint[0]) != 0) // protect the wall again
            setAreaLine(wallCornerPoint[0], wallCornerPoint[1], Area.PROTECTEDAREA);
            if (xCoord(wallCornerPoint[1]) != BOARDSIZE - 1)
              setAreaLine(wallCornerPoint[1], wallCornerPoint[3], Area.PROTECTEDAREA);
            if (yCoord(wallCornerPoint[2]) != BOARDSIZE - 1)
              setAreaLine(wallCornerPoint[2], wallCornerPoint[3], Area.PROTECTEDAREA);
            if (xCoord(wallCornerPoint[2]) != 0)
              setAreaLine(wallCornerPoint[2], wallCornerPoint[0], Area.PROTECTEDAREA);
            return true;
          }
        }
      }
    } else {
      for (int i = BOARDSIZE; i > 12; i--) {
        for (int j = 0; j < BOARDSIZE; j++) {
          if (check8Neighborhood(getField(i, j), Area.PROTECTEDAREA)) {
            if (yCoord(wallCornerPoint[0])
                != 0) // reset Area status of wall to EMPTY so we can use the wall stones for a
              // living group
              setAreaLine(wallCornerPoint[0], wallCornerPoint[1], Area.EMPTY);
            if (xCoord(wallCornerPoint[1]) != BOARDSIZE - 1)
              setAreaLine(wallCornerPoint[1], wallCornerPoint[3], Area.EMPTY);
            if (yCoord(wallCornerPoint[2]) != BOARDSIZE - 1)
              setAreaLine(wallCornerPoint[2], wallCornerPoint[3], Area.EMPTY);
            if (xCoord(wallCornerPoint[2]) != 0)
              setAreaLine(wallCornerPoint[2], wallCornerPoint[0], Area.EMPTY);
            if (checkSpaceForLivingGroupAndChangeType2(
                getField(i, j), Area.PROTECTEDAREA, Area.PROTECTEDAREA)) {
              setLivingGroup(getField(i, j));
            }
            if (yCoord(wallCornerPoint[0]) != 0) // protect the wall again
            setAreaLine(wallCornerPoint[0], wallCornerPoint[1], Area.PROTECTEDAREA);
            if (xCoord(wallCornerPoint[1]) != BOARDSIZE - 1)
              setAreaLine(wallCornerPoint[1], wallCornerPoint[3], Area.PROTECTEDAREA);
            if (yCoord(wallCornerPoint[2]) != BOARDSIZE - 1)
              setAreaLine(wallCornerPoint[2], wallCornerPoint[3], Area.PROTECTEDAREA);
            if (xCoord(wallCornerPoint[2]) != 0)
              setAreaLine(wallCornerPoint[2], wallCornerPoint[0], Area.PROTECTEDAREA);
            return true;
          }
        }
      }
      for (int i = 12; i >= 0; i--) {
        for (int j = 0; j < BOARDSIZE; j++) {
          if (check8Neighborhood(getField(i, j), Area.PROTECTEDAREA)) {
            if (yCoord(wallCornerPoint[0])
                != 0) // reset Area status of wall to EMPTY so we can use the wall stones for a
              // living group
              setAreaLine(wallCornerPoint[0], wallCornerPoint[1], Area.EMPTY);
            if (xCoord(wallCornerPoint[1]) != BOARDSIZE - 1)
              setAreaLine(wallCornerPoint[1], wallCornerPoint[3], Area.EMPTY);
            if (yCoord(wallCornerPoint[2]) != BOARDSIZE - 1)
              setAreaLine(wallCornerPoint[2], wallCornerPoint[3], Area.EMPTY);
            if (xCoord(wallCornerPoint[2]) != 0)
              setAreaLine(wallCornerPoint[2], wallCornerPoint[0], Area.EMPTY);
            if (checkSpaceForLivingGroupAndChangeType2(
                getField(i, j), Area.PROTECTEDAREA, Area.PROTECTEDAREA)) {
              setLivingGroup(getField(i, j));
            }
            if (yCoord(wallCornerPoint[0]) != 0) // protect the wall again
            setAreaLine(wallCornerPoint[0], wallCornerPoint[1], Area.PROTECTEDAREA);
            if (xCoord(wallCornerPoint[1]) != BOARDSIZE - 1)
              setAreaLine(wallCornerPoint[1], wallCornerPoint[3], Area.PROTECTEDAREA);
            if (yCoord(wallCornerPoint[2]) != BOARDSIZE - 1)
              setAreaLine(wallCornerPoint[2], wallCornerPoint[3], Area.PROTECTEDAREA);
            if (xCoord(wallCornerPoint[2]) != 0)
              setAreaLine(wallCornerPoint[2], wallCornerPoint[0], Area.PROTECTEDAREA);
            return true;
          }
        }
      }
    }
    return false;
  }

  // TODO alive points
  // TODO more dynamic
  /**
   * builds eyes for wall connects as Alive marked Points to the outside and builds eyes for them
   */
  private void buildEyesForWallAndAlivePoints() {
    placeLivingGroupesVertical = false;
    placeBlackStones = !playerInsideBlack;

    if (!buildEyesForWallHelper()) {
      placeLivingGroupesVertical = true;
      if (!buildEyesForWallHelper()) {
        // ERROR
      }
    }
  }

  /**
   * protects all fields in a square with corner points a and b. also sets points for the
   * surrounding wall later.
   */
  private void protectFieldSquareAndSetWall(int a, int b) {

    wallCornerPoint = new int[4];
    int x_1, x_2, y_1, y_2;
    if (xCoord(a) < xCoord(b)) {
      x_1 = xCoord(a);
      x_2 = xCoord(b);
    } else {
      x_1 = xCoord(b);
      x_2 = xCoord(a);
    }
    if (yCoord(a) < yCoord(b)) {
      y_1 = yCoord(a);
      y_2 = yCoord(b);
    } else {
      y_1 = yCoord(b);
      y_2 = yCoord(a);
    }
    // protect selected area
    for (int i = x_1; i <= x_2; i++) {
      for (int j = y_1; j <= y_2; j++) {
        area[getField(i, j)] = Area.PROTECTEDAREA;
      }
    }
    // clear and protect area between wall and selected area
    int wallDistanceLeft = Math.min(wallDistance, x_1);
    int wallDistanceRight = Math.min(wallDistance, BOARDSIZE - x_2 - 1);
    int wallDistanceBottom = Math.min(wallDistance, y_1);
    int wallDistanceTop = Math.min(wallDistance, BOARDSIZE - y_2 - 1);

    for (int i = x_1 - wallDistanceLeft; i <= x_2 + wallDistanceRight; i++) {
      for (int j = y_1 - wallDistanceBottom; j <= y_2 + wallDistanceTop; j++) {
        if (area[getField(i, j)] != Area.PROTECTEDAREA) {
          area[getField(i, j)] = Area.PROTECTEDAREA;
          board[getField(i, j)] = Stone.EMPTY;
        }
      }
    }
    wallCornerPoint[0] = getField(x_1 - wallDistanceLeft, y_1 - wallDistanceBottom);
    wallCornerPoint[1] = getField(x_2 + wallDistanceRight, y_1 - wallDistanceBottom);
    wallCornerPoint[2] = getField(x_1 - wallDistanceLeft, y_2 + wallDistanceTop);
    wallCornerPoint[3] = getField(x_2 + wallDistanceRight, y_2 + wallDistanceTop);

    // set surrounding wall
    placeBlackStones = !playerInsideBlack;
    if (yCoord(wallCornerPoint[0]) != 0) setStoneLine(wallCornerPoint[0], wallCornerPoint[1]);
    if (xCoord(wallCornerPoint[1]) != BOARDSIZE - 1)
      setStoneLine(wallCornerPoint[1], wallCornerPoint[3]);
    if (yCoord(wallCornerPoint[2]) != BOARDSIZE - 1)
      setStoneLine(wallCornerPoint[2], wallCornerPoint[3]);
    if (xCoord(wallCornerPoint[2]) != 0) setStoneLine(wallCornerPoint[2], wallCornerPoint[0]);
  }

  // TODO handle unconnected groups
  /** separates main area from wall so we can use main area to create Ko threats */
  private void separateMainAreas() {
    if (playerInsideBlack) {
      placeBlackStones = true;
      int[] separateAreaBlack =
          new int[50]; // local variable to remember stones we are going to change
      int k = 0; // number of stones we are going to change

      for (int i = 0; i < BOARDSIZE * BOARDSIZE; i++) { // blacks main area
        if (area[i] == Area.MAINAREAWHITE && check8Neighborhood(i, Area.PROTECTEDAREA)) {
          separateAreaBlack[k] = i;
          k++;
          if (k > 50) {
            printError(23);
            return;
            // TODO ERROR should have used bigger k
          }
        }
      }
      if (k <= 0) {
        printError(32);
        return;
      }
      // TODO check if we got a connected group else throw mistake
      // set eyes for separation group
      placeLivingGroupesVertical = true;
      dummy2 = false;
      for (int i = 0; i < k; i++) {
        if (setLivingGroupForSeparation(separateAreaBlack[i])
            || setLivingGroupForSeparation(separateAreaBlack[i] - 1)
            || setLivingGroupForSeparation(separateAreaBlack[i] + BOARDSIZE)
            || setLivingGroupForSeparation(separateAreaBlack[i] - BOARDSIZE)) {
          dummy2 = true;
          break;
        }
      }
      if (!dummy2) { // might lead to different bugs, but worth a try if we didn't find space for
        // our eyes so far
        dummy2 = true;
        printError(31);
        for (int i = 0; i < k; i++) {
          if (setLivingGroupForSeparation(separateAreaBlack[i])
              || setLivingGroupForSeparation(separateAreaBlack[i] - 1)
              || setLivingGroupForSeparation(separateAreaBlack[i] + BOARDSIZE)
              || setLivingGroupForSeparation(separateAreaBlack[i] - BOARDSIZE)) {
            dummy2 = false;
            break;
          }
        }
        if (dummy2) {
          // ERROR if we don't find enough space to place a living group
          printError(30);
        }
      }
      for (int i = 0; i < k; i++) {
        if (area[separateAreaBlack[i]] != Area.SEPARATIONGROUPS) {
          area[separateAreaBlack[i]] = Area.SEPARATIONGROUPS;
          board[separateAreaBlack[i]] = Stone.BLACK;
        }
      }

      boolean repeat = true; // necessary if the separation group is divided into multiple parts
      int counter = 0;
      while (repeat) {
        if (counter > k) {
          printError(40);
          break;
        }
        repeat = false;
        for (int i = 0; i < k; i++) {
          if (!localLibertyCheck(separateAreaBlack[i])) {
            if (!(setLivingGroupForSeparation(separateAreaBlack[i])
                || setLivingGroupForSeparation(separateAreaBlack[i] - 1)
                || setLivingGroupForSeparation(separateAreaBlack[i] + BOARDSIZE)
                || setLivingGroupForSeparation(separateAreaBlack[i] - BOARDSIZE))) {
              repeat = true;
              counter++;
              break;
            }
          }
        }
      }

      // separate SEPARATIONGROUPS and other main area
      placeBlackStones = false;
      for (int a = 0; a < BOARDSIZE * BOARDSIZE; a++) {
        if (area[a] == Area.MAINAREABLACK) {
          if (check4Neighborhood(a, Area.SEPARATIONGROUPS)) {
            setStone(a);
            boolean[] checkBoard = new boolean[BOARDSIZE * BOARDSIZE];
            if (!connectToAreaAndChange(a, checkBoard, Area.PROTECTEDAREA, Stone.WHITE)) {
              boolean[] checkBoard2 = new boolean[BOARDSIZE * BOARDSIZE];
              connectToAreaAndChange(a, checkBoard2, Area.MAINAREAWHITE, Stone.WHITE);
            }
          }
        }
      }
      for (int a = 0; a < BOARDSIZE * BOARDSIZE; a++) {
        if (area[a] == Area.MAINAREABLACK) {
          if (check4Neighborhood(a, Area.SEPARATIONGROUPS)) {
            setStone(a);
            boolean[] checkBoard = new boolean[BOARDSIZE * BOARDSIZE];
            if (!connectToAreaAndChange(a, checkBoard, Area.PROTECTEDAREA, Stone.WHITE)) {
              boolean[] checkBoard2 = new boolean[BOARDSIZE * BOARDSIZE];
              if (!connectToAreaAndChange(a, checkBoard2, Area.MAINAREAWHITE, Stone.WHITE)) {
                if (!check4Neighborhood(a, Area.PROTECTEDAREA)) {
                  if (check4Neighborhood(a + 1, Area.PROTECTEDAREA)
                      && area[a + 1] != Area.SEPARATIONGROUPS) {
                    setProtectedStone(a + 1);
                  } else {
                    if (check4Neighborhood(a - 1, Area.PROTECTEDAREA)
                        && area[a - 1] != Area.SEPARATIONGROUPS) {
                      setProtectedStone(a - 1);
                    } else {
                      if (check4Neighborhood(a + BOARDSIZE, Area.PROTECTEDAREA)
                          && area[a + BOARDSIZE] != Area.SEPARATIONGROUPS) {
                        setProtectedStone(a + BOARDSIZE);
                      } else {
                        if (check4Neighborhood(a - BOARDSIZE, Area.PROTECTEDAREA)
                            && area[a - BOARDSIZE] != Area.SEPARATIONGROUPS) {
                          setProtectedStone(a - BOARDSIZE);
                        } else {
                          printError(34);
                        }
                      }
                    }
                  }
                }
                boolean[] checkBoard3 = new boolean[BOARDSIZE * BOARDSIZE];
                connectToAreaAndChange(a, checkBoard3, Area.PROTECTEDAREA, Stone.WHITE);
              }
            }
          }
        }
      }
    }

    // white
    else {
      placeBlackStones = false;
      int[] separateAreaWhite = new int[50];
      ; // local variable to remember stones we are going to change
      int k = 0; // number of stones we are going to change

      for (int i = 0; i < BOARDSIZE * BOARDSIZE; i++) { // whites main area
        if (area[i] == Area.MAINAREABLACK && check8Neighborhood(i, Area.PROTECTEDAREA)) {
          separateAreaWhite[k] = i;
          k++;
          if (k > 50) {
            printError(22);
            return;
            // TODO ERROR should have used bigger k
          }
        }
      }
      // TODO check if we got a connected group else throw mistake

      // set eyes for separation group
      placeLivingGroupesVertical = true;
      dummy2 = false;
      for (int i = 0; i < k; i++) {
        if (setLivingGroupForSeparation(separateAreaWhite[i])
            || setLivingGroupForSeparation(separateAreaWhite[i] + 1)
            || setLivingGroupForSeparation(separateAreaWhite[i] + BOARDSIZE)
            || setLivingGroupForSeparation(separateAreaWhite[i] - BOARDSIZE)) {
          dummy2 = true;
          break;
        }
      }
      if (!dummy2) { // might lead to different bugs, but worth a try if we didn't find space for
        // our eyes so far
        dummy2 = true;
        printError(31);
        for (int i = 0; i < k; i++) {
          if (setLivingGroupForSeparation(separateAreaWhite[i])
              || setLivingGroupForSeparation(separateAreaWhite[i] + 1)
              || setLivingGroupForSeparation(separateAreaWhite[i] + BOARDSIZE)
              || setLivingGroupForSeparation(separateAreaWhite[i] - BOARDSIZE)) {
            dummy2 = false;
            break;
          }
        }
        if (dummy2) {
          // ERROR if we don't find enough space to place a living group
          printError(30);
        }
      }
      for (int i = 0; i < k; i++) {
        if (area[separateAreaWhite[i]] != Area.SEPARATIONGROUPS) {
          area[separateAreaWhite[i]] = Area.SEPARATIONGROUPS;
          board[separateAreaWhite[i]] = Stone.WHITE;
        }
      }

      boolean repeat = true; // necessary if the separation group is divided into multiple parts
      int counter = 0;
      while (repeat) {
        if (counter > k) {
          printError(40);
          break;
        }
        repeat = false;
        for (int i = 0; i < k; i++) {
          if (!localLibertyCheck(separateAreaWhite[i])) {
            if (!(setLivingGroupForSeparation(separateAreaWhite[i])
                || setLivingGroupForSeparation(separateAreaWhite[i] - 1)
                || setLivingGroupForSeparation(separateAreaWhite[i] + BOARDSIZE)
                || setLivingGroupForSeparation(separateAreaWhite[i] - BOARDSIZE))) {
              repeat = true;
              counter++;
              break;
            }
          }
        }
      }

      // separate SEPARATIONGROUPS and other main area
      placeBlackStones = true;
      for (int a = 0; a < BOARDSIZE * BOARDSIZE; a++) {
        if (area[a] == Area.MAINAREAWHITE) {
          if (check4Neighborhood(a, Area.SEPARATIONGROUPS)) {
            setStone(a);
            boolean[] checkBoard = new boolean[BOARDSIZE * BOARDSIZE];
            if (!connectToAreaAndChange(a, checkBoard, Area.PROTECTEDAREA, Stone.BLACK)) {
              boolean[] checkBoard2 = new boolean[BOARDSIZE * BOARDSIZE];
              connectToAreaAndChange(a, checkBoard2, Area.MAINAREABLACK, Stone.BLACK);
            }
          }
        }
      }
      for (int a = 0; a < BOARDSIZE * BOARDSIZE; a++) {
        if (area[a] == Area.MAINAREAWHITE) {
          if (check4Neighborhood(a, Area.SEPARATIONGROUPS)) {
            setStone(a);
            boolean[] checkBoard = new boolean[BOARDSIZE * BOARDSIZE];
            if (!connectToAreaAndChange(a, checkBoard, Area.PROTECTEDAREA, Stone.BLACK)) {
              boolean[] checkBoard2 = new boolean[BOARDSIZE * BOARDSIZE];
              if (!connectToAreaAndChange(a, checkBoard2, Area.MAINAREABLACK, Stone.BLACK)) {
                if (!check4Neighborhood(a, Area.PROTECTEDAREA)) {
                  if (check4Neighborhood(a + 1, Area.PROTECTEDAREA)
                      && area[a + 1] != Area.SEPARATIONGROUPS) {
                    setProtectedStone(a + 1);
                  } else {
                    if (check4Neighborhood(a - 1, Area.PROTECTEDAREA)
                        && area[a - 1] != Area.SEPARATIONGROUPS) {
                      setProtectedStone(a - 1);
                    } else {
                      if (check4Neighborhood(a + BOARDSIZE, Area.PROTECTEDAREA)
                          && area[a + BOARDSIZE] != Area.SEPARATIONGROUPS) {
                        setProtectedStone(a + BOARDSIZE);
                      } else {
                        if (check4Neighborhood(a - BOARDSIZE, Area.PROTECTEDAREA)
                            && area[a - BOARDSIZE] != Area.SEPARATIONGROUPS) {
                          setProtectedStone(a - BOARDSIZE);
                        } else {
                          printError(35);
                        }
                      }
                    }
                  }
                }
                boolean[] checkBoard3 = new boolean[BOARDSIZE * BOARDSIZE];
                connectToAreaAndChange(a, checkBoard3, Area.PROTECTEDAREA, Stone.BLACK);
              }
            }
          }
        }
      }
    }
  }

  /**
   * builds eyes at a for the separation group
   *
   * @return true if we succeeded
   */
  private boolean setLivingGroupForSeparation(int a) {
    if (a < 0 || a >= BOARDSIZE * BOARDSIZE) {
      return false;
    }
    if (dummy2
        && ((playerInsideBlack && xCoord(a) > BOARDSIZE - 6)
            || (!playerInsideBlack && xCoord(a) < 6))) {
      return false;
    }
    placeLivingGroupesVertical = true;
    if (checkSpaceForLivingGroupAndChangeType2(a, Area.PROTECTEDAREA, Area.PROTECTEDAREA)) {
      setLivingGroup(a);
      if (!checkSpaceForLivingGroupAndChangeType(a, Area.PROTECTEDAREA, Area.SEPARATIONGROUPS)) {
        printError(24);
      }
      return true;
    }
    placeLivingGroupesVertical = false;
    if (checkSpaceForLivingGroupAndChangeType2(a, Area.PROTECTEDAREA, Area.SEPARATIONGROUPS)) {
      setLivingGroup(a);
      if (!checkSpaceForLivingGroupAndChangeType(a, Area.PROTECTEDAREA, Area.SEPARATIONGROUPS)) {
        printError(25);
      }
      return true;
    }
    return false;
  }

  /** restarts everything */
  private void restart() {
    if (Lizzie.frame.localAnalysisFrame.board.inAnalysisMode())
      Lizzie.frame.localAnalysisFrame.board.toggleAnalysis(); // stop ponder while initiation
    placeStonesAlternating = false;
    area = new Area[BOARDSIZE * BOARDSIZE];
    for (int i = 0; i < BOARDSIZE * BOARDSIZE; i++) {
      area[i] = Area.EMPTY;
    }

    copyBoard();
    protectFieldSquareAndSetWall(
        startCoordinates[0],
        startCoordinates[1]); // protects the selected area and sets corner points for the wall
    clearBoard(); // clear board except selected area

    buildEyesForWallAndAlivePoints(); // save marked stones and get wall alive

    // set main areas
    placeBlackStones = true;
    int a = 0;
    for (int i = 0; i < BOARDSIZE / 2 + BOARDSIZE % 2; i++) { // black
      for (int j = 0; j < BOARDSIZE; j++) {
        a = j * BOARDSIZE + i;
        if (area[a] != Area.PROTECTEDAREA) {
          area[a] = Area.MAINAREABLACK;
          board[a] = Stone.BLACK;
        }
      }
    }

    placeBlackStones = false;
    for (int i = BOARDSIZE / 2 + BOARDSIZE % 2; i < BOARDSIZE; i++) { // white
      for (int j = 0; j < BOARDSIZE; j++) {
        a = j * BOARDSIZE + i;
        if (area[a] != Area.PROTECTEDAREA) {
          area[a] = Area.MAINAREAWHITE;
          board[a] = Stone.WHITE;
        }
      }
    }
    changingPoint = BOARDSIZE * BOARDSIZE;
    separateMainAreas(); // separates main areas from rest of board
    createEyeSpace(); // searches eyespace for mainareas
    createEyes(true); // builds eyes for mainareas
    createEyes2(); // builds eyes for parts of main area which arn't connected to the main part
    // for example if the main area gets divided in two parts by the Separation group
    if (!libertyCheck()) { // check if we have a legal board position
      printError(43);
    }
    buildRealBoard(); // place the stones so we can see it

    Lizzie.frame.localAnalysisFrame.board.toggleAnalysis(); // start ponder
  }

  private void refreshBoard() {
    if (Lizzie.frame.localAnalysisFrame.board.inAnalysisMode())
      Lizzie.frame.localAnalysisFrame.board.toggleAnalysis(); // stop ponder while initiation
    returnToGame();
    buildRealBoard();
    Lizzie.frame.localAnalysisFrame.board.toggleAnalysis();
  }

  // TODO wer soll am ende dran sein?
  /** goes to the start of the game and places the stones so we can see them */
  private void buildRealBoard() {
    if (mirror != 0) { // we have to mirror back
      if (mirror > 3) {
        mirror1();
      }
      if (mirror % 4 != 0) {
        for (int i = mirror % 4; i < 4; i++) {
          mirror2();
        }
      }
    }
    // find start of tree
    Lizzie.frame.localAnalysisFrame.board.goToMoveNumberBeyondBranch(0);
    // build new Branch
    int differenceNumberStones = 0;
    int indexBlack = -1;
    int indexWhite = -1;
    for (int i = 0; i < BOARDSIZE * BOARDSIZE; i++) {
      if (board[i] == Stone.BLACK) {
        differenceNumberStones++;
      }
      if (board[i] == Stone.WHITE) {
        differenceNumberStones--;
      }
    }
    if (differenceNumberStones < 0) {
      if (blackToMove) {
        differenceNumberStones++;
      }
    }
    if (differenceNumberStones > 0) {
      if (!blackToMove) {
        differenceNumberStones--;
      }
    }

    if (differenceNumberStones < 0) {
      Lizzie.frame.localAnalysisFrame.board.pass(Stone.BLACK, true);
      while (differenceNumberStones != 0) {
        indexWhite = buildRealBoardHelper(indexWhite + 1, false);
        Lizzie.frame.localAnalysisFrame.board.pass(Stone.BLACK);
        differenceNumberStones++;
      }
      indexWhite = buildRealBoardHelper(indexWhite + 1, false);
    }
    if (differenceNumberStones > 0) {
      indexBlack = buildRealBoardHelper(indexBlack + 1, true);
      while (differenceNumberStones != 0) {
        Lizzie.frame.localAnalysisFrame.board.pass(Stone.WHITE);
        indexBlack = buildRealBoardHelper(indexBlack + 1, true);
        differenceNumberStones--;
      }
      indexWhite = buildRealBoardHelper(indexWhite + 1, false);
    }
    while (indexBlack < BOARDSIZE * BOARDSIZE && indexWhite < BOARDSIZE * BOARDSIZE) {
      indexBlack = buildRealBoardHelper(indexBlack + 1, true);
      indexWhite = buildRealBoardHelper(indexWhite + 1, false);
    }
  }

  /**
   * searches the next point where we have to place a stone
   *
   * @param index where we start our search
   * @param BlackToPlaceStones true if we want to place Black stones. false for White stones
   * @return point where we placed a stone
   */
  private int buildRealBoardHelper(int index, boolean BlackToPlaceStones) {
    if (BlackToPlaceStones) {
      for (; index < BOARDSIZE * BOARDSIZE; index++) {
        if (board[index] == Stone.BLACK) {
          placeRealStone(xCoord(index), yCoord(index), Stone.BLACK);
          return index;
        }
      }
      return index;
    } else {
      for (; index < BOARDSIZE * BOARDSIZE; index++) {
        if (board[index] == Stone.WHITE) {
          placeRealStone(xCoord(index), yCoord(index), Stone.WHITE);
          return index;
        }
      }
      return index;
    }
  }

  /**
   * places a stone so we can see it
   *
   * @param coord where we place the stone
   * @param color color of the stone we place
   */
  private void placeRealStone(int coord, Stone color) {
    placeRealStone(xCoord(coord), yCoord(coord), color);
  }

  /**
   * places a stone so we can see it
   *
   * @param x-coord where we place the stone
   * @param y-coord where we place the stone
   * @param color color of the stone we place
   */
  private void placeRealStone(int x, int y, Stone color) {
    Lizzie.frame.localAnalysisFrame.board.place(x, BOARDSIZE - 1 - y, color);
  }

  // TODO what to do if we have a group with no liberties? no concept yet
  /** @return true if every group has liberties */
  private boolean libertyCheck() {
    boolean[] libertyField = new boolean[BOARDSIZE * BOARDSIZE];
    for (int a = 0; dummy < BOARDSIZE * BOARDSIZE && a < BOARDSIZE * BOARDSIZE; a++) {
      if (libertyField[a]) {
        continue;
      }
      if (board[a] == Stone.EMPTY) {
        helpLibertyCheck(a, libertyField, Stone.EMPTY);
      }
    }
    if (dummy == BOARDSIZE * BOARDSIZE) {
      dummy = 0; // we might want to use this method again
      return true;
    } else {
      dummy = 0;
      return false;
    }
  }

  /** @return true if a and every group next to a has liberties */
  private boolean localLibertyCheck(int a) {
    boolean[] checkBoard = new boolean[BOARDSIZE * BOARDSIZE];
    if (!helpLocalLibertyCheck(a, checkBoard, board[a])) {
      return false;
    }
    Arrays.fill(checkBoard, false);
    if (xCoord(a) < BOARDSIZE - 1) {
      if (!helpLocalLibertyCheck(a + 1, checkBoard, board[a + 1])) {
        return false;
      }
    }
    Arrays.fill(checkBoard, false);
    if (xCoord(a) > 0) {
      if (!helpLocalLibertyCheck(a - 1, checkBoard, board[a - 1])) {
        return false;
      }
    }
    Arrays.fill(checkBoard, false);
    if (yCoord(a) < BOARDSIZE - 1) {
      if (!helpLocalLibertyCheck(a + BOARDSIZE, checkBoard, board[a + BOARDSIZE])) {
        return false;
      }
    }
    Arrays.fill(checkBoard, false);
    if (yCoord(a) > 0) {
      if (!helpLocalLibertyCheck(a - BOARDSIZE, checkBoard, board[a - BOARDSIZE])) {
        return false;
      }
    }
    return true;
  }

  /** help function for libertyCheck() */
  private boolean helpLocalLibertyCheck(int a, boolean[] checkBoard, Stone color) {
    if (!isFieldInBoard(a)) {
      return false;
    }
    if (checkBoard[a]) {
      return false;
    }
    checkBoard[a] = true;
    if (board[a] == Stone.EMPTY) {
      return true;
    }
    if (board[a] == color) {
      return (xCoord(a) != BOARDSIZE - 1 && helpLocalLibertyCheck(a + 1, checkBoard, color))
          || (xCoord(a) != 0 && helpLocalLibertyCheck(a - 1, checkBoard, color))
          || (yCoord(a) != BOARDSIZE - 1 && helpLocalLibertyCheck(a + BOARDSIZE, checkBoard, color))
          || (yCoord(a) != 0 && helpLocalLibertyCheck(a - BOARDSIZE, checkBoard, color));
    }
    return false;
  }

  /** help function for libertyCheck() */
  private void helpLibertyCheck(int a, boolean[] libertyField, Stone color) {
    if (!isFieldInBoard(a)) {
      return;
    }
    if (libertyField[a]) {
      return;
    }
    if (color == Stone.EMPTY) {
      if (board[a] == Stone.EMPTY) {
        libertyField[a] = true;
        dummy++;
        if (xCoord(a) != BOARDSIZE - 1) helpLibertyCheck(a + 1, libertyField, Stone.EMPTY);
        if (xCoord(a) != 0) helpLibertyCheck(a - 1, libertyField, Stone.EMPTY);
        if (yCoord(a) != BOARDSIZE - 1) helpLibertyCheck(a + BOARDSIZE, libertyField, Stone.EMPTY);
        if (yCoord(a) != 0) helpLibertyCheck(a - BOARDSIZE, libertyField, Stone.EMPTY);
        return;
      } else {
        libertyField[a] = true;
        dummy++;
        if (xCoord(a) != BOARDSIZE - 1) helpLibertyCheck(a + 1, libertyField, board[a]);
        if (xCoord(a) != 0) helpLibertyCheck(a - 1, libertyField, board[a]);
        if (yCoord(a) != BOARDSIZE - 1) helpLibertyCheck(a + BOARDSIZE, libertyField, board[a]);
        if (yCoord(a) != 0) helpLibertyCheck(a - BOARDSIZE, libertyField, board[a]);
        return;
      }
    } else {
      if (board[a] == color) {
        libertyField[a] = true;
        dummy++;
        if (xCoord(a) != BOARDSIZE - 1) helpLibertyCheck(a + 1, libertyField, color);
        if (xCoord(a) != 0) helpLibertyCheck(a - 1, libertyField, color);
        if (yCoord(a) != BOARDSIZE - 1) helpLibertyCheck(a + BOARDSIZE, libertyField, color);
        if (yCoord(a) != 0) helpLibertyCheck(a - BOARDSIZE, libertyField, color);
        return;
      } else {
        return;
      }
    }
  }

  /**
   * searches for a field from type areatype. starts with corners than tries every field.
   *
   * @param areatype we want to search
   * @return first coordinate or BOARDSIZE*BOARDSIZE if we don't find any
   */
  private int findAreaType(Area areatype) {
    // try corners
    if (area[0] == areatype) {
      return 0;
    }
    if (area[BOARDSIZE - 1] == areatype) {
      return BOARDSIZE - 1;
    }
    if (area[BOARDSIZE * BOARDSIZE - BOARDSIZE] == areatype) {
      return BOARDSIZE * BOARDSIZE - BOARDSIZE;
    }
    if (area[BOARDSIZE * BOARDSIZE - 1] == areatype) {
      return BOARDSIZE * BOARDSIZE - 1;
    }
    // try boarders
    for (int a = 0; a < BOARDSIZE * BOARDSIZE; a++) {
      if (isBorder(a)) {
        if (area[a] == areatype) {
          return a;
        }
      }
    }
    // try every field
    for (int a = 0; a < BOARDSIZE * BOARDSIZE; a++) {
      if (area[a] == areatype) {
        return a;
      }
    }
    return BOARDSIZE * BOARDSIZE;
  }

  /** changes all stones of the area type of a around a to changetype and stone color to color */
  private void changeArea(int a, Area changetype, Stone color) {
    boolean[] checkBoard = new boolean[BOARDSIZE * BOARDSIZE];
    Area areatype = area[a];
    changeAreaHelper(a, checkBoard, areatype, changetype, color);
  }

  private void changeAreaHelper(
      int a, boolean[] checkBoard, Area areatype, Area changetype, Stone color) {
    if (!isFieldInBoard(a)) {
      return;
    }
    if (checkBoard[a]) {
      return;
    }
    checkBoard[a] = true;
    if (area[a] == areatype) {
      area[a] = changetype;
      board[a] = color;
      changeAreaHelper(a + 1, checkBoard, areatype, changetype, color);
      changeAreaHelper(a - 1, checkBoard, areatype, changetype, color);
      changeAreaHelper(a + BOARDSIZE, checkBoard, areatype, changetype, color);
      changeAreaHelper(a - BOARDSIZE, checkBoard, areatype, changetype, color);
      return;
    }
  }

  /**
   * checks if the stone is connected to another stone of type areatype. If so, all the stones on
   * the way will be changed to areatype and it will return true
   *
   * @param checkBoard: all the fields we already have gone through
   */
  private boolean connectToAreaAndChange(int a, boolean[] checkBoard, Area areatype, Stone color) {
    if (!isFieldInBoard(a)) {
      return false;
    }
    if (checkBoard[a]) {
      return false;
    }
    checkBoard[a] = true;
    if (board[a] != color) {
      return false;
    }
    if (area[a] == areatype) {
      return true;
    }
    if ((xCoord(a) < BOARDSIZE - 1 && connectToAreaAndChange(a + 1, checkBoard, areatype, color))
        || (xCoord(a) > 0 && connectToAreaAndChange(a - 1, checkBoard, areatype, color))
        || (yCoord(a) < BOARDSIZE - 1
            && connectToAreaAndChange(a + BOARDSIZE, checkBoard, areatype, color))
        || (yCoord(a) > 0 && connectToAreaAndChange(a - BOARDSIZE, checkBoard, areatype, color))) {
      area[a] = areatype;
      return true;
    } else {
      return false;
    }
  }

  /**
   * checks if the stone is connected to another stone of type areatype. If so, it will return true
   * and mark all connected fields which we checked
   *
   * @param checkBoard: all the fields we already have gone through
   * @param returnBoard: fields which are connected to a Stone of type areatpye
   */
  private boolean connectToArea(
      int a, boolean[] checkBoard, boolean[] returnBoard, Area areatype, Stone color) {
    if (!isFieldInBoard(a)) {
      return false;
    }
    if (checkBoard[a]) {
      return false;
    }
    checkBoard[a] = true;
    if (board[a] != color) {
      return false;
    }
    if (area[a] == areatype) {
      return true;
    }
    if ((xCoord(a) < BOARDSIZE - 1
            && connectToArea(a + 1, checkBoard, returnBoard, areatype, color))
        || (xCoord(a) > 0 && connectToArea(a - 1, checkBoard, returnBoard, areatype, color))
        || (yCoord(a) < BOARDSIZE - 1
            && connectToArea(a + BOARDSIZE, checkBoard, returnBoard, areatype, color))
        || (yCoord(a) > 0
            && connectToArea(a - BOARDSIZE, checkBoard, returnBoard, areatype, color))) {
      returnBoard[a] = true;
      return true;
    } else {
      return false;
    }
  }

  /**
   * checks if the stone is connected to another stone of type areatype. If so it will return true.
   * ELSE it will mark all connected fields which we checked
   *
   * @param checkBoard: all the fields we already have gone through
   * @param returnBoard: fields which are connected to a Stone of type areatpye
   */
  private boolean connectToArea2(
      int a, boolean[] checkBoard, boolean[] returnBoard, Area areatype, Stone color) {
    if (!isFieldInBoard(a)) {
      return false;
    }
    if (checkBoard[a]) {
      return false;
    }
    checkBoard[a] = true;
    if (board[a] != color) {
      return false;
    }
    if (area[a] == areatype) {
      return true;
    }
    if ((xCoord(a) < BOARDSIZE - 1
            && connectToArea(a + 1, checkBoard, returnBoard, areatype, color))
        || (xCoord(a) > 0 && connectToArea(a - 1, checkBoard, returnBoard, areatype, color))
        || (yCoord(a) < BOARDSIZE - 1
            && connectToArea(a + BOARDSIZE, checkBoard, returnBoard, areatype, color))
        || (yCoord(a) > 0
            && connectToArea(a - BOARDSIZE, checkBoard, returnBoard, areatype, color))) {
      return true;
    } else {
      returnBoard[a] = true;
      return false;
    }
  }

  /** @return true if (x,y) is a valid field on the board */
  private boolean isFieldInBoard(int x, int y) {
    return x >= 0 && x < BOARDSIZE && y >= 0 && y < BOARDSIZE;
  }

  /** @return true if a is a valid field on the board */
  private boolean isFieldInBoard(int a) {
    return a >= 0 && a < BOARDSIZE * BOARDSIZE;
  }

  private boolean isCorner(int a) {
    if (a == 0
        || a == BOARDSIZE - 1
        || a == BOARDSIZE * BOARDSIZE - BOARDSIZE
        || a == BOARDSIZE * BOARDSIZE - 1) {
      return true;
    }
    return false;
  }

  private boolean isBorder(int a) {
    if (a % BOARDSIZE == 0
        || a % BOARDSIZE == BOARDSIZE - 1
        || a < BOARDSIZE
        || a > BOARDSIZE * BOARDSIZE - BOARDSIZE) {
      return true;
    }
    return false;
  }

  /** @return x-coordinate of a field */
  private int xCoord(int a) {
    return a % BOARDSIZE;
  }

  /** @return y-coordinate of a field */
  private int yCoord(int a) {
    return (a / BOARDSIZE) % BOARDSIZE;
  }

  /** @return field with x-coordinate x and y-coordinate y */
  private int getField(int x, int y) {
    return y * BOARDSIZE + x;
  }

  /** translates Lizzie coordinates to our coordinates */
  private int translateCoordinates(int[] a) {
    if (a == null || a.length != 2) {
      printError(44);
      return -1;
    } else {
      return a[0] + (BOARDSIZE - 1 - a[1]) * BOARDSIZE;
    }
  }

  /** takes a field and tests if one of the 4 surrounding stones is from type areatype */
  private boolean check4Neighborhood(int a, Area areatype) {
    if (a < 0 || a >= BOARDSIZE * BOARDSIZE) {
      return false;
    }
    boolean[] Neighborhood = new boolean[4];
    if (a >= BOARDSIZE) {
      Neighborhood[0] = true;
      if (a < BOARDSIZE * BOARDSIZE - BOARDSIZE) {
        Neighborhood[3] = true;
      }
    } else {
      Neighborhood[3] = true;
    }
    if (a % BOARDSIZE != 0) {
      Neighborhood[1] = true;
      if (a % BOARDSIZE != BOARDSIZE - 1) {
        Neighborhood[2] = true;
      }
    } else {
      Neighborhood[2] = true;
    }

    if ((Neighborhood[2] && area[a + 1] == areatype)
        || (Neighborhood[1] && area[a - 1] == areatype)
        || (Neighborhood[3] && area[a + BOARDSIZE] == areatype)
        || (Neighborhood[0] && area[a - BOARDSIZE] == areatype)) {
      return true;
    }
    return false;
  }

  /** takes a field and tests if one of the 8 surrounding stones is from type areatype */
  private boolean check8Neighborhood(int a, Area areatype) {
    if (a < 0 || a >= BOARDSIZE * BOARDSIZE) {
      return false;
    }
    boolean[] Neighborhood = new boolean[8];
    if (a > BOARDSIZE) {
      Neighborhood[1] = true;
    }
    if (a < BOARDSIZE * BOARDSIZE - BOARDSIZE) {
      Neighborhood[6] = true;
    }
    if (xCoord(a) > 0) {
      Neighborhood[3] = true;
    }
    if (xCoord(a) < BOARDSIZE - 1) {
      Neighborhood[4] = true;
    }
    if (a > BOARDSIZE && xCoord(a) > 0) {
      Neighborhood[0] = true;
    }
    if (a > BOARDSIZE && xCoord(a) < BOARDSIZE - 1) {
      Neighborhood[2] = true;
    }
    if (a < BOARDSIZE * BOARDSIZE - BOARDSIZE && xCoord(a) > 0) {
      Neighborhood[5] = true;
    }
    if (a < BOARDSIZE * BOARDSIZE - BOARDSIZE && xCoord(a) < BOARDSIZE - 1) {
      Neighborhood[7] = true;
    }

    if ((Neighborhood[4] && area[a + 1] == Area.PROTECTEDAREA)
        || (Neighborhood[3] && area[a - 1] == Area.PROTECTEDAREA)
        || (Neighborhood[5] && area[a + BOARDSIZE - 1] == Area.PROTECTEDAREA)
        || (Neighborhood[6] && area[a + BOARDSIZE] == Area.PROTECTEDAREA)
        || (Neighborhood[7] && area[a + BOARDSIZE + 1] == Area.PROTECTEDAREA)
        || (Neighborhood[0] && area[a - BOARDSIZE - 1] == Area.PROTECTEDAREA)
        || (Neighborhood[1] && area[a - BOARDSIZE] == Area.PROTECTEDAREA)
        || (Neighborhood[2] && area[a - BOARDSIZE + 1] == Area.PROTECTEDAREA)) {
      return true;
    }
    return false;
  }

  /** checks if all fields which we need for setLivingGroup are from type areatype */
  private boolean checkSpaceForLivingGroup(int a, Area areatype) {
    return checkSpaceForLivingGroupAndChangeType(a, areatype, areatype);
  }

  /**
   * checks if all fields which we need for setLivingGroup are from type areatype and change if true
   * every field to changetype
   */
  private boolean checkSpaceForLivingGroupAndChangeType(int a, Area areatype, Area changetype) {
    if (a < 0 || a >= BOARDSIZE * BOARDSIZE) {
      return false;
    }
    // if a is located in a corner
    if (a == 0) {
      if (area[0] == areatype
          && area[1] == areatype
          && area[2] == areatype
          && area[BOARDSIZE] == areatype
          && area[2 + BOARDSIZE] == areatype
          && area[1 + BOARDSIZE] == areatype
          && area[1 + 2 * BOARDSIZE] == areatype
          && area[2 * BOARDSIZE] == areatype) {
        area[0] = changetype;
        area[1] = changetype;
        area[2] = changetype;
        area[2 + BOARDSIZE] = changetype;
        area[BOARDSIZE] = changetype;
        area[1 + BOARDSIZE] = changetype;
        area[1 + 2 * BOARDSIZE] = changetype;
        area[2 * BOARDSIZE] = changetype;
        return true;
      } else {
        return false;
      }
    }
    if (a == BOARDSIZE - 1) {
      if (area[BOARDSIZE - 1] == areatype
          && area[BOARDSIZE - 2] == areatype
          && area[BOARDSIZE - 3] == areatype
          && area[2 * BOARDSIZE - 3] == areatype
          && area[2 * BOARDSIZE - 2] == areatype
          && area[2 * BOARDSIZE - 1] == areatype
          && area[3 * BOARDSIZE - 2] == areatype
          && area[3 * BOARDSIZE - 1] == areatype) {
        area[BOARDSIZE - 1] = changetype;
        area[BOARDSIZE - 2] = changetype;
        area[BOARDSIZE - 3] = changetype;
        area[2 * BOARDSIZE - 3] = changetype;
        area[2 * BOARDSIZE - 2] = changetype;
        area[2 * BOARDSIZE - 1] = changetype;
        area[3 * BOARDSIZE - 2] = changetype;
        area[3 * BOARDSIZE - 1] = changetype;
        return true;
      } else {
        return false;
      }
    }
    if (a == BOARDSIZE * BOARDSIZE - BOARDSIZE) {
      if (area[BOARDSIZE * BOARDSIZE - BOARDSIZE] == areatype
          && area[BOARDSIZE * BOARDSIZE - BOARDSIZE + 1] == areatype
          && area[BOARDSIZE * BOARDSIZE - BOARDSIZE + 2] == areatype
          && area[BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE + 2] == areatype
          && area[BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE + 1] == areatype
          && area[BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE] == areatype
          && area[BOARDSIZE * BOARDSIZE - 3 * BOARDSIZE + 1] == areatype
          && area[BOARDSIZE * BOARDSIZE - 3 * BOARDSIZE] == areatype) {
        area[BOARDSIZE * BOARDSIZE - BOARDSIZE] = changetype;
        area[BOARDSIZE * BOARDSIZE - BOARDSIZE + 1] = changetype;
        area[BOARDSIZE * BOARDSIZE - BOARDSIZE + 2] = changetype;
        area[BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE] = changetype;
        area[BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE + 1] = changetype;
        area[BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE + 2] = changetype;
        area[BOARDSIZE * BOARDSIZE - 3 * BOARDSIZE + 1] = changetype;
        area[BOARDSIZE * BOARDSIZE - 3 * BOARDSIZE] = changetype;
        return true;
      } else {
        return false;
      }
    }
    if (a == BOARDSIZE * BOARDSIZE - 1) {
      if (area[BOARDSIZE * BOARDSIZE - 1] == areatype
          && area[BOARDSIZE * BOARDSIZE - 2] == areatype
          && area[BOARDSIZE * BOARDSIZE - 3] == areatype
          && area[BOARDSIZE * BOARDSIZE - BOARDSIZE - 3] == areatype
          && area[BOARDSIZE * BOARDSIZE - BOARDSIZE - 2] == areatype
          && area[BOARDSIZE * BOARDSIZE - BOARDSIZE - 1] == areatype
          && area[BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE - 2] == areatype
          && area[BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE - 1] == areatype) {
        area[BOARDSIZE * BOARDSIZE - 1] = changetype;
        area[BOARDSIZE * BOARDSIZE - 2] = changetype;
        area[BOARDSIZE * BOARDSIZE - 3] = changetype;
        area[BOARDSIZE * BOARDSIZE - BOARDSIZE - 3] = changetype;
        area[BOARDSIZE * BOARDSIZE - BOARDSIZE - 2] = changetype;
        area[BOARDSIZE * BOARDSIZE - BOARDSIZE - 1] = changetype;
        area[BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE - 2] = changetype;
        area[BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE - 1] = changetype;
        return true;
      } else {
        return false;
      }
    }
    // if a is located at the edge
    if (a < BOARDSIZE) {
      for (int i = xCoord(a) - 2; i <= xCoord(a) + 2; i++) {
        for (int j = yCoord(a); j <= yCoord(a) + 1; j++) {
          if (i >= 0 && i < BOARDSIZE) {
            if (area[getField(i, j)] != areatype) {
              return false;
            }
          }
        }
      }
      for (int i = xCoord(a) - 2; i <= xCoord(a) + 2; i++) {
        for (int j = yCoord(a); j <= yCoord(a) + 1; j++) {
          if (i >= 0 && i < BOARDSIZE) {
            area[getField(i, j)] = changetype;
          }
        }
      }
      return true;
    }
    if (a % BOARDSIZE == 0) {
      for (int i = xCoord(a); i <= xCoord(a) + 1; i++) {
        for (int j = yCoord(a) - 2; j <= yCoord(a) + 2; j++) {
          if (j >= 0 && j < BOARDSIZE) {
            if (area[getField(i, j)] != areatype) {
              return false;
            }
          }
        }
      }
      for (int i = xCoord(a); i <= xCoord(a) + 1; i++) {
        for (int j = yCoord(a) - 2; j <= yCoord(a) + 2; j++) {
          if (j >= 0 && j < BOARDSIZE) {
            area[getField(i, j)] = changetype;
          }
        }
      }
      return true;
    }
    if (a % BOARDSIZE == BOARDSIZE - 1) {
      for (int i = xCoord(a) - 1; i <= xCoord(a); i++) {
        for (int j = yCoord(a) - 2; j <= yCoord(a) + 2; j++) {
          if (j >= 0 && j < BOARDSIZE) {
            if (area[getField(i, j)] != areatype) {
              return false;
            }
          }
        }
      }
      for (int i = xCoord(a) - 1; i <= xCoord(a); i++) {
        for (int j = yCoord(a) - 2; j <= yCoord(a) + 2; j++) {
          if (j >= 0 && j < BOARDSIZE) {
            area[getField(i, j)] = changetype;
          }
        }
      }
      return true;
    }
    if (a >= BOARDSIZE * BOARDSIZE - BOARDSIZE) {
      for (int i = xCoord(a) - 2; i <= xCoord(a) + 2; i++) {
        for (int j = yCoord(a) - 1; j <= yCoord(a); j++) {
          if (i >= 0 && i < BOARDSIZE) {
            if (area[getField(i, j)] != areatype) {
              return false;
            }
          }
        }
      }
      for (int i = xCoord(a) - 2; i <= xCoord(a) + 2; i++) {
        for (int j = yCoord(a) - 1; j <= yCoord(a); j++) {
          if (i >= 0 && i < BOARDSIZE) {
            area[getField(i, j)] = changetype;
          }
        }
      }
      return true;
    }
    // if a doesn't lie at the edge or in a corner
    if (!placeLivingGroupesVertical) {
      for (int i = xCoord(a) - 1; i <= xCoord(a) + 1; i++) {
        for (int j = yCoord(a) - 2; j <= yCoord(a) + 2; j++) {
          if (j >= 0 && j < BOARDSIZE) {
            if (area[getField(i, j)] != areatype) {
              return false;
            }
          }
        }
      }
      for (int i = xCoord(a) - 1; i <= xCoord(a) + 1; i++) {
        for (int j = yCoord(a) - 2; j <= yCoord(a) + 2; j++) {
          if (j >= 0 && j < BOARDSIZE) {
            area[getField(i, j)] = changetype;
          }
        }
      }
      return true;
    } else {
      for (int i = xCoord(a) - 2; i <= xCoord(a) + 2; i++) {
        for (int j = yCoord(a) - 1; j <= yCoord(a) + 1; j++) {
          if (i >= 0 && i < BOARDSIZE) {
            if (area[getField(i, j)] != areatype) {
              return false;
            }
          }
        }
      }
      for (int i = xCoord(a) - 2; i <= xCoord(a) + 2; i++) {
        for (int j = yCoord(a) - 1; j <= yCoord(a) + 1; j++) {
          if (i >= 0 && i < BOARDSIZE) {
            area[getField(i, j)] = changetype;
          }
        }
      }
      return true;
    }
  }

  /**
   * checks if all fields which we need for setLivingGroup are NOT from type areatype and change if
   * true every field to changetype
   */
  private boolean checkSpaceForLivingGroupAndChangeType2(int a, Area areatype, Area changetype) {
    if (a < 0 || a >= BOARDSIZE * BOARDSIZE) {
      return false;
    }
    // if a is located in a corner
    if (a == 0) {
      if (area[0] != areatype
          && area[1] != areatype
          && area[2] != areatype
          && area[BOARDSIZE] != areatype
          && area[2 + BOARDSIZE] != areatype
          && area[1 + BOARDSIZE] != areatype
          && area[1 + 2 * BOARDSIZE] != areatype
          && area[2 * BOARDSIZE] != areatype) {
        area[0] = changetype;
        area[1] = changetype;
        area[2] = changetype;
        area[2 + BOARDSIZE] = changetype;
        area[BOARDSIZE] = changetype;
        area[1 + BOARDSIZE] = changetype;
        area[1 + 2 * BOARDSIZE] = changetype;
        area[2 * BOARDSIZE] = changetype;
        return true;
      } else {
        return false;
      }
    }
    if (a == BOARDSIZE - 1) {
      if (area[BOARDSIZE - 1] != areatype
          && area[BOARDSIZE - 2] != areatype
          && area[BOARDSIZE - 3] != areatype
          && area[2 * BOARDSIZE - 3] != areatype
          && area[2 * BOARDSIZE - 2] != areatype
          && area[2 * BOARDSIZE - 1] != areatype
          && area[3 * BOARDSIZE - 2] != areatype
          && area[3 * BOARDSIZE - 1] != areatype) {
        area[BOARDSIZE - 1] = changetype;
        area[BOARDSIZE - 2] = changetype;
        area[BOARDSIZE - 3] = changetype;
        area[2 * BOARDSIZE - 3] = changetype;
        area[2 * BOARDSIZE - 2] = changetype;
        area[2 * BOARDSIZE - 1] = changetype;
        area[3 * BOARDSIZE - 2] = changetype;
        area[3 * BOARDSIZE - 1] = changetype;
        return true;
      } else {
        return false;
      }
    }
    if (a == BOARDSIZE * BOARDSIZE - BOARDSIZE) {
      if (area[BOARDSIZE * BOARDSIZE - BOARDSIZE] != areatype
          && area[BOARDSIZE * BOARDSIZE - BOARDSIZE + 1] != areatype
          && area[BOARDSIZE * BOARDSIZE - BOARDSIZE + 2] != areatype
          && area[BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE + 2] != areatype
          && area[BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE + 1] != areatype
          && area[BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE] != areatype
          && area[BOARDSIZE * BOARDSIZE - 3 * BOARDSIZE + 1] != areatype
          && area[BOARDSIZE * BOARDSIZE - 3 * BOARDSIZE] != areatype) {
        area[BOARDSIZE * BOARDSIZE - BOARDSIZE] = changetype;
        area[BOARDSIZE * BOARDSIZE - BOARDSIZE + 1] = changetype;
        area[BOARDSIZE * BOARDSIZE - BOARDSIZE + 2] = changetype;
        area[BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE] = changetype;
        area[BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE + 1] = changetype;
        area[BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE + 2] = changetype;
        area[BOARDSIZE * BOARDSIZE - 3 * BOARDSIZE + 1] = changetype;
        area[BOARDSIZE * BOARDSIZE - 3 * BOARDSIZE] = changetype;
        return true;
      } else {
        return false;
      }
    }
    if (a == BOARDSIZE * BOARDSIZE - 1) {
      if (area[BOARDSIZE * BOARDSIZE - 1] != areatype
          && area[BOARDSIZE * BOARDSIZE - 2] != areatype
          && area[BOARDSIZE * BOARDSIZE - 3] != areatype
          && area[BOARDSIZE * BOARDSIZE - BOARDSIZE - 3] != areatype
          && area[BOARDSIZE * BOARDSIZE - BOARDSIZE - 2] != areatype
          && area[BOARDSIZE * BOARDSIZE - BOARDSIZE - 1] != areatype
          && area[BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE - 2] != areatype
          && area[BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE - 1] != areatype) {
        area[BOARDSIZE * BOARDSIZE - 1] = changetype;
        area[BOARDSIZE * BOARDSIZE - 2] = changetype;
        area[BOARDSIZE * BOARDSIZE - 3] = changetype;
        area[BOARDSIZE * BOARDSIZE - BOARDSIZE - 3] = changetype;
        area[BOARDSIZE * BOARDSIZE - BOARDSIZE - 2] = changetype;
        area[BOARDSIZE * BOARDSIZE - BOARDSIZE - 1] = changetype;
        area[BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE - 2] = changetype;
        area[BOARDSIZE * BOARDSIZE - 2 * BOARDSIZE - 1] = changetype;
        return true;
      } else {
        return false;
      }
    }
    // if a is located at the edge
    if (a < BOARDSIZE) {
      for (int i = xCoord(a) - 2; i <= xCoord(a) + 2; i++) {
        for (int j = yCoord(a); j <= yCoord(a) + 1; j++) {
          if (i >= 0 && i < BOARDSIZE) {
            if (area[getField(i, j)] == areatype) {
              return false;
            }
          }
        }
      }
      for (int i = xCoord(a) - 2; i <= xCoord(a) + 2; i++) {
        for (int j = yCoord(a); j <= yCoord(a) + 1; j++) {
          if (i >= 0 && i < BOARDSIZE) {
            area[getField(i, j)] = changetype;
          }
        }
      }
      return true;
    }
    if (a % BOARDSIZE == 0) {
      for (int i = xCoord(a); i <= xCoord(a) + 1; i++) {
        for (int j = yCoord(a) - 2; j <= yCoord(a) + 2; j++) {
          if (j >= 0 && j < BOARDSIZE) {
            if (area[getField(i, j)] == areatype) {
              return false;
            }
          }
        }
      }
      for (int i = xCoord(a); i <= xCoord(a) + 1; i++) {
        for (int j = yCoord(a) - 2; j <= yCoord(a) + 2; j++) {
          if (j >= 0 && j < BOARDSIZE) {
            area[getField(i, j)] = changetype;
          }
        }
      }
      return true;
    }
    if (a % BOARDSIZE == BOARDSIZE - 1) {
      for (int i = xCoord(a) - 1; i <= xCoord(a); i++) {
        for (int j = yCoord(a) - 2; j <= yCoord(a) + 2; j++) {
          if (j >= 0 && j < BOARDSIZE) {
            if (area[getField(i, j)] == areatype) {
              return false;
            }
          }
        }
      }
      for (int i = xCoord(a) - 1; i <= xCoord(a); i++) {
        for (int j = yCoord(a) - 2; j <= yCoord(a) + 2; j++) {
          if (j >= 0 && j < BOARDSIZE) {
            area[getField(i, j)] = changetype;
          }
        }
      }
      return true;
    }
    if (a >= BOARDSIZE * BOARDSIZE - BOARDSIZE) {
      for (int i = xCoord(a) - 2; i <= xCoord(a) + 2; i++) {
        for (int j = yCoord(a) - 1; j <= yCoord(a); j++) {
          if (i >= 0 && i < BOARDSIZE) {
            if (area[getField(i, j)] == areatype) {
              return false;
            }
          }
        }
      }
      for (int i = xCoord(a) - 2; i <= xCoord(a) + 2; i++) {
        for (int j = yCoord(a) - 1; j <= yCoord(a); j++) {
          if (i >= 0 && i < BOARDSIZE) {
            area[getField(i, j)] = changetype;
          }
        }
      }
      return true;
    }
    // if a doesn't lie at the edge or in a corner
    if (!placeLivingGroupesVertical) {
      for (int i = xCoord(a) - 1; i <= xCoord(a) + 1; i++) {
        for (int j = yCoord(a) - 2; j <= yCoord(a) + 2; j++) {
          if (j >= 0 && j < BOARDSIZE) {
            if (area[getField(i, j)] == areatype) {
              return false;
            }
          }
        }
      }
      for (int i = xCoord(a) - 1; i <= xCoord(a) + 1; i++) {
        for (int j = yCoord(a) - 2; j <= yCoord(a) + 2; j++) {
          if (j >= 0 && j < BOARDSIZE) {
            area[getField(i, j)] = changetype;
          }
        }
      }
      return true;
    } else {
      for (int i = xCoord(a) - 2; i <= xCoord(a) + 2; i++) {
        for (int j = yCoord(a) - 1; j <= yCoord(a) + 1; j++) {
          if (i >= 0 && i < BOARDSIZE) {
            if (area[getField(i, j)] == areatype) {
              return false;
            }
          }
        }
      }
      for (int i = xCoord(a) - 2; i <= xCoord(a) + 2; i++) {
        for (int j = yCoord(a) - 1; j <= yCoord(a) + 1; j++) {
          if (i >= 0 && i < BOARDSIZE) {
            area[getField(i, j)] = changetype;
          }
        }
      }
      return true;
    }
  }

  /**
   * checks if the XxY-square with starting point a is from type checktype
   *
   * @param a startpoint
   * @param x width
   * @param y height
   * @param checktype Area-type which all fields should have
   * @return true if all fields are from type checktype
   */
  private boolean checkSquare(int a, int x, int y, Area checktype) {
    return checkSquareAndChange(a, x, y, checktype, checktype);
  }

  /**
   * checks if the XxY-square with starting point a is from type checktype
   *
   * @param a startpoint
   * @param x width
   * @param y height
   * @param checktype Area-type which all fields should have
   * @param changetype Area-type which all fields will be if we succeed
   * @return true if all fields are changed
   */
  private boolean checkSquareAndChange(int a, int x, int y, Area checktype, Area changetype) {
    int ax = xCoord(a);
    if (ax + x > BOARDSIZE) {
      return false;
    }
    int ay = yCoord(a);
    if (ay + y > BOARDSIZE) {
      return false;
    }
    for (int i = ax; i < ax + x; i++) {
      for (int j = ay; j < ay + y; j++) {
        if (area[getField(i, j)] != checktype) {
          return false;
        }
      }
    }
    if (checktype == changetype) {
      return true;
    }
    for (int i = ax; i < ax + x; i++) {
      for (int j = ay; j < ay + y; j++) {
        if (area[getField(i, j)] == checktype) {
          area[getField(i, j)] = changetype;
        }
      }
    }
    return true;
  }

  /** test functions, can be deleted if everything works */
  private void printField(int a) {
    Lizzie.frame.localAnalysisFrame.gtpConsole.addLine(
        "coordinate:" + String.valueOf(alphabet[a % BOARDSIZE]) + (a / BOARDSIZE + 1) + "\n");
  }

  private void printError(int a) {
    Lizzie.frame.localAnalysisFrame.gtpConsole.addLine("error:" + a);
  }

  private void printInt(int a) {
    Lizzie.frame.localAnalysisFrame.gtpConsole.addLine("number:" + a);
  }

  private void printBoard() {
    Lizzie.frame.localAnalysisFrame.gtpConsole.addLine("_____________");
    StringBuilder[] stringbuilder = new StringBuilder[BOARDSIZE];
    for (int i = BOARDSIZE - 1; i >= 0; i--) {
      stringbuilder[i] = new StringBuilder(20);
      stringbuilder[i].append("Z");
      for (int j = 0; j < BOARDSIZE; j++) {
        if (board[getField(j, i)] == Stone.BLACK) {
          stringbuilder[i].append("B");
        } else {
          if (board[getField(j, i)] == Stone.WHITE) {
            stringbuilder[i].append("W");
          } else {
            if (board[getField(j, i)] == Stone.EMPTY) {
              stringbuilder[i].append(" ");
            } else {
              stringbuilder[i].append("F");
            }
          }
        }
      }

      Lizzie.frame.localAnalysisFrame.gtpConsole.addLine(stringbuilder[i].toString());
    }
  }

  private void printArea() {
    Lizzie.frame.localAnalysisFrame.gtpConsole.addLine("_____________");
    StringBuilder[] stringbuilder = new StringBuilder[BOARDSIZE];
    for (int i = BOARDSIZE - 1; i >= 0; i--) {
      stringbuilder[i] = new StringBuilder(20);
      stringbuilder[i].append("Z");
      for (int j = 0; j < BOARDSIZE; j++) {
        if (area[getField(j, i)] == Area.EMPTY) {
          stringbuilder[i].append("E");
        } else {
          if (area[getField(j, i)] == Area.PROTECTEDAREA) {
            stringbuilder[i].append("P");
          } else {
            if (area[getField(j, i)] == Area.MAINAREABLACK) {
              stringbuilder[i].append("M");
            } else {
              if (area[getField(j, i)] == Area.MAINAREAWHITE) {
                stringbuilder[i].append("A");
              } else {
                if (area[getField(j, i)] == Area.EYESPACEBLACK) {
                  stringbuilder[i].append("O");
                } else {
                  if (area[getField(j, i)] == Area.MAINAREAWHITE) {
                    stringbuilder[i].append("B");
                  } else {
                    if (area[getField(j, i)] == Area.SEPARATIONGROUPS) {
                      stringbuilder[i].append("S");
                    } else {
                      stringbuilder[i].append("L");
                    }
                  }
                }
              }
            }
          }
        }
      }

      Lizzie.frame.localAnalysisFrame.gtpConsole.addLine(stringbuilder[i].toString());
    }
  }
}
