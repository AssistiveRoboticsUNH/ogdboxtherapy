package edu.uml.odgboxtherapy;

import android.graphics.Point;

import java.util.ArrayList;

/**
 * Created by chris on 11/25/15.
 */
public class Board {

    private ArrayList<Point> objectives;
    private final Ball b;
    private int currentObjective;
    private final int width, height;
    private int animationTimer;

    public Board() {
        this(1280, 720);
    }

    public Board(int width, int height) {
        this.width = width;
        this.height = height;
        b = new Ball(this);
        objectives = new ArrayList<Point>();
    }

    public boolean addAnObjective(int x, int y) {
        if(x >= 0 && x <= width && y >= 0 && y <= height) {
            objectives.add(new Point(x, y));
            return true;
        }

        return false;
    }

    public Ball getBall() {
        return b;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void update() {
        b.updatePos();

        if(animationTimer > 0) {
            if(--animationTimer <= 0) {
                if (++currentObjective >= objectives.size()) {
                    currentObjective = 0;
                    //change gameView
                }

                b.unlock();
            }
        } else if( b.isTouching(getCurrentObjective()) ) {
            animationTimer = 19;
            b.lock();
        }
    }

    public boolean isVisible() {
        return animationTimer <= 0 || animationTimer / 5 % 2 != 0;
    }

    public Point getCurrentObjective() {
        return objectives.get(currentObjective);
    }
}
