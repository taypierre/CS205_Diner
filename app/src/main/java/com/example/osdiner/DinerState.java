// DinerState.java (Cleaned up)
package com.example.osdiner;

import static com.example.osdiner.Customer.UNIVERSAL_EATING_DURATION;

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
    private final List<Customer> waitingCustomers; // Customers visible in the waiting area

    private final List<Table> tables = new ArrayList<>();

    private static final float COOK_DURATION_SECONDS = 8.0f;

    private static final float EATING_DURATION_SECONDS = 10.0f;

    private int score = 0;


    //Lives and Levels
    private static final int INITIAL_LIVES = 5;
    private static final int MAX_LIVES = 7;
    private static final int SCORE_PER_LEVEL = 500;

    private int playerLives;
    private int currentLevel;
    private int scoreForNextLevel;

    // --- Game Over Condition Fields ---
    private static final int MAX_ANGRY_CUSTOMERS = 5; // Lose after 5 angry customers
    private int angryCustomersCount = 0;
    private boolean isGameOver = false;

    private CustomerGeneratorThread customerGeneratorRef = null;


    // TODO: Add variables for tables, waiter position, etc. later

    public DinerState() {
        customerArrivalQueue = new ArrayBlockingQueue<>(ARRIVAL_QUEUE_CAPACITY);
        waitingCustomers = new ArrayList<>();
        score = 0;
        isGameOver = false;
        playerLives = INITIAL_LIVES;
        currentLevel = 1;
        scoreForNextLevel = SCORE_PER_LEVEL;
        Log.d(TAG, "DinerState initialized (default constructor)");
    }

    public DinerState(int tableCount, RectF counterRect, RectF[] tableRects, RectF doorRect) {
        // Initialize core components
        customerArrivalQueue = new ArrayBlockingQueue<>(ARRIVAL_QUEUE_CAPACITY);
        waitingCustomers = new ArrayList<>();
        score = 0;
        angryCustomersCount = 0;
        isGameOver = false;

        // <<< Initialize Lives and Leveling >>>
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
    public static int getMaxLives() { return MAX_LIVES; } // Static getter for the constant
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
        // Consider returning a copy if modification elsewhere is a risk,
        // but for now, direct access is fine if only GameThread modifies via processCustomerArrivals
        // and DinerView only reads during drawGame.
        return waitingCustomers;
    }

    public List<Table> getTables() {
        return tables;
    }

    // Update game state based on time elapsed since last frame
    public int update(double deltaTime) {
        if (isGameOver) {
            return 0;     // <<< Return immediately if game over
        }

        int angryLeavesThisFrame = 0;
        // Convert delta time to float for calculations
        float dt = (float)deltaTime;

        // --- 1. Update Patience for WAITING Customers ---
        // (Your existing logic for waiting customers seems correct - no changes needed here)
        Iterator<Customer> waitingIterator = waitingCustomers.iterator();
        while (waitingIterator.hasNext()) {
            Customer customer = waitingIterator.next();
            if (customer.getState() == Customer.CustomerState.WAITING_QUEUE) {
                customer.decreasePatience(PATIENCE_DECREASE_RATE * dt);
                if (customer.getPatience() <= 0 && customer.getState() != Customer.CustomerState.ANGRY_LEFT) {
                    customer.leaveAngry();
                    waitingIterator.remove();

                    playerLives--;
                    angryLeavesThisFrame++; // Keep tracking for sound/vibration trigger
                    Log.i(TAG, customer.getDisplayId() + " left angry from waiting. Lives remaining: " + playerLives);

                    Log.i(TAG, customer.getDisplayId() + " removed from waiting queue (patience ran out). Size: " + waitingCustomers.size());
                }
            }
        }

        // --- 2. Update SEATED Customers (Timers and Patience) ---
        for (Table table : tables) {
            if (table.isOccupied()) {
                Customer customer = table.getSeatedCustomer();
                if (customer != null && customer.getState() != Customer.CustomerState.ANGRY_LEFT) {

                    // Only process timers/patience if game not over? Prevents state changes after game over.
                    if (!isGameOver) { // <<< Wrap state-dependent logic

                        // === A. Handle State-Specific Timers ===
                        switch (customer.getState()) {
                            case SEATED_IDLE:
                                customer.decreaseOrderReadyTimer(dt);
                                if (customer.getTimeUntilReadyToOrder() <= 0) {
                                    customer.setState(Customer.CustomerState.WAITING_ORDER_CONFIRM);
                                    // Log.d(...) // Log if needed
                                }
                                break;
                            case WAITING_FOOD:
                                customer.decreaseCookingTimer(dt);
                                if (customer.isCookingFinished()) {
                                    customer.setState(Customer.CustomerState.FOOD_READY);
                                    // Log.i(...) // Log if needed
                                }
                                break;
                            case EATING:
                                customer.decreaseEatingTimer(dt);
                                if (customer.isFinishedEating()) {
                                    customer.setState(Customer.CustomerState.READY_TO_LEAVE);
                                    // Log.i(...) // Log if needed
                                }
                                break;
                        } // End Switch Timer Handling

                        // === B. Decrease Patience based on current state ===
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
                                table.vacate(); // Vacate table BEFORE incrementing/logging count

                                // <<< CHANGE START: Lose a life instead of incrementing angry count >>>
                                playerLives--;
                                angryLeavesThisFrame++; // Increment frame counter for sound/vibration
                                Log.i(TAG, customer.getDisplayId() + " left angry from table " + table.id + ". Lives remaining: " + playerLives);
                                // <<< CHANGE END >>>

                                continue; // Skip further processing for this table this frame
                            }
                        } // End if shouldDecreasePatience

                    } // End wrap !isGameOver

                } // end if customer not null
            } // end if table occupied
        }  // End of table loop


        if (!isGameOver && playerLives <= 0) {
            Log.i(TAG, "GAME OVER - Player lives reached 0!");
            isGameOver = true;
            // Stop customer generation immediately
            if (customerGeneratorRef != null) {
                Log.i(TAG, "Signaling Customer Generator to stop (Game Over).");
                customerGeneratorRef.stopGenerating();
            } else {
                Log.e(TAG, "Cannot stop Customer Generator: Reference is NULL in DinerState!");
            }
        }


        // TODO: 3. Update waiter position/state based on deltaTime (Req #2a - Animation)
        // TODO: 4. Update cooking/eating timers and state changes
        // TODO: 5. Handle other game logic
        return angryLeavesThisFrame;
    }








    // Method called by GameThread's interval timer
    public void processCustomerArrivals() {
        // Move all currently available customers from the background queue
        // to the visible waiting list. drainTo is non-blocking and efficient.
        int count = customerArrivalQueue.drainTo(waitingCustomers); // Adds all available elements
        if (count > 0) {
            Log.d(TAG, "Interval Trigger: Moved " + count + " customers to waiting list. Total waiting: " + waitingCustomers.size());
        } else {
            // This is normal, just means no customers were generated since the last check
            // Log.d(TAG, "Interval Trigger: No new customers in arrival queue."); // Optional: can remove this log if too noisy
        }
        // TODO: Potentially limit the size of waitingCustomers visually or implement scrolling
    }

    public boolean trySeatCustomerByDrag(Customer customerToSeat, Table targetTable) {
        if (customerToSeat == null || targetTable == null) {
            Log.w(TAG, "SEATING FAILED (Drag): Null customer or table provided.");
            return false;
        }

        // Check if the customer is actually in the waiting list
        // Use synchronized block if modifying waitingCustomers from multiple threads (GameThread/UI)
        // For now, assume updates happen primarily on GameThread logic cycle
        if (!waitingCustomers.contains(customerToSeat)) {
            Log.e(TAG, "SEATING FAILED (Drag): Customer " + customerToSeat.getDisplayId() + " not found in waiting list!");
            return false; // Critical error or customer left already
        }

        // Check if the target table is free
        if (targetTable.isOccupied()) {
            Log.w(TAG, "SEATING FAILED (Drag): Table " + targetTable.id + " is already occupied.");
            return false;
        }

        // --- Success Path ---
        // 1. Remove customer from waiting list
        boolean removed = waitingCustomers.remove(customerToSeat);
        if (!removed) {
            // This shouldn't happen if the contains check passed, but good to log
            Log.e(TAG, "SEATING FAILED (Drag): Failed to remove customer " + customerToSeat.getDisplayId() + " from waiting list after check!");
            return false;
        }
        Log.d(TAG, "Removed " + customerToSeat.getDisplayId() + " from waiting list (size=" + waitingCustomers.size() + ")");

        // 2. Occupy the table
        targetTable.occupy(customerToSeat);

        // 3. Set customer state (and start timers if needed)
        customerToSeat.setState(Customer.CustomerState.SEATED_IDLE);
        // Assuming occupy or setState starts the SEATED_IDLE timer (timeUntilReadyToOrder)
        // If not, start it explicitly here.

        Log.i(TAG, "SEATING SUCCESS (Drag): Seated " + customerToSeat.getDisplayId() + " at table " + targetTable.id + ". State: " + customerToSeat.getState());
        return true;
    }


    public void confirmCustomerOrder(Customer customer) {
        if (customer != null && customer.getState() == Customer.CustomerState.WAITING_ORDER_CONFIRM) {
            customer.setState(Customer.CustomerState.WAITING_FOOD);
            // === Start the cooking timer ===
            customer.startCookingTimer(COOK_DURATION_SECONDS);
            // ===============================
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

        // Check 1: Is the target table occupied?
        if (!targetTable.isOccupied()) {
            Log.w(TAG, "DELIVERY FAILED: Target table " + targetTable.id + " is not occupied (for customer " + customerWhoseFood.getDisplayId() + ").");
            return false;
        }

        // Check 2: Does the customer on the table match the food's intended customer?
        Customer seatedCustomer = targetTable.getSeatedCustomer();
        if (seatedCustomer != customerWhoseFood) {
            Log.w(TAG, "DELIVERY FAILED: Food for " + customerWhoseFood.getDisplayId()
                    + " dropped on table " + targetTable.id + " occupied by "
                    + (seatedCustomer != null ? seatedCustomer.getDisplayId() : "null") + ".");
            return false;
        }

        // Check 3: Is the customer actually waiting for food (state FOOD_READY)?
        if (seatedCustomer.getState() != Customer.CustomerState.FOOD_READY) {
            Log.w(TAG, "DELIVERY FAILED: Customer " + seatedCustomer.getDisplayId()
                    + " at table " + targetTable.id + " is in state " + seatedCustomer.getState()
                    + ", not FOOD_READY.");
            return false;
        }

        // --- Success Path ---
        // 1. Change customer state to EATING
        seatedCustomer.setState(Customer.CustomerState.EATING);
        Log.i(TAG, "DELIVERY SUCCESS: Food delivered to " + seatedCustomer.getDisplayId()
                + " at table " + targetTable.id + ". State set to EATING.");

        seatedCustomer.startEatingTimer();
        // seatedCustomer.startEatingTimer(EATING_DURATION_SECONDS);

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
            // Optional: Maybe still clear if they left angry? Depends on game logic.
            // if (customerToClear.getState() == Customer.CustomerState.ANGRY_LEFT) { ... }
            return;
        }

        boolean tableFound = false;
        for (Table table : tables) {
            if (table.isOccupied() && table.getSeatedCustomer() == customerToClear) {
                Log.i(TAG, "Clearing table " + table.id + " for customer " + customerToClear.getDisplayId());

                // --- SCORE ---
                int pointsAwarded = 100; // Example score value
                this.score += pointsAwarded;
                Log.i(TAG, "Awarded " + pointsAwarded + " points. Total score: " + this.score);
                // -----------------

                table.vacate(); // Make the table available again
                tableFound = true;

                checkLevelUp();


                break; // Found and cleared the table
            }
        }

        if (!tableFound) {
            // This might happen if the state changed between the draw and the tap, or other logic errors
            Log.w(TAG, "Could not find occupied table for customer " + customerToClear.getDisplayId() + " to clear.");
        }
    }

    private void checkLevelUp() {
        if (score >= scoreForNextLevel) {
            currentLevel++;
            scoreForNextLevel += SCORE_PER_LEVEL; // Set threshold for the *next* level

            // Gain a life, respecting the maximum
            playerLives++;
            if (playerLives > MAX_LIVES) {
                playerLives = MAX_LIVES;
            }

            Log.i(TAG, "LEVEL UP! Reached Level " + currentLevel + ". Lives: " + playerLives + "/" + MAX_LIVES + ". Next level at " + scoreForNextLevel + " points.");
        }
    }
}