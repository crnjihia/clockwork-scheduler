package com.clockwork.scheduler.workflow;

import com.clockwork.scheduler.domain.enums.StatementEvent;
import com.clockwork.scheduler.domain.enums.StatementState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = StatementStateMachineConfig.class)
class StatementStateMachineTest {

    @Autowired
    private StateMachineFactory<StatementState, StatementEvent> stateMachineFactory;

    private StateMachine<StatementState, StatementEvent> stateMachine;

    @BeforeEach
    void setUp() {
        stateMachine = stateMachineFactory.getStateMachine("test-machine");
        stateMachine.startReactively().block();
    }

    private void sendEvent(StatementEvent event) {
        stateMachine.sendEvent(Mono.just(MessageBuilder.withPayload(event).build())).blockLast();
    }

    @Test
    @DisplayName("Initial state should be CREATED")
    void testInitialState() {
        assertEquals(StatementState.CREATED, stateMachine.getState().getId());
    }

    @Test
    @DisplayName("Successful linear progression across all workflow states to COMPLETED")
    void testFullWorkflowHappyPath() {
        // CREATED -> GENERATING
        sendEvent(StatementEvent.START);
        assertEquals(StatementState.GENERATING, stateMachine.getState().getId());

        // GENERATING -> EMAILING
        sendEvent(StatementEvent.GENERATE_SUCCESS);
        assertEquals(StatementState.EMAILING, stateMachine.getState().getId());

        // EMAILING -> ARCHIVING
        sendEvent(StatementEvent.EMAIL_SUCCESS);
        assertEquals(StatementState.ARCHIVING, stateMachine.getState().getId());

        // ARCHIVING -> COMPLETED
        sendEvent(StatementEvent.ARCHIVE_SUCCESS);
        assertEquals(StatementState.COMPLETED, stateMachine.getState().getId());
        assertTrue(stateMachine.isComplete());
    }

    @Test
    @DisplayName("Failure transition moves to FAILED state and can be RETRIED")
    void testFailureAndRetry() {
        sendEvent(StatementEvent.START);
        assertEquals(StatementState.GENERATING, stateMachine.getState().getId());

        // Failure at generation step
        sendEvent(StatementEvent.GENERATE_FAILURE);
        assertEquals(StatementState.FAILED, stateMachine.getState().getId());

        // Retry transition resets to GENERATING
        sendEvent(StatementEvent.RETRY);
        assertEquals(StatementState.GENERATING, stateMachine.getState().getId());
    }
}
