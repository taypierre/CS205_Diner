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
    private DinerState dinerState;
    private Paint backgroundPaint;
    private Paint tablePaint;

    private CustomerGeneratorThread customerGenerator;
    private Paint customerPaint;
    private Paint selectedCustomerPaint;
    private Paint waitingAreaPaint;
    private Paint counterPaint;
    private Paint textPaint;
    private Paint uiTextPaint;

    private Paint patienceBarBgPaint;
    private Paint patienceBarFgPaint;

    private Paint orderIndicatorPaint;
    private Paint orderIndicatorTextPaint;

    private Paint foodReadyIndicatorPaint;
    private Paint foodItemPaint;


    private Paint clearTableIndicatorPaint;
    private Paint clearTableIndicatorTextPaint;

    private RectF waitingAreaRect;
    private RectF counterRect;
    private RectF[] tableRects;


    private Paint bitmapPaint;

    private Map<Integer, Bitmap> customerBitmaps = new HashMap<>();

    private Bitmap heartBitmap;
    private static final float HEART_SIZE = 50f;
    private static final float HEART_SPACING = 10f;
    private static final float HEART_MARGIN_TOP = 50f;
    private static final float HEART_MARGIN_LEFT = 30f;

    private static final float CUSTOMER_ICON_WIDTH = 80f;
    private static final float CUSTOMER_ICON_HEIGHT = 100f;

    private boolean isDragging = false;
    private Customer draggedCustomer = null;
    private Customer draggedFoodCustomer = null;
    private float dragX = 0f;
    private float dragY = 0f;

    private static final float PATIENCE_BAR_WIDTH = 60f;
    private static final float PATIENCE_BAR_HEIGHT = 8f;
    private static final float PATIENCE_BAR_Y_OFFSET = 5f;

    private List<RectF> waitingCustomerTapAreas = new ArrayList<>();

    // Tap Areas for Order Indicators
    private List<Pair<RectF, Customer>> confirmOrderTapAreas = new ArrayList<>();

    private List<Pair<RectF, Customer>> foodReadyTapAreas = new ArrayList<>();

    private List<Pair<RectF, Customer>> clearTableTapAreas = new ArrayList<>();
    private Rect textBounds = new Rect();

    // Score Animation Fields
    private float displayedScore = 0f;
    private static final float SCORE_ANIMATION_SPEED = 4.0f;



    // Sound and Vibration Fields
    private SoundPool soundPool;
    private int angrySoundId = -1;
    private boolean soundPoolLoaded = false;
    private Vibrator vibrator;


    // Menu Button Fields
    private RectF menuButtonArea;
    private Paint menuButtonPaint;
    private Paint menuButtonTextPaint;
    private static final float MENU_BUTTON_WIDTH = 120f;
    private static final float MENU_BUTTON_HEIGHT = 70f;
    private static final float MENU_BUTTON_MARGIN = 30f;

    private boolean gameOverDialogShown = false;


    private static final float FOOD_PLATE_DIAMETER = 60f;


    public DinerView(Context context, @Nullable  AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
        getHolder().addCallback(this);


        backgroundPaint = new Paint();
        backgroundPaint.setColor(Color.rgb(255, 167, 110));

        tablePaint = new Paint();
        tablePaint.setColor(Color.rgb(139, 69, 19));
        tablePaint.setStyle(Paint.Style.FILL);

        customerPaint = new Paint();
        customerPaint.setColor(Color.BLACK);
        customerPaint.setTextSize(50f);
        customerPaint.setAntiAlias(true);



        orderIndicatorPaint = new Paint();
        orderIndicatorPaint.setColor(Color.BLUE);
        orderIndicatorPaint.setStyle(Paint.Style.FILL);

        orderIndicatorTextPaint = new Paint();
        orderIndicatorTextPaint.setColor(Color.WHITE);
        orderIndicatorTextPaint.setTextSize(25f);
        orderIndicatorTextPaint.setTextAlign(Paint.Align.CENTER);
        orderIndicatorTextPaint.setAntiAlias(true);


        foodReadyIndicatorPaint = new Paint();
        foodReadyIndicatorPaint.setColor(Color.LTGRAY);
        foodReadyIndicatorPaint.setStyle(Paint.Style.FILL);
        foodReadyIndicatorPaint.setAntiAlias(true);

        foodItemPaint = new Paint();
        foodItemPaint.setColor(Color.rgb(255, 255, 255));
        foodItemPaint.setStyle(Paint.Style.FILL);
        foodItemPaint.setAntiAlias(true);


        selectedCustomerPaint = new Paint();
        selectedCustomerPaint.setColor(Color.RED);
        selectedCustomerPaint.setTextSize(35f);
        selectedCustomerPaint.setFakeBoldText(true);
        selectedCustomerPaint.setAntiAlias(true);


        clearTableIndicatorPaint = new Paint();
        clearTableIndicatorPaint.setColor(Color.rgb(34, 139, 34));
        clearTableIndicatorPaint.setStyle(Paint.Style.FILL);
        clearTableIndicatorPaint.setAntiAlias(true);

        clearTableIndicatorTextPaint = new Paint();
        clearTableIndicatorTextPaint.setColor(Color.WHITE);
        clearTableIndicatorTextPaint.setTextSize(25f);
        clearTableIndicatorTextPaint.setTextAlign(Paint.Align.CENTER);
        clearTableIndicatorTextPaint.setFakeBoldText(true);
        clearTableIndicatorTextPaint.setAntiAlias(true);

        waitingAreaPaint = new Paint();
        waitingAreaPaint.setColor(Color.LTGRAY);
        waitingAreaPaint.setStyle(Paint.Style.FILL);

        counterPaint = new Paint();
        counterPaint.setColor(Color.GRAY);
        counterPaint.setStyle(Paint.Style.FILL);

        textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(35f);
        textPaint.setAntiAlias(true);

        uiTextPaint = new Paint();
        uiTextPaint.setColor(Color.BLACK);
        uiTextPaint.setTextSize(40f);
        uiTextPaint.setAntiAlias(true);
        uiTextPaint.setTextAlign(Paint.Align.RIGHT);



        patienceBarBgPaint = new Paint();
        patienceBarBgPaint.setColor(Color.rgb(200, 200, 200));
        patienceBarBgPaint.setStyle(Paint.Style.FILL);

        patienceBarFgPaint = new Paint();
        patienceBarFgPaint.setColor(Color.GREEN);
        patienceBarFgPaint.setStyle(Paint.Style.FILL);


        bitmapPaint = new Paint();
        bitmapPaint.setAntiAlias(true);
        bitmapPaint.setFilterBitmap(true);


        menuButtonPaint = new Paint();
        menuButtonPaint.setColor(Color.argb(180, 100, 100, 100));
        menuButtonPaint.setStyle(Paint.Style.FILL);

        menuButtonTextPaint = new Paint();
        menuButtonTextPaint.setColor(Color.WHITE);
        menuButtonTextPaint.setTextSize(30f);
        menuButtonTextPaint.setTextAlign(Paint.Align.CENTER);
        menuButtonTextPaint.setAntiAlias(true);


        initializeSoundAndVibration(context);


        loadBitmaps(context.getResources());


        displayedScore = 0f;

        setFocusable(true);
        Log.d(TAG, "DinerView constructed");
    }


    private void initializeSoundAndVibration(Context context) {
        // SoundPool Setup
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(2)
                .setAudioAttributes(audioAttributes)
                .build();

        // Listener to know when sounds are loaded
        soundPool.setOnLoadCompleteListener((pool, sampleId, status) -> {
            if (status == 0) {
                soundPoolLoaded = true;
                Log.d(TAG, "Sound ID " + sampleId + " loaded successfully.");
            } else {
                // Failed to load
                Log.e(TAG, "Failed to load sound ID " + sampleId + ", status: " + status);
            }
        });

        // Load the sound
        try {
            angrySoundId = soundPool.load(context, R.raw.angry, 1);
            if (angrySoundId == 0) {
                Log.e(TAG, "Error loading sound R.raw.angry: SoundPool.load returned 0. Check file presence and format.");
            } else {
                Log.d(TAG, "Sound R.raw.angry queued for loading with ID: " + angrySoundId);
            }
        } catch (android.content.res.Resources.NotFoundException e) {
            Log.e(TAG, "Sound file R.raw.angry not found! Make sure it's in res/raw/", e);
            angrySoundId = -1;
        }

        // Vibrator Setup
        vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) {
            Log.w(TAG, "Device does not have a vibrator or service not available.");
            vibrator = null;
        } else {
            Log.d(TAG, "Vibrator service obtained.");
        }
    }

    private void calculateLayout(int width, int height) {
        Log.d(TAG, "Calculating layout for width=" + width + ", height=" + height);

        //  Waiting Area
        float waitingAreaWidth = width / 5.0f;
        float margin = 20f;
        waitingAreaRect = new RectF(margin, margin, margin + waitingAreaWidth, height - margin);

        //  Counter
        float newCounterTop = height * 0.70f;     // Start counter 70% down the screen
        float newCounterBottom = height * 0.95f; // End counter 85% down the screen

        float counterLeft = waitingAreaRect.right + margin; // Start after waiting area
        float counterRight = width - margin;
        counterRect = new RectF(counterLeft, newCounterTop, counterRight, newCounterBottom);

        //  Tables
        int numTables = 3;
        tableRects = new RectF[numTables];
        float tableAreaLeft = waitingAreaRect.right + margin;
        float tableAreaRight = width - margin;
        float tableAreaTop = margin;

        float tableAreaBottom = newCounterTop - margin;
        float tableAreaWidth = tableAreaRight - tableAreaLeft;
        float tableAreaHeight = tableAreaBottom - tableAreaTop;

        float tableSize = Math.min(tableAreaWidth / (numTables + 1), tableAreaHeight / 2.5f);

        float tableSpacingX = (tableAreaWidth - (numTables * tableSize)) / (numTables + 1);

        for (int i = 0; i < numTables; i++) {
            float tableLeft = tableAreaLeft + (i + 1) * tableSpacingX + i * tableSize;
            float tableTop = tableAreaTop + (tableAreaHeight - tableSize) / 2.0f;
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
        return false;
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
                    Thread.currentThread().interrupt();
                }
            }
            if (retry) { // If loop finished but retry is still true, join timed out
                Log.e(TAG, "GameThread join timed out!");
            }
            gameThread = null;
        } else {
            Log.d(TAG, "GameThread was already null.");
        }
    }



    private void loadBitmaps(Resources res) {
        //  Load Customer Bitmaps
        customerBitmaps.clear();
        for (Customer.CustomerType type : Customer.CustomerType.values()) {
            Customer.CustomerConfig config = Customer.getConfig(type);
            int resId = config.getIconResId();
            try {
                Bitmap bitmap = BitmapFactory.decodeResource(res, resId);
                if (bitmap != null) {
                    customerBitmaps.put(resId, bitmap);
                    Log.d(TAG, "Loaded customer bitmap for " + type + " (ResID: " + resId + ")");
                } else {
                    Log.e(TAG, "Failed to load customer bitmap for " + type + " (ResID: " + resId + ")");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading customer bitmap for " + type + " (ResID: " + resId + ")", e);
            }
        }

        // Load Heart Bitmap
        try {
            Bitmap originalHeart = BitmapFactory.decodeResource(res, R.drawable.heart_full);
            if (originalHeart != null) {

                heartBitmap = Bitmap.createScaledBitmap(originalHeart, (int) HEART_SIZE, (int) HEART_SIZE, true);
                Log.d(TAG, "Loaded and scaled heart bitmap (ResID: " + R.drawable.heart_full + ")");
            } else {
                Log.e(TAG, "Failed to load heart bitmap (ResID: " + R.drawable.heart_full + ") - decodeResource returned null.");
                heartBitmap = null;
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
        Log.d(TAG,"triggerProcessArrivals called by GameThread");
        if (dinerState != null) {
            dinerState.processCustomerArrivals();
        }
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        Log.d(TAG, "Surface Created - Doing minimal work here.");
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {
        Log.d(TAG, "Surface Changed: width=" + width + ", height=" + height);
        calculateLayout(width, height);

        // Create DinerState if it doesn't exist AND layout is ready
        if (dinerState == null && tableRects != null && counterRect != null) {
            Log.d(TAG, "surfaceChanged: Creating DinerState...");
            dinerState = new DinerState(tableRects.length, counterRect, tableRects, null);
            Log.i(TAG, "DinerState created.");
        }

        if (dinerState != null && tableRects != null) {
            Log.d(TAG, "surfaceChanged: Initializing tables in DinerState.");
            dinerState.initializeTables(tableRects);
        } else if(dinerState == null) {
            Log.e(TAG, "surfaceChanged: Cannot initialize tables, DinerState is still null!");
        }

        // Create CustomerGenerator after DinerState
        if (dinerState != null && customerGenerator == null) {
            Log.d(TAG,"Creating CustomerGeneratorThread...");
            customerGenerator = new CustomerGeneratorThread(dinerState.getCustomerArrivalQueue(), dinerState);
            dinerState.setCustomerGenerator(customerGenerator);
            customerGenerator.start();
            Log.i(TAG, "CustomerGeneratorThread CREATED and STARTED.");
        }

        // Create/Start GameThread after DinerState
        if (dinerState != null && (gameThread == null || !gameThread.isAlive())) {
            Log.d(TAG,"Creating/Starting GameThread...");
            gameThread = new GameThread(this.context, getHolder(), this, dinerState);
            gameOverDialogShown = false;
            gameThread.setRunning(true);
            gameThread.start();
            Log.d(TAG, "GameThread started/restarted");
        } else if (dinerState != null && gameThread != null) {
            // If thread existed but was paused, ensure running flag is true
            gameThread.setRunning(true);
            Log.d(TAG, "GameThread resumed");
        } else if (dinerState == null){
            Log.e(TAG, "surfaceChanged: Cannot start GameThread, DinerState is null!");
        }
    }

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        Log.d(TAG, "Surface Destroyed");

        stopGame();

        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
            soundPoolLoaded = false;
            Log.d(TAG, "SoundPool released.");
        }

        if (heartBitmap != null && !heartBitmap.isRecycled()) {
            heartBitmap.recycle();
            heartBitmap = null;
            Log.d(TAG, "Heart bitmap recycled.");
        }

        if (customerBitmaps != null) {
            for (Bitmap bmp : customerBitmaps.values()) {
                if (bmp != null && !bmp.isRecycled()) {
                    bmp.recycle();
                }
            }
            customerBitmaps.clear();
            Log.d(TAG, "Customer bitmaps recycled and cleared.");
        }

        dinerState = null;

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

        //  Trigger Vibration
        if (vibrator != null) { // Check if vibrator exists and is supported
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Duration 150ms, default intensity
                    vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));
                } else {
                    vibrator.vibrate(150);
                }
                Log.d(TAG, "Vibration triggered.");
            } catch (Exception e) {
                Log.e(TAG, "Error triggering vibration", e);
            }
        }

        //  Trigger Sound
        // Check if pool created, sound loaded, and ID is valid
        if (soundPool != null && soundPoolLoaded && angrySoundId > 0) {
            soundPool.play(angrySoundId, 1.0f, 1.0f, 1, 0, 1.0f);
            Log.d(TAG, "Angry leave sound played (ID: " + angrySoundId + ").");
        } else {
            Log.w(TAG, "Could not play angry leave sound. Pool:" + (soundPool!=null) + " Loaded:" + soundPoolLoaded + " ID:" + angrySoundId);
        }
    }
    private void updateScoreDisplay(float dt) {
        if (dinerState == null) return;

        int actualScore = dinerState.getScore();

        float diff = actualScore - displayedScore;

        if (Math.abs(diff) < 0.5f) {
            displayedScore = actualScore;
        } else {
            displayedScore += diff * SCORE_ANIMATION_SPEED * dt;
        }
    }

    public void drawGame(Canvas canvas) {
        if (canvas == null || dinerState == null) { return; }

        // Draw Background
        canvas.drawColor(backgroundPaint.getColor());

        if (waitingAreaRect == null || counterRect == null || tableRects == null) {
            // Layout not ready
            canvas.drawText("Calculating layout...", getWidth() / 2f, getHeight() / 2f, textPaint);
            return;
        }

        // Draw Layout Elements (Waiting Area, Counter, Tables)
        canvas.drawRect(waitingAreaRect, waitingAreaPaint);
        canvas.drawText("Waiting Area", waitingAreaRect.left + 10, waitingAreaRect.top + 40, textPaint);
        canvas.drawRect(counterRect, counterPaint);
        canvas.drawText("Kitchen Counter", counterRect.left + 10, counterRect.top + 40, textPaint);
        // Draw Tables
        List<Table> tables = dinerState.getTables();
        for (Table table : tables) {
            if (table != null && table.getPositionRect() != null) {
                canvas.drawRect(table.getPositionRect(), tablePaint);
            }
        }

        // Clear previous tap areas before recalculating
        waitingCustomerTapAreas.clear();
        confirmOrderTapAreas.clear();
        foodReadyTapAreas.clear();
        clearTableTapAreas.clear();

        // Draw Waiting Customers
        List<Customer> waiting = dinerState.getWaitingCustomers();
        float drawX = waitingAreaRect.left + 20;
        float iconPadding = 5f;

        Paint.FontMetrics fm = customerPaint.getFontMetrics();
        float textHeight = fm.descent - fm.ascent;
        float spacing = PATIENCE_BAR_HEIGHT + iconPadding + CUSTOMER_ICON_HEIGHT + iconPadding + textHeight + 10f;

        float waitingAreaCenterX = waitingAreaRect.centerX();

        synchronized (waiting) {
            int maxVisibleCustomers = (int) ((waitingAreaRect.height() - 40) / spacing); // Adjust max count based on spacing
            int drawnCount = 0;

            float currentIconTop = waitingAreaRect.top + 40 + iconPadding + PATIENCE_BAR_HEIGHT + iconPadding;

            for (int i = 0; i < waiting.size(); i++) {
                if (drawnCount >= maxVisibleCustomers) break;

                Customer customer = waiting.get(i);
                if (customer == null || customer.getState() == Customer.CustomerState.ANGRY_LEFT) continue;
                if (isDragging && customer == draggedCustomer) continue;

                int iconResId = customer.getCustomerIconResId();
                Bitmap customerBitmap = customerBitmaps.get(iconResId);
                String customerText = customer.getDisplayId();

                //  Calculate Positions
                float iconLeft = waitingAreaCenterX - CUSTOMER_ICON_WIDTH / 2f;
                float iconTop = currentIconTop;
                RectF destRect = new RectF(iconLeft, iconTop, iconLeft + CUSTOMER_ICON_WIDTH, iconTop + CUSTOMER_ICON_HEIGHT);

                // Bar position (Above icon)
                float barY = destRect.top - iconPadding - PATIENCE_BAR_HEIGHT;
                float barX = destRect.left;

                // Text position (Below icon)
                float textDrawX = destRect.centerX();
                float textDrawY = destRect.bottom + iconPadding + textHeight - fm.descent;

                // Draw Patience Bar
                float patiencePercent = customer.getPatiencePercentage();
                updatePatienceBarColor(patiencePercent);
                canvas.drawRect(barX, barY, barX + PATIENCE_BAR_WIDTH, barY + PATIENCE_BAR_HEIGHT, patienceBarBgPaint);
                canvas.drawRect(barX, barY, barX + PATIENCE_BAR_WIDTH * patiencePercent, barY + PATIENCE_BAR_HEIGHT, patienceBarFgPaint);

                // Draw Icon
                if (customerBitmap != null) {
                    canvas.drawBitmap(customerBitmap, null, destRect, bitmapPaint);
                    waitingCustomerTapAreas.add(destRect);
                } else {

                    customerPaint.setColor(Color.DKGRAY);
                    customerPaint.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText("[IMG]", destRect.centerX(), destRect.centerY(), customerPaint);
                    waitingCustomerTapAreas.add(destRect);
                    customerPaint.setTextAlign(Paint.Align.LEFT);
                }

                // Draw Text Label
                customerPaint.setColor(Color.DKGRAY);
                customerPaint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(customerText, textDrawX, textDrawY, customerPaint);
                customerPaint.setTextAlign(Paint.Align.LEFT);

                // Add padding
                currentIconTop += spacing;
                drawnCount++;
            }
        }

        // Draw Seated Customers and State Indicators/Food
        Paint seatedCustomerPaint = customerPaint;
        seatedCustomerPaint.setTextAlign(Paint.Align.CENTER);

        List<Customer> customersWithFoodReady = new ArrayList<>();

        for (Table table : tables) {
            if (table != null && table.isOccupied()) {
                Customer seatedCustomer = table.getSeatedCustomer();
                if (seatedCustomer == null || seatedCustomer.getState() == Customer.CustomerState.ANGRY_LEFT) continue;

                RectF tableRect = table.getPositionRect();
                float tableCenterX = tableRect.centerX();

                // Get Customer Info
                String customerText = seatedCustomer.getDisplayId();
                int iconResId = seatedCustomer.getCustomerIconResId();
                Bitmap customerBitmap = customerBitmaps.get(iconResId);

                // Calculate Icon Position
                float iconWidth = CUSTOMER_ICON_WIDTH;
                float iconHeight = CUSTOMER_ICON_HEIGHT;
                float iconLeft = tableCenterX - iconWidth / 2f;
                float iconCenterY = tableRect.centerY() - iconHeight * 0.1f;
                float iconTop = iconCenterY - iconHeight / 2f;
                RectF iconDestRect = new RectF(iconLeft, iconTop, iconLeft + iconWidth, iconTop + iconHeight);

                // Calculate Bar Position
                float barX = tableCenterX - PATIENCE_BAR_WIDTH / 2.0f;
                float barY = iconDestRect.top - PATIENCE_BAR_HEIGHT - 5f;

                // Calculate Text Position
                seatedCustomerPaint.getTextBounds(customerText, 0, customerText.length(), textBounds);
                float textDrawX_seated = tableCenterX;
                float textDrawY_seated = iconDestRect.bottom + textBounds.height() + 5f;


                if (customerBitmap != null) {
                    canvas.drawBitmap(customerBitmap, null, iconDestRect, bitmapPaint);
                } else {
                    seatedCustomerPaint.setColor(Color.DKGRAY);
                    canvas.drawText("[IMG]", tableCenterX, tableRect.centerY(), seatedCustomerPaint);
                }

                // Draw Patience Bar
                if (seatedCustomer.getState() != Customer.CustomerState.EATING) {
                    float patiencePercent = seatedCustomer.getPatiencePercentage();
                    updatePatienceBarColor(patiencePercent);
                    canvas.drawRect(barX, barY, barX + PATIENCE_BAR_WIDTH, barY + PATIENCE_BAR_HEIGHT, patienceBarBgPaint);
                    canvas.drawRect(barX, barY, barX + PATIENCE_BAR_WIDTH * patiencePercent, barY + PATIENCE_BAR_HEIGHT, patienceBarFgPaint);
                }

                //  Draw Customer Text Label
                seatedCustomerPaint.setColor(Color.DKGRAY);
                canvas.drawText(customerText, textDrawX_seated, textDrawY_seated, seatedCustomerPaint);


                // Draw State Indicators OR Food on Table
                float indicatorPadding = 10f;

                if (seatedCustomer.getState() == Customer.CustomerState.WAITING_ORDER_CONFIRM) {
                    // Draw 'ORDER' indicator (Above Table)
                    float indicatorWidth = 200f;
                    float indicatorHeight = 50f;
                    float indicatorX = tableCenterX - indicatorWidth / 2.0f;
                    float indicatorY = tableRect.top - indicatorPadding - indicatorHeight;


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
                    clearTableTapAreas.add(new Pair<>(indicatorRect, seatedCustomer));
                    canvas.drawRect(indicatorRect, clearTableIndicatorPaint);
                    clearTableIndicatorTextPaint.getTextBounds(indicatorText, 0, indicatorText.length(), textBounds);
                    float indicatorTextY = indicatorRect.centerY() + textBounds.height() / 2.0f;
                    canvas.drawText(indicatorText, indicatorRect.centerX(), indicatorTextY, clearTableIndicatorTextPaint); // Use text paint
                }
            }
        }
        // Reset alignment
        seatedCustomerPaint.setTextAlign(Paint.Align.LEFT);
        customerPaint.setTextAlign(Paint.Align.LEFT);

        // Draw Food Ready Indicators ON THE COUNTER
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


        //  Draw the item currently being DRAGGED
        if (isDragging) {

            if (draggedCustomer != null) {
                //  Draw Dragged Customer Icon
                int iconResId = draggedCustomer.getCustomerIconResId();
                Bitmap customerBitmap = customerBitmaps.get(iconResId);
                if (customerBitmap != null) {
                    float iconWidth = CUSTOMER_ICON_WIDTH;
                    float iconHeight = CUSTOMER_ICON_HEIGHT;
                    float iconLeft = dragX - iconWidth / 2f;
                    float iconTop = dragY - iconHeight / 2f;
                    RectF destRect = new RectF(iconLeft, iconTop, iconLeft + iconWidth, iconTop + iconHeight);
                    canvas.drawBitmap(customerBitmap, null, destRect, bitmapPaint);
                } else {

                    selectedCustomerPaint.setTextAlign(Paint.Align.CENTER);
                    canvas.drawText(draggedCustomer.getDisplayId(), dragX, dragY, selectedCustomerPaint);
                    selectedCustomerPaint.setTextAlign(Paint.Align.LEFT);
                }
            } else if (draggedFoodCustomer != null) {

                float draggedFoodSize = FOOD_PLATE_DIAMETER;
                float plateRadius_drag = draggedFoodSize / 2f;
                float foodRadius_drag = plateRadius_drag * 0.65f;
                canvas.drawCircle(dragX, dragY, plateRadius_drag, foodReadyIndicatorPaint);
                canvas.drawCircle(dragX, dragY, foodRadius_drag, foodItemPaint);

            }
        }



        // UI Positioning Constants
        float uiPaddingTop = 120f;
        float uiPaddingRight = 30f;
        float uiVerticalSpacing = 5f;

        // Setup Paint for UI
        uiTextPaint.setTextAlign(Paint.Align.RIGHT);
        uiTextPaint.setColor(Color.BLACK);
        uiTextPaint.setTextSize(40f);

        // Calculate Score Position & Draw
        float scoreX = canvas.getWidth() - uiPaddingRight;
        float scoreY = uiPaddingTop;
        String scoreText = String.format(Locale.US, "Score: %d", (int) displayedScore);

        Paint.FontMetrics scoreFm = uiTextPaint.getFontMetrics();
        float scoreTextHeight = scoreFm.descent - scoreFm.ascent;

        canvas.drawText(scoreText, scoreX, scoreY + scoreTextHeight, uiTextPaint);


        float heartsY = scoreY + scoreTextHeight + uiVerticalSpacing;

        // Draw Lives
        if (dinerState != null && heartBitmap != null) {
            int currentLives = dinerState.getPlayerLives();
            if (currentLives > 0) {
                // Calculate total width needed for the hearts
                float totalHeartWidth = (currentLives * HEART_SIZE) + (Math.max(0, currentLives - 1) * HEART_SPACING);

                float startHeartX = scoreX - totalHeartWidth;

                for (int i = 0; i < currentLives; i++) {
                    float heartX = startHeartX + i * (HEART_SIZE + HEART_SPACING);
                    canvas.drawBitmap(heartBitmap, heartX, heartsY, bitmapPaint);
                }
            }

            heartsY += HEART_SIZE;

        } else if (heartBitmap == null) {
            uiTextPaint.setTextSize(35f);
            Paint.FontMetrics fallbackFm = uiTextPaint.getFontMetrics();
            float fallbackTextHeight = fallbackFm.descent - fallbackFm.ascent;
            String livesFallbackText = "Lives: " + (dinerState != null ? dinerState.getPlayerLives() : "?");
            canvas.drawText(livesFallbackText, scoreX, heartsY + fallbackTextHeight, uiTextPaint);
            Log.w(TAG, "Heart bitmap is null, drawing text fallback for lives.");
            heartsY += fallbackTextHeight;
        }



        //  Calculate Y position for Level (below hearts)
        float levelY = heartsY + uiVerticalSpacing;

        //  Draw Level
        if (dinerState != null) {
            String levelText = String.format(Locale.US, "Level: %d", dinerState.getCurrentLevel());
            uiTextPaint.setColor(Color.DKGRAY);
            uiTextPaint.setTextSize(35f);

            Paint.FontMetrics levelFm = uiTextPaint.getFontMetrics();
            float levelTextHeight = levelFm.descent - levelFm.ascent;

            canvas.drawText(levelText, scoreX, levelY + levelTextHeight, uiTextPaint);
        }


        uiTextPaint.setTextAlign(Paint.Align.LEFT);

        // Draw Menu Button
        if (dinerState != null && !dinerState.isGameOver()) {
            float buttonLeft = canvas.getWidth() - MENU_BUTTON_WIDTH - MENU_BUTTON_MARGIN;
            float buttonTop = MENU_BUTTON_MARGIN;

            menuButtonArea = new RectF(
                    buttonLeft,
                    buttonTop,
                    buttonLeft + MENU_BUTTON_WIDTH,
                    buttonTop + MENU_BUTTON_HEIGHT
            );

            canvas.drawRect(menuButtonArea, menuButtonPaint);

            float textX = menuButtonArea.centerX();
            float textY = menuButtonArea.centerY() - ((menuButtonTextPaint.descent() + menuButtonTextPaint.ascent()) / 2f);
            canvas.drawText("Menu", textX, textY, menuButtonTextPaint);

        } else {
            menuButtonArea = null;
        }

        // Game Over Dialog Trigger
        if (dinerState != null && dinerState.isGameOver() && !gameOverDialogShown && !isPaused()) {
            gameOverDialogShown = true;
            pauseGame();

            if (context instanceof Activity) {
                Log.d(TAG, "Game Over detected! Posting task to show dialog.");
                ((Activity) context).runOnUiThread(() -> {
                    if (context instanceof GameActivity && !((GameActivity) context).isFinishing()) {
                        Log.d(TAG, "Running on UI thread to show Game Over dialog.");
                        ((GameActivity) context).showGameOverDialog(dinerState.getScore());
                    }
                });
            } else {
                Log.e(TAG, "Context is not an Activity, cannot show game over dialog!");
            }
        }


        // Draw Game Over Message
        if (dinerState.isGameOver()) {
            String endMessage = "GAME OVER!";
            Paint endPaint = new Paint();
            endPaint.setTextAlign(Paint.Align.CENTER);
            endPaint.setTextSize(80f);
            endPaint.setFakeBoldText(true);
            endPaint.setColor(Color.RED);

            float centerX = canvas.getWidth() / 2f;
            float centerY = canvas.getHeight() / 2f;
            canvas.drawText(endMessage, centerX, centerY, endPaint);

            String finalScoreText = "Final Score: " + dinerState.getScore();
            endPaint.setTextSize(50f);
            canvas.drawText(finalScoreText, centerX, centerY + 80, endPaint);
        }
    }


    private void updatePatienceBarColor(float percentage) {
        if (percentage < 0.3f) patienceBarFgPaint.setColor(Color.RED);
        else if (percentage < 0.6f) patienceBarFgPaint.setColor(Color.YELLOW);
        else patienceBarFgPaint.setColor(Color.GREEN);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {

        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            // Check if tap is within the MENU button area (only if it's currently drawn)
            if (menuButtonArea != null && dinerState !=null && !dinerState.isGameOver()) {
                float touchX = event.getX();
                float touchY = event.getY();
                if (menuButtonArea.contains(touchX, touchY)) {
                    Log.d(TAG, "Menu button tapped!");
                    pauseGame();
                    if (context instanceof GameActivity) {
                        ((GameActivity) context).showInGameMenu();
                    } else {
                        Log.e(TAG, "Context is not GameActivity, cannot show menu!");
                    }
                    return true;
                }
            }
        }

        if (dinerState == null || isPaused() || dinerState.isGameOver()) {
            return false;
        }

        if (dinerState == null || isPaused()) return false;

        float touchX = event.getX();
        float touchY = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                Log.d(TAG, "ACTION_DOWN at (" + touchX + ", " + touchY + ")");
                boolean handledDownEvent = false;

                // Tap on WAITING customer (to start drag)
                List<Customer> waiting = dinerState.getWaitingCustomers();
                for (int i = 0; i < waitingCustomerTapAreas.size(); i++) {

                    RectF tapArea = waitingCustomerTapAreas.get(i);
                    Customer potentialDragCustomer = null;
                    try { potentialDragCustomer = waiting.get(i); } catch (IndexOutOfBoundsException e) { continue; }

                    if (potentialDragCustomer != null && tapArea.contains(touchX, touchY) && potentialDragCustomer.getState() == Customer.CustomerState.WAITING_QUEUE) {
                        isDragging = true;
                        draggedCustomer = potentialDragCustomer;
                        draggedFoodCustomer = null;
                        dragX = touchX;
                        dragY = touchY;
                        Log.i(TAG, "Started dragging waiting customer: " + draggedCustomer.getDisplayId());
                        handledDownEvent = true;
                        break;
                    }
                }

                if (!isDragging) {

                    // Tap on 'OK' Order Confirmation Button
                    for (Pair<RectF, Customer> pair : confirmOrderTapAreas) {
                        RectF area = pair.first;
                        Customer customer = pair.second;
                        if (area != null && customer != null && customer.getState() == Customer.CustomerState.WAITING_ORDER_CONFIRM && area.contains(touchX, touchY)) {
                            Log.d(TAG, "Tap hit CONFIRM ORDER indicator for " + customer.getDisplayId());
                            dinerState.confirmCustomerOrder(customer);
                            handledDownEvent = true;
                            break;
                        }
                    }

                    // Tap on FOOD on Counter (to start drag)
                    if (!handledDownEvent) {
                        for (Pair<RectF, Customer> pair : foodReadyTapAreas) {
                            RectF area = pair.first;
                            Customer foodCustomer = pair.second;

                            // Check tap hit and ensure customer is still expecting food
                            if (area != null && foodCustomer != null && foodCustomer.getState() == Customer.CustomerState.FOOD_READY && area.contains(touchX, touchY)) {
                                isDragging = true;
                                draggedFoodCustomer = foodCustomer;
                                draggedCustomer = null;
                                dragX = touchX;
                                dragY = touchY;
                                Log.i(TAG, "Started dragging FOOD for customer: " + draggedFoodCustomer.getDisplayId());
                                handledDownEvent = true;
                                break;
                            }
                        }
                    }
                    // ------------------------------------------------------
                    if (!handledDownEvent) {
                        for (Pair<RectF, Customer> pair : clearTableTapAreas) {
                            RectF area = pair.first;
                            Customer customer = pair.second;

                            if (area != null && customer != null && customer.getState() == Customer.CustomerState.READY_TO_LEAVE && area.contains(touchX, touchY)) {
                                Log.d(TAG, "Tap hit DONE/Clear Table indicator for " + customer.getDisplayId());

                                dinerState.clearTableForCustomer(customer);
                                handledDownEvent = true;
                                break;
                            }
                        }
                    }

                }

                return handledDownEvent;

            // Update drag position
            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    dragX = touchX;
                    dragY = touchY;
                    return true;
                }
                break;

            // End the drag
            case MotionEvent.ACTION_UP:
                Log.d(TAG, "ACTION_UP at (" + touchX + ", " + touchY + ")");
                if (isDragging) {

                    //  If dragging a CUSTOMER
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
                    // If dragging FOOD
                    else if (draggedFoodCustomer != null) {
                        Log.d(TAG, "Dropped FOOD for " + draggedFoodCustomer.getDisplayId());
                        boolean delivered = false;
                        List<Table> tables = dinerState.getTables();
                        for (Table table : tables) {
                            if (table.isOccupied() && table.getPositionRect().contains(touchX, touchY)) {
                                Customer customerAtTable = table.getSeatedCustomer();

                                if (customerAtTable == draggedFoodCustomer && draggedFoodCustomer.getState() == Customer.CustomerState.FOOD_READY) {
                                    Log.d(TAG, "Attempting to deliver food to customer " + draggedFoodCustomer.getDisplayId() + " at table " + table.id);

                                    delivered = dinerState.deliverFood(draggedFoodCustomer, table);
                                    break;
                                } else {

                                    Log.d(TAG, "Food drop hit occupied table " + table.id + " but customer mismatch ("
                                            + (customerAtTable != null ? customerAtTable.getDisplayId() : "NONE") + " != " + draggedFoodCustomer.getDisplayId()
                                            + ") or wrong state (" + (customerAtTable != null ? customerAtTable.getState() : "N/A") + ")");
                                }
                            }
                        }
                        if (!delivered) {
                            Log.d(TAG, "Food drop missed valid target table/customer.");
                        }
                    }

                    isDragging = false;
                    draggedCustomer = null;
                    draggedFoodCustomer = null;
                    Log.d(TAG, "Drag ended.");
                    return true;

                }
                break;
        }

        return super.onTouchEvent(event);
    }

}