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

package org.axonframework.commandhandling.annotation;

import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.GenericCommandMessage;
import org.axonframework.commandhandling.annotation.AnnotationEventDispatchingAdapter;
import org.axonframework.domain.MetaData;
import org.axonframework.eventsourcing.annotation.AbstractAnnotatedAggregateRoot;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Annotated aggregate that supports execution of additional business logic in response to the applied domain events.
 * While it's possible to apply(..) further domain events inside of an @EventSourcingHandler, the events are not
 * processed immediately since the aggregate is already in the middle of an event dispatch cycle.
 * <p/>
 * This class allows events to be routed to @CommandHandler methods, which is outside of the 'state update' lifecycle.
 * Domain logic can thus be more centralized and act in response to events applied from various components of an
 * aggregate.
 * <p/>
 * Implemented as a subclass to be easily incorporated into existing projects pending official support
 * TODO: Merge functionality with AbstractAnnotatedAggregateRoot in future version of Axon.
 *
 * @author Patrick Haas
 * @since 2.4.4
 */
public class EventDispatchingAnnotatedAggregateRoot<T> extends AbstractAnnotatedAggregateRoot<T> {

    /**
     * JVM-level cache of command handlers by class.
     */
    private static final Map<Class<?>, AnnotationEventDispatchingAdapter> handlers =
            new ConcurrentHashMap<Class<?>, AnnotationEventDispatchingAdapter>();

    /**
     * Domain events that have been applied (to a "live" aggregate) that need to be routed to additional
     * business logic handlers once event sourcing has finished and the aggregate is ready to receive
     * further state changes.
     */
    private transient Queue<GenericCommandMessage> eventsToDispatch = new ArrayDeque<GenericCommandMessage>();

    @Override
    protected void apply(Object eventPayload, MetaData metaData) {
        super.apply(eventPayload, metaData);

        if (isLive()) {
            // Record all 'live' events and apply them to command handlers when even sourcing has finished
            String name = eventPayload.getClass().getName();
            if (getHandler().canHandle(name)) {
                if (eventsToDispatch == null) {
                    eventsToDispatch = new ArrayDeque<GenericCommandMessage>();
                }
                eventsToDispatch.add(new GenericCommandMessage(name, eventPayload, metaData));
            }

            if (!isApplyingEvents()) {
                while (eventsToDispatch != null && !eventsToDispatch.isEmpty()) {
                    dispatch(eventsToDispatch.poll());
                }
            }
        }
    }

    /**
     * Find and invoke @CommandHandlers that respond to applied events.
     *
     * @param payload
     */
    protected void dispatch(CommandMessage<Object> payload) {
        try {
            getHandler().handle(this, payload);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Retrieve or create a CommandHandler invoker for this aggregate.
     *
     * @return
     */
    private AnnotationEventDispatchingAdapter getHandler() {
        AnnotationEventDispatchingAdapter handler = handlers.get(getClass());
        if (handler == null) {
            handler = new AnnotationEventDispatchingAdapter(getClass());
            handlers.put(getClass(), handler);
        }
        return handler;
    }
}
