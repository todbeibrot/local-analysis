package la;

import static java.awt.event.KeyEvent.*;

import featurecat.lizzie.Lizzie;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

public class LocalAnalysisInput
    implements MouseListener, KeyListener, MouseWheelListener, MouseMotionListener {
  @Override
  public void mouseClicked(MouseEvent e) {}

  @Override
  public void mousePressed(MouseEvent e) {
    if (Lizzie.frame.localAnalysisFrame.subBoardOnClick(e)) return;
    if (e.getButton() == MouseEvent.BUTTON1) { // left click
      if (e.getClickCount() == 2) { // TODO: Maybe need to delay check
        Lizzie.frame.localAnalysisFrame.onDoubleClicked(e.getX(), e.getY());
      } else {
        Lizzie.frame.localAnalysisFrame.onClicked(e.getX(), e.getY());
      }
    } else if (e.getButton() == MouseEvent.BUTTON3) // right click
    {
      if (!Lizzie.frame.localAnalysisFrame.openRightClickMenu(e.getX(), e.getY())) undo(1);
    }
  }

  @Override
  public void mouseReleased(MouseEvent e) {}

  @Override
  public void mouseEntered(MouseEvent e) {}

  @Override
  public void mouseExited(MouseEvent e) {}

  @Override
  public void mouseDragged(MouseEvent e) {
    Lizzie.frame.localAnalysisFrame.onMouseDragged(e.getX(), e.getY());
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    if (!Lizzie.frame.localAnalysisFrame.isShowingRightMenu)
      Lizzie.frame.localAnalysisFrame.onMouseMoved(e.getX(), e.getY());
  }

  @Override
  public void keyTyped(KeyEvent e) {}

  public static void undo() {
    undo(1);
  }

  public static void undo(int movesToAdvance) {
    if (Lizzie.frame.localAnalysisFrame.board.inAnalysisMode())
      Lizzie.frame.localAnalysisFrame.board.toggleAnalysis();
    if (Lizzie.frame.localAnalysisFrame.isPlayingAgainstLeelaz) {
      Lizzie.frame.localAnalysisFrame.isPlayingAgainstLeelaz = false;
    }
    if (Lizzie.frame.localAnalysisFrame.incrementDisplayedBranchLength(-movesToAdvance)) {
      return;
    }

    for (int i = 0; i < movesToAdvance; i++) Lizzie.frame.localAnalysisFrame.board.previousMove();
  }

  private void undoToChildOfPreviousWithVariation() {
    // Undo until the position just after the junction position.
    // If we are already on such a position, we go to
    // the junction position for convenience.
    // Use cases:
    // [Delete branch] Call this function and then deleteMove.
    // [Go to junction] Call this function twice.
    if (!Lizzie.frame.localAnalysisFrame.board.undoToChildOfPreviousWithVariation())
      Lizzie.frame.localAnalysisFrame.board.previousMove();
  }

  private void undoToFirstParentWithVariations() {
    if (Lizzie.frame.localAnalysisFrame.board.undoToChildOfPreviousWithVariation()) {
      Lizzie.frame.localAnalysisFrame.board.previousMove();
    }
  }

  private void goCommentNode(boolean moveForward) {
    if (moveForward) {
      redo(
          Lizzie.frame
              .localAnalysisFrame
              .board
              .getHistory()
              .getCurrentHistoryNode()
              .goToNextNodeWithComment());
    } else {
      undo(
          Lizzie.frame
              .localAnalysisFrame
              .board
              .getHistory()
              .getCurrentHistoryNode()
              .goToPreviousNodeWithComment());
    }
  }

  private void redo() {
    redo(1);
  }

  public static void redo(int movesToAdvance) {
    if (Lizzie.frame.localAnalysisFrame.board.inAnalysisMode())
      Lizzie.frame.localAnalysisFrame.board.toggleAnalysis();
    if (Lizzie.frame.localAnalysisFrame.isPlayingAgainstLeelaz) {
      Lizzie.frame.localAnalysisFrame.isPlayingAgainstLeelaz = false;
    }
    if (Lizzie.frame.localAnalysisFrame.incrementDisplayedBranchLength(movesToAdvance)) {
      return;
    }

    for (int i = 0; i < movesToAdvance; i++) Lizzie.frame.localAnalysisFrame.board.nextMove();
  }

  private void startTemporaryBoard() {
    if (Lizzie.config.showBestMoves) {
      startRawBoard();
    } else {
      Lizzie.config.showBestMovesTemporarily = true;
    }
  }

  private void startRawBoard() {
    if (!Lizzie.config.showRawBoard) {
      Lizzie.frame.localAnalysisFrame.startRawBoard();
    }
    Lizzie.config.showRawBoard = true;
  }

  private void stopRawBoard() {
    Lizzie.frame.localAnalysisFrame.stopRawBoard();
    Lizzie.config.showRawBoard = false;
  }

  private void stopTemporaryBoard() {
    stopRawBoard();
    Lizzie.config.showBestMovesTemporarily = false;
  }

  private void toggleHints() {
    Lizzie.config.toggleShowBranch();
    Lizzie.config.showSubBoard =
        Lizzie.config.showNextMoves = Lizzie.config.showBestMoves = Lizzie.config.showBranch;
  }

  private void nextBranch() {
    if (Lizzie.frame.localAnalysisFrame.isPlayingAgainstLeelaz) {
      Lizzie.frame.localAnalysisFrame.isPlayingAgainstLeelaz = false;
    }
    Lizzie.board.nextBranch();
  }

  private void previousBranch() {
    if (Lizzie.frame.localAnalysisFrame.isPlayingAgainstLeelaz) {
      Lizzie.frame.localAnalysisFrame.isPlayingAgainstLeelaz = false;
    }
    Lizzie.frame.localAnalysisFrame.board.previousBranch();
  }

  private void moveBranchUp() {
    Lizzie.frame.localAnalysisFrame.board.moveBranchUp();
  }

  private void moveBranchDown() {
    Lizzie.frame.localAnalysisFrame.board.moveBranchDown();
  }

  private void deleteMove() {
    Lizzie.frame.localAnalysisFrame.board.deleteMove();
  }

  private void deleteBranch() {
    Lizzie.frame.localAnalysisFrame.board.deleteBranch();
  }

  private boolean controlIsPressed(KeyEvent e) {
    boolean mac = System.getProperty("os.name", "").toUpperCase().startsWith("MAC");
    return e.isControlDown() || (mac && e.isMetaDown());
  }

  private void toggleShowDynamicKomi() {
    Lizzie.config.showDynamicKomi = !Lizzie.config.showDynamicKomi;
  }

  @Override
  public void keyPressed(KeyEvent e) {
    // If any controls key is pressed, let's disable analysis mode.
    // This is probably the user attempting to exit analysis mode.
    boolean shouldDisableAnalysis = true;
    int refreshType = 1;

    switch (e.getKeyCode()) {
      case VK_E:
        Lizzie.frame.localAnalysisFrame.toggleGtpConsole();
        break;
      case VK_RIGHT:
        if (e.isShiftDown()) {
          moveBranchDown();
        } else {
          nextBranch();
        }
        break;

      case VK_LEFT:
        if (e.isShiftDown()) {
          moveBranchUp();
        } else if (controlIsPressed(e)) {
          undoToFirstParentWithVariations();
        } else {
          previousBranch();
        }
        break;

      case VK_UP:
        if (controlIsPressed(e) && e.isShiftDown()) {
          goCommentNode(false);
        } else if (e.isShiftDown()) {
          undoToChildOfPreviousWithVariation();
        } else if (controlIsPressed(e)) {
          undo(10);
        } else {
          if (Lizzie.frame.localAnalysisFrame.isMouseOver) {
            Lizzie.frame.localAnalysisFrame.doBranch(-1);
          } else {
            undo();
          }
        }
        break;

      case VK_PAGE_DOWN:
        if (controlIsPressed(e) && e.isShiftDown()) {
          Lizzie.frame.localAnalysisFrame.increaseMaxAlpha(-5);
        } else {
          redo(10);
        }
        break;

      case VK_DOWN:
        if (controlIsPressed(e) && e.isShiftDown()) {
          goCommentNode(true);
        } else if (controlIsPressed(e)) {
          redo(10);
        } else {
          if (Lizzie.frame.localAnalysisFrame.isMouseOver) {
            Lizzie.frame.localAnalysisFrame.doBranch(1);
          } else {
            redo();
          }
        }
        break;

        //      case VK_N:
        //        // stop the ponder
        //        if (Lizzie.frame.localAnalysisFrame.leelaz.isPondering())
        // Lizzie.frame.localAnalysisFrame.leelaz.togglePonder();
        //        Lizzie.frame.localAnalysisFrame.startGame();
        //        break;
      case VK_SPACE:
        if (Lizzie.frame.localAnalysisFrame.isPlayingAgainstLeelaz) {
          Lizzie.frame.localAnalysisFrame.isPlayingAgainstLeelaz = false;
          Lizzie.frame.localAnalysisFrame.leelaz.isThinking = false;
        }
        Lizzie.frame.localAnalysisFrame.leelaz.togglePonder();
        refreshType = 2;
        break;

      case VK_P:
        Lizzie.frame.localAnalysisFrame.board.pass();
        break;

      case VK_COMMA:
        if (!Lizzie.frame.localAnalysisFrame.playCurrentVariation())
          Lizzie.frame.localAnalysisFrame.playBestMove();
        break;

      case VK_M:
        if (e.isAltDown()) {
          Lizzie.frame.localAnalysisFrame.openChangeMoveDialog();
        } else {
          Lizzie.config.toggleShowMoveNumber();
        }
        break;

        //      case VK_Q:
        //        Lizzie.frame.localAnalysisFrame.openOnlineDialog();
        //        break;

      case VK_F:
        Lizzie.config.toggleShowNextMoves();
        break;

      case VK_H:
        Lizzie.config.toggleHandicapInsteadOfWinrate();
        break;

      case VK_PAGE_UP:
        if (controlIsPressed(e) && e.isShiftDown()) {
          Lizzie.frame.localAnalysisFrame.increaseMaxAlpha(5);
        } else {
          undo(10);
        }
        break;

        //      case VK_I:
        //        // stop the ponder
        //        boolean isPondering = Lizzie.frame.localAnalysisFrame.leelaz.isPondering();
        //        if (isPondering) Lizzie.frame.localAnalysisFrame.leelaz.togglePonder();
        //        Lizzie.frame.localAnalysisFrame.editGameInfo();
        //        if (isPondering) Lizzie.frame.localAnalysisFrame.leelaz.togglePonder();
        //        break;

      case VK_S:
        if (e.isAltDown()) {
          Lizzie.frame.localAnalysisFrame.saveImage();
        } else {
          // stop the ponder
          if (Lizzie.frame.localAnalysisFrame.leelaz.isPondering())
            Lizzie.frame.localAnalysisFrame.leelaz.togglePonder();
          Lizzie.frame.localAnalysisFrame.saveFile();
        }
        break;

        //      case VK_O:
        //        if (Lizzie.frame.localAnalysisFrame.leelaz.isPondering())
        // Lizzie.frame.localAnalysisFrame.leelaz.togglePonder();
        //        Lizzie.frame.localAnalysisFrame.openFile();
        //        break;

      case VK_V:
        if (controlIsPressed(e)) {
          Lizzie.frame.localAnalysisFrame.pasteSgf();
        } else {
          Lizzie.config.toggleShowBranch();
        }
        break;

        //      case VK_HOME:
        //        if (controlIsPressed(e)) {
        //          Lizzie.frame.localAnalysisFrame.board.clear();
        //        } else {
        //          while (Lizzie.frame.localAnalysisFrame.board.previousMove()) ;
        //        }
        //        break;

      case VK_END:
        while (Lizzie.frame.localAnalysisFrame.board.nextMove()) ;
        break;

      case VK_X:
        //        if (controlIsPressed(e)) {
        //          Lizzie.frame.localAnalysisFrame.openConfigDialog();
        //        } else {
        if (!Lizzie.frame.localAnalysisFrame.showControls) {
          if (Lizzie.frame.localAnalysisFrame.leelaz.isPondering()) {
            wasPonderingWhenControlsShown = true;
            Lizzie.frame.localAnalysisFrame.leelaz.togglePonder();
          } else {
            wasPonderingWhenControlsShown = false;
          }
          Lizzie.frame.localAnalysisFrame.drawControls();
        }
        Lizzie.frame.localAnalysisFrame.showControls = true;
        //        }
        break;

      case VK_W:
        if (controlIsPressed(e)) {
          Lizzie.config.toggleLargeWinrate();
          refreshType = 2;
        } else if (e.isAltDown()) {
          Lizzie.frame.localAnalysisFrame.toggleDesignMode();
        } else {
          Lizzie.config.toggleShowWinrate();
          refreshType = 2;
        }
        break;

      case VK_L:
        Lizzie.config.toggleShowLcbWinrate();
        break;

      case VK_G:
        Lizzie.config.toggleShowVariationGraph();
        refreshType = 2;
        break;

      case VK_T:
        if (controlIsPressed(e)) {
          Lizzie.config.toggleShowCommentNodeColor();
        } else {
          Lizzie.config.toggleShowComment();
          refreshType = 2;
        }
        break;

      case VK_Y:
        Lizzie.config.toggleNodeColorMode();
        break;

      case VK_C:
        if (controlIsPressed(e)) {
          Lizzie.frame.localAnalysisFrame.copySgf();
        } else {
          Lizzie.config.toggleCoordinates();
          refreshType = 2;
        }
        break;

      case VK_ENTER:
        if (!Lizzie.frame.localAnalysisFrame.leelaz.isThinking) {
          Lizzie.frame.localAnalysisFrame.leelaz.sendCommand(
              "time_settings 0 "
                  + Lizzie.config
                      .config
                      .getJSONObject("leelaz")
                      .getInt("max-game-thinking-time-seconds")
                  + " 1");
          Lizzie.frame.localAnalysisFrame.playerIsBlack =
              !Lizzie.frame.localAnalysisFrame.board.getData().blackToPlay;
          Lizzie.frame.localAnalysisFrame.isPlayingAgainstLeelaz = true;
          Lizzie.frame.localAnalysisFrame.leelaz.genmove(
              (Lizzie.frame.localAnalysisFrame.board.getData().blackToPlay ? "B" : "W"));
        }
        break;

      case VK_ESCAPE:
        Lizzie.frame.localAnalysisFrame.close();
        break;

      case VK_SHIFT:
      case VK_CAPS_LOCK:
        if (Lizzie.frame.localAnalysisFrame.localAnalysis != null) {
          Lizzie.frame.localAnalysisFrame.localAnalysis.changePlaceBlackStones();
        }
        break;

      case VK_DELETE:
      case VK_BACK_SPACE:
        if (e.isShiftDown()) {
          deleteBranch();
        } else {
          deleteMove();
        }
        break;

      case VK_Z:
        if (e.isShiftDown()) {
          toggleHints();
        } else if (e.isAltDown()) {
          Lizzie.config.toggleShowSubBoard();
        } else {
          startTemporaryBoard();
        }
        break;

      case VK_A:
        if (controlIsPressed(e)) {
          Lizzie.frame.localAnalysisFrame.board.clearAnalysis();
        } else if (e.isAltDown()) {
          Lizzie.frame.localAnalysisFrame.openAvoidMoveDialog();
        } else {
          shouldDisableAnalysis = false;
          Lizzie.frame.localAnalysisFrame.board.toggleAnalysis();
        }
        break;

      case VK_B:
        Lizzie.config.toggleShowPolicy();
        break;

      case VK_PERIOD:
        if (Lizzie.frame.localAnalysisFrame.leelaz.isKataGo) {
          if (e.isAltDown()) {
            Lizzie.frame.localAnalysisFrame.toggleEstimateByZen();
          } else if (e.isShiftDown()) {
            if (Lizzie.config.showKataGoEstimate) Lizzie.config.toggleKataGoEstimateBlend();
          } else {
            if (e.isControlDown()) {
              // ctrl-. cycles modes, but only if estimates being displayed
              if (Lizzie.config.showKataGoEstimate) Lizzie.config.cycleKataGoEstimateMode();
            } else Lizzie.config.toggleKataGoEstimate();
            Lizzie.frame.localAnalysisFrame.leelaz.ponder();
            if (!Lizzie.config.showKataGoEstimate) {
              Lizzie.frame.localAnalysisFrame.removeEstimateRect();
            }
          }
        } else Lizzie.frame.localAnalysisFrame.toggleEstimateByZen();
        // if (!Lizzie.board.getHistory().getNext().isPresent()) {
        // Lizzie.board.setScoreMode(!Lizzie.board.inScoreMode());}
        break;

      case VK_D:
        if (Lizzie.frame.localAnalysisFrame.leelaz.isKataGo) {
          if (Lizzie.config.showKataGoScoreMean && Lizzie.config.kataGoNotShowWinrate) {
            Lizzie.config.showKataGoScoreMean = false;
            Lizzie.config.kataGoNotShowWinrate = false;
            break;
          }
          if (Lizzie.config.showKataGoScoreMean && !Lizzie.config.kataGoNotShowWinrate) {
            Lizzie.config.kataGoNotShowWinrate = true;
            break;
          }
          if (Lizzie.config.showKataGoScoreMean) {
            Lizzie.config.showKataGoScoreMean = false;
            break;
          }
          if (!Lizzie.config.showKataGoScoreMean) {
            Lizzie.config.showKataGoScoreMean = true;
            Lizzie.config.kataGoNotShowWinrate = false;
          }
        } else {
          toggleShowDynamicKomi();
        }
        break;

      case VK_R:
        Lizzie.frame.localAnalysisFrame.replayBranch(e.isAltDown());
        break;

        //      case VK_J:
        //    	  Lizzie.frame.localAnalysisFrame.startLocalAnalysis();
        //    	  break;

      case VK_OPEN_BRACKET:
        if (Lizzie.frame.localAnalysisFrame.boardPositionProportion > 0) {
          Lizzie.frame.localAnalysisFrame.boardPositionProportion--;
          refreshType = 2;
        }
        break;

      case VK_CLOSE_BRACKET:
        if (Lizzie.frame.localAnalysisFrame.boardPositionProportion < 8) {
          Lizzie.frame.localAnalysisFrame.boardPositionProportion++;
          refreshType = 2;
        }
        break;

      case VK_K:
        Lizzie.config.toggleEvaluationColoring();
        break;

        // Use Ctrl+Num to switching multiple engine
      case VK_0:
      case VK_1:
      case VK_2:
      case VK_3:
      case VK_4:
      case VK_5:
      case VK_6:
      case VK_7:
      case VK_8:
      case VK_9:
        if (controlIsPressed(e)) {
          Lizzie.engineManager.switchEngine(e.getKeyCode() - VK_0);
          refreshType = 0;
        }
        break;
      default:
        shouldDisableAnalysis = false;
    }

    if (shouldDisableAnalysis
        && Lizzie.frame.localAnalysisFrame != null
        && Lizzie.frame.localAnalysisFrame.board.inAnalysisMode())
      Lizzie.frame.localAnalysisFrame.board.toggleAnalysis();

    Lizzie.frame.localAnalysisFrame.refresh(refreshType);
  }

  private boolean wasPonderingWhenControlsShown = false;

  @Override
  public void keyReleased(KeyEvent e) {
    switch (e.getKeyCode()) {
        //      case VK_SHIFT:
        //  	    if(Lizzie.frame.localAnalysisFrame.localAnalysis != null) {
        //  		  Lizzie.frame.localAnalysisFrame.localAnalysis.changePlaceBlackStones();
        //  	    }
        //  	    break;
      case VK_X:
        if (wasPonderingWhenControlsShown) Lizzie.frame.localAnalysisFrame.leelaz.togglePonder();
        Lizzie.frame.localAnalysisFrame.showControls = false;
        Lizzie.frame.localAnalysisFrame.refresh(1);
        break;

      case VK_Z:
        stopTemporaryBoard();
        Lizzie.frame.localAnalysisFrame.refresh(1);
        break;

      default:
    }
  }

  private long wheelWhen;

  @Override
  public void mouseWheelMoved(MouseWheelEvent e) {
    if (Lizzie.frame.localAnalysisFrame.processCommentMouseWheelMoved(e)) {
      return;
    }
    if (Lizzie.frame.localAnalysisFrame.processSubBoardMouseWheelMoved(e)) {
      return;
    }
    if (e.getWhen() - wheelWhen > 0) {
      wheelWhen = e.getWhen();
      if (Lizzie.frame.localAnalysisFrame.board.inAnalysisMode())
        Lizzie.frame.localAnalysisFrame.board.toggleAnalysis();
      if (e.getWheelRotation() > 0) {
        if (Lizzie.frame.localAnalysisFrame.isMouseOver) {
          Lizzie.frame.localAnalysisFrame.doBranch(1);
        } else {
          redo();
        }
      } else if (e.getWheelRotation() < 0) {
        if (Lizzie.frame.localAnalysisFrame.isMouseOver) {
          Lizzie.frame.localAnalysisFrame.doBranch(-1);
        } else {
          undo();
        }
      }
      Lizzie.frame.localAnalysisFrame.refresh();
    }
  }
}
