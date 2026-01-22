/*
 * Copyright (C) 2025 Jerome Blanchard <jayblanc@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package fr.jayblanc.mbyte.manager.process.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProcessTaskOrderingTest {

    @Test
    void findFirstTaskOrdersByNumericPrefixThenType() {
        Process p = new Process();
        p.setTasks("10.deploy,2.validate,1.build,2.archive");

        assertEquals("1.build", p.findFirstTask());
    }

    @Test
    void findNextTaskReturnsFollowingTaskInOrderedList() {
        Process p = new Process();
        p.setTasks("2.validate,1.build,2.archive");

        assertEquals("2.archive", p.findNextTask("1.build"));
        assertEquals("2.validate", p.findNextTask("2.archive"));
        assertEquals(null, p.findNextTask("2.validate"));
    }

    @Test
    void invalidTaskIdsAreSortedAfterValidOnes() {
        Process p = new Process();
        p.setTasks("x,2.validate,1.build,3");

        assertEquals("1.build", p.findFirstTask());
        assertEquals("3", p.findNextTask("2.validate"));
    }
}
