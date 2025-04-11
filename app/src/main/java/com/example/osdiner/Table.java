package com.example.osdiner;
import android.graphics.RectF;
public class Table {
    public final int id;
    public final RectF positionRect; // Position/bounds for drawing/tapping
    private boolean isOccupied = false;
    private Customer seatedCustomer = null;

    // Keep track of table IDs
    private static int nextId = 0;

    public Table(RectF rect) {
        this.id = nextId++;
        this.positionRect = rect;
    }

    public boolean isOccupied() { return isOccupied; }
    public Customer getSeatedCustomer() { return seatedCustomer; }

    public RectF getPositionRect() {
        return positionRect;
    }

    public void occupy(Customer customer) {
        this.seatedCustomer = customer;
        this.isOccupied = true;
    }
    public void vacate() {
        this.seatedCustomer = null;
        this.isOccupied = false;
    }

    public static void resetIds() {
        nextId = 0;
    }
}