package tileworld;

import tileworld.environment.BaselineEnvironment;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * BaselineTileworldMain
 *
 * Headless runner for baseline testing: 6 plain SmartTWAgent instances,
 * no specialization. Identical loop to TileworldMain but uses BaselineEnvironment.
 */
public class BaselineTileworldMain {

    public static void main(String[] args) throws InterruptedException {
        int overallScore = 0;
        int iteration = 10;
        for (int i = 0; i < iteration; i++) {
            int seed = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);
            System.out.println("Seed: " + seed);
            BaselineEnvironment tw = new BaselineEnvironment(seed);
            tw.start();
            long steps = 0;
            while (steps < Parameters.endTime) {
                if (!tw.schedule.step(tw)) break;
                steps = tw.schedule.getSteps();
            }
            System.out.println("The final reward is: " + tw.getReward());
            overallScore += tw.getReward();
            tw.finish();
            TimeUnit.SECONDS.sleep(1);
        }
        System.out.println("The average reward is: " + ((float) overallScore / iteration));
        System.exit(0);
    }
}
