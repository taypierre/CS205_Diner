package com.example.osdiner;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.AttributeSet;
import android.util.Log; // Import Log
import android.util.Pair;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import androidx.annotation.NonNull; // Use androidx annotation
import androidx.annotation.Nullable;

import java.util.Locale;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DinerView extends SurfaceView implements SurfaceHolder.Callback {

    private static final String TAG = "DinerView"; // Tag for logging

    private Context context;
    private GameThread gameThread;
    private DinerState dinerState; // Will hold game objects like tables, customers, waiter
    private Paint backgroundPaint;
    private Paint tablePaint; // Example paint

    // === Customer Generator Thread Field ===
    private CustomerGeneratorThread customerGenerator;
    private Paint customerPaint; // Paint for drawing customers
    private Paint selectedCustomerPaint; // Paint for selected customer
    private Paint waitingAreaPaint;
    private Paint counterPaint;
    private Paint textPaint; // General purpose text
    private Paint uiTextPaint;

    private Paint patienceBarBgPaint;
    private Paint patienceBarFgPaint;

    // === Paint for Order Indicator ===
    private Paint orderIndicatorPaint;
    private Paint orderIndicatorTextPaint;

    // === Paint for Food Ready Indicator ===
    private Paint foodReadyIndicatorPaint;
    private Paint foodItemPaint;


    private Paint clearTableIndicatorPaint;
    private Paint clearTableIndicatorTextPaint;

    // === Layout Dimension Variables ===
    // These will hold the calculated pixel coordinates/sizes
    private RectF waitingAreaRect;
    private RectF counterRect;
    private RectF[] tableRects; // Array to hold multiple tables


    private Paint bitmapPaint; // Paint for drawing bitmaps

    // Map to hold pre-loaded bitmaps <ResourceID, Bitmap>
    private Map<Integer, Bitmap> customerBitmaps = new HashMap<>();

    private Bitmap heartBitmap;
    private static final float HEART_SIZE = 50f;
    private static final float HEART_SPACING = 10f;
    private static final float HEART_MARGIN_TOP = 50f;
    private static final float HEART_MARGIN_LEFT = 30f;

    private static final float CUSTOMER_ICON_WIDTH = 80f; // Adjust as needed
    private static final float CUSTOMER_ICON_HEIGHT = 100f;

    private boolean isDragging = false;
    private Customer draggedCustomer = null; // Customer being dragged from waiting list
    private Customer draggedFoodCustomer = null;
    private float dragX = 0f;
    private float dragY = 0f;


    // === Patience Bar Constants ===
    private static final float PATIENCE_BAR_WIDTH = 60f;
    private static final float PATIENCE_BAR_HEIGHT = 8f;
    private static final float PATIENCE_BAR_Y_OFFSET = 5f;

    // === For Interaction ===
    // Stores the clickable area for each displayed customer ID
    private List<RectF> waitingCustomerTapAreas = new ArrayList<>();

    // === Tap Areas for Order Indicators ===
    private List<Pair<RectF, Customer>> confirmOrderTapAreas = new ArrayList<>();

    private List<Pair<RectF, Customer>> foodReadyTapAreas = new ArrayList<>();

    private List<Pair<RectF, Customer>> clearTableTapAreas = new ArrayList<>();



    // Temporarily stores bounds during calculation
    private Rect textBounds = new Rect();
    // ==========================

    // === Score Animation Fields ===
    private float displayedScore = 0f; // Score currently shown on screen (float for smooth animation)
    private static final float SCORE_ANIMATION_SPEED = 4.0f;



    // === Sound and Vibration Fields ===
    private SoundPool soundPool;
    private int angrySoundId = -1; // Store the ID for the loaded sound
    private boolean soundPoolLoaded = false;
    private Vibrator vibrator; // Vibrator service


    // === Menu Button Fields ===
    private RectF menuButtonArea;
    private Paint menuButtonPaint;
    private Paint menuButtonTextPaint; // Changed from IconPaint
    private static final float MENU_BUTTON_WIDTH = 120f; // Adjusted size for text maybe
    private static final float MENU_BUTTON_HEIGHT = 70f;
    private static final float MENU_BUTTON_MARGIN = 30f;

    private boolean gameOverDialogShown = false;
    // <<< END MENU BUTTON FIELDS >>>

    private static final float FOOD_PLATE_DIAMETER = 60f;

    // Constructor needed for inflating from XML
    public DinerView(Context context, @Nullable  AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        getHolder().addCallback(this);

        // --- Initialize Paints ---
        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.rgb(255, 167, 110)); // Beige background

        tablePaint = new Paint();
        tablePaint.setColor(Color.rgb(139, 69, 19)); // Brown for tables
        tablePaint.setStyle(Paint.Style.FILL);

        customerPaint = new Paint();
        customerPaint.setColor(Color.BLACK); // Dark Gray text for customers
        customerPaint.setTextSize(50f);
        customerPaint.setAntiAlias(true);


        // === Initialize Order Indicator Paints ===
        orderIndicatorPaint = new Paint();
        orderIndicatorPaint.setColor(Color.BLUE); // Blue button/indicator
        orderIndicatorPaint.setStyle(Paint.Style.FILL);

        orderIndicatorTextPaint = new Paint();
        orderIndicatorTextPaint.setColor(Color.WHITE);
        orderIndicatorTextPaint.setTextSize(25f); // Smaller text for button
        orderIndicatorTextPaint.setTextAlign(Paint.Align.CENTER);
        orderIndicatorTextPaint.setAntiAlias(true);
        // =======================================

        // === Initialize Food Ready Indicator Paint ===
        foodReadyIndicatorPaint = new Paint();
        foodReadyIndicatorPaint.setColor(Color.LTGRAY); // Light gray for plate
        foodReadyIndicatorPaint.setStyle(Paint.Style.FILL);
        foodReadyIndicatorPaint.setAntiAlias(true);
        // ================================================

        foodItemPaint = new Paint();
        foodItemPaint.setColor(Color.rgb(255, 255, 255));
        foodItemPaint.setStyle(Paint.Style.FILL);
        foodItemPaint.setAntiAlias(true);



        // Paint for selected customer text
        selectedCustomerPaint = new Paint();
        selectedCustomerPaint.setColor(Color.RED); // Highlight in Red
        selectedCustomerPaint.setTextSize(35f);
        selectedCustomerPaint.setFakeBoldText(true); // Make it bold
        selectedCustomerPaint.setAntiAlias(true);


        clearTableIndicatorPaint = new Paint();
        clearTableIndicatorPaint.setColor(Color.rgb(34, 139, 34)); // Forest Green (or Color.GREEN)
        clearTableIndicatorPaint.setStyle(Paint.Style.FILL);
        clearTableIndicatorPaint.setAntiAlias(true);

        clearTableIndicatorTextPaint = new Paint();
        clearTableIndicatorTextPaint.setColor(Color.WHITE); // White text for contrast
        clearTableIndicatorTextPaint.setTextSize(25f); // Adjust size as needed
        clearTableIndicatorTextPaint.setTextAlign(Paint.Align.CENTER);
        clearTableIndicatorTextPaint.setFakeBoldText(true); // Make text bold
        clearTableIndicatorTextPaint.setAntiAlias(true);

        waitingAreaPaint = new Paint();
        waitingAreaPaint.setColor(Color.LTGRAY); // Light gray for waiting area background
        waitingAreaPaint.setStyle(Paint.Style.FILL);

        counterPaint = new Paint();
        counterPaint.setColor(Color.GRAY); // Gray for counter
        counterPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(35f);
        textPaint.setAntiAlias(true);

        uiTextPaint = new Paint(); // <<< Initialize new paint
        uiTextPaint.setColor(Color.BLACK);
        uiTextPaint.setTextSize(40f); // Default size for UI text
        uiTextPaint.setAntiAlias(true);
        uiTextPaint.setTextAlign(Paint.Align.RIGHT); // Default alignment for score/UI



        patienceBarBgPaint = new Paint();
        patienceBarBgPaint.setColor(Color.rgb(200, 200, 200)); // Light grey background
        patienceBarBgPaint.setStyle(Paint.Style.FILL);

        patienceBarFgPaint = new Paint();
        patienceBarFgPaint.setColor(Color.GREEN); // Start green
        patienceBarFgPaint.setStyle(Paint.Style.FILL);


        bitmapPaint = new Paint();
        bitmapPaint.setAntiAlias(true);
        bitmapPaint.setFilterBitmap(true); // Optional: for smoother scaling


        // <<< Initialize Menu Button Paints >>>
        menuButtonPaint = new Paint();
        menuButtonPaint.setColor(Color.argb(180, 100, 100, 100)); // Semi-transparent gray background
        menuButtonPaint.setStyle(Paint.Style.FILL);

        menuButtonTextPaint = new Paint(); // Changed from IconPaint
        menuButtonTextPaint.setColor(Color.WHITE);
        menuButtonTextPaint.setTextSize(30f); // Adjust text size as needed
        menuButtonTextPaint.setTextAlign(Paint.Align.CENTER); // Center align text
        menuButtonTextPaint.setAntiAlias(true);
        // <<< Initialize Menu Button Paints >>>

        initializeSoundAndVibration(context);

        // --- Load Customer Bitmaps ---
        loadBitmaps(context.getResources());


        displayedScore = 0f; // Start display at 0

        setFocusable(true);
        Log.d(TAG, "DinerView constructed");
    }


    private void initializeSoundAndVibration(Context context) {
        // --- SoundPool Setup ---
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(2) // Allow 2 simultaneous sounds
                .setAudioAttributes(audioAttributes)
                .build();

        // Listener to know when sounds are loaded
        soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> {
            if (status == 0) {
                // Successfully loaded
                soundPoolLoaded = true;
                Log.d(TAG, "Sound ID " + sampleId + " loaded successfully.");
            } else {
                // Failed to load
                Log.e(TAG, "Failed to load sound ID " + sampleId + ", status: " + status);
            }
        });

        // Load the sound
        try {
            angrySoundId = soundPool.load(context, R.raw.angry, 1); // Priority 1
            if (angrySoundId == 0) { // Check if load returns 0 (error)
                Log.e(TAG, "Error loading sound R.raw.angry: SoundPool.load returned 0. Check file presence and format.");
            } else {
                Log.d(TAG, "Sound R.raw.angry queued for loading with ID: " + angrySoundId);
            }
        } catch (android.content.res.Resources.NotFoundException e) {
            Log.e(TAG, "Sound file R.raw.angry not found! Make sure it's in res/raw/", e);
            angrySoundId = -1; // Ensure ID is invalid
        }

        // --- Vibrator Setup ---
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) {
            Log.w(TAG, "Device does not have a vibrator or service not available.");
            vibrator = null; // Set to null if not available
        } else {
            Log.d(TAG, "Vibrator service obtained.");
        }
    }

    // === NEW Helper Method to Calculate Layout ===
    private void calculateLayout(int width, int height) {
        Log.d(TAG, "Calculating layout for width=" + width + ", height=" + height);

        // --- Waiting Area ---
        // Example: Left 1/4th of the screen, full height with margin
        float waitingAreaWidth = width / 5.0f;
        float margin = 20f;
        waitingAreaRect = new RectF(margin, margin, margin + waitingAreaWidth, height - margin);

        // --- Counter ---
        float newCounterTop = height * 0.70f;     // Start counter 70% down the screen
        float newCounterBottom = height * 0.95f; // End counter 85% down the screen
        // float counterHeight = newCounterBottom - newCounterTop; // Calculated height if needed
        float counterLeft = waitingAreaRect.right + margin; // Start after waiting area
        float counterRight = width - margin; // Go to right margin
        counterRect = new RectF(counterLeft, newCounterTop, counterRight, newCounterBottom);

        // --- Tables ---
        // Example: 3 Tables in the remaining space
        int numTables = 3;
        tableRects = new RectF[numTables];
        float tableAreaLeft = waitingAreaRect.right + margin;
        float tableAreaRight = width - margin;
        float tableAreaTop = margin;

        float tableAreaBottom = newCounterTop - margin; // Use the calculated newCounterTop
        float tableAreaWidth = tableAreaRight - tableAreaLeft;
        float tableAreaHeight = tableAreaBottom - tableAreaTop;

        float tableSize = Math.min(tableAreaWidth / (numTables + 1), tableAreaHeight / 2.5f); // Size based on available space
        // Adjust spacing if needed
        float tableSpacingX = (tableAreaWidth - (numTables * tableSize)) / (numTables + 1);

        for (int i = 0; i < numTables; i++) {
            float tableLeft = tableAreaLeft + (i + 1) * tableSpacingX + i * tableSize;
            float tableTop = tableAreaTop + (tableAreaHeight - tableSize) / 2.0f; // Center vertically
            tableRects[i] = new RectF(tableLeft, tableTop, tableLeft + tableSize, tableTop + tableSize);
        }
        Log.d(TAG, "Layout calculated.");
    }


    public void pauseGame() {
        if (gameThread != null) {
            gameThread.pauseGame();
            Log.d(TAG, "Game paused via DinerView.");
        }
        if (customerGenerator != null) {
            customerGenerator.pauseGeneration();
            Log.d(TAG, "CustomerGenerator paused via DinerView.");
        }
    }

    public void resumeGame() {
        if (customerGenerator != null) {
            customerGenerator.resumeGeneration();
            Log.d(TAG, "CustomerGenerator resumed via DinerView.");
        }
        if (gameThread != null) {
            gameThread.resumeGame();
            Log.d(TAG, "Game resumed via DinerView.");
        }
    }

    public boolean isPaused() {
        if (gameThread != null) {
            return gameThread.isPaused();
        }
        return false; // Default if thread doesn't exist
    }

    public void stopGame() {
        Log.d(TAG, "stopGame called.");
        if (customerGenerator != null) {
            customerGenerator.resumeGeneration();
            customerGenerator.stopGenerating();
            customerGenerator = null;
            Log.d(TAG, "CustomerGeneratorThread signaled to stop.");
        } else {
            Log.d(TAG, "CustomerGeneratorThread was already null.");
        }


        // Stop Game Loop Thread
        if (gameThread != null) {
            boolean retry = true;
            gameThread.setRunning(false); // Signal thread to stop running
            gameThread.resumeGame();      // Ensure it's not stuck in pause wait

            Log.d(TAG, "Attempting to join GameThread...");
            long joinStartTime = System.currentTimeMillis();
            while (retry && (System.currentTimeMillis() - joinStartTime < 1000)) { // Max wait 1 sec
                try {
                    gameThread.join(500); // Wait max 500ms for thread to die
                    retry = !gameThread.isAlive(); // Stop retrying if thread is no longer alive
                    if (!retry) Log.d(TAG, "GameThread joined successfully.");
                } catch (InterruptedException e) {
                    Log.w(TAG, "InterruptedException joining GameThread, retrying...", e);
                    // Re-interrupt the current thread if needed
                    Thread.currentThread().interrupt();
                }
            }
            if (retry) { // If loop finished but retry is still true, join timed out
                Log.e(TAG, "GameThread join timed out!");
            }
            gameThread = null; // Release reference
        } else {
            Log.d(TAG, "GameThread was already null.");
        }
    }



    private void loadBitmaps(Resources res) {
        // --- Load Customer Bitmaps ---
        customerBitmaps.clear(); // Clear previous bitmaps if any
        for (Customer.CustomerType type : Customer.CustomerType.values()) {
            Customer.CustomerConfig config = Customer.getConfig(type);
            int resId = config.getIconResId();
            try {
                Bitmap bitmap = BitmapFactory.decodeResource(res, resId);
                if (bitmap != null) {
                    // Store original bitmap, scaling happens during draw if needed,
                    // or scale here to CUSTOMER_ICON_WIDTH/HEIGHT if preferred
                    customerBitmaps.put(resId, bitmap);
                    Log.d(TAG, "Loaded customer bitmap for " + type + " (ResID: " + resId + ")");
                } else {
                    Log.e(TAG, "Failed to load customer bitmap for " + type + " (ResID: " + resId + ")");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading customer bitmap for " + type + " (ResID: " + resId + ")", e);
            }
        }

        // --- Load Heart Bitmap ---
        try {
            Bitmap originalHeart = BitmapFactory.decodeResource(res, R.drawable.heart_full);
            if (originalHeart != null) {
                // Scale the heart bitmap to the desired size
                heartBitmap = Bitmap.createScaledBitmap(originalHeart, (int) HEART_SIZE, (int) HEART_SIZE, true);
                Log.d(TAG, "Loaded and scaled heart bitmap (ResID: " + R.drawable.heart_full + ")");
            } else {
                Log.e(TAG, "Failed to load heart bitmap (ResID: " + R.drawable.heart_full + ") - decodeResource returned null.");
                heartBitmap = null; // Ensure it's null if loading failed
            }
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Heart bitmap resource not found (R.drawable.heart_full). Make sure the file exists!", e);
            heartBitmap = null;
        } catch (Exception e) {
            Log.e(TAG, "Error loading or scaling heart bitmap (ResID: " + R.drawable.heart_full + ")", e);
            heartBitmap = null;
        }
    }
    public void triggerProcessArrivals() {
        Log.d(TAG,"triggerProcessArrivals called by GameThread"); // Debug log
        if (dinerState != null) {
            dinerState.processCustomerArrivals(); // Delegate to DinerState
        }
    }

    // Called when the drawing surface is ready
    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        Log.d(TAG, "Surface Created - Doing minimal work here.");
        // <<< REMOVED creation of dinerState, customerGenerator, gameThread >>>
        // GameThread will be started in surfaceChanged AFTER state is ready
    }
    // Called when surface dimensions change (e.g., rotation)
    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "Surface Changed: width=" + width + ", height=" + height);
        calculateLayout(width, height); // Calculate layout first

        // Create DinerState *first* if it doesn't exist AND layout is ready
        if (dinerState == null && tableRects != null && counterRect != null) {
            Log.d(TAG, "surfaceChanged: Creating DinerState...");
            dinerState = new DinerState(tableRects.length, counterRect, tableRects, null);
            Log.i(TAG, "DinerState created.");
        }

        // Initialize/Re-initialize tables if state and rects exist
        if (dinerState != null && tableRects != null) {
            Log.d(TAG, "surfaceChanged: Initializing tables in DinerState.");
            dinerState.initializeTables(tableRects);
        } else if(dinerState == null) {
            Log.e(TAG, "surfaceChanged: Cannot initialize tables, DinerState is still null!");
        }

        // Create CustomerGenerator *after* DinerState exists
        // Ensure this only happens once or is handled correctly on recreation
        if (dinerState != null && customerGenerator == null) {
            Log.d(TAG,"Creating CustomerGeneratorThread...");
            customerGenerator = new CustomerGeneratorThread(dinerState.getCustomerArrivalQueue(), dinerState);
            dinerState.setCustomerGenerator(customerGenerator); // Pass reference
            // Consider pausing generator if game shouldn't start immediately?
            customerGenerator.start();
            Log.i(TAG, "CustomerGeneratorThread CREATED and STARTED.");
        }

        // Create/Start GameThread *after* DinerState exists
        if (dinerState != null && (gameThread == null || !gameThread.isAlive())) {
            Log.d(TAG,"Creating/Starting GameThread...");
            gameThread = new GameThread(this.context, getHolder(), this, dinerState);
            // <<< ADD Reset for Game Over Dialog Flag >>>
            gameOverDialogShown = false;
            // <<< END Reset >>>
            gameThread.setRunning(true); // Set running BEFORE starting
            gameThread.start();
            Log.d(TAG, "GameThread started/restarted");
        } else if (dinerState != null && gameThread != null) {
            // If thread existed but was paused, just ensure running flag is true
            gameThread.setRunning(true);
            Log.d(TAG, "GameThread resumed");
        } else if (dinerState == null){
            Log.e(TAG, "surfaceChanged: Cannot start GameThread, DinerState is null!");
        }
    }

    // Called when the drawing surface is destroyed
    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        Log.d(TAG, "Surface Destroyed");

        stopGame();

        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
            soundPoolLoaded = false; // Reset flag
            Log.d(TAG, "SoundPool released.");
        }

        if (heartBitmap != null && !heartBitmap.isRecycled()) {
            heartBitmap.recycle();
            heartBitmap = null;
            Log.d(TAG, "Heart bitmap recycled.");
        }

        // Cleanup customer bitmaps map (optional but good practice)
        if (customerBitmaps != null) {
            for (Bitmap bmp : customerBitmaps.values()) {
                if (bmp != null && !bmp.isRecycled()) {
                    bmp.recycle();
                }
            }
            customerBitmaps.clear();
            Log.d(TAG, "Customer bitmaps recycled and cleared.");
        }

        // Nullify other references if needed
        dinerState = null; // Release state reference

        Log.d(TAG, "surfaceDestroyed finished cleanup.");
    }


    // Method called by GameThread to update game logic
    public void update(double deltaTime) {
        if (dinerState != null) {
            // Convert double deltaTime to float
            float dt = (float)deltaTime;

            // Update main game state
            dinerState.update(dt);
            updateScoreDisplay(dt);
        }
    }


    public void triggerAngryLeaveEffects(int count) {
        Log.d(TAG, "Triggering angry leave effects for " + count + " customer(s).");

        // --- Trigger Vibration ---
        if (vibrator != null) { // Check if vibrator exists and is supported
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Duration 150ms, default intensity
                    vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    // Deprecated in API 26
                    vibrator.vibrate(150);
                }
                Log.d(TAG, "Vibration triggered.");
            } catch (Exception e) {
                // Catch potential exceptions (e.g., SecurityException if permission missing)
                Log.e(TAG, "Error triggering vibration", e);
            }
        }

        // --- Trigger Sound ---
        // Check if pool created, sound loaded, and ID is valid
        if (soundPool != null && soundPoolLoaded && angrySoundId > 0) {
            // Play the sound (Left Volume, Right Volume, Priority, Loop, Rate)
            soundPool.play(angrySoundId, 1.0f, 1.0f, 1, 0, 1.0f);
            Log.d(TAG, "Angry leave sound played (ID: " + angrySoundId + ").");
        } else {
            Log.w(TAG, "Could not play angry leave sound. Pool:" + (soundPool!=null) + " Loaded:" + soundPoolLoaded + " ID:" + angrySoundId);
        }
    }
    private void updateScoreDisplay(float dt) {
        if (dinerState == null) return;

        int actualScore = dinerState.getScore();

        // Calculate the difference between target and current display
        float diff = actualScore - displayedScore;

        // If the difference is negligible, snap to the target and stop animating
        // Using 0.5f threshold: if less than half a point away, just snap it.
        if (Math.abs(diff) < 0.5f) {
            displayedScore = actualScore;
        } else {
            // Move displayedScore towards actualScore
            // The change is proportional to the difference and delta time
            displayedScore += diff * SCORE_ANIMATION_SPEED * dt;
        }
    }

    // Method called by GameThread to draw the current game state
    public void drawGame(Canvas canvas) {
        if (canvas == null || dinerState == null) { return; }

        // 1. Draw Background
        canvas.drawColor(backgroundPaint.getColor());

        if (waitingAreaRect == null || counterRect == null || tableRects == null) {
            // Layout not ready
            canvas.drawText("Calculating layout...", getWidth() / 2f, getHeight() / 2f, textPaint);
            return;
        }

        // 2. Draw Layout Elements (Waiting Area, Counter, Tables)
        canvas.drawRect(waitingAreaRect, waitingAreaPaint);
        canvas.drawText("Waiting Area", waitingAreaRect.left + 10, waitingAreaRect.top + 40, textPaint);
        canvas.drawRect(counterRect, counterPaint);
        canvas.drawText("Kitchen Counter", counterRect.left + 10, counterRect.top + 40, textPaint);
        // 3. Draw Tables (Always draw the table rectangles)
        List<Table> tables = dinerState.getTables(); // Get tables from state
        for (Table table : tables) {
            if (table != null && table.getPositionRect() != null) { // Safety checks
                canvas.drawRect(table.getPositionRect(), tablePaint);
            }
        }

        // Clear previous tap areas before recalculating
        waitingCustomerTapAreas.clear();
        confirmOrderTapAreas.clear();
        foodReadyTapAreas.clear();
        clearTableTapAreas.clear();

        // --- 4. Draw Waiting Customers ---
        List<Customer> waiting = dinerState.getWaitingCustomers();
        float drawX = waitingAreaRect.left + 20;
        float iconPadding = 5f; // Padding above/below elements
        // Calculate spacing needed: Bar + Padding + Icon + Padding + Text Height (approx)
        Paint.FontMetrics fm = customerPaint.getFontMetrics(); // Get font metrics once
        float textHeight = fm.descent - fm.ascent;
        float spacing = PATIENCE_BAR_HEIGHT + iconPadding + CUSTOMER_ICON_HEIGHT + iconPadding + textHeight + 10f; // Total vertical space per customer

        float waitingAreaCenterX = waitingAreaRect.centerX();

        synchronized (waiting) {
            int maxVisibleCustomers = (int) ((waitingAreaRect.height() - 40) / spacing); // Adjust max count based on spacing
            int drawnCount = 0;
            // Start drawing from top padding
            float currentIconTop = waitingAreaRect.top + 40 + iconPadding + PATIENCE_BAR_HEIGHT + iconPadding; // Y coord for the TOP of the FIRST icon

            for (int i = 0; i < waiting.size(); i++) {
                if (drawnCount >= maxVisibleCustomers) break;

                Customer customer = waiting.get(i);
                if (customer == null || customer.getState() == Customer.CustomerState.ANGRY_LEFT) continue;
                if (isDragging && customer == draggedCustomer) continue;

                int iconResId = customer.getCustomerIconResId();
                Bitmap customerBitmap = customerBitmaps.get(iconResId);
                String customerText = customer.getDisplayId(); // <<< Get type-specific ID

                // --- Calculate Positions ---
                float iconLeft = waitingAreaCenterX - CUSTOMER_ICON_WIDTH / 2f;
                float iconTop = currentIconTop;
                RectF destRect = new RectF(iconLeft, iconTop, iconLeft + CUSTOMER_ICON_WIDTH, iconTop + CUSTOMER_ICON_HEIGHT);

                // Bar position (Above icon)
                float barY = destRect.top - iconPadding - PATIENCE_BAR_HEIGHT; // Bar top Y
                float barX = destRect.left; // Align bar left with icon left

                // Text position (Below icon)
                float textDrawX = destRect.centerX(); // Center text horizontally under icon
                float textDrawY = destRect.bottom + iconPadding + textHeight - fm.descent; // Baseline below icon + padding

                // --- Draw Elements ---
                // 1. Draw Patience Bar
                float patiencePercent = customer.getPatiencePercentage();
                updatePatienceBarColor(patiencePercent);
                canvas.drawRect(barX, barY, barX + PATIENCE_BAR_WIDTH, barY + PATIENCE_BAR_HEIGHT, patienceBarBgPaint);
                canvas.drawRect(barX, barY, barX + PATIENCE_BAR_WIDTH * patiencePercent, barY + PATIENCE_BAR_HEIGHT, patienceBarFgPaint);

                // 2. Draw Icon (or fallback)
                if (customerBitmap != null) {
                    canvas.drawBitmap(customerBitmap, null, destRect, bitmapPaint);
                    waitingCustomerTapAreas.add(destRect); // Tap area is the icon
                } else {
                    // Fallback: Draw placeholder text if bitmap is missing
                    customerPaint.setColor(Color.DKGRAY);
                    customerPaint.setTextAlign(Paint.Align.CENTER); // Center fallback text too
                    canvas.drawText("[IMG]", destRect.centerX(), destRect.centerY(), customerPaint);
                    waitingCustomerTapAreas.add(destRect); // Still add tap area
                    customerPaint.setTextAlign(Paint.Align.LEFT); // Reset alignment
                }

                // 3. Draw Text Label (Below icon)
                customerPaint.setColor(Color.DKGRAY); // Ensure text color
                customerPaint.setTextAlign(Paint.Align.CENTER); // Center align text
                canvas.drawText(customerText, textDrawX, textDrawY, customerPaint);
                customerPaint.setTextAlign(Paint.Align.LEFT); // Reset alignment

                // --- Move Y for next customer ---
                currentIconTop += spacing; // Move down by the calculated total spacing
                drawnCount++;
            } // End for loop (waiting)
        }

        // --- 5. Draw Seated Customers and State Indicators/Food --- // <<< MODIFIED SECTION START
        Paint seatedCustomerPaint = customerPaint;
        seatedCustomerPaint.setTextAlign(Paint.Align.CENTER);

        List<Customer> customersWithFoodReady = new ArrayList<>();

        for (Table table : tables) {
            if (table != null && table.isOccupied()) {
                Customer seatedCustomer = table.getSeatedCustomer();
                if (seatedCustomer == null || seatedCustomer.getState() == Customer.CustomerState.ANGRY_LEFT) continue;

                RectF tableRect = table.getPositionRect();
                float tableCenterX = tableRect.centerX(); // Use frequently

                // --- Get Customer Info ---
                String customerText = seatedCustomer.getDisplayId(); // <<< Get type-specific ID
                int iconResId = seatedCustomer.getCustomerIconResId();
                Bitmap customerBitmap = customerBitmaps.get(iconResId);

                // --- Calculate Icon Position --- (e.g., centered horizontally, slightly above vertical center)
                float iconWidth = CUSTOMER_ICON_WIDTH;
                float iconHeight = CUSTOMER_ICON_HEIGHT;
                float iconLeft = tableCenterX - iconWidth / 2f;
                float iconCenterY = tableRect.centerY() - iconHeight * 0.1f; // Move icon center slightly up
                float iconTop = iconCenterY - iconHeight / 2f;
                RectF iconDestRect = new RectF(iconLeft, iconTop, iconLeft + iconWidth, iconTop + iconHeight);

                // --- Calculate Bar Position --- (Above Icon)
                float barX = tableCenterX - PATIENCE_BAR_WIDTH / 2.0f; // Center bar horizontally
                float barY = iconDestRect.top - PATIENCE_BAR_HEIGHT - 5f; // Position above the icon

                // --- Calculate Text Position --- (Below Icon)
                seatedCustomerPaint.getTextBounds(customerText, 0, customerText.length(), textBounds); // Needed for height calc
                float textDrawX_seated = tableCenterX; // Center text horizontally
                float textDrawY_seated = iconDestRect.bottom + textBounds.height() + 5f; // Position below icon


                if (customerBitmap != null) {
                    canvas.drawBitmap(customerBitmap, null, iconDestRect, bitmapPaint);
                } else {
                    // Fallback text if bitmap is missing
                    seatedCustomerPaint.setColor(Color.DKGRAY);
                    canvas.drawText("[IMG]", tableCenterX, tableRect.centerY(), seatedCustomerPaint);
                }
                // -------------------------------------------------------------

                // --- Draw Patience Bar --- (Only if NOT EATING)
                if (seatedCustomer.getState() != Customer.CustomerState.EATING) {
                    float patiencePercent = seatedCustomer.getPatiencePercentage();
                    updatePatienceBarColor(patiencePercent);
                    canvas.drawRect(barX, barY, barX + PATIENCE_BAR_WIDTH, barY + PATIENCE_BAR_HEIGHT, patienceBarBgPaint);
                    canvas.drawRect(barX, barY, barX + PATIENCE_BAR_WIDTH * patiencePercent, barY + PATIENCE_BAR_HEIGHT, patienceBarFgPaint);
                }

                // --- Draw Customer Text Label --- (Below Icon)
                seatedCustomerPaint.setColor(Color.DKGRAY); // Ensure text color
                canvas.drawText(customerText, textDrawX_seated, textDrawY_seated, seatedCustomerPaint);


                // --- Draw State Indicators OR Food on Table ---
                float indicatorPadding = 10f; // Padding above table

                if (seatedCustomer.getState() == Customer.CustomerState.WAITING_ORDER_CONFIRM) {
                    // Draw 'ORDER' indicator (Above Table)
                    float indicatorWidth = 200f;
                    float indicatorHeight = 50f;
                    float indicatorX = tableCenterX - indicatorWidth / 2.0f;
                    float indicatorY = tableRect.top - indicatorPadding - indicatorHeight;
                    // ****************************

                    String indicatorText = "Take ORDER";
                    RectF indicatorRect = new RectF(indicatorX, indicatorY, indicatorX + indicatorWidth, indicatorY + indicatorHeight);
                    confirmOrderTapAreas.add(new Pair<>(indicatorRect, seatedCustomer));
                    canvas.drawRect(indicatorRect, orderIndicatorPaint);
                    orderIndicatorTextPaint.getTextBounds(indicatorText, 0, indicatorText.length(), textBounds);
                    float indicatorTextY = indicatorRect.centerY() + textBounds.height() / 2.0f;
                    canvas.drawText(indicatorText, indicatorRect.centerX(), indicatorTextY, orderIndicatorTextPaint);

                } else if (seatedCustomer.getState() == Customer.CustomerState.FOOD_READY) {
                    // Collect customer for drawing food on counter later
                    customersWithFoodReady.add(seatedCustomer);

                } else if (seatedCustomer.getState() == Customer.CustomerState.EATING) {
                    float plateRadius = FOOD_PLATE_DIAMETER / 2f;
                    float foodRadius = plateRadius * 0.65f;
                    float plateX = iconDestRect.right + plateRadius + 5f;
                    float plateY = iconDestRect.centerY();
                    canvas.drawCircle(plateX, plateY, plateRadius, foodReadyIndicatorPaint);
                    canvas.drawCircle(plateX, plateY, foodRadius, foodItemPaint);
                } else if (seatedCustomer.getState() == Customer.CustomerState.READY_TO_LEAVE) {
                    // Draw "DONE" indicator
                    float indicatorWidth = 200f;
                    float indicatorHeight = 50f;
                    float indicatorX = tableCenterX - indicatorWidth / 2.0f;
                    float indicatorY = tableRect.top - indicatorPadding - indicatorHeight;

                    String indicatorText = "DONE";
                    RectF indicatorRect = new RectF(indicatorX, indicatorY, indicatorX + indicatorWidth, indicatorY + indicatorHeight);
                    clearTableTapAreas.add(new Pair<>(indicatorRect, seatedCustomer)); // Store tap area
                    canvas.drawRect(indicatorRect, clearTableIndicatorPaint); // Use green paint
                    clearTableIndicatorTextPaint.getTextBounds(indicatorText, 0, indicatorText.length(), textBounds);
                    float indicatorTextY = indicatorRect.centerY() + textBounds.height() / 2.0f;
                    canvas.drawText(indicatorText, indicatorRect.centerX(), indicatorTextY, clearTableIndicatorTextPaint); // Use text paint
                }
            }
        }
        // Reset alignment
        seatedCustomerPaint.setTextAlign(Paint.Align.LEFT);
        customerPaint.setTextAlign(Paint.Align.LEFT); // Reset base paint too

        // --- 6. Draw Food Ready Indicators ON THE COUNTER ---
        // ... (Existing loop for drawing food on counter - unchanged) ...
        if (!customersWithFoodReady.isEmpty()) {
            float foodIndicatorSize = FOOD_PLATE_DIAMETER;
            float foodSpacing = 15f;
            float startX = counterRect.left + foodSpacing + foodIndicatorSize / 2f;
            float counterItemY = counterRect.centerY();

            for (int i = 0; i < customersWithFoodReady.size(); i++) {
                Customer customer = customersWithFoodReady.get(i);
                if (customer == null) continue;

                if (isDragging && customer == draggedFoodCustomer) {
                    Log.d(TAG, "Skipping draw food on counter for dragged item: " + customer.getDisplayId());
                    continue;
                }

                float currentX = startX + i * (foodIndicatorSize + foodSpacing);
                RectF indicatorRect = new RectF(
                        currentX - foodIndicatorSize / 2f, counterItemY - foodIndicatorSize / 2f,
                        currentX + foodIndicatorSize / 2f, counterItemY + foodIndicatorSize / 2f
                );

                if (indicatorRect.right > counterRect.right - foodSpacing) {
                    Log.w(TAG, "Counter full, not drawing more food indicators.");
                    break;
                }

                foodReadyTapAreas.add(new Pair<>(indicatorRect, customer));

                float plateRadius_counter = foodIndicatorSize / 2f;
                canvas.drawCircle(currentX, counterItemY, plateRadius_counter, foodReadyIndicatorPaint);
                float foodRadius_counter = plateRadius_counter * 0.65f;
                canvas.drawCircle(currentX, counterItemY, foodRadius_counter, foodItemPaint);

                textPaint.setTextSize(35f);
                textPaint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(customer.getDisplayId(), currentX, counterItemY + foodIndicatorSize / 2f + 20f, textPaint);
                textPaint.setTextSize(35f);
                textPaint.setTextAlign(Paint.Align.LEFT);
            }
        }


        // --- 7. Draw the item currently being DRAGGED --- // Changed numbering
        // ... (Existing logic to draw dragged customer or food - unchanged) ...
        if (isDragging) {
            // Log.d(TAG, "Drawing dragged item at (" + dragX + ", " + dragY + ")");
            if (draggedCustomer != null) {
                // --- Draw Dragged Customer Icon ---
                int iconResId = draggedCustomer.getCustomerIconResId();
                Bitmap customerBitmap = customerBitmaps.get(iconResId);
                if (customerBitmap != null) {
                    float iconWidth = CUSTOMER_ICON_WIDTH;
                    float iconHeight = CUSTOMER_ICON_HEIGHT;
                    float iconLeft = dragX - iconWidth / 2f; // Center horizontally
                    float iconTop = dragY - iconHeight / 2f; // Center vertically
                    RectF destRect = new RectF(iconLeft, iconTop, iconLeft + iconWidth, iconTop + iconHeight);
                    canvas.drawBitmap(customerBitmap, null, destRect, bitmapPaint);
                } else {
                    // Fallback text if bitmap fails
                    selectedCustomerPaint.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText(draggedCustomer.getDisplayId(), dragX, dragY, selectedCustomerPaint);
                    selectedCustomerPaint.setTextAlign(Paint.Align.LEFT);
                }
            } else if (draggedFoodCustomer != null) {
                // --- Draw Dragged Food --- (Existing code is likely fine)
                float draggedFoodSize = FOOD_PLATE_DIAMETER;
                float plateRadius_drag = draggedFoodSize / 2f;
                float foodRadius_drag = plateRadius_drag * 0.65f;
                canvas.drawCircle(dragX, dragY, plateRadius_drag, foodReadyIndicatorPaint);
                canvas.drawCircle(dragX, dragY, foodRadius_drag, foodItemPaint);
                // Optional: Draw customer ID with dragged food
                // ...
            }
        }


        // --- 8. Draw UI Elements (Score, Lives, Level) --- // <<< SECTION UPDATED >>>

        // --- Define UI Positioning Constants ---
        float uiPaddingTop = 50f;    // General top padding for UI block
        float uiPaddingRight = 30f;   // General right padding for UI block
        float uiVerticalSpacing = 5f; // Vertical space between score/hearts/level

        // --- Setup Paint for UI (Align Right for the whole block) ---
        uiTextPaint.setTextAlign(Paint.Align.RIGHT); // <<< CHANGE: Set alignment once for the block
        uiTextPaint.setColor(Color.BLACK);
        uiTextPaint.setTextSize(40f); // Default size for score

        // --- Calculate Score Position & Draw ---
        float scoreX = canvas.getWidth() - uiPaddingRight; // X position is the right edge minus padding
        float scoreY = uiPaddingTop;                       // Y position for the top element (score baseline)
        String scoreText = String.format(Locale.US, "Score: %d", (int) displayedScore);
        // We need score text height to position elements below it accurately
        Paint.FontMetrics scoreFm = uiTextPaint.getFontMetrics();
        float scoreTextHeight = scoreFm.descent - scoreFm.ascent;
        // Draw score using scoreY as the baseline
        canvas.drawText(scoreText, scoreX, scoreY + scoreTextHeight, uiTextPaint); // Added height for baseline

        // --- Calculate Y position for Hearts (below score) ---
        // Start Y position for the top of the heart icons
        float heartsY = scoreY + scoreTextHeight + uiVerticalSpacing;

        // --- Draw Lives (Hearts) ---
        if (dinerState != null && heartBitmap != null) {
            int currentLives = dinerState.getPlayerLives();
            if (currentLives > 0) {
                // Calculate total width needed for the hearts
                float totalHeartWidth = (currentLives * HEART_SIZE) + (Math.max(0, currentLives - 1) * HEART_SPACING);
                // Calculate the starting X for the first heart so the row ends near scoreX
                float startHeartX = scoreX - totalHeartWidth; // <<< CHANGE: Calculate start X based on scoreX

                for (int i = 0; i < currentLives; i++) {
                    // Calculate X position for this specific heart
                    float heartX = startHeartX + i * (HEART_SIZE + HEART_SPACING); // <<< CHANGE: Use calculated startHeartX
                    // Draw the heart bitmap (Y position is heartsY - the top of the icon)
                    canvas.drawBitmap(heartBitmap, heartX, heartsY, bitmapPaint); // <<< CHANGE: Use heartsY
                }
            }
            // Update heartsY to point to the baseline position *below* the drawn hearts for the next element
            heartsY += HEART_SIZE;

        } else if (heartBitmap == null) {
            // Fallback if heart bitmap failed to load
            // Draw fallback text aligned right, below score
            uiTextPaint.setTextSize(35f); // Use a slightly smaller size maybe
            Paint.FontMetrics fallbackFm = uiTextPaint.getFontMetrics();
            float fallbackTextHeight = fallbackFm.descent - fallbackFm.ascent;
            String livesFallbackText = "Lives: " + (dinerState != null ? dinerState.getPlayerLives() : "?");
            canvas.drawText(livesFallbackText, scoreX, heartsY + fallbackTextHeight, uiTextPaint); // Draw using heartsY as baseline
            Log.w(TAG, "Heart bitmap is null, drawing text fallback for lives.");
            // Update heartsY to be below the fallback text
            heartsY += fallbackTextHeight;
        }
        // --------------------------


        // --- Calculate Y position for Level (below hearts) ---
        float levelY = heartsY + uiVerticalSpacing;

        // --- Draw Level ---
        if (dinerState != null) {
            String levelText = String.format(Locale.US, "Level: %d", dinerState.getCurrentLevel());
            uiTextPaint.setColor(Color.DKGRAY);
            uiTextPaint.setTextSize(35f); // Slightly smaller size for level
            // Calculate level text height for baseline positioning
            Paint.FontMetrics levelFm = uiTextPaint.getFontMetrics();
            float levelTextHeight = levelFm.descent - levelFm.ascent;
            // Draw level text aligned right (scoreX), positioned at the calculated levelY baseline
            canvas.drawText(levelText, scoreX, levelY + levelTextHeight, uiTextPaint); // <<< CHANGE: Use scoreX and levelY + height
        }
        // --------------------------

        // Reset paint alignment if needed elsewhere
        uiTextPaint.setTextAlign(Paint.Align.LEFT);

        // <<< START Draw Menu Button >>>
        if (dinerState != null && !dinerState.isGameOver()) { // Only draw if game not over
            // Calculate position (e.g., top left)
            float buttonLeft = MENU_BUTTON_MARGIN;
            float buttonTop = MENU_BUTTON_MARGIN;
            menuButtonArea = new RectF(
                    buttonLeft,
                    buttonTop,
                    buttonLeft + MENU_BUTTON_WIDTH,
                    buttonTop + MENU_BUTTON_HEIGHT
            );

            // Draw button background
            canvas.drawRect(menuButtonArea, menuButtonPaint);

            // Draw "Menu" text
            float textX = menuButtonArea.centerX();
            // Adjust Y for vertical centering of text baseline
            float textY = menuButtonArea.centerY() - ((menuButtonTextPaint.descent() + menuButtonTextPaint.ascent()) / 2f) ;
            canvas.drawText("Menu", textX, textY, menuButtonTextPaint);

        } else {
            menuButtonArea = null; // Ensure no active tap area if button not drawn
        }
        // <<< END Draw Menu Button >>>

        // <<< START Game Over Dialog Trigger >>>
        if (dinerState != null && dinerState.isGameOver() && !gameOverDialogShown && !isPaused()) {
            // Check if game is over, dialog not shown yet, and not already paused by the user menu
            gameOverDialogShown = true; // Set flag: only trigger once per game over
            pauseGame(); // Pause game logic updates

            // Trigger the dialog in GameActivity - MUST run on UI thread
            if (context instanceof Activity) { // Check context is an Activity
                Log.d(TAG, "Game Over detected! Posting task to show dialog.");
                ((Activity) context).runOnUiThread(() -> {
                    // Double check state within UI thread runnable
                    if (context instanceof GameActivity && !((GameActivity) context).isFinishing()) {
                        Log.d(TAG, "Running on UI thread to show Game Over dialog.");
                        ((GameActivity) context).showGameOverDialog(dinerState.getScore());
                    }
                });
            } else {
                Log.e(TAG, "Context is not an Activity, cannot show game over dialog!");
            }
        }
        // <<< END Game Over Dialog Trigger >>>


        // --- Draw Game Over Message --- //
        if (dinerState.isGameOver()) {
            String endMessage = "GAME OVER!";
            Paint endPaint = new Paint();
            endPaint.setTextAlign(Paint.Align.CENTER);
            endPaint.setTextSize(80f);
            endPaint.setFakeBoldText(true);
            endPaint.setColor(Color.RED); // Only need loss color now

            // Draw centered message
            float centerX = canvas.getWidth() / 2f;
            float centerY = canvas.getHeight() / 2f;
            canvas.drawText(endMessage, centerX, centerY, endPaint);

            // Optional: Draw final score again prominently below "GAME OVER"
            String finalScoreText = "Final Score: " + dinerState.getScore();
            endPaint.setTextSize(50f); // Smaller size for score
            canvas.drawText(finalScoreText, centerX, centerY + 80, endPaint); // Offset slightly below
        }
        // ------------------------------------
    }


    private void updatePatienceBarColor(float percentage) {
        if (percentage < 0.3f) patienceBarFgPaint.setColor(Color.RED);
        else if (percentage < 0.6f) patienceBarFgPaint.setColor(Color.YELLOW);
        else patienceBarFgPaint.setColor(Color.GREEN);
    }

    // DinerView.java

    // DinerView.java

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // Check if tap is within the MENU button area (only if it's currently drawn)
            if (menuButtonArea != null && dinerState !=null && !dinerState.isGameOver()) { // Check if button exists and game not over
                float touchX = event.getX();
                float touchY = event.getY();
                if (menuButtonArea.contains(touchX, touchY)) {
                    Log.d(TAG, "Menu button tapped!");
                    // <<< Action: Pause *AND* Show Menu >>>
                    pauseGame(); // Pause the game thread IMMEDIATELY
                    // Trigger the dialog in GameActivity
                    if (context instanceof GameActivity) {
                        // Maybe rename showPauseMenu -> showInGameMenu for clarity? Let's do that.
                        ((GameActivity) context).showInGameMenu();
                    } else {
                        Log.e(TAG, "Context is not GameActivity, cannot show menu!");
                    }
                    return true; // Consume the event, menu button handled it
                }
            }
        }


        // --- If paused or game over, ignore other game interactions ---
        if (dinerState == null || isPaused() || dinerState.isGameOver()) {
            return false; // Block game interactions if paused by menu or game over
        }

        if (dinerState == null || isPaused()) return false;

        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()) {
            // --- ACTION_DOWN: Start a drag OR Handle Taps ---
            case MotionEvent.ACTION_DOWN:
                Log.d(TAG, "ACTION_DOWN at (" + touchX + ", " + touchY + ")");
                boolean handledDownEvent = false;

                // --- Check 1: Tap on WAITING customer (to start drag) ---
                List<Customer> waiting = dinerState.getWaitingCustomers();
                for (int i = 0; i < waitingCustomerTapAreas.size(); i++) {
                    // ... (Bounds check, potentialDragCustomer retrieval, null check) ...
                    RectF tapArea = waitingCustomerTapAreas.get(i);
                    Customer potentialDragCustomer = null;
                    try { potentialDragCustomer = waiting.get(i); } catch (IndexOutOfBoundsException e) { continue; }

                    if (potentialDragCustomer != null && tapArea.contains(touchX, touchY) && potentialDragCustomer.getState() == Customer.CustomerState.WAITING_QUEUE) {
                        isDragging = true;
                        draggedCustomer = potentialDragCustomer;
                        draggedFoodCustomer = null; // Ensure food drag is off
                        dragX = touchX;
                        dragY = touchY;
                        Log.i(TAG, "Started dragging waiting customer: " + draggedCustomer.getDisplayId());
                        handledDownEvent = true;
                        break; // Stop checking waiting list
                    }
                }

                // --- If we didn't start a customer drag, check other taps ---
                if (!isDragging) { // Or !handledDownEvent

                    // --- Check 2: Tap on 'OK' Order Confirmation Button ---
                    for (Pair<RectF, Customer> pair : confirmOrderTapAreas) {
                        RectF area = pair.first;
                        Customer customer = pair.second;
                        if (area != null && customer != null && customer.getState() == Customer.CustomerState.WAITING_ORDER_CONFIRM && area.contains(touchX, touchY)) {
                            Log.d(TAG, "Tap hit CONFIRM ORDER indicator for " + customer.getDisplayId());
                            dinerState.confirmCustomerOrder(customer);
                            handledDownEvent = true;
                            break; // Stop checking OK buttons
                        }
                    }

                    // --- Check 3: Tap on FOOD on Counter (to start drag) --- // <<< MODIFIED SECTION
                    if (!handledDownEvent) { // Only check if OK wasn't tapped
                        for (Pair<RectF, Customer> pair : foodReadyTapAreas) {
                            RectF area = pair.first;
                            Customer foodCustomer = pair.second; // Customer whose food this is

                            // Check tap hit and ensure customer is still expecting food
                            if (area != null && foodCustomer != null && foodCustomer.getState() == Customer.CustomerState.FOOD_READY && area.contains(touchX, touchY)) {
                                // Start dragging this food item!
                                isDragging = true;
                                draggedFoodCustomer = foodCustomer; // <<< Track food customer
                                draggedCustomer = null; // <<< Ensure waiting customer drag is off
                                dragX = touchX;
                                dragY = touchY;
                                Log.i(TAG, "Started dragging FOOD for customer: " + draggedFoodCustomer.getDisplayId());
                                handledDownEvent = true; // Mark as handled (started food drag)
                                break; // Stop checking food items
                            }
                        }
                    }
                    // ------------------------------------------------------
                    if (!handledDownEvent) { // Only check if nothing else handled the tap yet
                        for (Pair<RectF, Customer> pair : clearTableTapAreas) {
                            RectF area = pair.first;
                            Customer customer = pair.second; // Customer ready to leave

                            // Check tap hit and customer state
                            if (area != null && customer != null && customer.getState() == Customer.CustomerState.READY_TO_LEAVE && area.contains(touchX, touchY)) {
                                Log.d(TAG, "Tap hit DONE/Clear Table indicator for " + customer.getDisplayId());
                                // Call the new method in DinerState
                                dinerState.clearTableForCustomer(customer);
                                handledDownEvent = true; // Mark as handled
                                break; // Stop checking clear table buttons
                            }
                        }
                    }

                } // End if(!isDragging)

                // Return true if we handled the event (started drag OR tapped OK OR started food drag)
                return handledDownEvent;

            // --- ACTION_MOVE: Update drag position ---
            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    dragX = touchX;
                    dragY = touchY;
                    return true; // Consume the event
                }
                break; // If not dragging, let system handle it

            // --- ACTION_UP: End the drag ---
            case MotionEvent.ACTION_UP:
                Log.d(TAG, "ACTION_UP at (" + touchX + ", " + touchY + ")");
                if (isDragging) {
                    // --- Handle Drop Logic ---

                    // A. If we were dragging a CUSTOMER...
                    if (draggedCustomer != null) {
                        Log.d(TAG, "Dropped customer " + draggedCustomer.getDisplayId());
                        boolean seated = false;
                        List<Table> tables = dinerState.getTables();
                        for (Table table : tables) {
                            if (!table.isOccupied() && table.getPositionRect().contains(touchX, touchY)) {
                                Log.d(TAG, "Attempting to seat customer " + draggedCustomer.getDisplayId() + " at table " + table.id);
                                seated = dinerState.trySeatCustomerByDrag(draggedCustomer, table);
                                break;
                            }
                        }
                        if (!seated) {
                            Log.d(TAG, "Customer drop missed valid empty table.");
                        }
                    }
                    // B. If we were dragging FOOD... // <<< MODIFIED SECTION
                    else if (draggedFoodCustomer != null) {
                        Log.d(TAG, "Dropped FOOD for " + draggedFoodCustomer.getDisplayId());
                        boolean delivered = false;
                        List<Table> tables = dinerState.getTables();
                        for (Table table : tables) {
                            // Check if dropped onto an OCCUPIED table...
                            if (table.isOccupied() && table.getPositionRect().contains(touchX, touchY)) {
                                Customer customerAtTable = table.getSeatedCustomer();
                                // ... that belongs to the correct customer who is ready for food
                                if (customerAtTable == draggedFoodCustomer && draggedFoodCustomer.getState() == Customer.CustomerState.FOOD_READY) {
                                    Log.d(TAG, "Attempting to deliver food to customer " + draggedFoodCustomer.getDisplayId() + " at table " + table.id);
                                    // Call the method in DinerState
                                    delivered = dinerState.deliverFood(draggedFoodCustomer, table);
                                    break; // Stop checking tables
                                } else {
                                    // Log details if it's the wrong table/customer/state
                                    Log.d(TAG, "Food drop hit occupied table " + table.id + " but customer mismatch ("
                                            + (customerAtTable != null ? customerAtTable.getDisplayId() : "NONE") + " != " + draggedFoodCustomer.getDisplayId()
                                            + ") or wrong state (" + (customerAtTable != null ? customerAtTable.getState() : "N/A") + ")");
                                }
                            }
                        }
                        if (!delivered) {
                            Log.d(TAG, "Food drop missed valid target table/customer.");
                            // Food just snaps back visually
                        }
                    }
                    // ------------------------------------------------------

                    // --- Reset Drag State --- // <<< MODIFIED SECTION
                    isDragging = false;
                    draggedCustomer = null;
                    draggedFoodCustomer = null; // <<< Reset food drag too
                    Log.d(TAG, "Drag ended.");
                    return true; // Consume the event (we were dragging)
                    // ------------------------------------------------------
                }
                break; // If not dragging, let system handle it
        } // End Switch

        // Allow system to handle other actions if not consumed
        return super.onTouchEvent(event);
    }

}