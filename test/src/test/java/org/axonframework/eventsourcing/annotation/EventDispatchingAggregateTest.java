/*
 * Copyright (c) 2015. Axon Framework
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

package org.axonframework.eventsourcing.annotation;

import org.axonframework.commandhandling.annotation.CommandHandler;
import org.axonframework.commandhandling.annotation.CommandHandlingMember;
import org.axonframework.commandhandling.annotation.EventDispatchingAnnotatedAggregateRoot;
import org.axonframework.commandhandling.annotation.TargetAggregateIdentifier;
import org.axonframework.eventhandling.annotation.EventHandler;
import org.axonframework.test.FixtureConfiguration;
import org.axonframework.test.Fixtures;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

/**
 * TODO replicate test in axon-core/test, without access to GivenWhenThenTestFixture.
 *
 * @author Patrick Haas
 */
public class EventDispatchingAggregateTest {

    static final Logger logger = LoggerFactory.getLogger(EventDispatchingAggregateTest.class);
    public static final String ID = "15";

    static class MyAggregate extends EventDispatchingAnnotatedAggregateRoot<String> {

        @AggregateIdentifier
        private String id;
        private int checksum;

        @CommandHandlingMember
        private final MyMember member = new MyMember(this);

        protected MyAggregate() {
            // For event sourcing
        }

        @CommandHandler
        public MyAggregate(CreateAggregateCommand cmd) {
            logger.info("handle(CreateAggregateCommand)");
            apply(new AggregateCreatedEvent(cmd.id));
        }

        @CommandHandler
        public void handle(ProcessAggregateCommand cmd) {
            logger.info("handle(ProcessAggregateCommand)");
            apply(new AggregateProcessedEvent());
        }

        @CommandHandler
        private void handle(AggregateCreatedEvent event, MyResource query) {
            logger.info("handle(AggregateCreatedEvent)");

            apply(new AggregateUpdatedEvent("applying event in response to event: " + query.useResource(), 3));
            assertEquals(3, checksum);
        }

        @EventHandler
        private void on(AggregateCreatedEvent event) {
            logger.info("on(AggregateCreatedEvent)");
            this.id = event.id;
        }

        @EventHandler
        private void on(AggregateUpdatedEvent event) {
            logger.info("on(AggregateUpdatedEvent)");
            this.checksum = event.checksum;
        }

        private class MyMember extends AbstractAnnotatedEntity {

            public MyMember(MyAggregate myAggregate) {
                registerAggregateRoot(myAggregate);
            }

            @CommandHandler
            public void handle(AggregateProcessedEvent event) {
                logger.info("handle(AggregateProcessedEvent");

                int initial = checksum;
                int intermediate = initial * 3;

                apply(new AggregateUpdatedEvent("Processing...", intermediate));
                assertEquals(intermediate, checksum);

                int result = checksum * 2;
                apply(new AggregateUpdatedEvent("Completing...", result));
                assertEquals(result, checksum);
            }
        }
    }

    private FixtureConfiguration<MyAggregate> fixture;

    @Before
    public void setUp() {
        fixture = Fixtures.newGivenWhenThenFixture(MyAggregate.class);
        fixture.registerInjectableResource(new MyResource());
    }

    @Test
    public void testHandleEvents_OnCreation() {
        fixture.givenCommands().when(
                new CreateAggregateCommand(ID)
        ).expectEvents(
                new AggregateCreatedEvent(ID),
                new AggregateUpdatedEvent("applying event in response to event: OK", 3)
        );
    }

    @Test
    public void testHandleEvents() {
        fixture.givenCommands(
                new CreateAggregateCommand(ID)
        ).when(
                new ProcessAggregateCommand(ID)
        ).expectEvents(
                new AggregateProcessedEvent(),
                new AggregateUpdatedEvent("Processing...", 9),
                new AggregateUpdatedEvent("Completing...", 18)
        );
    }

    private static class AggregateCreatedEvent {

        public final String id;

        public AggregateCreatedEvent(String id) {
            this.id = id;
        }
    }

    private static class AggregateUpdatedEvent {

        public final String message;
        public final int checksum;

        public AggregateUpdatedEvent(String message, int checksum) {
            this.message = message;
            this.checksum = checksum;
        }
    }

    private static class AggregateProcessedEvent {

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

    static class MyResource {

        public String useResource() {
            logger.info("useResource()");
            return "OK";
        }
    }
}
