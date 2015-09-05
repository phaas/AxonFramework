/*
 * Copyright (c) 2010-2012. Axon Framework
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

package org.axonframework.dev;

import org.axonframework.commandhandling.annotation.CommandHandler;
import org.axonframework.commandhandling.annotation.TargetAggregateIdentifier;
import org.axonframework.eventhandling.annotation.EventHandler;
import org.axonframework.eventsourcing.annotation.AbstractAnnotatedAggregateRoot;
import org.axonframework.eventsourcing.annotation.AggregateIdentifier;
import org.axonframework.eventsourcing.annotation.EventSourcingHandler;
import org.axonframework.test.FixtureConfiguration;
import org.axonframework.test.Fixtures;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Allard Buijze
 */
public class AggregateEventCommandHandlingTest {

    static final Logger logger = LoggerFactory.getLogger(AggregateEventCommandHandlingTest.class);

    static class MyAggregate extends AbstractAnnotatedAggregateRoot<String> {

        @AggregateIdentifier
        private String id;
        private int checksum;

        protected MyAggregate() {
            // For event sourcing
        }

        @CommandHandler
        public MyAggregate(CreateAggregateCommand cmd) {
            logger.info("handle(CreateAggregateCommand)");
            apply(new AggregateCreatedEvent(cmd.id));
        }


        @CommandHandler
        private void handle(AggregateCreatedEvent event) {
            logger.info("handle(AggregateCreatedEvent)");

            apply(new AggregateInitializedEvent("applying event in response to event", 3));
            Assert.assertEquals(3, checksum);
        }

        @EventSourcingHandler
        private void on(AggregateCreatedEvent event) {
            logger.info("on(AggregateCreatedEvent)");
            this.id = event.id;
        }

        @EventHandler
        private void on(AggregateInitializedEvent event) {
            logger.info("on(AggregateInitializedEvent)");
            this.checksum = event.checksum;
        }
    }

    private FixtureConfiguration<MyAggregate> fixture = Fixtures.newGivenWhenThenFixture(MyAggregate.class);


    @Test
    public void testHandleEvents() {
        fixture.given().when(
                new CreateAggregateCommand("15")
        ).expectEvents(
                new AggregateCreatedEvent("15"),
                new AggregateInitializedEvent("applying event in response to event", 3)
        );
    }

    private static class AggregateCreatedEvent {

        public final String id;

        public AggregateCreatedEvent(String id) {
            this.id = id;
        }
    }

    private static class AggregateInitializedEvent {

        public final String message;
        public final int checksum;

        public AggregateInitializedEvent(String message, int checksum) {
            this.message = message;
            this.checksum = checksum;
        }
    }

    abstract static class AggregateCommand {

        @TargetAggregateIdentifier
        public final String id;

        protected AggregateCommand(String id) {
            this.id = id;
        }
    }

    static class CreateAggregateCommand extends AggregateCommand {

        protected CreateAggregateCommand(String id) {
            super(id);
        }
    }

    static class ProcessAggregateCommand extends AggregateCommand {

        protected ProcessAggregateCommand(String id) {
            super(id);
        }
    }
}
