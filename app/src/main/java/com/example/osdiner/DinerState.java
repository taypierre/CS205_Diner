package com.example.osdiner;


import android.graphics.RectF;
import android.util.Log;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;

public class DinerState {
    private static final String TAG = "DinerState";

    private final BlockingQueue<Customer> customerArrivalQueue;
    private static final int ARRIVAL_QUEUE_CAPACITY = 10;

    private static final float PATIENCE_DECREASE_RATE = 2.0f;
    private final List<Customer> waitingCustomers;

    private final List<Table> tables = new ArrayList<>();

    private static final float COOK_DURATION_SECONDS = 8.0f;

    private int score;

    //Lives and Levels
    private static final int INITIAL_LIVES = 5;
    private static final int MAX_LIVES = 7;
    private static final int SCORE_PER_LEVEL = 500;
    private int playerLives;
    private int currentLevel;
    private int scoreForNextLevel;
    private boolean isGameOver;
    private CustomerGeneratorThread customerGeneratorRef = null;

    public DinerState() {
        // Initialize core components
        customerArrivalQueue = new ArrayBlockingQueue<>(ARRIVAL_QUEUE_CAPACITY);
        waitingCustomers = new ArrayList<>();
        score = 0;
        isGameOver = false;

        // Initialize Lives and Leveling
        playerLives = INITIAL_LIVES;
        currentLevel = 1;
        scoreForNextLevel = SCORE_PER_LEVEL; // First level up at SCORE_PER_LEVEL points
        Log.i(TAG, "Game Start - Lives: " + playerLives + ", Level: " + currentLevel + ", Next Level Score: " + scoreForNextLevel);


        Log.d(TAG, "DinerState initialized (4-arg constructor)");
    }

    public void setCustomerGenerator(CustomerGeneratorThread generator) {
        this.customerGeneratorRef = generator;
    }

    public int getScore() { return score; }
    public boolean isGameOver() { return isGameOver; }

    public int getPlayerLives() { return playerLives; }
    public int getCurrentLevel() { return currentLevel; }

    public void initializeTables(RectF[] tableRects) {
        tables.clear(); // Clear previous tables if layout changes
        Table.resetIds(); // Reset IDs before creating new ones
        if (tableRects != null) {
            for (RectF rect : tableRects) {
                tables.add(new Table(rect));
            }
            Log.d(TAG, "Initialized " + tables.size() + " tables.");
        } else {
            Log.w(TAG, "initializeTables called with null tableRects array.");
        }
    }

    public BlockingQueue<Customer> getCustomerArrivalQueue() {
        return customerArrivalQueue;
    }

    public List<Customer> getWaitingCustomers() {
        return waitingCustomers;
    }

    public List<Table> getTables() {
        return tables;
    }

    // Update game state based on time elapsed since last frame
    public int update(double deltaTime) {
        if (isGameOver) {
            return 0;
        }

        int angryLeavesThisFrame = 0;

        float dt = (float)deltaTime;

        //  Update Patience for WAITING Customers
        Iterator<Customer> waitingIterator = waitingCustomers.iterator();
        while (waitingIterator.hasNext()) {
            Customer customer = waitingIterator.next();
            if (customer.getState() == Customer.CustomerState.WAITING_QUEUE) {
                customer.decreasePatience(PATIENCE_DECREASE_RATE * dt);
                if (customer.getPatience() <= 0 && customer.getState() != Customer.CustomerState.ANGRY_LEFT) {
                    customer.leaveAngry();
                    waitingIterator.remove();

                    playerLives--;
                    angryLeavesThisFrame++;
                    Log.i(TAG, customer.getDisplayId() + " left angry from waiting. Lives remaining: " + playerLives);

                    Log.i(TAG, customer.getDisplayId() + " removed from waiting queue (patience ran out). Size: " + waitingCustomers.size());
                }
            }
        }

        //  Update SEATED Customers (Timers and Patience)
        for (Table table : tables) {
            if (table.isOccupied()) {
                Customer customer = table.getSeatedCustomer();
                if (customer != null && customer.getState() != Customer.CustomerState.ANGRY_LEFT) {

                    // Only process timers/patience if game not over, prevents state changes after game over.
                    if (!isGameOver) {

                        //  Handle State-Specific Timers
                        switch (customer.getState()) {
                            case SEATED_IDLE:
                                customer.decreaseOrderReadyTimer(dt);
                                if (customer.getTimeUntilReadyToOrder() <= 0) {
                                    customer.setState(Customer.CustomerState.WAITING_ORDER_CONFIRM);

                                }
                                break;
                            case WAITING_FOOD:
                                customer.decreaseCookingTimer(dt);
                                if (customer.isCookingFinished()) {
                                    customer.setState(Customer.CustomerState.FOOD_READY);

                                }
                                break;
                            case EATING:
                                customer.decreaseEatingTimer(dt);
                                if (customer.isFinishedEating()) {
                                    customer.setState(Customer.CustomerState.READY_TO_LEAVE);

                                }
                                break;
                        }

                        //  Decrease Patience based on current state
                        boolean shouldDecreasePatience = false;
                        switch (customer.getState()) {
                            case SEATED_IDLE:
                            case WAITING_ORDER_CONFIRM:
                            case WAITING_FOOD:
                            case FOOD_READY:
                                shouldDecreasePatience = true;
                                break;
                        }

                        if (shouldDecreasePatience) {
                            customer.decreasePatience(PATIENCE_DECREASE_RATE * dt);
                            if (customer.getPatience() <= 0) {
                                customer.leaveAngry();
                                table.vacate();

                                // Lose a life
                                playerLives--;
                                angryLeavesThisFrame++;
                                Log.i(TAG, customer.getDisplayId() + " left angry from table " + table.id + ". Lives remaining: " + playerLives);

                            }
                        }

                    }

                }
            }
        }


        if (!isGameOver && playerLives <= 0) {
            Log.i(TAG, "GAME OVER - Player lives reached 0!");
            isGameOver = true;
            // Stop customer generation
            if (customerGeneratorRef != null) {
                Log.i(TAG, "Signaling Customer Generator to stop (Game Over).");
                customerGeneratorRef.stopGenerating();
            } else {
                Log.e(TAG, "Cannot stop Customer Generator: Reference is NULL in DinerState!");
            }
        }

        return angryLeavesThisFrame;
    }

    public void processCustomerArrivals() {
        // Move all currently available customers from the background queue
        int count = customerArrivalQueue.drainTo(waitingCustomers);
        if (count > 0) {
            Log.d(TAG, "Interval Trigger: Moved " + count + " customers to waiting list. Total waiting: " + waitingCustomers.size());
        }
    }

    public boolean trySeatCustomerByDrag(Customer customerToSeat, Table targetTable) {
        if (customerToSeat == null || targetTable == null) {
            Log.w(TAG, "SEATING FAILED (Drag): Null customer or table provided.");
            return false;
        }

        // Check if the customer is actually in the waiting list
        if (!waitingCustomers.contains(customerToSeat)) {
            Log.e(TAG, "SEATING FAILED (Drag): Customer " + customerToSeat.getDisplayId() + " not found in waiting list!");
            return false;
        }

        // Check if the target table is free
        if (targetTable.isOccupied()) {
            Log.w(TAG, "SEATING FAILED (Drag): Table " + targetTable.id + " is already occupied.");
            return false;
        }

        // Remove customer from waiting list
        boolean removed = waitingCustomers.remove(customerToSeat);
        if (!removed) {
            Log.e(TAG, "SEATING FAILED (Drag): Failed to remove customer " + customerToSeat.getDisplayId() + " from waiting list after check!");
            return false;
        }
        Log.d(TAG, "Removed " + customerToSeat.getDisplayId() + " from waiting list (size=" + waitingCustomers.size() + ")");

        // Occupy the table
        targetTable.occupy(customerToSeat);

        // Set customer state
        customerToSeat.setState(Customer.CustomerState.SEATED_IDLE);

        Log.i(TAG, "SEATING SUCCESS (Drag): Seated " + customerToSeat.getDisplayId() + " at table " + targetTable.id + ". State: " + customerToSeat.getState());
        return true;
    }


    public void confirmCustomerOrder(Customer customer) {
        if (customer != null && customer.getState() == Customer.CustomerState.WAITING_ORDER_CONFIRM) {
            customer.setState(Customer.CustomerState.WAITING_FOOD);
            customer.startCookingTimer(COOK_DURATION_SECONDS);

            Log.d(TAG, "Order confirmed for " + customer.getDisplayId() + ". State set to " + customer.getState() + ". Cooking started.");
        } else {
            Log.w(TAG,"Attempted to confirm order for customer not in correct state: " + (customer != null ? customer.getDisplayId() + " state=" + customer.getState() : "null customer"));
        }
    }

    public boolean deliverFood(Customer customerWhoseFood, Table targetTable) {
        if (customerWhoseFood == null || targetTable == null) {
            Log.w(TAG, "DELIVERY FAILED: Null customer or table provided.");
            return false;
        }

        // Check if table is empty
        if (!targetTable.isOccupied()) {
            Log.w(TAG, "DELIVERY FAILED: Target table " + targetTable.id + " is not occupied (for customer " + customerWhoseFood.getDisplayId() + ").");
            return false;
        }

        // Check food matches customer
        Customer seatedCustomer = targetTable.getSeatedCustomer();
        if (seatedCustomer != customerWhoseFood) {
            Log.w(TAG, "DELIVERY FAILED: Food for " + customerWhoseFood.getDisplayId()
                    + " dropped on table " + targetTable.id + " occupied by "
                    + (seatedCustomer != null ? seatedCustomer.getDisplayId() : "null") + ".");
            return false;
        }

        // Check if customer is waiting for food
        if (seatedCustomer.getState() != Customer.CustomerState.FOOD_READY) {
            Log.w(TAG, "DELIVERY FAILED: Customer " + seatedCustomer.getDisplayId()
                    + " at table " + targetTable.id + " is in state " + seatedCustomer.getState()
                    + ", not FOOD_READY.");
            return false;
        }

        // Change customer state to EATING
        seatedCustomer.setState(Customer.CustomerState.EATING);
        Log.i(TAG, "DELIVERY SUCCESS: Food delivered to " + seatedCustomer.getDisplayId()
                + " at table " + targetTable.id + ". State set to EATING.");

        seatedCustomer.startEatingTimer();


        return true;
    }


    public void clearTableForCustomer(Customer customerToClear) {
        if (customerToClear == null) {
            Log.w(TAG, "clearTableForCustomer called with null customer.");
            return;
        }

        // Ensure customer is actually ready to leave before clearing
        if (customerToClear.getState() != Customer.CustomerState.READY_TO_LEAVE) {
            Log.w(TAG, "Attempted to clear table for customer " + customerToClear.getDisplayId()
                    + " who is in state " + customerToClear.getState() + ", not READY_TO_LEAVE.");
            return;
        }

        boolean tableFound = false;
        for (Table table : tables) {
            if (table.isOccupied() && table.getSeatedCustomer() == customerToClear) {
                Log.i(TAG, "Clearing table " + table.id + " for customer " + customerToClear.getDisplayId());

                // Score
                int pointsAwarded = customerToClear.getScoreValue();
                this.score += pointsAwarded;
                Log.i(TAG, "Awarded " + pointsAwarded + " points. Total score: " + this.score);

                // Make the table available again
                table.vacate();
                tableFound = true;

                checkLevelUp();


                break;
            }
        }

        if (!tableFound) {
            Log.w(TAG, "Could not find occupied table for customer " + customerToClear.getDisplayId() + " to clear.");
        }
    }

    private void checkLevelUp() {
        if (score >= scoreForNextLevel) {
            currentLevel++;
            scoreForNextLevel += SCORE_PER_LEVEL; // Set threshold for the next level

            // Gain a life, respecting the maximum
            playerLives++;
            if (playerLives > MAX_LIVES) {
                playerLives = MAX_LIVES;
            }

            Log.i(TAG, "LEVEL UP! Reached Level " + currentLevel + ". Lives: " + playerLives + "/" + MAX_LIVES + ". Next level at " + scoreForNextLevel + " points.");
        }
    }
}