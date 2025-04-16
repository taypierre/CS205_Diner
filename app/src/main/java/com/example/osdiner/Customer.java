package com.example.osdiner;

import android.util.Log;

import androidx.annotation.DrawableRes;

import java.util.Random;
import java.util.Map;
import java.util.EnumMap;


public class Customer {

    // Enum for Customer Types
    public enum CustomerType {
        NORMAL,
        IMPATIENT,
        VIP
    }

    public static class CustomerConfig {
        private final CustomerType type;
        private final float initialPatience;
        private final int scoreValue;
        @DrawableRes
        private final int iconResId;

        private final float patienceRateMultiplier;

        public CustomerConfig(CustomerType type, float initialPatience, int scoreValue, @DrawableRes int iconResId, float patienceRateMultiplier) {
            this.type = type;
            this.initialPatience = initialPatience;
            this.scoreValue = scoreValue;
            this.iconResId = iconResId;
            this.patienceRateMultiplier = patienceRateMultiplier;
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

    public static CustomerConfig getConfig(CustomerType type) {
        return CONFIGS.getOrDefault(type, CONFIGS.get(CustomerType.NORMAL));
    }

    public enum CustomerState {
        WAITING_QUEUE, SEATED_IDLE, WAITING_ORDER_CONFIRM, WAITING_FOOD,
        FOOD_READY, EATING, READY_TO_LEAVE, ANGRY_LEFT
    }

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
    private float patience;

    private float timeUntilReadyToOrder;
    private float timeUntilFoodReady;
    private float timeUntilFinishedEating;
    private static final float PATIENCE_DECREASE_RATE = 2.0f;
    public static final float ORDER_READY_DELAY = 5.0f;
    public static final float UNIVERSAL_EATING_DURATION = 10.0f;

    public Customer() {
        this.id = nextId++;

        // Assign a Type randomly
        CustomerType[] allTypes = CustomerType.values();
        this.type = allTypes[random.nextInt(allTypes.length)];

        // Get Config for this type
        CustomerConfig config = getConfig(this.type);

        // Initialize instance fields from config using GETTERS
        this.initialPatience = config.getInitialPatience();
        this.scoreValue = config.getScoreValue();
        this.customerIconResId = config.getIconResId();
        this.patienceRateMultiplier = config.getPatienceRateMultiplier();

        // Initialize dynamic state
        this.patience = this.initialPatience;
        this.state = CustomerState.WAITING_QUEUE;

        // Initialize timers
        this.timeUntilReadyToOrder = -1f;
        this.timeUntilFoodReady = -1f;
        this.timeUntilFinishedEating = -1f;

        Log.d(TAG, "Created " + getDisplayId() + " of type " + this.type
                + " (Patience: " + this.initialPatience + ", RateMult: " + this.patienceRateMultiplier
                + ", Score: " + this.scoreValue + ", IconID: " + this.customerIconResId + ")");
    }

    public String getDisplayId() {

        String prefix;

        switch (this.type) {
            case NORMAL:
                prefix = "C";
                break;
            case IMPATIENT:
                prefix = "IMP";
                break;
            case VIP:
                prefix = "VIP";
                break;
            default:
                prefix = "C";
                Log.w(TAG, "getDisplayId() encountered unexpected type: " + this.type);
                break;
        }
        return prefix + id;
    }
    public CustomerState getState() { return this.state; }
    public float getPatience() { return this.patience; }
    @DrawableRes public int getCustomerIconResId() { return customerIconResId; }

    // Calculate percentage based on initial patience for this customer
    public float getPatiencePercentage() {
        if (this.initialPatience <= 0) return 0.0f;
        return Math.max(0.0f, Math.min(1.0f, this.patience / this.initialPatience));
    }

    // State and Timer Methods
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

    public void startCookingTimer(float duration) { this.timeUntilFoodReady = duration; }
    public void decreaseCookingTimer(float deltaTime) {
        if (this.state == CustomerState.WAITING_FOOD && this.timeUntilFoodReady > 0) {
            this.timeUntilFoodReady -= deltaTime;
        }
    }
    public boolean isCookingFinished() {
        return this.state == CustomerState.WAITING_FOOD && this.timeUntilFoodReady <= 0;
    }

    public void startEatingTimer() {
        this.timeUntilFinishedEating = UNIVERSAL_EATING_DURATION;
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
    public static void resetCustomerIdCounter() {
        nextId = 0;
    }

    public int getScoreValue() {
        return this.scoreValue;
    }

}