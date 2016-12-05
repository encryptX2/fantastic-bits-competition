import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

/**
 * Grab Snaffles and try to throw them through the opponent's goal!
 * Move towards a Snaffle and use your team id to determine where you need to throw it.
 **/
class Player {
    private static final String                TEAM_1_TARGET                   = "0 3750";
    private static final String                TEAM_0_TARGET                   = "16000 3750";

    private static final int                   TEAM_1_IDLE_X                   = 14800;
    private static final int                   TEAM_0_IDLE_X                   = 1200;

    private static final int                   IDLE_Y                          = 3750;

    private static final int                   MIN_SNAFFLE_TO_SAVE             = 3200;
    private static int                         MY_TEAM;

    private static final int                   MAX_DIST_FOR_FLIPENDO           = 6000;
    private static final int                   MIN_DIST_FOR_FLIPENDO           = 200;

    private static final int                   TOP_GOAL_FOR_SCORING            = 2500;
    private static final int                   LOWER_GOAL_FOR_SCORING          = 5000;

    private static final int                   TOP_GOAL_REAL                   = 2200;
    private static final int                   LOWER_GOAL_REAL                 = 5300;
    private static final int                   MIN_DIST_FOR_HORIZONTAL_SHOT    = 1000;

    private static final int                   MIN_DIST_ENEMY_TO_STOPABLE      = 2500;
    private static final int                   INTERCEPT_DISTANCE              = 3500;
    private static final int                   MAX_DIST_FOR_ACCIO              = 5000;
    private static final int                   MIN_DIST_ENEMY_TO_SNAFFLE_PETRI = 4000;
    private static final int                   MIN_DIST_FOR_ACCIO              = 2000;
    private static final int                   UNITS_PER_Y_SCALE               = 600;
    private static boolean                     ATTACK_LEFT                     = false;
    private static boolean                     ATTACK_RIGHT                    = false;
    private static final int                   DIST_FOR_INCREASE_ACC_DEFAULT   = 2000;
    private static final int                   MIN_SPEED_FOR_DEPOSES           = 350;
    private static final Map<Integer, Integer> DEFAULT_OFFSETS                 = new HashMap<Integer, Integer>();
    private static final int                   MIN_DIST_TO_FRIENDLY_FOR_ACCIO  = 2000;
    private static final int                   EXPECTED_TRAVEL_DISTANCE        = 3000;
    static {
        DEFAULT_OFFSETS.put(1, 600);
        DEFAULT_OFFSETS.put(2, 1050);
        DEFAULT_OFFSETS.put(3, 1200);
        DEFAULT_OFFSETS.put(4, 1600);
        DEFAULT_OFFSETS.put(5, 1700);

        DEFAULT_OFFSETS.put(-1, -600);
        DEFAULT_OFFSETS.put(-2, -1050);
        DEFAULT_OFFSETS.put(-3, -1200);
        DEFAULT_OFFSETS.put(-4, -1600);
        DEFAULT_OFFSETS.put(-5, -1700);
    }

    private static int mana      = 0;
    private static int crtTurn   = 0;
    private static int lastAccio = 0;

    public static void main(String args[]) throws Exception {
        Scanner in = new Scanner(System.in);
        MY_TEAM = in.nextInt(); // if 0 you need to score on the right of the map, if 1 you need to score on the left
        if (MY_TEAM == 0) {
            ATTACK_RIGHT = true;
        } else {
            ATTACK_LEFT = true;
        }

        int timeSinceLastFlip = 0;
        int timeSinceLastProtectPetri = 0;
        int timeSinceLastDeposesPetri = 0;
        int timeSinceLastAccio = 0;
        Intersection intersect2 = getLineIntersection(10156, 6967, 10156 + 53, 6967 + 463, 0, 7400, 16000,
                7400);
        System.err.println("Is there any intersection? " + (intersect2 != null));
        if (intersect2 != null) {
            System.err.println("Intersection at x/y : " + intersect2.getX() + "/" + intersect2.getY());
        }
        List<Wizard> previousEnemies = null;

        // game loop
        while (true) {
            mana++;
            crtTurn++;
            timeSinceLastFlip++;
            timeSinceLastProtectPetri++;
            timeSinceLastAccio++;
            timeSinceLastDeposesPetri++;
            long startTime = Calendar.getInstance().getTimeInMillis();
            System.err.println("Mana : " + mana);
            if (lastAccio > 0) {
                lastAccio--;
            }
            RoundParams params = initRoundParams(in);
            Thread.sleep(20);
            System.err.println("Init time: " + (Calendar.getInstance().getTimeInMillis() - startTime));
            params.getMyWizards();
            Wizard otherWizard = null;

            for (Wizard myWizard : params.getMyWizards()) {
                if (myWizard.getHoldsSnaffle()) {

                    int throwPower = 500;
                    ThrowParams throwParams = getThrowParams(params, myWizard, previousEnemies);

                    /*if (MY_TEAM == 0) {
                        System.out.println("THROW " + TEAM_0_TARGET + " " + throwPower);
                    } else {
                        System.out.println("THROW " + TEAM_1_TARGET + " " + throwPower);
                    }
                    continue;*/
                    if (!throwParams.isShouldMove()) {
                        System.err.println("------------ Throwing snaffle ----------");
                        System.err.println(getSnaffleAtPosition(params, myWizard.getX(), myWizard.getY()));
                        System.out.println("THROW " + throwParams.getX() + " " + throwParams.getY() + " "
                                + throwParams.getPower());
                    } else {
                        System.err.println("------------ Moving to protect snaffle ----------");
                        System.out.println("MOVE " + throwParams.getX() + " " + throwParams.getY() + " "
                                + throwParams.getPower());
                    }

                    continue;
                }
                System.err.println("Nr of snaffles: " + params.getSnaffles().size());
                otherWizard = getOtherWizard(params, myWizard);
                Snaffle closestSnaffle = getClosestSnaffle(myWizard, params, otherWizard);

                // Try to cast some spells bruh
                System.err.println("Mana : " + mana);

                if (mana > 10 && timeSinceLastProtectPetri > 3) {
                    Snaffle farGoalSnaffle = getFarGoalSnaffle(params);
                    if (farGoalSnaffle != null && !playerHasSnaffle(params, farGoalSnaffle)) {
                        System.out.println("PETRIFICUS " + farGoalSnaffle.getId());
                        timeSinceLastProtectPetri = 0;
                        mana -= 10;
                        continue;
                    }
                }
                /*if (mana > 10 && timeSinceLastDeposesPetri > 3) {
                    SnaffleWizardPair enemyWithSnaffle = getEnemyWithSnaffle(params);
                    if (enemyWithSnaffle != null) {
                        System.out.println("PETRIFICUS " + enemyWithSnaffle.getSnaffle().getId());
                        timeSinceLastDeposesPetri = 0;
                        mana -= 10;
                        continue;
                    }
                }*/
                if (mana > 25) {

                    if (timeSinceLastAccio > 5) {
                        //Wizard firstWizard = getFirstWizard(params.getMyWizards());
                        Snaffle snaffleBehind = getSnaffleBehindMe(params.getSnaffles(), myWizard,
                                otherWizard);
                        if (snaffleBehind != null) {
                            System.out.println("ACCIO " + snaffleBehind.getId());
                            timeSinceLastAccio = 0;
                            mana -= 20;
                            continue;
                        }
                    }
                }
                if (mana > 20) {

                    if (timeSinceLastFlip > 5) {
                        Snaffle sureHit = getSureGoal(params, myWizard);
                        if (sureHit != null) {
                            System.out.println("FLIPENDO " + sureHit.getId());
                            timeSinceLastFlip = 0;
                            System.err.println("--  Opportunistic shooting  --");
                            System.err
                                    .println("Wizard " + myWizard.getId() + ", FLIPENDO " + sureHit.getId());
                            System.err.println(myWizard);
                            System.err.println(sureHit);
                            mana -= 20;
                            continue;
                        }
                    }
                }

                if (closestSnaffle != null) {
                    Snaffle snaffleInTheFuture = getSnaffleAtNextStep(closestSnaffle);
                    System.out.println(
                            "MOVE " + snaffleInTheFuture.getX() + " " + snaffleInTheFuture.getY() + " 150");
                } else {

                    Snaffle lastSnaffle = getClosestSnaffle(myWizard, params, null);
                    Snaffle snaffleInTheFuture = getSnaffleAtNextStep(lastSnaffle);

                    System.out.println(
                            "MOVE " + snaffleInTheFuture.getX() + " " + snaffleInTheFuture.getY() + " 150");
                    continue;
                }
            }
            previousEnemies = new ArrayList<Wizard>();
            for (Wizard opponent : params.getOpponentWizards()) {
                previousEnemies.add(opponent);
            }
            System.err.println("Time this round: " + (Calendar.getInstance().getTimeInMillis() - startTime));
        }
    }

    private static Wizard getCastingEnemy(RoundParams params, List<Wizard> previousEnemies) {
        for (Wizard opponent : params.getOpponentWizards()) {
            Wizard opponentBefore = getOpponentById(opponent.getId(), previousEnemies);
            // Can't tell if the opponent will cast a spell when holding a snaffle, it's unikely that it would
            if (opponentBefore.getHoldsSnaffle()) {
                continue;
            }
            Coord estimatedPosition = new Coord(opponentBefore.getX() + opponentBefore.getVx(),
                    opponentBefore.getY() + opponentBefore.getVy());
            if (Math.abs(opponent.getX() - estimatedPosition.getX()) < 2
                    && Math.abs(opponent.getY() - estimatedPosition.getY()) < 2) {
                // Opponent wizard moved in a predicted manner -> he used a spell
                return opponent;
            }
            System.err.println("********************************");
            System.err.println("1. " + opponent);
            System.err.println("2. " + opponentBefore);
            System.err.println("3. " + estimatedPosition);
            System.err.println("********************************");
        }
        return null;
    }

    private static Wizard getOpponentById(int id, List<Wizard> previousEnemies) {
        for (Wizard enemy : previousEnemies) {
            if (enemy.getId() == id) {
                return enemy;
            }
        }
        return null;
    }

    /**
     * Returns a snaffle wizard pair if the wizard is an opponent holding a snaffle in our side of the court
     * @param params
     * @return
     */
    private static SnaffleWizardPair getEnemyWithSnaffle(RoundParams params) {
        for (Wizard opponent : params.getOpponentWizards()) {
            //Snaffle heldSnaffle = getSnaffleAtPosition(params, opponent.getX(), opponent.getY());
            Snaffle heldSnaffle = getSnaffleHeldNextTurn(params, opponent);
            if (heldSnaffle != null
                    && ((ATTACK_LEFT && opponent.getX() > 8000) || (ATTACK_RIGHT && opponent.getX() < 8000))
                    && getTrueSpeed(opponent) > MIN_SPEED_FOR_DEPOSES) {
                System.err.println("True speed : " + getTrueSpeed(opponent));
                return new SnaffleWizardPair(opponent, heldSnaffle);
            }
        }
        return null;
    }

    /**
     * Return the snaffle this wizard will hold next turn, if any
     * @param params
     * @param opponent
     * @return
     */
    private static Snaffle getSnaffleHeldNextTurn(RoundParams params, Wizard opponent) {
        Coord nextWizardCoord = new Coord(opponent.getX() + opponent.getVx(),
                opponent.getY() + opponent.getVy());
        for (Snaffle snaffle : params.getSnaffles()) {
            Coord nextSnaffleCoord = new Coord(snaffle.getX() + snaffle.getVx(),
                    snaffle.getY() + snaffle.getVy());
            if (getDistance(nextSnaffleCoord.getX(), nextSnaffleCoord.getY(), nextWizardCoord.getX(),
                    nextWizardCoord.getY()) < opponent.getSize()) {
                return snaffle;
            }
        }
        return null;
    }

    private static int getTrueSpeed(Entity opponent) {
        return new Double(
                Math.sqrt(opponent.getVx() * opponent.getVx() + opponent.getVy() * opponent.getVy()))
                        .intValue();
    }

    private static Snaffle getSnaffleBehindMe(List<Snaffle> snaffles, Wizard thisWizard, Wizard otherWizard) {
        for (Snaffle snaffle : snaffles) {
            int dist = getDistance(snaffle.getX(), snaffle.getY(), thisWizard.getX(), thisWizard.getY());
            if (dist < MAX_DIST_FOR_ACCIO && dist > MIN_DIST_FOR_ACCIO
                    && ((thisWizard.getX() > snaffle.getX() && ATTACK_RIGHT)
                            || (thisWizard.getX() < snaffle.getX() && ATTACK_LEFT))
                    && getDistance(snaffle.getX(), snaffle.getY(), otherWizard.getX(),
                            otherWizard.getY()) > MIN_DIST_TO_FRIENDLY_FOR_ACCIO) {
                return snaffle;
            }
        }
        return null;
    }

    private static Wizard getFirstWizard(List<Wizard> myWizards) {
        Wizard firstWizard = null;
        for (Wizard wizard : myWizards) {
            if (firstWizard == null || (firstWizard.getX() < wizard.getX() && ATTACK_RIGHT)
                    || (firstWizard.getX() > wizard.getX() && ATTACK_LEFT)) {
                firstWizard = wizard;
            }
        }
        return firstWizard;
    }

    private static boolean playerHasSnaffle(RoundParams params, Snaffle snaffle) {
        for (Wizard wizard : params.getMyWizards()) {
            if (wizard.getX() == snaffle.getX() && wizard.getY() == snaffle.getY()) {
                return true;
            }
            System.err.println("wizard " + wizard.getId() + " at " + wizard.getX() + "/" + wizard.getY()
                    + " does not have the snaffle at " + snaffle.getX() + "/" + snaffle.getY());
        }
        for (Wizard wizard : params.getOpponentWizards()) {
            if (wizard.getX() == snaffle.getX() && wizard.getY() == snaffle.getY()) {
                return true;
            }
            System.err.println("wizard " + wizard.getId() + " at " + wizard.getX() + "/" + wizard.getY()
                    + " does not have the snaffle at " + snaffle.getX() + "/" + snaffle.getY());
        }
        return false;
    }

    private static Snaffle getFarGoalSnaffle(RoundParams params) {
        for (Snaffle snaffle : params.getSnaffles()) {
            int xGoal = MY_TEAM == 0 ? 0 : 16000;

            // Don't petri if it will score next turn anyway

            /*System.err.println(snaffle);
            System.err.println("is intersection null ? " + (getLineIntersection(snaffle.getX(),
                    snaffle.getY(), new Double(snaffle.getX() + snaffle.getVx() * 2).intValue(),
                    new Double(snaffle.getY() + snaffle.getVy() * 2).intValue(), xGoal, TOP_GOAL_REAL,
                    xGoal, LOWER_GOAL_REAL) == null));*/
            if (getLineIntersection(snaffle.getX(), snaffle.getY(), snaffle.getX() + snaffle.getVx() * 2,
                    snaffle.getY() + snaffle.getVy() * 2, xGoal, TOP_GOAL_REAL, xGoal,
                    LOWER_GOAL_REAL) != null
                    && !isEnemyNear(snaffle, params.getOpponentWizards(), MIN_DIST_ENEMY_TO_SNAFFLE_PETRI)
                    && !willSnaffleScoreNextTurn(snaffle, true)) {
                // This bitch scores in max 4 turns
                return snaffle;
            }
        }
        return null;
    }

    private static boolean isEnemyNear(Snaffle snaffle, List<Wizard> opponentWizards, int minDist) {
        int smallestDist = closestEnemyDistance(snaffle, opponentWizards);
        return smallestDist < minDist;
    }

    private static ThrowParams getThrowParams(RoundParams params, Wizard wizard,
            List<Wizard> previousEnemies) {

        // Detect snaffle stuns
        // If opponent casts spell, don't throw but try to negate your movement vector
        // with opposing vx and by to your current ones and speed equal or less than your current modulus speed
        /*if (previousEnemies != null) {
            Wizard castingEnemy = getCastingEnemy(params, previousEnemies);
            if (castingEnemy != null) {
                System.err.println("Casting enemy: " + castingEnemy);
                SpeedVector speed = new SpeedVector();
                speed.setVx(-1 * wizard.getVx());
                speed.setVy(-1 * wizard.getVy());
                int throwPower = new Double(
                        Math.sqrt(wizard.getVx() * wizard.getVx() + wizard.getVy() * wizard.getVx()))
                                .intValue();
                throwPower = throwPower < 150 ? throwPower : 150;
                throwPower = throwPower > 0 ? throwPower : 0;
        
                ThrowParams avoidDribbleParams = new ThrowParams();
                avoidDribbleParams.setX(wizard.getX() + 5 * speed.getVx());
                avoidDribbleParams.setY(wizard.getY() + 5 * speed.getVy());
                avoidDribbleParams.setPower(throwPower);
                avoidDribbleParams.setShouldMove(true);
        
                return avoidDribbleParams;
            }
        }*/

        ThrowParams finalThrowParams = null;
        Coord defaultTarget = getDefaultTarget(wizard, 500);
        ThrowParams defaultParams = getDefaultThrowParams(defaultTarget);
        if (!throwGetsIntercepted(wizard, params, getGoalCenterCoord(), 2500, 500)) {
            finalThrowParams = defaultParams;
            System.err.println("Went with good old default");
            return finalThrowParams;
        }
        Coord sideShotTarget = getSideShotTarget(params, wizard);
        //if (!throwGetsIntercepted(wizard, params, sideShotTarget, 2500, 500)) {
            System.err.println("Went with left / right align");
            Coord compensatedCoord = getCompensatedCoord(wizard, 500, sideShotTarget);
            ThrowParams sideShotParams = new ThrowParams();
            sideShotParams.setX(compensatedCoord.getX());
            sideShotParams.setY(compensatedCoord.getY());
            sideShotParams.setPower(500);
            return sideShotParams;
        //}

        /*ThrowParams passBehindParams = getPassBehindParams(wizard);
        finalThrowParams = passBehindParams;
        System.err.println("Went with pass behind");
        return finalThrowParams;*/
    }

    private static Coord limitSegmentLength(Entity entity, Coord coord, int segmentLength) {
        Coord limitedSegment = new Coord();
        int vx = coord.getX() - entity.getX();
        int vy = coord.getY() - entity.getY();
        int crtSegmentLength = new Double(Math.sqrt(vx * vx + vy * vy)).intValue();
        double multiplier = segmentLength * 1.0 / crtSegmentLength;
        vx = new Double(vx * multiplier).intValue();
        vy = new Double(vy * multiplier).intValue();

        limitedSegment.setX(entity.getX() + vx);
        limitedSegment.setY(entity.getY() + vy);

        return limitedSegment;
    }

    private static boolean throwGetsIntercepted(Entity entity, RoundParams params, Coord target, int distance,
            int throwPower) {
        return throwGetsIntercepted(entity, params, target, distance, false, throwPower);
    }

    private static Coord getSideShotTarget(RoundParams params, Wizard wizard) {
        Coord coord = getGoalCenterCoord();
        SpeedVector directShot = new SpeedVector(coord.getX() - wizard.getX(), coord.getY() - wizard.getY());
        SpeedVector leftAuxVector = new SpeedVector(-1 * directShot.getVy(), directShot.getVx());
        SpeedVector rightAuxVector = new SpeedVector(directShot.getVy(), -1 * directShot.getVx());

        SpeedVector leftShot = new SpeedVector(directShot.getVx() + leftAuxVector.getVx(),
                directShot.getVy() + leftAuxVector.getVy());
        SpeedVector rightShot = new SpeedVector(directShot.getVx() + rightAuxVector.getVx(),
                directShot.getVy() + rightAuxVector.getVy());

        int vectSize = new Double(
                Math.sqrt(directShot.getVx() * directShot.getVx() + directShot.getVy() * directShot.getVy()))
                        .intValue();

        double multiplier = EXPECTED_TRAVEL_DISTANCE * 1.0 / vectSize;
        leftShot.setVx(new Double(leftShot.getVx() * multiplier).intValue());
        leftShot.setVy(new Double(leftShot.getVy() * multiplier).intValue());
        rightShot.setVx(new Double(rightShot.getVx() * multiplier).intValue());
        rightShot.setVy(new Double(rightShot.getVy() * multiplier).intValue());

        Coord leftPoint = new Coord(wizard.getX() + leftShot.getVx(), wizard.getY() + leftShot.getVy());
        Coord rightPoint = new Coord(wizard.getX() + rightShot.getVx(), wizard.getY() + rightShot.getVy());

        int leftEnemyDist = getClosestEnemyDistance(params, leftPoint);
        int rightEnemyDist = getClosestEnemyDistance(params, rightPoint);
        if (leftEnemyDist > rightEnemyDist) {
            return leftPoint;
        }

        return rightPoint;
    }

    private static int getClosestEnemyDistance(RoundParams params, Coord leftPoint) {
        int lowestDist = Integer.MAX_VALUE;
        for (Wizard enemy : params.getOpponentWizards()) {
            int dist = getDistance(enemy.getX(), enemy.getY(), leftPoint.getX(), leftPoint.getY());
            if (dist < lowestDist) {
                lowestDist = dist;
            }
        }
        return lowestDist;
    }

    private static ThrowParams getPassBehindParams(Wizard wizard) {
        ThrowParams passBackParams = new ThrowParams();
        passBackParams.setPower(100);
        passBackParams.setX(wizard.getX() - wizard.getVx());
        passBackParams.setY(wizard.getY() - wizard.getVy());
        return passBackParams;
    }

    private static Coord getDefaultTarget(Wizard wizard, int throwPower) {
        // Make default target consider wizard speed vector
        // Throwing a snaffle requires multiply by 2 since it has a mass of 
        throwPower *= 2;
        Coord coord = getGoalCenterCoord();
        return getCompensatedCoord(wizard, throwPower, coord);
    }

    private static Coord getGoalCenterCoord() {
        Coord coord = new Coord();
        coord.setY(IDLE_Y);
        if (MY_TEAM == 0) {
            coord.setX(16000);
        } else {
            coord.setX(0);
        }
        return coord;
    }

    private static Coord getCompensatedCoord(Wizard wizard, int throwPower, Coord coord) {
        //System.err.println("###########################################");
        SpeedVector desiredVector = new SpeedVector();
        desiredVector.setVx(coord.getX() - wizard.getX());
        desiredVector.setVy(coord.getY() - wizard.getY());
        //System.err.println("Desired vector: " + desiredVector);

        int desiredVectorLength = new Double(
                Math.sqrt((wizard.getVx() * wizard.getVx() + wizard.getVy() * wizard.getVy())
                        + throwPower * throwPower)).intValue();
        //System.err.println("Desired vector length: " + desiredVectorLength);
        int curentVectorLength = new Double(Math.sqrt(desiredVector.getVx() * desiredVector.getVx()
                + desiredVector.getVy() * desiredVector.getVy())).intValue();

        //System.err.println("Current vector length: " + curentVectorLength);
        // Change desired vector length from curentVectorLength to desiredVectorLength
        double multiplier = desiredVectorLength * 1.0 / curentVectorLength;
        //System.err.println("Multiplier: " + multiplier);
        desiredVector.setVx(new Double(desiredVector.getVx() * multiplier).intValue());
        desiredVector.setVy(new Double(desiredVector.getVy() * multiplier).intValue());
        //System.err.println("Normalized desired vector: " + desiredVector);
        SpeedVector compensationVector = new SpeedVector();
        compensationVector.setVx(desiredVector.getVx() - wizard.getVx());
        compensationVector.setVy(desiredVector.getVy() - wizard.getVy());
        //System.err.println("Compensation vector: " + compensationVector);

        Coord compensatedCoord = new Coord();
        compensatedCoord.setX(wizard.getX() + compensationVector.getVx());
        compensatedCoord.setY(wizard.getY() + compensationVector.getVy());
        //System.err.println("Compensation coordinate: " + compensatedCoord);

        //System.err.println("###########################################");
        return compensatedCoord;
    }

    private static boolean throwGetsIntercepted(Entity entity, RoundParams params, Coord target,
            int interceptDistance, boolean ignoreSnaffles, int throwPower) {
        /*SpeedVector sv = new SpeedVector();
        sv.setVx(target.getX() - entity.getX());
        sv.setVy(target.getY() - entity.getY());
        System.err.println("Initial speed vector " + sv);
        // Consider existing speed vector of the entity and throw power!
        // Duplicate throw power to take into consideration snaffle mass which is .5
        throwPower *= 2;
        // Scale speed vector to the throw power
        double scale = Math
                .sqrt((sv.getVx() * sv.getVx() + sv.getVy() * sv.getVy()) / (throwPower * throwPower));
        System.err.println("Scale : " + scale);
        sv.setVx(new Double(sv.getVx() / scale).intValue());
        sv.setVy(new Double(sv.getVy() / scale).intValue());
        System.err.println("Scaled speed vector " + sv);
        // Add existing entity speeds
        sv.setVx(sv.getVx() + entity.getVx());
        sv.setVy(sv.getVy() + entity.getVy());
        System.err.println("True speed vector(considers entity speed) " + sv);
        
        //System.err.println("true vector " + sv);
        double multiplier = interceptDistance
                / Math.sqrt((sv.getVx() * sv.getVx() + sv.getVy() * sv.getVy()));
        System.err.println("Multiplier value: " + multiplier);
        sv.setVx(new Double(sv.getVx() * multiplier).intValue());
        sv.setVy(new Double(sv.getVy() * multiplier).intValue());
        System.err.println("shortened vector " + sv);
        
        Coord updatedTarget = new Coord(entity.getX() + sv.getVx(), entity.getY() + sv.getVy());*/
        /*Segment travelPath = new Segment(entity.getX(), entity.getY(), updatedTarget.getX(),
                updatedTarget.getY());*/
        Coord updatedTarget = limitSegmentLength(entity, getGoalCenterCoord(), interceptDistance);

        Segment travelPath = new Segment(entity.getX(), entity.getY(), updatedTarget.getX(),
                updatedTarget.getY());

        System.err.println(travelPath);

        List<Entity> entitiesToAvoid = new ArrayList<Entity>();
        entitiesToAvoid.addAll(params.getBludgers());
        entitiesToAvoid.addAll(params.getOpponentWizards());
        if (!ignoreSnaffles) {
            entitiesToAvoid.addAll(params.getSnaffles());
        }
        for (Entity entityToCheck : entitiesToAvoid) {
            if (entity.getX() != entityToCheck.getX() && entity.getY() != entityToCheck.getY()
                    && entitiesColide(travelPath, entityToCheck)) {
                //System.err.println("detected collision with entity " + entityToCheck);
                //System.err.println("Evaluated path is " + travelPath);
                return true;
            }
        }

        return false;
    }

    private static boolean entitiesColide(Segment travelPath, Entity entityToCheck) {
        List<Segment> obstacleBounds = entityToCheck.getDefiningBoundaries(true);
        for (Segment obstacleSegment : obstacleBounds) {
            Intersection collision = getLineIntersection(travelPath.getX1(), travelPath.getY1(),
                    travelPath.getX2(), travelPath.getY2(), obstacleSegment.getX1(), obstacleSegment.getY1(),
                    obstacleSegment.getX2(), obstacleSegment.getY2());
            if (collision != null) {
                System.err.println("Segment of entity " + entityToCheck.getId());
                System.err.println(obstacleSegment);
                System.err.println("---------------------------------");
                System.err.println("Collision on " + collision);
                return true;
            }
        }
        return false;
    }

    private static ThrowParams getDefaultThrowParams(Coord pointToTarget) {
        ThrowParams throwParams = new ThrowParams();
        throwParams.setPower(500);
        throwParams.setX(pointToTarget.getX());
        throwParams.setY(pointToTarget.getY());

        return throwParams;
    }

    private static Intersection getLineIntersection(int o1x1, int o1y1, int o1x2, int o1y2, int o2x1,
            int o2y1, int o2x2, int o2y2) {
        double denom = (o2y2 - o2y1) * (o1x2 - o1x1) - (o2x2 - o2x1) * (o1y2 - o1y1);
        if (denom > -0.01 && denom < 0.01) { // Lines are parallel.
            return null;
        }
        double ua = ((o2x2 - o2x1) * (o1y1 - o2y1) - (o2y2 - o2y1) * (o1x1 - o2x1)) / denom;
        double ub = ((o1x2 - o1x1) * (o1y1 - o2y1) - (o1y2 - o1y1) * (o1x1 - o2x1)) / denom;
        if (ua >= 0.0f && ua <= 1.0f && ub >= 0.0f && ub <= 1.0f) {
            // Get the intersection point.
            return new Intersection((int) (o1x1 + ua * (o1x2 - o1x1)), (int) (o1y1 + ua * (o1y2 - o1y1)));
        }

        return null;
    }

    private static Snaffle getSnaffleAtNextStep(Snaffle snaffle) {
        Snaffle nextPosSnaffle = new Snaffle();
        nextPosSnaffle.setX(new Double(snaffle.getX() + snaffle.getVx() + 1.31 * snaffle.getVx()).intValue());
        nextPosSnaffle.setY(new Double(snaffle.getY() + snaffle.getVy() + 1.31 * snaffle.getVy()).intValue());
        return nextPosSnaffle;
    }

    private static boolean existsSnaffle(List<Snaffle> snaffles, Snaffle snaffleToCheck) {
        for (Snaffle snaffle : snaffles) {
            if (snaffle.getId() == snaffleToCheck.getId()) {
                return true;
            }
        }
        return false;
    }

    private static Snaffle getSnaffleAtPosition(RoundParams params, int x, int y) {
        for (Snaffle snaffle : params.getSnaffles()) {
            if (snaffle.getX() == x && snaffle.getY() == y) {
                return snaffle;
            }
        }
        return null;
    }

    /**
     * For each snaffle, check if pushing it will score a goal
     * @param snaffles
     * @param myWizard
     * @return
     */
    private static Snaffle getSureGoal(RoundParams params, Wizard myWizard) {
        for (Snaffle snaffle : params.getSnaffles()) {
            Snaffle scoringSnaffle = returnScoringSnaffle(myWizard, snaffle);
            if (scoringSnaffle != null && !playerHasSnaffle(params, scoringSnaffle)) {
                System.err.println(scoringSnaffle);
                return scoringSnaffle;
            }
        }
        return null;
    }

    private static Snaffle returnScoringSnaffle(Wizard myWizard, Snaffle snaffle) {
        if ((MY_TEAM == 0 && snaffle.getX() > myWizard.getX())
                || (MY_TEAM == 1 && snaffle.getX() < myWizard.getX())
                        && !willSnaffleScoreNextTurn(snaffle, false)) {
            System.err.println("snaffle id: " + snaffle.getId());
            System.err.println("snaffle x: " + snaffle.getX());
            System.err.println("myWizard x: " + myWizard.getX());
            /*int ys = snaffle.getY();
            int xs = snaffle.getX();*/
            int ys = snaffle.getY() + snaffle.getVy();
            int xs = snaffle.getX() + snaffle.getVx();
            int yw = new Double(myWizard.getY() + 1.37 * myWizard.getVy()).intValue();
            int xw = new Double(myWizard.getX() + 1.37 * myWizard.getVy()).intValue();
            int xd = MY_TEAM == 0 ? 16000 : 0;
            int xReal = Math.abs(xw - xs);
            int yReal = Math.abs(yw - ys);
            int xDesired = Math.abs(xs - xd);
            int yDesired = new Double(Math.floor(xDesired * yReal * 1.0 / xReal)).intValue();
            yDesired = ys > yw ? yDesired : -1 * yDesired;
            int yd = ys + yDesired;
            /*int yd = new Double(ys*1.0 - (1.0 * Math.abs(yw - ys) * Math.abs(xs - xd))/ Math.abs(xw - xs))
                    .intValue();
            yd = yd > 0 ? yd : -1 * yd;*/
            if (yd > 2400 && yd < 5100 && (Math.abs(xs - xw) < MAX_DIST_FOR_FLIPENDO)
                    && Math.abs(xs - xw) > MIN_DIST_FOR_FLIPENDO) {

                return snaffle;
            }
        }
        System.err.println("-------------------------------------");
        return null;
    }

    private static boolean willSnaffleScoreNextTurn(Snaffle snaffle, boolean opposingTeam) {
        int xGoal = ATTACK_RIGHT ? 16000 : 0;
        if (opposingTeam) {
            xGoal = 16000 - xGoal;
        }
        Intersection intersection = getLineIntersection(snaffle.getX(), snaffle.getY(),
                new Double(snaffle.getX() + snaffle.getVx() * 1.75).intValue(),
                new Double(snaffle.getY() + snaffle.getVy() * 1.75).intValue(), xGoal, TOP_GOAL_FOR_SCORING,
                xGoal, LOWER_GOAL_FOR_SCORING);
        if (intersection != null) {
            return true;
        }
        return false;
    }

    private static Snaffle getSnaffleToStop(RoundParams params) {
        for (Snaffle snaffle : params.getSnaffles()) {
            if ((MY_TEAM == 0 && snaffle.getX() < MIN_SNAFFLE_TO_SAVE
                    || MY_TEAM == 1 && snaffle.getX() > (16000 - MIN_SNAFFLE_TO_SAVE))
                    && (Math.abs(snaffle.getVx()) > 50)
                    && closestEnemyDistance(snaffle, params.getOpponentWizards()) > MIN_DIST_ENEMY_TO_STOPABLE
                    && snaffle.getY() > TOP_GOAL_FOR_SCORING && snaffle.getY() < LOWER_GOAL_FOR_SCORING
                    && isSnaffleTargettingMe(snaffle)) {
                System.err.println("Snaffle details (id. vx, vy): " + snaffle.getId() + ", " + snaffle.getVx()
                        + ", " + snaffle.getVy());
                return snaffle;
            }
        }
        return null;
    }

    private static boolean isSnaffleTargettingMe(Snaffle snaffle) {
        if (MY_TEAM == 0 && snaffle.getVx() < 0 || MY_TEAM == 1 && snaffle.getVx() > 0) {
            return true;
        }
        return false;
    }

    private static int closestEnemyDistance(Entity snaffle, List<Wizard> opponents) {
        int smallestDist = Integer.MAX_VALUE;
        for (Wizard opponent : opponents) {
            int dist = getDistance(snaffle.getX(), snaffle.getY(), opponent.getX(), opponent.getY());
            if (dist < smallestDist) {
                smallestDist = dist;
            }
        }
        return smallestDist;
    }

    private static Wizard getOtherWizard(RoundParams params, Wizard myWizard) {
        for (Wizard wizard : params.getMyWizards()) {
            if (wizard.getId() != myWizard.getId()) {
                return wizard;
            }
        }
        return null;
    }

    private static Snaffle getClosestSnaffle(Wizard wizard, RoundParams params, Wizard additionalWizard) {

        Snaffle closestSnaffle = null;
        Snaffle secondClosest = null;
        int smallestDist = Integer.MAX_VALUE;
        int secondSmallestDist = Integer.MAX_VALUE;
        for (Snaffle snaffle : params.getSnaffles()) {

            int crtDist = getDistance(wizard.getX(), wizard.getY(), snaffle.getX(), snaffle.getY());
            if (crtDist < smallestDist) {
                if (smallestDist < secondSmallestDist) {
                    secondSmallestDist = smallestDist;
                    secondClosest = closestSnaffle;
                }
                smallestDist = crtDist;
                closestSnaffle = snaffle;
            } else if (crtDist < secondSmallestDist) {
                secondSmallestDist = crtDist;
                secondClosest = snaffle;
            }
        }
        if (closestSnaffle != null && additionalWizard != null) {
            Snaffle additionalSnaffe = getClosestSnaffle(additionalWizard, params, null);
            //our buddy is closer, leave it to him
            if (additionalSnaffe != null && additionalSnaffe.getId() == closestSnaffle.getId()
                    && getDistance(wizard.getX(), wizard.getY(), closestSnaffle.getX(),
                            closestSnaffle.getY()) > getDistance(additionalWizard.getX(),
                                    additionalWizard.getY(), closestSnaffle.getX(), closestSnaffle.getY())) {
                /*System.err.println("I, wizard " + wizard.getId() + " will leave snaffle with id "
                        + closestSnaffle.getId());*/
                closestSnaffle = secondClosest;
            }
        }

        return closestSnaffle;
    }

    private static int getDistance(int x1, int y1, int x2, int y2) {
        return new Double(Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2))).intValue();
    }

    private static RoundParams initRoundParams(Scanner in) throws Exception {
        RoundParams params = new RoundParams();
        params.setMyWizards(new ArrayList<Wizard>());
        params.setOpponentWizards(new ArrayList<Wizard>());
        params.setSnaffles(new ArrayList<Snaffle>());
        params.setBludgers(new ArrayList<Bludger>());

        int entities = in.nextInt(); // number of entities still in game
        for (int i = 0; i < entities; i++) {
            int entityId = in.nextInt(); // entity identifier
            String entityType = in.next(); // "WIZARD", "OPPONENT_WIZARD" or "SNAFFLE" (or "BLUDGER" after first league)
            int x = in.nextInt(); // position
            int y = in.nextInt(); // position
            int vx = in.nextInt(); // velocity
            int vy = in.nextInt(); // velocity
            int state = in.nextInt(); // 1 if the wizard is holding a Snaffle, 0 otherwise

            switch (entityType) {
                case "WIZARD":
                    params.getMyWizards().add(new Wizard(entityId, x, y, vx, vy, state));
                    break;
                case "OPPONENT_WIZARD":
                    params.getOpponentWizards().add(new Wizard(entityId, x, y, vx, vy, state));
                    break;
                case "SNAFFLE":
                    params.getSnaffles().add(new Snaffle(entityId, x, y, vx, vy));
                    break;
                case "BLUDGER":
                    params.getBludgers().add(new Bludger(entityId, x, y, vx, vy));
                    break;
                default:
                    throw new Exception("Not implemented");
            }
        }
        for (Snaffle snaffle : params.getSnaffles()) {
            if (snaffle.getId() == 7) {
                System.err.println(snaffle);
            }
        }
        return params;
    }
}

class SpeedVector {
    private int vx;
    private int vy;

    public SpeedVector() {
    };

    public SpeedVector(int vx, int vy) {
        super();
        this.vx = vx;
        this.vy = vy;
    }

    public int getVx() {
        return vx;
    }

    public void setVx(int vx) {
        this.vx = vx;
    }

    public int getVy() {
        return vy;
    }

    public void setVy(int vy) {
        this.vy = vy;
    }

    public String toString() {
        return "{Speed vector vx=" + vx + "vy=" + vy + "}";
    }
}

class Coord {
    private int x;
    private int y;

    public Coord() {
    }

    public Coord(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public String toString() {
        return "{Coord x=" + x + " y=" + y + "}";
    }
}

class ThrowParams {
    private int     power;
    private int     x;
    private int     y;
    private boolean shouldMove = false;

    public int getPower() {
        return power;
    }

    public void setPower(int power) {
        this.power = power;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public String toString() {
        return "{ThrowParams x=" + x + " y=" + y + " power=" + power + "}";
    }

    public boolean isShouldMove() {
        return shouldMove;
    }

    public void setShouldMove(boolean shouldMove) {
        this.shouldMove = shouldMove;
    }
}

class Intersection {
    private int x;
    private int y;

    public Intersection(int x, int y) {
        super();
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public String toString() {
        return "{Intersection x=" + x + " y=" + y + "}";
    }
}

class SnaffleWizardPair {
    private Wizard  wizard;
    private Snaffle snaffle;

    public SnaffleWizardPair(Wizard wizard, Snaffle snaffle) {
        this.wizard = wizard;
        this.snaffle = snaffle;
    }

    public Wizard getWizard() {
        return wizard;
    }

    public void setWizard(Wizard wizard) {
        this.wizard = wizard;
    }

    public Snaffle getSnaffle() {
        return snaffle;
    }

    public void setSnaffle(Snaffle snaffle) {
        this.snaffle = snaffle;
    }
}

class RoundParams {
    private List<Wizard>  myWizards;
    private List<Wizard>  opponentWizards;
    private List<Snaffle> snaffles;
    private List<Bludger> bludgers;
    private Wizard        defender;

    public List<Wizard> getMyWizards() {
        return myWizards;
    }

    public void setMyWizards(List<Wizard> myWizards) {
        this.myWizards = myWizards;
    }

    public List<Wizard> getOpponentWizards() {
        return opponentWizards;
    }

    public void setOpponentWizards(List<Wizard> opponentWizards) {
        this.opponentWizards = opponentWizards;
    }

    public List<Snaffle> getSnaffles() {
        return snaffles;
    }

    public void setSnaffles(List<Snaffle> snaffles) {
        this.snaffles = snaffles;
    }

    public List<Bludger> getBludgers() {
        return bludgers;
    }

    public void setBludgers(List<Bludger> bludgers) {
        this.bludgers = bludgers;
    }

    public Wizard getDefender() {
        return defender;
    }

    public void setDefender(Wizard defender) {
        this.defender = defender;
    }
}

class Snaffle extends Entity {
    public Snaffle(int entityId, int x, int y, int vx, int vy) {
        this.id = entityId;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
    }

    public Snaffle() {

    }

    public String toString() {
        return "{Snaffle entity=" + super.toString() + "}";
    }

    @Override
    public int getSize() {
        return 300;
    }
}

class Bludger extends Entity {
    public Bludger(int entityId, int x, int y, int vx, int vy) {
        this.id = entityId;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
    }

    public String toString() {
        return "{Bludger entity=" + super.toString() + "}";
    }

    @Override
    public int getSize() {
        return 400;
    }
}

class Wizard extends Entity {

    private boolean holdsSnaffle;

    public Wizard(int entityId, int x, int y, int vx, int vy, int state) {
        this.id = entityId;
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.holdsSnaffle = state == 1;
    }

    public boolean getHoldsSnaffle() {
        return holdsSnaffle;
    }

    public void setHoldsSnaffle(boolean holdsSnaffle) {
        this.holdsSnaffle = holdsSnaffle;
    }

    public String toString() {
        return "{Wizard entity=" + super.toString() + " holdsSnaffle=" + holdsSnaffle + "}";
    }

    @Override
    public int getSize() {
        return 800;
    }
}

abstract class Entity {

    protected static final int WIZARD = 1;

    public Entity() {
    };

    protected int id;
    protected int x;
    protected int y;

    protected int vx;
    protected int vy;
    protected int type;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getVx() {
        return vx;
    }

    public void setVx(int vx) {
        this.vx = vx;
    }

    public int getVy() {
        return vy;
    }

    public void setVy(int vy) {
        this.vy = vy;
    }

    public String toString() {
        return "{Entity id=" + id + ", x=" + x + ", y=" + y + ", vx=" + vx + ", vy=" + vy + "}";
    }

    public abstract int getSize();

    public List<Segment> getDefiningBoundaries(boolean includeFuture) {
        List<Segment> boundingSegments = new ArrayList<Segment>();
        int size = getSize();
        Coord topLeft = new Coord(x - (size / 2), y - (size / 2));
        boundingSegments
                .add(new Segment(topLeft.getX(), topLeft.getY(), topLeft.getX(), topLeft.getY() + size));
        boundingSegments
                .add(new Segment(topLeft.getX(), topLeft.getY(), topLeft.getX() + size, topLeft.getY()));
        boundingSegments.add(new Segment(topLeft.getX() + size, topLeft.getY() + size, topLeft.getX() + size,
                topLeft.getY()));
        boundingSegments.add(new Segment(topLeft.getX() + size, topLeft.getY() + size, topLeft.getX(),
                topLeft.getY() + size));
        if (includeFuture) {
            List<Segment> allSegments = new ArrayList<Segment>();
            for (Segment crtSegment : boundingSegments) {
                allSegments.add(crtSegment);
                allSegments.add(new Segment(crtSegment.getX1() + vx, crtSegment.getY1() + vy,
                        crtSegment.getX2() + vx, crtSegment.getY2() + vy));
                allSegments.add(new Segment(crtSegment.getX1() + vx * 1.75, crtSegment.getY1() + vy * 1.75,
                        crtSegment.getX2() + vx * 1.75, crtSegment.getY2() + vy * 1.75));
            }
            boundingSegments = allSegments;
        }
        return boundingSegments;
    }

}

class Segment {
    private int x1;
    private int y1;
    private int x2;
    private int y2;

    public Segment(int x1, int y1, int x2, int y2) {
        super();
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
    }

    public Segment(double x1, double y1, double x2, double y2) {
        this.x1 = new Double(x1).intValue();
        this.y1 = new Double(y1).intValue();
        this.x2 = new Double(x2).intValue();
        this.y2 = new Double(y2).intValue();
    }

    public int getX1() {
        return x1;
    }

    public void setX1(int x1) {
        this.x1 = x1;
    }

    public int getX2() {
        return x2;
    }

    public void setX2(int x2) {
        this.x2 = x2;
    }

    public int getY1() {
        return y1;
    }

    public void setY1(int y1) {
        this.y1 = y1;
    }

    public int getY2() {
        return y2;
    }

    public void setY2(int y2) {
        this.y2 = y2;
    }

    public String toString() {
        return "{Segment x1=" + x1 + " y1=" + y1 + "x2=" + x2 + " y2=" + y2 + "}";
    }
}