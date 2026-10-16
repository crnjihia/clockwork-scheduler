package com.clockwork.scheduler.workflow;

import com.clockwork.scheduler.domain.enums.StatementEvent;
import com.clockwork.scheduler.domain.enums.StatementState;
import org.springframework.context.annotation.Configuration;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.EnumStateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;

import java.util.EnumSet;

@Configuration
@EnableStateMachineFactory(name = "statementStateMachineFactory")
public class StatementStateMachineConfig extends EnumStateMachineConfigurerAdapter<StatementState, StatementEvent> {

    @Override
    public void configure(StateMachineConfigurationConfigurer<StatementState, StatementEvent> config) throws Exception {
        config.withConfiguration()
                .autoStartup(false);
    }

    @Override
    public void configure(StateMachineStateConfigurer<StatementState, StatementEvent> states) throws Exception {
        states.withStates()
                .initial(StatementState.CREATED)
                .states(EnumSet.allOf(StatementState.class))
                .end(StatementState.COMPLETED);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<StatementState, StatementEvent> transitions) throws Exception {
        transitions
                // START -> CREATED to GENERATING
                .withExternal()
                    .source(StatementState.CREATED).target(StatementState.GENERATING)
                    .event(StatementEvent.START)
                .and()
                // GENERATING to EMAILING
                .withExternal()
                    .source(StatementState.GENERATING).target(StatementState.EMAILING)
                    .event(StatementEvent.GENERATE_SUCCESS)
                .and()
                // GENERATING to FAILED
                .withExternal()
                    .source(StatementState.GENERATING).target(StatementState.FAILED)
                    .event(StatementEvent.GENERATE_FAILURE)
                .and()
                // EMAILING to ARCHIVING
                .withExternal()
                    .source(StatementState.EMAILING).target(StatementState.ARCHIVING)
                    .event(StatementEvent.EMAIL_SUCCESS)
                .and()
                // EMAILING to FAILED
                .withExternal()
                    .source(StatementState.EMAILING).target(StatementState.FAILED)
                    .event(StatementEvent.EMAIL_FAILURE)
                .and()
                // ARCHIVING to COMPLETED
                .withExternal()
                    .source(StatementState.ARCHIVING).target(StatementState.COMPLETED)
                    .event(StatementEvent.ARCHIVE_SUCCESS)
                .and()
                // ARCHIVING to FAILED
                .withExternal()
                    .source(StatementState.ARCHIVING).target(StatementState.FAILED)
                    .event(StatementEvent.ARCHIVE_FAILURE)
                .and()
                // RETRY from FAILED back to GENERATING
                .withExternal()
                    .source(StatementState.FAILED).target(StatementState.GENERATING)
                    .event(StatementEvent.RETRY);
    }
}
