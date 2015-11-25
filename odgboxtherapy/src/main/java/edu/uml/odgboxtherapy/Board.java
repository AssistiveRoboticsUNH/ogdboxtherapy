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

    public Board() {
        this(1280, 720);
    }

    public Board(int width, int height) {
        this.width = width;
        this.height = height;
        b = new Ball(this);
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
        return width;
    }
}
