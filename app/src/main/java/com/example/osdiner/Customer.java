package com.example.osdiner;

// --- Add Missing Imports ---
import android.graphics.Color;
import android.util.Log;

import androidx.annotation.DrawableRes;

import java.util.Random;
import java.util.Map;
import java.util.EnumMap;
// --------------------------

public class Customer {

    // --- Nested Enum for Customer Types ---
    public enum CustomerType {
        NORMAL,
        IMPATIENT,
        VIP
        // Add more later
    }

    // --- Nested Class for Configuration Data --- (Replaced record)
    public static class CustomerConfig { // Use 'static' if nested inside Customer
        private final CustomerType type;
        private final float initialPatience;
        // eatingDuration removed for now
        private final int scoreValue;
        @DrawableRes
        private final int iconResId;

        private final float patienceRateMultiplier;

        // Constructor
        public CustomerConfig(CustomerType type, float initialPatience, int scoreValue, @DrawableRes int iconResId, float patienceRateMultiplier) {
            this.type = type;
            this.initialPatience = initialPatience;
            this.scoreValue = scoreValue;
            this.iconResId = iconResId;
            this.patienceRateMultiplier = patienceRateMultiplier;
        }


        public CustomerType getType() {
            return type;
        }
        public float getInitialPatience() {
            return initialPatience;
        }
        public int getScoreValue() {
            return scoreValue;
        }
        @DrawableRes public int getIconResId() {
            return iconResId;
        }

        public float getPatienceRateMultiplier() {
            return patienceRateMultiplier;
        }

    }
    // --- End CustomerConfig Class ---


    // --- Static Configuration Map --- // <<< ADDED STATIC MAP & INIT
    private static final Map<CustomerType, CustomerConfig> CONFIGS;
    static {
        CONFIGS = new EnumMap<>(CustomerType.class);
        CONFIGS.put(CustomerType.NORMAL, new CustomerConfig(
                CustomerType.NORMAL, 250f, 100,
                R.drawable.customer_normal,
                1.0f
        ));
        CONFIGS.put(CustomerType.IMPATIENT, new CustomerConfig(
                CustomerType.IMPATIENT, 200f, 150,
                R.drawable.customer_impatient,
                1.5f
        ));
        CONFIGS.put(CustomerType.VIP, new CustomerConfig(
                CustomerType.VIP, 180f, 250,
                R.drawable.customer_vip,
                1.0f
        ));
    }
    // -----------------------------------

    // --- Static Helper to Get Config --- // <<< ADDED GETCONFIG METHOD
    public static CustomerConfig getConfig(CustomerType type) {
        // Default to NORMAL config if the requested type isn't found (safe fallback)
        return CONFIGS.getOrDefault(type, CONFIGS.get(CustomerType.NORMAL));
    }
    // -----------------------------------


    // --- Existing Enum ---
    public enum CustomerState {
        WAITING_QUEUE, SEATED_IDLE, WAITING_ORDER_CONFIRM, WAITING_FOOD,
        FOOD_READY, EATING, READY_TO_LEAVE, ANGRY_LEFT
    }

    // --- Customer Instance Fields ---
    private static final String TAG = "Customer";
    private static int nextId = 0;
    private static Random random = new Random();

    public final int id;
    private final CustomerType type;
    private final float initialPatience;
    private final int scoreValue;
    @DrawableRes private final int customerIconResId;

    private final float patienceRateMultiplier;

    private CustomerState state;
    private float patience; // Current patience

    // Timers
    private float timeUntilReadyToOrder;
    private float timeUntilFoodReady;
    private float timeUntilFinishedEating;

    // --- Constants ---
    // Note: MAX_PATIENCE might be less relevant now that initialPatience varies
    // public static final float MAX_PATIENCE = 100.0f;
    private static final float PATIENCE_DECREASE_RATE = 2.0f; // Units per second
    public static final float ORDER_READY_DELAY = 5.0f;
    public static final float UNIVERSAL_EATING_DURATION = 10.0f;


    // --- Constructor ---
    public Customer() {
        this.id = nextId++;

        // 1. Assign a Type randomly
        CustomerType[] allTypes = CustomerType.values();
        this.type = allTypes[random.nextInt(allTypes.length)];

        // 2. Get Config for this type
        CustomerConfig config = getConfig(this.type); // Use static getter

        // 3. Initialize instance fields from config using GETTERS // <<< FIXED GETTERS
        this.initialPatience = config.getInitialPatience();
        this.scoreValue = config.getScoreValue();
        this.customerIconResId = config.getIconResId();
        this.patienceRateMultiplier = config.getPatienceRateMultiplier();
        //-----------------------------------------------------------

        // 4. Initialize dynamic state
        this.patience = this.initialPatience; // Start with type-specific patience
        this.state = CustomerState.WAITING_QUEUE; // Initial state

        // Initialize timers
        this.timeUntilReadyToOrder = -1f; // Start timer only when SEATED_IDLE
        this.timeUntilFoodReady = -1f;
        this.timeUntilFinishedEating = -1f;

        // Updated Log message to use the assigned values
        Log.d(TAG, "Created " + getDisplayId() + " of type " + this.type
                + " (Patience: " + this.initialPatience + ", RateMult: " + this.patienceRateMultiplier
                + ", Score: " + this.scoreValue + ", IconID: " + this.customerIconResId + ")"); // Log Icon ID
    }

    // --- Getters ---
    public String getDisplayId() {
        // <<< METHOD MODIFIED >>>
        String prefix; // Declare prefix variable

        // Use traditional switch STATEMENT
        switch (this.type) {
            case NORMAL:
                prefix = "C";
                break; // Need break statements
            case IMPATIENT:
                prefix = "IMP";
                break;
            case VIP:
                prefix = "VIP";
                break;
            default: // Add a default case as good practice
                prefix = "C"; // Fallback to Normal prefix
                Log.w(TAG, "getDisplayId() encountered unexpected type: " + this.type); // Log warning
                break;
        }
        return prefix + id; // Combine prefix and unique numeric ID
    }
    public CustomerState getState() { return this.state; }
    public float getPatience() { return this.patience; }
    public CustomerType getType() { return type; }
    public int getScoreValue() { return scoreValue; }
    @DrawableRes public int getCustomerIconResId() { return customerIconResId; }

    // Calculate percentage based on initial patience for this customer
    public float getPatiencePercentage() {
        if (this.initialPatience <= 0) return 0.0f;
        // Clamp value between 0 and 1
        return Math.max(0.0f, Math.min(1.0f, this.patience / this.initialPatience));
    }

    // --- State and Timer Methods ---
    // (leaveAngry, setState, decreasePatience, timer methods remain largely the same)
    // ... ensure startEatingTimer uses UNIVERSAL_EATING_DURATION ...
    public void leaveAngry() {
        this.state = CustomerState.ANGRY_LEFT;
        this.patience = 0;
        Log.w(TAG, getDisplayId() + " (" + this.type + ") left angry!");
    }

    public void setState(CustomerState newState) {
        if (this.state != newState) {
            Log.d(TAG, getDisplayId() + " changing state from " + this.state + " to " + newState);
            this.state = newState;

            if (newState == CustomerState.SEATED_IDLE) {
                this.timeUntilReadyToOrder = ORDER_READY_DELAY;
                Log.d(TAG, getDisplayId() + " order ready timer started ("+this.timeUntilReadyToOrder+"s)");
            } else {
                if (newState != CustomerState.WAITING_ORDER_CONFIRM) {
                    this.timeUntilReadyToOrder = -1f;
                }
            }
        }
    }

    public void decreasePatience(float deltaTime) {
        if (this.state != CustomerState.ANGRY_LEFT) {
            // Calculate actual decrease using the multiplier
            float decreaseAmount = (PATIENCE_DECREASE_RATE * this.patienceRateMultiplier) * deltaTime; // <<< Use multiplier
            this.patience -= decreaseAmount;
        }
    }

    public void decreaseOrderReadyTimer(float deltaTime) {
        if (this.state == CustomerState.SEATED_IDLE && this.timeUntilReadyToOrder > 0) {
            this.timeUntilReadyToOrder -= deltaTime;
        }
    }
    public float getTimeUntilReadyToOrder() { return this.timeUntilReadyToOrder; }
    public boolean isReadyToOrder() {
        return this.state == CustomerState.SEATED_IDLE && this.timeUntilReadyToOrder <= 0;
    }

    public void startCookingTimer(float duration) { this.timeUntilFoodReady = duration; }
    public void decreaseCookingTimer(float deltaTime) {
        if (this.state == CustomerState.WAITING_FOOD && this.timeUntilFoodReady > 0) {
            this.timeUntilFoodReady -= deltaTime;
        }
    }
    public float getTimeUntilFoodReady() { return this.timeUntilFoodReady; }
    public boolean isCookingFinished() {
        return this.state == CustomerState.WAITING_FOOD && this.timeUntilFoodReady <= 0;
    }

    public void startEatingTimer() {
        this.timeUntilFinishedEating = UNIVERSAL_EATING_DURATION; // Uses the constant internally
        Log.d(TAG, getDisplayId() + " started eating timer: " + UNIVERSAL_EATING_DURATION + "s");
    }
    public void decreaseEatingTimer(float deltaTime) {
        if (this.state == CustomerState.EATING && this.timeUntilFinishedEating > 0) {
            this.timeUntilFinishedEating -= deltaTime;
        }
    }
    public boolean isFinishedEating() {
        return this.state == CustomerState.EATING && this.timeUntilFinishedEating <= 0;
    }
    public float getTimeUntilFinishedEating() { return timeUntilFinishedEating; }

} // End of Customer class