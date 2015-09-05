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
import org.axonframework.commandhandling.annotation.AggregateCommandHandlerInspector;
import org.axonframework.common.Assert;
import org.axonframework.common.annotation.AbstractMessageHandler;
import org.axonframework.common.annotation.ClasspathParameterResolverFactory;
import org.axonframework.common.annotation.ParameterResolverFactory;
import org.axonframework.domain.AggregateRoot;

import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import static org.axonframework.commandhandling.annotation.CommandMessageHandlerUtils.resolveAcceptedCommandName;

/**
 * Command handler that dispatches commands to {@link org.axonframework.commandhandling.annotation.CommandHandler}
 * annotated methods on an aggregate.
 * <p/>
 * <code>@CommandHandler</code> methods may be on the aggregate itself, as well as on
 * <code>@CommmandHandlingMember</code>s or <code>@CommandHandlingMemberCollection</code>s.
 * However, constructor-based <code>@CommandHandler</code>s are not supported since this adapter only targets already
 * instantiated
 * objects.
 *
 * @param <T> the type of aggregate this handler handles commands for
 * @author Allard Buijze
 * @author Patrick Haas
 * @since 2.4.4
 */
public class AnnotationEventDispatchingAdapter<T extends AggregateRoot> {

    private final Map<String, AbstractMessageHandler> handlers;

    /**
     * Initializes an AnnotationEventDispatchingAdapter based on the annotations on given <code>aggregateType</code>.
     *
     * @param aggregateType The type of aggregate
     */
    public AnnotationEventDispatchingAdapter(Class<T> aggregateType) {
        this(aggregateType, ClasspathParameterResolverFactory.forClass(aggregateType));
    }

    /**
     * Initializes an AnnotationCommandHandler based on the annotations on given <code>aggregateType</code>, using the
     * given <code>parameterResolverFactory</code>.
     *
     * @param aggregateType            The type of aggregate
     * @param parameterResolverFactory The strategy for resolving parameter values for handler methods
     */
    public AnnotationEventDispatchingAdapter(Class<T> aggregateType,
                                             ParameterResolverFactory parameterResolverFactory) {
        Assert.notNull(aggregateType, "aggregateType may not be null");
        Assert.notNull(parameterResolverFactory, "parameterResolverFactory may not be null");
        this.handlers = initializeHandlers(aggregateType, parameterResolverFactory);
    }


    private static <T extends AggregateRoot> Map<String, AbstractMessageHandler> initializeHandlers(
            Class<T> aggregateType, ParameterResolverFactory parameterResolverFactory) {

        AggregateCommandHandlerInspector<T> inspector = new AggregateCommandHandlerInspector<T>(
                aggregateType, parameterResolverFactory);

        Map<String, AbstractMessageHandler> handlersFound = new HashMap<String, AbstractMessageHandler>();
        for (final AbstractMessageHandler commandHandler : inspector.getHandlers()) {
            handlersFound.put(resolveAcceptedCommandName(commandHandler), commandHandler);
        }
        return handlersFound;
    }

    public boolean canHandle(String commandName) {
        return handlers.containsKey(commandName);
    }

    public void handle(T target, CommandMessage<Object> commandMessage)
            throws InvocationTargetException, IllegalAccessException {
        AbstractMessageHandler handler = handlers.get(commandMessage.getCommandName());
        if (handler == null) {
            return;
        }
        handler.invoke(target, commandMessage);
    }
}
