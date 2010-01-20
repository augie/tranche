/*
 *    Copyright 2005 The Regents of the University of Michigan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.tranche.timeestimator;

import org.tranche.util.TrancheTestCase;

/**
 *
 * @author Bryan Smith - bryanesmith@gmail.com
 */
public class ContextualTimeEstimatorTest extends TrancheTestCase {

//    public void testWithImaginaryJobWithSteadyProgressFast() throws Exception {
//        testWithImaginaryJobWithSteadyProgress(100, 10);
//    }
//
//    public void testWithImaginaryJobWithSteadyProgressModerate() throws Exception {
//        testWithImaginaryJobWithSteadyProgress(100, 25);
//    }
//
//    public void testWithImaginaryJobWithSteadyProgressSlow() throws Exception {
//        testWithImaginaryJobWithSteadyProgress(200, 50);
//    }
//
//    /**
//     * Tests the contextual time estimator with an imaginary job that progresses at constant rate.
//     */
//    public void testWithImaginaryJobWithSteadyProgress(int INTERVAL, int TIME_TO_CREATE_WIDGET) throws Exception {
//
//        // Building widgets
//        long widgetsToCreate = 100;
//
//        ContextualTimeEstimator estimator = new ContextualTimeEstimator();
//        estimator.setContextInterval(INTERVAL); // In ms
//
//        for (int created = 0; created <= 100; created++) {
//            Thread.sleep(TIME_TO_CREATE_WIDGET);
//            estimator.update(created, widgetsToCreate);
//
//            assertTrue("Each widget represents 1%", created == estimator.getPercentDone());
//
//            // Skip first 10 estimates; the remaining time will be off
//            if (created > 10 && created % 10 == 0) {
//                long remainingSeconds = Math.round((double) (100 - created) * TIME_TO_CREATE_WIDGET / 1000);
//
//                // 1 second fudge
//                assertTrue("Should anticipate remaining time (finished " + created + " widgets): expecting " + remainingSeconds + " +- 1, instead found " + estimator.getSeconds() + ".", remainingSeconds >= estimator.getSeconds() - 1 && estimator.getSeconds() + 1 >= remainingSeconds);
//            }
//        }
//
//    }
}
