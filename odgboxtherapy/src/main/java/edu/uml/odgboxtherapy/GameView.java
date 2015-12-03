package edu.uml.odgboxtherapy;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

/**
 * Created by chris on 11/25/15.
 */
public class GameView extends View {

    public enum State {
        START, PLAY, FINISH;
        private static State[] vals = values();
        public State next()
        {
            return vals[(this.ordinal()+1) % vals.length];
        }
    };

    private final Board board;
    private State state;
    private Paint paint;

    public GameView(Context context) {
        super(context);

        setupPaint();

        board = new Board();
        setupBoard();

        state = State.PLAY;
    }

    public GameView(Context context, AttributeSet attributeSet) {
        this(context);
    }

    @Override
    public void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        switch(state) {
            case START:
                drawText(canvas, "Squeeze to begin");
                break;
            case PLAY:
                drawBoard(canvas);
                break;
            case FINISH:
                drawText(canvas, "Great Job!");
                break;
        }

        //calls next draw
        //invalidate();
    }

    /**
     * Draws text to the center of the screen
     * @param canvas object to draw to
     * @param msg string to draw on screen
     */
    void drawText(Canvas canvas, String msg) {
        Rect bounds = new Rect();
        paint.getTextBounds(msg, 0, msg.length(), bounds); //gets width&height of provided text

        canvas.drawText(msg, getWidth() / 2 - bounds.width() / 2,
                getHeight() / 2 - bounds.height() / 2, paint);
    }

    void drawBoard(Canvas canvas) {
        if(board.isVisible()) {
            Ball b = board.getBall();
            Point objective = board.getCurrentObjective();

            paint.setColor(Color.RED);
            canvas.drawCircle((float) objective.x, (float) objective.y, Ball.DIAMETER / 2, paint);

            paint.setColor(Color.GREEN);
            paint.setStyle(Paint.Style.FILL);
            canvas.drawCircle((float) b.getX(), (float) b.getY(), Ball.DIAMETER / 2, paint);
        }

        paint.setColor(Color.RED);
        paint.setStyle(Paint.Style.STROKE);
        canvas.drawRect(0, 0, board.getWidth(), board.getHeight(), paint);

        board.update();
    }

    void setupPaint() {
        //setBackgroundColor(Color.BLACK);

        paint = new Paint();
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.GREEN);
        paint.setTextSize(120);
        paint.setStrokeWidth(10);
    }

    void setupBoard() {
        final int PADDING = 50;

        board.addAnObjective(PADDING + Ball.DIAMETER/2, board.getHeight()/2 - Ball.DIAMETER/2);
        board.addAnObjective(board.getWidth() - PADDING - Ball.DIAMETER/2, board.getHeight()/2 - Ball.DIAMETER/2);
        board.addAnObjective(PADDING + Ball.DIAMETER/2, board.getHeight()/2 - Ball.DIAMETER/2);
        board.addAnObjective(board.getWidth()/2, PADDING + Ball.DIAMETER/2);
        board.addAnObjective(board.getWidth()/2, board.getHeight() - PADDING - Ball.DIAMETER/2);
    }

    public Board getBoard() {
        return board;
    }
}
