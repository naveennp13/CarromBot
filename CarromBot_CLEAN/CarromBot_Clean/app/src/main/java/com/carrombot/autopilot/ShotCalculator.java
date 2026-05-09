package com.carrombot.autopilot;

public class ShotCalculator {

    private float strikerX, strikerY;
    private float[][] coinPositions;
    private float carromX, carromY;
    
    private static final float STRIKER_RADIUS = 15f;
    private static final float COIN_RADIUS = 12f;
    private static final float CARROM_RADIUS = 14f;
    
    // Board dimensions (typical carrom board ratio)
    private static final float BOARD_WIDTH = 1050f;
    private static final float BOARD_HEIGHT = 1050f;
    
    // Physics constants
    private static final float FRICTION = 0.98f;
    private static final float VELOCITY_DAMPING = 0.95f;

    public static class OptimalShot {
        public float angle;
        public float power;
        public float targetX;
        public float targetY;
        public float expectedScore;
        public String strategy; // "carrom", "coin", "combo"

        public OptimalShot(float angle, float power, float targetX, float targetY, float score, String strategy) {
            this.angle = angle;
            this.power = power;
            this.targetX = targetX;
            this.targetY = targetY;
            this.expectedScore = score;
            this.strategy = strategy;
        }
    }

    public ShotCalculator(float strikerX, float strikerY, float[][] coinPositions, float carromX, float carromY) {
        this.strikerX = strikerX;
        this.strikerY = strikerY;
        this.coinPositions = coinPositions;
        this.carromX = carromX;
        this.carromY = carromY;
    }

    /**
     * Calculate the optimal shot using multi-criteria analysis
     */
    public OptimalShot calculateOptimalShot() {
        OptimalShot bestShot = null;
        float bestScore = -1;

        // Test multiple angles
        for (int angleInt = 0; angleInt < 360; angleInt += 5) {
            float angle = (float) Math.toRadians(angleInt);
            
            // Test multiple power levels
            for (int power = 20; power <= 100; power += 10) {
                OptimalShot shot = evaluateShot(angle, power);
                
                if (shot != null && shot.expectedScore > bestScore) {
                    bestScore = shot.expectedScore;
                    bestShot = shot;
                }
            }
        }

        // If no valid shot found, return safe defaults
        if (bestShot == null) {
            bestShot = new OptimalShot(
                (float) Math.PI / 4,
                50,
                carromX,
                carromY,
                10f,
                "safe"
            );
        }

        return bestShot;
    }

    private OptimalShot evaluateShot(float angle, int power) {
        // Simulate ball trajectory
        float velocityX = (float) Math.cos(angle) * power;
        float velocityY = (float) Math.sin(angle) * power;

        // Simulate physics
        SimulationResult result = simulatePhysics(strikerX, strikerY, velocityX, velocityY);

        if (result == null) return null;

        // Score based on pocketing probability
        float score = calculateScore(result, angle, power);

        return new OptimalShot(
            (float) Math.toDegrees(angle),
            power,
            result.finalX,
            result.finalY,
            score,
            result.strategy
        );
    }

    private SimulationResult simulatePhysics(float startX, float startY, float velX, float velY) {
        float x = startX;
        float y = startY;
        float vx = velX;
        float vy = velY;
        
        float maxIterations = 1000;
        float deltaTime = 0.016f; // 60 FPS
        String strategy = "none";
        boolean hitCarrom = false;
        boolean hitCoin = false;

        for (int i = 0; i < maxIterations; i++) {
            // Update position
            x += vx * deltaTime;
            y += vy * deltaTime;

            // Apply friction
            vx *= FRICTION;
            vy *= FRICTION;

            // Stop if velocity too low
            if (Math.sqrt(vx * vx + vy * vy) < 0.5f) {
                break;
            }

            // Bounce off walls
            if (x - STRIKER_RADIUS < 0 || x + STRIKER_RADIUS > BOARD_WIDTH) {
                vx *= -0.8f;
                x = Math.max(STRIKER_RADIUS, Math.min(BOARD_WIDTH - STRIKER_RADIUS, x));
            }
            if (y - STRIKER_RADIUS < 0 || y + STRIKER_RADIUS > BOARD_HEIGHT) {
                vy *= -0.8f;
                y = Math.max(STRIKER_RADIUS, Math.min(BOARD_HEIGHT - STRIKER_RADIUS, y));
            }

            // Check collision with carrom
            float distToCarrom = (float) Math.hypot(x - carromX, y - carromY);
            if (distToCarrom < STRIKER_RADIUS + CARROM_RADIUS && !hitCarrom) {
                hitCarrom = true;
                strategy = "carrom";
                // Simulate carrom motion (simplified)
                float angle = (float) Math.atan2(carromY - y, carromX - x);
                // Carrom would move in this direction
            }

            // Check collision with coins
            for (int j = 0; j < coinPositions.length; j++) {
                if (coinPositions[j][0] == 0 && coinPositions[j][1] == 0) continue;
                
                float distToCoin = (float) Math.hypot(
                    x - coinPositions[j][0],
                    y - coinPositions[j][1]
                );
                
                if (distToCoin < STRIKER_RADIUS + COIN_RADIUS && !hitCoin) {
                    hitCoin = true;
                    if (hitCarrom) {
                        strategy = "combo";
                    } else {
                        strategy = "coin";
                    }
                }
            }

            // Check if in pocket (simplified - corners)
            if (isPocketted(x, y)) {
                strategy += "_pocketed";
                break;
            }
        }

        return new SimulationResult(x, y, vx, vy, strategy, hitCarrom, hitCoin);
    }

    private boolean isPocketted(float x, float y) {
        // Define pocket zones (corners and pockets)
        float pocketMargin = 50f;
        
        // Top-left
        if (x < pocketMargin && y < pocketMargin) return true;
        // Top-right
        if (x > BOARD_WIDTH - pocketMargin && y < pocketMargin) return true;
        // Bottom-left
        if (x < pocketMargin && y > BOARD_HEIGHT - pocketMargin) return true;
        // Bottom-right
        if (x > BOARD_WIDTH - pocketMargin && y > BOARD_HEIGHT - pocketMargin) return true;

        return false;
    }

    private float calculateScore(SimulationResult result, float angle, int power) {
        float score = 0f;

        // Base score
        if (result.hitCarrom) {
            score += 50f;
        }
        if (result.hitCoin) {
            score += 30f;
        }

        // Bonus for pocket hit
        if (result.strategy.contains("pocketed")) {
            score += 100f;
        }

        // Efficiency bonus (less power used)
        float efficiencyBonus = (100 - power) * 0.2f;
        score += efficiencyBonus;

        // Combo bonus
        if ("combo".equals(result.strategy)) {
            score += 50f;
        }

        return score;
    }

    private static class SimulationResult {
        float finalX, finalY;
        float finalVelX, finalVelY;
        String strategy;
        boolean hitCarrom;
        boolean hitCoin;

        SimulationResult(float x, float y, float vx, float vy, String strategy, boolean hitCarrom, boolean hitCoin) {
            this.finalX = x;
            this.finalY = y;
            this.finalVelX = vx;
            this.finalVelY = vy;
            this.strategy = strategy;
            this.hitCarrom = hitCarrom;
            this.hitCoin = hitCoin;
        }
    }

    /**
     * Get recommended angle and power for a specific target
     */
    public OptimalShot getRecommendedShot(float targetX, float targetY) {
        // Calculate angle to target
        float dx = targetX - strikerX;
        float dy = targetY - strikerY;
        float distanceToTarget = (float) Math.sqrt(dx * dx + dy * dy);
        float angle = (float) Math.atan2(dy, dx);

        // Calculate power needed based on distance
        float power = Math.min(100, (distanceToTarget / BOARD_WIDTH) * 150);

        return new OptimalShot(
            (float) Math.toDegrees(angle),
            power,
            targetX,
            targetY,
            70f,
            "guided"
        );
    }
}
